/*
 * Ranked Choice Voting Universal Tabulator
 * Copyright (c) 2018 Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
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
 *
 * Purpose:
 * Wrapper for Jackson JSON parser to parse JSON files into Java objects.
 */

package com.rcv;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;

class JsonParser {

  // function: createRawContestConfigFromFile
  // purpose: parse input json file into a RawContestConfig
  // param: jsonFilePath path to json file to be parsed into java
  // file access: read
  // returns: instance of the RawContestConfig parsed from json, or null if there was a problem
  static RawContestConfig createRawContestConfigFromFile(String jsonFilePath) {
    RawContestConfig rawConfig;
    try {
      rawConfig =
          new ObjectMapper().readValue(new FileReader(jsonFilePath), RawContestConfig.class);
    } catch (JsonParseException | JsonMappingException exception) {
      Logger.Log(
          Level.SEVERE, "Error parsing JSON file: %s\n%s", jsonFilePath, exception.toString());
      Logger.Log(
          Level.SEVERE, "Check your file formatting and values to make sure they are correct.");
      rawConfig = null;
    } catch (IOException exception) {
      Logger.Log(
          Level.SEVERE, "Error opening file: %s\n%s", jsonFilePath, exception.toString());
      Logger.Log(
          Level.SEVERE, "Check your file path and permissions and make sure they are correct.");
      rawConfig = null;
    }
    return rawConfig;
  }

  static void createFileFromRawContestConfig(File jsonFile, RawContestConfig config) {
    try {
      new ObjectMapper().writer().withDefaultPrettyPrinter().writeValue(jsonFile, config);
      Logger.Log(
          Level.INFO, "Saved config via the GUI to: %s", jsonFile.getAbsolutePath());
    } catch (IOException exception) {
      Logger.Log(
          Level.SEVERE,
          "Error saving file: %s\n%s",
          jsonFile.getAbsolutePath(),
          exception.toString());
    }
  }
}
