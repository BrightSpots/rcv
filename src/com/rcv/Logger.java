/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose: Wrapper for console and file logging functions
 * All logging messages including execution, tabulation, and audit information go through this
 * Version: 1.0
 *
 * log message
 *  |
 *  v
 * tabulation logger -> tabulation file (if installed)
 *  |
 *  v
 * default logger -> rcv.log + console
 *
 *
 */

package com.rcv;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

class Logger {

  // cache for the default logger
  private static java.util.logging.Logger defaultLogger;
  // cache for the tabulation logger
  private static java.util.logging.Logger tabulationLogger;
  // tabulation logger name: dot "." parents it to default logger so all messages will propagate
  private static String TABULATION_LOGGER_NAME = ".tabulation";
  // execution log file name
  private static String DEFAULT_FILE_NAME = "rcv.log";

  // function: setup
  // purpose: initialize logging module
  // throws: IOException if unable to open output log file
  static void setup() throws IOException {
    // create and cache default logger
    defaultLogger = java.util.logging.Logger.getLogger("");
    // remove any loggers the system may have installed
    for (Handler handler : defaultLogger.getHandlers()) {
      defaultLogger.removeHandler(handler);
    }
    // logPath is where default file logging is written
    // "user.dir" property is the current working directory, i.e. folder from whence the rcv jar
    // was launched
    String logPath = Paths.get(System.getProperty("user.dir"), DEFAULT_FILE_NAME).toString();
    // formatter specifies how logging output lines should appear
    LogFormatter formatter = new LogFormatter();
    // fileHandler writes formatted strings to file
    FileHandler fileHandler = new FileHandler(logPath, true);
    fileHandler.setFormatter(formatter);
    // create a consoleHandler to writes formatted strings to console for debugging
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setFormatter(formatter);
    // add the  handlers
    defaultLogger.addHandler(consoleHandler);
    defaultLogger.addHandler(fileHandler);

    // create and cache the tabulation logger object
    // whenever a tabulation happens we will add tabulation-specific file handlers here
    tabulationLogger = java.util.logging.Logger.getLogger(TABULATION_LOGGER_NAME);
  }

  // function: info
  // purpose: log formatted output to console and audit file at INFO level
  // format: format string into which object params will be formatted
  // param: obj object to be parsed into format string
  static void info(String format, Object... obj) {
    tabulationLogger.info(String.format(format, obj));
  }

  // function: warn
  // purpose: log formatted output to console and audit file at WARNING level
  // param: format string into which object params will be formatted
  // param: obj object to be parsed into format string
  static void warn(String format, Object... obj) {
    tabulationLogger.warning(String.format(format, obj));
  }

  // function: severe
  // purpose: log formatted output to console and audit file at SEVERE level
  // param: format string into which object params will be formatted
  // param: obj object to be parsed into format string
  static void severe(String format, Object... obj) {
    tabulationLogger.severe(String.format(format, obj));
  }

  // function: addTabulationFileLogging
  // purpose: adds file logging for a tabulation run
  // param: loggerOutputPath: file path for tabulationLogger logging output
  // file access: write - existing file will be overwritten
  // throws: IOException if unable to open loggerOutputPath
  static void addTabulationFileLogging(String loggerOutputPath) throws IOException {
    // create new handler for file logging and add it
    FileHandler fileHandler = new FileHandler(loggerOutputPath);
    // specifies how logging output lines should appear
    LogFormatter formatter = new LogFormatter();
    fileHandler.setFormatter(formatter);
    // get the tabulationLogger logger and add file handler
    tabulationLogger.addHandler(fileHandler);
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
      return new Date(record.getMillis()) +
          " " +
          record.getLevel().getLocalizedName() +
          ": " +
          formatMessage(record) +
          System.getProperty("line.separator");
    }
  }
}
