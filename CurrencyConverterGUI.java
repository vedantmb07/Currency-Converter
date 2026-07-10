import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CurrencyConverterGUI extends JFrame {

    private static final String API_URL = "https://open.er-api.com/v6/latest/";

    private static final String[][] CURRENCIES = {
            {"INR", "Indian Rupee", "IN"},
            {"USD", "US Dollar", "US"},
            {"SGD", "Singapore Dollar", "SG"},
            {"JPY", "Japanese Yen", "JP"},
            {"EUR", "Euro", "EU"},
            {"GBP", "British Pound", "GB"},
            {"AUD", "Australian Dollar", "AU"},
            {"CAD", "Canadian Dollar", "CA"},
            {"CHF", "Swiss Franc", "CH"},
            {"CNY", "Chinese Yuan", "CN"},
            {"AED", "UAE Dirham", "AE"},
            {"NZD", "New Zealand Dollar", "NZ"}
    };

    private JTextField amountField;
    private JComboBox<String> fromCurrencyCombo;
    private JComboBox<String> toCurrencyCombo;
    private JLabel fromFlagLabel;
    private JLabel toFlagLabel;
    private JLabel convertedAmountLabel;
    private JLabel conversionRateLabel;
    private JLabel lastUpdatedLabel;
    private JButton swapButton;

    private Map<String, Double> currentRates = new HashMap<>();
    private boolean isFetching = false;
    private String lastFetchedBase = "";
    private String lastUpdatedTime = "";
    private Timer debounceTimer;
    private boolean isSwapping = false;

    public CurrencyConverterGUI() {
        setTitle("Premium Currency Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 580);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 247, 250));

        initUI();
        
        // Initial setup matching the website logic
        fromCurrencyCombo.setSelectedItem("INR - Indian Rupee");
        toCurrencyCombo.setSelectedItem("USD - US Dollar");
        
        updateFlags();
        fetchRatesAndConvert();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        mainPanel.setOpaque(false);

        // Header
        JLabel titleLabel = new JLabel("Currency Converter", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Real-time exchange rates at your fingertips.", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(100, 110, 120));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 35)));

        // Card Panel to mimic the HTML "converter-card"
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 235), 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // --- Amount Group ---
        JPanel amountPanel = new JPanel(new BorderLayout(0, 8));
        amountPanel.setOpaque(false);
        JLabel amountLabel = new JLabel("Amount");
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        amountLabel.setForeground(new Color(60, 70, 80));
        
        amountField = new JTextField("1000");
        amountField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        amountField.setPreferredSize(new Dimension(0, 45));
        amountField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 220), 1, true),
            new EmptyBorder(5, 10, 5, 10)
        ));
        
        amountPanel.add(amountLabel, BorderLayout.NORTH);
        amountPanel.add(amountField, BorderLayout.CENTER);
        
        cardPanel.add(amountPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        // --- Currency Selection ---
        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.X_AXIS));
        selectionPanel.setOpaque(false);

        // From
        JPanel fromPanel = new JPanel(new BorderLayout(0, 8));
        fromPanel.setOpaque(false);
        JLabel fromLabel = new JLabel("From");
        fromLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        fromLabel.setForeground(new Color(60, 70, 80));
        fromPanel.add(fromLabel, BorderLayout.NORTH);

        JPanel fromComboPanel = new JPanel(new BorderLayout(8, 0));
        fromComboPanel.setOpaque(false);
        fromFlagLabel = new JLabel();
        fromFlagLabel.setPreferredSize(new Dimension(32, 32));
        fromCurrencyCombo = new JComboBox<>(getCurrencyOptions());
        fromCurrencyCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        fromCurrencyCombo.setBackground(Color.WHITE);
        
        fromComboPanel.add(fromFlagLabel, BorderLayout.WEST);
        fromComboPanel.add(fromCurrencyCombo, BorderLayout.CENTER);
        fromPanel.add(fromComboPanel, BorderLayout.CENTER);

        // Swap Button
        swapButton = new JButton("<html><p style='font-size:18px; margin-top:-3px;'>&#8644;</p></html>");
        swapButton.setFocusPainted(false);
        swapButton.setBackground(new Color(240, 245, 250));
        swapButton.setForeground(new Color(30, 40, 50));
        swapButton.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 220), 1, true));
        swapButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        swapButton.setPreferredSize(new Dimension(45, 45));
        swapButton.setMaximumSize(new Dimension(45, 45));
        
        JPanel swapPanel = new JPanel(new GridBagLayout()); // to center the button vertically
        swapPanel.setOpaque(false);
        swapPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(24, 0, 0, 0); // push it down to align with selectors
        swapPanel.add(swapButton, gbc);

        // To
        JPanel toPanel = new JPanel(new BorderLayout(0, 8));
        toPanel.setOpaque(false);
        JLabel toLabel = new JLabel("To");
        toLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        toLabel.setForeground(new Color(60, 70, 80));
        toPanel.add(toLabel, BorderLayout.NORTH);

        JPanel toComboPanel = new JPanel(new BorderLayout(8, 0));
        toComboPanel.setOpaque(false);
        toFlagLabel = new JLabel();
        toFlagLabel.setPreferredSize(new Dimension(32, 32));
        toCurrencyCombo = new JComboBox<>(getCurrencyOptions());
        toCurrencyCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        toCurrencyCombo.setBackground(Color.WHITE);
        
        toComboPanel.add(toFlagLabel, BorderLayout.WEST);
        toComboPanel.add(toCurrencyCombo, BorderLayout.CENTER);
        toPanel.add(toComboPanel, BorderLayout.CENTER);

        selectionPanel.add(fromPanel);
        selectionPanel.add(swapPanel);
        selectionPanel.add(toPanel);
        
        cardPanel.add(selectionPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 35)));

        // --- Result Area ---
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setOpaque(false);
        resultPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        convertedAmountLabel = new JLabel("Loading...", SwingConstants.CENTER);
        convertedAmountLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        convertedAmountLabel.setForeground(new Color(40, 110, 200)); // Premium Blue
        convertedAmountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        conversionRateLabel = new JLabel("Please wait while we fetch the rates", SwingConstants.CENTER);
        conversionRateLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        conversionRateLabel.setForeground(new Color(100, 110, 120));
        conversionRateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        resultPanel.add(convertedAmountLabel);
        resultPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        resultPanel.add(conversionRateLabel);
        
        cardPanel.add(resultPanel);
        
        mainPanel.add(cardPanel);
        
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        lastUpdatedLabel = new JLabel("Last updated: ", SwingConstants.CENTER);
        lastUpdatedLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lastUpdatedLabel.setForeground(Color.GRAY);
        lastUpdatedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(lastUpdatedLabel);

        add(mainPanel);

        // --- Events ---
        fromCurrencyCombo.addActionListener(e -> {
            if (isSwapping) return;
            updateFlags();
            fetchRatesAndConvert();
        });

        toCurrencyCombo.addActionListener(e -> {
            if (isSwapping) return;
            updateFlags();
            calculateConversion();
        });

        swapButton.addActionListener(e -> {
            isSwapping = true;
            int fromIdx = fromCurrencyCombo.getSelectedIndex();
            fromCurrencyCombo.setSelectedIndex(toCurrencyCombo.getSelectedIndex());
            toCurrencyCombo.setSelectedIndex(fromIdx);
            isSwapping = false;
            
            updateFlags();
            fetchRatesAndConvert();
        });

        debounceTimer = new Timer(300, e -> calculateConversion());
        debounceTimer.setRepeats(false);

        amountField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { debounceTimer.restart(); }
            public void removeUpdate(DocumentEvent e) { debounceTimer.restart(); }
            public void insertUpdate(DocumentEvent e) { debounceTimer.restart(); }
        });
    }

    private String[] getCurrencyOptions() {
        String[] options = new String[CURRENCIES.length];
        for (int i = 0; i < CURRENCIES.length; i++) {
            options[i] = CURRENCIES[i][0] + " - " + CURRENCIES[i][1];
        }
        return options;
    }

    private String getCurrencyCode(JComboBox<String> combo) {
        String selected = (String) combo.getSelectedItem();
        if (selected != null && selected.length() >= 3) {
            return selected.substring(0, 3);
        }
        return "USD";
    }

    private String getFlagCode(String currencyCode) {
        for (String[] currency : CURRENCIES) {
            if (currency[0].equals(currencyCode)) {
                return currency[2];
            }
        }
        return "US";
    }

    private void updateFlags() {
        String fromCode = getCurrencyCode(fromCurrencyCombo);
        String toCode = getCurrencyCode(toCurrencyCombo);
        
        loadFlagImageAsync(getFlagCode(fromCode), fromFlagLabel);
        loadFlagImageAsync(getFlagCode(toCode), toFlagLabel);
    }

    private void loadFlagImageAsync(String countryCode, JLabel label) {
        label.setIcon(null);
        label.setText("?"); // Show placeholder while fetching
        
        SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                String urlStr = "https://flagsapi.com/" + countryCode + "/flat/32.png";
                try {
                    URL url = new URI(urlStr).toURL();
                    return new ImageIcon(url);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        label.setText("");
                        label.setIcon(icon);
                    }
                } catch (Exception e) {
                    // retain placeholder
                }
            }
        };
        worker.execute();
    }

    private void fetchRatesAndConvert() {
        String baseCurrency = getCurrencyCode(fromCurrencyCombo);
        
        if (baseCurrency.equals(lastFetchedBase) && !currentRates.isEmpty()) {
            calculateConversion();
            return;
        }

        isFetching = true;
        convertedAmountLabel.setText("Loading...");
        conversionRateLabel.setText("Fetching latest rates...");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    URL url = new URI(API_URL + baseCurrency).toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000); // 8 sec timeout in case of slow internet
                    connection.setReadTimeout(8000);

                    if (connection.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();
                        return response.toString();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                isFetching = false;
                try {
                    String json = get();
                    if (json != null && json.contains("\"success\"")) {
                        lastFetchedBase = baseCurrency;
                        currentRates.clear();
                        
                        // Parse rates manually (no external dependency)
                        for (String[] currency : CURRENCIES) {
                            String code = currency[0];
                            String search = "\"" + code + "\":";
                            int index = json.indexOf(search);
                            if (index != -1) {
                                int start = index + search.length();
                                int end = start;
                                while(end < json.length() && (json.charAt(end) == ' ' || json.charAt(end) == ':')) {
                                    start++;
                                    end++;
                                }
                                while (end < json.length() && 
                                      (Character.isDigit(json.charAt(end)) || 
                                       json.charAt(end) == '.' || 
                                       json.charAt(end) == 'e' || 
                                       json.charAt(end) == 'E' || 
                                       json.charAt(end) == '-')) {
                                    end++;
                                }
                                if(start < end) {
                                    try {
                                        double rate = Double.parseDouble(json.substring(start, end));
                                        currentRates.put(code, rate);
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }
                        
                        // Try to find Last Update Time
                        int timeIndex = json.indexOf("\"time_last_update_utc\":\"");
                        if (timeIndex != -1) {
                            int start = timeIndex + 24;
                            int end = json.indexOf("\"", start);
                            if (end != -1) {
                                lastUpdatedTime = json.substring(start, end);
                                lastUpdatedLabel.setText("Last updated: " + lastUpdatedTime);
                            }
                        }
                        
                        calculateConversion();
                    } else {
                        convertedAmountLabel.setText("Error");
                        conversionRateLabel.setText("Failed to parse communication.");
                    }
                } catch (Exception e) {
                    convertedAmountLabel.setText("Network Error");
                    conversionRateLabel.setText("Please check your internet connection.");
                }
            }
        };
        worker.execute();
    }

    private void calculateConversion() {
        if (isFetching || currentRates.isEmpty()) return;

        String amountText = amountField.getText().trim();
        if (amountText.isEmpty()) {
            convertedAmountLabel.setText("0.00");
            conversionRateLabel.setText("Please enter an amount");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (amount < 0) throw new NumberFormatException();

            String toCurrency = getCurrencyCode(toCurrencyCombo);
            String fromCurrency = getCurrencyCode(fromCurrencyCombo);

            if (fromCurrency.equals(toCurrency)) {
                convertedAmountLabel.setText(String.format("%,.2f %s", amount, toCurrency));
                conversionRateLabel.setText(String.format("1 %s = 1.0000 %s", fromCurrency, toCurrency));
                return;
            }

            Double rate = currentRates.get(toCurrency);
            if (rate != null) {
                double convertedAmount = amount * rate;
                double invertedRate = 1 / rate;

                convertedAmountLabel.setText(String.format("%,.2f %s", convertedAmount, toCurrency));
                conversionRateLabel.setText(String.format("<html><center>1 %s = %.4f %s<br>1 %s = %.4f %s</center></html>", 
                        fromCurrency, rate, toCurrency, 
                        toCurrency, invertedRate, fromCurrency));
            } else {
                convertedAmountLabel.setText("N/A");
                conversionRateLabel.setText("Rate not available for " + toCurrency);
            }

        } catch (NumberFormatException e) {
            convertedAmountLabel.setText("0.00");
            conversionRateLabel.setText("Invalid amount entered");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {} // Fallback to cross-platform is fine
            new CurrencyConverterGUI().setVisible(true);
        });
    }
}
