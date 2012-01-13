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

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;

/**
 * Provides the infrastructure to house training points in one image.
 * It implements {@link java.io.Serializable Serializable} to easily dump its data to XML format 
 * when saving a <b>GemIdent</b> project
 * 
 * @author Adam Kapelner
 */
public class TrainingImageData implements Serializable{
	
	private static final long serialVersionUID = 1205339007491217013L;
	
	/** **Deprecated do not use** */
	private int blissNum;
	/** the filename of this image */
	private String filename;
	/** the list of the training point locations */
	private ArrayList<Point> points;

	/** keeps serializable happy */
	public TrainingImageData(){}
	/** basically a defauly constructor */
	public TrainingImageData(String filename){
		this.filename=filename;
		points=new ArrayList<Point>();
	}
	/**
	 * Adds a training point to this training image
	 * @param t		the point's location
	 */
	public void addPoint(Point t){
		points.add(t);
	}
	/**
	 * Deletes a training point to this training image
	 * @param t		the point's location
	 */
	public void deletePoint(Point t){
		points.remove(t);
	}
	/**
	 * The number of training points in this image
	 * @return		the number of points
	 */
	public int getNumPoints(){
		return points.size();
	}
	/** Deprecated do not use */
	public int getBlissNum() {
		return blissNum;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	/** Deprecated do not use */
	public void setBlissNum(int num) {
		this.blissNum = num;
	}
	public ArrayList<Point> getPoints() {
		return points;
	}
	public void setPoints(ArrayList<Point> points) {
		this.points = points;
	}
	/**
	 * Actually returns the image itself as a {@link GemIdentImageSets.DataImage DataImage}
	 * @return		the actual image
	 */
	public DataImage getImage(){
		return ImageAndScoresBank.getOrAddDataImage(filename);
	}
}