package edu.cmu.inmind.multiuser.controller.composer.ui;



import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.util.*;

/**
 * Created by oscarr on 3/1/18.
 */
public class GuiHelper {

    public static double widthFirstPanel;
    public static double heightFirstPanel;
    public static double widthSecondPanel;
    public static double heightSecondPanel;  //780 - heightFirstPanel;
    public static double widthThirdPanel;
    private static final String tableStyle = "font-family: Calibri, Lucida Console, Menlo; width: 100%;";//  border-collapse: collapse;  ";
    private static final String commonStyle = "font-size: 11px; text-align: left; padding: 0px;"; //border: 1px solid #dddddd;"; //padding: 0px; "; //
    private static final String header = "<head> <style> " +
            "table {" + tableStyle + "} " +
            "th {" + commonStyle + ";} " +
            "td {" + commonStyle + ";} " +
            "tr:nth-child(even) {background-color: #dddddd;}" +
            "</style></head>";

    static {
        init( new Dimension(1420, 780)) ;
    }


    public static void init(Dimension dim){
        widthFirstPanel = dim.getWidth() * 0.35;
        widthSecondPanel =  dim.getWidth() * 0.40;
        widthThirdPanel = dim.getWidth() - (widthFirstPanel + widthSecondPanel);
        heightFirstPanel = dim.getHeight() * 0.40;
        heightSecondPanel =  dim.getHeight() - heightFirstPanel;
    }

    public static String convertToHTML(String text, String token) {
        return convertToHTML(Arrays.asList(text.split(", ")));
    }

    public static String convertToHTML(java.util.List<String> text){
        String formattedText = "<HTML>" + header;
        for( String line : text ){
            String[] split = line.split(":");
            formattedText += String.format("<tr><td>%s</td><td>%s</td></tr>",
                    split[0], split.length>1? split[1] : "");
        }
        formattedText += "</table></HTML>";
        return formattedText;
    }


    static class MultilinePane extends JTextPane {
        private HTMLDocument doc;
        private StringBuilder text = new StringBuilder();

        public MultilinePane() {
            super();
            setContentType("text/html");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            doc = (HTMLDocument) getStyledDocument();
            setBorder(BorderFactory.createEtchedBorder());
            setOpaque(true);
            setBackground(Color.WHITE);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        }


        public void addText(String txt){
            text.append( GuiHelper.convertToHTML(txt, ", "));
        }

        public void addText(java.util.List<String> txt){
            text.append( GuiHelper.convertToHTML(txt) );
        }

        public void reset(){
            setText("");
            text = new StringBuilder();
        }

        public void build(){
            try {
                doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), text.toString());
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}

