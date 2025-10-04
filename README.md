README.md:

# Splitwise Clone - Java Swing + MySQL

A desktop application built in **Java** using **Swing** for GUI and **MySQL** as the backend database, inspired by the popular expense-sharing app Splitwise. Manage group expenses, track balances, and calculate settlements easily.

## Features

- Add, view, and delete members
- Add, view, and delete expenses with multiple participants
- Automatic calculation of each member’s balance
- Settlements showing who owes whom
- Simple and interactive GUI with Java Swing
- MySQL database backend with proper foreign key constraints

## Screenshots

![Members Tab](<img width="1321" height="881" alt="image" src="https://github.com/user-attachments/assets/e8a0a654-34a1-4203-b4c9-14a0d228c380" />
)  
![Expenses Tab](<img width="1308" height="891" alt="image" src="https://github.com/user-attachments/assets/a056bee9-735c-478b-8d86-439ded66d568" />
)  
![Balances Tab](<img width="1358" height="891" alt="image" src="https://github.com/user-attachments/assets/d49808e2-280b-4e45-a685-a2c3d8138736" />
)

## Prerequisites

- Java JDK 8 or above
- MySQL Server
- MySQL JDBC Driver (Connector/J)

## Database Setup

1. Create the database:

```sql
CREATE DATABASE splitwise_db;


The application will automatically create the following tables if they do not exist:

members

expenses

expense_shares

Make sure to update the DatabaseManager credentials in the code:

private static final String USER = "root";
private static final String PASSWORD = "your_mysql_password";

How to Run

Clone the repository:

git clone https://github.com/yourusername/Splitwise-Java.git
cd Splitwise-Java


Compile the code:

javac Splitwise.java


Run the application:

java Splitwise

Project Structure
Splitwise-Java/
│
├─ Splitwise.java         # Main GUI and app logic
└─ README.md

Future Enhancements

Export balances to CSV or PDF

Email notifications for settlements

Advanced reporting for groups

Mobile version using JavaFX or Kotlin
