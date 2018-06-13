package edu.cmu.inmind.multiuser.controller.composer.ui;



import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;

/**
 * Created by oscarr on 3/1/18.
 */
public class GuiHelper {

    public static final String TEXT_PANE_SEPATATOR = "\n▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔▔";//"\n===========";

    public static final String NEW_GAME_SEPARATOR = "==> NEW GAME. *** GAME %s ***\n";
    public static final String HIGHLIGHT_TOKEN = "$$$$";

    // Constants for Colors
    public static final Color BACKGROUND = Color.decode("#2B2B2B");
    public static final Color NORMAL_TEXT = Color.WHITE; //Color.decode("#A9B7C6"); // Color.decode("#9876AA") //Color.WHITE;
    public static final Color TITLE_TEXT = Color.GREEN;
    public static final Color SUBTITLE_TEXT = Color.ORANGE;
    public static final Color USER_TEXT = Color.GREEN;
    public static final Color AGENT_TEXT = Color.ORANGE;
    public static final Color SIGNAL_TEXT = Color.CYAN;
    public static final Color HIGHLIGHTED_TEXT = Color.YELLOW;
    public static final Color TREE_BACKGROUND = Color.RED;

    public static double widthFirstPanel;
    public static double heightFirstPanel;
    public static double widthSecondPanel;
    public static double heightSecondPanel;  //780 - heightFirstPanel;

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
}


class StatePane extends JTextPane {
    private HTMLDocument doc;
    private StringBuilder text = new StringBuilder();

    public StatePane() {
        super();
        setContentType("text/html");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        doc = (HTMLDocument) getStyledDocument();
    }


    public void addText(String txt){
        text.append( GuiHelper.convertToHTML(txt, ", ") );
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

//
//class ColorPane extends JTextPane {
//    private StyleContext sc;
//    private AttributeSet aset;
//
//    public ColorPane() {
//        super();
//        // StyleContext
//        sc = StyleContext.getDefaultStyleContext();
//        changeAttSet(NORMAL_TEXT, 12);
//        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Menlo"); //Lucida Console
//        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_LEFT);
//    }
//
//    public void appendWithColor(Color color, String text, int fontSize, boolean newLine) {
//        changeAttSet(color, fontSize);
//        replaceSelection(text + (newLine? "\n" : "")); // there is no selection, so inserts at caret
//    }
//
//    public void setText(Color color, String text, int fontSize, boolean newLine){
//        changeAttSet(color, fontSize);
//        setText(text + (newLine? "\n" : "")); // there is no selection, so inserts at caret
//    }
//
//    private void changeAttSet(Color color, int fontSize){
//        aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);
//        aset = sc.addAttribute(aset, StyleConstants.FontSize, fontSize);
//        int len = getDocument().getLength();// same value as getText().length();
//        setCaretPosition(len); // place caret at the end (with no selection)
//        setCharacterAttributes(aset, false);
//    }
//}