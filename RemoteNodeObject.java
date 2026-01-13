//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: 
//::                                                                         ::
//::     Antonio Manuel Rodrigues Manso                                      ::
//::                                                                         ::
//::     I N S T I T U T O    P O L I T E C N I C O   D E   T O M A R        ::
//::     Escola Superior de Tecnologia de Tomar                              ::
//::     e-mail: manso@ipt.pt                                                ::
//::     url   : http://orion.ipt.pt/~manso                                  ::
//::                                                                         ::
//::     This software was build with the purpose of investigate and         ::
//::     learning.                                                           ::
//::                                                                         ::
//::                                                               (c)2024   ::
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 //////////////////////////////////////////////////////////////////////////////
package foodchain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.RMI;

/**
 * Created on 27/11/2024, 17:48:32
 *
 * @author manso - computer
 */
public class RemoteNodeObject extends UnicastRemoteObject implements RemoteNodeInterface {

    public static String REMOTE_OBJECT_NAME = "remoteNode";

    String address;
    Set<RemoteNodeInterface> network;
    Set<String> transactions;
    Nodelistener listener;
    MinerDistibuted miner = new MinerDistibuted();
    BlockChain blockchain;
    Block currentBlock;

    public RemoteNodeObject(int port, Nodelistener listener) throws RemoteException {
        super(port);
        try {
            //local adress of server
            String host = InetAddress.getLocalHost().getHostAddress();
            this.address = RMI.getRemoteName(host, port, REMOTE_OBJECT_NAME);
            this.network = new CopyOnWriteArraySet<>();
            this.transactions = new CopyOnWriteArraySet<>();
            this.blockchain = BlockChain.load("data/" + port + "/", "blockchain.blc");
            // addNode(this);
            this.listener = listener;
            if (listener != null) {
                listener.onStart("Object " + address + " listening");
                listener.onBlockchain(blockchain);
            } else {
                System.err.println("Object " + address + " listening");
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(RemoteNodeObject.class.getName()).log(Level.SEVERE, null, ex);
            if (listener != null) {
                listener.onException(ex, "Start remote Object");
            }
        }

    }

    @Override
    public String getAdress() throws RemoteException {
        return address;
    }

    @Override
    public void addNode(RemoteNodeInterface node) throws RemoteException {
        //se já tiver o nó  -  não faz nada
        if (network.contains(node)) {
            return;
        }
        //adicionar o no
        network.add(node);
        //Adicionar as transacoes
        this.transactions.addAll(node.getTransactions());
        //adicionar o this ao remoto
        node.addNode(this);
        //sincronizar a blockchain
        synchronizeBlockchain(node);
        //sincronizar utilizadores
        try {
            sendUserSyncTo(node);
        } catch (Exception e) {
            if (listener != null) {
                listener.onMessage("Aviso: Erro ao sincronizar utilizadores - " + e.getMessage());
            }
        }
        //propagar o no na rede
        for (RemoteNodeInterface iremoteP2P : network) {
            iremoteP2P.addNode(node);
        }
        if (listener != null) {
            listener.onConect(node.getAdress());
        } else {
            System.out.println("Connected to node.getAdress()");
        }
        //::::::::: DEBUG  ::::::::::::::::
        System.out.println("Rede p2p");
        for (RemoteNodeInterface iremoteP2P : network) {
            System.out.println(iremoteP2P.getAdress());

        }

    }

    @Override
    public List<RemoteNodeInterface> getNetwork() throws RemoteException {
        return new ArrayList<>(network);
    }
//::::::::::: T R A NS A C T IO N S  :::::::::::

    @Override
    public void addTransaction(String data) throws RemoteException {
        if (this.transactions.contains(data)) {
            return;
        }
        this.transactions.add(data);
        for (RemoteNodeInterface node : network) {
            //uma thread para ligar a cada no
            new Thread(() -> {
                try {
                    node.addTransaction(data);
                } catch (Exception e) {
                    network.remove(node);
                }
            }).start();

        }
        if (listener != null) {
            listener.onConect("");
            listener.onTransaction(data);
        } else {
            System.out.println("Transaction from  " + getRemoteHost());
        }
        for (String t : transactions) {
            System.out.println(t);
        }
    }

    @Override
    public List<String> getTransactions() throws RemoteException {
        return new ArrayList<>(transactions);
        
    }

    private String getRemoteHost() {
        try {
            return RemoteServer.getClientHost();
        } catch (ServerNotActiveException ex) {
            return "unknown";
        }
    }

    //::::::::::: M I N E R  :::::::::::
    public void startMiner(int dificulty) throws RemoteException {        
        startMiner(address, dificulty);
    }

    @Override
    public void startMiner(String message, int dificulty) throws RemoteException {

//        se estiver a minar
        if (miner.isMining() || transactions.isEmpty()) {
            return; // não faz nada
        }
        //criar um bloco com as transações
        currentBlock = blockchain.createNewBlock(new ArrayList<>(transactions));
        miner.isWorking.set(true);
        for (RemoteNodeInterface node : network) {
            //uma thread para ligar a cada no
            new Thread(() -> {
                try {
                    node.startMiner(message, dificulty);
                } catch (Exception e) {
                    network.remove(node);
                }
            }).start();
        }        
        new Thread(() -> {
            miner.mine(currentBlock.getHeaderDataBase64(), dificulty);
        }).start();
    }

    @Override
    public void stopMining(int nonce) throws RemoteException {
        //se não estiver a minar
        if (!miner.isMining()) {
            return; //nao faz nada
        }
        miner.stopMining(nonce);
        for (RemoteNodeInterface node : network) {
            //uma thread para ligar a cada no
            new Thread(() -> {
                try {
                    node.stopMining(nonce);
                } catch (Exception e) {
                    network.remove(node);
                }
            }).start();

        }
    }

    @Override
    public MinerDistibuted getMiner() throws RemoteException {
        return miner;
    }

    public void setNonce(int nonce) throws Exception {
        currentBlock.setNonce(nonce);
        addBlock(currentBlock);
    }

    @Override
    public void addBlock(Block block) throws RemoteException {
        System.out.println("prev " + blockchain.getLastBlock().toString());
        System.out.println("add " + block.toString());
        try {
            //blockchain constains the current block
            if (this.blockchain.getBlocks().contains(block)) {
                return;
            }
            //add block
            blockchain.add(block);
            //propagate block
            for (RemoteNodeInterface node : network) {
                node.addBlock(block);
            }
            //remove transactions of block
            List<String> blockTrasactions = block.getData().getElements();
            for (String blockt : blockTrasactions) {
                if (transactions.contains(blockt)) {
                    transactions.remove(blockt);
                }
            }
            //notify listener
            if (listener != null) {
                listener.onTransaction("");
                listener.onBlockchain(blockchain);
            }
        } catch (Exception ex) {
            throw new RemoteException(ex.getMessage());
        }
    }

    @Override
    public int getBlockchainSize() throws RemoteException {
        return blockchain.getBlocks().size();
    }

    @Override
    public BlockChain getBlockchain() throws RemoteException {
        return blockchain;
    }

    @Override
    public void setBlockchain(BlockChain b) throws RemoteException {
        try {
            this.blockchain.setBlocks(b.getBlocks());
             //notify listener
            if (listener != null) {
                listener.onBlockchain(blockchain);
            }
        } catch (Exception ex) {
            throw new RemoteException(ex.getMessage());
        }
    }

    public Block getlastBlock() throws RemoteException {
        return blockchain.getLastBlock();
    }

    @Override
    public void synchronizeBlockchain(RemoteNodeInterface node) throws RemoteException {
        //::::::::::::::::::::::::::::::::::::::::
        //node blockchain is bigger
        if (node.getBlockchainSize() > getBlockchainSize()) {
            setBlockchain(node.getBlockchain());
            //notify listener
            if (listener != null) {
                listener.onBlockchain(blockchain);
            }
        }//::::::::::::::::::::::::::::::::::::::::
        //my blockchain is bigger
        else if (node.getBlockchainSize() < getBlockchainSize()) {
            //update node blockchain
            node.setBlockchain(blockchain);
        } //::::::::::::::::::::::::::::::::::::::::
        //the size is equal
        else {
            // my last block time is younger
            if (getlastBlock().getTimestamp() > node.getlastBlock().getTimestamp()) {
                setBlockchain(node.getBlockchain());
                //notify listener
                if (listener != null) {
                    listener.onBlockchain(blockchain);
                }
            }
            // my last block time is older
            if (getlastBlock().getTimestamp() < node.getlastBlock().getTimestamp()) {
                //update node blockchain
                node.setBlockchain(blockchain);
            }
        }
    }
     public List getRegisteredTransactions() throws RemoteException{         
         return blockchain.getTransactions();
     }
     
     //::::::::::: U S E R S  S Y N C  :::::::::::
     @Override
     public void syncUserFiles(String userName, byte[] pubKey, byte[] aesKey, byte[] privKey, int userType) throws RemoteException {
         try {
             java.nio.file.Path userPath = java.nio.file.Path.of(FoodUser.FILE_PATH);
             java.nio.file.Files.createDirectories(userPath);
             
             // Verificar se o utilizador já existe
             boolean userExists = java.nio.file.Files.exists(userPath.resolve(userName + ".pub"));
             
             // Guardar ficheiros do utilizador localmente
             java.nio.file.Files.write(userPath.resolve(userName + ".pub"), pubKey);
             java.nio.file.Files.write(userPath.resolve(userName + ".aes"), aesKey);
             java.nio.file.Files.write(userPath.resolve(userName + ".priv"), privKey);
             java.nio.file.Files.write(userPath.resolve(userName + ".type"), String.valueOf(userType).getBytes());
             
             if (listener != null) {
                 listener.onMessage("Utilizador sincronizado: " + userName);
             }
             
             // Propagar APENAS se for novo utilizador (evita loops)
             if (!userExists) {
                 for (RemoteNodeInterface node : network) {
                     try {
                         node.syncUserFiles(userName, pubKey, aesKey, privKey, userType);
                     } catch (Exception e) {
                         // Ignora erros de propagação
                     }
                 }
             }
         } catch (Exception ex) {
             throw new RemoteException("Erro ao sincronizar utilizador: " + ex.getMessage());
         }
     }
     
     /**
      * Envia apenas os utilizadores locais para um nó específico (sem propagação para evitar loop)
      */
     private void sendUserSyncTo(RemoteNodeInterface targetNode) throws RemoteException {
         try {
             java.io.File[] files = new java.io.File(FoodUser.FILE_PATH).listFiles();
             if (files == null) return;
             
             for (java.io.File file : files) {
                 if (file.getName().endsWith(".pub")) {
                     String userName = file.getName().substring(0, file.getName().lastIndexOf("."));
                     try {
                         byte[] pubKey = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(FoodUser.FILE_PATH + userName + ".pub"));
                         byte[] aesKey = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(FoodUser.FILE_PATH + userName + ".aes"));
                         byte[] privKey = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(FoodUser.FILE_PATH + userName + ".priv"));
                         byte[] typeBytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(FoodUser.FILE_PATH + userName + ".type"));
                         int userType = Integer.parseInt(new String(typeBytes).trim());
                         
                         // Enviar APENAS para o nó específico (sem loop)
                         targetNode.syncUserFiles(userName, pubKey, aesKey, privKey, userType);
                     } catch (Exception e) {
                         // Ignora utilizadores com ficheiros incompletos
                     }
                 }
             }
         } catch (Exception ex) {
             throw new RemoteException("Erro ao sincronizar: " + ex.getMessage());
         }
     }
     
     @Override
     public void requestUserSync() throws RemoteException {
         try {
             // Ler todos os utilizadores locais
             java.io.File[] files = new java.io.File(FoodUser.FILE_PATH).listFiles();
             if (files == null) return;
             
             for (java.io.File file : files) {
                 if (file.getName().endsWith(".pub")) {
                     String userName = file.getName().substring(0, file.getName().lastIndexOf("."));
                     try {
                         byte[] pubKey = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(FoodUser.FILE_PATH + userName + ".pub"));
                         byte[] aesKey = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(FoodUser.FILE_PATH + userName + ".aes"));
                         byte[] privKey = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(FoodUser.FILE_PATH + userName + ".priv"));
                         byte[] typeBytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(FoodUser.FILE_PATH + userName + ".type"));
                         int userType = Integer.parseInt(new String(typeBytes).trim());
                         
                         // Enviar para todos os nós da rede
                         for (RemoteNodeInterface node : network) {
                             try {
                                 node.syncUserFiles(userName, pubKey, aesKey, privKey, userType);
                             } catch (Exception e) {
                                 // Ignora erros individuais
                             }
                         }
                     } catch (Exception e) {
                         // Ignora utilizadores com ficheiros incompletos
                     }
                 }
             }
             
             if (listener != null) {
                 listener.onMessage("Sincronização de utilizadores concluída");
             }
         } catch (Exception ex) {
             throw new RemoteException("Erro na sincronização: " + ex.getMessage());
         }
     }
}
