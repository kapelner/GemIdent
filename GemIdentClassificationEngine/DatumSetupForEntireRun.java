package GemIdentClassificationEngine;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import GemIdentClassificationEngine.Features.*;
import GemIdentClassificationEngine.Features.DatumFeatureSet.FeatureSetName;
import GemIdentClassificationEngine.Features.DatumFeatureSet.FeatureType;
import GemIdentImageSets.ImageSetInterface;

public class DatumSetupForEntireRun {

	/** the number of features */
	protected int p;
	/** the data types of the features */
	protected ArrayList<FeatureType> feature_types;	
	/** the names of the features */
	protected ArrayList<String> feature_names;
	/** the colors of the features (to be used when printing out a graphic of the importances) */
	protected ArrayList<Color> feature_colors;	
	/** the sets of features used, this is different than the number of features, since each feature set can contain MANY features */
	protected LinkedHashSet<DatumFeatureSet> feature_sets;
	protected ImageSetInterface imageSetInterface;
	
	public DatumSetupForEntireRun(){}	

	public DatumSetupForEntireRun(ImageSetInterface imageSetInterface){
		feature_sets = new LinkedHashSet<DatumFeatureSet>();
		this.imageSetInterface = imageSetInterface;		
	}

	public void initialize() {
		//add up all the features
		for (DatumFeatureSet feature_set : feature_sets){
			feature_set.InitializeForRun(imageSetInterface);
			p += feature_set.NumFeatures();			
		}
//		System.out.println("DatumSetupForEntireRun()  p = " + numFeatures());		
		InitializeFeatureTypesNamesAndColors();
	}	
	
	private void InitializeFeatureTypesNamesAndColors() {
		feature_types = new ArrayList<FeatureType>(p);
		feature_names = new ArrayList<String>(p);
		feature_colors = new ArrayList<Color>(p);
		
		//now get the feature types and feature names in one shot
		int p_0 = 0;
		for (DatumFeatureSet feature_set : feature_sets){
			//build the features into the array
			feature_set.UpdateFeatureTypes(feature_types, p_0);
			feature_set.UpdateFeatureNames(feature_names, p_0);
			feature_set.UpdateFeatureColors(feature_colors, p_0);
			//now make sure we keep track of where we are in the record to make room for other features
			p_0 += feature_set.NumFeatures();
		}
	}
	
	/**
	 * What is the name of the jth feature? To be overwritten.
	 * 
	 * @param j 	The number of the feature
	 * @return		The feature's name
	 */
	public String getFeatureName(int j) {
		return feature_names.get(j);
	}
	
	public ArrayList<String> getFeatureNames() {
		return feature_names;
	}

	/**
	 * What is the name of the jth feature? To be overwritten.
	 * 
	 * @param j 	The number of the feature
	 * @return		The feature's name
	 */
	public FeatureType getFeatureType(int j) {
		return feature_types.get(j);
	}	
	
	public LinkedHashSet<DatumFeatureSet> getFeatureSets(){
		return feature_sets;
	}
	
	/**
	 * How many features does this datum have? 
	 * What is M? xi. = [xi1, xi2, ..., xim]
	 * 
	 * @return	The size of the features vector
	 */		
	public int numFeatures(){
		return p;
	}

	/**
	 * The place where feature sets get added to the classification run. If
	 * you're building a new feature set, put the symbol here and the add code
	 * that instantiates the class after the appropriate case statement and add a break
	 * 
	 * @param featureSetName
	 */
	public void addFeatureSet(FeatureSetName featureSetName) {
		switch (featureSetName){
			case ColorRingScores:
				feature_sets.add(new ColorRingScores());
				break;
			case MaxLineScores:
				feature_sets.add(new MaxLineScores());
				break;
			case EdgeRingScores:
				feature_sets.add(new EdgeRingScores());
				break;
			case RawPixelValues:
				feature_sets.add(new RawPixels());
		}
	}

}
