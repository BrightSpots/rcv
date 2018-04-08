package com.rcv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

class RcvGui {
  private static ElectionConfig config;
  private static final JLabel labelStatus;
  private static final JFileChooser fc;
  private static final JFrame frame;
  private static final FileNameExtensionFilter filterJson;

  static {
    labelStatus = new JLabel("Welcome to the Universal RCV Tabulator!");
    fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
    frame = new JFrame("Universal RCV Tabulator");
    filterJson = new FileNameExtensionFilter("JSON file", "json");
  }

  void launch() {
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    JButton buttonLoadConfig = new JButton("Load config");
    buttonLoadConfig.addActionListener(new LoadConfigListener());

    JButton buttonTabulate = new JButton("Tabulate");
    buttonTabulate.addActionListener(new TabulateListener());

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
    public void actionPerformed(ActionEvent event) {
      int returnValue = fc.showOpenDialog(null);
      if (returnValue == JFileChooser.APPROVE_OPTION) {
        String configPath = fc.getSelectedFile().getAbsolutePath();
        config = Main.makeElectionConfig(configPath);
        if (config == null) {
          labelStatus.setText(String.format("ERROR: Unable to load config file: %s", configPath));
        } else {
          labelStatus.setText(String.format("Successfully loaded config file: %s", configPath));
        }
      }
    }
  }

  class TabulateListener implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      String response = Main.executeTabulation(config);
      labelStatus.setText(response);
    }
  }
}
