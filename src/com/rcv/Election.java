package com.rcv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// container for an election configuration
// Has a name, e.g. "2018 Maine General Election"
// and a list of contests which will appear on the ballot

@JsonIgnoreProperties(ignoreUnknown = true)
public class Election {

  public String name;
  public List<Contest> contests;
  public Boolean batch_elimination;

  Election() {}

  List<Contest> getContests() {
    return contests;
  }
}
