package com.rcv;

import org.json.*;
import sun.misc.IOUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.util.*;

public class RCVParser {

  List<CastVoteRecord> castVoteRecords;

  // create a Tabulator by specifying an election configuration file and the cast vote records for it
  RCVParser(String electionConfigPath, String castVoteRecordsPath) {

    String jsonData = readFile(electionConfigPath);
    Election election = parseElectionConfig(jsonData);

    String cvrJsonString = readFile(castVoteRecordsPath);
    castVoteRecords = parseCastVoteRecords(cvrJsonString, election);
  }

  List<CastVoteRecord> getCastVoteRecords() {
    return castVoteRecords;
  }

  // extract cast vote record data from json string
  List<CastVoteRecord> parseCastVoteRecords(String jsonString, Election election) {

    ArrayList<CastVoteRecord> castVoteRecords = null;
    try {
      // cvr file has the election id and name for which these votes were cast
      // followed by an array of cast vote records
      JSONObject cvrObject = new JSONObject(jsonString);
      int electionID = cvrObject.getInt("id");
      String electionName = cvrObject.getString("name");
      JSONArray cvrArray = cvrObject.getJSONArray("records");

      // array to store the parsed results
      castVoteRecords = new ArrayList<CastVoteRecord>(cvrArray.length());

      // each cast vote record is a mapping from election contest ID(s) to voter selections
      // voter selections is a map of rank to contest option ID (a candidate)

      // for each record
      for(int i = 0; i < cvrArray.length(); i++) {
        JSONObject voteObject = cvrArray.getJSONObject(i);

        // container for parse java object
        Map<Integer, SortedMap<Integer, Integer>> rankings = new HashMap<Integer, SortedMap<Integer, Integer>>();

        // for each contest in the election
        for(Contest contest : election.getContests()) {
          // note: contest IDs are stored as strings in cvr json since we use them as keys
          String contestID = Integer.toString(contest.getId());
          // get voter selections
          JSONObject contestSelections = voteObject.getJSONObject(contestID);
          // container for parsed java object
          SortedMap<Integer, Integer> userSelections = new TreeMap<Integer, Integer>();
          // iterate through their selection
          JSONArray ranks = contestSelections.names();
          for(int j = 0; j < ranks.length(); j++) {
            String rank = ranks.getString(j);
            int selectionID = contestSelections.getInt(rank);
            userSelections.put( Integer.decode(rank), selectionID);
          }
          rankings.put(Integer.decode(contestID), userSelections);
        }

        CastVoteRecord castVoteRecord = new CastVoteRecord(rankings);
        castVoteRecords.add(castVoteRecord);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
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
