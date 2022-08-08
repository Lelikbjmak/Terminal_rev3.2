package Termminal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Bill_reg {

    void reg(Database db, DataOutputStream write, DataInputStream read) throws IOException;
}
