/*
 * Quick Fit scheme
 * Lazy Coalescing
 * Author: 38879557
 * Sources: Weinstock, Charles B. Dynamic Storage Allocation Techniques, Ph.D. dissertation, Department of Computer Science, Carnegie Mellon University, 1976
 */
package src;

import java.util.LinkedList;

public class MemoryMgmt {
    
    Visualize visualize;                    // GUI

    private final int WORD = 4;             // 32-bit system
    private final int NULL = -1;            

    private final int minQL = 1;
    private final int maxQL = 16;

    private int memorySize;

    // Heap extensions after exceeding main heap memory limit
    private LinkedList<Byte[]> heapExtensions;

    private Byte[] heap;                  // Array simulating heap virtual memory

    /*
     * Memory is divided into:
     * Working storage - allocated or previously allocated space
     * Tail - untouched memory
     * Keeping track of this tail optimizes 2nd priority allocation
     */
    private int tail;                   

    /*
     * Free lists of predefined sizes
     * Free areas of n*DWORD in size
     * n ranges in between @minQL and @maxQL
     */
    private Bin[] bins;
    
    /*
     * Miscellaneous list of free blocks
     * No predefined sizes
     * Allocation handled using First Fit (could be optimized using balanced AVL tree ~ Fast Fit)
     * Original Blis-11 implementation
     */
    private FreeList misc;              

    // Distinction between length and previous length
    public enum LengthType {
        PLEN, LEN;
    }
    
    public MemoryMgmt(int memorySize) {
        this.memorySize = memorySize;
        visualize = null;
        initializeMemory();
    }

    /*
     * 1. Check bins
     * 2. Check "tail" space
     * 3. Check misc
     * 4. [1st run] Coalesce -> go to 1.
     * 5. [2nd run] Request for more memory (sbrk)
     */
    public int malloc(int size) {

        int actualSize = size;

        if (size < 2*WORD) actualSize = 2*WORD;   // Minimum amount necessary to create a free block

        actualSize = actualSize + 2*WORD;

        boolean coalesced = false;

        // Looping until coalesced
        while (true) {

            if (!coalesced) System.out.print("Requesting " + size + " bytes of memory ... ");
        
            if (size <= 0) {
                System.out.print("Exception triggered in thread. Exiting.\n\n");
                return NULL;
            }
        
            int pointer = checkBins(actualSize);

            if (pointer != NULL) {

                allocateSpace(actualSize, pointer, true);

                System.out.print("memory allocated.\n");
                System.out.print("Pointer: " + integerToHex(pointer+2*WORD) + "\n\n");

                return pointer+2*WORD;

            } else {

                pointer = checkTail(actualSize, tail);

                if (pointer != NULL) {

                    allocateSpace(actualSize, pointer, false);

                    System.out.print("memory allocated.\n");
                    System.out.print("Pointer: " + integerToHex(pointer+2*WORD) + "\n\n");

                    return pointer+2*WORD;

                } else {

                    pointer = checkMisc(actualSize);

                    if (pointer != NULL) {

                        allocateSpace(actualSize, pointer, false);

                        System.out.print("memory allocated.\n");
                        System.out.print("Pointer: " + integerToHex(pointer+2*WORD) + "\n\n");

                        return pointer+2*WORD;
                    }
                }
            }

            if (coalesced) break;

            System.out.print("\nAttempting to coalesce free blocks ... ");

            // If no space available, coalesce adjacent free blocks
            coalescePass();

            coalesced = true;
        }

        System.out.print("\nMemory limit exceeded, requesting further memory blocks ... ");

        sbrk(actualSize);

        // Search for new freeblock *we can skip tail and bin search*
        int extensionPointer = checkMisc(actualSize);

        if (extensionPointer == NULL) throw new MemoryError("Memory request failed.");
        
        allocateSpace(actualSize, extensionPointer, false);
        
        System.out.print("memory allocated.\n");
        System.out.print("Pointer: " + integerToHex(extensionPointer+2*WORD) + "\n\n");

        return extensionPointer+2*WORD;    
    }

    /*
     * If size corresponds to n*DWORD + overhead -> add free block to bin
     * If not, add to misc
     */
    public void free(int ptr) {

        if (ptr <= 0){ 
            System.out.print("Invalid pointer ... Exception triggered in thread. Exiting.\n\n");
            return;
        }

        System.out.print("Freeing pointer " + integerToHex(ptr) + " ... ");

        if (!(getByte(ptr-WORD) instanceof FlaggedByte)) {
            System.out.print("Exception triggered in thread. Exiting.\n\n");
            return;
        }

        if (((FlaggedByte) getByte(ptr-WORD)).flag == 'F') {
            System.out.print("Exception triggered in thread. Exiting.\n\n");
            return;
        }

        int blockSize = ((FlaggedByte) getByte(ptr-WORD)).length;
        int ptrToBlock = ptr - 2*WORD;

        ((FlaggedByte) getByte(ptrToBlock+WORD)).flag = 'F';
        ((FlaggedByte) getByte(ptrToBlock+blockSize)).flag = 'F';

        getListOrigin(blockSize).addToList(ptrToBlock);

        System.out.print("memory freed.\n\n");
    }

    public Byte[] sbrk(int size) {

        int closestPower = 0;

        int i = 0;
        while(true) {
            closestPower = (int) Math.pow(2, i);
            // Add 2 WORDS to power due to the extra 2 cells for metadata at the end
            if (size == closestPower-2*WORD) break;
            // Preventing unreacheable memory, we need the freeblock at the end
            if (((closestPower-2*WORD)-size >= 16)) break;
            i++;
        }

        Byte[] extension = new Byte[closestPower];

        // Start *this will prevent coallescing across sbrk-allocated boundaries*
        extension[0] = new FlaggedByte('U', 0, LengthType.PLEN);    
        
        // End
        extension[closestPower-WORD] = new FlaggedByte('U', 0, LengthType.LEN);     

        // First free block
        extension[WORD] = new FlaggedByte('F', closestPower-WORD*2, LengthType.LEN);
        extension[closestPower-WORD*2] = new FlaggedByte('F', closestPower-WORD*2, LengthType.PLEN);

        int macroPointer = memorySize;

        for (Byte[] exten : heapExtensions) {
            macroPointer += exten.length;
        }

        heapExtensions.add(extension);
        
        // Adding newly created free block to misc 
        misc.addToList(macroPointer);

        return extension;
    }
    
    // Running tests
    public void print() {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
        test8();
        test9();
        test10();
    }

    public int checkBins(int size) {
        
        // Checking if the memory requested corresponds to a predefined size
        if (size >= (bins[0].freeArea) && size % (2*WORD) == 0) {
            int index = (size/(2*WORD)) - 2;
            if (index > maxQL-1) return NULL;
            Bin bin = bins[index];
            if (bin.HEAD != NULL) return bin.HEAD;
        } 
        return NULL;
    }

    public int checkTail(int size, int freeblock) {

        // If the tail is pointing to the end
        if (tail == memorySize-3*WORD) return NULL;

        // Calculating remaining memory after hypothetical tail allocation
        int tailRemainingSize = ((FlaggedByte) heap[tail+WORD]).length - size;

        /*
         * Accepting only if block fits perfectly
         * Or if there is enough space to build a new free block (at least 4 words for metadata)
         * Avoids unreachable memory
         */
        if (tailRemainingSize == 0 || tailRemainingSize >= 4*WORD) return tail;
    
        return NULL;
    }

    // First Fit
    public int checkMisc(int size) {

        if (misc.HEAD == NULL) return NULL;         // Checking if misc is empty
        
        int current = misc.HEAD;                    // Current free block

        do {
            int remainingSize = ((FlaggedByte) getByte(current+WORD)).length - size; // Calculating hypothetical remaining size
            /*
             * Accepting only if block fits perfectly
             * Or if there is enough space to build a new free block (at least 4 words for metadata)
             * Avoids unreachable memory
             */
            if (remainingSize == 0 || remainingSize >= 4*WORD) return current;
            current = ((PointerByte) getByte(current+3*WORD)).pointer;   // Iterating to next free block in list
        } while (current != NULL);

        return NULL;
    }

    public void allocateSpace(int size, int freeblock, boolean binAlloc) {

        if (binAlloc) {
            // Double check if the byte at the other end is flagged
            if (! (getByte(freeblock+size) instanceof FlaggedByte)) {
                throw new MemoryError("Byte at " + (freeblock+size) + "should be flagged");
            } else {

                getListOrigin(size).removeFromList(freeblock);

                setByte(freeblock+WORD, new FlaggedByte('U', size, LengthType.LEN));
                
                ((FlaggedByte) getByte(freeblock+size)).flag = 'U';

                return;
            }
        } else {

            int blockSize = ((FlaggedByte) getByte(freeblock+WORD)).length;

            setByte(freeblock+WORD, new FlaggedByte('U', size, LengthType.LEN));

            setByte(freeblock+size, new FlaggedByte('U', size, LengthType.PLEN));

            if (freeblock != tail) misc.removeFromList(freeblock);  // If it is a misc allocation, we remove the freeblock from the list
                
            // if it is not a perfect fit, create a new free block
            if (blockSize != size) {
                
                setByte(freeblock+size+WORD, new FlaggedByte('F', blockSize-size, LengthType.LEN));
                ((FlaggedByte) getByte(freeblock+blockSize)).length = blockSize - size;

                // If it was a tail allocation
                if (freeblock == tail){
                    tail = freeblock+size;
                    setByte(freeblock+size+2*WORD, new PointerByte(NULL));
                    setByte(freeblock+size+3*WORD, new PointerByte(NULL));
                    // Clearing out list pointers *good practice*
                    setByte(freeblock+2*WORD, null);
                    setByte(freeblock+3*WORD, null);
                } else {
                    // We add the new freeblock to its respective list
                    getListOrigin(blockSize-size).addToList(freeblock+size);
                }
            } else {
                if (freeblock == tail) tail = freeblock+size;
            }
        }
    }

    public FreeList getListOrigin(int blockSize) {
        if (blockSize >= (bins[0].freeArea) && blockSize % (2*WORD) == 0) {
            
            int index = (blockSize/(2*WORD)) - 2;

            if (!(index > maxQL-1)) {  
                return bins[index];
            } 
        }
        return misc;
    }

    /*
     * Lazy coalescing
     * Only if we run out of memory
     * Coalesce all possible freeblocks
     */
    public void coalescePass() {

        // Check if tail can be coalesced
        if (tail != memorySize-3*WORD && ((FlaggedByte) heap[tail]).flag == 'F') mergeAndDistribute(tail);

        // Misc search
        int current = misc.HEAD;  

        if (current != NULL) {
            do {
                if (((FlaggedByte) getByte(current)).flag == 'F') {
                    mergeAndDistribute(current);
                    // Restart search due to new potential coalescing
                    current = misc.HEAD;
                    continue;
                }
                current = ((PointerByte)getByte(current+3*WORD)).pointer;   // Iterating to next free block in list
            } while (current != NULL);
        }
        
        // Bin search
        for (Bin bin : bins) {
            current = bin.HEAD;
            if (current != NULL) {
                do {
                    if (((FlaggedByte) getByte(current)).flag == 'F') {
                        mergeAndDistribute(current);
                        // Restart search due to new potential coalescing
                        current = bin.HEAD;
                        continue;
                    }
                    current = ((PointerByte) getByte(current+3*WORD)).pointer;   // Iterating to next free block in list
                } while (current != NULL);
            }
        } 
    }

    /*
     * Takes the intersection between 2 freeblocks
     * Merges both blocks
     * Distributes list references accordingly
     */
    public void mergeAndDistribute(int intersection) {

        int prevLength = ((FlaggedByte) getByte(intersection)).length;     // Getting length of prev block 
        
        int length = ((FlaggedByte) getByte(intersection+WORD)).length;    // Getting length of block

        // Cleaning up metadata (PLEN and LEN) *good practice*
        setByte(intersection, null);
        setByte(intersection, null);
 
        // Updating length of new bigger free block
        ((FlaggedByte) getByte(intersection+length)).length = prevLength + length;
        ((FlaggedByte) getByte(intersection-prevLength+WORD)).length = prevLength + length;

        // Removing the left block from its list
        getListOrigin(prevLength).removeFromList(intersection-prevLength);

        // If we are not coalescing the tail (includes heap extensions)
        if (intersection != tail || intersection > memorySize) {
            // Removing the right block from its list
            getListOrigin(length).removeFromList(intersection);
            // Adding new bigger block to its list
            getListOrigin(prevLength+length).addToList(intersection-prevLength);
            return;
        }

        tail -= prevLength;

        // Setting tail pointers to null
        setByte(tail+2*WORD, new PointerByte(NULL));
        setByte(tail+3*WORD, new PointerByte(NULL));
     
        // Cleaning up old pointers
        setByte(intersection+2*WORD, null);
        setByte(intersection+3*WORD, null);
    }

    public void storeData(int ptr, String data) {
        System.out.print("Storing '" + data + "' at address " + integerToHex(ptr) + " ... ");
        setByte(ptr, new DataByte(data));
        System.out.print("stored.\n\n");
    }
    
    public String retrieveData(int ptr) {
        System.out.print("Retrieving data at address " + integerToHex(ptr) + " ... ");
        String data = ((DataByte)getByte(ptr)).data;
        System.out.print("'" + data + "' retrieved.\n\n");
        return data;
    } 

    /*
     * Helper methods
     */

    public void setGUI(Visualize visualize) {
        this.visualize = visualize;
    }

    public void refreshGUI() {
        if (visualize == null) return;
        visualize.update();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
    }

    public void initializeMemory() {
        
        heapExtensions = new LinkedList<>();

        heap = new Byte[memorySize];

        // Start
        heap[0] = new FlaggedByte('U', 0, LengthType.PLEN);    
        
        tail = 0;   // Intializing tail
        
        // End
        heap[memorySize-WORD] = new FlaggedByte('U', 0, LengthType.LEN);     

        // First free block
        heap[WORD] = new FlaggedByte('F', memorySize-WORD*2, LengthType.LEN);
        heap[WORD*2] = new PointerByte(NULL);
        heap[WORD*3] = new PointerByte(NULL);

        heap[memorySize-WORD*2] = new FlaggedByte('F', memorySize-WORD*2, LengthType.PLEN);

        // Initializing bins
        bins = new Bin[maxQL];

        for (int i = minQL; i <= maxQL; i++) {
            bins[i-1] = new Bin(NULL, NULL, i);
        }

        // Initializing misc list
        misc = new FreeList(NULL, NULL);
    
    }

    // Methods to hide away pointer complexity
    public void setByte(int pointer, Byte byteToSet) {

        if (pointer < memorySize) {
            heap[pointer] = byteToSet;
            return;
        }

        int cumulative = memorySize;

        for (Byte[] extension : heapExtensions) {
            if (pointer < cumulative + extension.length) {
                extension[pointer-cumulative] = byteToSet;
                return;
            }
            cumulative += extension.length;
        }

    }

    public Byte getByte(int pointer) {
        
        // If it is a pointer within the main heap
        if (pointer < memorySize) {
            return heap[pointer];
        }
        
        int cumulative = memorySize;

        // If it is a pointer outside main heap bounds (extension)
        for (Byte[] extension : heapExtensions) {
            if (pointer < cumulative+extension.length) {
                return extension[pointer-cumulative];
            }
            cumulative += extension.length;
        }

        return null;

    }

    public String integerToHex(int ptr) {
        
        String hex = Integer.toHexString(ptr);

        int zerosToAdd = 8-hex.length();

        for (int i = 0; i < zerosToAdd; i++) {
            hex = "0" + hex;
        }

        return "0x" + hex;
    }

    /*
     * Helper classes
     */

    public interface Byte {}

    public class FlaggedByte implements Byte {
        char flag;      // 1 bit flag
        int length;    // ~4 byte word (63 bits)
        LengthType type;

        public FlaggedByte(char flag, int length, LengthType type) {
            this.type = type;
            this.flag = flag;
            this.length = length;
        }
    }

    public class DataByte implements Byte {  
        String data;    // n byte data
            
        public DataByte(String data) {
            this.data = data;
        }
    }

    public class PointerByte implements Byte {
        int pointer;    // 8 byte word
            
        public PointerByte(int pointer) {
            this.pointer = pointer;
        }
    }

    private class FreeList {
        int HEAD;
        int TAIL;

        public FreeList(int HEAD, int TAIL) {
            this.HEAD = HEAD;
            this.TAIL = TAIL;
        }

        // Removing free block from list
        public void removeFromList(int freeblock) {

            // If there is only one item
            if (HEAD == TAIL) {
                HEAD = NULL;
                TAIL = NULL;
                // Clearing out list pointers *good practice*
                setByte(freeblock+2*WORD, null);
                setByte(freeblock+3*WORD, null);
                return;
            }

            // If it is the first block int the list
            if (HEAD == freeblock) {
                HEAD = ((PointerByte) getByte(freeblock+3*WORD)).pointer;
                ((PointerByte) getByte(HEAD+2*WORD)).pointer = NULL;    // Updating prev reference of new head
                // Clearing out list pointers *good practice*
                setByte(freeblock+2*WORD, null);
                setByte(freeblock+3*WORD, null);
                return;
            }

            // If it is the last block in the list
            if (TAIL == freeblock) {
                TAIL = ((PointerByte) getByte(freeblock+2*WORD)).pointer;
                ((PointerByte) getByte(TAIL+3*WORD)).pointer = NULL;    // Updating next reference of new tail
                // Clearing out list pointers *good practice*
                setByte(freeblock+2*WORD, null);
                setByte(freeblock+3*WORD, null);
                return;
            }

            // Look for block

            int current = misc.HEAD;                    // Current free block

            do {
                if (current == freeblock) {
                    int prevBlock = ((PointerByte) getByte(current+2*WORD)).pointer;
                    int nextBlock = ((PointerByte) getByte(current+3*WORD)).pointer;
                    // Exchanging prev and next references of adjacent blocks
                    ((PointerByte) getByte(prevBlock+3*WORD)).pointer = nextBlock;
                    ((PointerByte) getByte(nextBlock+2*WORD)).pointer = prevBlock;
                }
                current = ((PointerByte) getByte(current+3*WORD)).pointer;   // Iterating to next free block in list
            } while (current != NULL);

            // Clearing out list pointers *good practice*
            setByte(freeblock+2*WORD, null);
            setByte(freeblock+3*WORD, null);
        }

        // Appending free block to head of list
        public void addToList(int freeblock) {

            // Setting prev as null
            setByte(freeblock+2*WORD, new PointerByte(NULL));

            // If the list is empty
            if (HEAD == NULL) {
                HEAD = freeblock;
                TAIL = freeblock;
                // Setting next as null
                setByte(freeblock+3*WORD, new PointerByte(NULL));
                return;
            }

            // If list is not empty
            ((PointerByte) getByte(HEAD+2*WORD)).pointer = freeblock;           // Setting prev reference of current head to new block
            // Set next to current HEAD
            setByte(freeblock+3*WORD, new PointerByte(HEAD));
            HEAD = freeblock;
        }
    }

    private class Bin extends FreeList{
        int n;
        int freeArea;

        public Bin(int HEAD, int TAIL, int n) {
            super(HEAD, TAIL);
            this.n = n;
            this.freeArea = n*WORD*2 + (2*WORD);    // n*DWORD + pointers(2*WORD)
        }
    }

    private class MemoryError extends Error {
        public MemoryError(String description) {
            super(description);
        }
    }

    // Getters

    public Byte[] getHeap() {
        return heap;
    }

    public int getWord() {
        return WORD;
    }

    // Tests

    public void testHeader(int number, boolean reset, String description) {
        if (reset) initializeMemory();
        System.out.print("===================================\n\n");
        System.out.print("Running test number " + number + " ...\n\n");
        System.out.print("Description: " + description + "\n\n");
        System.out.print("HEAD pointer: " + integerToHex(tail) + "\n\n");
    }

    public void test1() {
        testHeader(1, true, "Required.");
        int ptr1 = malloc(28);
        refreshGUI();
        storeData(ptr1, "string");
        retrieveData(ptr1);
        free(ptr1);
        refreshGUI();
    }

    public void test2() {
        testHeader(2, true, "Required.");
        int ptr1 = malloc(28);
        refreshGUI();
        int ptr2 = malloc(1024);
        refreshGUI();
        int ptr3 = malloc(28);
        refreshGUI();
        free(ptr2);
        refreshGUI();
        int ptr4 = malloc(512);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr3);
        refreshGUI();
        free(ptr4);
        refreshGUI();
    }

    public void test3() {
        testHeader(3, true, "Required.");
        int ptr1 = malloc(8150);
        refreshGUI();
        int ptr2 = malloc(72);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr2);
        refreshGUI();
    }

    public void test4() {
        testHeader(4, true, "Required.");
        malloc(1024);
        refreshGUI();
        int ptr1 = malloc(28);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr1);
        refreshGUI();
    }

    public void test5() {
        testHeader(5, true, "Quick-fit bin allocation.");
        int ptr1 = malloc(16);
        refreshGUI();
        int ptr2 = malloc(24);
        refreshGUI();
        int ptr3 = malloc(32);
        refreshGUI();
        int ptr4 = malloc(512);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr2);
        refreshGUI();
        free(ptr3);
        refreshGUI();
        free(ptr4);
        refreshGUI();
        malloc(24);
        refreshGUI();
        malloc(16);
        refreshGUI();
        malloc(32);
        refreshGUI();
    }

    public void test6() {
        testHeader(6, true, "First Fit misc allocation.");
        int ptr1 = malloc(230);
        refreshGUI();
        int ptr2 = malloc(100);
        refreshGUI();
        malloc(7750);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr2);
        refreshGUI();
        int ptr3 = malloc(50);  
        refreshGUI();
        storeData(ptr3, "stored in 108 free block as it is the first in the misc list");
        retrieveData(ptr3);
        free(ptr3);
        refreshGUI();
    }

    public void test7() {
        testHeader(7, true, "Minimum malloc allocation set to 8 bytes to aviod unreacheable memory.");
        int ptr1 = malloc(1);
        refreshGUI();
        storeData(ptr1, "minimum space is 8");
        retrieveData(ptr1);
        int ptr2 = malloc(24);
        refreshGUI();
        malloc(2);
        refreshGUI();
        malloc(512);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr2);
        refreshGUI();
    }

    public void test8() {   
        testHeader(7, true, "Lazy coalescing, free lists.");
        int ptr1 = malloc(8);
        refreshGUI();
        int ptr2 = malloc(20);
        refreshGUI();
        int ptr3 = malloc(32);
        refreshGUI();
        malloc(8092);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr2);
        refreshGUI();
        free(ptr3);
        refreshGUI();
        malloc(76);
    }

    public void test9() {
        testHeader(7, true, "Lazy coalescing, tail.");
        int ptr1 = malloc(1);
        refreshGUI();
        storeData(ptr1, "minimum space is 8");
        retrieveData(ptr1);
        int ptr2 = malloc(24);
        refreshGUI();
        malloc(2);
        refreshGUI();
        malloc(512);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr2);
        refreshGUI();
    }

    public void test10() {
        testHeader(7, true, "Lazy coalescing, sbrk allocated area.");
        int ptr1 = malloc(1);
        refreshGUI();
        storeData(ptr1, "minimum space is 8");
        retrieveData(ptr1);
        int ptr2 = malloc(24);
        refreshGUI();
        malloc(2);
        refreshGUI();
        malloc(512);
        refreshGUI();
        free(ptr1);
        refreshGUI();
        free(ptr2);
        refreshGUI();
    }

}