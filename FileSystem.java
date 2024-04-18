import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FileSystem {
    private final Disk disk;
    public Directory root;
    public OpenFile lastOpenedFile;


    public FileSystem(Disk disk) { //initialized the disk, the openFiles map, and the root directory!
        this.disk = disk;

        root = new Directory("root", 0); // allocates the root directory!
        serializeDirectory(root, 0);
    }

    public void create(String path, char fileType) {
        String[] parts = path.split("/");
        String name = parts[parts.length - 1];

        Directory currentDirectory = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].isEmpty()) continue; // Skip empty parts from paths like "/dir/"

            File subDir = currentDirectory.getFile(parts[i]);
            if (subDir instanceof Directory) {
                currentDirectory = (Directory) subDir;
            } else {
                System.out.println("Invalid path: " + parts[i] + " is not a directory.");
                return;
            }
        }

        File existing = currentDirectory.getFile(name);
        if (existing != null) {
            System.out.println("A file already exists with the name: " + name + " in the path: " + path);
            return;
        }

        int blockIndex = findFreeBlock();
        if (blockIndex == -1) {
            System.out.println("No free blocks available to allocate.");
            return;
        }

        if (fileType == 'D') { // If the file type is a directory
            Directory newDirectory = new Directory(name, blockIndex);
            currentDirectory.addFile(newDirectory);
            serializeDirectory(newDirectory, blockIndex); // Serialize new directory structure to disk

        } else if (fileType == 'U') { // If the file type is a user data file
            UserData newUserFile = new UserData(name, blockIndex);
            currentDirectory.addFile(newUserFile);
            serializeUserData(newUserFile, blockIndex); // Initialize and serialize user data file to disk
        }
    }

    public void open(String path, char mode) {
        String[] parts = path.split("/");
        String fileName = parts[parts.length - 1];
        Directory currentDirectory = root;

        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].isEmpty() || parts[i].equals(".")) continue;
            File file = currentDirectory.getFile(parts[i]);
            if (file instanceof Directory) {
                currentDirectory = (Directory) file;
            } else {
                System.out.println("Path is invalid, " + parts[i] + " is not a directory.");
                return;
            }
        }

        File targetFile = currentDirectory.getFile(fileName);
        if (targetFile == null) {
            System.out.println("File does not exist: " + path);
            return;
        }

        if (targetFile instanceof UserData) {
            OpenFile openFile = new OpenFile(targetFile, mode);
            if (mode == 'O') {
                openFile.position = targetFile.size; // Position at the end for output mode
            } else {
                openFile.position = 0; // Position at the start for input and update mode
            }

            lastOpenedFile = openFile;

            System.out.println(fileName + " has been opened!");

        } else {
            System.out.println("Cannot open directories or invalid file types for reading or writing.");
        }
    }

    public void close() {
        System.out.println("You closed " + lastOpenedFile.file.name);
        lastOpenedFile = null;
    }

    public void delete(String name) {

    }

    public void read(int n) {
        if (lastOpenedFile == null || (lastOpenedFile.mode != 'I' && lastOpenedFile.mode != 'U')) {
            System.out.println("No file is open for reading or is not opened in correct mode.");
            return;
        }

        UserData userDataFile = (UserData) lastOpenedFile.file;
        int currentPosition = lastOpenedFile.position;
        int fileSize = userDataFile.size;

        if (currentPosition >= fileSize) {
            System.out.println("End of file reached. No more data to read.");
            return;
        }

        int bytesToRead = Math.min(n, fileSize - currentPosition);
        byte[] data = new byte[bytesToRead]; // Buffer to store data to be read

        int bytesRead = 0;
        while (bytesRead < bytesToRead) {
            int blockIndex = (currentPosition + bytesRead) / Disk.SECTOR_SIZE;
            int blockOffset = (currentPosition + bytesRead) % Disk.SECTOR_SIZE;
            byte[] blockData = disk.diskRead(userDataFile.firstBlock + blockIndex);

            if (isSectorEmpty(blockData)) {
                System.out.println("Read from an empty sector at block index: " + (userDataFile.firstBlock + blockIndex));
            }

            int bytesInBlock = Math.min(bytesToRead - bytesRead, Disk.SECTOR_SIZE - blockOffset);
            System.arraycopy(blockData, blockOffset, data, bytesRead, bytesInBlock);

            bytesRead += bytesInBlock;
        }

        lastOpenedFile.position += bytesRead; // Update file pointer

        // Display the read data
        System.out.write(data, 0, bytesRead);
        System.out.flush();

        if (bytesRead < n) {
            System.out.println("\nEnd of file reached after reading " + bytesRead + " bytes.");
        }
    }

    public void write(int n, byte[] data) throws UnsupportedEncodingException {
        if (lastOpenedFile == null || lastOpenedFile.mode == 'I') {
            System.out.println("No file is open for writing or the last opened file is in read mode.");
            return;
        }

        UserData userDataFile = (UserData) lastOpenedFile.file;

        if (data.length < n) {
            data = Arrays.copyOf(data, n); // Extend data array to n bytes
            Arrays.fill(data, data.length, n, (byte) ' '); // Fill with spaces if data is shorter than n
        }

        writeToUserData(userDataFile, data, lastOpenedFile.position, n);
        lastOpenedFile.position += n; // Update the file pointer

        // Update the file size in metadata
        int newFileSize = lastOpenedFile.position;
        if (newFileSize > userDataFile.size) {
            userDataFile.size = newFileSize; // Update the size if it has increased
            serializeUserData(userDataFile, userDataFile.firstBlock); // Persist changes to the disk

            System.out.println("You printed '" + new String(data, StandardCharsets.US_ASCII) + "' onto " + lastOpenedFile.file.name);
        }
    }

    public void seek(String name, int position) {

    }

    private int findFreeBlock() {
        return disk.allocateBlock();
    }

    private void writeToUserData(UserData userData, byte[] data, int position, int writeBytes) {
        int sectorSize = Disk.SECTOR_SIZE - 8; // Assuming 8 bytes are used for block management
        int offset = position % sectorSize;
        int currentBlockIndex = position / sectorSize;
        int dataIndex = 0;
        int remainingData = writeBytes;

        // Navigate to the correct block or allocate new ones if necessary
        UserData.Block currentBlock = userData.getOrCreateHead(disk, sectorSize);
        while (currentBlockIndex > 0) {
            if (currentBlock.next == null) {
                currentBlock.next = new UserData.Block(disk.allocateBlock());
            }
            currentBlock = currentBlock.next;
            currentBlockIndex--;
        }

        // Write data across blocks
        while (remainingData > 0) {
            int writeLength = Math.min(sectorSize - offset, remainingData);
            System.arraycopy(data, dataIndex, currentBlock.data, offset, writeLength);

            disk.diskWrite(currentBlock.blockIndex, currentBlock.data);

            dataIndex += writeLength;
            remainingData -= writeLength;
            offset = 0; // Reset offset for next blocks

            if (remainingData > 0) {
                if (currentBlock.next == null) {
                    currentBlock.next = new UserData.Block(disk.allocateBlock());
                }
                currentBlock = currentBlock.next;
            }
        }
    }

    private void serializeDirectory(Directory directory, int blockIndex) {
        // Assume serialization logic to disk for a directory
        // This would convert directory metadata and file entries to bytes and write to disk
        byte[] data = convertDirectoryToBytes(directory);
        disk.diskWrite(blockIndex, data);
    }

    private void serializeUserData(UserData userData, int blockIndex) {
        byte[] data = new byte[Disk.SECTOR_SIZE]; // Simple example, real implementation needed
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putInt(userData.size); // Example: store the size at the start of the sector
        disk.diskWrite(blockIndex, buffer.array());
    }

    private byte[] convertDirectoryToBytes(Directory directory) {
        // Convert directory details to bytes; for example, list of files, metadata, etc.
        ByteBuffer buffer = ByteBuffer.allocate(Disk.SECTOR_SIZE);
        for (File file : directory.getFiles()) {
            if (buffer.remaining() < 16) break; // Check space for entry
            buffer.put((byte)(file instanceof Directory ? 'D' : 'U'));
            byte[] nameBytes = file.name.getBytes();
            buffer.put(nameBytes);
            buffer.position(buffer.position() + 10 - nameBytes.length); // Pad the name to 10 bytes
            buffer.putInt(file.firstBlock);
            buffer.putInt(file.size);
        }
        return buffer.array();
    }

    private boolean isSectorEmpty(byte[] sectorData) {
//        for (byte b : sectorData) {
//            if (b != 0) return false; // If any byte is not zero, the sector isn't empty
//        }

        return sectorData[20] == 0; // All bytes are zero, sector is empty
    }

    public void printFileTree() {
        printDirectory(root, ""); // Start from the root with no indentation
    }

    private void printDirectory(Directory dir, String indent) {
        System.out.println(indent + dir.name + "/"); // Print the directory name
        for (File file : dir.getFiles()) {
            if (file instanceof Directory) {
                printDirectory((Directory) file, indent + "    "); // Recursively print subdirectories
            } else {
                System.out.println(indent + "    " + file.name); // Print file name
            }
        }
    }
}
