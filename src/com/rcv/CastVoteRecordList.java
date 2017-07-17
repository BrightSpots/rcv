package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

// Container for multiple CastVoteRecords
// this could be a group of Ballots cast in a particular election
@JsonIgnoreProperties(ignoreUnknown = true)
public class CastVoteRecordList {
  
  public int electionId;
  List<CastVoteRecord> records = new ArrayList<CastVoteRecord>();

  CastVoteRecordList() {}

  public List<CastVoteRecord> getRecords() {
    return records;
  }
}
