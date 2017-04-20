package GemIdentClassificationEngine;

import java.awt.Point;
import java.util.LinkedHashSet;

import GemIdentClassificationEngine.Features.DatumFeatureSet;
import GemIdentClassificationEngine.Features.RawPixels;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;

/**
 * This is the most generic datum setup possible,
 * only the number of features is required
 * 
 * @author kapelner
 *
 */
public class DatumSetupForImage {

	/** the filename of this image */
	private String filename;
	/** the "master" setup for all datums across this classification run */
	private DatumSetupForEntireRun datumSetupForEntireRun;

	public DatumSetupForImage(DatumSetupForEntireRun datumSetupForEntireRun, String filename){
		this.datumSetupForEntireRun = datumSetupForEntireRun;
		this.filename = filename;
		InitializeFeatureDataForImage();		
	}
	
	public LinkedHashSet<DatumFeatureSet> getFeatureSets(){
		return datumSetupForEntireRun.getFeatureSets();
	}	
	
	private void InitializeFeatureDataForImage() {
		for (DatumFeatureSet feature_set : datumSetupForEntireRun.getFeatureSets()){
			feature_set.InitializeDataForImage(filename);
		}
	}

	public int numFeatures(){
		return datumSetupForEntireRun.numFeatures();
	}

	
	public Datum generateDatumAtPoint(Point t){
		SuperImage superImage = ImageAndScoresBank.getOrAddSuperImage(filename);
		return new Datum(this, superImage.AdjustPointForSuper(t));
	}
	
	public String filename(){
		return filename;
	}

	public boolean containsRawPixelFeatures() {
		for (DatumFeatureSet feature_set : datumSetupForEntireRun.feature_sets){
			if (feature_set instanceof RawPixels){
				return true;
			}
		}
		return false;
	}
}
