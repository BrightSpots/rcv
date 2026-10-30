/*
 * RCTab
 * Copyright (c) 2017-2026 RCV Resource Center.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: These tests check file utilities, particularly hashing.
 * Design: Unit tests.
 * Conditions: During automated testing.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsTests {
  @BeforeAll
  static void setup() {
    Logger.setup();
  }

  @Test
  @DisplayName("folder hash is the hash of each file's hash, alphabetized by relative path")
  void testDirectoryHash(@TempDir Path tempDir) throws IOException, NoSuchAlgorithmException {
    Path subdir = Files.createDirectory(tempDir.resolve("subdir"));
    Files.writeString(subdir.resolve("c.txt"), "third");
    Files.writeString(tempDir.resolve("b.txt"), "second");
    // Written last, but sorts first alphabetically
    Files.writeString(tempDir.resolve("a.txt"), "first");

    MessageDigest expectedDigest = MessageDigest.getInstance("SHA-512");
    expectedDigest.update(FileUtils.getHashBytes(tempDir.resolve("a.txt").toFile(), "SHA-512"));
    expectedDigest.update(FileUtils.getHashBytes(tempDir.resolve("b.txt").toFile(), "SHA-512"));
    expectedDigest.update(FileUtils.getHashBytes(subdir.resolve("c.txt").toFile(), "SHA-512"));

    FileUtils.DirectoryHashResult result =
        FileUtils.getDirectoryHashBytes(tempDir.toFile(), "SHA-512");
    assertEquals(3, result.numFiles());
    assertEquals(
        Utils.bytesToHex(expectedDigest.digest()), Utils.bytesToHex(result.hashBytes()));
  }

  @Test
  @DisplayName("folder hash changes when a file's contents change")
  void testDirectoryHashChangesWithContents(@TempDir Path tempDir) throws IOException {
    Path file = Files.writeString(tempDir.resolve("cvr.csv"), "contents");
    String originalHash =
        Utils.bytesToHex(FileUtils.getDirectoryHashBytes(tempDir.toFile(), "SHA-512").hashBytes());

    Files.writeString(file, "different contents");
    String modifiedHash =
        Utils.bytesToHex(FileUtils.getDirectoryHashBytes(tempDir.toFile(), "SHA-512").hashBytes());
    assertNotEquals(originalHash, modifiedHash);
  }
}
