import java.util.ArrayList;
import java.util.List;

abstract class File {
    protected String name;  // Name of the file, up to 9 characters
    protected char type;    // 'D' for directory, 'U' for user data, 'F' for free/unused
    protected int firstBlock;  // Points to the first block of the file or directory
    protected int size;     // Number of bytes used in the last block of the file for U, unused for D

    public File(String name, char type, int firstBlock) {
        this.name = name;
        this.type = type;
        this.firstBlock = firstBlock;
        this.size = 0;  // Initialized as 0, will be set accordingly later
    }

    public abstract void addFile(File file);
    public abstract File getFile(String name);
}