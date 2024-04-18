public class OpenFile {
    File file;
    char mode; // 'I' for input, 'O' for output, 'U' for update
    int position; // Pointer to the next byte to be read or written

    public OpenFile(File file, char mode) {
        this.file = file;
        this.mode = mode;
        this.position = 0; // Default to start of file
    }
}