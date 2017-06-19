package com.rcv;

import org.json.*;

public class Main {
  public static void main(String[] args) {
    System.out.println("Hello, World");
    RCVTabulator tabulator = new RCVTabulator(RCVTabulator.TEST_ELECTION_PATH,null);
  }
}
