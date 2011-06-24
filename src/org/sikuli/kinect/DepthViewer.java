package org.sikuli.kinect;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.LookupOp;
import java.awt.image.RescaleOp;
import java.awt.image.SampleModel;
import java.awt.image.ShortLookupTable;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

class DepthViewer extends KinectViewer {
   
   class F_ {
//      double fx = 4.8378065692480374e+02;
//      double fy = 4.8064891800790127e+02;
//      double cx = 3.5888505420667735e+02;
//      double cy = 2.4766889087469457e+02;
      
      double fx = 5.6915669033277982e+02;
      double fy = 5.7156824112896481e+02;
      double cx = 3.1426974666578479e+02;
      double cy = 2.5447067453656601e+02;

   };
   F_ F = new F_();
   
   
   boolean ready = false;
   
   BufferedImage image;
   
   int getDepth(int x, int y){
      int i = x*2 + y*640*2;
      return buffer[i] + buffer[i+1]*256;
   }
   
   public DepthViewer(File imageFile) throws IOException{
      image = ImageIO.read(imageFile);  
   }
   
   public DepthViewer(){
      buffer = new byte[w*h*2];      
      ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
      int[] nBits = {16};
      final ColorModel cm = new ComponentColorModel(cs, nBits, 
            false, false,
            Transparency.OPAQUE, 
            DataBuffer.TYPE_USHORT);      
      int[] bandOffsets = {0};
      final SampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_USHORT, w,h,2,w*2, bandOffsets);
      
      DataBufferByte db = new DataBufferByte(buffer, w*h*2);
      WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0,0));
      image = new BufferedImage(cm,r,false,null);
   }
      
   @Override
   public void paintComponent(Graphics g){
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g;
      //ShortLookupTable blt = new ShortLookupTable(0, this.freenectTest.depthPixelsLookupFarWhite);
      //LookupOp op = new LookupOp(blt, null);
      //
      RescaleOp op = new RescaleOp(200f,0f,null);
      g2d.drawImage(image,op,0,0);
      
//      g2d.drawImage(image,0,0,null);
   }
   
   public Point3d getWorldLocation(int x, int y){
      
      double wz = getDistanceTo(x,y)*0.01;
      //double wz = distance * 0.01; // centimeter to meter               
      double wx = (x - F.cx) * wz / F.fx;
      double wy = (y - F.cy) * wz / F.fy;      
      return new Point3d(wx,wy,wz);
   }

   public float getDistanceTo(int x, int y) {
      int depthValue = getDepth(x,y);
      return Kinect.depthToDistanceInCentimeters(depthValue);
   }
   
}