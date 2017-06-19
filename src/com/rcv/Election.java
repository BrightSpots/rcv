package com.rcv;

import java.util.List;

public class Election {
  private int id;
  private String name;
  private List<Contest> contests;

  Election(int id, String name, List<Contest> contests) {
    this.id = id;
    this.name = name;
    this.contests = contests;
  }

  List<Contest> getContests() {
    return contests;
  }
}
