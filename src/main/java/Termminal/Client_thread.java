package Termminal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client_thread implements Runnable{

    Socket client;

    Client_thread(){
        try {
            client = new Socket("localhost", 5858);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host!");
        } catch (IOException e) {
            System.err.println("IO error creating socket! Server is not exist!");
        }
    }

    public void registr (DataInputStream in, DataOutputStream out, Scanner sc) throws IOException {

        System.out.print(in.readUTF());
        out.writeUTF(sc.nextLine());  // input full name
        out.flush();

        System.out.print(in.readUTF());
        out.writeUTF(sc.nextLine());  // input passport
        out.flush();

        System.out.print(in.readUTF());
        out.writeUTF(sc.nextLine());  // phone number
        out.flush();

        System.out.print(in.readUTF());
        out.writeUTF(sc.nextLine());  // birthday
        out.flush();


        System.out.println(in.readUTF()); // for sql request -> if successful we add a person to DB

        System.out.print(in.readUTF()); // from currency
        out.writeUTF(sc.nextLine());
        out.flush();

        System.out.println(in.readUTF()); // from password | Currency exception
        String ans = sc.nextLine();
        out.writeUTF(ans); // to solve switch case

        if(ans.equals("1")){
            System.out.println(in.readUTF());
            out.writeUTF(sc.nextLine());
        }else
            if (ans.equals("2")){
                System.out.println(in.readUTF());
            }

        System.out.println("1" + in.readUTF());
        System.out.println("2" + in.readUTF());
       // System.out.println("3" + in.readUTF());

    }


    public void operation(DataInputStream in, DataOutputStream out, Scanner sc) throws IOException {

        System.out.print(in.readUTF());  // from transaction menu
        String ans = sc.nextLine();
        out.writeUTF(ans);  // sent decision about operation
        out.flush();

        System.out.print(in.readUTF());  // password
        out.writeInt(Integer.parseInt(sc.nextLine()));  // send password
        out.flush();

        System.out.println(in.readUTF());
        System.out.println(in.readUTF());

        ans = sc.nextLine();
        out.writeUTF(ans);  // choose type of transaction
        out.flush();

        switch (Integer.parseInt(ans)) {
            case 1 -> Cash_transfer(in, out, sc);
            case 2 -> Convert(in, out, sc);
            case 3 -> bill_info(in);
            case 4 -> Cash_extradition(in, out, sc);
            case 5 -> Deposit(in, out, sc);
        }

    }

    public void Cash_transfer(DataInputStream in, DataOutputStream out, Scanner sc) throws IOException {


        System.out.println(in.readUTF());
        System.out.println(in.readUTF());
        System.out.print(in.readUTF());

        out.writeUTF(sc.nextLine());  // enter recipient card#
        out.flush();

        System.out.print(in.readUTF());
        out.writeUTF(sc.nextLine());  // enter payment
        out.flush();


        System.out.print(in.readUTF());
        out.writeInt(Integer.parseInt(sc.nextLine()));  // password
        out.flush();

        System.out.println("1" + in.readUTF()); // verification
        System.out.println("2" + in.readUTF()); // successful
        System.out.println("3" + in.readUTF()); // print receipt

        String answer = sc.nextLine();
        out.writeUTF(answer);
        out.flush();
        if(answer.equalsIgnoreCase("yes"))
        System.out.println("1" + in.readUTF());  // for receipt print

    }


    public void Convert(DataInputStream in, DataOutputStream out, Scanner sc) throws IOException {

        System.out.println(in.readUTF());  // Convert
        System.out.println(in.readUTF());  // Ledger
        System.out.print(in.readUTF());  // Convert to

        out.writeUTF(sc.nextLine());
        out.flush();  // currency to

        System.out.print(in.readUTF());  // Summa
        out.writeUTF(sc.nextLine());
        out.flush();

        System.out.print(in.readUTF());  // Acception
        out.writeInt(Integer.parseInt(sc.nextLine()));
        out.flush();

        System.out.println("1" + in.readUTF()); // verification
        System.out.println("2" + in.readUTF()); // successful
        System.out.println("3" + in.readUTF()); // print receipt

        String answer = sc.nextLine();
        out.writeUTF(answer);
        out.flush();
        if(answer.equalsIgnoreCase("yes"))
            System.out.println("1" + in.readUTF());  // for receipt print

    }

    public void Cash_extradition(DataInputStream in, DataOutputStream out, Scanner sc) throws IOException {

        System.out.println(in.readUTF()); // ledger
        System.out.print(in.readUTF()); // summa to withdraw
        out.writeUTF(sc.nextLine()); // input summa
        out.flush();

        System.out.print(in.readUTF());  // password
        out.writeInt(Integer.parseInt(sc.nextLine()));  // input password
        out.flush();

        System.out.println(in.readUTF());  // verification
        System.out.println(in.readUTF());   // successful

        System.out.println(in.readUTF()); // print receipt

        String answer = sc.nextLine();
        out.writeUTF(answer);
        out.flush();
        if(answer.equalsIgnoreCase("yes"))
            System.out.println("1" + in.readUTF());  // for receipt print
    }


    public void Deposit(DataInputStream in, DataOutputStream out, Scanner sc) throws IOException {

        System.out.println(in.readUTF());
        System.out.print(in.readUTF()); //Summa to dep
        out.writeUTF(sc.nextLine());  // summa
        out.flush();

        System.out.println(in.readUTF());  // verification
        System.out.println(in.readUTF());   // successful

        System.out.println(in.readUTF()); // print receipt

        String answer = sc.nextLine();
        out.writeUTF(answer);
        out.flush();
        if(answer.equalsIgnoreCase("yes"))
            System.out.println("1" + in.readUTF());  // for receipt print
    }


    public void bill_info(DataInputStream in) throws IOException {

        System.out.println(in.readUTF());
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);

        try(DataOutputStream write = new DataOutputStream(client.getOutputStream());
            DataInputStream read = new DataInputStream(client.getInputStream())) {

            while (!client.isClosed()) {

                System.out.println(read.readUTF());

                int answer = Integer.parseInt(sc.nextLine());
                write.writeInt(answer);
                write.flush();
                switch (answer) {
                    case 1 -> registr(read, write, sc);
                    case 2 -> operation(read, write, sc);
                    case 3 -> client.close();
                    default -> System.err.println("Incorrect operation!");
                }

            }

        } catch (IOException e) {
            System.err.println("Error in creating stream | socket is not connected!");
        }
    }




}
