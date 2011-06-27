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
            {9.9984030034806526e-01, 1.9232056628715403e-03, -1.7767247391449924e-02, 0},
               {-2.2409877935182822e-03,9.9983756997140416e-01, -1.7883278429492212e-02, 0},
               {1.7729968234601757e-02, 1.7920238660680227e-02, 9.9968220613990344e-01,0},
               {0,0,0,1}};
            
      rawT = new double[][]{
//            {3.1157357150415746e-02f, -9.4281476188136612e-05f,-4.2779889235526058e-02f,1}};
  //          {4.0456789050979568e-02, 4.0590972985856157e-03,-1.2116280862500817e-02,0}};
//              {3.6411643748641725e-02, 3.6528891652559698e-03,-1.0904282790689758e-02,1}};
//       {3.2365405861595475e-02, 3.2469093690957775e-03,-9.6929557976244262e-03,0}}; 
            {2.4273980618708606e-02, 2.4353380786142249e-03,-7.2698236703982394e-03,0}}; 
         

      
      
      Matrix R_rgb = (new Matrix(rawR)).transpose();
      Matrix T_rgb = (new Matrix(rawT)).transpose();
      
      double rawI[][] = {{-1,0,0,0},{0,1,0,0},{0,0,-1,0},{0,0,0,1}};
      Matrix I = new Matrix(rawI);
      Matrix Iinv = I.inverse();
      Matrix Rinv = R_rgb.inverse();
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
      if (isPaused()){
         return;
      }
      
      ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
      byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

      
      for (int dy=0;dy<480;++dy){   
         for (int dx=0;dx<640;++dx){
            
            //short depthValue = shortBuffer.get();
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


