package src;

public class Tests implements Problem {

    MemoryMgmt memoryMgmt;
    Visualize visualize;
    boolean GUIFlag;

    public Tests(boolean GUIFlag) {
        this.GUIFlag = GUIFlag;
    }

    public String name() {
        return("Tests");
    }

    public void init() {
        memoryMgmt = new MemoryMgmt(8192);
        if (GUIFlag) {
            visualize = new Visualize(memoryMgmt);
            memoryMgmt.setGUI(visualize);
        }
    }

    public void go() {
        memoryMgmt.print();
        if (visualize != null) visualize.closeFrame();
    }
    
}
