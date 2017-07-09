package com.rcv;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * Created by Jon on 7/8/17.
 */
public class RCVLogger {

  private final static Logger sLogger = Logger.getLogger("RCV");
  private static FileHandler sFileHandler = null;

  public static void log(String msg) {
    sLogger.info(msg);
  }

  public static void log(String var0, Object... var1) {
    assert(sFileHandler != null);
    String formattedString = String.format(var0, var1);
    sLogger.info(formattedString);
  }

  public static void setup(String log_output_path) throws IOException {
    sFileHandler = new FileHandler(log_output_path);
    LogFormatter formatter = new LogFormatter();
    sFileHandler.setFormatter(formatter);
    sLogger.addHandler(sFileHandler);
  }

}
