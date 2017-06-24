package com.rcv;


// Represents a contest option (candidate) running in a contest
public class ContestOption {
  public int id;
  public String name;

  public ContestOption() {}

  public ContestOption(int id, String name) {
    this.id = id;
    this.name = name;
  }
}
