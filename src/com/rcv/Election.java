package com.rcv;

import java.util.List;

public class Election {
  int id;
  String name;
  List<Contest> contests;

  public Election(int id, String name, List<Contest> contests) {
    this.id = id;
    this.name = name;
    this.contests = contests;
  }
}
