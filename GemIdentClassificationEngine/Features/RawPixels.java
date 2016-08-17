package GemIdentClassificationEngine.Features;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import GemIdentImageSets.ImageSetInterface;

public class RawPixels extends DatumFeatureSet {

	@Override
	public void InitializeForRun(ImageSetInterface imageSetInterface) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void InitializeDataForImage(String filename) {
		// TODO Auto-generated method stub
		RGB_Reader RGB_from_img = new RGB_Reader(filename);
		RGB_Store RGB_vals [][] = RGB_from_img.RGB_matrix;
		
	}

	@Override
	public void BuildFeaturesIntoRecord(Point t, double[] record, int p_0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void UpdateFeatureTypes(ArrayList<FeatureType> feature_types, int p_0) {
		//don't care
	}

	@Override
	public void UpdateFeatureNames(ArrayList<String> feature_names, int p_0) {
		//don't care
		
	}

	@Override
	public void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0) {
		//don't care
	}
	/*
	 * Gets image and creates on matrix of each of the RGB_values in the image
	 */
	
	public static class RGB_Reader {
		   BufferedImage image;
		   int width;
		   int height;
		   public RGB_Store [][] RGB_matrix;
		   public RGB_Reader(String filename) {
		      try {
		         File input = new File(filename);
		         image = ImageIO.read(input);
		         width = image.getWidth();
		         height = image.getHeight();
		         RGB_matrix = new RGB_Store[height][width];
		         
		         int count = 0;
		         
		         for(int i=0; i<height; i++){
		         
		            for(int j=0; j<width; j++){
		            
		               count++;
		               Color c = new Color(image.getRGB(i,j));
		               RGB_matrix[i][j] = new RGB_Store(c.getRed(),c.getGreen(),c.getBlue());
		            }
		         }
		         
		      } catch (Exception e) {}
		   }

	}
	
	public static class RGB_Store{
		public int Red;
		public int Green;
		public int Blue;
		public RGB_Store(int r, int g, int b){
			Red = r;
			Green = g;
			Blue = b;
		}
	}
}
