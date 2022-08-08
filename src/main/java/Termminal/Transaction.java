package Termminal;

import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Date;
import java.util.Scanner;

public class Transaction {

    public static void Operation(Scanner sc, String bill,  Database db, DataOutputStream write, DataInputStream read) throws IncorrectDataException, InterruptedException, IOException, SQLException {

        Thread.sleep(750);

        write.writeUTF("\nChoose the operation: \n1>>Cash Transfer(P2P).\n2>>Convert.\n3>>Ledger.\n4>>Cash extradition.\n5>>Card deposit.\n6>>Exit.");
        write.flush();

        int operation = 0;

        try {
            operation = Integer.parseInt(read.readUTF());
        } catch (NumberFormatException ex) {
           write.writeUTF(ex.getMessage() + "Incorrect format of operation code! Must be a digit!");
           write.flush();
        }


        switch (operation) {
            case 1:
                Thread t1 = new Thread(new Print_receipt(Transaction.Cash_transfer(bill, sc, db, write, read), db, read, write));
                write.writeUTF("Print receipt?");
                write.flush();
                if (read.readUTF().equalsIgnoreCase("yes")) {
                    t1.start();
                    t1.join();
                }
                else break;
                break;
            case 2:
                Thread t2 = new Thread(new Print_receipt(Transaction.Convert(bill, db, sc, write, read), db, read, write));
                write.writeUTF("Print receipt?");
                write.flush();
                if (read.readUTF().equalsIgnoreCase("yes")) {
                    t2.start();
                    t2.join();
                }
                break;
            case 3:
                Transaction.bill_info(bill, db, write);
                break;
            case 4:
                Thread t3 = new Thread(new Print_receipt(Cash_extradition(sc, bill, db, write, read), db, read, write));
                write.writeUTF("Print receipt?");
                write.flush();
                if (read.readUTF().equalsIgnoreCase("yes")) {
                    t3.start();
                    t3.join();
                }
                break;
            case 5:
                Thread t4 = new Thread(new Print_receipt(Card_deposit(bill, db, write, read), db, read, write));
                write.writeUTF("Print receipt?");
                write.flush();
                if (read.readUTF().equalsIgnoreCase("yes")) {
                    t4.start();
                    t4.join();
                }
                break;
            case 6:
                return;
            default: {
                write.writeUTF("Operation not found!");
                write.flush();
                break;
            }
        }

    }

    public static String authorisation(Scanner sc, Database db, DataOutputStream out, DataInputStream in) throws IncorrectDataException, IOException {

        String temp = null;
        try {
            temp = Bill.bill_search(sc, db, out, in);
        } catch (IOException e) {
            out.writeUTF("Can't find a user!");
            out.flush();
        }

        if(temp!=null) {
            out.writeUTF("Welcome!\n");
            out.flush();
            return temp;
        }

        return null;
    }


    public static String Cash_transfer(String bill, Scanner sc, Database db, DataOutputStream out, DataInputStream in) throws IncorrectDataException, IOException, SQLException {

        out.writeUTF("\n#Cash Transfer#");
        out.flush();

        try {
            PreparedStatement stm = db.getConnection().prepareStatement("SELECT Ledger, currency FROM bill WHERE card = ?");
            stm.setString(1, bill);
            ResultSet res = stm.executeQuery();
            while (res.next()){
                out.writeUTF("Ledger: " + res.getDouble("Ledger") + " " + res.getString("currency"));
                out.flush();
            }
            stm.close();

        } catch (SQLException e) {
            out.writeUTF("SQL Syntax error!");
            out.flush();
        }

        out.writeUTF("Recipient card#: ");
        out.flush();

        String bill_to = null;
        try {
            bill_to = Bill.input_card_number(sc, db, out, in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        out.writeUTF("Payment: ");
        out.flush();

        double value_to = Double.parseDouble(in.readUTF());
        double value_from = value_to;

        if(Bill.input_password(sc, bill, db,out, in))
        {
            try(PreparedStatement st = db.getConnection().prepareStatement("SELECT currency FROM bill WHERE card = ?")) {

                st.setString(1, bill);
                ResultSet res = st.executeQuery();
                String curr_from = null;
                while (res.next())
                     curr_from = res.getString("currency").toUpperCase();

                st.clearParameters();
                st.setString(1, bill_to);
                ResultSet res1 = st.executeQuery();
                String curr_to = null;
                while (res1.next())
                        curr_to = res1.getString("currency").toUpperCase();


                if(!curr_from.equalsIgnoreCase(curr_to)) { // means that currency isn't equal
                    value_to =  value_to * Currency_rate.valueOf(curr_from + "_to_" + curr_to).rate;
                }

            } catch (SQLException e) {
                out.writeUTF("SQL Syntax error! Currency is not found!");
                out.flush();
            }

            Savepoint save = db.getConnection().setSavepoint("save1");  // make save in order to rollback if transaction failed

            try {
                db.getConnection().setAutoCommit(false);
                // make a transaction
                String sql_1 = "UPDATE bill SET Ledger = Ledger + (?) WHERE card = (?)";
                PreparedStatement sttm = db.getConnection().prepareStatement(sql_1);
                sttm.setDouble(1, value_to);
                sttm.setString(2, bill_to);
                sttm.executeUpdate();
                sttm.close();

                String sql_2 = "UPDATE bill SET Ledger = Ledger - (?) WHERE card = (?)";
                PreparedStatement sttm2 = db.getConnection().prepareStatement(sql_2);
                sttm2.setDouble(1, value_from);
                sttm2.setString(2, bill);
                sttm2.executeUpdate();
                sttm2.close();
                db.getConnection().commit();

            } catch (SQLException ex) {
                out.writeUTF("SQL syntax error. Transaction is not accomplished (money send)");
                out.flush();
                db.getConnection().rollback(save);
            }

            db.getConnection().setAutoCommit(true);

            out.writeUTF("Verification...");
            out.flush();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            out.writeUTF("Successful!");
            out.flush();

        }


        String currency = null;
        try {
            PreparedStatement stmt1 = db.getConnection().prepareStatement("SELECT Currency FROM bill WHERE card = ?");
            stmt1.setString(1, bill);
            ResultSet res = stmt1.executeQuery();
            while (res.next()){
                currency = res.getString("currency");
            }

            stmt1.close();
        } catch (SQLException e) {
            out.writeUTF("SQL Syntax error! Can't find a bill from currency!");
            out.flush();
        }


        try{
            PreparedStatement st = db.getConnection().prepareStatement("INSERT INTO Payments (Operation_type, Person_1, Person_2, Summa) VALUES (?,?,?,?)");
            st.setString(1, "Cash Transfer");
            st.setString(2, bill);
            st.setString(3, bill_to);
            st.setDouble(4, value_from);

            st.executeUpdate();
            st.close();

        } catch (SQLException e) {
            out.writeUTF("SQL syntax ex! Can't input a operation value in DB!");
            out.flush();
        }
        String inf = new StringBuilder()
                .append("Operation: Cash transfer.\n")
                .append("Sender card# " + bill + "\n")
                .append("Recipient card# " + bill_to +"\n")
                .append("Summa: " + value_from + " " + currency + "\n")
                .append(new Date())
                .toString();

        return inf;
    }


    public static String Convert(String bill, Database db, Scanner sc, DataOutputStream out, DataInputStream in) throws IncorrectDataException, IOException, SQLException {

        out.writeUTF("#Convert#");
        out.flush();

        try {
            PreparedStatement stm = db.getConnection().prepareStatement("SELECT Ledger, currency FROM bill WHERE card = ?");
            stm.setString(1, bill);
            ResultSet res = stm.executeQuery();
            while (res.next()){
                out.writeUTF("Ledger: " + res.getDouble("Ledger") + " " + res.getString("currency"));
                out.flush();
            }
            stm.close();

        } catch (SQLException e) {
            out.writeUTF("SQL Syntax error!");
            out.flush();
        }

        out.writeUTF("Convert currency: ");
        out.flush();

        String curr_from = null;
        try(PreparedStatement st = db.getConnection().prepareStatement("SELECT currency FROM bill WHERE card = (?)")) {
            st.setString(1, bill);
            ResultSet res = st.executeQuery();
            while (res.next())
                curr_from = res.getString("currency").toUpperCase();
        } catch (SQLException e) {
            out.writeUTF("SQL syntax error! Convert find Currency of bill!");
        }

        String currency_to = in.readUTF().toUpperCase();
        Currency_rate to = Currency_rate.valueOf(curr_from + "_to_" + currency_to);
        System.out.println(to.toString());
        out.writeUTF("Summa of convert: ");
        out.flush();

        double value = Double.parseDouble(in.readUTF());

        if(Bill.input_password(sc, bill, db, out, in)){
            Savepoint save = db.getConnection().setSavepoint("save1");  // make save in order to rollback if transaction failed
            try {
                db.getConnection().setAutoCommit(false);
                // make a transaction

                String sql_2 = "UPDATE bill SET Ledger = Ledger - (?) WHERE card = (?)";
                PreparedStatement sttm2 = db.getConnection().prepareStatement(sql_2);
                sttm2.setDouble(1, value);
                sttm2.setString(2, bill);
                sttm2.executeUpdate();

                sttm2.clearParameters();

                String sql_3 = "UPDATE Cash SET balance = balance + (?) WHERE client_id = (SELECT person_id FROM bill WHERE card = (?)) AND currency = (?)";
                sttm2 = db.getConnection().prepareStatement(sql_3);
                sttm2.setDouble(1, value * to.rate);
                sttm2.setString(2, bill);
                sttm2.setString(3, currency_to);
                sttm2.executeUpdate();
                sttm2.close();

                db.getConnection().commit();

            } catch (SQLException ex) {
                out.writeUTF("SQL syntax error. Transaction is not accomplished (money send)");
                out.flush();
                db.getConnection().rollback(save);
            }

            db.getConnection().setAutoCommit(true);

            out.writeUTF("Verification...");
            out.flush();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            out.writeUTF("Successful!");
            out.flush();

            try{
                PreparedStatement st = db.getConnection().prepareStatement("INSERT INTO Payments (Operation_type, Person_1, Summa) VALUES (?,?,?)");
                st.setString(1, "Convert");
                st.setString(2, bill);
                st.setDouble(3, value);

                st.executeUpdate();
                st.close();

            } catch (SQLException e) {
                out.writeUTF("SQL syntax ex! Can't input a operation value in DB!");
                out.flush();
            }

        }
        String info = new StringBuilder()
                .append("Type: Convert\n")
                .append("Consumer card# " + bill + "\n")
                .append("Summa: " + value + " " + curr_from + "\n")
                .append(new Date())
                .toString();

        return info;

    }

    public static void bill_info(String bill, Database db, DataOutputStream out) throws IOException {
        String sql = "select B.card, B.Ledger, B.currency " +
                "From bill as B WHERE B.card = ?";

        try {
            PreparedStatement st = db.getConnection().prepareStatement(sql);
            st.setString(1, bill);
            ResultSet res = st.executeQuery();
            while (res.next()){
                out.writeUTF("\nCard# " + res.getString("B.card") + "\nLedger: " + String.valueOf(res.getDouble("B.Ledger")) + " " + res.getString("B.currency"));
                out.flush();
            }
            st.close();

        } catch (SQLException e) {
            out.writeUTF("SQL exception!");
        }
    }


    public static String Cash_extradition(Scanner sc, String bill, Database db, DataOutputStream out, DataInputStream in) throws IncorrectDataException, IOException, SQLException {

        double current_ledger = 0;
        String currency = null;

        try(PreparedStatement st = db.getConnection().prepareStatement("SELECT Ledger, currency FROM bill WHERE card = (?)")){
            st.setString(1, bill);
            ResultSet res = st.executeQuery();
            while (res.next()){
                current_ledger = res.getDouble("Ledger");
                currency = res.getString("currency");
                out.writeUTF("Ledger: " + String.valueOf(current_ledger) + " " + currency);
                out.flush();
            }
        } catch (SQLException e) {
            out.writeUTF("SQL Syntax error! Cash extradition!");
            out.flush();
        }

        out.writeUTF("Summa to withdraw: ");
        out.flush();

        double cash = Double.parseDouble(in.readUTF());

        if(cash < 0){
            throw new IncorrectDataException("Incorrect format! Summa to withdraw can't be below zero!");
        } else if (cash > current_ledger) {
            throw new IncorrectDataException("Not enough ledger!");
        }else {
            if(Bill.input_password(sc, bill, db, out, in)) {

                Savepoint savepoint = db.getConnection().setSavepoint("savepoint_extradition_money");

                try {

                    PreparedStatement st = db.getConnection().prepareStatement("UPDATE bill SET Ledger = Ledger - (?) WHERE card = (?)");
                    db.getConnection().setAutoCommit(false);

                    st.setDouble(1, cash);
                    st.setString(2, bill);
                    st.executeUpdate();

                    st.clearParameters();

                    st = db.getConnection().prepareStatement("UPDATE Cash SET balance = balance + (?) WHERE client_id = (SELECT person_id FROM bill WHERE card = (?)) AND currency = (?)");
                    st.setDouble(1, cash);
                    st.setString(2, bill);
                    st.setString(3, currency);
                    st.executeUpdate();

                    db.getConnection().commit();
                    db.getConnection().setAutoCommit(true);
                    out.writeUTF("Verification...");
                    out.flush();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    out.writeUTF("Successful!");
                    out.flush();

                    try{
                        PreparedStatement stm = db.getConnection().prepareStatement("INSERT INTO Payments (Operation_type, Person_1, Summa) VALUES (?,?,?)");
                        stm.setString(1, "Money extradition");
                        stm.setString(2, bill);
                        stm.setDouble(3, cash);

                        stm.executeUpdate();
                        stm.close();

                    } catch (SQLException e) {
                        out.writeUTF("SQL syntax ex! Can't input a operation value in DB!");
                        out.flush();
                    }

                } catch (SQLException e) {
                    out.writeUTF("SQL Syntax error! Rollback!");
                    out.flush();
                    db.getConnection().rollback(savepoint);
                }
            }

            }

        String info = new StringBuilder()
                .append("Type: Money extradition\n")
                .append("Consumer card# " + bill + "\n")
                .append("Summa: " + cash + " " +currency  + "\n")
                .append(new Date())
                .toString();

        return info;

        }



    public static String Card_deposit(String bill, Database db, DataOutputStream out, DataInputStream in) throws IncorrectDataException, IOException {

        out.writeUTF("Summa to dep: ");
        out.flush();

        double value = Double.parseDouble(in.readUTF());
        if(value < 0){
            throw new IncorrectDataException("Incorrect format of deposit money!");
        }

        String currency = null;
        try(PreparedStatement st = db.getConnection().prepareStatement("SELECT currency FROM bill WHERE card = (?)")){
            st.setString(1, bill);
            ResultSet res = st.executeQuery();
            while (res.next()){
                currency = res.getString("currency");
            }

        } catch (SQLException e) {
            out.writeUTF("SQL Syntax error!");
            out.flush();
        }
        try(PreparedStatement st = db.getConnection().prepareStatement("UPDATE bill SET Ledger = Ledger + (?) WHERE card = (?)")){
            st.setDouble(1, value);
            st.setString(2, bill);
            st.executeUpdate();

        } catch (SQLException e) {
            out.writeUTF("SQL Syntax error!");
            out.flush();
        }

        try{
            PreparedStatement st = db.getConnection().prepareStatement("INSERT INTO Payments (Operation_type, Person_1, Summa) VALUES (?,?,?)");
            st.setString(1, "Deposit");
            st.setString(2, bill);
            st.setDouble(3, value);

            st.executeUpdate();
            st.close();

            out.writeUTF("Verification...");
            out.flush();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            out.writeUTF("Successful!");
            out.flush();

        } catch (SQLException e) {
            out.writeUTF("SQL syntax ex! Can't input a operation value in DB!");
            out.flush();
        }

        String info = new StringBuilder()
                .append("Type: Deposit\n")
                .append("Consumer card# " + bill + "\n")
                .append("Summa: " + value + " " + currency + "\n")
                .append(new Date())
                .toString();

        return info;

    }

}

