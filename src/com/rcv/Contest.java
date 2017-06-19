package com.rcv;

import java.util.List;

public class Contest {
  private int id;
  private String name;
  private List<ContestOption> options;

  public Contest(int id, String name, List<ContestOption> options) {
    this.id = id;
    this.name = name;
    this.options = options;
  }

  int getId() {
    return id;
  }
}
