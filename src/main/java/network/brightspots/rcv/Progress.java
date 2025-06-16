/*
 * RCTab
 * Copyright (c) 2017-2024 Bright Spots Developers.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Purpose: Track the progress of tabulation for a GUI progress bar.
 * Design: Tracks the number of files read, the number of candidates eliminated, and an
 * estimate of the percentage of time that will be spent doing each. Uses the percent
 * of eliminations completed as a proxy for the progress of tabulation.
 * Conditions: During tabulation, validation, and conversion.
 * Version history: see https://github.com/BrightSpots/rcv.
 */

package network.brightspots.rcv;

import java.util.function.BiConsumer;

class Progress {
  private final BiConsumer<Double, Double> progressUpdate;
  private final float estPercentTimeTabulating;
  private final int numFilesToRead;
  private final int numToEliminate;
  private int numFilesRead = 0;
  private int numEliminated = 0;

  /**
   * Uses the contest configuration to determine the number of files to read and the number of
   * candidates that may be eliminated.
   *
   * @param config The contest configuration
   * @param estPercentTimeTabulating An estimate of the percentage of time that will be spent
   *                                 tabulating, between 0 and 1. Set to 0 if no tabulation will
   *                                 be done.
   * @param progressUpdate A consumer that will be called with the current progress percentage.
   *                       May be null if not linked to a progress bar anywhere.
   */
  public Progress(ContestConfig config,
                  float estPercentTimeTabulating,
                  BiConsumer<Double, Double> progressUpdate) {
    if (!config.isMultiSeatSequentialWinnerTakesAllEnabled()) {
      this.numFilesToRead = config.rawConfig.cvrFileSources.size();
      this.numToEliminate = config.getNumCandidates() - config.getNumberOfWinners();
    } else {
      int numPasses = config.getSequentialWinners().size();

      // The maximum number of eliminations in each pass is the number of active candidates
      // minus the one winner.
      int totalEliminations = 0;
      for (int i = 0; i < numPasses; i++) {
        totalEliminations += config.getNumCandidates() - i - 1;
      }

      this.numFilesToRead = config.rawConfig.cvrFileSources.size() * numPasses;
      this.numToEliminate = totalEliminations;
    }
    this.estPercentTimeTabulating = estPercentTimeTabulating;
    this.progressUpdate = progressUpdate;
  }

  /**
   * Call this function after each CVR file is read to increment the read count.
   */
  public void markFileRead() {
    numFilesRead++;

    if (numFilesRead > numFilesToRead) {
      Logger.warning("Progress Bar error: numFilesRead exceeds numFilesToRead!");
    }

    updateConsumer();
  }

  /**
   * Call this function with the number of new eliminations that have occurred.
   * Do not call with the total number of eliminations. This function increments
   * the elimination count by the given number.
   */
  public void markCandidatesEliminated(int numEliminated) {
    this.numEliminated += numEliminated;

    if (this.numEliminated > numToEliminate) {
      Logger.warning("Progress Bar error: numBallotsTabulated exceeds numBallotsToTabulate!");
    }

    updateConsumer();
  }

  private void updateConsumer() {
    double percentFilesRead = (double) numFilesRead / numFilesToRead;
    double percentEliminationsComplete = numToEliminate != 0
            ? (double) numEliminated / numToEliminate
            : 0;
    if (progressUpdate != null) {
      progressUpdate.accept(
              percentFilesRead * (1 - estPercentTimeTabulating)
                      + percentEliminationsComplete * estPercentTimeTabulating,
              1.0);
    }
  }
}
