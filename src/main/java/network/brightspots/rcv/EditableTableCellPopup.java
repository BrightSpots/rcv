/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Generates an editable table cell for a list of items. Displays a semicolon-separated
 * list. On click, it pops up a textarea with one item per line. On close, it saves the data.
 * Design: The cell is actually a 100%-width unstyled button. The popup is a modal dialog which
 * cancels the edit on hitting the "X" or Cancel, and saves on hitting "Save".
 * Conditions: Runs in GUI mode.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** A table cell that pops up a modal dialog to edit a list of strings. */
public class EditableTableCellPopup<T> extends TableCell<T, List<String>> {
  private final Button cellButton;

  /** Set up a cell that will pop up a modal dialog to edit a list of strings. */
  public EditableTableCellPopup() {
    super();

    cellButton = new Button("[placeholder]"); // Placeholder text is never seen by the user
    cellButton.setTooltip(new Tooltip("Enter one per line"));
    cellButton.setOnAction(event -> popUpModal());

    // Set a very wide width so clicking on whitespace triggers the action
    cellButton.setPrefWidth(999);
  }

  @Override
  public void commitEdit(List<String> item) {
    if (isEditing()) {
      super.commitEdit(item);
    } else {
      TableView table = getTableView();
      if (table != null) {
        // Populate an edit event for the correct cell
        TablePosition position =
            new TablePosition(getTableView(), getTableRow().getIndex(), getTableColumn());
        CellEditEvent editEvent =
            new CellEditEvent(table, position, TableColumn.editCommitEvent(), item);
        Event.fireEvent(getTableColumn(), editEvent);
      }

      // Update the button text with the new data
      updateItem(item, false);

      // Cancel editing by passing a negative row
      if (table != null) {
        table.edit(-1, null);
      }
    }
  }

  @Override
  public void updateItem(List<String> item, boolean empty) {
    super.updateItem(item, empty);
    cellButton.getStyleClass().clear();
    setEditable(false);

    if (empty) {
      setGraphic(null);
    } else {
      setGraphic(cellButton);
    }

    cellButton.setText(safeJoin(";  ", item));
  }

  private String safeJoin(String delimiter, Iterable<? extends String> strings) {
    return strings == null ? "" : String.join(delimiter, strings);
  }

  private void popUpModal() {
    // Three simple items in a vertical layout
    VBox verticalLayout = new VBox();
    verticalLayout.setSpacing(15);
    verticalLayout.setPadding(new Insets(5, 5, 5, 5));

    // One string per line
    Text title = new Text("Enter one per line");
    verticalLayout.getChildren().add(title);

    // Set up the text area
    TextArea textArea = new TextArea();
    textArea.setPrefHeight(200);
    verticalLayout.getChildren().add(textArea);

    // Set up the Save and Cancel button
    Button cancelButton = new Button("Cancel");
    cancelButton.setCancelButton(true);
    Button saveButton = new Button("Save");
    saveButton.setDefaultButton(true);

    // Add to a horizontal layout
    HBox buttons = new HBox();
    buttons.setSpacing(50);
    buttons.getChildren().add(cancelButton);
    buttons.getChildren().add(saveButton);
    verticalLayout.getChildren().add(buttons);
    buttons.setAlignment(Pos.CENTER);

    // Hook up cancel button to close the popup without saving
    cancelButton.setOnAction(
        event1 -> {
          Stage stage = (Stage) cancelButton.getScene().getWindow();
          stage.close();
        });

    // Hook up done button to save the edit
    saveButton.setOnAction(
        event1 -> {
          ObservableList<String> observableList =
              FXCollections.observableArrayList(Utils.splitByNewline(textArea.getText()));
          observableList.addListener(
              (ListChangeListener<String>) c -> cellButton.setText(safeJoin("; ", c.getList())));
          commitEdit(observableList);

          Stage stage = (Stage) saveButton.getScene().getWindow();
          stage.close();
        });

    // Create the stage and hook it up to the done button, and don't let user click away
    Stage stage = new Stage();
    stage.initModality(Modality.APPLICATION_MODAL);

    // Add it all to a little popup
    Scene scene = new Scene(verticalLayout, 200, 250);
    stage.setScene(scene);

    // Populate text area with the current item
    textArea.setText(safeJoin("\n", getItem()));
    textArea.selectAll();

    // Pop up, and save data on close
    stage.show();
  }
}
