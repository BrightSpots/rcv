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
 * Static utils for simple file manipulation.
 */

package com.rcv;

import java.io.File;
import java.nio.file.Paths;

class FileUtils {

  // cache location for finding and creating user files and folders
  private static String userDirectory = null;

  // function: setUserDirectory
  // param: userDirectory default folder for finding and creating files and folders
  static void setUserDirectory(String userDirectory) {
    FileUtils.userDirectory = userDirectory;
  }

  // function: getUserDirectory
  // returns: returns root for loading and saving user files
  static String getUserDirectory() {
    // return userDirectory if it exists
    // fallback to current working directory
    return userDirectory == null ? System.getProperty("user.dir") : userDirectory;
  }

  // function: resolveUserPath
  // purpose: given input user path returns an absolute path for use in File IO
  // param: user path
  // returns: resolved path
  static String resolveUserPath(String userPath) {
    // create File for IO operations
    File userFile = new File(userPath);
    // resolvedPath will be returned to caller
    String resolvedPath;
    // if input path is not absolute prepend the userDirectory location
    if (userFile.isAbsolute()) {
      resolvedPath = userFile.getAbsolutePath();
    } else {
      resolvedPath = Paths.get(FileUtils.getUserDirectory(), userPath).toAbsolutePath().toString();
    }
    return resolvedPath;
  }

  static void createOutputDirectory(String dir) throws UnableToCreateDirectoryException {
    if (dir != null && !dir.isEmpty()) {
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
