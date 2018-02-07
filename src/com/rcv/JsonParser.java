/**
 * Created by Jonathan Moldover on 7/8/17
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
  // throws: JsonParseException | JsonMappingException if there was a problem mapping json data
  // throws: IOException if there was a problem reading the file
  // file access: read
  // returns: instance of the object parsed from json
  public static <T> T parseObjectFromFile(String jsonFilePath,
    Class<T> valueType
  ) throws Exception {
    try {
      // fileReader will read the json file from disk
      FileReader fileReader = new FileReader(jsonFilePath);
      // objectMapper will map json values into the new java object
      ObjectMapper objectMapper = new ObjectMapper();
      // object is the newly created object populated with json values
      T object = objectMapper.readValue(fileReader, valueType);
      return object;
    } catch (JsonParseException | JsonMappingException jsonException) {
      Logger.log("Error parsing file:%s", jsonFilePath);
      Logger.log("Check your file formatting and values to make sure they are correct.");
      Logger.log(jsonException.getMessage());
      throw jsonException;
    } catch (IOException fileException) {
      Logger.log("Error opening file:%s", jsonFilePath);
      Logger.log("Check your file path and make sure it is correct.");
      Logger.log(fileException.toString());
      throw fileException;
    }
  }
}


