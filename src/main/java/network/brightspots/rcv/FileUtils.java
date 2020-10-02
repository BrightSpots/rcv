/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2020 Bright Spots Developers.
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
 */

/*
 * Purpose:
 * Static utils for simple file manipulation.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.File;

final class FileUtils {

  // cache location for finding and creating user files and folders
  private static String userDirectory = null;

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

  private FileUtils() {
  }
}
