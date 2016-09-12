package GemIdentClassificationEngine.Features;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import GemIdentImageSets.ImageSetInterface;

/**
 * This is the abstract class that all feature sets
 * must inherit
 * 
 * @author kapelner
 *
 */
public abstract class DatumFeatureSet {
	
	/** how many features is represented by this feature set? */
	protected int num_features;
	
	/** what type of feature is this, a number, and ordinal category, or a category? */
	public enum FeatureType {NUMBER, ORDINAL, NOMINAL};
	
	/** the different feature sets themselves as symbols -- ADD TO THIS MASTER LIST */
	public enum FeatureSetName {ColorRingScores, MaxLineScores, EdgeRingScores, RawPixelValues};
	
	/**
	 * Each type of feature set requires all sorts of custom information based on the image
	 * @param imageSetInterface 
	 * 
	 * @param filename		the filename of the image being classified
	 */
	public abstract void InitializeForRun(ImageSetInterface imageSetInterface);	

	/**
	 * Each type of feature set requires all sorts of custom information based on the image
	 * @param imageSetInterface 
	 * 
	 * @param filename		the filename of the image being classified
	 */
	public abstract void InitializeDataForImage(String filename);
	
	/**
	 * We need to take these features and build them into the actual record
	 * 
	 * @param record		the data record
	 * @param p_0			the starting column number to begin to build
	 */
	public abstract void BuildFeaturesIntoRecord(Point t, double[] record, int p_0);
	
	/**
	 * We need to update the column types
	 * 
	 * @param feature_types		the record of the feature types
	 * @param p_0				the starting column number to begin to build
	 */
	public abstract void UpdateFeatureTypes(ArrayList<FeatureType> feature_types, int p_0);	
	
	/**
	 * We need to update the names of the columns
	 * 
	 * @param feature_names		the record of the feature names
	 * @param p_0				the starting column number to begin to build
	 */
	public abstract void UpdateFeatureNames(ArrayList<String> feature_names, int p_0);
	
	/**
	 * 
	 * @param feature_colors	the record of the feature colors
	 * @param p_0				the starting column number to begin to build
	 */
	public abstract void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0);
	
	public int NumFeatures(){
		return num_features;
	}

	

}
