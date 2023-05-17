import java.sql.*;
import java.util.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The MySQLDatabaseManager class is responsible for interacting with the database.
 * This includes establishing the connection, querying the database for data,
 * and closing the connection when it is no longer needed.
 * All conn objects will be automatically closed via AutoCloseable in DatabaseInterface.
 */
class MySQLDatabaseManager implements DatabaseInterface {
    private Connection conn;
    private final double[] splitRatios = {2.0, 3.0, 1.5};
    private final String[] splitStrings = {"2:1", "3:1", "3:2"};
    private final double[] splitTolerances = {0.20, 0.30, 0.15};
    MySQLDatabaseManager(String paramsFile) throws SQLException, ClassNotFoundException, IOException {
        // Load database connection properties from the file
        Properties connectprops = new Properties();
        try (FileInputStream fis = new FileInputStream(paramsFile)) {
            connectprops.load(fis);
        }

        // Register JDBC driver
        Class.forName("com.mysql.cj.jdbc.Driver");

        // Establish the database connection
        String dburl=connectprops.getProperty("dburl");
        conn=DriverManager.getConnection(dburl,connectprops);
        System.out.println("Database connection is established");
    }

    // check if the input ticker exists in the database
    @Override
    public boolean getName(String ticker) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "select Name from company "
                        + " where Ticker = ?")) {
            pstmt.setString(1, ticker);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println(rs.getString(1));
                    return true;
                } else {
                    System.out.printf("%s not found in database.\n\n", ticker);
                    return false;
                }
            }
        }
    }

    // helper method for getStockData to establish a prepared statement without dates
    private PreparedStatement prepareStatementNoDates(String ticker) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT Ticker, transDate, OpenPrice, HighPrice, LowPrice, ClosePrice, Volume, AdjustedClose"
                        + " FROM pricevolume"
                        + " WHERE Ticker = ?"
                        + " ORDER BY transDate DESC");
        pstmt.setString(1, ticker);
        return pstmt;
    }

    // helper method for getStockData to establish a prepared statement with dates
    private PreparedStatement prepareStatementWithDates(String ticker, String start, String end) throws SQLException {
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

    // helper method for getStockData to create StockData objects with the appropriate splitMultiply added to the value
    private static StockData createStockData(ResultSet rs, String ticker, double splitMultiply) throws SQLException {
        double openPrice = Double.parseDouble(rs.getString("OpenPrice")) / splitMultiply;
        double closePrice = Double.parseDouble(rs.getString("ClosePrice")) / splitMultiply;
        double highPrice = Double.parseDouble(rs.getString("HighPrice")) / splitMultiply;
        double lowPrice = Double.parseDouble(rs.getString("LowPrice")) / splitMultiply;

        return new StockData(ticker, rs.getString("transDate"), openPrice, closePrice, highPrice, lowPrice);
    }

    // helper method to print split information with original previousClose price and currentOpen price
    private static void printSplitInfo(String splitString, ResultSet rs, double previousClose, double currentOpen, double splitMultiply) throws SQLException {
        System.out.printf("%s split on %s %.2f --> %.2f\n",
                splitString,
                rs.getString("transDate"),
                previousClose * splitMultiply,
                currentOpen * splitMultiply);
    }

    /**
     * Retrieves stock data from the database for a given ticker within a specified date range.
     * The method detects stock splits, adjusts data for splits, and returns a Deque of StockData objects.
    */
     @Override
    public Deque<StockData> getStockData(String ticker, String start, String end) throws SQLException {
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

    @Override
    public void close() throws SQLException {
        if (conn != null) {
            conn.close();
            System.out.println("Database connection closed.\n");
        }
    }
}
