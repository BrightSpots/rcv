/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Utility class for restarting the RCTab application with new JVM arguments.
 * Design: Uses ProcessBuilder to launch a new instance with updated memory settings.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;

/**
 * Utility class for restarting the RCTab application with new JVM arguments.
 * Handles cross-platform restart mechanisms for Windows, macOS, and Linux.
 */
class ApplicationRestarter {

  /**
   * Restart the application with specified max heap size.
   * Launches a new process with the updated -Xmx parameter and exits the current instance.
   *
   * @param maxHeapMB maximum heap size in megabytes
   * @return true if restart initiated successfully, false otherwise
   */
  static boolean restartWithMemory(long maxHeapMB) {
    try {
      // Get current java executable path
      String javaPath = getJavaExecutablePath();
      Logger.info("Java executable path: %s", javaPath);

      // Build command to restart application
      List<String> command = buildRestartCommand(javaPath, maxHeapMB);
      Logger.info("Restart command: %s", String.join(" ", command));

      // Start new process
      ProcessBuilder builder = new ProcessBuilder(command);
      // Inherit the working directory from current process
      builder.directory(new File(System.getProperty("user.dir")));
      // Redirect error stream to output for debugging
      builder.redirectErrorStream(true);

      Process process = builder.start();
      Logger.info("New RCTab process started with PID (if available)");

      // Give the new process a moment to start before we exit
      Thread.sleep(500);

      // Exit current instance
      Logger.info("Restarting RCTab with %d MB heap. Shutting down current instance...",
          maxHeapMB);
      Platform.exit();
      System.exit(0);

      return true;
    } catch (IOException e) {
      Logger.severe("Failed to restart application (IO error): %s", e.getMessage());
      return false;
    } catch (InterruptedException e) {
      Logger.severe("Restart interrupted: %s", e.getMessage());
      Thread.currentThread().interrupt();
      return false;
    } catch (Exception e) {
      Logger.severe("Failed to restart application: %s", e.getMessage());
      return false;
    }
  }

  /**
   * Get path to java executable.
   * Checks JAVA_HOME first, then uses 'java' from PATH as fallback.
   *
   * @return path to java executable
   */
  private static String getJavaExecutablePath() {
    String javaHome = System.getProperty("java.home");
    if (javaHome != null && !javaHome.isEmpty()) {
      String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
      if (isWindows()) {
        javaBin += ".exe";
      }
      File javaFile = new File(javaBin);
      if (javaFile.exists()) {
        return javaBin;
      }
      Logger.warning("Java executable not found at %s, falling back to 'java' from PATH",
          javaBin);
    }
    // Fallback to PATH
    return isWindows() ? "java.exe" : "java";
  }

  /**
   * Build command line for restarting the application.
   * Reconstructs: java -Xmx{mem}m --module-path {path} --module {module}
   *
   * @param javaPath path to java executable
   * @param maxHeapMB maximum heap size in MB
   * @return command as list of strings for ProcessBuilder
   */
  private static List<String> buildRestartCommand(String javaPath, long maxHeapMB) {
    List<String> command = new ArrayList<>();

    // Java executable
    command.add(javaPath);

    // Memory parameter
    command.add("-Xmx" + maxHeapMB + "m");

    // Get module path from current runtime
    String modulePath = System.getProperty("jdk.module.path");
    if (modulePath != null && !modulePath.isEmpty()) {
      command.add("--module-path");
      command.add(modulePath);
      Logger.info("Using module path: %s", modulePath);
    } else {
      Logger.warning("jdk.module.path not set. Restart may not work correctly.");
      Logger.warning("This is expected when running from an IDE during development.");

      // Try to use classpath as fallback
      String classPath = System.getProperty("java.class.path");
      if (classPath != null && !classPath.isEmpty()) {
        command.add("-cp");
        command.add(classPath);
        Logger.info("Using classpath fallback: %s", classPath);
      }
    }

    // Add module and main class
    if (modulePath != null && !modulePath.isEmpty()) {
      command.add("--module");
      command.add("network.brightspots.rcv/network.brightspots.rcv.Main");
    } else {
      // If no module path, use direct class launch
      command.add("network.brightspots.rcv.Main");
    }

    return command;
  }

  /**
   * Check if running on Windows.
   *
   * @return true if OS is Windows
   */
  private static boolean isWindows() {
    String osName = System.getProperty("os.name");
    return osName != null && osName.toLowerCase().contains("win");
  }

  /**
   * Check if running on macOS.
   *
   * @return true if OS is macOS
   */
  @SuppressWarnings("unused")
  private static boolean isMac() {
    String osName = System.getProperty("os.name");
    return osName != null && osName.toLowerCase().contains("mac");
  }
}
