/*
 * RCTab
 * Copyright (c) 2025 Ranked Choice Voting Resource Center.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: These unit tests test various components of the CastVoteRecord class.
 * Design: Passing these tests ensures that changes to code have not altered how the CastVoteRecord
 * class works in unexpected ways. (Warning: these unit tests do not provide full test coverage of
 * the CastVoteRecord class.)
 * Conditions: During automated testing.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the CastVoteRecord.
 */
public class CastVoteRecordTests {
  String defaultComputedId = "computed123";
  String defualtSuppliedId = "supplied456";
  String idWithSpecialCharacters = "~!@#$%^&*(){}|:<>?[]`,./";
  String precinct = "precinct1";
  String batchId = "batch1";
  boolean usesLastAllowedRanking = false;
  List<Pair<Integer, String>> rankings = new ArrayList<>();

  @Nested
  @DisplayName("getId() tests")
  class GetIdTests {
    @Test
    @DisplayName("returns computedId when present and not blank")
    void returnsComputedIdWhenPresent() {
      // Arrange
      CastVoteRecord cvr = new CastVoteRecord(
              defaultComputedId,
              defualtSuppliedId,
              precinct,
              batchId,
              usesLastAllowedRanking,
              rankings
      );

      // Act
      String expectId = defaultComputedId;
      String actualId = cvr.getId();

      // Assert
      assertEquals(expectId, actualId);
    }

    @Test
    @DisplayName("returns suppliedId when computedId is null")
    void returnsSuppliedIdWhenComputedIdNull() {
      // Arrange
      CastVoteRecord cvr = new CastVoteRecord(
              null,
              defualtSuppliedId,
              precinct,
              batchId,
              usesLastAllowedRanking,
              rankings
      );

      // Act
      String expectId = defualtSuppliedId;
      String actualId = cvr.getId();

      // Assert
      assertEquals(expectId, actualId);
    }

    @Test
    @DisplayName("returns suppliedId when computedId is blank")
    void returnsSuppliedIdWhenComputedIdBlank() {
      // Arrange
      CastVoteRecord cvr = new CastVoteRecord(
              "",
              defualtSuppliedId,
              precinct,
              batchId,
              usesLastAllowedRanking,
              rankings
      );

      // Act
      String expectId = defualtSuppliedId;
      String actualId = cvr.getId();

      // Assert
      assertEquals(expectId, actualId);
    }

    @Test
    @DisplayName("returns computedId with special characters exactly as is")
    void handlesSpecialCharactersInComputedId() {
      // Arrange
      CastVoteRecord cvr = new CastVoteRecord(
              idWithSpecialCharacters,
              defualtSuppliedId,
              precinct,
              batchId,
              usesLastAllowedRanking,
              rankings
      );

      // Act
      String expectId = idWithSpecialCharacters;
      String actualId = cvr.getId();

      // Assert
      assertEquals(expectId, actualId);
    }
  }

  @Nested
  @DisplayName("getSuppliedId() tests")
  class GetSuppliedIdTests {
    @Test
    @DisplayName("returns suppliedId when present")
    void returnsSuppliedIdWhenPresent() {
      // Arrange
      CastVoteRecord cvr = new CastVoteRecord(
              defaultComputedId,
              defualtSuppliedId,
              precinct,
              batchId,
              usesLastAllowedRanking,
              rankings
      );

      // Act
      String expectId = defualtSuppliedId;
      String actualId = cvr.getSuppliedId();

      // Assert
      assertEquals(expectId, actualId);
    }

    @Test
    @DisplayName("returns empty string when suppliedId is null")
    void returnsEmptyStringWhenNull() {
      // Arrange
      CastVoteRecord cvr = new CastVoteRecord(
              defaultComputedId,
              null,
              precinct,
              batchId,
              usesLastAllowedRanking,
              rankings
      );

      // Act
      String expectId = "";
      String actualId = cvr.getSuppliedId();

      // Assert
      assertEquals(expectId, actualId);
    }

    @Test
    @DisplayName("returns suppliedId with special characters exactly as is")
    void handlesSpecialCharacters() {
      // Arrange
      CastVoteRecord cvr = new CastVoteRecord(
              defaultComputedId,
              idWithSpecialCharacters,
              precinct,
              batchId,
              usesLastAllowedRanking,
              rankings
      );

      // Act
      String expectId = idWithSpecialCharacters;
      String actualId = cvr.getSuppliedId();

      // Assert
      assertEquals(expectId, actualId);
    }
  }
}
