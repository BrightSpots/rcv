package com.rcv;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Jon on 6/19/17.
 *
 *  Jackson wrapper to help parse json into rcv objects
 *
 */
public class JsonParser {

  // TODO: add logging and recovery logic
  public static <T> T parseObjectFromFile(String jsonFilePath, Class<T> valueType) {
    try {
      ObjectMapper electionMapper = new ObjectMapper();
      FileReader electionReader = new FileReader(jsonFilePath);
      T object = electionMapper.readValue(electionReader, valueType);
      return object;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

}


