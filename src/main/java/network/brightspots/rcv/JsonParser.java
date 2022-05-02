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

/*
 * Purpose:
 * Wrapper around Jackson JSON package for reading and writing JSON objects to disk.
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
