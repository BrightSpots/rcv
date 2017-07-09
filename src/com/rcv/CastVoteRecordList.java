package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jon on 6/20/17.
 *
 * Container for multiple CastVoteRecords
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CastVoteRecordList {
  
  // the election id for which these votes were cast
  int electionId;
  // list of CastVoteRecords
  List<CastVoteRecord> records = new ArrayList<CastVoteRecord>();

  CastVoteRecordList() {}

  List<CastVoteRecord> getRecords() {
    return records;
  }
}
