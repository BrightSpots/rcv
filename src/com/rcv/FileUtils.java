/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose: Static utils for simple file manipulation
 * Version: 1.0
 */

package com.rcv;

import java.io.File;

class FileUtils {

  static String buildPath(String dir, String file) {
    // path is the value we're returning
    String path = file;
    if (dir != null) {
      path = new File(dir, file).toString();
    }
    return path;
  }

  static void createOutputDirectory(String dir) throws UnableToCreateDirectoryException {
    if (dir != null) {
      // dirFile is the File object for dir
      File dirFile = new File(dir);
      if (!dirFile.exists() && !dirFile.mkdirs()) {
        throw new UnableToCreateDirectoryException("Unable to create output directory: " + dir);
      }
    }
  }

  static class UnableToCreateDirectoryException extends Exception {

    UnableToCreateDirectoryException(String message) {
      super(message);
    }
  }
}
