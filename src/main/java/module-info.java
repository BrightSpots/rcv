module com.rcv {
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires commons.csv;
  requires java.logging;
  requires javafx.controls;
  requires javafx.fxml;
  requires java.xml;
  requires poi.ooxml;
  // enable reflexive calls from com.rcv into javafx.fxml
  opens com.rcv to javafx.fxml;
  // our main module
  exports com.rcv;
}
