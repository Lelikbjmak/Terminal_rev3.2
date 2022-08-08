package Termminal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.function.Supplier;

public class Database {

    // jdbc:mysql://localhost:3306/bank

    String driver = "com.mysql.jdbc.Driver";
    // URL указывает на базу данных для доступа
    String url = "jdbc:mysql://localhost:3306/bank";
    // Имя пользователя при настройке MySQL
    String username = "root";
    // Пароль для настройки MySQL
    String password = "qwertY_1351530";
    Connection connection;
    Statement statement;

    Database(/*String username, String password*/) {
        //this.username = username;
        //this.password = password;
       // this.url = url;


        try {// make a connection with our database
            this.connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            System.err.println("Connection wit DB is not synchronized!");
        }


        try { // statement to execute SQL requests
            this.statement = connection.createStatement();
        } catch (SQLException e) {
            System.out.println("Statement is not created!");
        }


        System.out.println("Database successfully connected!");
    }

    public Connection getConnection() {
        return connection;
    }

    public Statement getStatement() {
        return statement;
    }


    public static Supplier<Database> connect_to_DB = () -> {
        Scanner sc = new Scanner(System.in);

        try {  // download a jdbc driver
            Class.forName("com.mysql.cj.jdbc.Driver");  // connect jdbc driver to use SQL java com,and interpritet like aql request
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver is not connected!");
        }

//        System.out.print("Username: ");
//        String username = sc.nextLine();
//
//        System.out.print("Password: ");
//        String password = sc.nextLine();

//        System.out.print("URL: ");
//        String url = sc.nextLine();

        Database temp = new Database();

        return temp;
    };
}
