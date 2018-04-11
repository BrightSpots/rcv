/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose: Front-end graphical user interface (GUI)
 * Version: 1.0
 */

package com.rcv;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

class RcvGui {

  // currently-loaded tabulator config
  private static ElectionConfig config;
  // label which communicates the status of the tabulator's operations
  private static final JLabel labelStatus;
  // FileChooser used as a dialog box for loading a config
  private static final JFileChooser fc;
  // main frame to render GUI elements
  private static final JFrame frame;
  // filter used by FileChooser to restrict it to loading JSON files
  private static final FileNameExtensionFilter filterJson;

  static {
    labelStatus = new JLabel("Welcome to the Universal RCV Tabulator!");
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

    // button that summons the dialog box to load the config
    JButton buttonLoadConfig = new JButton("Load config");
    buttonLoadConfig.addActionListener(new LoadConfigListener());

    // button that starts tabulation after config is loaded
    JButton buttonTabulate = new JButton("Tabulate");
    buttonTabulate.addActionListener(new TabulateListener());

    // main panel to render GUI elements
    JPanel panelMain = new JPanel();
    frame.setContentPane(panelMain);

    frame.getContentPane().add(BorderLayout.NORTH, buttonLoadConfig);
    frame.getContentPane().add(BorderLayout.CENTER, buttonTabulate);
    frame.getContentPane().add(BorderLayout.SOUTH, labelStatus);

    // TODO: Make below a % of window size if possible
    frame.setSize(800, 800);
    frame.setVisible(true);

    fc.setDialogTitle("Select a config file");
    fc.setAcceptAllFileFilterUsed(false);
    fc.addChoosableFileFilter(filterJson);
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
        Logger.log("Tabulator is being used via the GUI.");
        if (config == null) {
          labelStatus.setText(String.format("ERROR: Unable to load config file: %s", configPath));
        } else {
          labelStatus.setText(String.format("Successfully loaded config file: %s", configPath));
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
      String response = Main.executeTabulation(config);
      labelStatus.setText(response);
    }
  }
}
