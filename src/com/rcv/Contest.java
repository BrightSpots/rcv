package com.rcv;

import java.util.List;

public class Contest {
  int id;
  String name;
  List<ContestOption> options;

  public Contest(int id, String name, List<ContestOption> options) {
    this.id = id;
    this.name = name;
    this.options = options;
  }
}
