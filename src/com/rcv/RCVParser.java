package com.rcv;

import org.json.*;
import sun.misc.IOUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jon on 6/18/17.
 */
public class RCVParser {


  // create a Tabulator by specifying an election configuration file and the cast vote records for it
  RCVParser(String electionConfigPath, String castVoteRecordsPath) {

    String jsonData = readFile(electionConfigPath);
    Election election = parseElectionConfig(jsonData);

    String cvrJsonString = readFile(castVoteRecordsPath);
    List<CastVoteRecord> castVoteRecords = parseCastVoteRecords(cvrJsonString);

  }

  List<CastVoteRecord> parseCastVoteRecords(String jsonString) {

    try {
      JSONObject cvrObject = new JSONObject(jsonString);

    } catch (JSONException e) {
      e.printStackTrace();
    }

    ArrayList<CastVoteRecord> castVoteRecords = new ArrayList<CastVoteRecord>();

    return castVoteRecords;
  }



  Election parseElectionConfig(String jsonData) {

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
      return election;
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static String readFile(String filename) {
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

}
