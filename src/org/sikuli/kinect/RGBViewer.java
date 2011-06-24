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

class RGBViewer extends KinectViewer {

   
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

   BufferedImage getImage(){
      return image;
   }

}