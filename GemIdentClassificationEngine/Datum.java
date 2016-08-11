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

package GemIdentClassificationEngine;

import java.awt.Point;

import GemIdentClassificationEngine.Features.DatumFeatureSet;

/**
 * This class is the abstract class responsible for pulling data from images
 * and constructing an array of integers (a "record") that can be processed
 * by a machine learning classifier. The {@link #datumSetup datum setup} object
 * holds common information about each Datum. There are many ways to pull data
 * from images and construct features. Extend this class to write your own such method
 * and put it in the package: GemIdentClassificationEngine.<your new datum's name>
 * 
 * @author Adam Kapelner
 */
public class Datum implements Cloneable {

	/** the coordinate in the that a Datum is being generated for in the {@link GemIdentImageSets.SuperImage SuperImage} of interest */
	protected Point to;
	/** the data record as an double array (holds the attribute values as entries, the last entry is the class - the response value) */ 
	protected double[] record;
	/** the object that contains common information for each Datum by image */	
	private DatumSetupForImage datumSetupForImage;
	
	/**
	 * constructs a Datum from pixel location. See the superclass's constructor
	 * for information
	 * 
	 * @param datumSetup	the object that contains common information for each Datum 	
	 * @param to			the pixel location in the image this Datum is constructed for
	 */
	public Datum(DatumSetupForImage datumSetupForImage, Point to){
		this.datumSetupForImage = datumSetupForImage;
		this.to = to;
		//instantiate the record add 1 for the class, y, so the record looks like the vector: X_i. = [x_1, x_2, ..., x_M, y_i]
		record = new double[datumSetupForImage.numFeatures() + 1];
		BuildRecord();
	}

	/**
	 * From the image data, builds the {@link #record record} consisting of an int[] for
	 * later classification
	 */
	public void BuildRecord(){
		int p_0 = 0;
		for (DatumFeatureSet feature_set : datumSetupForImage.getFeatureSets()){
			//build the features into the array
			feature_set.BuildFeaturesIntoRecord(to, record, p_0);
			//now make sure we keep track of where we are in the record to make room for other features
			p_0 += feature_set.NumFeatures();
		}
	}

	/**
	 * Sets the response variable, the phenotype class
	 * 
	 * @param classNum		the class to set
	 */
	public void setClass(int classNum){
		record[datumSetupForImage.numFeatures()] = classNum; //the last value is the y_i
	}
	
	public double[] getRecord(){
		return record;
	}
	
//	public Set<String> filterNames(){
//		return datumSetup.getFilterNamesToColors().keySet();
//	}
	
	/** debug purposes only */
	public void Print(){
		for (int i = 0; i <= datumSetupForImage.numFeatures(); i++){
			System.out.print(record[i] + ":");
		}
		System.out.print("\n");
	}
}