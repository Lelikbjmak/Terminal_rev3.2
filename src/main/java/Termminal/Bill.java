package Termminal;

import java.awt.*;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Bill {
    private final Person client;  // person who owns bill
    private final Currency currency;  // currency of bill
    private double ledger;  // current ledger
    private final String bill_id;  // Card number
    private Map<Currency, Double> cash;    // Balance for ur cash savings
    private final int password;  // pin code of card

    Bill(Person client, Currency currency, int password, String bill_id, double ledger){
        this.client = client;
        this.currency = currency;
        this.password = password;
        this.bill_id = bill_id;
        this.ledger = ledger;
        this.cash = new HashMap<>() {{
            put(Currency.BYN, Double.valueOf(0));
            put(Currency.USD, Double.valueOf(0));
            put(Currency.EURO, Double.valueOf(0));
            put(Currency.RUB, Double.valueOf(0));
        }};
    }


    public Person getClient() {
        return client;
    }

    public String getBill_id() {
        return bill_id;
    }

    public double getLedger() {
        return ledger;
    }

    public int getPassword() {
        return password;
    }

    public void setLedger(double ledger) {
        this.ledger = ledger;
    }

    public String Print_ledger(){
        String formattedDouble = new DecimalFormat("#0.00").format(ledger);
        formattedDouble = formattedDouble.concat(" " + currency.toString());
        return formattedDouble;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Map<Currency, Double> getCash() {
        return cash;
    }

    public static Bill add_client(Database db, DataOutputStream o, DataInputStream i) throws IOException, SQLException {
        Scanner sc = new Scanner(System.in);
        Savepoint savepoint = db.getConnection().setSavepoint("save_before_reg"); // make savepoint if catch EX rollback
        db.getConnection().setAutoCommit(false);
        Person client1 = null;
        try {
            client1 = Person.register_new_client(sc, db, o, i);
        } catch (IncorrectDataException e) {
            o.writeUTF(e.getMessage());
        } catch (IOException e) {
            o.writeUTF("Can't supply info to server!");
        }

        o.writeUTF("Currency: ");
        Currency currency1 = null;
        String currency_1 = i.readUTF().toUpperCase();
        // if(Arrays.stream(Currency.values()).anyMatch(Currency.valueOf(currency_1)))
        try {
            if (Stream.of(Currency.values()).anyMatch((p) -> p.name().equals(currency_1.toUpperCase())))
                currency1 = Currency.valueOf(currency_1.toUpperCase());
            else
                throw new IncorrectDataException("Incorrect choice of currency!");
        }catch (IncorrectDataException ex1){
            o.writeUTF(ex1.getMessage());
        }


        int password = -1;
        o.writeUTF("1>>Create your own password\n2>>Obtain password in envelope");

        switch (Integer.parseInt(i.readUTF())) {
            case 1 -> password = Bill.create_password(o, i);
            case 2 -> {
                Callable<Integer> callable = () -> {
                    return Bill.obtain_password.get();
                };
                ExecutorService ex = Executors.newFixedThreadPool(1);
                Future<Integer> future = ex.submit(callable);
                try {
                    Thread.sleep(500);
                    password = future.get();
                } catch (InterruptedException e) {
                    o.writeUTF("Registration is interrupted!");
                } catch (ExecutionException e) {
                    o.writeUTF("Can't implement register!");
                } finally {
                    if (future.isDone())
                        o.writeUTF("Password printed!");
                    ex.shutdown();
                }
            }
            default -> o.writeUTF("Incorrect password choice!");
        }

        String id = (Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000_000_000L, 9_999_999_999_999_999L))).replaceAll("(.{4})", "$1 ").trim();
        //double value = Math.random()*(3000 - 1000) + 1000;

        String sql = "INSERT INTO bill(card, person_id, currency, password) VALUES (?,?,?,?)";

        try {
            PreparedStatement stmt = db.getConnection().prepareStatement(sql);
            PreparedStatement stmt1 = db.getConnection().prepareStatement("SELECT ID FROM Client WHERE Full_name LIKE ? ");
            stmt1.setString(1, client1.getFull_name());

            ResultSet res = stmt1.executeQuery();
            int person = -1;

                while (res.next()) {
                    person = res.getInt("ID");
                }
            if (person == -1) {
                o.writeUTF("Can't find a person! ");
                o.flush();
            }

            stmt.setString(1, id);
            stmt.setInt(2, person);
            stmt.setString(3, currency1.toString());
            stmt.setInt(4, password);

            stmt.executeUpdate();
            stmt1.close();

            stmt.clearParameters();

            stmt = db.getConnection().prepareStatement("INSERT INTO Cash(client_id, currency) VALUES (?,?) ");
            for (Currency cr: Currency.values()) {
                stmt.setInt(1, person);
                stmt.setString(2, cr.toString());
                stmt.addBatch();
            }

            stmt.executeBatch();
            stmt.close();
            db.getConnection().commit();
            db.getConnection().setAutoCommit(true);
            o.writeUTF("bill has added to database!");
            o.flush();

        } catch (SQLException e) {
            System.err.println("Can't create statement!");
        }

        return new Bill(client1, currency1, password, id, 0);
    };



    public static int create_password (DataOutputStream out, DataInputStream in) throws IOException {
        out.writeUTF("Password: ");
        out.flush();
        int password = Integer.parseInt(in.readUTF());
        return password;
    };


    public static Supplier<Integer> obtain_password = () -> {

        int password = (int)(Math.random()*(10000 - 1000) + 1000);

        File password_file = new File("#Password#.txt");
        try {
            password_file.createNewFile();
        } catch (IOException e) {}

        try(FileOutputStream fos = new FileOutputStream(password_file)) {
            byte[] buff = String.valueOf(password).getBytes();
            fos.write(buff,0, buff.length);

            Desktop dsk;
            if(Desktop.isDesktopSupported()) {
                dsk = Desktop.getDesktop();
                dsk.open(password_file);
            }

        } catch (IOException e) {
            System.err.println("Can't open directory or file!");
        }
        finally {
            password_file.deleteOnExit();
        }
        return password;
    };

    public static Bill_reg reg = (db, write, read) ->{
        Bill temp = null;
        try {
            temp = add_client (db, write, read);
        } catch (SQLException e) {
            write.writeUTF("Can;t create a savepoint!");
            write.flush();
        }
        write.writeUTF("Card number: " + temp.bill_id);
        write.flush();
    };


    public static String input_card_number(Scanner sc, Database db, DataOutputStream write, DataInputStream read) throws IncorrectDataException, IOException {

        String str = null;

        try{
            str = read.readUTF().trim();
            if (!(str.matches("(\\d{4}\\s?){4}"))){
                throw new IncorrectDataException("Incorrect format of card number!");
            }
        }catch (IncorrectDataException | IOException ex){
            write.writeUTF(ex.getMessage());
            write.flush();
            System.exit(0);
        }

        try {
            String sql = "SELECT * from bill WHERE card = ?";
            PreparedStatement stmt = db.getConnection().prepareStatement(sql);
            stmt.setString(1, str);
            ResultSet res = stmt.executeQuery();

                if(!res.next()) {
                    stmt.close();
                    throw new IncorrectDataException("User is not found!");
                }
                else {
                    stmt.close();
                    return str;
                }

        }catch (IncorrectDataException ex1){
            write.writeUTF(ex1.getMessage());
            write.flush();
        } catch (SQLException e) {
            write.writeUTF("Incorrect SQL syntax!");
            write.flush();
        }


        return  null;
    }// for input card number


    public static boolean input_password(Scanner sc, String bill, Database db, DataOutputStream write, DataInputStream read) throws IOException {

        int pass = 0;
        write.writeUTF("Password: ");
        write.flush();
        int password = read.readInt();  // obtain password from user

        try{
            String sql = "SELECT password FROM bill WHERE card = ?";
            PreparedStatement stmt = db.getConnection().prepareStatement(sql);
            stmt.setString(1, bill);

            ResultSet res = stmt.executeQuery();
            while (res.next()){
                pass = res.getInt("password");
            }

            if (password != pass){
                throw new IncorrectDataException("Invalid password!");
            }

            stmt.close();

        }catch (NumberFormatException ex1){
            write.writeUTF("Incorrect format of password!");
            write.flush();
            System.exit(0);
        }catch (IncorrectDataException ex2){
            write.writeUTF(ex2.getMessage());
            write.flush();
            System.exit(0);
        } catch (SQLException e) {
            write.writeUTF("SQL Error!");
            write.flush();
        }

        return true;
    }  // for input_password




    public static String bill_search(Scanner sc, Database db, DataOutputStream write, DataInputStream read) throws IOException {

        write.writeUTF("Card# ");
        write.flush();

        String temp = null;
        try {
            temp = Bill.input_card_number(sc, db, write, read);
        } catch(IncorrectDataException ex){
            System.exit(0);
        }

        if(Bill.input_password(sc, temp, db, write, read)) {

            return temp;
        }

        return null;
    } // for bill search





}

