package org.sikuli.kinect;

import org.junit.Test;

public class KinectMeasureTest {

   
   @Test
   public void testCreation() throws InterruptedException{
      
      Kinect.start();
      
      KinectMeasureFrame kf = new KinectMeasureFrame();
      kf.setVisible(true);

      Object lock = new Object();
      synchronized (lock) {
         lock.wait(20000);
      }
      
      Kinect.shutdown();
      
   }
}
