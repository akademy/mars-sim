/**
 * Mars Simulation Project
 * Installer.java
 * @version 2.71 2000-09-25
 * @author Scott Davis
 */

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

// This stand alone application decompresses the data files needed by the Mars Simulation Project.
// By default it decompresses both the surface and topographical data files.
// Type:  "java Installer surface" to decompress just the surface data file.
// Type:  "java Installer topo" to decompress just the topographical data file.

public class Installer extends JFrame implements ActionListener, WindowListener {

    private final static int compressedMapWidth = 3359;
    private final static int compressedMapHeight = 786;

    private FileOutputStream dataOut; // Output stream to file.
    private BufferedOutputStream buffOut; // The output stream buffer.
    private JFrame frame; // Primary window frame.
    private JLabel statusLabel; // Status label.
    private JProgressBar fileBar, totalBar; // The file and tool progress bars.

    public Installer(String args[]) {

        // Use JFrame constructor

        super("Mars Simulation Project Map Data Installer");

        // Create UI window.
        
        setupWindow();

        // Decompress data files

        decompressDataFiles(args);
    }

    /** Prepare GUI Interface */
    private void setupWindow() {

        // Setup frame

        addWindowListener(this);
        setVisible(false);

        // Create Main Pane

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder( new CompoundBorder(new BevelBorder(BevelBorder.RAISED),
                new EmptyBorder(10, 10, 10, 10)));
        setContentPane(panel);

        // Create Title Pane

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.add("North", titlePanel);

        // Create Title Label

        JLabel titleLabel = new JLabel("Mars Simulation Installer", JLabel.CENTER);
        titleLabel.setFont(new Font("Helvetica", Font.BOLD, 20));
        titleLabel.setForeground(Color.red);
        titlePanel.add(titleLabel);

        // Create Status Label

        statusLabel = new JLabel("Loading Compressed Files", JLabel.CENTER);
        titlePanel.add(statusLabel);

        // Create Progress Bar Pane

        JPanel progressPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        panel.add("Center", progressPanel);

        // Create File Progress Bar

        fileBar = new JProgressBar(0, 100);
        progressPanel.add(fileBar);

        // Create Total Progress Bar

        totalBar = new JProgressBar(0, 100);
        progressPanel.add(totalBar);

        // Create Cancel Button Pane

        JPanel cancelPanel = new JPanel();
        panel.add("South", cancelPanel);

        // Create Cancel Button

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        cancelPanel.add(cancelButton);

        // Prepare and Show Window

        pack();
        Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frame_size = getSize();
        setLocation(((screen_size.width - frame_size.width) / 2),
                ((screen_size.height - frame_size.height) / 2));
        setVisible(true);
    }

    /** Decompress Data Files */
    private void decompressDataFiles(String args[]) {

        // Prepare File Names

        String[] mapNames = {"Surface", "Topo"};

        int numFiles = 2;

        if (args.length > 0) {
            if (args[0].equals("topo")) {
                numFiles = 1;
                mapNames[0] = new String("Topo");
            } else if (args[0].equals("surface"))
                numFiles = 1;
        }

        // Convert both JPEG's into DAT Files

        for (int x = 0; x < numFiles; x++) {

            // Display status of image being loaded

            statusLabel.setText("Loading " + mapNames[x] + "MarsMap.jpg");

            // Load image as ImageIcon to make sure image loads completely

            Image compressedMap = (new ImageIcon("map_data/" + mapNames[x] + "MarsMap.jpg")). getImage();

            // Display status of data file being created

            statusLabel.setText("Creating " + mapNames[x] + "MarsMap.dat");

            int count = 0;
            try {
                // Prepare Buffered Output Stream To Create DAT File

                dataOut = new FileOutputStream("map_data/" + mapNames[x] + "MarsMap.dat");
                buffOut = new BufferedOutputStream(dataOut);

                // Create Array To Hold Compressed Row Of Data

                int[] rowArray = new int[compressedMapWidth];
                ColorModel dColorModel = ColorModel.getRGBdefault();
                int rows = compressedMapHeight;

                // Go Through Each Row In JPEG File

                for (int z = 0; z < rows; z++) {

                    // Grab Row Of Pixels And Put In Array

                    PixelGrabber grabber =
                            new PixelGrabber(compressedMap, 0, z, compressedMapWidth, 1, rowArray,
                            0, compressedMapWidth);
                    try {
                        grabber.grabPixels();
                    } catch (Exception e) {
                        System.out.println(e.toString());
                    }
                    if ((grabber.getStatus() & ImageObserver.ABORT) != 0)
                        System.err.println("image fetch aborted or errored");

                    // Go Through Each Pixel In Row And Write Pixel Value To DAT File

                    for (int y = 0; y < rowArray.length; y++) {
                        int pixel = rowArray[y];
                        buffOut.write((byte) dColorModel.getRed(pixel));
                        buffOut.write((byte) dColorModel.getGreen(pixel));
                        buffOut.write((byte) dColorModel.getBlue(pixel));
                    }

                    // Update Progress Bars If New Percentage Done

                    int newCount = (int) Math.round(((float) z / (float) compressedMapHeight) * 100F);
                    if (newCount > count) {
                        count = newCount;
                        fileBar.setValue(newCount);
                        if (numFiles == 1)
                            totalBar.setValue(newCount);
                        else
                            totalBar.setValue((newCount / 2) + (x * 50));
                    }
                }

                // Close Output Streams

                buffOut.close();
                dataOut.close();
            }
            catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }

    /** ActionListener Method */
    public void actionPerformed(ActionEvent event) {
        cancelOut();
    }

    /** WindowListener Methods */
    public void windowClosing(WindowEvent event) {
        cancelOut();
    }
    public void windowClosed(WindowEvent event) {}
    public void windowDeiconified(WindowEvent event) {}
    public void windowIconified(WindowEvent event) {}
    public void windowActivated(WindowEvent event) {}
    public void windowDeactivated(WindowEvent event) {}
    public void windowOpened(WindowEvent event) {}

    /** Closes Data Files And Exits Application */
    private void cancelOut() {
        try {
            if (dataOut != null) {
                buffOut.close();
                dataOut.close();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        frame.dispose();
        System.exit(0);
    }

    /** Main Method */
    public static void main(String args[]) {
        Installer installer = new Installer(args);
        System.exit(0);
    }
}



