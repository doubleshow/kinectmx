package org.sikuli.kinect;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.swing.JComponent;

public abstract class KinectViewer extends JComponent {

   byte[] buffer;
   
   protected int w = 640;
   protected int h = 480;
   protected BufferedImage image;
   boolean isUpdating = false;

   public KinectViewer() {
      super();
   }
   
   boolean isPaused = false;
   public boolean isPaused(){
      return isPaused;
   }
   public void setPaused(boolean paused){
      isPaused = paused;
   }

   private synchronized void doUpdate(ByteBuffer frame) {
      isUpdating = true;
      frame.rewind();
      frame.get(buffer);
      isUpdating = false;
   }

   protected void update(ByteBuffer frame) {      
      if (isPaused || isUpdating)
         return;
      else
         doUpdate(frame);
   }

   BufferedImage getImage(){
      return image;
   }

}