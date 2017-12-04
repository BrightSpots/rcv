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
    ElectionConfig config = JsonParser.parseObjectFromFile(args[0], ElectionConfig.class);
    try {
      // setup log output
      RCVLogger.setup(config.audit_output);
      RCVLogger.log("parsed config file:%s",args[0]);

      // parse input files
      CVRReader reader;
      List<CastVoteRecord> castVoteRecords = new ArrayList<>();
      for (ElectionConfig.CVRSource source : config.cvr_file_sources) {
        RCVLogger.log("reading RCV:%s provider:%s",source.file_path, source.provider);
        reader = new CVRReader();
        reader.parseCVRFile(
          source.file_path,
          source.first_vote_column_index,
          config.max_rankings_allowed,
          config.candidates,
          config.rules.undeclared_write_in_label,
          config.rules.overvote_flag,
          config.rules.undervote_flag);
        castVoteRecords.addAll(reader.castVoteRecords);
      }

      RCVLogger.log("read %d records",castVoteRecords.size());
      // tabulate
      Tabulator tabulator = new Tabulator(
          castVoteRecords,
          1,
          config.candidates,
          config.rules.batch_elimination,
          config.rules.max_skipped_ranks_allowed,
          Tabulator.overvoteRuleForConfigSetting(config.rules.overvote_rule),
          null,
          config.rules.undeclared_write_in_label
      ).setContestName(config.contest_name).
        setJurisdiction(config.jurisdiction).
        setOffice(config.office).
        setElectionDate(config.date);
      try {
        tabulator.tabulate();
        tabulator.generateSummarySpreadsheet(config.visualizer_output);
        tabulator.doAudit(castVoteRecords);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      System.out.println("failed to open log file:" + config.audit_output);
      System.exit(1);
    }

  }


}
