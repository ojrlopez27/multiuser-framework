package edu.cmu.inmind.multiuser.controller.composer.ui;

import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by oscarr on 6/18/18.
 */
public class ExecutionPanel extends JPanel implements ActionListener, PlotObserver{

    private GuiHelper.MultilinePane winningService;
    private BehaviorNetwork network;
    private BNGUIVisualizer visualizer;
    private boolean paused;
    private final static String pickAutoCommand = "pickAutomatically";
    private final static String userSelectCommand = "userSelection";
    private final static String selectButtonCommand = "selectButton";
    private final static String pauseButtonCommand = "pauseButton";

    public ExecutionPanel(LayoutManager layout, double width, double height, BehaviorNetwork network,
                          BNGUIVisualizer visualizer) {
        super(layout);
        this.network = network;
        this.visualizer = visualizer;
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
        this.add(new JLabel("(Last) Winner Service:"), c);

        c.insets = new Insets(10,10,0,10);
        c.gridx = 0;
        c.gridy = 2;
        winningService = new GuiHelper.MultilinePane();
        winningService.addText("NO WINNING SERVICE");
        winningService.build();
        winningService.setPreferredSize(new Dimension((int)(width - 60), (int) (height * 0.25)));
        JScrollPane scrollPane = new JScrollPane(winningService);
        scrollPane.setPreferredSize(new Dimension(200, 80));
        this.add(scrollPane, c);

        c.insets = new Insets(20,0,0,0);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        JButton selectService = new JButton("Pick Service");
        selectService.addActionListener(this);
        selectService.setActionCommand(selectButtonCommand);
        this.add(selectService, c);

        c.gridx = 1;
        c.gridy = 3;
        JButton pause = new JButton("Pause");
        pause.addActionListener(this);
        pause.setActionCommand(pauseButtonCommand);
        this.add(pause, c);
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
            visualizer.setPaused(paused);
        }
    }

    @Override
    public void onWinnerService(String shortName) {
        winningService.reset();
        winningService.addText(String.format("%s&emsp;&emsp;%s", shortName,
                network.getBehaviorByShortName(shortName).getName()));
        winningService.build();
    }
}
