//package com.rcv;
//
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.BorderLayout;
//import java.io.File;
//import javax.swing.*;
//import javax.swing.filechooser.FileNameExtensionFilter;
//import javax.swing.filechooser.FileSystemView;
//
//public class RcvGui {
//  private static JFrame frame;
//
//  public void launch() {
//    frame = new JFrame("Universal RCV Tabulator");
//    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//
//    JButton buttonLoadConfig = new JButton("Load config");
//    buttonLoadConfig.addActionListener(new LoadConfigListener());
//
//    JButton buttonTabulate = new JButton("Tabulate");
//    buttonTabulate.addActionListener(new TabulateListener());
//
//    JPanel panelMain = new JPanel();
//    frame.setContentPane(panelMain);
//
//    frame.getContentPane().add(BorderLayout.CENTER, buttonLoadConfig);
//    frame.getContentPane().add(BorderLayout.SOUTH, buttonTabulate);
//
//    // TODO: Make below a % of window size if possible
//    frame.setSize(800, 800);
//    frame.setVisible(true);
//  }
//
//  class LoadConfigListener implements ActionListener {
//    public void actionPerformed(ActionEvent event) {
//      JFileChooser fc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
//      fc.setDialogTitle("Select a config file");
//      fc.setAcceptAllFileFilterUsed(false);
//      FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON file", "json");
//      fc.addChoosableFileFilter(filter);
//
//      int returnValue = fc.showOpenDialog(null);
//      if (returnValue == JFileChooser.APPROVE_OPTION) {
//        String configPath = fc.getSelectedFile().getAbsolutePath();
//        ElectionConfig config = Main.makeElectionConfig(configPath);
//        //This is where a real application would open the file.
//        log.append("Opening: " + file.getName() + "." + newline);
//      }
//    }
//  }
//
//  class TabulateListener implements ActionListener {
//    public void actionPerformed(ActionEvent event) {
//      System.out.println("Ouch!");
//    }
//  }
//}
