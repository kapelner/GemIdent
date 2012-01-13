package GemIdentClassificationEngine.Features;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.IntMatrix;

public class EdgeRingScores extends DatumFeatureSet {

	/** The intensity of "edgeness" by pixel for this image */
	private IntMatrix edge_scores;
	/** maximize radius in which to compute ring scores */
	private int R;
	

	@Override
	public void InitializeForRun(ImageSetInterface imageSetInterface) {
		R = Run.it.getMaxPhenotypeRadiusPlusMore(null);
		num_features = R + 1; 
	}
	
	@Override
	public void InitializeDataForImage(String filename) {
		edge_scores = ImageAndScoresBank.getOrAddEdgeScores(filename);
	}

	@Override
	public void BuildFeaturesIntoRecord(Point t, double[] record, int p_0) {
		int Lo=0;
		for (int r = 0; r <= R; r++){
			record[Lo] = ColorRingScores.ComputeRingScore(edge_scores, t, r);
			Lo++;
		}
	}

	@Override
	public void UpdateFeatureTypes(ArrayList<FeatureType> feature_types, int p_0) {
		for (int p = p_0; p < num_features + p_0; p++){
			feature_types.add(p, FeatureType.NUMBER);
		}
	}

	@Override
	public void UpdateFeatureNames(ArrayList<String> feature_names, int p_0) {
		for (int r = 0; r <= R; r++){
			feature_names.add(p_0, "edge_ring_r_" + r);
			p_0++;
		}
	}
	
	@Override
	public void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0) {
		for (int r = 0; r <= R; r++){
			feature_colors.add(p_0, Color.GRAY);
			p_0++;
		}
	}	


}
