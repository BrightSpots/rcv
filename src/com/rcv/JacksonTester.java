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
public class JacksonTester {

//  List<CastVoteRecord> mCastVoteRecords;

  Election election;

  // create a Tabulator by specifying an election configuration file and the cast vote records for it
  JacksonTester(String electionConfigPath, String castVoteRecordsPath) {


    // test parsing Election
    try {
      ObjectMapper electionMapper = new ObjectMapper();
      FileReader electionReader = new FileReader(electionConfigPath);
      election = electionMapper.readValue(electionReader, Election.class);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // test parsing ContestRankings
    try {
      ObjectMapper rankingsMapper = new ObjectMapper();
      FileReader fileReader = new FileReader("./data/test_ContestRankings.json");
      ContestRankings rankings = rankingsMapper.readValue(fileReader, ContestRankings.class);
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

    // test parsing a CastVoteRecord2
    try {
      ObjectMapper selectionMapper = new ObjectMapper();
      FileReader fileReader = new FileReader("./data/test_CastVoteRecord.json");
      CastVoteRecord2 cvr2 = selectionMapper.readValue(fileReader, CastVoteRecord2.class);
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

    // test parsing CastVoteRecordList
    try {
      ObjectMapper castVoteRecordListMapper = new ObjectMapper();
      FileReader fileReader = new FileReader("./data/test_CastVoteRecordList.json");
      CastVoteRecordList castVoteRecordList = castVoteRecordListMapper.readValue(fileReader, CastVoteRecordList.class);
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


