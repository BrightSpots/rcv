/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Create a file that, on close, is read-only and has its hash added to the audit log.
 * Design: Overrides the File class
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

final class AuditableFile extends File {
  public AuditableFile(String pathname) {
    super(pathname);
  }

  public void finalizeAndHash() {
    String hash = FileUtils.getHash(this);

    // Write hash to audit log
    Logger.info("File %s written with hash %s".formatted(getAbsolutePath(), hash));

    // Write hash to hash file
    File hashFile = new File(getAbsolutePath() + ".hash");
    writeStringToFile(hashFile, "sha512: " + hash);

    // Make both file and its hash file read-only
    makeReadOnlyOrLogWarning(this);
    makeReadOnlyOrLogWarning(hashFile);
  }

  private void writeStringToFile(File file, String string) {
    try {
      Files.writeString(file.toPath(), string);
    } catch (IOException e) {
      Logger.severe("Could not write to file %s: %s", file.getAbsoluteFile(), e.getMessage());
    }
  }

  private void makeReadOnlyOrLogWarning(File file) {
    boolean readOnlySucceeded = file.setReadOnly();
    if (!readOnlySucceeded) {
      Logger.warning("Failed to set file to read-only: %s", getAbsolutePath());
    }
  }
}
