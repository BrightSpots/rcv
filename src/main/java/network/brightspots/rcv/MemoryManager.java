/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Utility class for calculating system memory and determining optimal heap size.
 * Design: Uses JMX to query system memory, applies 80% rule with 512MB rounding.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

/**
 * Utility class for managing memory-related calculations and system queries.
 * Provides methods to determine system RAM, current JVM heap size, and calculate
 * optimal heap allocation based on available system resources.
 */
class MemoryManager {

  private static final long MEGABYTE = 1024L * 1024L;
  private static final long CHUNK_SIZE_MB = 512L;
  private static final double PERCENTAGE = 0.80; // 80% of total RAM

  /**
   * Get total physical memory in MB.
   * Uses com.sun.management.OperatingSystemMXBean for cross-platform compatibility.
   *
   * @return total physical RAM in MB, or -1 if cannot determine
   */
  static long getTotalPhysicalMemoryMb() {
    try {
      OperatingSystemMXBean osBean =
          (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
      long totalMemoryBytes = osBean.getTotalPhysicalMemorySize();
      if (totalMemoryBytes > 0) {
        return totalMemoryBytes / MEGABYTE;
      }
    } catch (Exception e) {
      Logger.warning("Unable to determine total physical memory: %s", e.getMessage());
    }
    return -1;
  }

  /**
   * Get current max heap size the JVM is running with.
   *
   * @return current max heap in MB
   */
  static long getCurrentMaxHeapMb() {
    return Runtime.getRuntime().maxMemory() / MEGABYTE;
  }

  /**
   * Calculate recommended memory: 80% of RAM, rounded down to nearest 512MB.
   * Examples:
   * - 8GB (8192MB) RAM → 6144MB (6GB)
   * - 16GB (16384MB) RAM → 12800MB (12.5GB)
   * - 32GB (32768MB) RAM → 26112MB (25.5GB)
   *
   * @return recommended heap size in MB, or -1 if unable to determine
   */
  static long calculateRecommendedMemoryMb() {
    long totalMb = getTotalPhysicalMemoryMb();
    if (totalMb <= 0) {
      Logger.warning("Cannot calculate recommended memory: total physical memory unknown");
      return -1;
    }

    long eightyPercent = (long) (totalMb * PERCENTAGE);
    // Round down to nearest 512MB chunk
    long recommended = (eightyPercent / CHUNK_SIZE_MB) * CHUNK_SIZE_MB;

    Logger.info(
        "Memory calculation: Total RAM = %d MB, 80%% = %d MB, Rounded = %d MB",
        totalMb, eightyPercent, recommended);

    return recommended;
  }

  /**
   * Format memory size for display.
   *
   * @param memoryMb memory size in megabytes
   * @return formatted string like "6144 MB (6.0 GB)"
   */
  static String formatMemorySize(long memoryMb) {
    double gb = memoryMb / 1024.0;
    return String.format("%d MB (%.1f GB)", memoryMb, gb);
  }
}
