package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;

public class Freestyle implements Problem {
    
    MemoryMgmt memoryMgmt;
    Visualize visualize;

    LinkedList<PointerVariable> ptrVariables;

    int variableCount = 0;

    public String name() {
        return "Freestyle";
    }

    public void init() {
        
        memoryMgmt = new MemoryMgmt(8192);
        visualize = new Visualize(memoryMgmt);
        ptrVariables = new LinkedList<>();
        
    }

    public void go() {

        visualize.update();

        while (true) {
            System.out.println("\r\n=======================");
            System.out.println("Operations");
            System.out.println("=======================\r\n");
            System.out.println("1. malloc");
            System.out.println("2. free");
            System.out.println("0. Back to main menu");

            System.out.print("Pick an operation: ");
            Integer selectedOption = 0;
            BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
            String line = "";

            try {
                line = buffer.readLine();
                System.out.println();
                selectedOption = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("I don't know what '" + line + "' is, please input a number." );
                continue;
            } catch (IOException e) {
                System.out.println("IOException, quitting...");
            }

            if (selectedOption == 0) { 
                visualize.closeFrame();
                break; 
            }

            int ptr;

            switch (selectedOption) {
                case 1: 
                    int size = inputSize();

                    ptr = malloc(size);

                    ptrVariables.addLast(new PointerVariable(ptr));

                    variableCount++;

                    System.out.println("You have allocated " + size + " bytes, and the memory is accesible via pointer: '" +  ptrVariables.getLast().name + "'");

                    break;
                case 2: 

                    ptr = inputPointer();

                    free(ptr);

                    break;
                default:
                    System.out.println("I don't know what '" + line + "' is, please input a valid option." );
                continue;
            }
        }

    }

    public int malloc(int size) {
        int ptr = memoryMgmt.malloc(size);
        visualize.update();
        return ptr;
    }

    public void free(int ptr) {
        memoryMgmt.free(ptr);
        visualize.update();
    }

    public int inputSize() {

        int size = 1;

        while (true) {
            System.out.print("Size (in bytes): " );
            BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
            String line = "";
            try {
                line = buffer.readLine();
                System.out.println();
                size = Integer.parseInt(line);
                if (size <= 0) {
                    System.out.println("Please choose a value greater than 0." );
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("I don't know what '" + line + "' is, please input a number." );
                continue;
            } catch (IOException e) {
                System.out.println("IOException, quitting...");
                break;
            }
        }

        return size;
    }

    public int inputPointer() {

        int ptr = -1;

        while (true) {
            System.out.print("Pointer name (ptrX): " );
            BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
            String name = "";
            try {
                name = buffer.readLine();
                System.out.println();

                ptr = mapNametoPtr(name);
                if (ptr == -1) {
                    System.out.println("Invalid name, please try again." );
                    continue;
                }
                break;
            } catch (IOException e) {
                System.out.println("IOException, quitting...");
                break;
            }
        }

        return ptr;
    }

    private int mapNametoPtr(String name) {

        for (PointerVariable variable : ptrVariables) {
            if (variable.name.equals(name)) { 
                return variable.ptr;
            }
        }

        return -1;

    }

    private class PointerVariable {

        int ptr;
        String name;

        public PointerVariable(int ptr) {
            this.ptr = ptr;
            this.name = "ptr" + variableCount;
        }

    }
}
