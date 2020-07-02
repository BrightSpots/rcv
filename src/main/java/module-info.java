module network.brightspots.rcv {
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires commons.csv;
  requires java.logging;
  requires javafx.controls;
  requires javafx.fxml;
  requires java.xml;
  requires poi.ooxml;
  requires com.fasterxml.jackson.dataformat.xml;
  // enable reflexive calls from network.brightspots.rcv into javafx.fxml
  opens network.brightspots.rcv;
  // our main module
  exports network.brightspots.rcv;
}
