package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

/**
 * Created by Jon on 6/20/17.
 *
 * Container for multiple CastVoteRecords
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CastVoteRecordList {
  
  // the election id for which these votes were cast
  public int election_id;
  // list of CastVoteRecords
  public ArrayList<CastVoteRecord> records = new ArrayList<CastVoteRecord>();

  CastVoteRecordList() {}

}
