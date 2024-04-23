public class OpenFile {
    int sector;
    char mode; // 'I' for input, 'O' for output, 'U' for update
    String name;
    int pointerBase;
    int pointerOffset;


    public OpenFile(int sector, char mode, String name) {
        this.sector = sector;
        this.mode = mode;
        this.name = name;
        this.pointerBase = 0;
        this.pointerOffset = 0;
    }
}