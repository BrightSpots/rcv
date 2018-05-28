/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose: Front-end graphical user interface (GUI)
 * Version: 1.0
 */

package com.rcv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

class RcvGui {

  // label showing the title of the application
  private static final JLabel labelTitle;
  // text area which communicates the status of the tabulator's operations
  private static final JTextArea textStatus;
  // scroll pane for textStatus
  private static final JScrollPane scrollerStatus;
  // FileChooser used as a dialog box for loading a config
  private static final JFileChooser fc;
  // main frame to render GUI elements
  private static final JFrame frame;
  // filter used by FileChooser to restrict it to loading JSON files
  private static final FileNameExtensionFilter filterJson;
  // currently-loaded tabulator config
  private static ElectionConfig config;

  static {
    labelTitle = new JLabel("Universal RCV Tabulator", SwingConstants.CENTER);
    textStatus = new JTextArea(10, 20);
    scrollerStatus = new JScrollPane(textStatus);
    fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
    frame = new JFrame("Universal RCV Tabulator");
    filterJson = new FileNameExtensionFilter("JSON file", "json");
  }

  // function: launch
  // purpose: launches the GUI
  // param: N/A
  // returns: N/A
  void launch() {
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    // main panel to render GUI elements
    JPanel panelMain = new JPanel(new BorderLayout());
    panelMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // box to contain the buttons
    Box boxButtons = new Box(BoxLayout.Y_AXIS);

    // button that summons the dialog box to load the config
    JButton buttonLoadConfig = new JButton("Load config");
    buttonLoadConfig.addActionListener(new LoadConfigListener());
    buttonLoadConfig.setAlignmentX(Component.CENTER_ALIGNMENT);
    boxButtons.add(buttonLoadConfig);

    // button that starts tabulation after config is loaded
    JButton buttonTabulate = new JButton("Tabulate");
    buttonTabulate.addActionListener(new TabulateListener());
    buttonTabulate.setAlignmentX(Component.CENTER_ALIGNMENT);
    boxButtons.add(buttonTabulate);

    scrollerStatus.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollerStatus.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    textStatus.setLineWrap(true);
    textStatus.setEditable(false);
    printToTextStatus("Welcome to the Universal RCV Tabulator!");

    panelMain.add(BorderLayout.NORTH, labelTitle);
    panelMain.add(BorderLayout.CENTER, boxButtons);
    panelMain.add(BorderLayout.SOUTH, scrollerStatus);
    frame.getContentPane().add(panelMain);

    // TODO: Make below a % of window size if possible
    frame.setSize(600, 300);
    frame.setVisible(true);

    fc.setDialogTitle("Select a config file");
    fc.setAcceptAllFileFilterUsed(false);
    fc.addChoosableFileFilter(filterJson);
  }

  // function: printToTextStatus
  // purpose: prints a message to the textStatus box, with timestamp and line break
  // param: the message to print
  // returns: N/A
  private void printToTextStatus(String message) {
    textStatus.append("* ");
    textStatus
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ZonedDateTime.now()));
    textStatus.append(": ");
    textStatus.append(message);
    textStatus.append("\n");
    // scroll to the bottom
    textStatus.setCaretPosition(textStatus.getDocument().getLength());
  }

  class LoadConfigListener implements ActionListener {

    // function: actionPerformed
    // purpose: performs an action when buttonLoadConfig is clicked
    // param: the event that is captured
    // returns: N/A
    public void actionPerformed(ActionEvent event) {
      // int that determines how user interacted with fc
      int returnValue = fc.showOpenDialog(null);
      if (returnValue == JFileChooser.APPROVE_OPTION) {
        // path to config file
        String configPath = fc.getSelectedFile().getAbsolutePath();
        config = Main.loadElectionConfig(configPath);
        Logger.info("Tabulator is being used via the GUI.");
        if (config == null) {
          printToTextStatus(String.format("ERROR: Unable to load config file: %s", configPath));
        } else {
          printToTextStatus(String.format("Successfully loaded config file: %s", configPath));
        }
      }
    }
  }

  class TabulateListener implements ActionListener {

    // function: actionPerformed
    // purpose: performs an action when buttonTabulate is clicked
    // param: the event that is captured
    // returns: N/A
    public void actionPerformed(ActionEvent event) {
      // String indicating whether or not execution was successful
      printToTextStatus("Starting tabulation...");
      String response = Main.executeTabulation(config);
      printToTextStatus(response);
    }
  }
}
