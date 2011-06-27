package org.sikuli.kinect;

import java.awt.Color;
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
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import Jama.Matrix;

class CalibratedDepthViewer extends DepthViewer {
   
   double rawR[][];
   double rawT[][];
   
   
   private RGBViewer rgbViewer;
   
   BufferedImage calibratedImage;
   
   Matrix R = null;     
   Matrix T = null;
   void setupMatrices(){
      
      rawR = new double[][]{
            {9.9981445712749240e-01f, 6.4684624053484830e-04f,-1.9251828713108998e-02f, 0},
            {-6.5459564770346747e-04f,9.9999970725334852e-01f, -3.9622942254266400e-04f, 0},
            {1.9251566777688248e-02f, 4.0875806828336519e-04f,9.9981458785789168e-01f, 0},
            {0,0,0,1}};
      
      rawT = new double[][]{
            {3.1157357150415746e-02f, -9.4281476188136612e-05f,-4.2779889235526058e-02f,1}};
         

      Matrix R_rgb = (new Matrix(rawR)).transpose();
      Matrix T_rgb = (new Matrix(rawT)).transpose();
      
      //double rawI[][] = {{1,0,0,0},{0,-1,0,0},{0,0,-1,0},{0,0,0,1}};
      double rawI[][] = {{-1,0,0,0},{0,1,0,0},{0,0,-1,0},{0,0,0,1}};
      Matrix I = new Matrix(rawI);
      Matrix Iinv = I.copy().inverse();
      Matrix Rinv = R_rgb.copy().inverse();
      R = Iinv.times(Rinv).times(I);         
      T = I.times(T_rgb);
   }
   
   
   
   CalibratedDepthViewer(File file) throws IOException{
      super(file);
      calibratedImage = getImage();
   }
   
   CalibratedDepthViewer(){
      
      calibratedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      
      setupMatrices();      
   }
   
   @Override
   protected void update(ByteBuffer frame){
      super.update(frame);
      
      ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
      byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

      
      for (int dy=0;dy<480;++dy){   
         for (int dx=0;dx<640;++dx){
            
            short depthValue = shortBuffer.get();
            //float distance = Kinect.depthToDistanceInCentimeters(depthValue); 
            
            Point3d w = getWorldLocation(dx,dy);
            
            // convert w to RGB camera's world coordinate
            double r[][] = R.getArray();
            double t[][] = T.getArray();
            
             w.x = r[0][0] * w.x + r[0][1] * w.y + r[0][2] * w.z + r[0][3] + t[0][0];
             w.y = r[1][0] * w.x + r[1][1] * w.y + r[1][2] * w.z + r[1][3] + t[1][0];
             w.z = r[2][0] * w.x + r[2][1] * w.y + r[2][2] * w.z + r[2][3] + t[2][0];

            // project to RGB camera's image coordinate
             Point q = rgbViewer.getImageLocationFromWorldLocation(w);

             int cx = q.x;
             int cy = q.y;
             // boundary clamp
             if (cx >= 0 && cx < 640 && cy >=0 && cy < 480){
                calibratedImage.setRGB(dx,dy,rgbViewer.getImage().getRGB(cx,cy));
                //calibratedImage.setRGB(dx,dy,c.getRGB());
             }else{
                calibratedImage.setRGB(dx,dy,Color.black.getRGB());
             }
         }
      }
      repaint();
   }
      
   

   @Override
   public void paintComponent(Graphics g){
      Graphics2D g2d = (Graphics2D) g;
      g2d.drawImage(calibratedImage,0,0,null);
   }



   public void setRGBViewer(RGBViewer rgbViewer) {
      this.rgbViewer = rgbViewer;
   }



   public RGBViewer getRgbViewer() {
      return rgbViewer;
   }

}


