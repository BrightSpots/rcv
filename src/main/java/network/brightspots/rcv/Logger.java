/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Wrapper for logging functions.  All logging messages including execution,
 * tabulation, and audit information go through this.
 * Design:
 * log message
 *  |
 *  v
 * Tabulation handler (FINE) -> tabulation "audit" file
 *  When a tabulation is in progress this captures all FINE level logging including audit info.
 *
 * Execution handler (INFO) -> execution file
 *  Captures all INFO level logging for the execution of a session.
 *  "session" could span multiple tabulations in GUI mode.
 *
 * GUI handler (INFO) -> listView
 *  Displays INFO level logging in GUI for user feedback in GUI mode.
 *
 * Default handler -> console
 *  Displays INFO level logging in console for debugging.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

class Logger {

  // execution log file name (%g tracks count of log file if additional versions are created)
  private static final String EXECUTION_LOG_FILE_NAME = "rcv_%g.log";
  // first value here is bytes per MB and the second is max MB for each execution log file
  private static final Integer LOG_FILE_MAX_SIZE_BYTES = 1000000 * 50;
  // how many execution files to keep
  private static final Integer EXECUTION_LOG_FILE_COUNT = 2;
  // how many tabulation files to keep
  // this will effectively keep ALL output from any tabulation
  private static final Integer TABULATION_LOG_FILE_COUNT = 1000;
  private static final java.util.logging.Formatter formatter = new LogFormatter();
  private static java.util.logging.Logger logger;
  private static java.util.logging.FileHandler tabulationHandler;
  private static String tabulationLogPattern;
  private static final List<Label> labelsQueue = new ArrayList<>();

  static void setup() {
    logger = java.util.logging.Logger.getLogger("");
    logger.setLevel(Level.FINE);

    // logPath is where execution file logging is written
    // "user.dir" property is the current working directory, i.e. folder from whence the rcv jar
    // was launched
    Path logPath =
        Paths.get(System.getProperty("user.dir"), EXECUTION_LOG_FILE_NAME).toAbsolutePath();

    // executionHandler writes to the execution log file in current working directory
    try {
      FileHandler executionHandler =
          new FileHandler(
              logPath.toString(), LOG_FILE_MAX_SIZE_BYTES, EXECUTION_LOG_FILE_COUNT, true);
      executionHandler.setLevel(Level.INFO);
      logger.addHandler(executionHandler);
    } catch (IOException exception) {
      warning(
          "Failed to start system logging!\nMake sure you have write access in %s\n%s.",
          System.getProperty("user.dir"), exception);
    }

    for (Handler handler : logger.getHandlers()) {
      handler.setFormatter(formatter);
    }

    info("Execution logging to: %s", logPath.toString().replace("%g", "*"));
  }

  // adds file logging for a tabulation run
  static void addTabulationFileLogging(String outputFolder, String timestampString)
      throws IOException {
    // log file name is: outputFolder + timestamp + log index
    // FileHandler requires % to be encoded as %%.  %g is the log index
    tabulationLogPattern =
        Paths.get(
                outputFolder.replace("%", "%%"),
                String.format("%s_audit_%%g.log", timestampString))
            .toAbsolutePath()
            .toString();

    tabulationHandler =
        new FileHandler(
            tabulationLogPattern,
            LOG_FILE_MAX_SIZE_BYTES, TABULATION_LOG_FILE_COUNT, true);
    tabulationHandler.setFormatter(formatter);
    tabulationHandler.setLevel(Level.FINE);
    logger.addHandler(tabulationHandler);
    info("Tabulation logging to: %s", tabulationLogPattern.replace("%g", "0"));
  }

  // remove file logging once a tabulation run is completed
  static void removeTabulationFileLogging() {
    tabulationHandler.flush();
    tabulationHandler.close();
    logger.removeHandler(tabulationHandler);

    // Find all files we wrote to, and finalize each one
    int index = 0;
    while (true) {
      AuditableFile file = new AuditableFile(tabulationLogPattern
              .replace("%g", String.valueOf(index)));
      if (!file.exists()) {
        break;
      }

      file.finalizeAndHash();
      index++;
    }
  }

  static void fine(String message, Object... obj) {
    log(Level.FINE, message, obj);
  }

  static void info(String message, Object... obj) {
    log(Level.INFO, message, obj);
  }

  static void warning(String message, Object... obj) {
    log(Level.WARNING, message, obj);
  }

  static void severe(String message, Object... obj) {
    log(Level.SEVERE, message, obj);
  }

  private static void log(Level level, String message, Object... obj) {
    // only call format if there are format args provided
    logger.log(level, obj.length > 0 ? String.format(message, obj) : message);
  }

  // add logging to the provided text area for display to user in the GUI
  static void addGuiLogging(ListView<Label> listView) {
    ObservableList<Label> logMessages = FXCollections.observableArrayList();
    listView.setItems(logMessages);

    // Set cell factory to reduce vertical gap
    listView.setCellFactory(param -> new ListCell<Label>() {
      @Override
      protected void updateItem(Label item, boolean empty) {
        // Sets zero padding and updates the cell colors
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
          setText(null);
          setStyle(null);
        } else {
          setGraphic(item);

          // Remove the label padding
          setPadding(new Insets(0, 0, 0, 0));

          // First remove any existing style, which can either be overridden
          // (if it needs a custom background) or can remain as the default.
          setStyle(null);

          // Set the entire background color to the label's background
          // This changes the background from being a text highlight to taking up the whole row
          Background bg = item.getBackground();
          if (bg != null) {
            List<BackgroundFill> fills = item.getBackground().getFills();
            if (!fills.isEmpty()) {
              Paint bgColor = fills.get(0).getFill();
              String hexColor = bgColor.toString().replace("0x", "#");
              setStyle("-fx-background-color: " + hexColor);
            }
          }

          // Change the look when selected -- lighten it up a bit
          // while maintaining the warning/severe color
          if (isSelected()) {
            setBlendMode(BlendMode.SCREEN);
          } else {
            setBlendMode(BlendMode.SRC_OVER);
          }
        }
      }
    });

    java.util.logging.Handler guiHandler =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            if (isLoggable(record)) {
              String msg = getFormatter().format(record);
              Label logLabel = new Label(msg);
              logLabel.setPadding(new Insets(0, 0, 0, 0));
              logLabel.setWrapText(false);

              // Set background color based on log level
              if (record.getLevel() == Level.SEVERE) {
                logLabel.setBackground(Background.fill(Color.DARKRED));
              } else if (record.getLevel() == Level.WARNING) {
                logLabel.setBackground(Background.fill(Color.DARKORANGE));
              }

              // On Right Click, user can copy text
              ContextMenu contextMenu = new ContextMenu();
              MenuItem copyMenuItem = new MenuItem("Copy");
              copyMenuItem.setOnAction(event -> copyToClipboard(logLabel));
              contextMenu.getItems().add(copyMenuItem);
              logLabel.setContextMenu(contextMenu);

              // Rather than adding to the list too many times in a row,
              // we add to a queue and schedule an occasional update to the UI.
              // This prevents the UI from lagging when there are many log messages.
              synchronized (labelsQueue) {
                labelsQueue.add(logLabel);

                // The first item in the queue is the only one that needs to trigger the update.
                if (labelsQueue.size() == 1) {
                  Platform.runLater(this::addFromMainThread);
                }
              }
            }
          }

          private void addFromMainThread() {
            synchronized (labelsQueue) {
              logMessages.addAll(labelsQueue);
              labelsQueue.clear();
            }
            listView.scrollTo(logMessages.size() - 1);
          }

          private void copyToClipboard(Label logLabel) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(logLabel.getText());
            clipboard.setContent(content);
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    guiHandler.setLevel(Level.INFO);
    guiHandler.setFormatter(formatter);
    logger.addHandler(guiHandler);
  }

  // custom LogFormatter is used for all logging
  private static class LogFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date())
          + " "
          + record.getLevel().getLocalizedName()
          + ": "
          + formatMessage(record)
          + System.getProperty("line.separator");
    }
  }
}
