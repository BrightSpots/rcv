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
import java.util.Date;
import java.util.logging.ErrorManager;
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
  // execution log file name (%g tracks count of log file if additional versions are created)
  private static final String EXECUTION_LOG_FILE_NAME = "rcv_%g.log";
  // first value here is bytes per MB and the second is max MB for the log file
  private static final Integer EXECUTION_LOG_MAX_SIZE_BYTES = 1000000 * 50;
  // how many execution files to keep
  private static final Integer EXECUTION_LOG_FILE_COUNT = 2;
  // cache for the execution logger
  private static java.util.logging.Logger executionLogger;
  // cache for the tabulation logger
  private static java.util.logging.FileHandler tabulationHandler;
  // cache for the tabulation logger
  private static java.util.logging.Handler guiHandler;

  // function: setup
  // purpose: initialize logging module
  // throws: IOException if unable to open output log file
  static void setup() throws IOException {
    // create and cache default logger
    executionLogger = java.util.logging.Logger.getLogger(EXECUTION_LOGGER_NAME);
    executionLogger.setLevel(Level.FINE);

    // set root console handler to use our formatter (just to keep things tidy and consistent)
    java.util.logging.Logger rootLogger = executionLogger.getParent();
    for(Handler handler : rootLogger.getHandlers()) {
      handler.setFormatter(new LogFormatter());
    }

    // logPath is where default file logging is written
    // "user.dir" property is the current working directory, i.e. folder from whence the rcv jar
    // was launched
    Path logPath = Paths.get(System.getProperty("user.dir"),
        EXECUTION_LOG_FILE_NAME).toAbsolutePath();

    // fileHandler writes formatted strings to file
    FileHandler fileHandler =
        new FileHandler(logPath.toString(),
            EXECUTION_LOG_MAX_SIZE_BYTES,
            EXECUTION_LOG_FILE_COUNT,
            true);
    // use our custom formatter
    fileHandler.setFormatter(new LogFormatter());
    fileHandler.setLevel(Level.INFO);
    executionLogger.addHandler(fileHandler);
    // log results
    executionLog(Level.INFO,"RCV Tabulator Logging execution to %s", logPath.toString());
  }

  // function: addTabulationFileLogging
  // purpose: adds file and console logging for a tabulation run
  // param: loggerOutputPath: file path for executionLogger logging output
  // file access: write - existing file will be overwritten
  // throws: IOException if unable to open loggerOutputPath
  static void addTabulationFileLogging(String loggerOutputPath) throws IOException {
    // create file handler at FINE level for audit logging
    tabulationHandler = new FileHandler(loggerOutputPath);
    // use our custom formatter
    tabulationHandler.setFormatter(new LogFormatter());
    tabulationHandler.setLevel(Level.FINE);
    executionLogger.addHandler(tabulationHandler);
  }

  // function: removeTabulationFileLogging
  // purpose: remove file logging once a tabulation run is complete
  static void removeTabulationFileLogging() {
    executionLogger.removeHandler(tabulationHandler);
  }

  // logs to execution log and GUI if there is one
  static void executionLog(Level level, String format, Object... obj) {
    executionLogger.log(level, String.format(format, obj));
  }

  // setup logging to the provided text area for display to user in the GUI
  static void addGuiLogging(TextArea textArea) {
    // custom handler overrides publish to post text to the GUI
    guiHandler = new Handler() {
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
    guiHandler.setFormatter(new LogFormatter());
    executionLogger.addHandler(guiHandler);
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
