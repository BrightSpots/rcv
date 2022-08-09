/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Utilities for file and folder creation.  Implements the notion of a user directory to
 * enable relative paths in config files.
 * Design: Mostly just a namespace to organize file-related utilities.
 * Conditions: Always.
 * Version history: Version 1.0.
 * Complete revision history is available at: https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.File;

final class FileUtils {

  // cache location for finding and creating user files and folders
  private static String userDirectory = null;

  private FileUtils() {
  }

  // return userDirectory if it exists
  // fallback to current working directory
  static String getUserDirectory() {
    return userDirectory == null ? System.getProperty("user.dir") : userDirectory;
  }

  static void setUserDirectory(String userDirectory) {
    FileUtils.userDirectory = userDirectory;
  }

  static void createOutputDirectory(String dir) throws UnableToCreateDirectoryException {
    if (!isNullOrBlank(dir)) {
      File dirFile = new File(dir);
      if (!dirFile.exists() && !dirFile.mkdirs()) {
        Logger.severe(
            "Failed to create output directory: %s\n" + "Check the directory name and permissions.",
            dir);
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
