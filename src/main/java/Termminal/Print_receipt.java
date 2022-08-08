package Termminal;

import java.awt.*;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Print_receipt implements Runnable{

    private final String info;  // type of operation
    Database db;
    DataInputStream in;
    DataOutputStream out;


    Print_receipt(String a, Database db, DataInputStream in, DataOutputStream out){
        this.db = db;
        this.info = a;
        this.out = out;
        this.in = in;
    }


    @Override
    public void run() {
        Thread.currentThread().setName("Print receipts thread");
        int id = -1;
        String sql = "SELECT Id FROM Payments ORDER BY Id DESC LIMIT 1";
        try {
            PreparedStatement stmt = db.getConnection().prepareStatement(sql);
            ResultSet res = stmt.executeQuery();
            while (res.next()){
                id = res.getInt("Id");
            }
            id++;
            System.out.println(id);
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        String informat = new StringBuilder()
                .append("Payment# " + String.valueOf(id) + "\n")
                .append(info + "\n")
                .toString();

        File dir = new File("Receipts");
        if(!dir.exists())
            dir.mkdir();

        File fl = new File(dir + "\\Payment#" + id + ".txt");

        try(FileWriter fr = new FileWriter(fl)) {
            fr.write(informat);
            fr.flush();
        } catch (IOException e) {
            System.err.println("Receipt is not printed!");
        }finally {

            if(fl.length()>0) {
                try {
                    out.writeUTF("Receipt is printed!");
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Desktop dsk = null;
                if(Desktop.isDesktopSupported())
                    dsk = Desktop.getDesktop();

                try {
                    assert dsk != null;
                    dsk.open(dir);
                    dsk.open(fl);
                } catch (IOException e) {
                    System.err.println("Can't open directory or file!");
                }

            }
        }

    }
}
