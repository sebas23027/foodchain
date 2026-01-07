package foodchain;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.Base64;

/**
 * Product metadata with custody chain tracking for food supply chain.
 */
public class FoodProduct implements Serializable {

    private final String productId;
    private final String productName;
    private final String batchId;
    private final String origin;
    private final String category; // tipo: fruta, vegetal, carne, etc
    private final double quantity; // quantidade em kg
    private final String unit; // kg, ton, boxes
    private final long productionDate;
    private final long expiryDate;
    
    // Informação da estação atual
    private final String currentStation;
    private final String currentLocation;
    private final String status;
    private final String notes;
    private final double temperature; // temperatura de armazenamento
    private final double price; // preço de venda (só na loja)

    /**
     * Construtor para CRIAÇÃO de produto (produtor)
     */
    public FoodProduct(String productName, String batchId, String category,
                       double quantity, String unit, String origin,
                       long productionDate, long expiryDate) {
        this.productId = generateProductId(productName, batchId, productionDate);
        this.productName = Objects.requireNonNull(productName, "productName");
        this.batchId = Objects.requireNonNull(batchId, "batchId");
        this.category = Objects.requireNonNull(category, "category");
        this.quantity = quantity;
        this.unit = Objects.requireNonNull(unit, "unit");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.productionDate = productionDate;
        this.expiryDate = expiryDate;
        
        // Dados iniciais
        this.currentStation = origin;
        this.currentLocation = origin;
        this.status = "Produzido";
        this.notes = "Produto criado pelo produtor";
        this.temperature = 0.0;
        this.price = 0.0; // sem preço até chegar à loja
    }
    
    /**
     * Construtor para TRANSFERÊNCIA entre estações (sem preço)
     */
    public FoodProduct(String productId, String productName, String batchId,
                       String category, double quantity, String unit,
                       String origin, long productionDate, long expiryDate,
                       String currentStation, String currentLocation,
                       String status, String notes, double temperature) {
        this(productId, productName, batchId, category, quantity, unit, origin,
             productionDate, expiryDate, currentStation, currentLocation,
             status, notes, temperature, 0.0);
    }

    /**
     * Construtor para TRANSFERÊNCIA entre estações (com preço)
     */
    public FoodProduct(String productId, String productName, String batchId,
                       String category, double quantity, String unit,
                       String origin, long productionDate, long expiryDate,
                       String currentStation, String currentLocation,
                       String status, String notes, double temperature, double price) {
        this.productId = Objects.requireNonNull(productId, "productId");
        this.productName = Objects.requireNonNull(productName, "productName");
        this.batchId = Objects.requireNonNull(batchId, "batchId");
        this.category = Objects.requireNonNull(category, "category");
        this.quantity = quantity;
        this.unit = Objects.requireNonNull(unit, "unit");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.productionDate = productionDate;
        this.expiryDate = expiryDate;
        
        this.currentStation = Objects.requireNonNull(currentStation, "currentStation");
        this.currentLocation = Objects.requireNonNull(currentLocation, "currentLocation");
        this.status = Objects.requireNonNull(status, "status");
        this.notes = notes == null ? "" : notes;
        this.temperature = temperature;
        this.price = price;
    }
    
    /**
     * Gera um ID único para o produto baseado no nome, lote e data de produção
     */
    private static String generateProductId(String productName, String batchId, long productionDate) {
        try {
            String data = productName + "|" + batchId + "|" + productionDate;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (Exception e) {
            return "PROD-" + System.currentTimeMillis();
        }
    }
    
    /**
     * Cria uma nova versão do produto com informações atualizadas (para transferência)
     */
    public FoodProduct updateForTransfer(String newStation, String newLocation,
                                         String newStatus, String newNotes, double newTemperature) {
        return new FoodProduct(
            this.productId, this.productName, this.batchId, this.category,
            this.quantity, this.unit, this.origin, this.productionDate, this.expiryDate,
            newStation, newLocation, newStatus, newNotes, newTemperature, this.price
        );
    }

    /**
     * Cria uma nova versão do produto com preço (para loja)
     */
    public FoodProduct updateForTransferWithPrice(String newStation, String newLocation,
                                                   String newStatus, String newNotes, 
                                                   double newTemperature, double newPrice) {
        return new FoodProduct(
            this.productId, this.productName, this.batchId, this.category,
            this.quantity, this.unit, this.origin, this.productionDate, this.expiryDate,
            newStation, newLocation, newStatus, newNotes, newTemperature, newPrice
        );
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getBatchId() {
        return batchId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public String getUnit() {
        return unit;
    }

    public String getOrigin() {
        return origin;
    }
    
    public String getCurrentStation() {
        return currentStation;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }
    
    public double getTemperature() {
        return temperature;
    }

    public double getPrice() {
        return price;
    }

    public long getProductionDate() {
        return productionDate;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder();
        txt.append(productName).append(" [").append(productId).append("]\n");
        txt.append("  Lote: ").append(batchId);
        txt.append(" | Produto: ").append(category);
        txt.append(" | Quantidade: ").append(quantity).append(" ").append(unit).append("\n");
        txt.append("  Origem: ").append(origin);
        txt.append(" | Estação Atual: ").append(currentStation).append("\n");
        txt.append("  Localização: ").append(currentLocation);
        txt.append(" | Status: ").append(status);
        if (temperature != 0.0) {
            txt.append(" | Temp: ").append(temperature).append("°C");
        }
        if (!notes.isEmpty()) {
            txt.append("\n  Notas: ").append(notes);
        }
        return txt.toString();
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    private static final long serialVersionUID = 202601050001L;
    //:::::::::::::::::::::::::::  Copyright(c) 2026  ::::::::::::::::::::::::::
}
