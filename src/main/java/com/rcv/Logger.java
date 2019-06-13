/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2017-2019 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Purpose:
 * Wrapper for console and file logging functions. All logging messages including execution,
 * tabulation, and audit information go through this.
 *
 * log message
 *  |
 *  v
 * tabulation handler (FINE) -> tabulation "audit" file
 *  when a tabulation is in progress this captures all FINE level logging including audit info
 *
 * execution handler (INFO) -> execution file
 *  captures all INFO level logging for the execution of a session
 *  "session" could span multiple tabulations in GUI mode
 *
 * GUI handler (INFO) -> textArea
 *  displays INFO level logging in GUI for user feedback in GUI mode
 *
 * default handler -> console
 *  displays INFO level logging in console for debugging
 *
 */

package com.rcv;

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
  // cache for custom formatter
  private static final java.util.logging.Formatter formatter = new LogFormatter();
  // cache for logger
  private static java.util.logging.Logger logger;
  // cache for tabulation handler
  private static java.util.logging.FileHandler tabulationHandler;

  // function: setup
  // purpose: initialize logging module
  // throws: IOException if unable to open output log file
  static void setup() throws IOException {
    // cache default logger
    logger = java.util.logging.Logger.getLogger("");
    logger.setLevel(Level.FINE);

    // logPath is where execution file logging is written
    // "user.dir" property is the current working directory, i.e. folder from whence the rcv jar
    // was launched
    Path logPath =
        Paths.get(System.getProperty("user.dir"), EXECUTION_LOG_FILE_NAME).toAbsolutePath();

    // executionHandler writes to the execution log file
    FileHandler executionHandler =
        new FileHandler(
            logPath.toString(), LOG_FILE_MAX_SIZE_BYTES, EXECUTION_LOG_FILE_COUNT, true);
    executionHandler.setLevel(Level.INFO);
    logger.addHandler(executionHandler);

    // use our custom formatter for all installed handlers
    for (Handler handler : logger.getHandlers()) {
      handler.setFormatter(formatter);
    }

    // log results
    log(Level.INFO, "RCV Tabulator logging execution to: %s", logPath.toString());
  }

  // function: addTabulationFileLogging
  // purpose: adds file logging for a tabulation run
  // param: loggerOutputPath: file path for logger logging output
  // file access: write - existing file will be overwritten
  // throws: IOException if unable to open loggerOutputPath
  static void addTabulationFileLogging(String outputPath) throws IOException {
    // create file handler at FINE level (we have detailed audit logging we want to capture)
    // and use our custom formatter
    tabulationHandler =
        new FileHandler(outputPath, LOG_FILE_MAX_SIZE_BYTES, TABULATION_LOG_FILE_COUNT, true);
    tabulationHandler.setFormatter(formatter);
    tabulationHandler.setLevel(Level.FINE);
    logger.addHandler(tabulationHandler);
  }

  // function: removeTabulationFileLogging
  // purpose: remove file logging once a tabulation run is complete
  static void removeTabulationFileLogging() {
    tabulationHandler.flush();
    tabulationHandler.close();
    logger.removeHandler(tabulationHandler);
  }

  // logs to default logger
  static void log(Level level, String format, Object... obj) {
    logger.log(level, String.format(format, obj));
  }

  // add logging to the provided text area for display to user in the GUI
  static void addGuiLogging(TextArea textArea) {
    // custom handler logs text to the GUI
    java.util.logging.Handler guiHandler =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            if (!isLoggable(record)) {
              return;
            }
            String msg = getFormatter().format(record);
            // if we are executing on the GUI thread we can post immediately
            // e.g. responses to button clicks
            if (Platform.isFxApplicationThread()) {
              textArea.appendText(msg);
            } else {
              // if not currently on GUI thread schedule the text update to run on the GUI thread
              // e.g. tabulation thread updates
              Platform.runLater(() -> textArea.appendText(msg));
            }
          }

          // nothing to do here
          @Override
          public void flush() {
          }

          // nothing to do here
          @Override
          public void close() {
          }
        };
    guiHandler.setLevel(Level.INFO);
    guiHandler.setFormatter(formatter);
    logger.addHandler(guiHandler);
  }

  // custom LogFormatter class for log output string formatting
  // extends the default logging Formatter class
  private static class LogFormatter extends Formatter {

    // function: format
    // purpose: overrides the format function with our custom formatter
    // param: record the LogRecord to be formatted into a string for output
    // returns: the formatted string for output
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
