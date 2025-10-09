# Splitwise Mini Project

A simplified version of the popular **Splitwise** application built in **Java** using **Swing** for the GUI and **JDBC** for MySQL database connectivity.

## Features

- Add members and expenses.
- Track individual and group expenses.
- Calculate balances for each member.
- User-friendly **Swing GUI** interface.
- Stores data in a **MySQL database**.

## Technologies Used

- **Java SE** 
- **Swing** for GUI
- **JDBC** for database connectivity
- **MySQL** for backend database


## Setup Instructions

1. **Clone the repository**
```bash
git clone https://github.com/harshh-2/Splitwise-mini.git
cd Splitwise-mini
Install Java and MySQL

Java JDK 17+ recommended.

MySQL Server installed and running.

Add MySQL Connector

Place mysql-connector-java-X.X.X.jar in the lib folder.

Update the classpath when compiling/running:

powershell
# Compile
javac -cp ".;lib\mysql-connector-java-9.3.0.jar" Splitwise.java

# Run
java -cp ".;lib\mysql-connector-java-9.3.0.jar" Splitwise
Configure Database

Create a MySQL database (e.g., splitwise_db).

Update DB credentials in Splitwise.java:

java
Connection con = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/splitwise_db", "root", "your_password");

Usage
Launch the application → GUI window opens.

Add members and expenses via the interface.

View balances and settle debts.
