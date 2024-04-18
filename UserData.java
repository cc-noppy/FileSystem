public class UserData extends File {
    private Block head;  // Points to the first block containing data

    public UserData(String name, int firstBlock) {
        super(name, 'U', firstBlock);
        head = null;  // Initially, there are no blocks
    }

    public Block getOrCreateHead(Disk disk, int sectorSize) {
        if (head == null) {
            head = new Block(disk.allocateBlock());
            head.data = new byte[sectorSize - 8]; // Accounting for next and previous pointers
        }
        return head;
    }
    public void addBlock(Block newBlock) {
        if (head == null) {
            head = newBlock;
        } else {
            Block current = head;
            while (current.nextBlock != -1) { // Traverse to the last block
                current = current.next; // Assume we maintain both link in memory and block index
            }
            current.next = newBlock;
            newBlock.previousBlock = current.blockIndex; // Link blocks bi-directionally if needed
        }
    }

    public Block getHead() {
        return head;
    }

    @Override
    public void addFile(File file) {
        throw new UnsupportedOperationException("Cannot add a file to user data.");
    }

    @Override
    public File getFile(String name) {
        throw new UnsupportedOperationException("Cannot get a file from user data.");
    }


    // Represents a block in the file data.
    public static class Block {
        int blockIndex;
        int previousBlock;
        int nextBlock;
        byte[] data;  // Actual user data, max size of 504 bytes
        Block next;   // Pointer to next block object

        public Block(int blockIndex) {
            this.blockIndex = blockIndex;
            this.previousBlock = -1;  // No previous block (start of the list)
            this.nextBlock = -1;      // No next block (end of the list)
            this.data = new byte[504];
            this.next = null;
        }
    }
}