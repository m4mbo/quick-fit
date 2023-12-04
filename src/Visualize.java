package src;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Image;
import java.awt.Graphics;

public class Visualize {
    
    private Frame frame;

    private MemoryMgmt memoryMgmt;

    private final int WORD; 

    public Visualize(MemoryMgmt memoryMgmt) {
        frame =  new Frame();
        this.memoryMgmt = memoryMgmt;
        this.WORD = memoryMgmt.getWord();
    }

    public void update() {
        frame.panel.repaint();
    }

    public void closeFrame() {
        frame.dispose();
    }

    private class Frame extends JFrame {
        int FRAME_WIDTH  = 1026;
        int FRAME_HEIGHT = 273;

        Panel panel;

        public Frame(){

            setSize(FRAME_WIDTH, FRAME_HEIGHT);				//setting the frame to the constant values declared as properties
            setTitle("Virtual Memory");						    // setting a title to the frame
            setLocationRelativeTo(null);					//setting frame location to the middle of the screen
            
            panel = new Panel();

            add(panel);										// adding a panel
            setVisible(true);								// making the frame visible
            setResizable(false);					        // disabling the frame to be resizable
            setDefaultCloseOperation(EXIT_ON_CLOSE);		
        }
    }

    private class Panel extends JPanel {

        int PANEL_WIDTH  = 1024;
        int PANEL_HEIGHT = 270;
        
        Graphics graphics;

        Image image;

        public Panel() {
            setLayout(null);
            setSize(PANEL_WIDTH, PANEL_HEIGHT);									// setting the size of the panel
            setFocusable(true);													// setting focusable to true to receive keyboard input 
            setVisible(true);
        }

        public void paint(Graphics g) {
            image = createImage(PANEL_WIDTH, PANEL_HEIGHT);
            graphics = image.getGraphics();
            draw(graphics);																	
            g.drawImage(image,1,1,this);
        }
    
        public void draw(java.awt.Graphics g) {

            MemoryMgmt.Byte[] heap = memoryMgmt.getHeap();

            int y = 0;
            int x = 0;

            char flag = '?';

            int overlook = -1;

            for (int i = 0; i < 8192; i++) {
                if (x == 1024) {
                    y += 30;
                    x = 0;
                }
                if (!(i < overlook)) {
                    if (heap[i] instanceof MemoryMgmt.FlaggedByte) {

                        g.setColor(new Color(0, 0, 255));

                        overlook = i + WORD;

                        flag = ((MemoryMgmt.FlaggedByte) heap[i]).flag;
                
                    } else if (heap[i] instanceof MemoryMgmt.PointerByte) {

                        g.setColor(new Color(0, 0, 255));

                        overlook = i + WORD;

                    } else {
                        if (flag == 'F') {
                            g.setColor(new Color(0, 255, 0));
                        } else {
                            g.setColor(new Color(255, 0, 0));
                        }
                    }
                }
                g.fillRect(x, y, 1, 29);
                x++;
            }
        }
    }

}
