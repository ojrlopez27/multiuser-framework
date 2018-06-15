package edu.cmu.inmind.multiuser.controller.composer.ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class BNXYPlot extends JPanel {

    private XYSeriesCollection dataset;
    private String[] series;
    private List<Double>[] behaviors;
    private JFreeChart chart;
    private int maxNumSamples = 20;
    private ArrayList<Double> thresholds;
    private ArrayList<Object[]> activations;
    private int offset = 0;
    private IntervalMarker target;
    private double minThreshold = 99999;
    private double maxThreshold = 0;
    private double maxActivation = 0;
    private double annotationHeight = 15;
    private JPanel chartPanel;


    /**
     * Creates a new demo instance.
     */
    public BNXYPlot(String[] series, double width, double height) {
        this.series = series;
        chartPanel = createPanel();
        chartPanel.setPreferredSize(new Dimension((int)width, (int)height));
        chartPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), " Network Plot ") );
        thresholds = new ArrayList<>();
        activations = new ArrayList<>();
    }

    public JPanel getPanel() {
        return chartPanel;
    }

    public IntervalXYDataset refreshDataset(){
        if (behaviors != null) {
            //keep number of samples no bigger than maxNumSamples
            if (behaviors[0].size() > maxNumSamples) {
                for (int i = 0; i < behaviors.length; i++) {
                    behaviors[i].remove(0);
                }
                thresholds.remove(0);
                activations.remove(0);
                offset++;
            }

            List<Double>[] behaviorsCopy = new List[behaviors.length];
            int sizeSeries = series.length - 1;
            // fill with zeros
            for(int i = 0; i < behaviors.length; i++){
                behaviorsCopy[i] = new ArrayList<>(behaviors[i]);
                while(behaviorsCopy[i].size() < maxNumSamples) {
                    behaviorsCopy[i].add(0, 0.0);
                }
            }


            //Activation thresholds
            XYSeries ts = dataset.getSeries(series[sizeSeries]);
            ts.clear();
            for (int j = 0; j < thresholds.size(); j++) {
                ts.addOrUpdate(j + offset, thresholds.get(j).doubleValue());
            }

            target.setStartValue( minThreshold );
            target.setEndValue( maxThreshold );

            //behaviors
            for (int i = 0; i < sizeSeries; i++) {
                int size = behaviorsCopy[i].size();
                ts = dataset.getSeries(series[i]);
                ts.clear();
                for (int j = 0; j < size; j++) {
                    ts.addOrUpdate(j + offset, behaviorsCopy[i].get(j).doubleValue());
                    //System.out.println(String.format("Value: (%s, %s)", j + offset, behaviorsCopy[i].get(j).doubleValue()));
                }
            }

            //who is activated?
            chart.getXYPlot().clearAnnotations();
            int offsetAnnotation = maxNumSamples - activations.size();
            for (int x = 0; x < activations.size(); x++) {
                String name = (String)activations.get(x)[0];
                double widthBox = name.length()/6.0; // 6 is the longest name for the service composition example
                double y = (Double)activations.get(x)[1];
                if( name != null && !name.isEmpty() ) {
                    double heightBox = maxActivation / annotationHeight;
                    //System.out.println("$$$$ Highest activation: " + activations.get(activations.size()-1)[1] + "  height: " + heightBox );
                    double x1 = x + offsetAnnotation - (widthBox/2);
                    double x2 = x1 + widthBox;
                    double y1 = y - (heightBox/2);
                    double y2 = y1 + heightBox;

                    XYBoxAnnotation annotation = new XYBoxAnnotation(x1, y1, x2, y2, new BasicStroke(1.0F),
                            Color.BLACK, new Color(255, 0, 0, 125));
                    chart.getXYPlot().addAnnotation(annotation);

                    XYTextAnnotation annotationText = new XYTextAnnotation( name, (x + offsetAnnotation), y);
                    annotationText.setFont(new Font("SansSerif", Font.ITALIC, 11));
                    //System.out.println(String.format("Annotation: (%s, %s)", (x + offsetAnnotation), y));
                    chart.getXYPlot().addAnnotation( annotationText );
                }
            }
        }
        return dataset;
    }


    /**
     * Creates an overlaid chart.
     *
     * @return The chart.
     */
    private JFreeChart createChart() {
        createDataset();
        final JFreeChart chart = ChartFactory.createScatterPlot(
                "Spreading Activation Dynamics",
                "Time",
                "Activation",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        XYPlot plot = (XYPlot) chart.getPlot();
        target = new IntervalMarker(14, 16);
        target.setLabel("Activation Threshold");
        target.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
        target.setLabelAnchor(RectangleAnchor.LEFT);
        target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
        target.setPaint(new Color(222, 222, 255, 128));
        plot.addRangeMarker(target, Layer.BACKGROUND);
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        BasicStroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        for(int i = 0; i < series.length-1; i++ ) {
            renderer.setSeriesStroke(i, stroke);
        }
        renderer.setSeriesStroke(series.length-1, new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f ));
        plot.setRenderer(renderer);
        return chart;
    }

    /**
     * Creates a sample dataset.  You wouldn't normally hard-code the
     * population of a dataset in this way (it would be better to read the
     * values from a file or a database query), but for a self-contained demo
     * this is the least complicated solution.
     *
     * @return The dataset.
     */
    private void createDataset() {
        dataset = new XYSeriesCollection();
        for( String serieName : series ){
            XYSeries serie = new XYSeries( serieName );
            dataset.addSeries(serie);
        }
    }

    /**
     * Creates a panel for the demo (used by SuperDemo.java).
     *
     * @return A panel.
     */
    public JPanel createPanel() {
        chart = createChart();
        return new ExtChartPanel(chart);
    }

    /**
     * Starting point for the demonstration application.
     *
     * @param args  ignored.
     */
    public static void main(String[] args) {
        BNXYPlot demo = new BNXYPlot(new String[]{"beh1", "beh2", "beh3", "threshold"}, 1500, 500);


        List<Double>[] behs = new List[4];
        for(int i = 0; i < behs.length; i++ ){
            behs[i] = new ArrayList<>();
        }
        Random random = new Random();
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < behs.length; j++){
                if(j < behs.length-1) behs[j].add(random.nextInt(20) + random.nextDouble());
                else behs[j].add(15d);
            }
            demo.setDataset(behs, 15, "beh" + (random.nextInt(3) + 1), 3.3333);
        }
    }

    public void setDataset(List<Double>[] behaviors, double threshhold, String nameBehActivated, double activation) {
        this.behaviors = behaviors;
        this.thresholds.add(threshhold);
        this.activations.add( new Object[]{nameBehActivated, activation} );

        maxThreshold = Collections.max(thresholds);
        minThreshold = Collections.min(thresholds);
        if( activation > maxActivation ){
            maxActivation = activation;
        }
        double portion = (maxThreshold - minThreshold) / 6;
        if( (portion * 6) > (maxActivation/4) ) {
            maxThreshold = threshhold + portion > maxThreshold? maxThreshold : threshhold + portion;
            minThreshold = threshhold - portion < minThreshold? minThreshold : threshhold - portion;
        }
        refreshDataset();
        chart.fireChartChanged();
    }
}
