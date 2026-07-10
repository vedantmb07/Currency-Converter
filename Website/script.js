// Supported currencies with their standard symbols
const currencies = {
    "INR": { name: "Indian Rupee", symbol: "₹", flag: "IN" },
    "USD": { name: "US Dollar", symbol: "$", flag: "US" },
    "SGD": { name: "Singapore Dollar", symbol: "S$", flag: "SG" },
    "JPY": { name: "Japanese Yen", symbol: "¥", flag: "JP" },
    "EUR": { name: "Euro", symbol: "€", flag: "EU" }, // Valid flag API code for EU
    "GBP": { name: "British Pound", symbol: "£", flag: "GB" },
    "AUD": { name: "Australian Dollar", symbol: "A$", flag: "AU" },
    "CAD": { name: "Canadian Dollar", symbol: "C$", flag: "CA" },
    "CHF": { name: "Swiss Franc", symbol: "CHF", flag: "CH" },
    "CNY": { name: "Chinese Yuan", symbol: "¥", flag: "CN" },
    "AED": { name: "UAE Dirham", symbol: "د.إ", flag: "AE" },
    "NZD": { name: "New Zealand Dollar", symbol: "NZ$", flag: "NZ" }
};

// DOM Elements
const amountInput = document.getElementById('amount');
const fromCurrencySelect = document.getElementById('from-currency');
const toCurrencySelect = document.getElementById('to-currency');
const fromFlag = document.getElementById('from-flag');
const toFlag = document.getElementById('to-flag');
const fromSymbol = document.getElementById('from-symbol');
const swapBtn = document.getElementById('swap-btn');
const convertedAmountEl = document.getElementById('converted-amount');
const conversionRateEl = document.getElementById('conversion-rate');
const lastUpdatedEl = document.getElementById('last-updated');
const loader = document.getElementById('loader');
const resultContent = document.getElementById('result-content');

// API endpoint (Using a free public endpoint with no key required for basic rates)
// open.er-api.com provides free exchange rates without an API key
const apiUrl = 'https://open.er-api.com/v6/latest/';

let currentRates = {};
let isFetching = false;

// Initialize the application
function init() {
    populateCurrencyDropdowns();

    // Set Default Values (INR to USD)
    fromCurrencySelect.value = 'INR';
    toCurrencySelect.value = 'USD';

    updateFlags();
    updateSymbol();

    // Fetch initial rates
    fetchRatesAndConvert();

    // Event Listeners
    amountInput.addEventListener('input', debounce(calculateConversion, 300));
    fromCurrencySelect.addEventListener('change', () => {
        updateFlags();
        updateSymbol();
        fetchRatesAndConvert();
    });
    toCurrencySelect.addEventListener('change', () => {
        updateFlags();
        calculateConversion();
    });
    swapBtn.addEventListener('click', swapCurrencies);
}

// Populate dropdowns with supported currencies
function populateCurrencyDropdowns() {
    let optionsHTML = '';
    for (const [code, details] of Object.entries(currencies)) {
        optionsHTML += `<option value="${code}">${code} - ${details.name}</option>`;
    }
    fromCurrencySelect.innerHTML = optionsHTML;
    toCurrencySelect.innerHTML = optionsHTML;
}

// Update the flag images based on selection
function updateFlags() {
    const fromCode = fromCurrencySelect.value;
    const toCode = toCurrencySelect.value;

    const fromCountry = currencies[fromCode].flag;
    const toCountry = currencies[toCode].flag;

    fromFlag.src = `https://flagsapi.com/${fromCountry}/flat/32.png`;
    toFlag.src = `https://flagsapi.com/${toCountry}/flat/32.png`;
}

// Update the currency symbol next to the input
function updateSymbol() {
    const fromCode = fromCurrencySelect.value;
    fromSymbol.textContent = currencies[fromCode].symbol;
}

// Swap From and To currencies
function swapCurrencies() {
    const tempValue = fromCurrencySelect.value;
    fromCurrencySelect.value = toCurrencySelect.value;
    toCurrencySelect.value = tempValue;

    // Add a tiny animation clss to the wrapper
    document.getElementById('app-container').classList.add('swapping');
    setTimeout(() => {
        document.getElementById('app-container').classList.remove('swapping');
    }, 300);

    updateFlags();
    updateSymbol();
    fetchRatesAndConvert();
}

// Fetch exchange rates from the API
async function fetchRatesAndConvert() {
    const baseCurrency = fromCurrencySelect.value;

    showLoader(true);
    isFetching = true;

    try {
        const response = await fetch(`${apiUrl}${baseCurrency}`);
        if (!response.ok) throw new Error('Network response was not ok');

        const data = await response.json();
        if (data.result === "success") {
            currentRates = data.rates;

            // Format Last Updated Time
            const date = new Date(data.time_last_update_unix * 1000);
            const formattedDate = date.toLocaleString(undefined, {
                year: 'numeric', month: 'short', day: 'numeric',
                hour: '2-digit', minute: '2-digit'
            });
            lastUpdatedEl.textContent = `Last updated: ${formattedDate}`;

            calculateConversion();
        } else {
            throw new Error('API returned error status');
        }
    } catch (error) {
        console.error("Error fetching exchange rates:", error);
        convertedAmountEl.textContent = "Error";
        conversionRateEl.textContent = "Could not fetch rates. Check your connection.";
    } finally {
        showLoader(false);
        isFetching = false;
    }
}

// Perform the calculation and update the UI
function calculateConversion() {
    if (Object.keys(currentRates).length === 0 || isFetching) return;

    const amount = parseFloat(amountInput.value);
    const toCurrency = toCurrencySelect.value;
    const baseCurrency = fromCurrencySelect.value;

    const rate = currentRates[toCurrency];

    if (isNaN(amount) || amount < 0) {
        convertedAmountEl.textContent = "0.00";
        conversionRateEl.textContent = "Please enter a valid amount";
        return;
    }

    const convertedVal = amount * rate;
    const invertedRate = 1 / rate;

    // Format the number beautifully
    const formattedAmount = new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency: toCurrency,
        maximumFractionDigits: 2
    }).format(convertedVal);

    const formattedRate = new Intl.NumberFormat(undefined, {
        maximumFractionDigits: 4
    }).format(rate);

    const formattedInvertedRate = new Intl.NumberFormat(undefined, {
        maximumFractionDigits: 4
    }).format(invertedRate);

    convertedAmountEl.textContent = formattedAmount;
    conversionRateEl.innerHTML = `1 ${baseCurrency} = ${formattedRate} ${toCurrency} <br> 1 ${toCurrency} = ${formattedInvertedRate} ${baseCurrency}`;
}

// UI Helpers
function showLoader(show) {
    if (show) {
        loader.classList.add('active');
        resultContent.classList.add('hidden');
    } else {
        loader.classList.remove('active');
        resultContent.classList.remove('hidden');
    }
}

// Debounce function to limit calculation frequency on quick inputs
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Run app
document.addEventListener('DOMContentLoaded', init);
