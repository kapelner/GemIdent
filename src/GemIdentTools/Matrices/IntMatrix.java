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

import java.awt.Point;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * This object just holds a matrix of int values.
 * 
 * @author Adam Kapelner
 */
public class IntMatrix extends SimpleMatrix implements Serializable {	
	private static final long serialVersionUID = 259956746272783682L;
	
	/** the internal storage */
	protected int[][] M;
	
	public IntMatrix(){} //serializable needs to be happy too
	
	/**
	 * Creates a new IntMatrix with specified dimensions
	 * 
	 * @param width		the width of the matrix
	 * @param height	the height of the matrix
	 */
	public IntMatrix(int width, int height){
		super(width, height);
		M=new int[width][height];
	}
	
	/**
	 * Creates a new IntMatrix with specified dimensions and 
	 * initializes all values
	 * 
	 * @param width		the width of the matrix
	 * @param height	the height of the matrix
	 * @param value		the intial value of every scalar in the matrix
	 */
	public IntMatrix(int width,int height,int value){
		super(width, height);
		M=new int[width][height];
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				M[i][j]=value;
	}
	/**
	 * Sets a value in the matrix (throws out of bounds exception)
	 * 
	 * @param i			the x-coordinate in which to set
	 * @param j			the y-coordinate in which to set
	 * @param val		the value in which to set
	 */
	public void set(int i,int j,int val){
		M[i][j]=val;
	}
	/**
	 * Sets a value in the matrix (throws out of bounds exception)
	 * 
	 * @param t			the coordinate in which to set
	 * @param val		the value in which to set
	 */
	public void set(Point t,int val){
		M[t.x][t.y]=val;
	}
	/**
	 * Gets a value from the matrix (throws out of bounds exception)
	 * 
	 * @param i			the x-coordinate in which to get
	 * @param j			the y-coordinate in which to get
	 * @return			the value of that position in the matrix
	 */
	public int get(int i,int j){
		return M[i][j];
	}
	/**
	 * Gets a value from the matrix (throws out of bounds exception)
	 * 
	 * @param t			the coordinate in which to get
	 * @return			the value of that position in the matrix
	 */
	public int get(Point t){
		return M[t.x][t.y];
	}

	/** If the value is greater than 255, it is set to 255, otherwise, pass the value along as is. */
	protected void setPixelInRaster(int i, int j, WritableRaster raster){
		int a=get(i,j);
		if (a > 255)
			raster.setSample(i,j,0,255);
		else
			raster.setSample(i,j,0,a);		
	}
	
	/** print out for debugging purposes */
	public void print(){
		String out="";
		for (int j=0;j<height;j++){
			for (int i=0;i<width;i++){
				out+=get(i,j)+" ";
			}
			out+="\n";
		}
		System.out.println(out);
	}
	
	/**
	 * Get the number of pixels lower than a certain value and set those pixels to zero if so
	 * 
	 * @param T			the threshold value
	 * @return			the number of pixels lower than T in this matrix
	 */
	public long ThresholdLowerThan(int T){
		short zero=0;
		int N=0;
		for (int j=0;j<height;j++)
			for (int i=0;i<width;i++)
				if (get(i,j) > T){
					set(i,j,zero);	
					N++;
				}
		return Area()-N;
	}
	
	public int[][] getM() {
		return M;
	}
	public void setM(int[][] M) {
		this.M = M;
	}

	/**
	 * Linearize the matrix
	 * 
	 * @return	all the values in the matrix as a list
	 */
	public ArrayList<Integer> values(){
		ArrayList<Integer> all = new ArrayList<Integer>((int)Area());
		for (int i=0;i<width;i++)
			for (int j=0;j<height;j++)
				all.add(get(i, j));
		return all;
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
	public IntMatrix crop(int xo, int xf, int yo, int yf) {
		IntMatrix cropped = new IntMatrix(xf - xo, yf - yo);
		for (int i = xo; i < xf; i++){
			for (int j = yo; j < yf; j++){
				cropped.set(i - xo, j - yo, get(i, j));
			}		
		}
		return cropped;
	}
}