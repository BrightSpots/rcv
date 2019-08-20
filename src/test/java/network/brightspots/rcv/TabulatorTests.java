/*
 * Universal RCV Tabulator
 * Copyright (c) 2017-2019 Bright Spots Developers.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this
 * program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Purpose:
 * These regression tests run various tabulations and compare the generated results to
 * expected results.  Passing these tests ensures that changes to tabulation code have not
 * altered the results of the tabulation.
 */

package network.brightspots.rcv;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
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
    try {
      BufferedReader reader1 = new BufferedReader(new FileReader(path1));
      BufferedReader reader2 = new BufferedReader(new FileReader(path2));
      int currentLine = 1;
      int errorCount = 0;

      // loop until EOF
      while (true) {
        String line1 = reader1.readLine();
        String line2 = reader2.readLine();
        if (line1 == null && line2 == null) {
          break;
        } else if (line1 == null || line2 == null) {
          Logger.log(Level.SEVERE, "Files are unequal lengths!");
          result = false;
          break;
        }
        // both files have content so compare it
        // ignore differences in date as that is expected
        if (!(line1.contains("GeneratedDate") && line2.contains("GeneratedDate"))
            && !line1.equals(line2)) {
          errorCount++;
          result = false;
          Logger.log(
              Level.SEVERE, "Files are not equal (line %d):\n%s\n%s", currentLine, line1, line2);
          if (errorCount >= MAX_LOG_ERRORS) {
            break;
          }
        }
        currentLine++;
      }
    } catch (FileNotFoundException e) {
      Logger.log(Level.SEVERE, "File not found!\n%s", e.toString());
      result = false;
    } catch (IOException e) {
      Logger.log(Level.SEVERE, "Error reading file!\n%s", e.toString());
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

  // helper function to support running various tabulation tests
  private static void runTabulationTest(String stem) {
    String configPath = getTestFilePath(stem, "_config.json");
    TabulatorSession session = new TabulatorSession(configPath);
    session.tabulate();

    String timestampString = session.getTimestampString();
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    assertNotNull(config);

    if (config.isMultiSeatSequentialWinnerTakesAllEnabled()) {
      for (int i = 1; i <= config.getNumberOfWinners(); i++) {
        compareJsons(config, stem, timestampString, i);
      }
    } else {
      compareJsons(config, stem, timestampString, null);
    }

    // test passed so cleanup test output folder
    File outputFolder = new File(session.getOutputPath());
    if (outputFolder.listFiles() != null) {
      //noinspection ConstantConditions
      for (File file : outputFolder.listFiles()) {
        if (!file.isDirectory()) {
          //noinspection ResultOfMethodCallIgnored
          file.delete();
        }
      }
    }
  }

  private static void compareJsons(
      ContestConfig config, String stem, String timestampString, Integer sequentialNumber) {
    compareJson(config, stem, "summary", timestampString, sequentialNumber);
    if (config.isGenerateCdfJsonEnabled()) {
      compareJson(config, stem, "cvr_cdf", timestampString, sequentialNumber);
    }
  }

  private static void compareJson(
      ContestConfig config,
      String stem,
      String jsonType,
      String timestampString,
      Integer sequentialNumber) {
    String actualOutputPath =
        ResultsWriter.getOutputFilePath(
                config.getOutputDirectory(), jsonType, timestampString, sequentialNumber)
            + ".json";
    String expectedPath =
        getTestFilePath(
            stem,
            ResultsWriter.sequentialSuffixForOutputPath(sequentialNumber)
                + "_expected_"
                + jsonType
                + ".json");
    assertTrue(fileCompare(expectedPath, actualOutputPath));
  }

  @BeforeAll
  static void setup() {
    Logger.setup();
  }

  @Test
  @DisplayName("test invalid params in config file")
  void invalidParamsTest() {
    String configPath = getTestFilePath("invalid_params_test", "_config.json");
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    assertNotNull(config);
    assertFalse(config.validate());
  }

  @Test
  @DisplayName("test invalid source files")
  void invalidSourcesTest() {
    String configPath = getTestFilePath("invalid_sources_test", "_config.json");
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    assertNotNull(config);
    assertFalse(config.validate());
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
  @DisplayName("test tiebreak seed")
  void testTiebreakSeed() {
    runTabulationTest("tiebreak_seed_test");
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
}
