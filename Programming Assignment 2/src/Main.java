class Main {
    public static void main(String[] args) {
        // Define parameter file. Modify this line to switch between local and remote connections
        // String paramsFile = "ConnectionParameters_LabComputer.txt";
        String paramsFile = "ConnectionParameters_RemoteComputer.txt";

        // If there are arguments passed to the program, consider the first one as a parameter file
        if (args.length >= 1) {
            paramsFile = args[0];
        }

        // Create the UserInterface and DatabaseManager
        try (UserInterface ui = new CommandLineInterface();
             DatabaseInterface db = new MySQLDatabaseManager(paramsFile)){

            // Create and execute the TradingStrategy
            TradingStrategy strategy = new TradingStrategy(ui, db);
            strategy.execute();
        } catch (Exception e) {
            System.out.println("Encounter error during trading strategy: " + e.getMessage());
        }
    }
}