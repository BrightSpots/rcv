package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

// Container for multiple CastVoteRecords
@JsonIgnoreProperties(ignoreUnknown = true)
public class CastVoteRecordList {
  
  public int electionId;
  List<CastVoteRecord> records = new ArrayList<CastVoteRecord>();

  CastVoteRecordList() {}

  public List<CastVoteRecord> getRecords() {
    return records;
  }
}
