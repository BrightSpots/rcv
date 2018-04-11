/*
 * Created by Jonathan Moldover, Louis Eisenberg, and Hylton Edingfield
 * Copyright 2018 Bright Spots
 * Purpose: Wrapper for Jackson json parser to parse json files into java objects
 * Version: 1.0
 */

package com.rcv;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileReader;
import java.io.IOException;

public class JsonParser {

  // function: parseObjectFromFile
  // purpose: parse input json file into an object of the specified type
  // param: jsonFilePath path to json file to be parsed into java
  // param: valueType class of the object to be created from parsed json
  // file access: read
  // returns: instance of the object parsed from json or null if there was a problem
  public static <T> T parseObjectFromFile(String jsonFilePath, Class<T> valueType) {
    try {
      // fileReader will read the json file from disk
      FileReader fileReader = new FileReader(jsonFilePath);
      // objectMapper will map json values into the new java object
      ObjectMapper objectMapper = new ObjectMapper();
      // object is the newly created object populated with json values
      return objectMapper.readValue(fileReader, valueType);
    } catch (JsonParseException | JsonMappingException jsonException) {
      Logger.log("Error parsing json file:%s", jsonFilePath);
      Logger.log("Check your file formatting and values to make sure they are correct.");
      Logger.log(jsonException.getMessage());
    } catch (IOException fileException) {
      Logger.log("Error opening file:%s", jsonFilePath);
      Logger.log("Check your file path and make sure it is correct.");
      Logger.log(fileException.toString());
    }
    return null;
  }
}


