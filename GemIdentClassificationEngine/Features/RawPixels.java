package GemIdentClassificationEngine.Features;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.SuperImage;

public class RawPixels extends DatumFeatureSet {

	private SuperImage superImage;

	@Override
	public void InitializeForRun(ImageSetInterface imageSetInterface) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void InitializeDataForImage(String filename) {
		superImage = ImageAndScoresBank.getOrAddSuperImage(filename);
	}

	@Override
	public void BuildFeaturesIntoRecord(Point t, double[] record, int p_0) {
//		superImage.getColo
		Point t_adj = superImage.AdjustPointForSuper(t);
		DataImage image = (DataImage)superImage;
		
		image.getR(i, j);
		image.getR(i, j);
		image.getR(i, j);
	}

	@Override
	public void UpdateFeatureTypes(ArrayList<FeatureType> feature_types, int p_0) {}

	@Override
	public void UpdateFeatureNames(ArrayList<String> feature_names, int p_0) {}

	@Override
	public void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0) {}

}
