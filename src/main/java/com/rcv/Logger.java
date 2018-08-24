/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
 *
 * Purpose:
 * Wrapper for console and file logging functions. All logging messages including execution,
 * tabulation, and audit information go through this.
 *
 * log message
 *  |
 *  v
 * tabulation logger -> tabulation file (if installed)
 *  |
 *  v
 * default logger -> rcv.log + console
 */

package com.rcv;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

class Logger {

  // execution logger name
  private static final String EXECUTION_LOGGER_NAME = "execution";
  // tabulation logger name
  private static final String TABULATION_LOGGER_NAME = "tabulation";
  // GUI logger name
  private static final String GUI_LOGGER_NAME = "GUI";
  // execution log file name (%g tracks count of log file if additional versions are created)
  private static final String EXECUTION_LOG_FILE_NAME = "rcv_%g.log";
  // first value here is bytes per MB and the second is max MB for the log file
  private static final Integer EXECUTION_LOG_FILE_MAX_SIZE_BYTES = 1000000 * 50;
  // how many execution files to keep
  private static final Integer EXECUTION_LOG_FILE_COUNT = 2;
  // cache for the execution logger
  private static java.util.logging.Logger executionLogger;
  // cache for the tabulation logger
  private static java.util.logging.Logger tabulationLogger;
  // cache for the tabulation logger
  private static java.util.logging.Logger guiLogger;

  // function: setup
  // purpose: initialize logging module
  // throws: IOException if unable to open output log file
  static void setup() throws IOException {
    // create and cache default logger
    executionLogger = java.util.logging.Logger.getLogger(EXECUTION_LOGGER_NAME);
    // remove any loggers the system may have installed
    for (Handler handler : executionLogger.getHandlers()) {
      executionLogger.removeHandler(handler);
    }
    // logPath is where default file logging is written
    // "user.dir" property is the current working directory, i.e. folder from whence the rcv jar
    // was launched
    String logPath = Paths.get(System.getProperty("user.dir"), EXECUTION_LOG_FILE_NAME).toString();
    // formatter specifies how logging output lines should appear
    LogFormatter formatter = new LogFormatter();
    // fileHandler writes formatted strings to file
    FileHandler fileHandler =
        new FileHandler(logPath, EXECUTION_LOG_FILE_MAX_SIZE_BYTES, EXECUTION_LOG_FILE_COUNT, true);
    fileHandler.setFormatter(formatter);
    fileHandler.setLevel(Level.FINE);
    // create a consoleHandler to writes formatted strings to console for debugging
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setFormatter(formatter);
    consoleHandler.setLevel(Level.FINE);
    // add the handlers
    executionLogger.addHandler(consoleHandler);
    executionLogger.addHandler(fileHandler);
    executionLogger.setLevel(Level.FINE);

    // create and cache the tabulation logger object
    // whenever a tabulation happens we will add tabulation-specific file handlers to it
    tabulationLogger = java.util.logging.Logger.getLogger(TABULATION_LOGGER_NAME);
  }

  // logs text to all output loggers
  static void allLogs(Level level, String format, Object... obj) {
    executionLogger.log(level, String.format(format, obj));
    tabulationLogger.log(level, String.format(format, obj));
    if (guiLogger != null) {
      guiLogger.log(level, String.format(format, obj));
    }
  }

  // logs to execution log and GUI if there is one
  static void executionLog(Level level, String format, Object... obj) {
    executionLogger.log(level, String.format(format, obj));
    if (guiLogger != null) {
      guiLogger.log(level, String.format(format, obj));
    }
  }

  // logs to tabulation log and GUI if there is one
  static void tabulationLog(Level level, String format, Object... obj) {
    tabulationLogger.log(level, String.format(format, obj));
    if (guiLogger != null) {
      guiLogger.log(level, String.format(format, obj));
    }
  }

  // audit logging goes to the tabulation file ONLY
  static void auditLog(Level level, String format, Object... obj) {
    tabulationLogger.log(level, String.format(format, obj));
  }

  // logs to GUI console only
  static void guiLog(Level level, String format, Object... obj) {
    guiLogger.log(level, String.format(format, obj));
  }

  // setup logging to the provided text area
  static void addGuiLogging(TextArea textArea) {
    guiLogger = java.util.logging.Logger.getLogger(GUI_LOGGER_NAME);
    LogFormatter formatter = new LogFormatter();
    // TODO: Prevent double-logging to console (i.e. why guiLogger is appearing in console at all?)
    guiLogger.addHandler(
        new Handler() {

          @Override
          public void publish(LogRecord record) {
            if (Platform.isFxApplicationThread()) {
              textArea.appendText(formatter.format(record));
            } else {
              Platform.runLater(() -> textArea.appendText(formatter.format(record)));
            }
          }

          @Override
          public void flush() {
          }

          @Override
          public void close() {
          }
        });
  }

  // function: addTabulationFileLogging
  // purpose: adds file logging for a tabulation run
  // param: loggerOutputPath: file path for tabulationLogger logging output
  // file access: write - existing file will be overwritten
  // throws: IOException if unable to open loggerOutputPath
  static void addTabulationFileLogging(String loggerOutputPath) throws IOException {
    // specifies how logging output lines should appear
    LogFormatter formatter = new LogFormatter();
    // create new handler for file logging and add it
    FileHandler fileHandler = new FileHandler(loggerOutputPath);
    fileHandler.setFormatter(formatter);
    fileHandler.setLevel(Level.FINER);
    // get the tabulationLogger logger and add file handler
    tabulationLogger.addHandler(fileHandler);
    tabulationLogger.setLevel(Level.FINER);
  }

  // function: removeTabulationFileLogging
  // purpose: remove file logging once a tabulation run is complete
  static void removeTabulationFileLogging() {
    // in practice there should only be the one FileHandler we added here
    for (Handler handler : tabulationLogger.getHandlers()) {
      handler.flush();
      handler.close();
      tabulationLogger.removeHandler(handler);
    }
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
      return new Date(record.getMillis())
          + " "
          + record.getLevel().getLocalizedName()
          + ": "
          + formatMessage(record)
          + System.getProperty("line.separator");
    }
  }
}
