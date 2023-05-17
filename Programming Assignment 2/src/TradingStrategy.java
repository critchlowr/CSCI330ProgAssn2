import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The TradingStrategy class is responsible for managing and executing a specific
 * investment strategy. It interacts with the database and the user through the
 * DatabaseManager and UserInterface interfaces respectively.
 */
public class TradingStrategy {
    private UserInterface ui;
    private DatabaseInterface db;

    TradingStrategy(UserInterface ui, DatabaseInterface db) {
        this.ui = ui;
        this.db = db;
    }

    /**
     * The execute method retrieves input from the user and runs the investment strategy
     * for each valid ticker symbol. Although not necessary, the start and end dates for the data to be used in
     * the strategy are also provided by the user.
     */
    void execute() {
            String ticker = ui.getTicker();
            String startdate = ui.getStartDate();
            String enddate = ui.getEndDate();
            while (!ticker.isEmpty()) {
            // If ticker is valid, execute the investment strategy
            try {
                if (db.getName(ticker)) {
                    Deque<StockData> data = db.getStockData(ticker, startdate, enddate);
                    System.out.println("\nExecuting investment strategy");
                    doStrategy(data);
                }
            } catch (SQLException e) {
                System.out.println("An error occurred while executing the trading strategy: " + e.getMessage());
            }
            // re-initialize values for next loop
            ticker = ui.getTicker();
            startdate = ui.getStartDate();
            enddate = ui.getEndDate();
        }
    }

    /**
     * Executes a trading strategy on the provided Deque of StockData.
     * Buys or sells stocks based on a 50-day moving average strategy, and prints the number of transactions and net cash.
     * Modifies the passed Deque by removing elements from the front.
     * Requires a Deque with at least 50 elements; otherwise, it returns without performing any transactions.
     */
    void doStrategy(Deque<StockData> data) {
        int transactionsExecuted = 0;
        double totalCash = 0;

        // Check for adequate data
        if (data.size() < 50) {
            System.out.printf("Transactions executed: %d\nNet Cash: %.2f\n\n", transactionsExecuted, totalCash);
            return;
        }

        // Initialize variables
        Deque<Double> previousDays = new ArrayDeque<>(); // Last 50 days of closing prices
        StockData stockData = null;
        double runningTotal = 0.0; // Sum of last 50 closing prices
        int totalShares = 0;
        boolean readyToBuy = false;

        // Populate previousDays and runningTotal with first 50 days
        for (int i = 0; i < 50; i++) {
            double closePrice = data.pop().closePrice;
            previousDays.addLast(closePrice);
            runningTotal += closePrice;
        }

        // Initialize loop variables
        double openPrice;
        double closePrice;
        double averagePrice;

        // Trading logic
        while (!data.isEmpty()) {
            stockData = data.pop();
            openPrice = stockData.openPrice;
            closePrice = stockData.closePrice;
            averagePrice = runningTotal / 50;

            // Execute buy if flagged on previous day
            if (readyToBuy) {
                totalShares += 100; // give yourself some shares. You deserve it.
                totalCash -= ((100 * openPrice) + 8); // Execute buy
                readyToBuy = false;
                transactionsExecuted++;
            }

            // Check buying and selling conditions
            if (closePrice < averagePrice && (closePrice / openPrice) < 0.97000001) {
                readyToBuy = true; // Flag buy for next day
            } else if (totalShares >= 100 && openPrice > averagePrice && (openPrice / previousDays.peekLast()) > 1.00999999) {
                totalShares -= 100;
                totalCash += (100 * ((openPrice + closePrice) / 2) - 8); // Execute sell
                transactionsExecuted++;
            }

            // Update runningTotal and previousDays
            runningTotal = runningTotal - previousDays.pop() + closePrice;
            previousDays.addLast(closePrice);
        }

        // Sell remaining shares if any
        if (totalShares > 0) {
            totalCash += totalShares * stockData.openPrice;
            transactionsExecuted++;
        }
        System.out.printf("Transactions executed: %d\nNet Cash: %.2f\n\n", transactionsExecuted, totalCash);
    }
}
