import java.sql.*;
import java.util.Deque;

public interface DatabaseInterface extends AutoCloseable {
    boolean getName(String ticker) throws SQLException;
    Deque<StockData> getStockData(String ticker, String start, String end) throws SQLException;
    void close() throws SQLException;
}

