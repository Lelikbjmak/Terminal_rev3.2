package Termminal;

import java.io.*;

public class Client_register implements Runnable{
    Database db;
    DataOutputStream write;
    DataInputStream read;

    Client_register(Database db, DataOutputStream write, DataInputStream read){
        this.db = db;
        this.write = write;
        this.read = read;
    }

    @Override
    public void run() {
        try {
            Bill.reg.reg(db, write, read);
        } catch (IOException e) {

        }
    }
}
