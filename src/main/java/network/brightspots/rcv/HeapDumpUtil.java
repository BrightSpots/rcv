/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package network.brightspots.rcv;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;

/**
 * Utility class for creating heap dumps programmatically.
 * Useful for memory profiling without using debugger breakpoints.
 */
public class HeapDumpUtil {

  private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";
  private static volatile HotSpotDiagnosticMXBean hotspotMBean;

  /**
   * Create a heap dump at the specified file path.
   *
   * @param filePath Path where the heap dump should be written (e.g., "/tmp/heap-dump.hprof")
   * @param live If true, dump only live objects. If false, dump all objects.
   * @throws IOException If heap dump creation fails
   */
  public static void dumpHeap(String filePath, boolean live) throws IOException {
    initHotspotMbean();
    hotspotMBean.dumpHeap(filePath, live);
    Logger.info("Heap dump created at: %s", filePath);
  }

  /**
   * Create a heap dump with a timestamped filename.
   *
   * @param baseDir Directory where heap dump should be created
   * @param prefix Prefix for the filename
   * @param live If true, dump only live objects
   * @return The full path to the created heap dump
   * @throws IOException If heap dump creation fails
   */
  public static String dumpHeapWithTimestamp(String baseDir, String prefix, boolean live)
      throws IOException {
    String timestamp = String.valueOf(System.currentTimeMillis());
    String filePath = String.format("%s/%s-%s.hprof", baseDir, prefix, timestamp);
    dumpHeap(filePath, live);
    return filePath;
  }

  private static void initHotspotMbean() throws IOException {
    if (hotspotMBean == null) {
      synchronized (HeapDumpUtil.class) {
        if (hotspotMBean == null) {
          MBeanServer server = ManagementFactory.getPlatformMBeanServer();
          try {
            hotspotMBean = ManagementFactory.newPlatformMXBeanProxy(
                server, HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
          } catch (Exception e) {
            throw new IOException("Failed to initialize HotSpotDiagnosticMXBean", e);
          }
        }
      }
    }
  }
}
