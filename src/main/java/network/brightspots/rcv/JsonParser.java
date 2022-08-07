/*
 * RCTab
 * Copyright (c) 2017-2022 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Wrapper around Jackson JSON package for reading and writing JSON objects to disk.
 * Design: the Jackson ObjectMapper class serialize and deserialize JSON with nifty annotations.
 * Conditions: During config loading, saving, or validating from the GUI, tabulation, and conversion
 * Version history: version 1.0
 * Complete revision history is available at: https://github.com/BrightSpots/rcv
 */

package network.brightspots.rcv;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("SameParameterValue")
class JsonParser {

  // purpose: parse input json file into an object of the specified type
  // param: jsonFilePath path to json file to be parsed into java
  // param: valueType class of the object to be created from parsed json
  // param: logsEnabled display log messages
  // returns: instance of the object parsed from json or null if there was a problem
  private static <T> T readFromFile(String jsonFilePath, Class<T> valueType, boolean logsEnabled) {
    T createdObject;
    try (FileReader fileReader = new FileReader(jsonFilePath, StandardCharsets.UTF_8)) {
      ObjectMapper objectMapper = new ObjectMapper();
      createdObject = objectMapper.readValue(fileReader, valueType);
    } catch (JsonParseException | JsonMappingException exception) {
      if (logsEnabled) {
        Logger.severe(
            """
                Error parsing JSON file: %s
                %s
                Check file formatting and values and make sure they are correct!
                It might help to try surrounding values causing problems with quotes (e.g. "value").
                See config_file_documentation.txt for more details.""",
            jsonFilePath, exception);
      }
      createdObject = null;
    } catch (IOException exception) {
      if (logsEnabled) {
        Logger.severe(
            """
                Error opening file: %s
                %s
                Check file path and permissions and make sure they are correct!""",
            jsonFilePath, exception);
      }
      createdObject = null;
    }
    return createdObject;
  }

  static <T> T readFromFile(String jsonFilePath, Class<T> valueType) {
    return readFromFile(jsonFilePath, valueType, true);
  }

  static <T> T readFromFileWithoutLogging(String jsonFilePath, Class<T> valueType) {
    return readFromFile(jsonFilePath, valueType, false);
  }

  static void writeToFile(File jsonFile, Object objectToSerialize) {
    try {
      new ObjectMapper()
          .writer()
          .withDefaultPrettyPrinter()
          .writeValue(jsonFile, objectToSerialize);
      Logger.info("Successfully saved file: %s", jsonFile.getAbsolutePath());
    } catch (IOException exception) {
      Logger.severe("Error saving file: %s\n%s", jsonFile.getAbsolutePath(), exception);
    }
  }
}
