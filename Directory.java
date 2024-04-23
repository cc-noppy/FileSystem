import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Directory extends File {
    public int back;
    public int forward;
    public int free;
    public int filler;
    public byte[] data = new byte[496];

    public Directory(String name, int firstBlock) {
        super(name, 'D', firstBlock);
        this.back = -1;
        this.forward = -1;
        this.free = this.firstBlock;
        this.filler = -1;
    }
}