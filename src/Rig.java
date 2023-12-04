/*
Rig.java

Author : Dr James Stovold
Date   : Aug 17, 2022
Version: 0.1
*/
package src;

import java.lang.System;

import java.io.*;

public class Rig { 

    Problem problem;    // abstract class interface (to be overridden by 
                        // student implementations) 
    
    public static void main(String[] args) {

        while (true) {

            System.out.println("\r\n=======================");
            System.out.println("Memory Management Menu");
            System.out.println("=======================\r\n");
            System.out.println("1. Freestyle (GUI)");
            System.out.println("2. Tests");
            System.out.println("0. Exit");

            System.out.print("Pick a problem: ");
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

            if (selectedOption == 0) { break; }
            Rig rig = new Rig();
            
            switch (selectedOption) {
                case 1: // single sensor
                System.out.println("Setting up problem...");
                rig.problem = new Freestyle(); 
                break;
                case 2: // multiple sensors
                boolean GUI = inputGUI();
                System.out.println("Setting up problem...");
                rig.problem = new Tests(GUI);
                break;
                default:
                System.out.println("I don't know what '" + line + "' is, please input a valid option." );
                continue;
            }

            System.out.println("Initialising problem: " + rig.problem.name());
            rig.problem.init();
            System.out.println(rig.problem.name() + " established.");
            System.out.println("Running...\r\n");
            rig.problem.go();     
        }
        System.exit(0);
    }  

    public static boolean inputGUI() {
        boolean GUI = false;
        while (true) {
            System.out.print("GUI [y/n]: " );
            BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
            String line = "";
            try {
                line = buffer.readLine();
                System.out.println();
                if (line.equals("y")) {
                    GUI = true;
                    break;
                } else if (line.equals("n")) {  
                    GUI = false;
                    break;
                }
                continue;
            } catch (NumberFormatException e) {
                System.out.println("I don't know what '" + line + "' is, please y or n." );
                continue;
            } catch (IOException e) {
                System.out.println("IOException, quitting...");
                break;
            }
        }
        return GUI;
    }
}


  







