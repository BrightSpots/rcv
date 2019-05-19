module rcv.main {
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.databind;
  requires commons.csv;
  requires java.logging;
  requires javafx.controls;
  requires javafx.fxml;
  requires java.xml;
  requires poi.ooxml;
  //  TODO: Verify that below is necessary; maybe try deleting and running
  opens com.rcv to
      javafx.fxml;

  exports com.rcv;
}
