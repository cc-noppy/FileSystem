import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

abstract class File {
    protected byte[] name = new byte[9];
    protected char type;    // 'D' for directory, 'U' for user data, 'F' for free/unused
    protected int firstBlock;
    protected int size;     // Number of bytes used in the last block of the file for U, unused for D

    public File(String name, char type, int firstBlock) {
        byte[] byteName = name.getBytes(StandardCharsets.UTF_8);
        int length = Math.min(byteName.length, 9);

        System.arraycopy(byteName,0,this.name,0, length);

        this.type = type;
        this.firstBlock = firstBlock;
        this.size = 0;
    }
}