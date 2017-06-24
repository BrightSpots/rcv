package com.rcv;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by Jon on 6/19/17.
 */
public class JacksonParser {

  public List<CastVoteRecord2> getCastVoteRecords() {
    return mCastVoteRecords.records;
  }

  public Election getElection() {
    return mElection;
  }

  private CastVoteRecordList mCastVoteRecords = null;
  private Election mElection = null;

  // create a Tabulator by specifying an election configuration file and the cast vote records for it
  JacksonParser(String electionConfigPath, String castVoteRecordsPath) {

    // Election
    try {
      ObjectMapper electionMapper = new ObjectMapper();
      FileReader electionReader = new FileReader(electionConfigPath);
      mElection = electionMapper.readValue(electionReader, Election.class);
      electionReader.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // CastVoteRecordList
    try {
      ObjectMapper castVoteRecordListMapper = new ObjectMapper();
      FileReader fileReader = new FileReader(castVoteRecordsPath);
      mCastVoteRecords = castVoteRecordListMapper.readValue(fileReader, CastVoteRecordList.class);
      fileReader.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}


