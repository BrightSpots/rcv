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
 * GUI handler (INFO) -> textArea
 *  Displays INFO level logging in GUI for user feedback in GUI mode.
 *
 * Default handler -> console
 *  Displays INFO level logging in console for debugging.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

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
  // The audit logs include the hash of the file in the filename. While the file
  // is still open, that can't happen -- so use this string in the filename in the meantime.
  private static final String inProgressHashValue = "hash-tbd";

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
    // log file name is: outputFolder + timestamp + log index + hash + .log
    // FileHandler requires % to be encoded as %%.
    // %g is the log index, and %h is the hash
    tabulationLogPattern =
        Paths.get(
                outputFolder.replace("%", "%%"),
                String.format("%s_audit_%%g_%%h.log", timestampString))
            .toAbsolutePath()
            .toString();

    tabulationHandler =
        new FileHandler(
            tabulationLogPattern.replace("%h", inProgressHashValue),
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

    int index = 0;
    while (true) {
      File fileWithoutHash = new File(tabulationLogPattern
              .replace("%g", String.valueOf(index))
              .replace("%h", inProgressHashValue));
      if (!fileWithoutHash.exists()) {
        break;
      }

      // Rename file to include hash
      String hash = Utils.getHash(fileWithoutHash);
      File fileWithHash = new File(tabulationLogPattern
              .replace("%g", String.valueOf(index))
              .replace("%h", hash));
      boolean moveSucceeded = fileWithoutHash.renameTo(fileWithHash);
      if (!moveSucceeded) {
        severe("Failed to rename %s to %s", fileWithoutHash.getAbsolutePath(), fileWithHash);
      } else {
        boolean readOnlySucceeded = fileWithHash.setReadOnly();
        if (!readOnlySucceeded) {
          warning("Failed to set file to read-only: %s", fileWithHash.getAbsolutePath());
        }
      }

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
  static void addGuiLogging(TextArea textArea) {
    java.util.logging.Handler guiHandler =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            if (isLoggable(record)) {
              String msg = getFormatter().format(record);
              // if we are executing on the GUI thread we can post immediately (e.g. button clicks)
              // otherwise schedule the text update to run on the GUI thread
              if (Platform.isFxApplicationThread()) {
                textArea.appendText(msg);
              } else {
                Platform.runLater(() -> textArea.appendText(msg));
              }
            }
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
