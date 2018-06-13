package edu.cmu.inmind.multiuser.controller.composer.ui;

import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by oscarr on 6/11/18.
 */
public class ParametersModel extends AbstractTableModel{
    private final List<NetworkParameter> parameterList;
    private BehaviorNetwork network;

    private final String[] columnNames = new String[] { "Description", "Symbol", "Value" };
    private final Class[] columnClass = new Class[] {String.class, String.class, Double.class};

    public ParametersModel(BehaviorNetwork network){
        this.parameterList = new ArrayList<>();
        this.network = network;
        parameterList.add( new NetworkParameter("The mean level of activation", "π", network.getPi()) );
        parameterList.add( new NetworkParameter("The threshold of activation", "θ", network.getTheta()) );
        parameterList.add( new NetworkParameter("Energy injected by the state per true proposition", "φ", network.getPhi()) );
        parameterList.add( new NetworkParameter("Energy taken away by the protected goals", "δ", network.getDelta()) );
        parameterList.add( new NetworkParameter("Energy injected by the goals per goal", "γ", network.getGamma()) );
    }

    public BehaviorNetwork getNetwork() {
        return network;
    }

    @Override
    public String getColumnName(int column)
    {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return columnClass[columnIndex];
    }

    @Override
    public int getColumnCount()
    {
        return columnNames.length;
    }

    @Override
    public int getRowCount()
    {
        return parameterList.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex){
        NetworkParameter row = parameterList.get(rowIndex);
        if(0 == columnIndex) {
            return row.getDescription();
        }
        else if(1 == columnIndex) {
            return row.getSymbol();
        }
        else if(2 == columnIndex) {
            return row.getValue();
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex){
        NetworkParameter row = parameterList.get(rowIndex);
        if(0 == columnIndex) {
            row.setDescription( (String) aValue );
        }
        if(1 == columnIndex) {
            row.setSymbol( (String) aValue );
        }
        else if(2 == columnIndex) {
            row.setValue( (Double) aValue );
        }
    }


    class NetworkParameter{
        private String description;
        private String symbol;
        private Double value;

        public NetworkParameter(String description, String symbol, Double value) {
            this.description = description;
            this.symbol = symbol;
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }
    }
}