package com.rcv;

import org.json.*;
import sun.misc.IOUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by Jon on 6/18/17.
 */
public class RCVTabulator {

  public static String TEST_ELECTION_PATH = "data/test_election_config.json";

  public static String readFile(String filename) {
    String result = "";
    try {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();
      while (line != null) {
        sb.append(line);
        line = br.readLine();
      }
      result = sb.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  // create a Tabulator by specifying an election configuration file and the cast vote records for it
  void RCVTabulator(String electionConfigPath, String castVoteRecordsPath) {

    // for testing use this for now
    electionConfigPath = TEST_ELECTION_PATH;

    String jsonData = readFile(electionConfigPath);
    JSONObject electionObject;

    // parse the election configuration
    try {
      // election object has id name and a list of contests
      electionObject = new JSONObject(jsonData);
      int electionID = electionObject.getInt("id");
      String electionName = electionObject.getString("name");
      JSONArray contestsArray = electionObject.getJSONArray("contests");
      ArrayList<Contest> contests = new ArrayList<Contest>(contestsArray.length());

      // each contest has an id name and list of contest options (candidates)
      for (int i = 0; i < contestsArray.length(); i++) {
        JSONObject contestObject = contestsArray.getJSONObject(i);
        int contestID = contestObject.getInt("id");
        String contestName = contestObject.getString("name");
        JSONArray optionArray = contestObject.getJSONArray("options");
        ArrayList<ContestOption> contestOptions = new ArrayList<ContestOption>(optionArray.length());

        // each contest option has an id and name
        for (int j = 0; j < optionArray.length(); j++) {
          JSONObject optionObject = optionArray.getJSONObject(j);
          int optionID = optionObject.getInt("id");
          String optionName = optionObject.getString("name");
          // create the option object and save it to the list
          ContestOption newOption = new ContestOption(optionID, optionName);
          contestOptions.add(newOption);
        }

        // create the contest object and add it to the list
        Contest contest = new Contest(contestID, contestName, contestOptions);
        contests.add(contest);
      }
      Election election = new Election(electionID, electionName, contests);

    } catch (JSONException e) {
      e.printStackTrace();
    }
//    System.out.println("Keyword: " + contestsArray.getString(i));

  }
}
