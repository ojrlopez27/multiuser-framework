package edu.cmu.inmind.multiuser.controller.composer.ui;

import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;

import javax.swing.*;
import java.beans.*;

/*
 *  This class listens for changes made to the data in the table via the
 *  TableCellEditor. When editing is started, the value of the cell is saved
 *  When editing is stopped the new value is saved. When the oold and new
 *  values are different, then the provided Action is invoked.
 *
 *  The source of the Action is a TableCellListener instance.
 */
public class TableCellListener implements PropertyChangeListener, Runnable{
    private int row;
    private int column;
    private Object oldValue;
    private Object newValue;
    private JTable table;


    public TableCellListener(JTable table) {
        this.table = table;
    }

    /**
     *  Get the column that was last edited
     *
     *  @return the column that was edited
     */
    public int getColumn()
    {
        return column;
    }

    /**
     *  Get the new value in the cell
     *
     *  @return the new value in the cell
     */
    public Object getNewValue()
    {
        return newValue;
    }

    /**
     *  Get the old value of the cell
     *
     *  @return the old value of the cell
     */
    public Object getOldValue()
    {
        return oldValue;
    }

    /**
     *  Get the row that was last edited
     *
     *  @return the row that was edited
     */
    public int getRow()
    {
        return row;
    }

    //
//  Implement the PropertyChangeListener interface
//
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        //  A cell has started/stopped editing

        if ("tableCellEditor".equals(e.getPropertyName()))
        {
            if (table.isEditing())
                processEditingStarted();
            else
                processEditingStopped();
        }
    }

    /*
     *  Save information of the cell about to be edited
     */
    private void processEditingStarted()
    {
        //  The invokeLater is necessary because the editing row and editing
        //  column of the table have not been set when the "tableCellEditor"
        //  PropertyChangeEvent is fired.
        //  This results in the "run" method being invoked

        SwingUtilities.invokeLater( this );
    }
    /*
     *  See above.
     */
    @Override
    public void run()
    {
        row = table.convertRowIndexToModel( table.getEditingRow() );
        column = table.convertColumnIndexToModel( table.getEditingColumn() );
        oldValue = table.getModel().getValueAt(row, column);
        newValue = null;
    }

    /*
     *	Update the Cell history when necessary
     */
    private void processEditingStopped(){
        newValue = table.getModel().getValueAt(row, column);
        if (! newValue.equals(oldValue)){
            String symbol = (String) table.getModel().getValueAt(row, column - 1);
            BehaviorNetwork network = ((ParametersModel) table.getModel()).getNetwork();
            Double newValDouble = (Double) newValue;
            if( symbol.equals("π") ) network.setPi(newValDouble);
            else if( symbol.equals("θ") ) network.setTheta(newValDouble);
            else if( symbol.equals("φ") ) network.setPhi(newValDouble);
            else if( symbol.equals("δ") ) network.setDelta(newValDouble);
            else if( symbol.equals("γ") ) network.setGamma(newValDouble);
        }
    }
}
