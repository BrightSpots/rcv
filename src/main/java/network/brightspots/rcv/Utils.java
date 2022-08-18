/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: System environment and GUI utilities.
 * Design: NA.
 * Conditions: Always.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

final class Utils {

  private static final Map<String, String> envMap = System.getenv();

  private Utils() {
  }

  static boolean isNullOrBlank(String s) {
    return s == null || s.isBlank();
  }

  static boolean isInt(String s) {
    boolean isInt = true;
    try {
      Integer.parseInt(s);
    } catch (NumberFormatException exception) {
      isInt = false;
    }
    return isInt;
  }

  static String listToSentenceWithQuotes(List<String> list) {
    String sentence;

    if (list.size() == 1) {
      sentence = String.format("\"%s\"", list.get(0));
    } else if (list.size() == 2) {
      // if there are only 2 items, don't use a comma
      sentence = String.format("\"%s\" and \"%s\"", list.get(0), list.get(1));
    } else {
      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < list.size() - 1; i++) {
        stringBuilder.append("\"").append(list.get(i)).append("\", ");
      }
      stringBuilder.append("and \"").append(list.get(list.size() - 1)).append("\"");
      sentence = stringBuilder.toString();
    }

    return sentence;
  }

  static String getComputerName() {
    String computerName = "[unknown]";
    try {
      java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
      computerName = localMachine.getHostName();
    } catch (UnknownHostException exception) {
      if (envMap.containsKey("COMPUTERNAME")) {
        computerName = envMap.get("COMPUTERNAME");
      } else if (envMap.containsKey("HOSTNAME")) {
        computerName = envMap.get("HOSTNAME");
      }
    }
    return computerName;
  }

  static String getUserName() {
    String user = System.getProperty("user.name");
    if (user == null) {
      user = envMap.getOrDefault("USERNAME", "[unknown]");
    }
    return user;
  }
}
