import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class CurrencyConverter {

    private static final String API_URL = "https://open.er-api.com/v6/latest/";

    private static final String[] SUPPORTED_CURRENCIES = {
        "INR", "USD", "SGD", "JPY", "EUR", "GBP", "AUD", "CAD", "CHF", "CNY", "AED", "NZD"
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("===========================================");
        System.out.println("             Currency Converter            ");
        System.out.println("===========================================");
        
        System.out.println("Supported Currencies:");
        for (int i = 0; i < SUPPORTED_CURRENCIES.length; i++) {
            System.out.print(SUPPORTED_CURRENCIES[i] + (i < SUPPORTED_CURRENCIES.length - 1 ? ", " : ""));
        }
        System.out.println("\n");

        System.out.print("Enter from currency (e.g., INR): ");
        String baseCurrency = scanner.nextLine().trim().toUpperCase();

        System.out.print("Enter to currency (e.g., USD): ");
        String targetCurrency = scanner.nextLine().trim().toUpperCase();

        System.out.print("Enter amount: ");
        double amount = 0;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount entered. Exiting...");
            return;
        }

        if (amount < 0) {
            System.out.println("Amount cannot be negative. Exiting...");
            return;
        }

        System.out.println("\nFetching exchange rates...");
        double rate = fetchExchangeRate(baseCurrency, targetCurrency);
        
        if (rate != -1) {
            double convertedAmount = amount * rate;
            System.out.println("===========================================");
            System.out.printf("Converted Amount: %.2f %s\n", convertedAmount, targetCurrency);
            System.out.println("-------------------------------------------");
            System.out.printf("Conversion Rate: 1 %s = %.4f %s\n", baseCurrency, rate, targetCurrency);
            System.out.printf("Inverse Rate:    1 %s = %.4f %s\n", targetCurrency, (1 / rate), baseCurrency);
            System.out.println("===========================================");
        } else {
            System.out.println("Failed to fetch exchange rate or currency not supported.");
        }
        
        scanner.close();
    }

    private static double fetchExchangeRate(String baseCurrency, String targetCurrency) {
        if (baseCurrency.equals(targetCurrency)) {
            return 1.0;
        }
        try {
            URL url = new java.net.URI(API_URL + baseCurrency).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String jsonResponse = response.toString();
                
                // Extracting the rate for the target currency without an external JSON library
                String targetSearch = "\"" + targetCurrency + "\":";
                int targetIndex = jsonResponse.indexOf(targetSearch);
                
                if (targetIndex != -1) {
                    int startIndex = targetIndex + targetSearch.length();
                    
                    // Skip any whitespace characters
                    while (startIndex < jsonResponse.length() && Character.isWhitespace(jsonResponse.charAt(startIndex))) {
                        startIndex++;
                    }
                    
                    int endIndex = startIndex;
                    while (endIndex < jsonResponse.length() && 
                           (Character.isDigit(jsonResponse.charAt(endIndex)) || 
                            jsonResponse.charAt(endIndex) == '.' || 
                            jsonResponse.charAt(endIndex) == 'e' || 
                            jsonResponse.charAt(endIndex) == 'E' || 
                            jsonResponse.charAt(endIndex) == '-')) {
                        endIndex++;
                    }
                    
                    if (startIndex < endIndex) {
                        String rateString = jsonResponse.substring(startIndex, endIndex);
                        return Double.parseDouble(rateString);
                    }
                }
            } else {
                System.out.println("API request failed. HTTP response code: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Network error: " + e.getMessage());
        }
        return -1;
    }
}
