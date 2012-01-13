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
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import GemIdentImageSets.DataImage;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;

/**
 * Provides the infrastructure to house training points across multiple images
 * for both Stains and Phenotypes (it's abstract because it can only be one or the other,
 * never generic). It implements {@link java.io.Serializable Serializable} to easily dump its data to XML format 
 * when saving a <b>GemIdent</b> project
 * 
 * @author Adam Kapelner
 */
public abstract class TrainSuperclass implements Serializable{

	private static final long serialVersionUID = -448833690310917971L;
	/** the colors of the pixels selected */
//	transient protected ArrayList<Color> pixels;
	
	/** maps an image filename to its training data */
	protected HashMap<String,TrainingImageData> trainingImagesMap;
	/** the display name of this stain or phenotype */
	protected String name;
	/** the display color of the points for this stain or phenotype */
	protected Color displayColor;
	/** the minimum radius included when generating data for this stain or phenotype */
	protected int rmin;
	/** the maximum radius included when generating data for a phenotype OR the display shadow for a Stain */
	protected int rmax;
	/** whether or not a change was made to this stain or phenotype */
	protected boolean dirty;
	
	/** Initializes a new stain or phenotype and sets defaults */
	public TrainSuperclass(){
		trainingImagesMap=new HashMap<String,TrainingImageData>();
		rmin=1; //default
		rmax=9; //default
		displayColor=Color.BLACK; //default
//		linked=false; //default
		setDirty(false); //default	
	}
	/**
	 * Adds a point to the training set. If the image trained upon
	 * is not in the set, it will automatically add it. It then sets
	 * the entire project dirty, so the user could save.
	 * 
	 * @param filename 		the image in which to add the training point
	 * @param t				the point's coordinates
	 */
	public void addPointToTrainingSet(String filename,Point t){
		TrainingImageData imageData=getTrainingImage(filename);
		if (imageData == null){
			imageData=new TrainingImageData(filename);
			trainingImagesMap.put(filename,imageData);
		}
		imageData.addPoint(t);
		setDirty(true);
		Run.it.GUIsetDirty(true);
	}
	/**
	 * Deletes a point from the training set. If this is the last
	 * point in an image, delete that image from the training set.
	 * 
	 * @param filename		the image in which to delete the training point
	 * @param t				the point's coordinates
	 */
	public void deletePointFromTrainingSet(String filename,Point t){
		TrainingImageData imageData=getTrainingImage(filename);
		if (imageData != null){
			imageData.deletePoint(t);
			if (imageData.getNumPoints() == 0)
				trainingImagesMap.remove(filename);
		}
		setDirty(true);
		if (getTotalPoints() == 0)
			setDirty(false);
	}
	/** gets all the training images' data */
	public Collection<TrainingImageData> getTrainingImages() {
		return trainingImagesMap.values();
	}
	/** gets a specific training image's data */
	public TrainingImageData getTrainingImage(String filename) {
		return trainingImagesMap.get(filename);
	}
	/** removes all training image's data */
	public void RemoveTrainingImage(String filename){
		trainingImagesMap.remove(filename);		
	}
	/** gets the total number of training points in this stain or phenotype */
	public int getTotalPoints(){
		int N=0;
		for (TrainingImageData i:trainingImagesMap.values())
			N+=i.getPoints().size();
		return N;
	}
	/** of these training points, get the associated colors */
	public ArrayList<Color> getColors(){
		ArrayList<Color> pixels = new ArrayList<Color>();
		for (TrainingImageData i:trainingImagesMap.values()){
			DataImage image=i.getImage();
			for (Point to:i.getPoints())
				for (Point t:Solids.GetPointsInSolidUsingCenter(rmin,to))
					try {
						pixels.add(image.getColorAt(t));
					} catch (Exception e){}
		}
		return pixels;
	}
	/** is this image included in the set? */
	public boolean hasImage(String filename){
		return trainingImagesMap.containsKey(filename);
	}
	/** get all points in an image. If the image doesn't exist, it returns null */
	public ArrayList<Point> getPointsInImage(String filename){
		TrainingImageData imagedata=trainingImagesMap.get(filename);
		if (imagedata == null)
			return null;
		else
			return imagedata.getPoints();
	}
	/** get the display color as a 24bit rgb integer */
	public int getDisplayRGB(){
		return displayColor.getRGB();
	}
	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	public Color getDisplayColor() {
		return displayColor;
	}
	public void setDisplayColor(Color displayColor) {
		this.displayColor = displayColor;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getRmax() {
		return rmax;
	}
	public void setRmax(int rmax) {
		this.rmax = rmax;
	}
	public int getRmin() {
		return rmin;
	}
	public void setRmin(int rmin) {
		this.rmin = rmin;
	}
	public HashMap<String,TrainingImageData> getTrainingImagesMap() {
		return trainingImagesMap;
	}
	public void setTrainingImagesMap(HashMap<String,TrainingImageData> trainingImagesMap) {
		this.trainingImagesMap = trainingImagesMap;
	}
}