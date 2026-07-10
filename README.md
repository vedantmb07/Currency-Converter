# Premium Currency Converter

A premium, fast, and interactive Currency Converter application providing multiple interfaces for maximum flexibility. It supports various global currencies and calculates conversions using live, real-time exchange rates.

## Features
- **Real-Time Exchange Rates**: Fetches live rates instantly via the [ExchangeRate-API](https://open.er-api.com).
- **Multiple Implementations**:
  - **Web Version**: Built with HTML, CSS, and vanilla JavaScript (`index.html`, `script.js`).
  - **Java GUI Version**: A premium standalone desktop application built with Java Swing (`CurrencyConverterGUI.java`) that visually matches the web design.
  - **Java CLI Version**: A simple command-line interface version (`CurrencyConverter.java`).
- **Dynamic Assets**: Automatically pulls high-quality country flags directly from the [FlagsAPI](https://flagsapi.com).
- **Responsive & Modern Design**: A clean layout prioritizing user experience with easy-to-read rates and conversions.

## How to Run

### Java GUI Version
To launch the desktop application version:
1. Ensure you have the Java Development Kit (JDK) installed.
2. Open your terminal in the project directory.
3. Compile the Java file:
   ```bash
   javac CurrencyConverterGUI.java
   ```
4. Run the application:
   ```bash
   java CurrencyConverterGUI
   ```

### Web Version
Simply open `index.html` in your favorite web browser (Chrome, Edge, Firefox, Safari) by double-clicking the file! No server setup is required.