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

/**
 * This object just holds a matrix of byte values.
 * 
 * @author Adam Kapelner
 */
public class ByteMatrix extends SimpleMatrix {
	private static final long serialVersionUID = -2263665631927185290L;

	/** the internal storage */
	private byte[][] M;

	public ByteMatrix(int width, int height){
		super(width, height);
		M=new byte[width][height];
	}

	/**
	 * Gets a value from the matrix (throws out of bounds exception)
	 * 
	 * @param i			the x-coordinate in which to get
	 * @param j			the y-coordinate in which to get
	 * @return			the value of that position in the matrix
	 */
	public byte get(int i,int j){
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
	
	/**
	 * Sets a value in the matrix (throws out of bounds exception)
	 * 
	 * @param i			the x-coordinate in which to set
	 * @param j			the y-coordinate in which to set
	 * @param val		the value in which to set
	 */
	public void set(int i,int j,byte val){
		M[i][j]=val;
	}
	

	public void set(Point t, byte alpha) {
		set(t.x, t.y, alpha);
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
	public ByteMatrix crop(int xo, int xf, int yo, int yf) {
		ByteMatrix cropped = new ByteMatrix(xf - xo, yf - yo);
		for (int i = xo; i < xf; i++){
			for (int j = yo; j < yf; j++){
				cropped.set(i - xo, j - yo, get(i, j));
			}		
		}
		return cropped;
	}

	/** If the value is greater than 255, it is set to 255, otherwise, pass the value along as is. */
	protected void setPixelInRaster(int i, int j, WritableRaster raster){
		int a=get(i,j);
		if (a > 255)
			raster.setSample(i,j,0,255);
		else
			raster.setSample(i,j,0,a);		
	}
}