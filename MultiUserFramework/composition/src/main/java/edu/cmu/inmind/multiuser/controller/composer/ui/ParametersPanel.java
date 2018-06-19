package edu.cmu.inmind.multiuser.controller.composer.ui;

import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.composer.bn.StateObserver;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Created by oscarr on 6/11/18.
 */
public class ParametersPanel extends JPanel implements StateObserver{
    private GuiHelper.MultilinePane stateText;
    private BehaviorNetwork network;

    public ParametersPanel(LayoutManager layout, double width, double height, BehaviorNetwork network) {
        super(layout);
        this.network = network;
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), " Network Parameters and State ") );
        setPreferredSize(new Dimension((int)width, (int)height));

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0,0,0,0);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 10;
        JLabel paramLabel = new JLabel("Parameters:");
        this.add(paramLabel, c);


        //create the model
        ParametersModel model = new ParametersModel(this.network);
        //create the table
        JTable table = new JTable(model);
        //width
        int columnWidth = (int) (width * 0.75);
        table.getColumnModel().getColumn(0).setPreferredWidth(columnWidth);
        table.getColumnModel().getColumn(1).setPreferredWidth((int) (width - columnWidth)/2);
        table.getColumnModel().getColumn(2).setPreferredWidth((int) (width - columnWidth)/2);
        //centering
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment( SwingConstants.CENTER );
        table.getColumnModel().getColumn(1).setCellRenderer( centerRenderer );
        table.getColumnModel().getColumn(2).setCellRenderer( centerRenderer );
        //grid lines
        table.setGridColor(Color.LIGHT_GRAY);
        table.addPropertyChangeListener( new TableCellListener(table) );

        c.insets = new Insets(10,0,0,0);
        c.gridx = 0;
        c.gridy = 1;
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension((int)(width - 60), (int) (height * 0.4)));
        this.add(scrollPane, c);

        c.insets = new Insets(20,0,0,0);
        c.gridx = 0;
        c.gridy = 2;
        JLabel stateLabel = new JLabel("State:");
        this.add(stateLabel, c);

        c.insets = new Insets(10,0,0,0);
        c.gridx = 0;
        c.gridy = 3;
        stateText = new GuiHelper.MultilinePane();
        stateText.addText(network.getStateString());
        stateText.build();
        stateText.setPreferredSize(new Dimension((int)(width - 60), (int) (height * 0.25)));
        JScrollPane scrollPane2 = new JScrollPane(stateText);
        scrollPane2.setPreferredSize(new Dimension(200, 80));
        this.add(scrollPane2, c);

        network.addStateObserver(this);
    }

    @Override
    public void updateState(String state) {
        stateText.reset();
        stateText.addText(state);
        stateText.build();
    }
}
