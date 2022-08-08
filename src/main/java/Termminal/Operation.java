package Termminal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;

public class Operation implements Runnable{

    Database db;
    DataOutputStream write;
    DataInputStream read;

    Operation(Database db, DataOutputStream out, DataInputStream in){
        this.db = db;
        this.write = out;
        this.read = in;
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        try {
            Transaction.Operation(sc, Transaction.authorisation(sc, db, write, read),db, write, read);
        } catch (IncorrectDataException e) {
            try {
                write.writeUTF("Incorrect card# or password!");
                write.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        } catch (InterruptedException e) {

            try {
                write.writeUTF("Interrupted!");
                write.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        } catch (IOException e) {
            try {
                write.writeUTF("IO error!");
                write.flush();
            }catch (IOException ex1){
                throw new RuntimeException(ex1);
            }
        } catch (SQLException e) {
            try {
                write.writeUTF("SQL ERROR!");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
