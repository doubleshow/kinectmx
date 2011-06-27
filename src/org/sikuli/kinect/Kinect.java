package org.sikuli.kinect;

import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.openkinect.freenect.Context;
import org.openkinect.freenect.DepthFormat;
import org.openkinect.freenect.DepthHandler;
import org.openkinect.freenect.Device;
import org.openkinect.freenect.Freenect;
import org.openkinect.freenect.LogLevel;
import org.openkinect.freenect.TiltStatus;
import org.openkinect.freenect.VideoFormat;
import org.openkinect.freenect.VideoHandler;
import org.openkinect.freenect.util.Jdk14LogHandler;

public class Kinect {
   
   static Context ctx;
   static Device dev;

   static boolean isFake = false;
   
   static void start(){
      ctx = Freenect.createContext();
      ctx.setLogHandler(new Jdk14LogHandler());
      ctx.setLogLevel(LogLevel.FATAL);//LogLevel.INFO);
      //ctx.setLogLevel(LogLevel.SPEW);      
      
      if (ctx.numDevices() > 0) {
         dev = ctx.openDevice(0);
      } else {
         System.err.println("WARNING: No kinects detected, hardware tests will be implicitly passed.");
         isFake = true;
      }   
   }
   
   static void shutdown(){
      if (ctx != null)
         if (dev != null) {
            dev.close();
         }
      ctx.shutdown();
   }
   
   
   static void tiltUp(int amount_in_degrees){
      dev.refreshTiltState();      
      setTiltAngle((int)dev.getTiltAngle() + amount_in_degrees);
   }
   
   static void tiltDown(int amount_in_degrees){
      dev.refreshTiltState();
      setTiltAngle((int)dev.getTiltAngle() - amount_in_degrees);
   }
   
   
   static void setTiltAngle(int degrees){
      dev.refreshTiltState();
      if (dev.getTiltAngle() >= (degrees - 2) && dev.getTiltAngle() <= (degrees + 2)) {
         return;
      }
      
      dev.setTiltAngle(degrees);

//      while (dev.getTiltStatus() == TiltStatus.STOPPED) {
//         dev.refreshTiltState();
//      }
//
//      if (dev.getTiltStatus() == TiltStatus.MOVING) {
//         while (dev.getTiltStatus() == TiltStatus.MOVING) {
//            dev.refreshTiltState();
//         }
//      }
//
//      if (dev.getTiltStatus() == TiltStatus.STOPPED) {
//         while (dev.getTiltAngle() < -32) {
//            dev.refreshTiltState();
//         }
//      }
      System.out.println("Tilt complete");

   }

   // TODO: don't restart video if another rgb viewer is created, instead, add it to the 
   // existing handler
   static RGBViewer createRGBViewer(){
      
      if (isFake){
         try {
            return new RGBViewer(new File("color.png"));
         } catch (IOException e) {
            e.printStackTrace();
            return null;
         }
      }
      
      
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

   static ArrayList<DepthViewer> depthViewers = new ArrayList<DepthViewer>();
   
   static CalibratedDepthViewer createCalibratedDepthViewer(){
      
      if (isFake){
         try {
            return new CalibratedDepthViewer(new File("depth.png"));
         } catch (IOException e) {
            e.printStackTrace();
            return null;
         }
      }
      
      CalibratedDepthViewer viewer = new CalibratedDepthViewer();
      if (depthViewers.size() == 0){     
         dev.startDepth(new DepthHandler() {
            @Override
            public void onFrameReceived(DepthFormat format, ByteBuffer frame, int timestamp) {
               for (DepthViewer viewer : depthViewers){
                  viewer.update(frame);
                  viewer.repaint();
               }
            }
         });
      }
      
      depthViewers.add(viewer);
      
      return viewer;
   
   }

   static DepthViewer createDepthViewer(){
      
      if (isFake){
         try {
            return new DepthViewer(new File("depth.png"));
         } catch (IOException e) {
            e.printStackTrace();
            return null;
         }
      }

      
      DepthViewer viewer = new DepthViewer();
      if (depthViewers.size() == 0){     
         dev.startDepth(new DepthHandler() {
            @Override
            public void onFrameReceived(DepthFormat format, ByteBuffer frame, int timestamp) {
               for (DepthViewer viewer : depthViewers){
                  viewer.update(frame);
                  viewer.repaint();
               }
            }
         });
      }
      
      depthViewers.add(viewer);
      
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
         if (i > 1000) {
            distancePixelsLookup[i] = 0;
         } else {
            distancePixelsLookup[i] = rawToCentimeters(i);
         }
      }
   }

   
}
