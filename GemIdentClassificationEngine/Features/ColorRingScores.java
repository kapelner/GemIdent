package GemIdentClassificationEngine.Features;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Rings;
import GemIdentTools.Matrices.IntMatrix;

public final class ColorRingScores extends DatumFeatureSet {
	
	/** A map from color name --> intensity values for each pixel, for this image */
	private HashMap<String, IntMatrix> color_scores;
	/** maximize radius in which to compute ring scores */
	private int R;
	private Set<String> color_names;

	@Override
	public void BuildFeaturesIntoRecord(Point t, double[] record, int p_0) {
		int Lo=0;
		for (String color : color_scores.keySet()){
			for (int r = 0; r <= R; r++){
				record[Lo] = ComputeRingScore(color_scores.get(color), t, r);
				Lo++;
			}
		}		
	}

	@Override
	public void InitializeDataForImage(String filename) {
		color_scores = ImageAndScoresBank.getOrAddScores(filename);		
	}

	@Override
	public void InitializeForRun(ImageSetInterface imageSetInterface) {
//		System.out.println("InitializeDataForRun()  " + imageSetInterface);	
		color_names = imageSetInterface.getFilterNames();
		R = Run.it.getMaxPhenotypeRadiusPlusMore(null);
		num_features = (R + 1) * color_names.size(); 
	}	

	@Override
	public void UpdateFeatureTypes(ArrayList<FeatureType> feature_types, int p_0) {
		for (int p = p_0; p < num_features + p_0; p++){
			feature_types.add(p, FeatureType.NUMBER);
		}
	}

	@Override
	public void UpdateFeatureNames(ArrayList<String> feature_names, int p_0) {
		for (String color : color_names){
			for (int r = 0; r <= R; r++){
				feature_names.add(p_0, color + "_ring_r_" + r);
				p_0++;
			}
		}
	}
	

	/**
	 * Generates a "ring score" -  a scalar score for 
	 * a given radius and a given score matrix
	 * by adding up the scores in the score matrix at each coordinate in
	 * the discretized ring of the given radius. See step 5a in the 
	 * Algorithm section of the IEEE paper for a formal mathematical description.
	 * The method is static because we will want to share it with other classes.
	 *  
	 * @param scoreMatrix		the matrix of scores for a given color
	 * @param r					the radius of the ring
	 * @return					the summed up total score
	 * 
	 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
	 */
	public static int ComputeRingScore(IntMatrix scoreMatrix, Point to, int r){
		int score = 0;
		for (Point t : Rings.getRing(r)){
			score += scoreMatrix.get(t.x + to.x, t.y + to.y);
		}
		return score;
	}

	@Override
	public void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0) {
		for (String color : color_names){
			for (int r = 0; r <= R; r++){
				feature_colors.add(p_0, Run.it.imageset.getWaveColor(color));
				p_0++;
			}
		}
	}	
}
