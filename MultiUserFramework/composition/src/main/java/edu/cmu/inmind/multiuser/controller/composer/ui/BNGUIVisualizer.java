package edu.cmu.inmind.multiuser.controller.composer.ui;

import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import static edu.cmu.inmind.multiuser.controller.composer.ui.GuiHelper.*;

/**
 * Created by oscarr on 6/11/18.
 */
public class BNGUIVisualizer extends JFrame{

    private ParametersPanel parametersPanel;
    private BehaviorsPanel behaviorsPanel;
    private ExecutionPanel executionPanel;
    private JPanel plotPanel;
    private BNXYPlot bnXYPlot;
    private BehaviorNetwork network;
    private boolean paused;
    static final String metal = UIManager.getSystemLookAndFeelClassName();

    public BNGUIVisualizer(String title, String[] series, BehaviorNetwork network) throws Exception {
        super(title);
        this.network = network;

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "WikiTeX");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // metal
        renderPanels(series);

        // closing the window
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                dispose();
                System.exit(0);
            }
        });

    }


    private void renderPanels(String[] series) {
        Container content = getContentPane();
        content.setLayout( new GridBagLayout() );
        parametersPanel = new ParametersPanel(new GridBagLayout(), widthFirstPanel, heightFirstPanel, network);
        behaviorsPanel = new BehaviorsPanel(new GridBagLayout(), widthSecondPanel, heightFirstPanel, network);
        executionPanel = new ExecutionPanel(new GridBagLayout(), widthThirdPanel, heightFirstPanel, network, this);
        bnXYPlot = new BNXYPlot(series, widthFirstPanel + widthSecondPanel + widthThirdPanel, heightSecondPanel);
        bnXYPlot.setPlotObserver(executionPanel);
        plotPanel = bnXYPlot.getPanel();

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        add(parametersPanel, c);
        add(Box.createRigidArea(new Dimension(0, 40)));

        c.gridx = 1;
        c.gridy = 0;
        add(behaviorsPanel, c);
        add(Box.createRigidArea(new Dimension(0, 40)));

        c.gridx = 2;
        c.gridy = 0;
        add(executionPanel, c);
        add(Box.createRigidArea(new Dimension(0, 40)));

        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 1;
        add(plotPanel, c);
        add(Box.createRigidArea(new Dimension(0, 40)));

        pack();
        setVisible(true);
        setResizable(true);
    }


    public static void main(String args[]) throws Exception{
        start("title", new String[]{"s1", "s2", "s3"}, null);
    }

    public static BNGUIVisualizer start(String title, String[] series, BehaviorNetwork network){
        try {
            init();
            return new BNGUIVisualizer(title, series, network);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static void init() throws Exception{
        UIManager.setLookAndFeel(metal);
        JFrame.setDefaultLookAndFeelDecorated(true);
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setDataset(List<Double>[] normalizedActivations, double threshold,
                           String behActivated, double activationBeh, boolean isExecutable) {
        bnXYPlot.setDataset(normalizedActivations, threshold, behActivated, activationBeh, isExecutable);
    }
}
