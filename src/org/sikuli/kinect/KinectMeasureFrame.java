package org.sikuli.kinect;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class KinectMeasureFrame extends JFrame {
   
   KinectViewer rgbViewer;
   DepthViewer depthViewer;
   ControlPanel controlPanel = new ControlPanel();
   
   KinectMeasureFrame(){
      
      setTitle("Kinect Measurer");
      
      rgbViewer = Kinect.createRGBViewer();
      depthViewer = Kinect.createDepthViewer();
      
      setSize(800,600);      
//      try {
//         rgbViewer = new RGBViewer(new File("color.png"));
//         depthViewer = new DepthViewer(new File("depth.png"));
//         rgbViewer.setPreferredSize(new Dimension(640,480));
//         depthViewer.setPreferredSize(new Dimension(640,480));
//      } catch (IOException e) {
//         
//      }
      
      selectionPanel = new SelectionCanvas(depthViewer);
      
      
      getContentPane().setLayout(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy= 1;
      c.anchor = GridBagConstraints.CENTER;
      add(selectionPanel,c);      
   
      selectionPanel.requestFocus();
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy= 0;
      c.anchor = GridBagConstraints.FIRST_LINE_START;
      c.fill = GridBagConstraints.HORIZONTAL;
      add(controlPanel,c);
      
   }
   

   class ControlPanel extends JPanel{

      JButton playPauseButton = new PlayPauseButton();
      JButton clearButton = new ClaerButton();
      JCheckBox colorViewCheckBox = new ColorViewCheckBox();
      public ControlPanel(){
         
         add(playPauseButton);
         add(clearButton);
         add(colorViewCheckBox);
      }
      
      class ColorViewCheckBox extends JCheckBox  implements ItemListener {

         public ColorViewCheckBox(){
            super("Color",false);
            addItemListener(this);
         }
         @Override
         public void itemStateChanged(ItemEvent e) {
            KinectMeasureFrame.this.setColorViewEnabled(isSelected());
         }
      }
      
      class ClaerButton extends JButton implements ActionListener {
         public ClaerButton(){
            super("Clear");
            addActionListener(this);
         }

         @Override
         public void actionPerformed(ActionEvent e) {
            selectionPanel.selector.clear();
         }         
         
      }
      
      class PlayPauseButton extends JButton implements ActionListener {         
         int state;         
         public PlayPauseButton(){
            super("Pause");
            addActionListener(this);
         }

         @Override
         public void actionPerformed(ActionEvent e) {
            if (getText() == "Pause"){
               
               selectionPanel.viewer_.setPaused(true);               
               setText("Play");               
               
            }else{
               selectionPanel.viewer_.setPaused(false);            
               setText("Pause");
            }
         }
      }
      
   }
   
   

   
   
   SelectionCanvas selectionPanel = null;
   
   enum Mode {
      EMPTY,
      SELECTING,
      SELECTED
   }

   class SelectionCanvas extends JPanel{
      
      
      class LineSelector extends JComponent implements MouseListener, MouseMotionListener {
         
         
         Point from = null;
         Point to = null;
         Mode mode = Mode.EMPTY;
         JLabel lengthLabel = new JLabel();

         LineSelector(){
            setLayout(null);
            add(lengthLabel);
            lengthLabel.setOpaque(true);
            lengthLabel.setBackground(Color.white);
         }
         
         public void clear() {
            from = null;
            to = null;
            repaint();
            lengthLabel.setVisible(false);
         }

         @Override
         public void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(3.0f));
            g2d.setColor(Color.red);
            
            if (from != null){               
               Ellipse2D c = new Ellipse2D.Double(from.x-5,from.y-5,10,10);
               g2d.draw(c);               
            }

            if (to != null){               
               Ellipse2D c = new Ellipse2D.Double(to.x-5,to.y-5,10,10);
               g2d.draw(c);               
            }
            
            if (from != null && to != null){
               g2d.drawLine(from.x,from.y,to.x,to.y);             
            }
            
         }
         
         void updateLength(){
            float length = (float) (100f + Math.random()*10);
            lengthLabel.setText(""+length);
            lengthLabel.setSize(lengthLabel.getPreferredSize());
            
            Point o = new Point();
            int dx = to.x - from.x;
            int dy = to.y - from.y;
            double d = to.distance(from);
            double angle = Math.atan2(dy,dx);
            o.x = (int) (from.x + (d+50) * Math.cos(angle));
            o.y = (int) (from.y + (d+50) * Math.sin(angle));
            
            Dimension size = lengthLabel.getSize();
            lengthLabel.setLocation(o.x-size.width/2, o.y - size.height/2);
            lengthLabel.setVisible(true);
         }


         @Override
         public void mouseClicked(MouseEvent e) {
            if (mode == Mode.EMPTY){
               mode = Mode.SELECTING;
               from = e.getPoint();
               to = e.getPoint();
               setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            }else if (mode == Mode.SELECTING){
               mode = Mode.SELECTED;
               to = e.getPoint();               
            }else if (mode == Mode.SELECTED){
               mode = Mode.SELECTING;
               from = e.getPoint();
               to = e.getPoint();
               setCursor(Cursor.getDefaultCursor());
            }
            repaint();
         }


         @Override
         public void mouseEntered(MouseEvent arg0) {
            // TODO Auto-generated method stub
            
         }


         @Override
         public void mouseExited(MouseEvent arg0) {
            // TODO Auto-generated method stub
            
         }


         @Override
         public void mousePressed(MouseEvent arg0) {
            // TODO Auto-generated method stub
            
         }


         @Override
         public void mouseReleased(MouseEvent arg0) {
            // TODO Auto-generated method stub
            
         }

         @Override
         public void mouseDragged(MouseEvent arg0) {
            // TODO Auto-generated method stub
            
         }

         @Override
         public void mouseMoved(MouseEvent e) {
            if (mode == Mode.SELECTING){
               to = e.getPoint();
               updateLength();
               repaint();
            }
            
         }
         
      }
      

      
      LineSelector selector = new LineSelector();
      
      KinectViewer viewer_ = null;
      void setViewer(KinectViewer viewer){
         System.out.println("set viewer");
         if (viewer_ != null)
            remove(viewer_);         
         this.viewer_ = viewer;         
         add(viewer_);
         viewer_.setBounds(0,0,640,480);
         repaint();
      }
      
      public SelectionCanvas(KinectViewer viewer){
         
         setFocusable(true);
         
         setLayout(null);
         selector = new LineSelector();
         setPreferredSize(new Dimension(640,480));
         
         setViewer(viewer);
         add(selector, 0);
         selector.setBounds(0,0,640,480);
         selector.setSize(640,480);
         
         addKeyListener(new KeyAdapter(){

            @Override
            public void keyPressed(KeyEvent k) {
               if (k.getKeyCode() == KeyEvent.VK_F){
                  System.out.println("Freeze");
               }
            }
            
         });
         
         addMouseListener(selector);
         addMouseMotionListener(selector);
         
         
      }
      
      
      
   }

   public void setColorViewEnabled(boolean enabled) {

      if (enabled) {
         selectionPanel.setViewer(rgbViewer);      
      }else{
         selectionPanel.setViewer(depthViewer);
      }      
   }

}