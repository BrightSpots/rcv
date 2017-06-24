package com.rcv;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Election {

  public int id;
  public String name;
  public List<Contest> contests;

  Election() {}

  Election(int id, String name, List<Contest> contests) {
    this.id = id;
    this.name = name;
    this.contests = contests;
  }

  List<Contest> getContests() {
    return contests;
  }
}
