/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
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
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static network.brightspots.rcv.Utils.isNullOrBlank;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class FileUtils {

  // cache location for finding and creating user files and folders
  private static String initialDirectory = null;

  private FileUtils() {}

  // When working with filesystem throughout RCTab
  // remember the latest folder the user was working in
  // fallback to current working directory if that hasn't been set
  static String getInitialDirectory() {
    String result;
    if (initialDirectory == null) {
      result = System.getProperty("user.dir");
    } else if (Files.isDirectory(new File(initialDirectory).toPath())) {
      result = initialDirectory;
    } else {
      Logger.info("Most recent .config load/save was done at path %s."
                      + " This path no longer exists. Falling back to current working directory.",
              initialDirectory);
      result = System.getProperty("user.dir");
    }

    return result;
  }

  static void setInitialDirectory(String initialDirectory) {
    FileUtils.initialDirectory = initialDirectory;
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

  static byte[] getHashBytes(File file, String algorithm) {
    byte[] bytesToReturn;
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm);

      try (InputStream is = Files.newInputStream(file.toPath())) {
        try (DigestInputStream hashingStream = new DigestInputStream(is, digest)) {
          while (hashingStream.readNBytes(1024).length > 0) {
            // Read in 1kb chunks -- don't need to do anything in the body here
          }
        }
      } catch (IOException e) {
        Logger.severe("Failed to read file: %s", file.getAbsolutePath());
        bytesToReturn = "[hash not available]".getBytes(StandardCharsets.UTF_8);
      }

      bytesToReturn = digest.digest();
    } catch (NoSuchAlgorithmException e) {
      Logger.severe("Failed to get the %s algorithm".formatted(algorithm));
      bytesToReturn = "[hash not available]".getBytes(StandardCharsets.UTF_8);
    }


    return bytesToReturn;
  }

  static class UnableToCreateDirectoryException extends Exception {

    UnableToCreateDirectoryException(String message) {
      super(message);
    }
  }
}
