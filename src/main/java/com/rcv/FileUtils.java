/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (C) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
