package Termminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {  // Server which catch clients
    static ExecutorService ex = Executors.newFixedThreadPool(2);

    public static void main(String[] args) {

        Database bank = Database.connect_to_DB.get();  // connecting to db

        try(ServerSocket serv = new ServerSocket(5858)) {
            System.out.println("Main server is starting...");
            while (true){

                System.out.println("Waiting for connection...");
                Socket client = serv.accept();  // server accept client

                ex.execute(new Server_thread(bank, client));

            }

        } catch (IOException e) {
            System.err.println("can't create a server!");
        }
    }
}
