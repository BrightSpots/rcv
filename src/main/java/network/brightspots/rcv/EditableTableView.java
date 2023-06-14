/**
 * TODO
 */
package network.brightspots.rcv;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

/**
 * Extended TableView that supports terminating an edit.
 *
 * Implemented by a custom property terminatingCell that supporting
 * TableCells can listen to and react as appropriate.
 *
 * Collaborators:
 * - an extended TableCellBehaviour that calls tableView.terminateEdit on
 *   simpleSelect before messaging super
 * - an extended TableCell that is configured with the extended TableCellBehaviour
 *   and listens to the table's terminatingCell property.
 *
 * Note: all TableCells in this table need the extended behaviour.
 *
 * @author Jeanette Winzenburg, Berlin
 */
public class EditableTableView<S> extends TableView<S> {

    /**
     * Setting the terminatingCell propertry to the previous edit before
     * calling super.
     *
     * Note JW: works in that it commits after selecting a cell different
     * from the currently editing, but disables a plain esc.
     */
    @Override
    public void edit(int row, TableColumn<S, ?> column) {
        setTerminatingCell(getEditingCell());
        super.edit(row, column);
        setTerminatingCell(null);
    }

    public void terminateEdit() {
        if (!isEditing()) return;
        setTerminatingCell(getEditingCell());
        if (isEditing()) throw new IllegalStateException("expected editing to be terminated but was " + getEditingCell());
        setTerminatingCell(null);
    }

    /**
     * Returns a boolean indicating whether this table is currently editing.
     *
     * PENDING JW: what's the exact semantics of editingCell? here we check
     * for null, what if != with row < 0 and tableColumn != null?
     *
     * @return
     */
    public boolean isEditing() {
        return getEditingCell() != null;
    }

    /**
     * terminatingCell is the table position that is currently editing
     * and should terminate. It is set in edit(row, column) to the currently editing cell
     * before calling super and reset to null after calling super.
     * TableCells that support terminating an edit
     * can listen and commit as appropriate (at least that's the idea).
     */
    private ReadOnlyObjectWrapper<TablePosition<S,?>> terminatingCell;

    protected void setTerminatingCell(TablePosition<S, ?> terminatingPosition) {
        terminatingCellPropertyImpl().set(terminatingPosition);
    }

    /**
     * Represents the current cell being edited, or null if
     * there is no cell being edited.
     */
    public final ReadOnlyObjectProperty<TablePosition<S,?>> terminatingCellProperty() {
        return terminatingCellPropertyImpl().getReadOnlyProperty();
    }
    public TablePosition<S, ?> getTerminatingCell(){
        return terminatingCellPropertyImpl().get();
    }

    private ReadOnlyObjectWrapper<TablePosition<S,?>> terminatingCellPropertyImpl() {
        if (terminatingCell == null) {
            terminatingCell = new ReadOnlyObjectWrapper<TablePosition<S,?>>(this, "terminatingCell");
        }
        return terminatingCell;
    }

}
