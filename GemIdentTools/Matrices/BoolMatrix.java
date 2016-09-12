/*
    GemIdent v1.1b
    Interactive Image Segmentation Software via Supervised Statistical Learning
    http://gemident.com
    
    Copyright (C) 2009 Professor Susan Holmes & Adam Kapelner, Stanford University

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details:
    
    http://www.gnu.org/licenses/gpl-2.0.txt

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package GemIdentTools.Matrices;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;

import GemIdentImageSets.NonGlobalImageSet;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;

/**
 * Houses a logical matrix stored internally as a {@link BitMatrix Bitmatrix}
 * (as to save memory). BoolMatrix also contains the functions necessary to do most of the 
 * operations in the Analysis Panel
 *  
 * @author Adam Kapelner
 */
public class BoolMatrix extends SimpleMatrix {
	private static final long serialVersionUID = -2484685929706846204L;

	/** the 24-bit RGB integer that represents the color white */
	public static final int WhiteRGB=(Color.WHITE).getRGB();
	/** the 24-bit RGB integer that represents the color black */
	public static final int BlackRGB=(Color.BLACK).getRGB();
	
	/** round to the nearest thousandth */
	private static final NumberFormat seconds_format = NumberFormat.getInstance();
	private static final int num_decimals_in_seconds = 3;
	static {
		seconds_format.setMaximumFractionDigits(num_decimals_in_seconds);
	}
	/** the data for this BoolMatrix as a {@link GemIdentTools.Matrices.BitMatrix CERN Bitmatrix} */
	private BitMatrix M;
	
	/** basically a default constructor specifying the dimensions */
	public BoolMatrix(int width,int height){
		super(width, height);
		M=new BitMatrix(width, height);
	}
	
	/** another flavor */
	public BoolMatrix(Dimension d) {
		this(d.width, d.height);
	}
	
	/**
	 * Creates a BoolMatrix from a binary BufferedImage.
	 * The dimensions will be the same as that of the image.
	 * 
	 * @param image				the binary image
	 * @throws Exception		if the image is not binary
	 */
	public BoolMatrix(BufferedImage image) throws Exception{	
		if (image.getType() != BufferedImage.TYPE_BYTE_BINARY)
			throw new Exception("need binary image");
		width=image.getWidth();
		height=image.getHeight();
		M=new BitMatrix(width,height);
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				if (image.getRGB(i,j) == WhiteRGB)
					M.put(i,j,true);
		
		
		
	}
	/**
	 * Creates a BoolMatrix where a certain number of locations are initially true
	 * 
	 * @param points		the list of locations that will be initially marked true
	 * @param width			the width of the BoolMatrix
	 * @param height		the height of the BoolMatrix
	 */
	public BoolMatrix(ArrayList<Point> points,int width,int height){
		super(width, height);
		M=new BitMatrix(width,height);
		for (Point t:points)
			set(t,true);
	}
	/**
	 * Set a location in the matrix
	 * 
	 * @param i			the x coordinate
	 * @param j			the y coordinate
	 * @param val		the value to be set
	 */
	public void set(int i,int j,boolean val){
		try {
			M.putQuick(i,j,val);
		} catch (Exception e){}
	}
	/**
	 * Set a location in the matrix
	 * @param t			the location to be set
	 * @param val		the value to be set
	 */
	public void set(Point t,boolean val){
		set(t.x,t.y,val);
	}
	/**
	 * Get a value from the matrix
	 * @param i			the x coordinate of the lookup
	 * @param j			the y coordinate of the lookup
	 * @return			the value of that location in the matrix
	 */
	public boolean get(int i,int j){
		try {
			if (M.getQuick(i,j))
				return true;
			else
				return false;
		} catch (Exception e){
			return false;
		}
	}
	/**
	 * Get a value from the matrix
	 * @param t			the location of the lookup
	 * @return			the value of that location in the matrix
	 */
	public boolean get(Point t){
		return get(t.x,t.y);
	}
	
	/**
	 * Set many points true at once
	 * @param points		the list of locations in the matrix to set true
	 */
	public void addPoints(ArrayList<Point> points){
		for (Point t:points)
			set(t,true);
	}
	/**
	 * For all locations that are true in the matrix, print them to a file
	 * 
	 * @param out 			the PrintWriter that writes to the file
	 * @param filename		the image that this BoolMatrix represents (used in order to get the global location)
	 */
	public void PrintPointsToFile(PrintWriter out,String filename){
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				if (get(i,j)){
					if (Run.it.imageset instanceof NonGlobalImageSet){
						out.print(filename+","+i+","+j+"\n");
					}
					else {
						Point g = Run.it.imageset.getTrueLocation(filename,new Point(i,j),true);
						out.print(filename+","+i+","+j+","+g.x+","+g.y+"\n");
					}
				}
	}
	/**
	 * Creates a binary BufferedImage (reflecting the data in the BoolMatrix)
	 * that can be displayed or written
	 * @return		the binary image
	 */
	public BufferedImage getBinaryImage(){
		BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_BYTE_BINARY);
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				if (M.get(i,j))
					image.setRGB(i,j,WhiteRGB);
		return image;
	}
	/**
	 * Illustrates this BoolMatrix as a mask over another image
	 * 
	 * @param image		the image to display the mask over
	 * @param color		the color of the mask
	 */
	public void IllustrateAsMask(BufferedImage image,Color color){
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				if (get(i,j))
					try {image.setRGB(i,j,color.getRGB());}catch(Exception e){}
	}
	/**
	 * Counts the number of points that appear within the mask in the actual matrix
	 * 
	 * @param i			the x coordinate to center the mask
	 * @param j			the y coordinate to center the mask
	 * @param mask		the mask
	 * @return			the total count
	 */
	public int GetMaskAnd(int i,int j,ArrayList<Point> mask){
		int N=0;
		for (Point t:mask) 
			if (get(t.x+i,t.y+j)) 
				N++;
		return N;
	}
	/**
	 * Sets all points within a mask to false
	 * @param i			the x coordinate to center the mask
	 * @param j			the y coordinate to center the mask
	 * @param mask		the mask
	 */
	public void DeleteMaskAnd(int i,int j,ArrayList<Point> mask){
		for (Point t:mask) 
			set(t.x+i,t.y+j,false);		
	}
	/**
	 * Erodes this matrix using N4 erosion and returns it.
	 * 
	 * N4 erosion is where every pixel is tested and if the pixel above, below,
	 * left, and right are not all true, it is itself set false
	 * 
	 * @return		the eroded image
	 */
	public BoolMatrix Erode(){
		BoolMatrix E=clone();
		for (int i=0;i<width;i++){
			for (int j=0;j<height;j++){
				if (get(i,j)){
					int N=0; //num neighbors in 4N metric
					if (get(i-1,j)) N++;
					if (get(i+1,j)) N++;
					if (get(i,j-1)) N++;
					if (get(i,j+1)) N++;
					if (N < 4)
						E.set(i,j,false);
				}
			}
		}
		return E;
	}
//	public BoolMatrix ErodeConvex(int m){
//		BoolMatrix E=clone();
//		for (int i=0;i<width;i++){
//			for (int j=0;j<height;j++){
//				if (get(i,j)){
//					int N=0; //num neighbors in 8N metric
//					if (get(i-1,j)) N++;
//					if (get(i+1,j)) N++;
//					if (get(i,j-1)) N++;
//					if (get(i,j+1)) N++;
//					if (get(i-1,j-1)) N++;
//					if (get(i+1,j+1)) N++;
//					if (get(i+1,j-1)) N++;
//					if (get(i-1,j+1)) N++;
//					if (N == 8-m)
//						E.set(i,j,false);
//				}
//			}
//		}
//		return E;
//	}
	/**
	 * Dilates this matrix using N4 dilation and returns it.
	 * 
	 * N4 dilation is where every pixel is tested and if the pixel is itself true, the pixel
	 * above, below, left, and right are all set true
	 *  
	 * @return		the dilated image
	 */
	public BoolMatrix Dilate(){
		BoolMatrix D=clone();
		for (int i=0;i<width;i++){
			for (int j=0;j<height;j++){
				if (get(i,j)){
					D.set(i,j,true);
					D.set(i-1,j,true);
					D.set(i+1,j,true);
					D.set(i,j-1,true);
					D.set(i,j+1,true);
				}
			}
		}
		return D;
	}
	/** the number of positives in the matrix */
	public long NumberPoints(){
		long N=0;
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				if (get(i,j))
					N++;
		return N;
	}

	/** clones the matrix */
	public BoolMatrix clone(){
		BoolMatrix B=new BoolMatrix(width,height);
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				B.set(i,j,get(i,j));
		return B;
	}
	/** returns the rth positive point's location starting at (0,0) and moving horizontally */
	public Point getRthPt(int r) {
		int R=0;
		for (int i=0;i<width;i++){
			for (int j=0;j<height;j++){
				if (get(i,j))
					R++;
				if (r == R)
					return new Point(i,j);
			}
		}
		return null;
	}
	/**
	 * Return locations of positives inside a circle
	 * @param i		the x coordinate of the center of the circle
	 * @param j		the y coordinate of the center of the circle
	 * @param R		the radius of the circle
	 * @return		the positive points in the matrix located within the circle 
	 */
	public ArrayList<Point> FindPointsInGivenRadius(int i,int j,int R){
		ArrayList<Point> points=new ArrayList<Point>();
		ArrayList<Point> solid=Solids.GetPointsInSolidUsingCenter(R, new Point(i,j));
		for (Point t:solid) 
			if (get(t))
				points.add(t);
		return points;
	}
	/** returns all positive points as a list of locations */
	public ArrayList<Point> getPositivePoints(){
		ArrayList<Point> points=new ArrayList<Point>();
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				if (M.get(i,j))
					points.add(new Point(i,j));
		return points;
	}
	/**
	 * For each pt in centroids, draw a star in the current boolmatrix
	 * 
	 * @param stars		the matrix of places to draw stars
	 */
	public void DrawStarsAt(BoolMatrix stars){
		for (int i=0;i<stars.width;i++)
			for (int j=0;j<stars.height;j++)
				if (stars.get(i, j))
					drawstar(i, j);	
	}
	
	/**
	 * Draw a dot at a given point
	 * 
	 * @param i		point's x coordinate
	 * @param j		point's y coordinate
	 */
	private void drawstar(int i, int j) {
		set(i, j, false);
//		set(i + 1, j, false);
//		set(i - 1, j, false);
//		set(i, j + 1, false);
//		set(i, j - 1, false);
	}
	/**
	 * Combines two BoolMatrix objects together. For every positive point in A or B,
	 * the resulting image will be positive at that point (non-exclusive OR)
	 * 
	 * @param A		the first matrix
	 * @param B		the second matrix
	 * @return		the combined matrix
	 */
	public static BoolMatrix Or(BoolMatrix A,BoolMatrix B){
		BoolMatrix R=new BoolMatrix(A.width,A.height);
		for (int i=0;i<A.width;i++)
			for (int j=0;j<A.height;j++)
				if (A.get(i,j) || B.get(i,j))
					R.set(i,j,true);			
		return R;
	}
	/**
	 * Counts all the positive points in A, and counts those that have a positive point 
	 * in B not more than radius away
	 * 
	 * @param A			matrix A
	 * @param B			matrix B
	 * @param radius	distance away
	 * @return			a Point object where the x value is the number of points that
	 * 					have a neighbor in matrix B, and the y value is the total number
	 * 					of positive points in matrix A
	 */
	public static Point NumWithinRadius(BoolMatrix A,BoolMatrix B,int radius){
		ArrayList<Point> pointsA=A.getPositivePoints();
		ArrayList<Point> pointsB=B.getPositivePoints();
		
		int num=0;
		int radsq=radius*radius;
		for (Point a:pointsA){
			for (Point b:pointsB){
				double distSq=a.distanceSq(b);
				if (distSq <= radsq){
					num++;
					break;
				}
			}
		}
		return new Point(num,pointsA.size());
	}
	/**
	 * The same as {@link #NumWithinRadius(BoolMatrix, BoolMatrix, int) NumWithinRadius} 
	 * except that it only accepts points in between two radii (a donut shape)
	 * 
	 * @param A			matrix A
	 * @param B			matrix B
	 * @param radiusA	the lower bound of the radius
	 * @param radiusB	the upper bound of the radius
	 * @return			a Point object where the x value is the number of points that
	 * 					has a neighbor in matrix B between radiusA and radiusB away,
	 * 					and the y value is the total number of positive points in matrix A		
	 */
	public static Point NumWithinRing(BoolMatrix A,BoolMatrix B,int radiusA,int radiusB){
		ArrayList<Point> pointsA=A.getPositivePoints();
		ArrayList<Point> pointsB=B.getPositivePoints();
		
		int num=0;
		int radAsq=radiusA*radiusA;
		int radBsq=radiusB*radiusB;
		for (Point a:pointsA){
			for (Point b:pointsB){
				double distSq=a.distanceSq(b);
				if (distSq >= radAsq && distSq <= radBsq){
					num++;
					break;
				}
			}
		}
		return new Point(num,pointsA.size());
	}
	/**
	 * Create a mapping from all positive points in matrix A, 
	 * to the distance to the nearest positive in matrix B
	 * @param A			matrix A
	 * @param B			matrix B
	 * @return			map from the point to the distance
	 */
	public static HashMap<Point,Double> DistanceAwayDistr(BoolMatrix A,BoolMatrix B){
		HashMap<Point,Double> distr=new HashMap<Point,Double>();
		ArrayList<Point> pointsB=B.getPositivePoints();
		for (int i=0;i<A.width;i++){
			for (int j=0;j<A.height;j++){
				if (A.get(i,j)){
					Point to=new Point(i,j);
					double minDistSq=Double.MAX_VALUE;
					for (Point t:pointsB){
						double distSq=to.distanceSq(t);
						if (distSq < minDistSq)
							minDistSq=distSq;
					}
					distr.put(to,Math.sqrt(minDistSq));
				}
			}
		}
		return distr;
	}

	/**
	 * Scales this matrix by the inverse of the scale value (eg
	 * if the scale value is 10, the width and the height will be
	 * shrunk by 10 so the total number of pixels will be 1% of the 
	 * original)
	 * 
	 * @param scale		the inverse scale value
	 * @return			the scaled matrix
	 */
	public BoolMatrix Scale(double scale){
		BoolMatrix S=new BoolMatrix((int)Math.round(width/scale),(int)Math.round(height/scale));
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				if (get(i,j))
					S.set((int)Math.floor(i/scale),(int)Math.floor(j/scale),true);
		return S;
	}
	/** construct a BoolMatrix from a CERN BitMatrix */
	public BoolMatrix(int width,int height,BitMatrix M){
		super(width, height);
		this.M = M;
	}	

	/**
	 * Saves a BoolMatrix to the hard drive in the proprietary "bit"
	 * format. The bit format is a bit pattern dump of the width, the height,
	 * and then the longs that compose the BitMatrix
	 * 
	 * @param B				the BoolMatrix to be copied
	 * @param filename		the filename to save the BoolMatrix to
	 */
	public static void Save(BoolMatrix B,String filename){
		DataOutputStream out=null;

		try {
			out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(Run.it.imageset.getFilenameWithHomePath(filename))));
		} catch (IOException e) {
			System.out.println("cannot save: "+filename);
		}
		try {
			out.writeInt(B.width);
			out.writeInt(B.height);
			for (long bit : B.M.getBits())
				out.writeLong(bit);
			out.close();
		} catch (IOException e) {e.printStackTrace();}		
	}
	/**
	 * Loads a BoolMatrix from the hard drive in the proprietary "bit"
	 * format. The bit format is a bit pattern dump of the width, the height,
	 * and then the longs that compose the BitMatrix
	 *
	 * @param filename		the filename of the saved BoolMatrix
	 * @return				the loaded BoolMatrix
	 */
	public static BoolMatrix Load(String filename){
		
		DataInputStream in=null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(Run.it.imageset.getFilenameWithHomePath(filename))));
		} catch (FileNotFoundException e) {e.printStackTrace();} 
		try {
			int width=in.readInt();
			int height=in.readInt();
			int nLongs =(width*height/64);
			long[] bits=new long[nLongs+1];
			for (int i=0;i<bits.length;i++)
				bits[i]=in.readLong();
			in.close();
			return new BoolMatrix(width,height,new BitMatrix(width,height,bits));
		}
		catch (IOException except){
			except.printStackTrace();
			System.out.println("file error opening "+filename);
		}
		return null;
	}

	/**
	 * Crop the matrix
	 * 
	 * @param xo	beginning x coordinate
	 * @param xf	ending x coordinate
	 * @param yo	beginning y coordinate
	 * @param yf	ending y coordinate
	 * @return		the cropped matrix
	 */	
	public BoolMatrix crop(int xo, int xf, int yo, int yf) {
		BoolMatrix cropped = new BoolMatrix(xf - xo, yf - yo);
		for (int i = xo; i < xf; i++){
			for (int j = yo; j < yf; j++){
				cropped.set(i - xo, j - yo, get(i, j));
			}		
		}
		return cropped;
	}
	
	/** If the pixel is 1, mark it white, if 0, mark it black */
	protected void setPixelInRaster(int i, int j, WritableRaster raster) {
		if (get(i, j))
			raster.setSample(i, j, 0, 255);
		else
			raster.setSample(i, j, 0, 0);
	}	

	/**
	 * Given another matrix, blank out all points in this mastrix within a given radius
	 * 
	 * @param if_matrix		the positive points to blank out
	 * @param num_pixels	the radius away
	 */	
	public void ExcludeAllPositivesWithinRadius(BoolMatrix if_matrix, int num_pixels) {
		for (int i = 0; i < if_matrix.getWidth(); i++){
			for (int j = 0; j < if_matrix.getHeight(); j++){
				if (if_matrix.get(i, j)){
					for (Point t : Solids.GetPointsInSolidUsingCenter(num_pixels, new Point(i, j))){
						set(t, false);
					}
				}
			}			
		}
	}

}