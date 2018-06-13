package edu.cmu.inmind.multiuser.controller.composer.ui;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.composer.bn.Premise;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.List;
import java.util.Vector;

/**
 * Created by oscarr on 6/11/18.
 */
public class BehaviorsPanel extends JPanel {
    private DefaultMutableTreeNode treeNode;
    private JTree behaviorTree;
    private BehaviorNetwork network;

    public BehaviorsPanel(LayoutManager layout, double width, double height, BehaviorNetwork network) {
        super(layout);
        this.network = network;
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), " Network Structure and State ") );
        setPreferredSize(new Dimension((int) width, (int) height));

        //create the root node
        treeNode = new DefaultMutableTreeNode("Behaviors");
        //create the tree by passing in the root node
        behaviorTree = new JTree(treeNode);

        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) behaviorTree.getCellRenderer();
        BehaviorTreeCellEditor behaviorEditor = new BehaviorTreeCellEditor();
        BehaviorDefTreeCellEditor editor = new BehaviorDefTreeCellEditor(behaviorTree, renderer, behaviorEditor);
        behaviorTree.setCellEditor(editor);
        behaviorTree.setEditable(true);


        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0,10,0,0);
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        JScrollPane scrollPane = new JScrollPane(behaviorTree);
        scrollPane.setPreferredSize(new Dimension( (int) width - 50, (int) height - 30));
        add(scrollPane, c);

        populate();
    }

    public void populate() {
        treeNode.removeAllChildren();
        //create the child nodes
        for (Behavior behavior : network.getBehaviors()) {
            DefaultMutableTreeNode behaviorNode = new DefaultMutableTreeNode(behavior.getName());
            treeNode.add(behaviorNode);
            for (List<Premise> premises : behavior.getPreconditions() ) {
                for(Premise premise : premises){
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(String.format("%s  :  [%s]",
                            premise.getLabel(), premise.getWeight()) );
                    behaviorNode.add(node);
                }
            }
        }
        ((DefaultTreeModel) behaviorTree.getModel()).reload();
        behaviorTree.getCellEditor().addCellEditorListener( new BehaviorNodeListener() );
    }

    private class BehaviorNodeListener implements CellEditorListener {

        @Override
        public void editingStopped(ChangeEvent e) {
            BehaviorEditor editor = (BehaviorEditor) e.getSource();
            network.setPremiseWeight(editor.getBehaviorName(), editor.getPremiseName(), editor.getWeight());
        }

        @Override
        public void editingCanceled(ChangeEvent e) {
            System.out.println("editingCanceled");
        }
    }

    class BehaviorTreeCellEditor implements TreeCellEditor{
        private BehaviorEditor leafEditor;
        private CellEditor currentEditor;

        public BehaviorTreeCellEditor() {
            leafEditor = new BehaviorEditor();
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            currentEditor = leafEditor;
            if (leaf) {
                leafEditor.setBehaviorName(((DefaultMutableTreeNode) value).getParent().toString());
                leafEditor.setText(value.toString());
            }
            return (Component) currentEditor;
        }

        @Override
        public Object getCellEditorValue() {
            return currentEditor.getCellEditorValue();
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public boolean shouldSelectCell(EventObject anEvent) {
            return currentEditor.shouldSelectCell(anEvent);
        }

        @Override
        public boolean stopCellEditing() {
            return currentEditor.stopCellEditing();
        }

        @Override
        public void cancelCellEditing() {
            currentEditor.cancelCellEditing();
        }

        public void addCellEditorListener(CellEditorListener l) {
            leafEditor.addCellEditorListener(l);
        }

        public void removeCellEditorListener(CellEditorListener l) {
            leafEditor.removeCellEditorListener(l);
        }
    }


    class BehaviorEditor extends JTextField implements CellEditor {
        private Vector listeners = new Vector();
        private String behaviorName;
        private String premiseName;
        private Double weight;

        // Mimic all the constructors people expect with text fields.
        public BehaviorEditor() {
            this("", 20);
        }

        public String getBehaviorName() {
            return behaviorName;
        }

        public String getPremiseName() {
            return premiseName;
        }

        public Double getWeight() {
            return weight;
        }

        public void setBehaviorName(String behaviorName) {
            this.behaviorName = behaviorName;
        }

        public BehaviorEditor(String s, int w) {
            super(s, w);
            setPreferredSize(new Dimension(300, 26));

            // Listen to our own action events so that we know when to stop editing.
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (stopCellEditing()) {
                        fireEditingStopped();
                    }
                }
            });
        }

        // Implement the CellEditor methods.
        public void cancelCellEditing() {
            setText("");
        }

        // Stop editing only if the user entered a valid value.
        public boolean stopCellEditing() {
            try {
                String tmp = getText();
                int idx = tmp.indexOf(":");
                if (idx != -1) idx = tmp.indexOf("[", idx);
                if(idx != -1) idx = tmp.indexOf("]", idx);
                if(idx != -1) return true;
                return false;
            } catch (Exception e) {
                // Something went wrong (most likely we don't have a valid integer).
                return false;
            }
        }

        public Object getCellEditorValue() {
            return getText();
        }

        public boolean isCellEditable(EventObject eo) {
            return true;
        }

        public boolean shouldSelectCell(EventObject eo) {
            return true;
        }

        // Add support for listeners.
        public void addCellEditorListener(CellEditorListener cel) {
            listeners.addElement(cel);
        }

        public void removeCellEditorListener(CellEditorListener cel) {
            listeners.removeElement(cel);
        }

        protected void fireEditingStopped() {
            String value = getText();
            premiseName = value.substring(0, value.indexOf(":") - 2);
            int idxStart = value.indexOf("["), idxEnd = value.indexOf("]");
            weight = Double.parseDouble(value.substring(idxStart+1, idxEnd));

            if (listeners.size() > 0) {
                ChangeEvent ce = new ChangeEvent(this);
                for (int i = listeners.size() - 1; i >= 0; i--) {
                    ((CellEditorListener) listeners.elementAt(i)).editingStopped(ce);
                }
            }
        }
    }


    class BehaviorDefTreeCellEditor extends DefaultTreeCellEditor{
        public BehaviorDefTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
        }

        public BehaviorDefTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer, TreeCellEditor editor) {
            super(tree, renderer, editor);
        }

        @Override
        protected boolean canEditImmediately(EventObject event) {
            if((event instanceof MouseEvent) && SwingUtilities.isLeftMouseButton((MouseEvent)event)) {
                MouseEvent me = (MouseEvent)event;
                return (inHitRegion(me.getX(), me.getY()));
            }
            return (event == null);
        }
    }
}
