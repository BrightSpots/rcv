/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose: Wrapper for console and file logging functions
 * All logging and audit output should use these methods to ensure output goes into audit file
 * Version: 1.0
 */

package com.rcv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

class Logger {

  // Logger objects this class wraps
  private static HashMap<String, java.util.logging.Logger> loggers = new HashMap<>();

  // function: log
  // purpose: log output to console and audit file
  // param: msg the message to be logged to console and audit file
  static void log(String msg) {
    for(java.util.logging.Logger logger : loggers.values()) {
      logger.info(msg);
    }
  }

  // function: log
  // purpose: log formatted output to console and audit file
  // format: format string into which object params will be formatted
  // param: obj object to be parsed into format string
  static void log(String format, Object... obj) {
    for(java.util.logging.Logger logger : loggers.values()) {
      logger.info(String.format(format, obj));
    }
  }

  // function: removeLogger
  // purpose: remove an existing logger object
  // param: loggerOutputPath the logger was created with
  static void removeLogger(String loggerOutputPath) {
  	// get or create the logger
    java.util.logging.Logger logger = loggers.get(loggerOutputPath);
    if (logger != null) {
    	// iterate through all handlers and remove them
      for(Handler handler : logger.getHandlers()) {
        logger.removeHandler(handler);
        // if handler is a file handler flush and close it
        if (handler instanceof FileHandler) {
          FileHandler fileHandler = (FileHandler)handler;
          fileHandler.flush();
          fileHandler.close();
        }
      }
    } else {
      Logger.log("Couldn't remove logger:%s",loggerOutputPath);
    }
  }


  // function: addLogger
  // purpose: create and add a new logger object
  // param: loggerOutputPath: file path for logging output
  // param: logToConsole: weather to log to console or not
  // file access: write (existing file will be overwritten)
  // throws: IOException if unable to open loggerOutputPath
  static void addLogger(String loggerOutputPath, boolean logToConsole) throws IOException {
    java.util.logging.Logger logger = loggers.get(loggerOutputPath);
    if (logger != null) {
      Logger.log("Logger has already been added:%s",loggerOutputPath);
    } else {
      // specifies how logging output lines should appear
      LogFormatter formatter = new LogFormatter();

      // get or create the logger
      logger = java.util.logging.Logger.getLogger(loggerOutputPath);
      // remove any existing handlers to prevent duplicate logging
      for (Handler h : logger.getHandlers()) {
        logger.removeHandler(h);
      }

      // add handlers
      if (logToConsole) {
        // create new handler for console logging and add it
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        logger.addHandler(consoleHandler);
      }

      // create new handler for file logging and add it
      FileHandler fileHandler = new FileHandler(loggerOutputPath);
      fileHandler.setFormatter(formatter);
      logger.addHandler(fileHandler);

      // don't log to default logger
      logger.setUseParentHandlers(false);

      // add to cache for later removal
      loggers.put(loggerOutputPath, logger);
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
