package GemIdentClassificationEngine.Features;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import GemIdentClassificationEngine.Datum;
import GemIdentClassificationEngine.DatumSetupForImage;
import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.RegularSubImage;
import GemIdentImageSets.SuperImage;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;

public class RawPixels extends DatumFeatureSet {

	private SuperImage superImage;
	
	private static final double RotationsPropOfCircumferencePoints = 0.5;
	private static final double TwoPI = 2 * Math.PI;
	public static ArrayList<Double> thetasForTrainingData(){
		ArrayList<Double> thetas = new ArrayList<Double>();
		int r_max = Run.it.getMaxPhenotypeRadiusPlusMore(null);
		int number_of_thetas = (int)Math.round(2 * Math.PI * r_max * RotationsPropOfCircumferencePoints);
		for (int i = 0; i < number_of_thetas; i++){
			thetas.add(TwoPI / i);
		}		
		return thetas;
	}

	@Override
	public void InitializeForRun(ImageSetInterface imageSetInterface) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void InitializeDataForImage(String filename) {
		superImage = ImageAndScoresBank.getOrAddSuperImage(filename);
	}

	@Override
	public void BuildFeaturesIntoRecord(Point t, double[] record, int p_0) {}

	@Override
	public void UpdateFeatureTypes(ArrayList<FeatureType> feature_types, int p_0) {}

	@Override
	public void UpdateFeatureNames(ArrayList<String> feature_names, int p_0) {}

	@Override
	public void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0) {}

	public static Datum generateDatum(DatumSetupForImage datumSetupForImage, Point t, double theta) {
		SuperImage super_image = ImageAndScoresBank.getOrAddSuperImage(datumSetupForImage.filename());
		Point t_adj = super_image.AdjustPointForSuper(t);
		int r_max = Run.it.getMaxPhenotypeRadiusPlusMore(null);
		int w = 2 * r_max + 1;
		
		//get the piece of the image with all the data we can ever need
		BufferedImage local_window = super_image.getAsBufferedImage().getSubimage(t_adj.x - r_max, t_adj.y - r_max, w, w);
		//build a storage image for the rotated image to be... it's going to be bigger... use Pythagorean theorem
		int w_rotated = (int)Math.ceil(w / Math.sqrt(2));
		BufferedImage local_window_rotated = new BufferedImage(w_rotated, w_rotated, BufferedImage.TYPE_INT_RGB);
		
		//setup the rotation function
		AffineTransform at = new AffineTransform();
		at.translate(w + 1, w + 1);
		at.rotate(theta);
		at.translate(-(w + 1), -(w + 1));
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		//do the rotation on the local_window and return the rotated image
		local_window_rotated = scaleOp.filter(local_window, local_window_rotated);
		
		//now we need to use the solid circle mask to generate the feature vector
		ArrayList<Point> solid_mask = Solids.getSolid(r_max);
		int num_points = solid_mask.size();
		//this is the center of the final rotated image
		Point center_rotated_image = new Point(w_rotated, w_rotated);
		
		//make the datum with the record (the vector)
		double[] record = new double[num_points * 3];
		Datum d = new Datum(datumSetupForImage, t);
		d.setRecord(record);
		
		//use this object for convenience (getR, getG, getB)
		DataImage local_window_rotated_dataimage = new RegularSubImage(local_window_rotated);
		
		//for each point in the mask, get the R,G,B values in the rotated image and enter them into the data vector (record)
		for (int i = 0; i < num_points; i++){
			Point offset = solid_mask.get(i);
			int x = center_rotated_image.x + offset.x;
			int y = center_rotated_image.y + offset.y;
			
			//red, green and blue go in the record IN THAT ORDER
			record[3 * i    ] = local_window_rotated_dataimage.getR(x, y);
			record[3 * i + 1] = local_window_rotated_dataimage.getG(x, y);
			record[3 * i + 2] = local_window_rotated_dataimage.getB(x, y);
		}
		//return the newly minted datum
		return d;
	}
}
