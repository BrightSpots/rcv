package com.rcv;

/*
 * Main entry point for the rcv module
 *
 * TODO: define command line parameters to control testing
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

  public static void main(String[] args) {
    if(args.length < 1) {
      System.err.print("Usage: rcv [path-to-config-file]");
      System.exit(1);
    }
    // look for config file
    File configFile = new File(args[0]);
    if(!configFile.exists()) {
      System.err.print(String.format("Config file:%s does not exist", args[0]));
      System.exit(1);
    }

    // parse config object
    RawElectionConfig rawConfig = JsonParser.parseObjectFromFile(args[0], RawElectionConfig.class);
    ElectionConfig config = new ElectionConfig(rawConfig);
    try {
      // setup log output
      RCVLogger.setup(config.auditOutput());
      RCVLogger.log("parsed config file:%s",args[0]);

      // parse input files
      CVRReader reader;
      List<CastVoteRecord> castVoteRecords = new ArrayList<>();
      for (RawElectionConfig.CVRSource source : rawConfig.cvr_file_sources) {
        RCVLogger.log("reading RCV:%s provider:%s",source.file_path, source.provider);
        reader = new CVRReader();
        reader.parseCVRFile(
          source.file_path,
          source.first_vote_column_index,
          config.maxRankingsAllowed(),
          rawConfig.getCandidateCodeList(),
          config
        );
        castVoteRecords.addAll(reader.castVoteRecords);
      }

      RCVLogger.log("read %d records",castVoteRecords.size());
      // tabulate
      Tabulator tabulator = new Tabulator(
        castVoteRecords,
        1,
        rawConfig.getCandidateCodeList(),
        config
      );
      try {
        tabulator.tabulate();
        tabulator.generateSummarySpreadsheet(config.visualizerOutput());
        tabulator.doAudit(castVoteRecords);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      System.out.println("failed to open log file:" + config.auditOutput());
      System.exit(1);
    }

  }


}
