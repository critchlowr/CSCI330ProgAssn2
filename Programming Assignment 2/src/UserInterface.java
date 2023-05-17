public interface UserInterface extends AutoCloseable {
    String getTicker();
    String getStartDate();
    String getEndDate();
}