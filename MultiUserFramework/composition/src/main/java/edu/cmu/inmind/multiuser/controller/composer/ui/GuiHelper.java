package edu.cmu.inmind.multiuser.controller.composer.ui;



import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;

/**
 * Created by oscarr on 3/1/18.
 */
public class GuiHelper {

    public static double widthFirstPanel;
    public static double heightFirstPanel;
    public static double widthSecondPanel;
    public static double heightSecondPanel;  //780 - heightFirstPanel;
    private static final String style = "font-family: Monospaced, Calibri, Lucida Console, Menlo; font-size: 12px";

    static {
        init( new Dimension(1420, 780)) ;
    }


    public static void init(Dimension dim){
        widthFirstPanel = dim.getWidth() * 0.40;
        widthSecondPanel = dim.getWidth() - widthFirstPanel;
        heightFirstPanel = dim.getHeight() * 0.40;
        heightSecondPanel =  dim.getHeight() - heightFirstPanel;
    }

    public static String convertToHTML(String text, String token) {
        String formattedText = "<HTML>";
        for( String line : text.split(token) ){
            formattedText += line + "<br>";
        }
        formattedText += "</HTML>";
        return formattedText;
    }


    static class StatePane extends JTextPane {
        private HTMLDocument doc;
        private StringBuilder text = new StringBuilder();

        public StatePane() {
            super();
            setContentType("text/html");
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            doc = (HTMLDocument) getStyledDocument();
        }


        public void addText(String txt){
            text.append( GuiHelper.convertToHTML(txt, ", "));
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

