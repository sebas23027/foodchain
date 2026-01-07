package foodchain;

import java.io.Serializable;
import java.security.PublicKey;
import utils.SecurityUtils;
import utils.Utils;

/**
 * Transaction for food supply chain.
 * Two types:
 *   - CREATE: Producer creates a new product (sender = producer, receiver = null)
 *   - TRANSFER: Transfer product custody between stations
 */
public class FoodTransaction implements Serializable {

    public enum TransactionType {
        CREATE,    // Produtor cria produto
        TRANSFER   // Transferência entre estações
    }

    private final TransactionType type;
    private final String txtSender;
    private final String txtReceiver;
    private final PublicKey sender;
    private final PublicKey receiver;
    private final FoodProduct product;
    private final long timestamp;
    private final byte[] signature;

    /**
     * Construtor para CRIAR produto (produtor)
     */
    public FoodTransaction(String producerName, FoodProduct product, String password) throws Exception {
        FoodUser uProducer = FoodUser.login(producerName, password);

        this.type = TransactionType.CREATE;
        this.txtSender = uProducer.getUserName();
        this.sender = uProducer.getPublicKey();
        this.txtReceiver = "SYSTEM"; // Não há recetor na criação
        this.receiver = null;
        this.product = product;
        this.timestamp = System.currentTimeMillis();

        byte[] allData = Utils.concatenate(sender.getEncoded(), new byte[]{0x01});
        allData = Utils.concatenate(allData, Utils.toBytes(product));
        allData = Utils.concatenate(allData, Utils.longToBytes(timestamp));
        this.signature = SecurityUtils.sign(allData, uProducer.getPrivateKey());
    }
    
    /**
     * Construtor para TRANSFERIR produto entre estações
     */
    public FoodTransaction(String senderName, String receiverName, FoodProduct product, String password) throws Exception {
        FoodUser uSender = FoodUser.login(senderName, password);
        FoodUser uReceiver = FoodUser.login(receiverName);

        this.type = TransactionType.TRANSFER;
        this.txtSender = uSender.getUserName();
        this.sender = uSender.getPublicKey();
        this.txtReceiver = uReceiver.getUserName();
        this.receiver = uReceiver.getPublicKey();
        this.product = product;
        this.timestamp = System.currentTimeMillis();

        byte[] allData = Utils.concatenate(sender.getEncoded(), receiver.getEncoded());
        allData = Utils.concatenate(allData, Utils.toBytes(product));
        allData = Utils.concatenate(allData, Utils.longToBytes(timestamp));
        this.signature = SecurityUtils.sign(allData, uSender.getPrivateKey());
    }

    public TransactionType getType() {
        return type;
    }
    
    public boolean isCreateTransaction() {
        return type == TransactionType.CREATE;
    }
    
    public boolean isTransferTransaction() {
        return type == TransactionType.TRANSFER;
    }

    public String getTxtSender() {
        return txtSender;
    }

    public String getTxtReceiver() {
        return txtReceiver;
    }

    public PublicKey getSender() {
        return sender;
    }

    public PublicKey getReceiver() {
        return receiver;
    }

    public FoodProduct getProduct() {
        return product;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder();
        if (isCreateTransaction()) {
            txt.append("[CREATE] Produtor: ").append(txtSender).append("\n");
        } else {
            txt.append("[TRANSFER] ").append(txtSender).append(" → ").append(txtReceiver).append("\n");
        }
        txt.append(product.toString());
        return txt.toString();
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    private static final long serialVersionUID = 202601050002L;
    //:::::::::::::::::::::::::::  Copyright(c) 2026  ::::::::::::::::::::::::::
}
