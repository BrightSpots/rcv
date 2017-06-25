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
 *  Jackson wrapper to help parser json into rcv objects
 *
 */
public class JsonParser {

  JsonParser() {}


  void runTests() {
    Election election = JsonParser.parseObjectFromFile("./data/test_election_0.json", Election.class);
    ContestRankings rankings = JsonParser.parseObjectFromFile("./data/test_ContestRankings.json", ContestRankings.class);
    CastVoteRecord cvr = JsonParser.parseObjectFromFile("./data/test_CastVoteRecord.json", CastVoteRecord.class);
    CastVoteRecordList cvrList = JsonParser.parseObjectFromFile("./data/test_CastVoteRecordList.json", CastVoteRecordList.class);

  }

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


