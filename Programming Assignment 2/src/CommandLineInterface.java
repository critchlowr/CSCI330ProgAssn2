import java.util.Scanner;

/**
 * The CommandLineInterface class represents the user interface from entering ticker symbols
 * It will parse out the stock ticker and the start and end dates from the scanner object.
 * All scanner objects will be automatically closed via AutoCloseable in UserInterface
 */
public class CommandLineInterface implements UserInterface {
    private Scanner scanner = new Scanner(System.in);
    private String[] inputs;

    @Override
    public String getTicker() {
        System.out.print("Enter ticker symbol [start/end dates]: ");
        inputs = scanner.nextLine().split(" ");
        return inputs[0];
    }

    @Override
    public String getStartDate() {
        if (inputs.length > 1) {
            return inputs[1];
        }
        return null;
    }

    @Override
    public String getEndDate() {
        if (inputs.length > 2) {
            return inputs[2];
        }
        return null;
    }

    @Override
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}
