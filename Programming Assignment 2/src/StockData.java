/**
 * The StockData class represents the data for a single day of trading for a specific stock.
 * This includes information such as the opening and closing price, the highest and lowest
 * price during the day, and the date of the trading day.
 */
public class StockData {
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