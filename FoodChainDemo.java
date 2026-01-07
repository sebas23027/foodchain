package foodchain;

/**
 * Exemplo de como usar o sistema de Food Chain
 */
public class FoodChainDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("FOOD CHAIN - DEMO");
        System.out.println("========================================\n");
        
        try {
            // 1. PRODUTOR CRIA UM PRODUTO
            System.out.println("1. PRODUTOR cria um novo produto\n");
            
            long now = System.currentTimeMillis();
            long expiry = now + 14L * 24 * 60 * 60 * 1000; // 14 dias
            
            FoodProduct tomateProduzido = new FoodProduct(
                "Tomate Bio",           // nome
                "LOTE-2026-001",        // lote
                "Vegetal",              // categoria
                500.0,                  // quantidade
                "kg",                   // unidade
                "Quinta do Produtor",   // origem
                now,                    // data produção
                expiry                  // data validade
            );
            
            System.out.println("Product ID gerado: " + tomateProduzido.getProductId());
            System.out.println(tomateProduzido);
            System.out.println("\n" + "=".repeat(80) + "\n");
            
            // 2. ARMAZÉM RECEBE O PRODUTO
            System.out.println("2. ARMAZÉM recebe e atualiza informações\n");
            
            FoodProduct tomateNoArmazem = tomateProduzido.updateForTransfer(
                "Armazém Central",              // nova estação
                "Lisboa - Armazém A",           // nova localização
                "Armazenado",                   // status
                "Produto recebido e armazenado a 4°C", // notas
                4.0                             // temperatura
            );
            
            System.out.println("Product ID (mesmo): " + tomateNoArmazem.getProductId());
            System.out.println(tomateNoArmazem);
            System.out.println("\n" + "=".repeat(80) + "\n");
            
            // 3. TRANSPORTADOR LEVA O PRODUTO
            System.out.println("3. TRANSPORTADOR leva para supermercado\n");
            
            FoodProduct tomateEmTransporte = tomateNoArmazem.updateForTransfer(
                "Transportadora Rápida",        // nova estação
                "Em rota para Porto",           // nova localização
                "Em trânsito",                  // status
                "Transporte refrigerado ativo", // notas
                5.0                             // temperatura
            );
            
            System.out.println("Product ID (mesmo): " + tomateEmTransporte.getProductId());
            System.out.println(tomateEmTransporte);
            System.out.println("\n" + "=".repeat(80) + "\n");
            
            // 4. SUPERMERCADO RECEBE
            System.out.println("4. SUPERMERCADO recebe o produto\n");
            
            FoodProduct tomateNoSupermercado = tomateEmTransporte.updateForTransfer(
                "Supermercado MegaFresh",       // nova estação
                "Porto - Loja 5",               // nova localização
                "À venda",                      // status
                "Produto à venda na seção de frescos", // notas
                8.0                             // temperatura
            );
            
            System.out.println("Product ID (mesmo): " + tomateNoSupermercado.getProductId());
            System.out.println(tomateNoSupermercado);
            System.out.println("\n" + "=".repeat(80) + "\n");
            
            System.out.println("✓ CADEIA COMPLETA RASTREADA!");
            System.out.println("  Origem: " + tomateNoSupermercado.getOrigin());
            System.out.println("  Destino Atual: " + tomateNoSupermercado.getCurrentStation());
            System.out.println("  Product ID permanece: " + tomateNoSupermercado.getProductId());
            
        } catch (Exception ex) {
            System.err.println("ERRO: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
