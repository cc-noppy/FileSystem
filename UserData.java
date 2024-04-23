import java.util.Arrays;

public class UserData extends File {
    public static final int DATA_SIZE = 504;
    public int forward;
    public int back;
    public byte[] data = new byte[DATA_SIZE];  // Actual user data, max size of 504 bytes

    public UserData(String name, int firstBlock) {
        super(name, 'U', firstBlock);
        this.back = -1;
        this.forward = -1;
    }

}