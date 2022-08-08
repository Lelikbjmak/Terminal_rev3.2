package Termminal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

public class Person {
    private String full_name;
    private String phone_number;
    private String passport_id;
    private String birth_date;

    Person(String full_name, String passport_id, String phone_number, String birth_date){
        this.birth_date = birth_date;
        this.passport_id = passport_id;
        this.full_name = full_name;
        this.phone_number = phone_number;
    }

    public String getFull_name() {
        return full_name;
    }

    public static Person register_new_client(Scanner sc, Database db, DataOutputStream write, DataInputStream read) throws IncorrectDataException, IOException {

        Client_adder adder = Person::new;

        write.writeUTF("<Client register>\nFull name: ");
        write.flush();

        String name = read.readUTF();

        if(!name.matches("\\s?[A-Z][a-z]+(?:[ \\t]*[A-Z]?[a-z]+)?[ \\t]*[A-Z][a-z]+\\b")) {
            throw new IncorrectDataException("Incorrect format of Full name!");
        }


        write.writeUTF("Passport: ");
        String passport = read.readUTF();

        if(!passport.matches("^[A-Z]{2}\\d{7}$"))
            throw new IncorrectDataException("Incorrect format of Passport!");

        write.writeUTF("Phone number: ");
        write.flush();

        String phone = read.readUTF();
//        if(!phone.matches("^\\+?(375)?(80)?(29)?(33)?(44)\\d{7}$"))
//            throw new IncorrectDataException("Incorrect format of Phone number!");

        write.writeUTF("Birthday: ");
        String birthday = read.readUTF();
//        if(!birthday.matches("\\s+(?:19\\d{2}|20[01][0-9]|2020)[-/.](?:0[1-9]|1[012])[-/.](?:0[1-9]|[12][0-9]|3[01])\\b") || !birthday.matches("\\s+(?:0[1-9]|1[012])[-/.](?:0[1-9]|[12][0-9]|3[01])[-/.](?:19\\d{2}|20[01][0-9]|2020)\\b") || !birthday.matches("\\s+(?:0[1-9]|[12][0-9]|3[01])[-/.](?:0[1-9]|1[012])[-/.](?:19\\d{2}|20[01][0-9]|2020)\\b"))
//            throw new IncorrectDataException("Incorrect format of Birthday!");

        String sql = "INSERT INTO Client(Full_name, Phone, Passport_id, Birth) VALUES (?,?,?,?)";


        try {
            PreparedStatement stmt = db.getConnection().prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, passport);
            stmt.setString(4, birthday);

            int rows = stmt.executeUpdate();

            write.writeUTF(Integer.toString(rows) + " has added in table Client!");
            write.flush();
        } catch (SQLException e) {
            write.writeUTF("Can't create statement!");
            write.flush();
        }
        return adder.add_client(name,passport, phone, birthday);

    }
}
