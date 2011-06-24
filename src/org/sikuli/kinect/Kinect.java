package org.sikuli.kinect;

import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

import org.openkinect.freenect.Context;
import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.DepthHandler;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.Freenect;
import org.openkinect.freenect.LogLevel;
import org.openkinect.freenect.VideoFormat;
import org.openkinect.freenect.VideoHandler;
import org.openkinect.freenect.util.Jdk14LogHandler;

public class Kinect {
   
   static Context ctx;
   static Device dev;

   static void start(){
      ctx = Freenect.createContext();
      ctx.setLogHandler(new Jdk14LogHandler());
      ctx.setLogLevel(LogLevel.SPEW);      
      if (ctx.numDevices() > 0) {
         dev = ctx.openDevice(0);
      } else {
         System.err.println("WARNING: No kinects detected, hardware tests will be implicitly passed.");
      }   
   }
   
   static void shutdown(){
      if (ctx != null)
         if (dev != null) {
            dev.close();
         }
      ctx.shutdown();
   }

   // TODO: don't restart video if another rgb viewer is created, instead, add it to the 
   // existing handler
   static RGBViewer createRGBViewer(){
      final RGBViewer rgbViewer = new RGBViewer();
      dev.startVideo(new VideoHandler() {
         @Override
         public void onFrameReceived(VideoFormat format, ByteBuffer frame, int timestamp) {
            rgbViewer.update(frame);
            rgbViewer.repaint();
         }
      });
      return rgbViewer;
   }

   static DepthViewer createDepthViewer(){
      final DepthViewer viewer = new DepthViewer();
      dev.startDepth(new DepthHandler() {
         @Override
         public void onFrameReceived(DepthFormat format, ByteBuffer frame, int timestamp) {
            viewer.update(frame);
            viewer.repaint();
         }
      });
      return viewer;
   }

   
   static float k1 = 0.1236f;
   static float k2 = 2842.5f;
   static float k3 = 1.1863f;
   static float k4 = 0.0370f;
   
   static {
      calculateLookup();
   }

   static public float depthToDistanceInCentimeters(int depthValue){
      return distancePixelsLookup[depthValue];
   }
   
   static private float rawToCentimeters(int raw) {
      return (float) (100 * (k1 * Math.tan((1f*raw / k2) + k3) - k4));
   }

   static private float[] distancePixelsLookup;

   static private void calculateLookup(){
      distancePixelsLookup = new float[2048];
      for(int i = 0; i < 2048; i++){
         if(i > 1000) {
            distancePixelsLookup[i] = 0;
         } else {
            distancePixelsLookup[i] = rawToCentimeters(i);
         }
      }
   }

   
}
