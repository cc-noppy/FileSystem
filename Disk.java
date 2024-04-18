import java.util.Arrays;

public class Disk {
    private final byte[][] sectors;
    private final boolean[] blockUsed;
    public static final int SECTOR_SIZE = 512;
    public final int numSectors;



    public Disk(int numSectors){
        this.numSectors = numSectors;
        this.sectors = new byte[numSectors][SECTOR_SIZE];
        this.blockUsed = new boolean[numSectors]; // Track block usage
        initializeDisk();
    }

    private void initializeDisk(){
        for(int i = 0; i < numSectors; i++){
            Arrays.fill(sectors[i], (byte) 0); //fill all sectors with zero
        }
    }

    public int sectorUsage(int sectorNumber) {
        int count = 0;
        for (byte b : sectors[sectorNumber]) {
            if (b != 0) {
                count++;
            }
        }
        return count;
    }

    public byte[] diskRead(int sectorNumber){
        if (sectorNumber < 0 || sectorNumber >= numSectors) {
            throw new IllegalArgumentException("Invalid sector number");
        }
        return sectors[sectorNumber];
    }

    public void diskWrite(int sectorNumber, byte[] data){
        if (sectorNumber < 0 || sectorNumber >= numSectors) {
            throw new IllegalArgumentException("Invalid sector number");
        }
        if (data.length > 512) {
            throw new IllegalArgumentException("Data exceeds sector size");
        }
        System.arraycopy(data, 0, sectors[sectorNumber], 0, data.length);
    }

    public int allocateBlock() {
        for (int i = 0; i < numSectors; i++) {
            if (!blockUsed[i]) { // Find the first free block
                blockUsed[i] = true; // Mark as used
                Arrays.fill(sectors[i], (byte) 0); // Clear the block when allocated
                return i;
            }
        }
        return -1; // No free blocks available
    }
}