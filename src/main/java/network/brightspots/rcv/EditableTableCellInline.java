/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Generates an editable table cell that commits an edit when focus is lost, and
 * cancels an edit when escape is pressed.
 * Design: A single cell to be used in an editable table. This file copies code and comments from
 * JavaFX's TextFieldTableCell and CellUtils and adapts it. Since TextFieldTableCell uses a private
 * TextField and CellUtils is package protected, we cannot easily extend those without duplicating
 * some code.
 * Conditions: Runs in GUI mode.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * A class containing a {@link TableCell} implementation that draws a {@link TextField} node inside
 * the cell. If the TextField is left, the value is committed.
 */
public class EditableTableCellInline<S, T> extends TableCell<S, T> {
  private static final List<Control> controlsToDisableWhileEditing = new ArrayList<>();
  private static final List<Tab> tabsToDisableWhileEditing = new ArrayList<>();
  private static final List<Node> nodesToDisableWhileEditing = new ArrayList<>();
  private final ObjectProperty<StringConverter<T>> converter =
      new SimpleObjectProperty<>(this, "converter");
  private TextField textField;
  private boolean escapePressed = false;
  private TablePosition<S, ?> tablePos = null;

  /**
   * Provides a TextFieldTableCell that allows editing of the cell content with any type. This
   * method will only work on any type, but you'll need to provide a StringConverter.
   */
  public EditableTableCellInline(StringConverter<T> converter) {
    this.getStyleClass().add("text-field-table-cell");
    setConverter(converter);
  }

  /**
   * Provides a TextField that allows editing of the cell content when the cell is double-clicked,
   * or when TableView.edit() is called. This method will only work on TableColumn instances which
   * are of type String.
   */
  public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
    return forTableColumn(new DefaultStringConverter());
  }

  /**
   * Provides a TextField that allows editing of the cell content when the cell is double-clicked,
   * or when TableView.edit() is called. This method will only work on any type, but you'll need to
   * provide a StringConverter.
   */
  public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
      final StringConverter<T> converter) {
    return list -> new EditableTableCellInline<>(converter);
  }

  /** Prevent the user from leaving the UI without committing. */
  public static void lockWhileEditing(Control control) {
    controlsToDisableWhileEditing.add(control);
  }

  /** Prevent the user from leaving the UI without committing. */
  public static void lockWhileEditing(Tab tab) {
    tabsToDisableWhileEditing.add(tab);
  }

  /** Prevent the user from leaving the UI without committing. */
  public static void lockWhileEditing(Node node) {
    nodesToDisableWhileEditing.add(node);
  }

  /** Disables or enables both controls and tabs requested by lockWhileEditing. */
  private static void setDisabledForEditing(boolean doLock) {
    for (Control control : controlsToDisableWhileEditing) {
      control.setDisable(doLock);
    }
    for (Tab tab : tabsToDisableWhileEditing) {
      if (!tab.isSelected()) {
        tab.setDisable(doLock);
      }
    }
    for (Node node : nodesToDisableWhileEditing) {
      node.setDisable(doLock);
    }
  }

  private ObjectProperty<StringConverter<T>> converterProperty() {
    return converter;
  }

  private StringConverter<T> getConverter() {
    return converterProperty().get();
  }

  private void setConverter(StringConverter<T> value) {
    converterProperty().set(value);
  }

  @Override
  public void startEdit() {
    if (isEditable() && getTableView().isEditable() && getTableColumn().isEditable()) {
      super.startEdit();

      setDisabledForEditing(true);

      if (isEditing()) {
        // Update members
        escapePressed = false;
        tablePos = getTableView().getEditingCell();

        // Update the textField
        if (textField == null) {
          textField = getTextField();
        }
        textField.setText(getItemText());

        // Update the cell graphics
        setText(null);
        setGraphic(textField);
        textField.selectAll();

        // requesting focus so that key input can immediately go into the
        // TextField (see RT-28132)
        textField.requestFocus();
      }
    }
  }

  @Override
  public void commitEdit(T newValue) {
    if (isEditing()) {
      final TableView<S> table = getTableView();
      if (table != null) {
        // Inform the TableView of the edit being ready to be committed
        CellEditEvent editEvent =
            new CellEditEvent(table, tablePos, TableColumn.editCommitEvent(), newValue);

        Event.fireEvent(getTableColumn(), editEvent);
      }

      // We need to setEditing(false):
      super.cancelEdit(); // this fires an invalid EditCancelEvent

      // Update the item within this cell, so that it represents the new value
      updateItem(newValue, false);

      // Reset the editing cell on the TableView
      if (table != null) {
        table.edit(-1, null);
      }
    }

    setDisabledForEditing(false);
  }

  @Override
  public void cancelEdit() {
    if (escapePressed) {
      // this is a cancel event after escape key
      super.cancelEdit();
      setText(getItemText()); // restore the original text in the view
    } else {
      // this is not a cancel event after escape key
      // we interpret it as commit.
      String newText = textField.getText(); // get the new text from the view
      this.commitEdit(getConverter().fromString(newText)); // commit the new text to the model
    }

    setDisabledForEditing(false);

    setGraphic(null); // stop editing with TextField
  }

  @Override
  public void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    if (isEmpty()) {
      setText(null);
      setGraphic(null);
    } else if (isEditing()) {
      if (textField != null) {
        textField.setText(getItemText());
      }
      setText(null);
      setGraphic(textField);
    } else {
      setText(getItemText());
      setGraphic(null);
    }
  }

  private TextField getTextField() {
    final TextField textField = new TextField(getItemText());
    // Use onAction here rather than onKeyReleased (with check for Enter),
    // as otherwise we encounter RT-34685
    textField.setOnAction(
        event -> {
          if (converter == null) {
            throw new IllegalStateException(
                "Attempting to convert text input into Object, but provided "
                    + "StringConverter is null. Be sure to set a StringConverter "
                    + "in your cell factory.");
          }
          this.commitEdit(getConverter().fromString(textField.getText()));
          event.consume();
        });

    textField.setOnKeyPressed(t -> escapePressed = t.getCode() == KeyCode.ESCAPE);
    textField.setOnKeyReleased(t -> escapePressed = t.getCode() == KeyCode.ESCAPE);

    return textField;
  }

  private String getItemText() {
    return getConverter() == null
        ? getItem() == null ? "" : getItem().toString()
        : getConverter().toString(getItem());
  }
}
