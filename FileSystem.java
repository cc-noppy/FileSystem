import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

// TODO : updated dictionary sizes everytime you update them!, Fix the user file system

public class FileSystem {
    private final Disk disk;
    public Directory root;
    public OpenFile lastOpenedFile;


    public FileSystem(Disk disk) { //initialized the disk, the openFiles map, and the root directory!
        this.disk = disk;

        root = new Directory("root", disk.allocateBlock()); // allocates the root directory!
        serializeDirectory(root, 0);
    }

    public void create(String path, char fileType) {
        String[] parts = path.split("/");
        String name = parts[parts.length - 1];

        Directory currentDirectory = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].isEmpty()) continue; // Skip root
            boolean found = false;

            byte[] subDirName = new byte[9];
            byte[] dirPart = parts[i].getBytes();
            System.arraycopy(dirPart, 0, subDirName, 0, Math.min(dirPart.length, 9));

            int blockIndex = currentDirectory.firstBlock;
            nameLoop:
            while (blockIndex != -1) {
                byte[] diskDir = disk.diskRead(blockIndex);
                List<byte[]> dirEntries = deserializeDirectoryData(diskDir);

                for (byte[] entry : dirEntries) {
                    byte[] byteName = new byte[9];
                    System.arraycopy(entry, 1, byteName, 0, 9);

                    byte[] sector = new byte[4];
                    System.arraycopy(entry, 10, sector, 0, 4);

                    if (Arrays.equals(subDirName, byteName)) {
                        found = true;
                        currentDirectory = deserializeDirectory(bytesToInt(sector), bytesToString(byteName));

                        break nameLoop;
                    } else {
                        blockIndex = currentDirectory.forward;
                    }
                }
            }

            if (!found) {
                System.out.println("Sub-directory or path not found.");
                return;
            }
        }

        int freeIndex = findFreeBlock();
        if (freeIndex == -1) {
            System.out.println("No free blocks available to allocate.");
            return;
        }

        if (fileType == 'D') {
            Directory newDirectory = new Directory(name, freeIndex);
            serializeDirectory(newDirectory, freeIndex);
            updateDirectoryEntries(currentDirectory, 'D', name, freeIndex, (short) 0);

            System.out.println("Allocated at sector: " + freeIndex);

            byte[] diskDir = disk.diskRead(freeIndex);
//            System.out.println("directory initialization: " + Arrays.toString(diskDir));

        } else if (fileType == 'U') {
            UserData newUserFile = new UserData(name, freeIndex);
            serializeUserData(newUserFile, freeIndex);
            updateDirectoryEntries(currentDirectory, 'U', name, freeIndex, (short) 0);

            System.out.println("Allocated at sector: " + freeIndex);

            byte[] diskDir = disk.diskRead(freeIndex);
//            System.out.println("user file initialization: " + Arrays.toString(diskDir));
        }
    }

    public void open(String path, char mode) {
        if (!(mode == 'I' | mode == 'O' | mode == 'U')) {
            System.out.println("Mode is not valid!");
            return;
        }

        String[] parts = path.split("/");
        String name = parts[parts.length - 1];

        Directory currentDirectory = deserializeDirectory(0, "root");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].isEmpty()) continue; // Skip root
            boolean found = false;

            byte[] subDirName = new byte[9];
            byte[] dirPart = parts[i].getBytes();
            System.arraycopy(dirPart, 0, subDirName, 0, Math.min(dirPart.length, 9));

            int blockIndex = currentDirectory.firstBlock;
            nameLoop:
            while (blockIndex != -1) {
                byte[] diskDir = disk.diskRead(blockIndex);
                List<byte[]> dirEntries = deserializeDirectoryData(diskDir);

                for (byte[] entry : dirEntries) {
                    byte[] byteName = new byte[9];
                    System.arraycopy(entry, 1, byteName, 0, 9);

                    byte[] sector = new byte[4];
                    System.arraycopy(entry, 10, sector, 0, 4);

                    if (Arrays.equals(subDirName, byteName)) {
                        found = true;
                        currentDirectory = deserializeDirectory(bytesToInt(sector), bytesToString(byteName));

                        break nameLoop;
                    } else {
                        blockIndex = currentDirectory.forward;
                    }
                }
            }

            if (!found) {
                System.out.println("Sub-directory or path not found.");
                return;
            }
        }

        boolean fileFound = false;
        List<byte[]> des = deserializeDirectoryData(currentDirectory.data);
        for (byte[] entry : des) {

            byte[] byteName = new byte[9];
            System.arraycopy(entry, 1, byteName, 0, 9);

            byte[] sector = new byte[4];
            System.arraycopy(entry, 10, sector, 0, 4);

            if (bytesToString(byteName).equals(name)) {
                this.lastOpenedFile = new OpenFile(bytesToInt(sector), mode, name);
                System.out.println("Opened file '" + bytesToString(byteName) + "' in mode " + mode);
                fileFound = true;
                break;
            }

        }
        if (!fileFound) {
            System.out.println("Cannot find file!");
        }
    }

    public void delete(String path) { // TODO : finish removing har har
        String[] parts = path.split("/");
        String name = parts[parts.length - 1];

        Directory currentDirectory = deserializeDirectory(0, "root");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].isEmpty()) continue; // Skip root
            boolean found = false;

            byte[] subDirName = new byte[9];
            byte[] dirPart = parts[i].getBytes();
            System.arraycopy(dirPart, 0, subDirName, 0, Math.min(dirPart.length, 9));

            int blockIndex = currentDirectory.firstBlock;
            nameLoop:
            while (blockIndex != -1) {
                byte[] diskDir = disk.diskRead(blockIndex);
                List<byte[]> dirEntries = deserializeDirectoryData(diskDir);

                for (byte[] entry : dirEntries) {
                    byte[] byteName = new byte[9];
                    System.arraycopy(entry, 1, byteName, 0, 9);

                    byte[] sector = new byte[4];
                    System.arraycopy(entry, 10, sector, 0, 4);

                    if (Arrays.equals(subDirName, byteName)) {
                        found = true;
                        currentDirectory = deserializeDirectory(bytesToInt(sector), bytesToString(byteName));

                        break nameLoop;
                    } else {
                        blockIndex = currentDirectory.forward;
                    }
                }
            }

            if (!found) {
                System.out.println("Sub-directory or path not found.");
                return;
            }
        }

        boolean fileFound = false;
        int count = 1;
        String s = Character.toString('F');
        List<byte[]> des = deserializeDirectoryData(currentDirectory.data);
        for (byte[] entry : des) {

            byte[] byteName = new byte[9];
            System.arraycopy(entry, 1, byteName, 0, 9);

            byte[] sector = new byte[4];
            System.arraycopy(entry, 10, sector, 0, 4);

            if (bytesToString(byteName).equals(name)) {
                fileFound = true;
                if (entry[0] == 'D') {
                    Directory del = deserializeDirectory(bytesToInt(sector), "ref");
                    List<byte[]> dirEntries = deserializeDirectoryData(del.data);
                    removeZeroFilledArrays(dirEntries);

                    if (dirEntries.isEmpty()) {
                        disk.sectors[currentDirectory.firstBlock][count * 16] = 70; // 70 = 'F'
                        disk.deallocateBlock(bytesToInt(sector));
                        System.out.println("Deleted directory: " + name);

                    } else {
                        System.out.println("Cannot delete a directory with items in it. " +
                                "Please empty the directory first.");
                        break;
                    }
                } else {

                    disk.sectors[currentDirectory.firstBlock][count * 16] = 70; // 70 = 'F'
                    disk.deallocateBlock(bytesToInt(sector));
                    System.out.println("Deleted user file: " + name);
                    break;
                }

            } else {
                count++;
            }

        }
        if (!fileFound) {
            System.out.println("Cannot find file!");
        }

    }

    public void seek(int base, int offset) {
        lastOpenedFile.pointerBase = base;
        lastOpenedFile.pointerOffset = offset;
    }

    public void close() {
        if (lastOpenedFile == null) {
            System.out.println("You closed the current opened file.");
            lastOpenedFile = null;
        } else {
            System.out.println("No file is open at the moment!");
        }
    }

    public void write(int n, String str) {
        if (lastOpenedFile == null) {
            System.out.println("You need to open a file first!");
            return;
        }

        if (lastOpenedFile.mode == 'O' || lastOpenedFile.mode == 'U') {
            UserData file = deserializeUserData(lastOpenedFile.sector, lastOpenedFile.name);
            byte[] writeArray = new byte[n];
            byte[] content = str.getBytes(StandardCharsets.UTF_8);

            // Copies content to write array
            System.arraycopy(content, 0, writeArray, 0, Math.min(content.length, n));

            // Copies writeArray to data
            System.arraycopy(writeArray, 0, file.data, 0, n);

            file.size = removeZeroBytes(file.data).length;

            serializeUserData(file, lastOpenedFile.sector);

            System.out.println("You wrote " + str + " on file '" + lastOpenedFile.name + "'.");
        } else {
            System.out.println("You aren't in the right mode!");
        }
    }

    public void read(int n) {
        if (lastOpenedFile.mode == 'I' || lastOpenedFile.mode == 'U') {
            UserData file = deserializeUserData(lastOpenedFile.sector, lastOpenedFile.name);

            byte[] toRead = new byte[n];

            if(lastOpenedFile.pointerBase == 0) {
                System.arraycopy(file.data, lastOpenedFile.pointerOffset, toRead, 0, n);
            } else if(lastOpenedFile.pointerBase == -1){
                System.arraycopy(file.data, file.size + lastOpenedFile.pointerOffset, toRead, 0, n);
            }

            for (byte i : toRead) {
                if (i != 0) {
                    System.out.print((char) (i & 0xFF));
                } else {
                    System.out.println("\nEnd of file.");
                    return;
                }
            }
        }
        System.out.println("\n");
    }

    private void serializeDirectory(Directory directory, int blockIndex) {
        ByteBuffer buffer = ByteBuffer.allocate(Disk.SECTOR_SIZE);

        int current = blockIndex;

        do {
            buffer.putInt(directory.back);
            buffer.putInt(directory.forward);
            buffer.putInt(directory.free);
            buffer.putInt(directory.filler);

            buffer.put(directory.data);

            disk.diskWrite(current, buffer.array());
            buffer.clear();

            if (directory.forward != -1) {
                current = directory.forward;
            }

        } while (directory.forward != -1);
    }

    private void serializeUserData(UserData userData, int blockIndex) {
        ByteBuffer buffer = ByteBuffer.allocate(Disk.SECTOR_SIZE);

        int current = blockIndex;

        do {
            buffer.putInt(userData.back);
            buffer.putInt(userData.forward);

            buffer.put(userData.data);


            disk.diskWrite(current, buffer.array());
            buffer.clear();

            if (userData.forward != -1) {
                current = userData.forward;
            }

        } while (userData.forward != -1);
    }

    private Directory deserializeDirectory(int blockIndex, String name) {
        byte[] data = disk.diskRead(blockIndex);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        Directory dir = new Directory(name, blockIndex);

        dir.back = buffer.getInt();
        dir.forward = buffer.getInt();
        dir.free = buffer.getInt();
        dir.filler = buffer.getInt();

        if (buffer.remaining() >= dir.data.length) {
            buffer.get(dir.data, 0, dir.data.length);
        } else {
            System.out.println("Not enough data in buffer to fill the directory data array");
        }

        return dir;
    }

    private UserData deserializeUserData(int blockIndex, String name) {
        byte[] data = disk.diskRead(blockIndex);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        UserData userData = new UserData(name, blockIndex);
        userData.back = buffer.getInt();
        userData.forward = buffer.getInt();
        buffer.get(userData.data);

        return userData;
    }

    public List<byte[]> deserializeDirectoryData(byte[] data) {
        List<byte[]> entries = new ArrayList<>();

        int step = 16;
        int index = 0;

        while (index < data.length) {
            byte[] entry = new byte[16];
            System.arraycopy(data, index, entry, 0, 16);
            entries.add(entry);
            index += step;
        }

        return entries;
    }

    private void updateDirectoryEntries(Directory directory, char fileType, String name, int blockIndex, short size) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put((byte) fileType);
        buffer.put(Arrays.copyOf(name.getBytes(StandardCharsets.UTF_8), 9));
        buffer.putInt(blockIndex);
        buffer.putShort(size);

        int step = 16; //might have to change to 16
        int index = 16;

        byte[] dir = disk.diskRead(directory.free);

        while (index < dir.length) {
            if (dir[index] == 0 || dir[index] == 'F') {
                break;
            } else {
                index += step;
            }
        }

        System.arraycopy(buffer.array(), 0, dir, index, 16);
    }

    private int findFreeBlock() {
        return disk.allocateBlock();
    }

    public void displayTree(Directory directory, int depth) throws UnsupportedEncodingException {
        if (directory == null) return;
        List<byte[]> entries;

        if (depth == 0) {
            System.out.println(getIndent(depth) + "root/");
            byte[] data = disk.diskRead(0);


            entries = deserializeDirectoryData(data);
            entries.removeIf(FileSystem::isZeroFilled);

        } else {
            System.out.println(getIndent(depth) + bytesToString(directory.name) + "/");


            entries = deserializeDirectoryData(directory.data);
            entries.removeIf(FileSystem::isZeroFilled);
        }

        for (byte[] entry : entries) {
            char fileType = (char) entry[0];
            byte[] nameBytes = Arrays.copyOfRange(entry, 1, 10);
            String name = bytesToString(nameBytes).trim();
            int linkToSector = bytesToInt(Arrays.copyOfRange(entry, 10, 14));

            if (fileType == 'D') {
                Directory subDirectory = deserializeDirectory(linkToSector, name);
                displayTree(subDirectory, depth + 1);
            } else if (fileType == 'U') {
                System.out.println(getIndent(depth + 1) + name);
            } else if (fileType == 'F') {
                break;
            }
        }
    }

    private String getIndent(int depth) {
        return " ".repeat(depth * 4); // 4 spaces per depth level
    }

    public static int bytesToInt(byte[] byteList) {
        if (byteList == null || byteList.length != 4) {
            throw new IllegalArgumentException("List must contain exactly 4 bytes");
        }

        int num = 0;
        for (int i = 0; i < 4; i++) {
            num |= (byteList[i] & 0xFF) << (8 * (3 - i));
        }
        return num;
    }

    public static String bytesToString(byte[] byteList) {
        if (byteList == null) {
            throw new IllegalArgumentException("Byte list must not be null");
        }

        int actualLength = 0;
        for (byte b : byteList) {
            if (b != 0) {
                actualLength += 1;
            }
        }

        // Create a string from the byte array using UTF-8 encoding
        return new String(byteList, 0, actualLength, StandardCharsets.UTF_8);
    }

    public static void removeZeroFilledArrays(List<byte[]> listOfArrays) {
        // Use an iterator to safely remove elements while iterating

        listOfArrays.removeIf(FileSystem::isZeroFilled);
    }

    private static boolean isZeroFilled(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    private static byte[] removeZeroBytes(byte[] original) {
        List<Byte> nonZeroBytes = new ArrayList<>();

        for (byte b : original) {
            if (b != 0) {
                nonZeroBytes.add(b);
            }
        }

        byte[] result = new byte[nonZeroBytes.size()];
        for (int i = 0; i < nonZeroBytes.size(); i++) {
            result[i] = nonZeroBytes.get(i);
        }

        return result;
    }
}