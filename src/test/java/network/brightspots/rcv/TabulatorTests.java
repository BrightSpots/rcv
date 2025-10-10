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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import network.brightspots.rcv.OutputWriter.OutputFileIdentifiers;
import network.brightspots.rcv.OutputWriter.OutputType;
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
    return path1.endsWith(".json") && path2.endsWith(".json")
        ? fileCompareJson(path1, path2)
        : fileCompareLineByLine(path1, path2);
  }

  private static boolean fileCompareJson(String path1, String path2) {
    ObjectMapper mapper = new ObjectMapper();
    boolean succeeded;
    List<String> keysToIgnore = List.of("GeneratedDate");
    try {
      TypeReference<HashMap<String, Object>> type = new TypeReference<>() {};
      HashMap<String, Object> map1 = mapper.readValue(new File(path1), type);
      HashMap<String, Object> map2 = mapper.readValue(new File(path2), type);

      succeeded = compareMaps(map1, map2, keysToIgnore);
    } catch (IOException e) {
      Logger.severe("Error parsing JSON file: " + e.getMessage());
      succeeded = false;
    }

    return succeeded;
  }

  private static boolean compareMaps(
      Map<String, Object> map1, Map<String, Object> map2, List<String> keysToIgnore) {
    boolean areEqual = true;
    if (map1.size() != map2.size()) {
      areEqual = false;
    } else {
      for (Map.Entry<String, Object> entry : map1.entrySet()) {
        String key = entry.getKey();
        Object value1 = entry.getValue();
        Object value2 = map2.get(key);

        if (keysToIgnore.contains(key)) {
          continue;
        }

        if (value1 instanceof Map && value2 instanceof Map) {
          if (!compareMaps(
              (Map<String, Object>) value1, (Map<String, Object>) value2, keysToIgnore)) {
            Logger.severe("Maps at key %s are not equal (%s != %s)", key, value1, value2);
            areEqual = false;
            break;
          }
        } else if (value1 instanceof List && value2 instanceof List) {
          if (!compareLists((List<Object>) value1, (List<Object>) value2, keysToIgnore)) {
            Logger.severe("Lists at key %s are not equal (%s != %s)", key, value1, value2);
            areEqual = false;
            break;
          }
        } else if (!value1.equals(value2)) {
          Logger.severe("Values at key %s are not equal (%s != %s)", key, value1, value2);
          areEqual = false;
          break;
        }
      }
    }

    return areEqual;
  }

  private static boolean compareLists(
      List<Object> value1, List<Object> value2, List<String> keysToIgnoreForMaps) {
    boolean areEqual = true;
    if (value1.size() != value2.size()) {
      areEqual = false;
    } else {
      for (int i = 0; i < value1.size(); i++) {
        Object item1 = value1.get(i);
        Object item2 = value2.get(i);

        if (item1 instanceof Map && item2 instanceof Map) {
          if (!compareMaps(
              (Map<String, Object>) item1, (Map<String, Object>) item2, keysToIgnoreForMaps)) {
            areEqual = false;
            break;
          }
        } else if (item1 instanceof List && item2 instanceof List) {
          if (!compareLists((List<Object>) item1, (List<Object>) item2, keysToIgnoreForMaps)) {
            areEqual = false;
            break;
          }
        } else if (!item1.equals(item2)) {
          areEqual = false;
          break;
        }
      }
    }

    return areEqual;
  }

  private static boolean fileCompareLineByLine(String path1, String path2) {
    boolean result = true;
    try (BufferedReader br1 = new BufferedReader(new FileReader(path1, UTF_8));
        BufferedReader br2 = new BufferedReader(new FileReader(path2, UTF_8))) {
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
  private static Path getTestDirectory(String stem) {
    return Paths.get(System.getProperty("user.dir"), TEST_ASSET_FOLDER, stem);
  }

  // given stem and suffix returns path to file in test asset folder
  private static String getTestFilePath(String stem, String suffix) {
    Path directory = getTestDirectory(stem);
    String filename = stem + suffix;
    return Paths.get(directory.toString(), filename).toAbsolutePath().toString();
  }

  private static void runTabulationTest(String testStem) {
    runTabulationTest(testStem, null, 0);
  }

  private static void runTabulationTest(String testStem, int expectedNumSliceFilesToCheck) {
    runTabulationTest(testStem, null, expectedNumSliceFilesToCheck);
  }

  private static void runTabulationTest(String testStem, String expectedException) {
    runTabulationTest(testStem, expectedException, 0);
  }

  // helper function to support running various tabulation tests
  private static void runTabulationTest(String stem, String expectedException,
                                        int expectedNumSliceFilesToCheck) {
    String configPath = getTestFilePath(stem, "_config.json");

    Logger.info("Running tabulation test: %s\nTabulating config file: %s...", stem, configPath);
    TabulatorSession session = new TabulatorSession(configPath);
    List<String> exceptionsEncountered = session.tabulate("Automated test");
    if (expectedException != null) {
      assertTrue(exceptionsEncountered.contains(expectedException));
    } else {
      Logger.info("Examining tabulation test results...");
      String timestampString = session.getTimestampString();
      ContestConfig config = ContestConfig.loadContestConfig(configPath);
      assertNotNull(config);

      if (config.isMultiSeatSequentialWinnerTakesAllEnabled()) {
        for (int i = 1; i <= config.getNumberOfWinners(); i++) {
          compareFiles(config, stem, timestampString, i);
        }
      } else {
        compareFiles(config, stem, timestampString, null);
      }

      int numSlicedFilesChecked = 0;
      for (ContestConfig.TabulateBySlice slice : config.enabledSlices()) {
        for (String sliceName : session.loadSliceNamesFromCvrs(slice, config)) {
          OutputFileIdentifiers outputFileIdentifiersJson = new OutputFileIdentifiers(
                  OutputType.DETAILED_JSON, slice, sliceName);
          OutputFileIdentifiers outputFileIdentifiersCsv = new OutputFileIdentifiers(
                  OutputType.DETAILED_CSV, slice, sliceName);
          if (compareFiles(config, stem, outputFileIdentifiersJson, timestampString, null, true)) {
            numSlicedFilesChecked++;
          }
          if (compareFiles(config, stem, outputFileIdentifiersCsv, timestampString, null, true)) {
            numSlicedFilesChecked++;
          }
        }
      }
      assertEquals(expectedNumSliceFilesToCheck, numSlicedFilesChecked);

      cleanOutputFolder(session);
    }
  }

  // helper function to support running convert-to-cdf function
  private static void runConvertToCdfTest(String stem) {
    String configPath = getTestFilePath(stem, "_config.json");
    TabulatorSession session = new TabulatorSession(configPath);
    session.convertToCdf();

    String timestampString = session.getTimestampString();
    ContestConfig config = ContestConfig.loadContestConfig(configPath);
    compareFiles(config, stem, OutputType.CDF_CVR, timestampString, null, false);

    cleanOutputFolder(session);
  }

  // Validate convert-to-CSV action, run before every tabulation
  private static void runConvertToRctabCvrTest(String stem) {
    String configPath = getTestFilePath(stem, "_config.json");
    TabulatorSession session = new TabulatorSession(configPath);
    session.tabulate("Automated test");

    String expectedPath = getTestFilePath(stem, "_expected.csv");
    assertTrue(fileCompare(session.getRctabCvrFilePath(), expectedPath));

    cleanOutputFolder(session);
  }

  private static void cleanOutputFolder(TabulatorSession session) {
    // Test passed so clean up test output folder
    File outputFolder = new File(session.getOutputPath());
    Stack<File> dirsToDeleteIfEmpty = new Stack<>();
    dirsToDeleteIfEmpty.add(outputFolder);

    File[] fileArray = outputFolder.listFiles();
    if (fileArray != null) {
      List<File> files = new java.util.ArrayList<>(List.of(fileArray));

      // Iterate over the list of files. As we encounter certain expected directories,
      // files within those directories may be added to the files array.
      for (int i = 0; i < files.size(); ++i) {
        File file = files.get(i);
        String filename = file.getName();

        if (filename.equals(".DS_Store")) {
          continue;
        }
        if (filename.endsWith(".lck")) {
          continue;
        }
        if (!file.isDirectory()) {
          try {
            // Every ephemeral file must be set to read-only on close, including audit logs
            if (file.canWrite()) {
              // If a previous test was exited early by a developer, the file may still be
              // writeable. That makes it pretty annoying for developers to see these spurious
              // failures. As a safeguard, we'll only check for writeability for files
              // created in the last five minutes -- well over the duration of any test we
              // have today.
              if (file.lastModified() > System.currentTimeMillis() - 1000 * 60 * 5) {
                fail("File must not be writeable: %s".formatted(file.getAbsolutePath()));
              } else {
                Logger.warning(
                        "File was writeable, but it was created more than five minutes ago"
                                + " so we assume a previous test failed to clean it up: %s",
                        file.getAbsolutePath());
              }
            }
            // Ensure it can be deleted -- make it writeable now.
            boolean writeableSucceeded = file.setWritable(true);
            if (!writeableSucceeded) {
              Logger.warning("Failed to set file to writeable: %s", file.getAbsolutePath());
            }
            Files.delete(file.toPath());
          } catch (IOException exception) {
            Logger.severe("Error deleting file: %s\n%s", file.getAbsolutePath(), exception);
          }
        } else {
          dirsToDeleteIfEmpty.add(file);
          Logger.info(file.getName());
          if (filename.endsWith(" Checksums")
                  || filename.equals("Log")
                  || filename.equals("Tabulate by Batch")
                  || filename.equals("Tabulate by Precinct")) {
            File[] subdirFiles = file.listFiles();
            if (subdirFiles != null) {
              files.addAll(List.of(subdirFiles));
            }
          }
        }
      }
    }


    // Clean up empty directories
    while (!dirsToDeleteIfEmpty.isEmpty()) {
      File dir = dirsToDeleteIfEmpty.pop();
      Logger.info(dir.getName());
      File[] listOfFiles = dir.listFiles();
      if (listOfFiles == null || listOfFiles.length == 0) {
        try {
          Files.delete(dir.toPath());
        } catch (IOException exception) {
          Logger.severe("Error deleting directory: %s\n%s", dir.getAbsolutePath(), exception);
        }
      }
    }

    Logger.info("Test complete.");
  }

  private static void compareFiles(
      ContestConfig config, String stem, String timestampString, Integer sequentialId) {
    compareFiles(config, stem, OutputType.DETAILED_JSON, timestampString, sequentialId, false);
    compareFiles(config, stem, OutputType.DETAILED_CSV, timestampString, sequentialId, false);
    compareExtendedSummaryToSummary(config, timestampString, sequentialId);
    if (config.isGenerateCdfJsonEnabled()) {
      compareFiles(config, stem, OutputType.CDF_CVR, timestampString, sequentialId, false);
    }
  }

  /**
   * Helper comparison for non-slice files.
   */
  private static boolean compareFiles(
          ContestConfig config,
          String stem,
          OutputType outputType,
          String timestampString,
          Integer sequentialId,
          boolean onlyCheckIfExpectedFileExists) {
    OutputFileIdentifiers actualOutputFileIdentifiers = new OutputFileIdentifiers(outputType);
    return compareFiles(
            config,
            stem,
            actualOutputFileIdentifiers,
            timestampString,
            sequentialId,
            onlyCheckIfExpectedFileExists);
  }

  /**
   * Returns whether the files were compared at all.
   * If they were compared and not equal, the test will fail.
   */
  private static boolean compareFiles(
      ContestConfig config,
      String stem,
      OutputFileIdentifiers actualOutputFileIdentifiers,
      String timestampString,
      Integer sequentialId,
      boolean onlyCheckIfExpectedFileExists) {
    String resultsDir = config.getOutputDirectory(timestampString);
    String actualOutputPath = actualOutputFileIdentifiers.getPath(
          resultsDir, timestampString, sequentialId).toAbsolutePath().toString();
    String expectedPath = actualOutputFileIdentifiers.getPath(getTestDirectory(stem).toString(),
            stem, "expected", sequentialId).toString();

    Logger.info("Comparing files:\nGenerated: %s\nReference: %s", actualOutputPath, expectedPath);
    boolean didCompare = true;
    if (onlyCheckIfExpectedFileExists && !new File(expectedPath).exists()) {
      didCompare = false;
      Logger.info("Skipping comparison: expected file does not exist.");
    } else if (fileCompare(expectedPath, actualOutputPath)) {
      Logger.info("Files are equal.");
    } else {
      Logger.info("Files are different.");
      fail();
    }
    return didCompare;
  }

  /**
   * Rather than storing both the extended summary and non-extended summary files in git, we can
   * directly check that the non-extended file is precisely what we expect: everything in the
   * extended file except for the inactive ballot breakdown.
   */
  private static void compareExtendedSummaryToSummary(
          ContestConfig config, String timestampString, Integer sequentialId) {
    String dir = config.getOutputDirectory(timestampString);
    String summaryPath = new OutputFileIdentifiers(OutputType.SUMMARY_CSV).getPath(
            dir, timestampString, sequentialId).toAbsolutePath().toString();
    String detailedPath = new OutputFileIdentifiers(OutputType.DETAILED_CSV).getPath(
            dir, timestampString, sequentialId).toAbsolutePath().toString();

    try (BufferedReader brSummary = new BufferedReader(new FileReader(summaryPath, UTF_8));
         BufferedReader brDetailed = new BufferedReader(new FileReader(detailedPath, UTF_8))) {
      while (true) {
        String lineDetailed = brDetailed.readLine();
        // If the extended file has reached its end, then the non-extended file must have too
        if (lineDetailed == null) {
          assertNull(brSummary.readLine(), "Extended file is missing a line");
          return;
        }

        // If the extended file should be excluded, continue without moving the file pointer
        // in the non-extended file. For now, there's only one type of row excluded, and they
        // happen to all start with "Inactive Ballots by"
        if (lineDetailed.startsWith("Inactive Ballots by")) {
          continue;
        }

        // This line should be equal in both files. Ensure the line exists and they're equal
        // in both files.
        String lineSummary = brSummary.readLine();
        assertNotNull(lineSummary, "Summary file is missing a line");
        if (!lineSummary.equals(lineDetailed)) {
          fail("Line differs in extended vs non-extended CSV: %n%s%n%s".formatted(
                  lineSummary, lineDetailed));
        }
      }
    } catch (FileNotFoundException exception) {
      Logger.severe("File not found!\n%s", exception);
      fail();
    } catch (IOException exception) {
      Logger.severe("Error reading file!\n%s", exception);
      fail();
    }
  }

  @BeforeAll
  static void setup() {
    Logger.setup();
    SecurityConfig.setEnableValidationForUnitTests(false);
    SecurityConfig.setAllowUsersDirectorySavingForUnitTests(true);
  }

  @Test
  @DisplayName("Test Convert to CDF works for CDF")
  void convertToCdfFromCdf() {
    runConvertToCdfTest("conversions_from_cdf");
  }

  @Test
  @DisplayName("Test Convert to CDF works for Dominion")
  void convertToCdfFromDominion() {
    runConvertToCdfTest("conversions_from_dominion");
  }

  @Test
  @DisplayName("Test Convert to CDF works for ES&S")
  void convertToCdfFromEss() {
    runConvertToCdfTest("conversions_from_ess");
  }

  @Test
  @DisplayName("Test Convert to rctab_cvr works for CDF")
  void convertToRctabCvrFromCdf() {
    runConvertToRctabCvrTest("conversions_from_cdf");
  }

  @Test
  @DisplayName("Test Convert to rctab_cvr works for Dominion")
  void convertToRctabCvrFromDominion() {
    runConvertToRctabCvrTest("conversions_from_dominion");
  }

  @Test
  @DisplayName("Test Convert to rctab_cvr works for ES&S")
  void convertToRctabCvrFromEss() {
    runConvertToRctabCvrTest("conversions_from_ess");
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
  @DisplayName("Clear Ballot - Inline Comma Parsing")
  void testClearBallotInlineComma() {
    runTabulationTest("clear_ballot_with_inline_comma");
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
  @DisplayName("Dominion test - Alaska 2024 Partial Ballot Test")
  void testDominionAlaskaPartialBallot() {
    runTabulationTest("2024_alaska_dominion");
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
  @DisplayName("More winners allowed than total candidates running is okay")
  void testMoreWinnersThanCandidates() {
    runTabulationTest("more_winners_than_candidates");
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
    runTabulationTest("2017_minneapolis_mayor", 4);
  }

  @Test
  @DisplayName("2013 Minneapolis Mayor")
  void test2013MinneapolisMayor() {
    runTabulationTest("2013_minneapolis_mayor", 4);
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
    runTabulationTest("minneapolis_multi_seat_threshold", 3);
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
  @DisplayName("test bottoms-up multi-seat with static threshold logic")
  void testBottomsUpMultiSeatWithStaticThreshold() {
    runTabulationTest("multi_seat_bottoms_up_with_static_threshold");
  }

  @Test
  @DisplayName("test bottoms-up multi-seat with dynamic threshold logic")
  void testBottomsUpMultiSeatWithDynamicThreshold() {
    runTabulationTest("multi_seat_bottoms_up_with_dynamic_threshold");
  }

  @Test
  @DisplayName("test allow only one winner per round logic")
  void testAllowOnlyOneWinnerPerRound() {
    runTabulationTest("test_set_allow_only_one_winner_per_round");
  }

  @Test
  @DisplayName("tabulate by precinct")
  void precinctExample() {
    runTabulationTest("precinct_example", 2);
  }

  @Test
  @DisplayName("tabulate by batch")
  void batchExample() {
    runTabulationTest("batch_example", 2);
  }

  @Test
  @DisplayName("missing precinct example")
  void missingPrecinctExample() {
    runTabulationTest("missing_precinct_example", 4);
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
  @DisplayName("continue until two threshold freeze test")
  void continueUntilTwoThresholdFreeze() {
    runTabulationTest("continue_until_two_threshold_freeze");
  }

  @Test
  @DisplayName("first round determine threshold test")
  void firstRoundDeterminesThresholdTest() {
    runTabulationTest("first_round_determines_threshold_test");
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
    runTabulationTest("generic_csv_test", 6);
  }

  @Test
  @DisplayName("CSV missing header test")
  void csvMissingHeaderTest() {
    runTabulationTest("csv_missing_header_test");
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

  @Test
  @DisplayName("halting error when CVRs have a ranking larger than the max-configured value")
  void maxRankingValidationFails() {
    runTabulationTest("max_ranking_enforcement",
        TabulatorSession.CastVoteRecordGenericParseException.class.toString());
  }
}
