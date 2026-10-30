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
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

final class FileUtils {

  // placeholder hash value used when a file can't be read or the algorithm is unavailable
  private static final byte[] HASH_NOT_AVAILABLE =
      "[hash not available]".getBytes(StandardCharsets.UTF_8);

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
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      Logger.severe("Failed to get the %s algorithm".formatted(algorithm));
      return HASH_NOT_AVAILABLE;
    }

    try (InputStream is = Files.newInputStream(file.toPath());
        DigestInputStream hashingStream = new DigestInputStream(is, digest)) {
      while (hashingStream.readNBytes(1024).length > 0) {
        // Read in 1kb chunks -- don't need to do anything in the body here
      }
    } catch (IOException e) {
      Logger.severe("Failed to read file: %s", file.getAbsolutePath());
      return HASH_NOT_AVAILABLE;
    }

    return digest.digest();
  }

  /**
   * Computes a single, reproducible hash covering every file in a directory. Each regular file
   * under the directory (searched recursively) is hashed with the given algorithm, the files are
   * sorted alphabetically by their path relative to the directory, the raw per-file hashes are
   * concatenated in that order, and the concatenation is hashed with the same algorithm.
   *
   * @return the combined hash and the number of files it covers.
   */
  static DirectoryHashResult getDirectoryHashBytes(File directory, String algorithm) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      Logger.severe("Failed to get the %s algorithm".formatted(algorithm));
      return new DirectoryHashResult(HASH_NOT_AVAILABLE, 0);
    }

    Path root = directory.toPath();
    List<Path> files;
    try (Stream<Path> paths = Files.walk(root)) {
      files =
          paths
              .filter(Files::isRegularFile)
              .sorted(
                  Comparator.comparing(
                      path -> root.relativize(path).toString().replace(File.separatorChar, '/')))
              .toList();
    } catch (IOException e) {
      Logger.severe("Failed to list files in directory: %s", directory.getAbsolutePath());
      return new DirectoryHashResult(HASH_NOT_AVAILABLE, 0);
    }

    for (Path file : files) {
      byte[] fileHash = getHashBytes(file.toFile(), algorithm);
      if (Arrays.equals(fileHash, HASH_NOT_AVAILABLE)) {
        // getHashBytes already logged the failure; a combined hash missing any
        // file's hash would not be reproducible, so don't report one
        return new DirectoryHashResult(HASH_NOT_AVAILABLE, files.size());
      }
      digest.update(fileHash);
    }

    return new DirectoryHashResult(digest.digest(), files.size());
  }

  record DirectoryHashResult(byte[] hashBytes, int numFiles) {}

  static class UnableToCreateDirectoryException extends Exception {

    UnableToCreateDirectoryException(String message) {
      super(message);
    }
  }
}
