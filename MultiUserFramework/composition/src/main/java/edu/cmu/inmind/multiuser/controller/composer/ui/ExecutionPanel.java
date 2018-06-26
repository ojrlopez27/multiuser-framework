package edu.cmu.inmind.multiuser.controller.composer.ui;

import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by oscarr on 6/18/18.
 */
public class ExecutionPanel extends JPanel implements ActionListener, PlotObserver{

    private GuiHelper.MultilinePane winningService;
    private BehaviorNetwork network;
    private BNGUIVisualizer visualizer;
    private JButton pauseButton;
    private JButton selectServiceButton;
    private boolean paused;
    private final static String pickAutoCommand = "pickAutomatically";
    private final static String userSelectCommand = "userSelection";
    private final static String selectButtonCommand = "selectButton";
    private final static String pauseButtonCommand = "pauseButton";
    private java.util.List<String> listWinners;

    public ExecutionPanel(LayoutManager layout, double width, double height, BehaviorNetwork network,
                          BNGUIVisualizer visualizer) {
        super(layout);
        this.network = network;
        this.visualizer = visualizer;
        this.listWinners = new ArrayList<>();
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), " Control of Execution ") );
        setPreferredSize(new Dimension((int) width, (int) height));


        JRadioButton userSelection = new JRadioButton("Wait for user selection");
        userSelection.setActionCommand(userSelectCommand);
        userSelection.addActionListener(this);
        userSelection.setSelected(true);
        network.shouldWaitForUserSelection(true);

        JRadioButton pickAutomatically = new JRadioButton("Pick Behavior automatically");
        pickAutomatically.setActionCommand(pickAutoCommand);
        pickAutomatically.addActionListener(this);

        //Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(userSelection);
        group.add(pickAutomatically);

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new GridLayout(2, 1));
        radioPanel.add(userSelection);
        radioPanel.add(pickAutomatically);

        //... Add a titled border to the button panel.
        radioPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Execution Mode "));

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 0.5;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0,10,0,10);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        this.add(radioPanel, c);

        c.insets = new Insets(20,10,0,10);
        c.gridx = 0;
        c.gridy = 1;
        this.add(new JLabel("Winner Services:"), c);

        c.insets = new Insets(10,10,0,10);
        c.gridx = 0;
        c.gridy = 2;
        winningService = new GuiHelper.MultilinePane();
        winningService.addText("NO WINNER SERVICES");
        winningService.build();
        winningService.setPreferredSize(new Dimension((int)(width - 60), (int) (height * 0.25)));
        JScrollPane scrollPane = new JScrollPane(winningService);
        scrollPane.setPreferredSize(new Dimension(200, 80));
        this.add(scrollPane, c);

        c.insets = new Insets(20,0,0,0);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        selectServiceButton = new JButton("Pick Service");
        selectServiceButton.addActionListener(this);
        selectServiceButton.setActionCommand(selectButtonCommand);
        this.add(selectServiceButton, c);

        c.gridx = 1;
        c.gridy = 3;
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(this);
        pauseButton.setPreferredSize(new Dimension(93, 29));
        pauseButton.setActionCommand(pauseButtonCommand);
        this.add(pauseButton, c);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals(userSelectCommand)){
            network.shouldWaitForUserSelection(true);
        }else if(e.getActionCommand().equals(pickAutoCommand)){
            network.shouldWaitForUserSelection(false);
        }else if(e.getActionCommand().equals(selectButtonCommand)){
            network.pickCurrentBestBehavior();
        }else if(e.getActionCommand().equals(pauseButtonCommand)){
            paused = !paused;
            pauseButton.setText( paused? "Resume" : "Pause" );
            visualizer.setPaused(paused);
        }
    }

    @Override
    public void onWinnerService(String shortName) {
        listWinners.add(0, String.format("%s:%s", shortName,
                network.getBehaviorByShortName(shortName).getName()));
        winningService.reset();
        winningService.addText(listWinners);
        winningService.build();
    }
}
