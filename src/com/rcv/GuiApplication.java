package com.rcv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuiApplication extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("GuiMainLayout.fxml"));
    primaryStage.setTitle("Universal RCV Tabulator");
    primaryStage.setScene(new Scene(root));
    primaryStage.show();
  }
}
