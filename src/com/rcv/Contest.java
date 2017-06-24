package com.rcv;

import java.util.List;

public class Contest {
  public int id;
  public String name;
  public List<ContestOption> options;

  public Contest() {}

  public Contest(int id, String name, List<ContestOption> options) {
    this.id = id;
    this.name = name;
    this.options = options;
  }

  int getId() {
    return id;
  }
}
