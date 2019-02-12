package com.rcv;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;



public class CandidateExcludedValueFactory implements
    Callback<TableColumn.CellDataFeatures<RawContestConfig.Candidate, CheckBox>,
        ObservableValue<CheckBox>>
{
  @Override
  public ObservableValue<CheckBox> call(TableColumn.CellDataFeatures<RawContestConfig.Candidate, CheckBox> param) {
    RawContestConfig.Candidate candidate = param.getValue();
    CheckBox checkBox = new CheckBox();
    checkBox.selectedProperty().setValue(candidate.isExcluded());
    checkBox.selectedProperty().addListener((ov, old_val, new_val) -> {
      candidate.setExcluded(new_val);
    });
    return new SimpleObjectProperty<>(checkBox);
  }
}