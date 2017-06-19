package com.rcv;

/**
 * Created by Jon on 6/18/17.
 */
public class RCVTester {


  public static String TEST_ELECTION_PATH = "./data/test_election_0.json";
  public static String TEST_CVR_PATH = "./data/test_cvr_0.json";
  public static String TEST_LOG_PATH = "./data/test_results.txt";

  public RCVTester() {
  }

  public void runTests() {

    RCVParser test1 = new RCVParser(TEST_ELECTION_PATH, TEST_CVR_PATH);


  }


}
