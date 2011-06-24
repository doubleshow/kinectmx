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
   
   boolean ready = false;
   
   BufferedImage image;
   
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
      
      //g2d.drawImage(image,0,0,null);
   }
   
}