/*
  Created by Jonathan Moldover and Louis Eisenberg
  Copyright 2018 Bright Spots
  Purpose: Wrapper for console and file logging functions
  All logging and audit output should use these methods to ensure output goes into audit file
  Version: 1.0
 */

package com.rcv;

import java.io.IOException;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class Logger {

  // Logger object this class wraps
  private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("RCV");

  // function: log
  // purpose: log output to console and audit file
  // param: msg the message to be logged to console and audit file
  static void log(String msg) {
    logger.info(msg);
  }

  // function: log
  // purpose: log formatted output to console and audit file
  // format: format string into which object params will be formatted
  // param: obj object to be parsed into format string
  static void log(String format, Object... obj) {
    logger.info(String.format(format, obj));
  }

  // function: setup
  // purpose: initialize logging module
  // param: file_output_path: File path for audit output
  // file access: write (existing file will be overwritten)
  // throws: IOException if unable to open file_output_path
  static void setup(String file_output_path) throws IOException {
    // specifies how logging output lines should appear
    LogFormatter formatter = new LogFormatter();
    // controls logging file output filters
    FileHandler fileHandler = new FileHandler(file_output_path);
    fileHandler.setFormatter(formatter);
    // controls logging console output filters
    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setFormatter(formatter);
    // remove any existing handlers to prevent duplicate logging
    for (Handler h : logger.getHandlers()) {
      logger.removeHandler(h);
    }
    // add new handlers
    logger.addHandler(consoleHandler);
    logger.addHandler(fileHandler);
    logger.setUseParentHandlers(false);
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
