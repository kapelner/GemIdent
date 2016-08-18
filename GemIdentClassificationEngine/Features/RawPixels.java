package GemIdentClassificationEngine.Features;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.SuperImage;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;

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
		Point t_adj = superImage.AdjustPointForSuper(t);
		DataImage image = (DataImage)superImage;
		ArrayList<Point> solid_offsets = Solids.getSolid(Run.it.getMaxPhenotypeRadiusPlusMore(null), 0D);
		int num_points = solid_offsets.size();
		
		for (int i = 0; i < num_points; i++){
			Point offset = solid_offsets.get(i);
			int x = t_adj.x + offset.x;
			int y = t_adj.y + offset.y;
			
			//red, green and blue go in the record IN THAT ORDER
			record[3 * i    ] = image.getR(x, y);
			record[3 * i + 1] = image.getR(x, y);
			record[3 * i + 2] = image.getR(x, y);
		}

	}

	@Override
	public void UpdateFeatureTypes(ArrayList<FeatureType> feature_types, int p_0) {}

	@Override
	public void UpdateFeatureNames(ArrayList<String> feature_names, int p_0) {}

	@Override
	public void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0) {}
}
