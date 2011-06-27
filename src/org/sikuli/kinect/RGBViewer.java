package org.sikuli.kinect;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.sikuli.kinect.DepthViewer.F_;

class RGBViewer extends KinectViewer {

   class F_ {

      double fx;
      double fy;
      double cx;
      double cy;

      F_(){
         fx = 5.2504024783808677e+02;
         fy = 5.2710595873970829e+02;
         cx = 3.1828143764732971e+02;
         cy = 2.5617267273960107e+02;
      }


   };
   F_ F = new F_();
   
   public RGBViewer(File imageFile) throws IOException{
      image = ImageIO.read(imageFile);
   }
   
   public RGBViewer(){
      buffer = new byte[w*h*3];
      
      ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
      int[] nBits = {8, 8, 8};
      final ColorModel cm = new ComponentColorModel(cs, nBits, 
            false, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);      
      final SampleModel sm = cm.createCompatibleSampleModel(w, h);      

      DataBufferByte db = new DataBufferByte(buffer, w*h*3);               
      WritableRaster r = WritableRaster.createWritableRaster(sm, db, new Point(0,0));
      image = new BufferedImage(cm,r,false,null);
   }
   
   @Override
   public void paintComponent(Graphics g){
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g;
      g2d.drawImage(image,0,0,null);
   }
   
   Point getImageLocationFromWorldLocation(Point3d w){
      double invZ = 1.0 / w.z;
      int x = (int) (w.x * F.fx * invZ + F.cx);
      int y = (int) (w.y * F.fy * invZ + F.cy);
      return new Point(x,y);
   }

   BufferedImage getImage(){
      return image;
   }

}