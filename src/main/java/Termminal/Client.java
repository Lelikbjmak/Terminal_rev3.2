package Termminal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    public static void main(String[] args) {

        ExecutorService exec = Executors.newFixedThreadPool(1); // threads of clients!

        exec.execute(new Client_thread());

        exec.shutdown();
    }


}

