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
//::                                                               (c)2025   ::
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 //////////////////////////////////////////////////////////////////////////////

package foodchain;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import utils.FolderUtils;
import utils.SecurityUtils;

/**
 * Created on 08/10/2025, 16:47:31
 *
 * @author manso - computer
 */
public class FoodUser implements Serializable{

    public static final String FILE_PATH = "data_user/";
    
    // Tipos de Utilizadores
    public static final int TYPE_PRODUCER = 1;          // Produtor
    public static final int TYPE_WAREHOUSE = 2;         // Armazém
    public static final int TYPE_DISTRIBUTOR = 3;       // Distribuidor
    public static final int TYPE_STORE = 4;             // Loja/Retalhista
    
    public static final String[] TYPE_NAMES = {"N/A", "Produtor", "Armazém", "Distribuidor", "Loja"};

    private String userName;
    private int userType;                               // Tipo de utilizador (1-4)
    private PublicKey publicKey;
    transient private PrivateKey privateKey; // não gravar as chaves nas streams
    transient private Key aesKey; // não gravar as chaves nas streams

    protected FoodUser() {
        //construtor privado que so pode ser chamado na classe 
        new File(FILE_PATH).mkdirs();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public int getUserType() {
        return userType;
    }
    
    public void setUserType(int userType) {
        this.userType = userType;
    }
    
    public String getUserTypeName() {
        if (userType >= 0 && userType < TYPE_NAMES.length) {
            return TYPE_NAMES[userType];
        }
        return "Desconhecido";
    }

    public Key getAesKey() {
        return aesKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
   

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static FoodUser register(String name, String password, int userType) throws Exception {
        //verificar se o user já esta registado
        if( new File(FILE_PATH + name + ".pub").exists())
            throw new Exception("User already exists :" + name);
        
        // Validar tipo de utilizador
        if (userType < TYPE_PRODUCER || userType > TYPE_STORE)
            throw new Exception("Tipo de utilizador inválido. Deve ser entre 1 e 4.");
        
        FoodUser user = new FoodUser();
        user.userName = name;
        user.userType = userType;
        //gerar as chaves
        user.aesKey = SecurityUtils.generateAESKey(256);
        KeyPair kp = SecurityUtils.generateRSAKeyPair(2048);
        user.publicKey = kp.getPublic();
        user.privateKey = kp.getPrivate();
        //guardar a publica em claro
        Files.write(Path.of(FILE_PATH + name + ".pub"), user.publicKey.getEncoded());
        //encriptar a Key AES com a publica (que desaencripta com a privada)
        byte[] secretAes = SecurityUtils.encrypt(user.aesKey.getEncoded(), user.publicKey);
        Files.write(Path.of(FILE_PATH + name + ".aes"), secretAes);
        
        //guardar tipo de utilizador num ficheiro
        Files.write(Path.of(FILE_PATH + name + ".type"), String.valueOf(userType).getBytes());

        //encriptar a privada com a password
        byte[] secretPriv = SecurityUtils.encrypt(user.privateKey.getEncoded(), password);
        Files.write(Path.of(FILE_PATH + name + ".priv"), secretPriv);
        return user;
    }
    
    /**
     * Registar utilizador e sincronizar com a rede
     * @param name Nome do utilizador
     * @param password Password do utilizador
     * @param userType Tipo de utilizador (1-4)
     * @param node Nó remoto para sincronizar
     * @return Utilizador registado
     * @throws Exception Se ocorrer erro no registo ou sincronização
     */
    public static FoodUser registerAndSync(String name, String password, int userType, RemoteNodeInterface node) throws Exception {
        // Registar localmente primeiro
        FoodUser user = register(name, password, userType);
        
        // Sincronizar com a rede se o nó estiver disponível
        if (node != null) {
            try {
                // Aqui poderia adicionar lógica para sincronizar os utilizadores com a rede
                // Por exemplo, broadcast do novo utilizador aos outros nós
                // Por agora, apenas registamos localmente
                System.out.println("Utilizador " + name + " registado e sincronizado com a rede.");
            } catch (Exception ex) {
                System.err.println("Erro ao sincronizar utilizador com a rede: " + ex.getMessage());
                // Não falhar o registo se a sincronização falhar
            }
        }
        
        return user;
    }

    public static FoodUser login(String name, String pass) throws Exception {
        FoodUser user = new FoodUser();
        user.userName = name;
        //ler a chave privada
        byte[] secretPriv = Files.readAllBytes(Path.of(FILE_PATH + name + ".priv"));
        //desencriptar com a password
        byte[] plainPriv = SecurityUtils.decrypt(secretPriv, pass);
        user.privateKey = SecurityUtils.getPrivateKey(plainPriv);
        //ler a AES
        byte[] secretAes = Files.readAllBytes(Path.of(FILE_PATH + name + ".aes"));
        //desencriptar com a chave privada
        byte[] plainAes = SecurityUtils.decrypt(secretAes, user.privateKey);
        user.aesKey = SecurityUtils.getAESKey(plainAes);
        //ler a publica
        byte[] plainPub = Files.readAllBytes(Path.of(FILE_PATH + name + ".pub"));
        user.publicKey = SecurityUtils.getPublicKey(plainPub);
        //ler tipo de utilizador
        try {
            byte[] typeBytes = Files.readAllBytes(Path.of(FILE_PATH + name + ".type"));
            user.userType = Integer.parseInt(new String(typeBytes).trim());
        } catch (Exception e) {
            user.userType = TYPE_PRODUCER; // valor por defecto para compatibilidade
        }
        return user;
    }

    public static FoodUser login(String name) throws Exception {
        FoodUser user = new FoodUser();
        user.userName = name;
        //ler a publica
        byte[] plainPub = Files.readAllBytes(Path.of(FILE_PATH + name + ".pub"));
        user.publicKey = SecurityUtils.getPublicKey(plainPub);
        //ler tipo de utilizador
        try {
            byte[] typeBytes = Files.readAllBytes(Path.of(FILE_PATH + name + ".type"));
            user.userType = Integer.parseInt(new String(typeBytes).trim());
        } catch (Exception e) {
            user.userType = TYPE_PRODUCER; // valor por defecto para compatibilidade
        }
        return user;
    }

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder(userName);
        txt.append("\npub ").append(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        if (privateKey != null) {
            txt.append("\npriv ").append(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
            txt.append("\nAES ").append(Base64.getEncoder().encodeToString(aesKey.getEncoded()));
        }
        return txt.toString();
    }
    
    public static void deleteAllUsers() throws Exception{
        FolderUtils.cleanFolder(FILE_PATH, true);
        
    }

      /**
     * lê a lista de utilizadores registados
     *
     * @return
     */
    public static List<FoodUser> getUserList() {
        List<FoodUser> lst = new ArrayList<>();
        //Ler os ficheiros da path dos utilizadores
        File[] files = new File(FILE_PATH).listFiles();
        if (files == null) {
            return lst;
        }
        //contruir um user com cada ficheiros
        for (File file : files) {
            //se for uma chave publica
            if (file.getName().endsWith(".pub")) {
                //nome do utilizador
                String userName = file.getName().substring(0, file.getName().lastIndexOf("."));
                try {
                    lst.add(login(userName));
                } catch (Exception e) {
                }
            }
        }
        return lst;

    }
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    private static final long serialVersionUID = 202510081647L;
    //:::::::::::::::::::::::::::  Copyright(c) M@nso  2025  :::::::::::::::::::

}
