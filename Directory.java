import java.util.ArrayList;
import java.util.List;

class Directory extends File {
    private List<File> files;  // Contains either Directory or UserData instances

    public Directory(String name, int firstBlock) {
        super(name, 'D', firstBlock);
        files = new ArrayList<>();
    }

    public List<File> getFiles() {
        return this.files;
    }

    @Override
    public void addFile(File file) {
        this.files.add(file);
    }

    public void removeFile(String name){
        files.removeIf(f -> f.name.equals(name));
    }

    @Override
    public File getFile(String name) {
        for (File file : files) {
            if (file.name.equals(name)) {
                return file;
            }
        }
        return null;  // File not found
    }
}