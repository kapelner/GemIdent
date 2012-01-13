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

/**
 * This object just holds a matrix of strings
 * 
 * @author Adam Kapelner
 */
public class StringMatrix extends SimpleMatrix implements Serializable {
	private static final long serialVersionUID = 6696923044227623372L;
	
	/** the internal storage */
	protected String[][] M;
	/** the width of the matrix */
	protected int width;
	/** the height of the matrix */
	protected int height;

	/**
	 * Creates a new StringMatrix with specified dimensions
	 * 
	 * @param width		the width of the matrix
	 * @param height	the height of the matrix
	 */
	public StringMatrix(int width, int height){
		super(width, height);
		M = new String[width][height];
	}
	
	/**
	 * Creates a new StringMatrix with specified dimensions and 
	 * initializes all values
	 * 
	 * @param width		the width of the matrix
	 * @param height	the height of the matrix
	 * @param value		the intial value of every scalar in the matrix
	 */
	public StringMatrix(int width, int height, String value){
		super(width, height);
		M=new String[width][height];
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
	public void set(int i,int j,String val){
		M[i][j]=val;
	}
	
	/**
	 * Sets a value in the matrix (throws out of bounds exception)
	 * 
	 * @param t			the coordinate in which to set
	 * @param val		the value in which to set
	 */
	public void set(Point t,String val){
		M[t.x][t.y]=val;
	}
	
	/**
	 * Gets a value from the matrix (throws out of bounds exception)
	 * 
	 * @param i			the x-coordinate in which to get
	 * @param j			the y-coordinate in which to get
	 * @return			the value of that position in the matrix
	 */
	public String get(int i,int j){
		return M[i][j];
	}
	
	/**
	 * Gets a value from the matrix (throws out of bounds exception)
	 * 
	 * @param t			the coordinate in which to get
	 * @return			the value of that position in the matrix
	 */
	public String get(Point t){
		return M[t.x][t.y];
	}

	protected void setPixelInRaster(int i, int j, WritableRaster raster) {
		//do nothing - String Matrices will never have to color an image
	}

}