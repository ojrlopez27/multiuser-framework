package edu.cmu.inmind.multiuser.controller.composer.ui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import java.awt.*;

/**
 * Created by oscarr on 6/13/18.
 *
 * This extension of ChartPanel intends to hide the 'weird' exceptions
 * produced while plotting the chart (IndexOutOfBoundsException).
 */
public class ExtChartPanel extends ChartPanel {
    public ExtChartPanel(JFreeChart chart) {
        super(chart);
    }

    @Override
    public void paintComponent(Graphics g) {
        try{
            super.paintComponent(g);
        }catch (Exception e){
            //e.printStackTrace();
        }
    }
}
