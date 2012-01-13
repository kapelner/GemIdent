package GemIdentClassificationEngine.Features;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Lines;
import GemIdentTools.Matrices.IntMatrix;


public class MaxLineScores extends DatumFeatureSet {

	public static final int[] half_lengths_to_consider = {5, 9, 15, 17};
	private HashMap<String, IntMatrix> color_scores;
	private Set<String> color_names;
	
	@Override
	public void BuildFeaturesIntoRecord(Point to, double[] record, int p_0) {
		for (IntMatrix scoreMatrix : color_scores.values()){
		
			for (int hl : half_lengths_to_consider){
				//now we need to pull out all lines of this half-width
				ArrayList<ArrayList<Point>> all_lines_hl = Lines.getLines(hl);
				int[] all_scores = new int[all_lines_hl.size()];
				int i = 0; //each_with_index????
				for (ArrayList<Point> line : all_lines_hl){
					//now we need to get the scores for all these 
					//lines and load them in the all_scores array
					int score = 0;
					for (Point t : line){
						score += scoreMatrix.get(t.x + to.x, t.y + to.y);
					}	
					all_scores[i] = score;
					i++;
				}
				//we're done building scores for all the lines in this half-length class
				//now we need to find the minimum and maximum and actually load them into
				//the actual record of this datum
				//first sort the array of scores
				Arrays.sort(all_scores);
				//now pull out the minimum and maximum
				int min_score_for_this_color = all_scores[0];
//				int max_score_for_this_color = all_scores[all_scores.length - 1];
//				System.out.println("color: " + color + " hl = " + hl + " min: " + min_score_for_this_color + " max: " + max_score_for_this_color + " n = " + all_scores.length);
				//now do some pointer arithmetic
				record[p_0] = min_score_for_this_color;				
				p_0++;
//				record[p_0] = max_score_for_this_color;				
//				p_0++;				
			}
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
		for (String color : color_names){
			for (int hl : half_lengths_to_consider){
				feature_names.add(p_0, color + "_min_line_hl_" + hl);
				p_0++;
			}
		}		
	}
	
	@Override
	public void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0) {
		for (String color : color_names){
			for (int i = 0; i < half_lengths_to_consider.length; i++){
				feature_colors.add(p_0, Run.it.imageset.getWaveColor(color)); //FIX THIS
				p_0++;
			}
		}
	}
	
	@Override
	public void InitializeForRun(ImageSetInterface imageSetInterface) {
//		System.out.println("InitializeDataForRun()  " + imageSetInterface);	
		color_names = imageSetInterface.getFilterNames();
		num_features = half_lengths_to_consider.length * color_names.size(); 
	}

	@Override
	public void InitializeDataForImage(String filename) {
		color_scores = ImageAndScoresBank.getOrAddScores(filename);	
	}

}
