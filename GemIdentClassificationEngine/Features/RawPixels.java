package GemIdentClassificationEngine.Features;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import javax.imageio.ImageIO;

public class RawPixels extends DatumFeatureSet {

	private SuperImage superImage;
	private static final double RotationsPropOfCircumferencePoints = 0.5;
	private static final double TwoPI = 2 * Math.PI;
	private static int idx = 0;

	public static ArrayList<Double> thetasForTrainingData() {
		ArrayList<Double> thetas = new ArrayList<Double>();
		int r_max = Run.it.getMaxPhenotypeRadiusPlusMore(null);
		int number_of_thetas = (int) Math.round(2 * Math.PI * r_max * RotationsPropOfCircumferencePoints);
		double increment = TwoPI / number_of_thetas;
		Double angle = 0.0;
		for (int i = 1; i <= number_of_thetas; i++) {
			thetas.add(angle);
			angle += increment;
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
	public void BuildFeaturesIntoRecord(Point t, double[] record, int p_0) {
	}

	@Override
	public void UpdateFeatureTypes(ArrayList<FeatureType> feature_types, int p_0) {
	}

	@Override
	public void UpdateFeatureNames(ArrayList<String> feature_names, int p_0) {
	}

	@Override
	public void UpdateFeatureColors(ArrayList<Color> feature_colors, int p_0) {
	}

	public static Datum generateDatum(DatumSetupForImage datumSetupForImage, Point t, double theta, String phenoName, int counterNum) throws Exception {
		//System.err.println("Datum generateDatum");

		SuperImage super_image = ImageAndScoresBank.getOrAddSuperImage(datumSetupForImage.filename());
		Point t_adj = super_image.AdjustPointForSuper(t);
		int r_max = Run.it.getMaxPhenotypeRadiusPlusMore(null);
		int w = 2 * r_max + 1; // width of Phenotype Subset
		//get the piece of the image with all the data we can ever need
		//int w_rotated = (int) Math.ceil(w * Math.sqrt(2));
		int topleft_x = t_adj.x - r_max;
		int topleft_y = t_adj.y - r_max;
		BufferedImage whole_image = super_image.getAsBufferedImage();

		//BufferedImage local_window = super_image.getAsBufferedImage().getSubimage(t_adj.x - r_max, t_adj.y - r_max, w , w);
		BufferedImage local_window = super_image.getAsBufferedImage().getSubimage((t_adj.x - r_max) - (10), (t_adj.y - r_max) + (10), w + 10, w + 10);
		BufferedImage local_window_rotated = new BufferedImage(local_window.getWidth(), local_window.getHeight(), local_window.getType());
		int w_rotated = local_window_rotated.getWidth();
		//build a storage image for the rotated image to be... it's going to be bigger... use Pythagorean theorem


		//BufferedImage local_window_rotated = new BufferedImage(w_rotated + 100, w_rotated + 100, local_window.getType());
		//BufferedImage whole_image_rotated = new BufferedImage(whole_image.getWidth(),whole_image.getHeight(),whole_image.getType());
		//int new_y = (int)Math.round(topleft_y*Math.cos(theta) - topleft_x*Math.sin(theta));
		//int new_x = (int)Math.round(topleft_y*Math.sin(theta) + topleft_x*Math.cos(theta));
		//System.err.println("setup the rotation function");
		//setup the rotation function
		AffineTransform at = new AffineTransform();
		//Get to center of local_window_rotated so rotation works well

		//System.err.println("at.translate 1");
		//at.translate((w_rotated + 100)/2, (w_rotated + 100)/2);
		//System.err.println("at.rotate");
		at.rotate(theta, local_window.getWidth() / 2, local_window.getHeight() / 2);
		//at.rotate(theta,0,0);
		//System.err.println("at.translate 2");
		//at.translate(-w/2, -w/2);
		//System.err.println("scaleOp = new AffineTransformOp");
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		//do the rotation on the local_window and return the rotated image
		//System.err.println("scaleOp.filter");
		//local_window_rotated = scaleOp.filter(local_window, local_window_rotated);
		local_window_rotated = scaleOp.filter(local_window, local_window_rotated);
		//BufferedImage sub = whole_image_rotated.getSubimage(new_x - (w/2) ,new_y ,w_rotated ,w_rotated );
		//System.out.println("Print sub");

//        File g = new File(super_image.filename()+"_Theta_"+ String.format("%.4f",theta)+".jpg");
//            //ImageIO.write(local_window, "JPEG", f);
//
//		try {
//			ImageIO.write(local_window_rotated, "JPEG", g);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		//(new File("C:/Users/stefh/Project GemIdent/GemIdent/examples_image_sets/"+phenoName+"/")).mkdirs();
		
		/** Create file for rotate independent phenotypes*/

        //File g = new File(System.getProperty("user.dir")+"/test/"+phenoName+"/"+counterNum+"_Theta_"+ String.format("%.4f",theta)+".jpg");


        File outputimage = new File(System.getProperty("user.dir")+"/ClassLabels/"+phenoName+"/"+counterNum+"_Theta_"+ String.format("%.4f",theta)+".jpg");

		/** Insert file into Directory*/
        ImageIO.write(local_window_rotated, "JPEG", outputimage);

		//System.out.println("Solids.getSolid before");
		//now we need to use the solid circle mask to generate the feature vector
		ArrayList<Point> solid_mask = Solids.getSolid(r_max);
		//System.out.println("Solids.getSolid after");
		int num_points = solid_mask.size();
		//this is the center of the final rotated image
		Point center_rotated_image = new Point(w_rotated / 2, w_rotated / 2);

		//make the datum with the record (the vector)
		double[] record = new double[num_points * 3];
		//System.out.println("Before Datum(datumSetupForImage, t)");
		Datum d = new Datum(datumSetupForImage, t);
		//System.out.println("After Datum(datumSetupForImage, t)");

		//System.out.println("Before set record");
		//What is this even suppose to do ?
		d.setRecord(record);
		//System.out.println("After set record");

		//use this object for convenience (getR, getG, getB)

		DataImage local_window_rotated_dataimage = new RegularSubImage(local_window_rotated);

		//for each point in the mask, get the R,G,B values in the rotated image and enter them into the data vector (record)
		//System.out.println("Before RGB loop");


		for (int i = 0; i < num_points; i++) {
			int j = i + 1;
			Point offset = solid_mask.get(i);
			int x = offset.x;
			int y = offset.y;
			int x0 = center_rotated_image.x + offset.x;
			int y0 = center_rotated_image.y + offset.y;
			//System.out.println("x:" + x + "  x0:" + x0 + " y:" + y + " y0:" + y0 + "Record size:" + record.length);

			//red, green and blue go in the record IN THAT ORDER
			//System.out.println("Before R set");
			record[3 * i] = local_window_rotated_dataimage.getR(x0, y0);
			//System.out.println("Before G set");
			record[3 * i + 1] = local_window_rotated_dataimage.getG(x0, y0);
			//System.out.println("Before B set");
			record[3 * i + 2] = local_window_rotated_dataimage.getB(x0, y0);
			num_features += 1;
			//System.out.println("After RGB loop");

			//return the newly minted datum

		}
		return d;
	}
}
