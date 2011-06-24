package org.sikuli.kinect;

import org.openkinect.freenect.*;

//import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.LookupOp;
import java.awt.image.Raster;
import java.awt.image.RescaleOp;
import java.awt.image.SampleModel;
import java.awt.image.ShortLookupTable;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

//import org.hamcrest.collection.IsEmptyCollection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openkinect.freenect.util.Jdk14LogHandler;

import Jama.Matrix;

public class FreenectTest {
   static Context ctx;
   static Device dev;

   @BeforeClass
   public static void initKinect() {
      ctx = Freenect.createContext();
      ctx.setLogHandler(new Jdk14LogHandler());
      ctx.setLogLevel(LogLevel.SPEW);
      if (ctx.numDevices() > 0) {
         dev = ctx.openDevice(0);
      } else {
         System.err.println("WARNING: No kinects detected, hardware tests will be implicitly passed.");
      }
   }

   @AfterClass
   public static void shutdownKinect() {
      if (ctx != null)
         if (dev != null) {
            dev.close();
         }
      ctx.shutdown();
   }

   protected void moveAndWait(Device device, int degrees) throws InterruptedException {
      device.refreshTiltState();
      if (device.getTiltAngle() >= (degrees - 2) && device.getTiltAngle() <= (degrees + 2)) {
         return;
      }
      //assertThat(device.setTiltAngle(degrees), is(0));

      while (device.getTiltStatus() == TiltStatus.STOPPED) {
         device.refreshTiltState();
      }

      if (device.getTiltStatus() == TiltStatus.MOVING) {
         while (device.getTiltStatus() == TiltStatus.MOVING) {
            device.refreshTiltState();
         }
      }

      if (device.getTiltStatus() == TiltStatus.STOPPED) {
         while (device.getTiltAngle() < -32) {
            device.refreshTiltState();
         }
      }
   }

   @Test(timeout = 10000)
   public void testSetTiltAngle() throws InterruptedException {
      //assumeThat(dev, is(not(nullValue())));

      ctx.setLogLevel(LogLevel.SPEW);
      dev.refreshTiltState();

      moveAndWait(dev, 0);
      //assertThat(dev.getTiltAngle(), is(closeTo(0, 2)));

      moveAndWait(dev, 20);
      //assertThat(dev.getTiltAngle(), is(closeTo(20, 2)));

      moveAndWait(dev, -20);
      //assertThat(dev.getTiltAngle(), is(closeTo(-20, 2)));

      moveAndWait(dev, 0);
      //assertThat(dev.getTiltAngle(), is(closeTo(0, 2)));
   }

   @Test(timeout = 5000)
   public void testLogEvents() throws InterruptedException {
      //assumeThat(dev, is(not(nullValue())));

      ctx.setLogLevel(LogLevel.FLOOD);
      final List<String> messages = new ArrayList<String>();
      ctx.setLogHandler(new LogHandler() {
         @Override
         public void onMessage(Device dev, LogLevel level, String msg) {
            messages.add(msg);
         }
      });
      dev.startVideo(new VideoHandler() {
         @Override
         public void onFrameReceived(VideoFormat format, ByteBuffer frame, int timestamp) {
         }
      });
      Thread.sleep(500);
      dev.stopVideo();
      ctx.setLogLevel(LogLevel.SPEW);
      ctx.setLogHandler(new Jdk14LogHandler());
      //assertThat(messages, is(not(IsEmptyCollection.<String>empty()))); // wtf hamcrest, fix this!
   }

   @Test(timeout = 2000)
   public void testDepth() throws InterruptedException {
      //assumeThat(dev, is(not(nullValue())));

      final Object lock = new Object();
      final long start = System.nanoTime();
      dev.startDepth(new DepthHandler() {
         int frameCount = 0;

         @Override
         public void onFrameReceived(DepthFormat format, ByteBuffer frame, int timestamp) {
            frameCount++;
            if (frameCount == 30) {
               synchronized (lock) {
                  lock.notify();
                  System.out.format("Got %d depth frames in %4.2fs%n", frameCount,
                        (((double) System.nanoTime() - start) / 1000000));
               }
            }
         }
      });
      synchronized (lock) {
         lock.wait(2000);
      }
   }
   
   static float k1 = 0.1236f;
   static float k2 = 2842.5f;
   static float k3 = 1.1863f;
   static float k4 = 0.0370f;

   private float rawToCentimeters(int raw) {
      return (float) (100 * (k1 * Math.tan((1f*raw / k2) + k3) - k4));
   }
   
   float[] distancePixelsLookup = new float[2048];
   short[] depthPixelsLookupFarWhite = new short[2048];
   short[] depthPixelsLookupNearWhite = new short[2048];
   int nearClipping = 30;
   int farClipping = 200;
   
   private int map(int v, int s1, int s2, int d1, int d2, boolean clamp){   
      v = Math.min(s2,v);
      v = Math.max(s1,v);
      return (d2 - d1) * (v - s1) / (s2 - s1);
      //return 40*255;
   }
   
   private void calculateLookup(){
      for(int i = 0; i < 2048; i++){
         if(i > 1000) {
            distancePixelsLookup[i] = 0;
            depthPixelsLookupNearWhite[i] = 0;
            depthPixelsLookupFarWhite[i] = 0;
         } else {
            distancePixelsLookup[i] = rawToCentimeters(i);
            //System.out.println(distancePixelsLookup[i]);
            depthPixelsLookupFarWhite[i] = (short) (255*150 - map((int)distancePixelsLookup[i], nearClipping, farClipping, 0, 150*255, true));
            System.out.println("" + i + ":" + distancePixelsLookup[i]  + "->" + depthPixelsLookupFarWhite[i]);
            depthPixelsLookupNearWhite[i] = (short) (255 - depthPixelsLookupFarWhite[i]);
         }
      }
   }
   
   
   class CalibratedRGBImage extends JComponent {
      
      BufferedImage image;
      DepthViewer depthImage;
      RGBViewer rgbImage;
      
      public CalibratedRGBImage(DepthViewer depthImage, RGBViewer rgbImage){
         
         image = new BufferedImage(640,480, BufferedImage.TYPE_INT_RGB);
         //image.getData();
         
         this.depthImage = depthImage;
         this.rgbImage = rgbImage;

         
         setupMatrices();
      }
      
      double fx_d,fy_d,cx_d,cy_d;
      double fx_rgb,fy_rgb,cx_rgb,cy_rgb;
      
      double rawR[][];
      double rawT[][];
      
      void setCalibrationParametersHome(){
         fx_d = 1.0 / 4.8378065692480374e+02;
         fy_d = 1.0 / 4.8064891800790127e+02;
         cx_d = 3.5888505420667735e+02;
         cy_d = 2.4766889087469457e+02;

         fx_rgb = 4.6831326560522564e+02;
         fy_rgb = 4.6491638429833148e+02;
         cx_rgb = 3.4281546567751963e+02;
         cy_rgb = 2.5035843764290559e+02;
         
         rawR = new double[][]{
               {9.9981445712749240e-01f, 6.4684624053484830e-04f,-1.9251828713108998e-02f, 0},
               {-6.5459564770346747e-04f,9.9999970725334852e-01f, -3.9622942254266400e-04f, 0},
               {1.9251566777688248e-02f, 4.0875806828336519e-04f,9.9981458785789168e-01f, 0},
               {0,0,0,1}};
         
         rawT = new double[][]{
               {3.1157357150415746e-02f, -9.4281476188136612e-05f,-4.2779889235526058e-02f,1}};

      }
      
      void setCalibrationParametersOffice(){
      
         fx_d = 1.0 / 5.6915669033277982e+02;
         fy_d = 1.0 / 5.7156824112896481e+02;
         cx_d = 3.1426974666578479e+02;
         cy_d = 2.5447067453656601e+02;

         fx_rgb = 5.2504024783808677e+02;
         fy_rgb = 5.2710595873970829e+02;
         cx_rgb = 3.1828143764732971e+02;
         cy_rgb = 2.5617267273960107e+02;
         
         rawR = new double[][]
           {{9.9984030034806526e-01, 1.9232056628715403e-03, -1.7767247391449924e-02, 0},
            {-2.2409877935182822e-03,9.9983756997140416e-01, -1.7883278429492212e-02, 0},
            {1.7729968234601757e-02, 1.7920238660680227e-02, 9.9968220613990344e-01,0},
            {0,0,0,1}};
         
         rawT = new double[][]
//           {{4.0456789050979568e-02, 4.0590972985856157e-03,-1.2116280862500817e-02,0}};
//             {{3.2365405861595475e-02, 3.2469093690957775e-03,-9.6929557976244262e-03,0}}; 
            {{2.4273980618708606e-02, 2.4353380786142249e-03,-7.2698236703982394e-03,0}}; 
      }
      
//      double rawT[][] = {{4.0456789050979568e-02, 4.0590972985856157e-03,
//         -1.2116280862500817e-02,0}};
      
//       with pattern size = 0.02
//      double rawT[][] = {{3.2365405861595475e-02, 3.2469093690957775e-03,
//         -9.6929557976244262e-03,0}};
      
//      // with pattern size = 0.0225
//      double rawT[][] = {{ 3.6411643748641725e-02, 3.6528891652559698e-03,
//         -1.0904282790689758e-02,1}};
      
      boolean isUpdating = false;
      void update(){
         if (isUpdating)
            return;
         doUpdate();
      }
      
      Matrix R = null;     
      Matrix T = null;
      void setupMatrices(){
         
         //setCalibrationParametersOffice();
         setCalibrationParametersHome();
         
         //Matrix R_rgb = (new Matrix(rawR)).transpose();
         Matrix R_rgb = (new Matrix(rawR)).transpose();
         Matrix T_rgb = (new Matrix(rawT)).transpose();
         
         double rawI[][] = {{1,0,0,0},{0,-1,0,0},{0,0,-1,0},{0,0,0,1}};
         Matrix I = new Matrix(rawI);
         Matrix Iinv = I.copy().inverse();
         Matrix Rinv = R_rgb.copy().inverse();
         Rinv.print(10,4);
         R_rgb.print(10,4);
         R = Iinv.times(Rinv).times(I);         
         T = I.times(T_rgb);
         R.print(10,4);
         T.print(10,4);

         //R = R_rgb;
//         R = 
//         T = T_rgb;
         
//         Matrix I = new Matrix(rawI);
//         Matrix Iinv = I.copy().inverse();
//         Matrix Rinv = R_rgb.copy().inverse();
//         R = Iinv.copy().times(Rinv).copy().times(I).copy(); 
//         T = I.copy().times(T_rgb).copy();
      }
      
      synchronized void doUpdate(){
         System.out.println("calibrated image updating");
         isUpdating = true;
         
         ByteBuffer buffer = ByteBuffer.wrap(depthImage.buffer);

      // you may or may not need to do this
         buffer.order(ByteOrder.LITTLE_ENDIAN);

         ShortBuffer shorts = buffer.asShortBuffer();
         shorts.rewind();

         if (!depthImage.ready)
            return;
         
         // depth image coordinate (dx,dy)
         for (int dy=0;dy<480;++dy){   
            for (int dx=0;dx<640;++dx){
               
               short depth = shorts.get();
               float distance = distancePixelsLookup[depth]; 
               //System.out.println(distance);
               
               if (distance == 0){
                  
                  
                  image.setRGB(dx,dy,0);
                  
                  
                  
                  
                  continue;
               }else{
                  
//                  if (distance > 100)
//                     image.setRGB(dx,dy,Color.red.getRGB());
//                  else
//                     image.setRGB(dx,dy,Color.blue.getRGB());
                  
               }
               
//               if (true)
//                  continue;
               
               // world coordinate (x,y,z)
               double wz = distance * 0.01; // centimeter to meter               
               double wx = (dx - cx_d) * wz * fx_d;
               double wy = (dy - cy_d) * wz * fy_d;
               
                              
//               Matrix p = new Matrix(4,1);
//               p.set(0,0,wx);
//               p.set(1,0,wy);
//               p.set(2,0,wz);               
//               Matrix p1 = R.times(p).plus(T);
//               wx = p1.get(0,0);
//               wy = p1.get(1,0);
//               wz = p1.get(2,0);
               
               
               double r[][] = R.getArray();
               double t[][] = T.getArray();
               //R = R.transpose();
//               wx = r[0][0] * wx + r[0][1] * wy + r[0][2] * wz + r[0][3] + t[0][0];
//               wy = r[1][0] * wx + r[1][1] * wy + r[1][2] * wz + r[1][3] + t[1][0];
//               wz = r[2][0] * wx + r[2][1] * wy + r[2][2] * wz + r[2][3] + t[2][0];

               wx = R.get(0,0) * wx + R.get(0,1) * wy + R.get(0,2) * wz + t[0][0];
               wy = R.get(1,0) * wx + R.get(1,1) * wy + R.get(1,2) * wz + t[1][0];
               wz = R.get(2,0) * wx + R.get(2,1) * wy + R.get(2,2) * wz + t[2][0];

//             wx = r[0][0] * wx + r[0][1] * wy + r[0][2] * wz + r[0][3] - t[0][0];
//             wy = r[1][0] * wx + r[1][1] * wy + r[1][2] * wz + r[1][3] - t[1][0];
//             wz = r[2][0] * wx + r[2][1] * wy + r[2][2] * wz + r[2][3] - t[2][0];
               
//               if (dx == 320 && dy > 100 && dy < 150){
//                  
//                  System.out.println(""+wx+"  "+wy+"  "+wz);
//               }

               
               
               // color image coordinate
               double invZ = 1.0 / wz;
               int cx = (int) ((int)(wx * fx_rgb * invZ) + cx_rgb);
               int cy = (int) ((int)(wy * fy_rgb * invZ) + cy_rgb);

                if (dx == 320 && dy > 100 && dy < 150){
                
              //  System.out.println(""+cx+"  "+cy);
             }
               
               
               // boundary clamp
               if (cx >= 0 && cx < 640 && cy >=0 && cy < 480){
                  
                  cx = 640-cx;
                  //double ps[] = new double[3];
                  byte[] bs = rgbImage.buffer;
                  int pos = (cy*640*3) + (cx*3);
                  
                  if (dx == 320 && dy > 100 && dy < 150){
                     int rr = bs[pos] & 0xff;
                     int g = bs[pos+1] & 0xff;
                     int b = bs[pos+2] & 0xff; 
                     System.out.println("("+cx+","+cy+") -> " + rr+"  "+g+" "+b);
                     
//                     rgbImage.getImage().getData().getPixel(cx,cy,ps);
//                     System.out.println(""+ps[0]+"  "+ps[1]+" "+ps[2]);
                  }  


                  image.setRGB(dx,dy,rgbImage.getImage().getRGB(cx,cy));
                  
                  
                  
               }
               else
                  image.setRGB(dx,dy,Color.green.getRGB());
               
            }
         }
         isUpdating = false;         
      }
    
      
      @Override
      public void paintComponent(Graphics g){
         super.paintComponent(g);
         Graphics2D g2d = (Graphics2D) g;
         g2d.drawImage(image,0,0,null);
      }

   }
   
   
   
   @Test
   public void testVideoDepthSync() throws InterruptedException {
      final Object lock = new Object();
      final long start = System.nanoTime();
      
      calculateLookup();
      
      final JFrame previewFrame = new JFrame("Image Preview");
      previewFrame.setSize(640, 480);
      previewFrame.setVisible(true);
      previewFrame.setAlwaysOnTop(true);
      
      final DepthViewer depthImage = new DepthViewer();
      depthImage.setBounds(new Rectangle(0,0,640,480));
      final RGBViewer rgbImage = new RGBViewer();
      rgbImage.setBounds(new Rectangle(640,0,640,480));
      
      
      final CalibratedRGBImage calibratedRGBImage = new CalibratedRGBImage(depthImage, rgbImage);
      //calibratedRGBImage.setBounds(new Rectangle(0,480,640,480));
      calibratedRGBImage.setBounds(new Rectangle(0,0,640,480));

      previewFrame.getContentPane().setLayout(null);//new BorderLayout());
      //previewFrame.getContentPane().add(rgbImage);
      //previewFrame.getContentPane().add(depthImage);
      previewFrame.getContentPane().add(calibratedRGBImage);
      
      JButton btn = new JButton("some button");
      btn.setBounds(10,10,100,100);
      previewFrame.getContentPane().add(btn,0);
      
      previewFrame.addKeyListener(new KeyListener(){

         @Override
         public void keyPressed(KeyEvent key) {
            if (key.getKeyCode() == KeyEvent.VK_ESCAPE){
               synchronized (lock) {
                  lock.notify();
               }

            }            
         }

         @Override
         public void keyReleased(KeyEvent arg0) {
            // TODO Auto-generated method stub
            
         }

         @Override
         public void keyTyped(KeyEvent arg0) {
            // TODO Auto-generated method stub
            
         }
         
      });
      
      
      dev.startDepth(new DepthHandler() {
         @Override
         public void onFrameReceived(DepthFormat format, ByteBuffer frame, int timestamp) {
            //System.out.format("Depth format is width=%d, height=%d, frameSize=%d, intValue=%d, timestamp=%d\n", format.getWidth(), format.getHeight(), format.getFrameSize(), format.intValue(), timestamp);
            depthImage.update(frame);
            depthImage.repaint();
         }
      });
      dev.startVideo(new VideoHandler() {
         @Override
         public void onFrameReceived(VideoFormat format, ByteBuffer frame, int timestamp) {
            //System.out.format("Video format is width=%d, height=%d, frameSize=%d, intValue=%d, timestamp=%d\n", format.getWidth(), format.getHeight(), format.getFrameSize(), format.intValue(), timestamp);
            rgbImage.update(frame);
            rgbImage.repaint();
            
            calibratedRGBImage.update();
            calibratedRGBImage.repaint();
         }
      });
      
      
      
      synchronized (lock) {
         //lock.wait(2000);
         lock.wait();
      }
      
   
   }
   
   
   @Test
   public void testDepthDisplay() throws InterruptedException {
      final Object lock = new Object();
      final long start = System.nanoTime();
      
      calculateLookup();
      
      
      final JFrame previewFrame = new JFrame("Image Preview");
      previewFrame.setSize(640, 480);
      previewFrame.setVisible(true);
      previewFrame.setAlwaysOnTop(true);

      int w = 640;
      int h = 480;
      ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
      int[] nBits = {16};
      final ColorModel cm = new ComponentColorModel(cs, nBits, 
            false, false,
            Transparency.OPAQUE, 
            DataBuffer.TYPE_SHORT);      
      int[] bandOffsets = {0};
      final SampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_SHORT, w,h,2,w*2, bandOffsets);

      dev.startDepth(new DepthHandler() {
         int frameCount = 0;
         byte[] buffer = new byte[640*480*2];
         DataBufferByte db = new DataBufferByte(buffer, 640*480*2);

         @Override
         public void onFrameReceived(DepthFormat format, ByteBuffer frame, int timestamp) {
            System.out.format("Depth format is width=%d, height=%d, frameSize=%d, intValue=%d\n", format.getWidth(), format.getHeight(), format.getFrameSize(), format.intValue());
            
            if (!(frameCount % 20 == 0)){
               frameCount += 1;
               //return;
            }
//
            synchronized (previewFrame){
               
               frame.rewind();
               frame.get(buffer);               
               
               WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0,0));
               BufferedImage image = new BufferedImage(cm,r,false,null);
               

//               float[] scales = {5f};
//               float[] offsets = {1f};
//               RescaleOp rop = new RescaleOp(scales, offsets, null);

               
               ShortLookupTable blt = new ShortLookupTable(0, depthPixelsLookupFarWhite);
               LookupOp op = new LookupOp(blt, null);
               
               Graphics2D g = (Graphics2D) previewFrame.getContentPane().getGraphics();
               //g.drawImage(image, rop, 0, 0);
               g.drawImage(image, op, 0, 0);
               
               //previewFrame.getContentPane().getGraphics().drawImage(image, 0, 0, null);
               
               
               
            }
            
            frameCount++;
            if (frameCount == 60) {
               synchronized (lock) {
                  lock.notify();
                  System.out.format("Got %d video frames in %4.2fs%n", frameCount,
                        (((double) System.nanoTime() - start) / 1000000));

               }
            }
         }
      });
      
      synchronized (lock) {
         lock.wait(10000);
      }
   }
   
   @Test
   public void testVideoDisplay() throws InterruptedException {
      final Object lock = new Object();
      final long start = System.nanoTime();
      
      
      final JFrame previewFrame = new JFrame("Image Preview");
      previewFrame.setSize(640, 480);
      previewFrame.setVisible(true);
      previewFrame.setAlwaysOnTop(true);

      int w = 640;
      int h = 480;
      ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
      int[] nBits = {8, 8, 8};
      final ColorModel cm = new ComponentColorModel(cs, nBits, 
            false, false,
            Transparency.TRANSLUCENT, 
            DataBuffer.TYPE_BYTE);      
      final SampleModel sm = cm.createCompatibleSampleModel(w, h);      

      dev.startVideo(new VideoHandler() {
         int frameCount = 0;
         byte[] buffer = new byte[640*480*3];
         DataBufferByte db = new DataBufferByte(buffer, 640*480*3);               

         @Override
         public void onFrameReceived(VideoFormat format, ByteBuffer frame, int timestamp) {
            //System.out.format("Format is width=%d, height=%d, frameSize=%d, intValue=%d\n", format.getWidth(), format.getHeight(), format.getFrameSize(), format.intValue());
            
            if (!(frameCount % 2 == 0)){
               frameCount += 1;
               //return;
            }

            synchronized (previewFrame){
               
               frame.rewind();
               frame.get(db.getData());               
               WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0,0));
               BufferedImage image = new BufferedImage(cm,r,false,null);
               
//               BufferedImage resizedRgbImage = new BufferedImage(320, 240,
//                     BufferedImage.TYPE_INT_RGB);
//                     Graphics2D g = resizedRgbImage.createGraphics();
//                     g.drawImage(image, 0, 0, 320, 240, null);
//                     g.dispose();
//                     //rgbImg = resizedRgbImage;
//               
               //BufferedImage bm = new BufferedImage(cm,r,false,null);

//               ByteBuffer byteBuffer = frame;
//               BufferedImage bufferedImage = bm;
//               for(int i=0; i<bufferedImage.getHeight(); i++) { 
//                  for(int j=0; j<bufferedImage.getWidth(); j++) { 
//                     int argb  = bufferedImage.getRGB( j, i ); 
//                     byte alpha = (byte) (argb >> 24); 
//                     byte red   = (byte) (argb >> 16); 
//                     byte green = (byte) (argb >> 8); 
//                     byte blue  = (byte) (argb); 
//
//                     byteBuffer.put(red); 
//                     byteBuffer.put(green); 
//                     byteBuffer.put(blue); 
//                     byteBuffer.put(alpha); 
//                  } 
//               }  


               
               
               
               
               
               
            
//               int count = 0;
//               System.out.println(frame.hasRemaining());
//               frame.rewind();
//               while(frame.hasRemaining())                     
//               {
//                  int r = frame.get() & 0xff;
//                  int g = frame.get() & 0xff;
//                  int b = frame.get() & 0xff;
//                  //System.out.format("(%d,%d,%d)\n", r,g,b);
//                  rasterArray[count] = (new Color(r,g,b)).getRGB();
//                  count += 1;
//               }
//               System.out.println("redrawing");
//
//               BufferedImage image = new BufferedImage(640, 480,
//                     BufferedImage.TYPE_3BYTE_BGR);
//               image.setRGB(0, 0, 640, 480, rasterArray, 0, 640);
               //previewFrame.getContentPane().getGraphics().clearRect(0,0,640,480);
               //previewFrame.getContentPane().getGraphics().drawImage(image, 0, 0, null);
                 previewFrame.getContentPane().getGraphics().drawImage(image, 0, 0, null);
               
//               try {
//                  ImageIO.write(image, "png", new File("/Users/tomyeh/desktop/frame" + frameCount + ".png"));
//               } catch (IOException e) {
//                  // TODO Auto-generated catch block
//                  e.printStackTrace();
//               }

            }
            
            frameCount++;
            if (frameCount == 60) {
               synchronized (lock) {
                  lock.notify();
                  System.out.format("Got %d video frames in %4.2fs%n", frameCount,
                        (((double) System.nanoTime() - start) / 1000000));
                  
                  
//                  int w = 640;
//                  int h = 480;
//                  
//                  ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
//                  int[] nBits = {8, 8, 8, 8};
//                  ColorModel cm = new ComponentColorModel(cs, nBits, 
//                                                true, false,
//                                                Transparency.TRANSLUCENT, 
//                                                DataBuffer.TYPE_BYTE);
//
//                  SampleModel sm = cm.createCompatibleSampleModel(w, h);
//                  DataBufferByte db = new DataBufferByte(w*h*4); //4 channels buffer
//                  WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0,0));
//                  BufferedImage bm = new BufferedImage(cm,r,false,null);
//                  
//                  ByteBuffer byteBuffer = frame;
//                  BufferedImage bufferedImage = bm;
//                  for(int i=0; i<bufferedImage.getHeight(); i++) { 
//                     for(int j=0; j<bufferedImage.getWidth(); j++) { 
//                         int argb  = bufferedImage.getRGB( j, i ); 
//                         byte alpha = (byte) (argb >> 24); 
//                         byte red   = (byte) (argb >> 16); 
//                         byte green = (byte) (argb >> 8); 
//                         byte blue  = (byte) (argb); 
//
//                         byteBuffer.put(red); 
//                         byteBuffer.put(green); 
//                         byteBuffer.put(blue); 
//                         byteBuffer.put(alpha); 
//                     } 
//                 }  

                  
//                  Raster raster =
//                     Raster.createInterleavedRaster( DataBuffer.TYPE_BYTE,
//                                 imgWidth,
//                                 imgHeight,
//                                 4,
//                                 null );
//                        ComponentColorModel colorModel =
//                     new ComponentColorModel( ColorSpace.getInstance(ColorSpace.CS_sRGB),
//                               new int[] {8,8,8,8},
//                               true,
//                               false,
//                               ComponentColorModel.TRANSLUCENT,
//                               DataBuffer.TYPE_BYTE );
                  
                  

//                  try {
//                     Thread.sleep(5000);
//                  } catch (InterruptedException e) {
//                     // TODO Auto-generated catch block
//                     e.printStackTrace();
//                  }
                  
               }
            }
         }
      });
      
      synchronized (lock) {
         lock.wait(10000);
      }
      //dev.stopVideo();
      
   }

   @Test(timeout = 2000)
   public void testVideo() throws InterruptedException {
      //assumeThat(dev, is(not(nullValue())));

      final Object lock = new Object();
      final long start = System.nanoTime();
      dev.startVideo(new VideoHandler() {
         int frameCount = 0;

         @Override
         public void onFrameReceived(VideoFormat format, ByteBuffer frame, int timestamp) {
            frameCount++;
            if (frameCount == 30) {
               synchronized (lock) {
                  lock.notify();
                  System.out.format("Got %d video frames in %4.2fs%n", frameCount,
                        (((double) System.nanoTime() - start) / 1000000));
               }
            }
         }
      });
      synchronized (lock) {
         lock.wait(2000);
      }
   }
}
