# CSCI330ProgAssn2
Stock Trading Simulation

This is a simple console-based stock trading simulation program implemented in Java. It allows users to execute a trading strategy based on historical stock data from a MySQL database.

Features

UserInterface: Interactively asks users for a ticker symbol and optional start and end dates.
DatabaseInterface: Connects to a MySQL database to fetch historical stock price data.
TradingStrategy: Simulates a trading strategy that executes buys and sells based on certain criteria.

Usage

On running the program, you will be asked to enter a ticker symbol and optionally start and end dates. The trading strategy will then be executed based on the historical stock data for the given ticker symbol from the database.

ConnectionParameters_RemoteComputer.txt should have the following format:

dburl=jdbc:mysql://IPaddress:port/dbname
user=
password=

It should be stored in the root directory right before src. 

Acknowledgements

Thanks to OpenAI for the GPT-4 model that helped in cleanly formatting the project.
And I guess thanks to Koerber for all the SQL skills that weren't used in this project.
