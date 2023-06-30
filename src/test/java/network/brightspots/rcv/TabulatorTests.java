/*
 * RCTab
 * Copyright (c) 2017-2023 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: These regression tests run various tabulations and compare the generated results to
 * expected results.
 * Design: Passing these tests ensures that changes to code have not altered the results of the
 * tabulation.
 * Conditions: During automated testing.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import network.brightspots.rcv.ContestConfig.Provider;
import network.brightspots.rcv.Tabulator.TabulationAbortedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TabulatorTests {

  // folder where we store test inputs
  private static final String TEST_ASSET_FOLDER =
      "src/test/resources/network/brightspots/rcv/test_data";
  // limit log output to avoid spam
  private static final Integer MAX_LOG_ERRORS = 10;

  // compare file contents line by line to identify differences
  private static boolean fileCompare(String path1, String path2) {
    boolean result = true;
    try (BufferedReader br1 = new BufferedReader(new FileReader(path1, StandardCharsets.UTF_8));
        BufferedReader br2 = new BufferedReader(new FileReader(path2, StandardCharsets.UTF_8))) {
      int currentLine = 1;
      int errorCount = 0;

      // loop until EOF
      while (true) {
        String line1 = br1.readLine();
        String line2 = br2.readLine();
        if (line1 == null && line2 == null) {
          break;
        } else if (line1 == null || line2 == null) {
          Logger.severe("Files are unequal lengths!");
          result = false;
          break;
        }
        // both files have content so compare it
        // ignore differences in date as that is expected
        if (!(line1.contains("GeneratedDate") && line2.contains("GeneratedDate"))
            && !line1.equals(line2)) {
          errorCount++;
          result = false;
          Logger.severe("Files are not equal (line %d):\n%s\n%s", currentLine, line1, line2);
          if (errorCount >= MAX_LOG_ERRORS) {
            break;
          }
        }
        currentLine++;
      }
    } catch (FileNotFoundException exception) {
      Logger.severe("File not found!\n%s", exception);
      result = false;
    } catch (IOException exception) {
      Logger.severe("Error reading file!\n%s", exception);
      result = false;
    }
    return result;
  }

  // given stem and suffix returns path to file in test asset folder
  private static String getTestFilePath(String stem, String suffix) {
    return Paths.get(System.getProperty("user.dir"), TEST_ASSET_FOLDER, stem, stem + suffix)
        .toAbsolutePath()
        .toString();
  }

  private static void runTabulationTest(String testStem) {
    runTabulationTest(testStem, null);
  }

  // helper function to support running various tabulation tests
  private static void runTabulationTest(String stem, String expectedException) {
    String configPath = getTestFilePath(stem, "_config.json");

    Logger.info("Running tabulation test: %s\nTabulating config file: %s...", stem, configPath);
    TabulatorSession session = new TabulatorSession(configPath);
    List<String> exceptionsEncountered = session.tabulate();
    if (expectedException != null) {
      assertTrue(exceptionsEncountered.contains(expectedException));
      return;
    }
    Logger.info("Examining tabulation test results...");
    String timestampString = session.getTimestampString();
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    assertNotNull(config);

    if (config.isMultiSeatSequentialWinnerTakesAllEnabled()) {
      for (int i = 1; i <= config.getNumberOfWinners(); i++) {
        compareFiles(config, stem, timestampString, Integer.toString(i));
      }
    } else {
      compareFiles(config, stem, timestampString, null);
    }

    cleanOutputFolder(session);
  }

  // helper function to support running convert-to-cdf function
  private static void runConvertToCdfTest(String stem) {
    String configPath = getTestFilePath(stem, "_config.json");
    TabulatorSession session = new TabulatorSession(configPath);
    session.convertToCdf();

    String timestampString = session.getTimestampString();
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    compareFiles(config, stem, "cvr_cdf", ".json", timestampString, null);

    cleanOutputFolder(session);
  }

  // Validate convert-to-CSV action, run before every tabulation
  private static void runConvertToCsvTest(String stem) {
    String configPath = getTestFilePath(stem, "_config.json");
    TabulatorSession session = new TabulatorSession(configPath);
    session.tabulate();

    String expectedPath = getTestFilePath(stem, "_expected.csv");
    assertTrue(fileCompare(session.getConvertedFilesWritten().get(0), expectedPath));

    cleanOutputFolder(session);
  }


  private static void cleanOutputFolder(TabulatorSession session) {
    // Test passed so clean up test output folder
    File outputFolder = new File(session.getOutputPath());
    File[] files = outputFolder.listFiles();
    if (files != null) {
      for (File file : files) {
        if (!file.isDirectory()) {
          try {
            // Every ephemeral file must be set to read-only on close, including audit logs
            assertFalse(
                file.canWrite(),
                "File must not be writeable: %s".formatted(file.getAbsolutePath()));
            // Then set it writeable so it can be deleted
            boolean writeableSucceeded = file.setWritable(true);
            if (!writeableSucceeded) {
              Logger.warning("Failed to set file to writeable: %s", file.getAbsolutePath());
            }
            Files.delete(file.toPath());
          } catch (IOException exception) {
            Logger.severe("Error deleting file: %s\n%s", file.getAbsolutePath(), exception);
          }
        }
      }
    }
    Logger.info("Test complete.");
  }

  private static void compareFiles(
      ContestConfig config, String stem, String timestampString, String sequentialId) {
    compareFiles(config, stem, "summary", ".json", timestampString, sequentialId);
    compareFiles(config, stem, "summary", ".csv", timestampString, sequentialId);
    if (config.isGenerateCdfJsonEnabled()) {
      compareFiles(config, stem, "cvr_cdf", ".json", timestampString, sequentialId);
    }
  }

  private static void compareFiles(
      ContestConfig config,
      String stem,
      String outputType,
      String extension,
      String timestampString,
      String sequentialId) {
    String actualOutputPath =
        ResultsWriter.getOutputFilePath(
            config.getOutputDirectory(), outputType, timestampString, sequentialId)
            + extension;
    String expectedPath =
        getTestFilePath(
            stem,
            ResultsWriter.sequentialSuffixForOutputPath(sequentialId)
                + "_expected_"
                + outputType
                + extension);

    Logger.info("Comparing files:\nGenerated: %s\nReference: %s", actualOutputPath, expectedPath);
    if (fileCompare(expectedPath, actualOutputPath)) {
      Logger.info("Files are equal.");
    } else {
      Logger.info("Files are different.");
      fail();
    }
  }

  @BeforeAll
  static void setup() {
    Logger.setup();
  }

  @Test
  @DisplayName("Test Convert to CDF works for CDF")
  void convertToCdfFromCdf() {
    runConvertToCdfTest("convert_to_cdf_from_cdf");
  }

  @Test
  @DisplayName("Test Convert to CDF works for Dominion")
  void convertToCdfFromDominion() {
    runConvertToCdfTest("convert_to_cdf_from_dominion");
  }

  @Test
  @DisplayName("Test Convert to CDF works for ES&S")
  void convertToCdfFromEss() {
    runConvertToCdfTest("convert_to_cdf_from_ess");
  }

  @Test
  @DisplayName("Test Convert to CSV works for CDF")
  void convertToCsvFromCdf() {
    runConvertToCsvTest("convert_to_cdf_from_cdf");
  }

  @Test
  @DisplayName("Test Convert to CSV works for Dominion")
  void convertToCsvFromDominion() {
    runConvertToCsvTest("convert_to_cdf_from_dominion");
  }

  @Test
  @DisplayName("Test Convert to CSV works for ES&S")
  void convertToCsvFromEss() {
    runConvertToCsvTest("convert_to_cdf_from_ess");
  }

  @Test
  @DisplayName("aliases (CDF JSON format)")
  void aliasesJson() {
    runTabulationTest("aliases_cdf_json");
  }

  @Test
  @DisplayName("aliases (ES&S XLSX format)")
  void aliasesXlsx() {
    runTabulationTest("aliases_ess_xlsx");
  }

  @Test
  @DisplayName("NIST XML CDF 2")
  void nistXmlCdf2() {
    runTabulationTest("nist_xml_cdf_2");
  }

  @Test
  @DisplayName("unisyn_xml_cdf_city_tax_collector")
  void unisynXmlCdfCityTaxCollector() {
    runTabulationTest("unisyn_xml_cdf_city_tax_collector");
  }

  @Test
  @DisplayName("unisyn_xml_cdf_city_mayor")
  void unisynXmlCdfCityMayor() {
    runTabulationTest("unisyn_xml_cdf_city_mayor");
  }

  @Test
  @DisplayName("unisyn_xml_cdf_city_council_member")
  void unisynXmlCdfCityCouncilMember() {
    runTabulationTest("unisyn_xml_cdf_city_council_member");
  }

  @Test
  @DisplayName("unisyn_xml_cdf_city_chief_of_police")
  void unisynXmlCdfCityChiefOfPolice() {
    runTabulationTest("unisyn_xml_cdf_city_chief_of_police");
  }

  @Test
  @DisplayName("unisyn_xml_cdf_city_coroner")
  void unisynXmlCdfCityCoroner() {
    runTabulationTest("unisyn_xml_cdf_city_coroner");
  }

  @Test
  @DisplayName("unisyn_xml_cdf_county_sheriff")
  void unisynXmlCdfCountySheriff() {
    runTabulationTest("unisyn_xml_cdf_county_sheriff");
  }

  @Test
  @DisplayName("unisyn_xml_cdf_county_coroner")
  void unisynXmlCdfCountyCoroner() {
    runTabulationTest("unisyn_xml_cdf_county_coroner");
  }

  @Test
  @DisplayName("Clear Ballot - Kansas Primary")
  void testClearBallotKansasPrimary() {
    runTabulationTest("clear_ballot_kansas_primary");
  }

  @Test
  @DisplayName("Hart - Travis County Officers")
  void testHartTravisCountyOfficers() {
    runTabulationTest("hart_travis_county_officers");
  }

  @Test
  @DisplayName("Hart - Cedar Park School Board")
  void testHartCedarParkSchoolBoard() {
    runTabulationTest("hart_cedar_park_school_board");
  }

  @Test
  @DisplayName("Dominion test - Alaska test data")
  void testDominionAlaska() {
    runTabulationTest("dominion_alaska");
  }

  @Test
  @DisplayName("Dominion test - Kansas test data")
  void testDominionKansas() {
    runTabulationTest("dominion_kansas");
  }

  @Test
  @DisplayName("Dominion test - Wyoming test data")
  void testDominionWyoming() {
    runTabulationTest("dominion_wyoming");
  }

  @Test
  @DisplayName("Dominion - No Precinct Data")
  void testDominionNoPrecinctData() {
    runTabulationTest("dominion_no_precinct_data");
  }

  @Test
  @DisplayName("multi-cvr file dominion test")
  void multiFileDominionTest() {
    runTabulationTest("dominion_multi_file");
  }

  @Test
  @DisplayName("test invalid params in config file")
  void invalidParamsTest() {
    String configPath = getTestFilePath("invalid_params_test", "_config.json");
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    assertNotNull(config);
    // Expect validation errors
    assertFalse(config.validate().isEmpty());
  }

  @Test
  @DisplayName("test invalid source files")
  void invalidSourcesTest() {
    String configPath = getTestFilePath("invalid_sources_test", "_config.json");
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    assertNotNull(config);
    // Expect validation errors
    assertFalse(config.validate().isEmpty());
  }

  @Test
  @DisplayName("2015 Portland Mayor")
  void testPortlandMayor() {
    runTabulationTest("2015_portland_mayor");
  }

  @Test
  @DisplayName("2015 Portland Mayor Candidate Codes")
  void testPortlandMayorCodes() {
    runTabulationTest("2015_portland_mayor_codes");
  }

  // test large scale (1,000,000+) cvr contest
  @Test
  @DisplayName("2013 Minneapolis Mayor Scale")
  void test2013MinneapolisMayorScale() {
    runTabulationTest("2013_minneapolis_mayor_scale");
  }

  @Test
  @DisplayName("Continue Until Two Candidates Remain")
  void testContinueUntilTwoCandidatesRemain() {
    runTabulationTest("continue_tabulation_test");
  }

  @Test
  @DisplayName("Continue Until Two Candidates Remain with Batch Elimination")
  void testContinueUntilTwoCandidatesRemainWithBatchElimination() {
    runTabulationTest("continue_until_two_with_batch_elimination_test");
  }

  @Test
  @DisplayName("2017 Minneapolis Mayor")
  void test2017MinneapolisMayor() {
    runTabulationTest("2017_minneapolis_mayor");
  }

  @Test
  @DisplayName("2013 Minneapolis Mayor")
  void test2013MinneapolisMayor() {
    runTabulationTest("2013_minneapolis_mayor");
  }

  @Test
  @DisplayName("2013 Minneapolis Park")
  void test2013MinneapolisPark() {
    runTabulationTest("2013_minneapolis_park");
  }

  @Test
  @DisplayName("2018 Maine Governor Democratic Primary")
  void test2018MaineGovPrimaryDem() {
    runTabulationTest("2018_maine_governor_primary");
  }

  @Test
  @DisplayName("testMinneapolisMultiSeatThreshold")
  void testMinneapolisMultiSeatThreshold() {
    runTabulationTest("minneapolis_multi_seat_threshold");
  }

  @Test
  @DisplayName("test for overvotes")
  void testDuplicate() {
    runTabulationTest("duplicate_test");
  }

  @Test
  @DisplayName("test excluding candidates in config file")
  void testExcludedCandidate() {
    runTabulationTest("excluded_test");
  }

  @Test
  @DisplayName("test minimum vote threshold setting")
  void testMinimumThreshold() {
    runTabulationTest("minimum_threshold_test");
  }

  @Test
  @DisplayName("test skipping to next candidate after overvote")
  void testSkipToNext() {
    runTabulationTest("skip_to_next_test");
  }

  @Test
  @DisplayName("test stopping tabulation early")
  void testStopTabulationEarly() {
    runTabulationTest("stop_tabulation_early_test");
  }

  @Test
  @DisplayName("test Hare quota")
  void testHareQuota() {
    runTabulationTest("2013_minneapolis_park_hare");
  }

  @Test
  @DisplayName("test sequential multi-seat logic")
  void testSequentialMultiSeat() {
    runTabulationTest("2013_minneapolis_park_sequential");
  }

  @Test
  @DisplayName("test bottoms-up multi-seat logic")
  void testBottomsUpMultiSeat() {
    runTabulationTest("2013_minneapolis_park_bottoms_up");
  }

  @Test
  @DisplayName("test bottoms-up multi-seat with threshold logic")
  void testBottomsUpMultiSeatWithThreshold() {
    runTabulationTest("multi_seat_bottoms_up_with_threshold");
  }

  @Test
  @DisplayName("test allow only one winner per round logic")
  void testAllowOnlyOneWinnerPerRound() {
    runTabulationTest("test_set_allow_only_one_winner_per_round");
  }

  @Test
  @DisplayName("precinct example")
  void precinctExample() {
    runTabulationTest("precinct_example");
  }

  @Test
  @DisplayName("missing precinct example")
  void missingPrecinctExample() {
    runTabulationTest("missing_precinct_example");
  }

  @Test
  @DisplayName("test tiebreak seed: final round eliminates")
  void testTiebreakSeedEliminate() {
    runTabulationTest("tiebreak_seed_test_eliminate");
  }

  @Test
  @DisplayName("test tiebreak seed: final round elects")
  void testTiebreakSeedElect() {
    runTabulationTest("tiebreak_seed_test_elect");
  }

  @Test
  @DisplayName("skipped first choice")
  void nistTest0() {
    runTabulationTest("test_set_0_skipped_first_choice");
  }

  @Test
  @DisplayName("exhaust at overvote rule")
  void nistTest1() {
    runTabulationTest("test_set_1_exhaust_at_overvote");
  }

  @Test
  @DisplayName("overvote skips to next rank")
  void nistTest2() {
    runTabulationTest("test_set_2_overvote_skip_to_next");
  }

  @Test
  @DisplayName("skipped choice exhausts option")
  void nistTest3() {
    runTabulationTest("test_set_3_skipped_choice_exhaust");
  }

  @Test
  @DisplayName("skipped choice next option")
  void nistTest4() {
    runTabulationTest("test_set_4_skipped_choice_next");
  }

  @Test
  @DisplayName("two skipped ranks exhausts option")
  void nistTest5() {
    runTabulationTest("test_set_5_two_skipped_choice_exhaust");
  }

  @Test
  @DisplayName("duplicate rank exhausts")
  void nistTest6() {
    runTabulationTest("test_set_6_duplicate_exhaust");
  }

  @Test
  @DisplayName("duplicate rank skips to next option")
  void nistTest7() {
    runTabulationTest("test_set_7_duplicate_skip_to_next");
  }

  @Test
  @DisplayName("multi-cdf tabulation")
  void nistTest8() {
    runTabulationTest("test_set_8_multi_cdf");
  }

  @Test
  @DisplayName("multi-seat whole number threshold")
  void multiWinnerWholeThresholdTest() {
    runTabulationTest("test_set_multi_winner_whole_threshold");
  }

  @Test
  @DisplayName("multi-seat fractional number threshold")
  void multiWinnerFractionalThresholdTest() {
    runTabulationTest("test_set_multi_winner_fractional_threshold");
  }

  @Test
  @DisplayName("tiebreak using permutation in config")
  void tiebreakUsePermutationInConfigTest() {
    runTabulationTest("tiebreak_use_permutation_in_config_test");
  }

  @Test
  @DisplayName("tiebreak using generated permutation")
  void tiebreakGeneratePermutationTest() {
    runTabulationTest("tiebreak_generate_permutation_test");
  }

  @Test
  @DisplayName("tiebreak using previousRoundCountsThenRandom")
  void tiebreakPreviousRoundCountsThenRandomTest() {
    runTabulationTest("tiebreak_previous_round_counts_then_random_test");
  }

  @Test
  @DisplayName("treat blank as undeclared write-in")
  void treatBlankAsUndeclaredWriteInTest() {
    runTabulationTest("test_set_treat_blank_as_undeclared_write_in");
  }

  @Test
  @DisplayName("undeclared write-in (UWI) cannot win test")
  void uwiCannotWinTest() {
    runTabulationTest("uwi_cannot_win_test");
  }

  @Test
  @DisplayName("multi-seat UWI test")
  void multiSeatUwiTest() {
    runTabulationTest("multi_seat_uwi_test");
  }

  @Test
  @DisplayName("overvote delimiter test")
  void overvoteDelimiterTest() {
    runTabulationTest("test_set_overvote_delimiter");
  }

  @Test
  @DisplayName("sequential with batch elimination test")
  void sequentialWithBatchElimination() {
    runTabulationTest("sequential_with_batch");
  }

  @Test
  @DisplayName("sequential with continue until two test")
  void sequentialWithContinueUntilTwo() {
    runTabulationTest("sequential_with_continue_until_two");
  }

  @Test
  @DisplayName("first round determine threshold test: elect on final round")
  void firstRoundDeterminesThresholdElectTest() {
    runTabulationTest("first_round_determines_threshold_elect_test");
  }

  @Test
  @DisplayName("first round determine threshold test: eliminate on final round")
  void firstRoundDeterminesThresholdEliminateTest() {
    runTabulationTest("first_round_determines_threshold_eliminate_test");
  }

  @Test
  @DisplayName("first round determine threshold and tiebreaker runs test")
  void firstRoundDeterminesTiebreakerThresholdTest() {
    runTabulationTest("first_round_determines_threshold_tiebreaker_test");
  }

  @Test
  @DisplayName("overvote exhaust if multiple continuing test")
  void overvoteExhaustIfMultipleContinuingTest() {
    runTabulationTest("exhaust_if_multiple_continuing");
  }

  @Test
  @DisplayName("generic CSV test")
  void genericCsvTest() {
    runTabulationTest("generic_csv_test");
  }

  @Test
  @DisplayName("no one meets minimum test")
  void noOneMeetsMinimumTest() {
    runTabulationTest("no_one_meets_minimum", TabulationAbortedException.class.toString());
  }

  @Test
  @DisplayName("gracefully fail when tabulate-by-precinct option set without any precincts in CVR")
  void tabulateByPrecinctWithoutPrecincts() {
    runTabulationTest(
        "tabulate_by_precinct_without_precincts", TabulationAbortedException.class.toString());
  }
}
