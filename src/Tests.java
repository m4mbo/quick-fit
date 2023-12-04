package src;

public class Tests implements Problem {

    MemoryMgmt memoryMgmt;
    Visualize visualize;

    public String name() {
        return("Tests");
    }

    public void init() {
        memoryMgmt = new MemoryMgmt(8192);
    }

    public void go() {
        memoryMgmt.print();
    }
    
}
