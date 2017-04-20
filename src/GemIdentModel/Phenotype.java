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

package GemIdentModel;

import java.awt.Color;
import java.io.Serializable;

/**
 * Houses the specific data necessary to build a phenotype (minimal). Built on top of
 * {@link TrainSuperclass TrainSuperclass}. It implements {@link java.io.Serializable Serializable} 
 * to easily dump its data to XML format when saving a <b>GemIdent</b> project
 * 
 * @author Adam Kapelner
 */
public class Phenotype extends TrainSuperclass implements Serializable{

	private static final long serialVersionUID = 4391353231882685179L;

	/** the name of the NON phenotype */
	transient public static final String NON_NAME="NON";
	
	/** should this phenotype's centroids be found during classification? */
	private boolean findCentroids;
	/** should this phenotype be looked for at all during classification? */
	private boolean findPixels;
	
	/** basically the default constructor */
	public Phenotype(){
		super();
		dirty=false;
		findCentroids=true;
		findPixels=true;
	}
	/**
	 * Gets the display color with a specified opacity
	 * 
	 * @param alpha		the opacity
	 * @return			the display color with the specified opacity
	 */
	public Color getDisplayColorWithAlpha(int alpha) {
		return new Color(
			displayColor.getRed(),
			displayColor.getGreen(),
			displayColor.getBlue(),
			alpha
		);
	}
	/** is this the NON phenotype? */
	public boolean isNON(){
		return name.equals(NON_NAME);
	}
	/** is this name the NON name? */
	public static boolean isNONNAME(String name){
		return name.equals(NON_NAME);
	}
	public boolean isFindCentroids() {
		return findCentroids;
	}
	public void setFindCentroids(boolean findCentroids) {
		this.findCentroids = findCentroids;
	}
	public boolean isFindPixels() {
		return findPixels;
	}
	public void setFindPixels(boolean findPixels) {
		this.findPixels = findPixels;
	}
}