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

package network.brightspots.rcv;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

@SuppressWarnings("WeakerAccess")
public class GuiTiebreakerController {

  private String candidateToEliminate = null;
  private boolean tabulationCancelled = false;

  @FXML
  private ListView<String> listViewCandidates;

  String getCandidateToEliminate() {
    return candidateToEliminate;
  }

  private void setCandidateToEliminate(String candidateToEliminate) {
    this.candidateToEliminate = candidateToEliminate;
  }

  boolean getTabulationCancelled() {
    return tabulationCancelled;
  }

  private void setTabulationCancelled(boolean tabulationCancelled) {
    this.tabulationCancelled = tabulationCancelled;
  }

  void populateTiedCandidates(List<String> tiedCandidates) {
    listViewCandidates.setItems(FXCollections.observableArrayList(tiedCandidates));
  }

  public void buttonCancelClicked(ActionEvent actionEvent) {
    setTabulationCancelled(true);
    ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
  }

  public void buttonEliminateClicked(ActionEvent actionEvent) {
    if (listViewCandidates.getSelectionModel().getSelectedItems().size() == 1) {
      setCandidateToEliminate(listViewCandidates.getSelectionModel().getSelectedItems().get(0));
      ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
  }
}
