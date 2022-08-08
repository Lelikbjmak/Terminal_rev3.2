package Termminal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Server_thread implements Runnable{
    Database db;
    Socket client;
    Server_thread(Database db, Socket client){
        this.db = db;
        this.client = client;
    }

    @Override
    public void run() {

        try(  DataOutputStream write = new DataOutputStream(client.getOutputStream());
              DataInputStream read = new DataInputStream(client.getInputStream());) {
            System.out.println("\nClient " + client.getLocalAddress() + " " + client.getLocalPort() + " has connected!");

            while (!client.isClosed()){


                write.writeUTF("\n#Menu#\nChoose the operation:\n1>>Register a new client.\n2>>Accomplish Operation.\n3>>Exit.");
                write.flush();

                int ans = read.readInt();

                switch (ans){
                    case 1: {
                        if(!client.isClosed()) {
                            Thread reg = new Thread(new Client_register(db, write, read));
                            reg.start();
                            try {
                                reg.join();
                            } catch (InterruptedException e) {
                                System.err.println("Can't register new client! (Check Thread od client registration!)");
                            }
                            break;
                        }
                        else { System.out.println("Client " + client.getLocalAddress() + " has disconnected!");
                            client.close(); break;}
                    }
                    case 2:{
                        if(!client.isClosed()) {
                            Thread oper = new Thread(new Operation(db, write, read));
                            oper.start();
                            try {
                                oper.join();
                            } catch (InterruptedException e) {
                                System.err.println("Can't accomplish the operation!");
                            }
                            break;
                        }else {
                            System.out.println("Client " + client.getLocalAddress() + " has disconnected!");
                            client.close();
                            break;
                        }
                    }
                    case 3: {
                        System.out.println("Client " + client.getLocalAddress() + " has disconnected!");
                        client.close();
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error in IO stream creating | Socket is closed!");
        }


    }

}
