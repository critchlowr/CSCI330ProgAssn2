/**
 * NOTE that all code blocks utilize try with resources, introduced in java 9, which will automatically close
 * connections on anything in a try-catch block if it exits normally or with exception.
 *
 */

import java.util.*;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class CritchlowAssignment2 {
    static class StockData {
        String ticker;
        String transDate;
        double openPrice;
        double highPrice;
        double lowPrice;
        double closePrice;

        StockData(String ticker, String transDate, double openPrice, double closePrice, double highPrice, double lowPrice) {
            this.ticker = ticker;
            this.transDate = transDate;
            this.openPrice = openPrice;
            this.highPrice = highPrice;
            this.lowPrice = lowPrice;
            this.closePrice = closePrice;
        }
    }
    static Connection conn;
    static final String prompt = "Enter ticker symbol [start/end dates]: ";



    public static void main(String[] args) throws Exception {
        // Define parameter file. Modify this line to switch between local and remote connections
        //String paramsFile = "ConnectionParameters_LabComputer.txt";
        String paramsFile = "ConnectionParameters_RemoteComputer.txt";

        // If there are arguments passed to the program, consider the first one as a parameter file
        if (args.length >= 1) {
            paramsFile = args[0];
        }

        // Load database connection properties from the file
        Properties connectprops = new Properties();
        connectprops.load(new FileInputStream(paramsFile));

        try {
            // Register JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish the database connection
            String dburl = connectprops.getProperty("dburl");
            conn = DriverManager.getConnection(dburl, connectprops);
            System.out.println("Database connection is established");

            // Begin user interaction
            Scanner in = new Scanner(System.in);
            System.out.print(prompt);
            String input = in.nextLine().trim();

            // Continue until user inputs an empty line
            while (!input.isEmpty()) {
                String[] params = input.split("\\s+");
                String ticker = params[0];

                // Parse optional date parameters if provided
                String startdate = null, enddate = null;
                if (params.length >= 3) {
                    startdate = params[1];
                    enddate = params[2];
                }

                // If ticker is valid, execute the investment strategy
                if (getName(ticker)) {
                    Deque<StockData> data = getStockData(ticker, startdate, enddate);
                    System.out.println("\nExecuting investment strategy");
                    doStrategy(ticker, data);
                }

                // Prompt for the next input
                System.out.println();
                System.out.print(prompt);
                input = in.nextLine().trim();
            }

            // Close the database connection
            conn.close();
            System.out.println("Database connection closed.");
        }
        catch (SQLException ex) {
            // Handle any errors that may occur during the database connection
            conn.close();
            System.out.printf("SQLException: %s%nSQLState: %s%nVendorError: %s%n",
                    ex.getMessage(), ex.getSQLState(), ex.getErrorCode());
        }
    }

    static boolean getName(String ticker) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "select Name from company "
                        + " where Ticker = ?")) {
            pstmt.setString(1, ticker);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.printf(rs.getString(1));
                    System.out.println();
                    return true;
                } else {
                    System.out.printf("%s not found in database.", ticker);
                    return false;
                }
            }
        }
    }

    private static PreparedStatement prepareStatementNoDates(String ticker) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT Ticker, transDate, OpenPrice, HighPrice, LowPrice, ClosePrice, Volume, AdjustedClose"
                        + " FROM pricevolume"
                        + " WHERE Ticker = ?"
                        + " ORDER BY transDate DESC");
        pstmt.setString(1, ticker);
        return pstmt;
    }

    private static PreparedStatement prepareStatementWithDates(String ticker, String start, String end) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT Ticker, transDate, OpenPrice, HighPrice, LowPrice, ClosePrice, Volume, AdjustedClose"
                        + " FROM pricevolume"
                        + " WHERE Ticker = ? AND transDate BETWEEN ? AND ?"
                        + " ORDER BY transDate DESC");
        pstmt.setString(1, ticker);
        pstmt.setString(2, start);
        pstmt.setString(3, end);
        return pstmt;
    }

    private static StockData createStockData(ResultSet rs, String ticker, double splitMultiply) throws SQLException {
        double openPrice = Double.parseDouble(rs.getString("OpenPrice")) / splitMultiply;
        double closePrice = Double.parseDouble(rs.getString("ClosePrice")) / splitMultiply;
        double highPrice = Double.parseDouble(rs.getString("HighPrice")) / splitMultiply;
        double lowPrice = Double.parseDouble(rs.getString("LowPrice")) / splitMultiply;

        return new StockData(ticker, rs.getString("transDate"), openPrice, closePrice, highPrice, lowPrice);
    }

    private static void printSplitInfo(String splitString, ResultSet rs, double previousClose, double currentOpen, double splitMultiply) throws SQLException {
        System.out.printf("%s split on %s %.2f --> %.2f\n",
                splitString,
                rs.getString("transDate"),
                previousClose * splitMultiply,
                currentOpen * splitMultiply);
    }

    // This method queries the 'pricevolume' table, detects stock splits, and returns a Deque of StockData.
    // Additionally, it prints stock splits and adjusts data for splits.
    static Deque<StockData> getStockData(String ticker, String start, String end) throws SQLException {
        Deque<StockData> result = new ArrayDeque<>();
        PreparedStatement pstmt;

        // Prepare SQL statements based on whether start and end dates are provided
        if (start == null || end == null) {
            // If no dates are provided, select all data for the ticker
            pstmt = prepareStatementNoDates(ticker);
        } else {
            // If dates are provided, select data within that date range
            pstmt = prepareStatementWithDates(ticker, start, end);
        }

        // Execute SQL query and process results
        try (pstmt; ResultSet rs = pstmt.executeQuery()) {
            double splitMultiply = 1;
            double currentOpen = 0;
            int count = 0;
            int splitCount = 0;
            double[] splitRatios = {2.0, 3.0, 1.5};
            String[] splitStrings = {"2:1", "3:1", "3:2"};
            double[] splitTolerances = {0.20, 0.30, 0.15};

            // Process first row separately to initialize variables
            if (rs.next()) {
                StockData stockData = createStockData(rs, ticker, splitMultiply);
                result.addFirst(stockData);
                currentOpen = stockData.openPrice;
                count++;
            }

            // Process remaining rows and check for stock splits
            double previousClose;
            double ratio;
            while (rs.next()) {
                previousClose = Double.parseDouble(rs.getString("ClosePrice")) / splitMultiply;
                ratio = previousClose / currentOpen;

                // Check each potential split ratio
                for (int i = 0; i < splitRatios.length; i++) {
                    if (Math.abs(ratio - splitRatios[i]) < splitTolerances[i]) {
                        printSplitInfo(splitStrings[i], rs, previousClose, currentOpen, splitMultiply);
                        splitMultiply *= splitRatios[i];
                        splitCount++;
                        break;
                    }
                }

                // Add row to result after adjusting for splits
                StockData stockData = createStockData(rs, ticker, splitMultiply);
                result.addFirst(stockData);
                currentOpen = stockData.openPrice;
                count++;
            }

            // Print final statistics
            System.out.printf("%d splits in %d trading days\n", splitCount, count);
            return result;
        }
    }

    // This method will execute buy and sell criteria for a given company
    static void doStrategy(String ticker, Deque<StockData> data) {
        int transactionsExecuted = 0;
        double totalCash = 0;

        // Check for adequate data
        if (data.size() < 50) {
            System.out.printf("Transactions executed: %d\nNet Cash: %.2f\n", transactionsExecuted, totalCash);
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
        System.out.printf("Transactions executed: %d\nNet Cash: %.2f\n", transactionsExecuted, totalCash);
    }

}