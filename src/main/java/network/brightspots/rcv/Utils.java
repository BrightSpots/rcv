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
