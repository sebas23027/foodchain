package foodchain;

/**
 * Utilitário para trabalhar com FoodTransaction
 * Anteriormente suportava conversão de sistema legacy
 */
public class TransactionConverter {
    
    /**
     * Verifica se uma string Base64 é uma FoodTransaction
     */
    public static String getTransactionType(String base64Transaction) {
        try {
            Object obj = utils.Utils.base64ToObject(base64Transaction);
            if (obj instanceof FoodTransaction) {
                return "FoodTransaction";
            } else {
                return "Unknown";
            }
        } catch (Exception e) {
            return "Error";
        }
    }
    
    /**
     * Extrai transação FoodTransaction de Base64
     */
    public static Object extractTransaction(String base64Transaction) {
        try {
            Object obj = utils.Utils.base64ToObject(base64Transaction);
            if (obj instanceof FoodTransaction) {
                return obj;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
