/*
    GemIdent v1.1b
    Interactive Image Segmentation Software via Supervised Statistical Learning
    http://gemident.com
    
    Copyright (C) 2009 Professor Susan Holmes & Adam Kapelner, Stanford University

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details:
    
    http://www.gnu.org/licenses/gpl-2.0.txt

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package GemIdentOperations;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JLabel;

import GemIdentCentroidFinding.PostProcess;
import GemIdentClassificationEngine.TrainingData;
import GemIdentImageSets.DataImage;
import GemIdentTools.IOTools;
import GemIdentTools.Geometry.Rings;
import GemIdentTools.Matrices.BoolMatrix;

/**
 * Finds Type I errors in the images - places where the user
 * marked positive in the training set, but the classification
 * failed to locate
 * 
 * @author Adam Kapelner
 */
public class FindErrorRateViaReconcilation {

	/** the thread pool responsible for processing each image */
	private ExecutorService errorPool;
	/** the mapping from filename to the mapping from phenotype to matrix of training points */
	private HashMap<String,HashMap<String,BoolMatrix>> allTrainingPoints;
	/** the mapping from phenotype to {@link .RightWrong RightWrong} objects that describe the error rates for a given phenotype */
	private LinkedHashMap<String,RightWrong> tabulations;
	/** the subset of phenotypes that have been post-processed to find centroids */
	private Set<String> postProcessSet;
	/** the mapping from phenotype to its type I error rate */
	private HashMap<String,Double> errorRates;
	/** the mapping from filename to a set of coordinates that are false negatives */
	private HashMap<String,HashSet<Point>> typeOneErrors;

	
	/** A simple struct to record the number of right and the number of wrong and compute an error rate */
	private class RightWrong{
		/** the number correct */
		public int right;
		/** the number incorrect */
		public int wrong;
		/** gets the error rate as a percentage */
		public double GetErrorRate(){
			return wrong*100/((double)(right+wrong));
		}
	}
	/**
	 * Initialize the error rate finding process. Find the subset of phenotypes
	 * that were post-processed to find centroids, get all the training points by
	 * calling {@link TrainingData#GetAllTrainingPoints(HashMap, Set) GetAllTrainingPoints},
	 * initialize the thread pool and add an initialized {@link FindErrorRateViaReconcilation.ErrorProcessor ErrorProcessor}
	 * object for each image to check, and ultimately create a dialog box that shows the user the errors
	 * 
	 * @param totals			a string header for the dialog box that will be created upong completion
	 * @param errorRates		a mapping from phenotype to error rate - passed in so the mapping can be saved for later use
	 * @param typeOneErrors		a mapping from image to the coordinates that are errors - passed in so the mapping can be saved for later use
	 */
	public FindErrorRateViaReconcilation(String totals,HashMap<String,Double> errorRates,HashMap<String,HashSet<Point>> typeOneErrors){
		
		this.errorRates=errorRates;
		this.typeOneErrors=typeOneErrors;
		
		postProcessSet=Run.it.getPhenotyeNamesSaveNONAndFindCenters();
		if (postProcessSet.size() == 0) //if there's nothing to do, leave
			return;
		
		allTrainingPoints=new HashMap<String,HashMap<String,BoolMatrix>>();
		TrainingData.GetAllTrainingPoints(allTrainingPoints,postProcessSet);
		
		
		tabulations=new LinkedHashMap<String,RightWrong>(postProcessSet.size());
		for (String phenotype:Run.it.getPhenotyeNamesSaveNON())
			tabulations.put(phenotype,new RightWrong());
		
		errorPool=Executors.newFixedThreadPool(Run.it.num_threads);
		for (String filename:Run.it.getPhenotypeTrainingImages())
			errorPool.execute(new ErrorProcessor(filename));
		errorPool.shutdown();
		try {	         
			errorPool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    errorPool = null;

		for (String phenotype:tabulations.keySet())
			errorRates.put(phenotype,tabulations.get(phenotype).GetErrorRate());
		PrintToSummary();
		CreateDialog(totals);
	}
	/**
	 * Creates a dialog box that displays a header with counts information 
	 * calculated from {@link PostProcess#GetCountMessage() GetCountMessage}
	 * and then adds the false negative rates for the phenotypes. The dialog box is
	 * then spawned inside a new Thread in order to not slow Swing down. For more
	 * information about this dialog, please consult section 4.4 of the manual.
	 * 
	 * @param totals		the header that contains title and count information
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private void CreateDialog(String totals){
		String message=totals+"<br><br><br>Type I Error Rates for <br>Phenotype Locations:<br><br>";
	    NumberFormat format=NumberFormat.getNumberInstance();
	    format.setMaximumFractionDigits(3);
		for (String phenotype:postProcessSet){
			String a=phenotype;
			int na=a.length();
			String b=format.format(errorRates.get(phenotype))+"%";
			int nb=b.length();
			message+=a;
			for (int i=0;i<PostProcess.numCharPerMessageLine-na-nb;i++)
				message+="&nbsp;";
			message+=b;
			message+="<br>";
		}
		final String fmessage="<HTML>"+message+"</HTML>";
		(new Thread(){ //easy way to make it non-modal, i'm too lazy  . . .
			public void run(){
				JFrame dialog=new JFrame();
				dialog.setTitle("Counts/Errors");
				JLabel label=new JLabel(fmessage);
				dialog.add(label);				
				dialog.setVisible(true);
				dialog.pack();	
//				JOptionPane.showMessageDialog(Run.it.getGUI(),fmessage,"Total Counts and Error Rates",JOptionPane.INFORMATION_MESSAGE);	
			}
		}).start();		
	}
	/** Adds the false negative rates to the summary file in the output directory */
	private void PrintToSummary() {
		PrintWriter out=null;
		try {
			out=new PrintWriter(new BufferedWriter(new FileWriter(Run.it.imageset.getFilenameWithHomePath(PostProcess.outputDir+File.separator+Run.it.projectName+"-"+PostProcess.summary),true)));
		} catch (IOException e) {
			System.out.println(PostProcess.outputDir+File.separator+Run.it.projectName+"-"+PostProcess.summary+" cannot be edited in CSV appending");
		}
		
		NumberFormat format=NumberFormat.getInstance();
		format.setMaximumFractionDigits(2);
		
		out.print("\n\nError Rates");
		for (String phenotype:postProcessSet){
			out.print("\n"+phenotype+"\t\t\t");
			out.print(format.format(errorRates.get(phenotype)) + "%");
		}
		out.close();		
	}
	/**
	 * Populates a map from phenotype to {@link GemIdentTools.Matrices.BoolMatrix BoolMatrix}
	 * that contains its postive points (as determined by classification)
	 * by loading the result image from the {@link GemIdentClassificationEngine.Classify#processedDir processedDir}
	 * 
	 * @param isCentroid		the mapping from phenotype - positive points as determined in classification
	 * @param filename			the image of interest
	 */
	private void PopulateIsCentroid(HashMap<String,BoolMatrix> isCentroid,String filename){
		for (String phenotype:postProcessSet){
			try {
				isCentroid.put(phenotype,new BoolMatrix(IOTools.OpenImage(PostProcess.GetIsCentroidName(filename,phenotype))));
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}	
	/** Framework for finding errors in one image and can be threaded in a thread pool */
	private class ErrorProcessor implements Runnable{

		/** the image that the errors are to be found in */
		private String filename;
		/** the mapping from phenotype to its positive points */
		private HashMap<String,BoolMatrix> isCentroid;
		/** the mapping from phenotype to its points trained by the user */
		private HashMap<String,BoolMatrix> train;

		/**
		 * Initializes an error processor for a given image, populates
		 * the mapping from all the image's phenotypes to their positively
		 * classified points, then gets the user's trained points.
		 * 
		 * @param filename		the image to find errors in
		 */
		public ErrorProcessor(String filename){
			this.filename=filename;
			isCentroid=new HashMap<String,BoolMatrix>(postProcessSet.size());
			PopulateIsCentroid(isCentroid,filename);
			train=allTrainingPoints.get(filename);
		}
		/**
		 * For all phenotypes, calculate the error rate
		 */
		public void run() {
			for (String phenotype:postProcessSet)
				ComputeErrors(isCentroid.get(phenotype),train.get(phenotype),phenotype);			
		}
		/**
		 * Given the classified coordinates and the trained coordinates, iterate over those trained
		 * and look for a classified coordinate 
		 * {@link GemIdentTools.Matrices.BoolMatrix#FindPointsInGivenRadius(int, int, int) nearby} - 
		 * within the rMax of the phenotype of interest.
		 * If one or more are found, find the one {@link #FindClosestPoint(Point, ArrayList) closest} and
		 * delete it from the grid of classified coordinates lest it be
		 * double-counted, then increment the number right. If one is not found, consider it a
		 * false negative, increment the number wrong and record its location in the typeOneErrors
		 * mapping. Then record the number right and wrong in the master tabulation for this phenotype
		 * 
		 * @param predicted		the coordinates classified by the computer as positive
		 * @param trained		the coordinates trained by the user
		 * @param phenotype		the phenotype that we are examining
		 */
		private void ComputeErrors(BoolMatrix predicted,BoolMatrix trained,String phenotype){
			if (trained == null)
				return;
			
			int right=0;
			int wrong=0;
			int rMax=(Run.it.getPhenotype(phenotype)).getRmax();
			
			for (int i=0;i<trained.getWidth();i++){
				for (int j=0;j<trained.getHeight();j++){
					if (trained.get(i,j)){
						ArrayList<Point> points=predicted.FindPointsInGivenRadius(i,j,rMax);
						Point it=FindClosestPoint(new Point(i,j),points);
						if (it == null){
							wrong++;
							HashSet<Point> set = typeOneErrors.get(filename);
							if (set == null){
								set=new HashSet<Point>();
								typeOneErrors.put(filename,set);
							}
							set.add(new Point(i,j));
						}
						else {
							right++;
							predicted.set(it,false); //
						}
					}
				}
			}			
			RightWrong r=tabulations.get(phenotype);
			r.right+=right;
			r.wrong+=wrong;				
		}
		/**
		 * From a "central" point, find the closest point in a given list
		 * by checking the euclidean distances for each
		 * 
		 * @param point		the central point
		 * @param points	the other points
		 * @return			the closest point in the "points" list
		 */
		private Point FindClosestPoint(Point point,ArrayList<Point> points) {
			int n=points.size();
			if (n == 0)
				return null;
			if (n == 1)
				return points.get(0);
			
			ArrayList<Double> distances=new ArrayList<Double>(n);
			
			for (Point t:points)
				distances.add(point.distance(t));
			
			double min=Double.MAX_VALUE;
			int place=0;
			
			for (int i=0;i<n;i++){
				if (distances.get(i) < min){
					min=distances.get(i);
					place=i;
				}
			}			
			return points.get(place);
		}		
	}
	/** the color used in the center of the ring */
	private static final Color in=Color.WHITE;
	/** the color used in the outside of the ring */
	private static final Color out=Color.RED;
	/** the radius of the drawn error ring */
	private static final int RingRad=8;
	/**
	 * Draw a red and white rings over an image around
	 * the places where there are false negatives.
	 * 
	 * @param displayImage		the dataimage to draw atop of using an overlay
	 * @return					the image with the rings drawn
	 */
	public static BufferedImage CreateErrorOverlay(DataImage displayImage) {
		BufferedImage overlay = new BufferedImage(displayImage.getWidth(),displayImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
		HashSet<Point> set=Run.it.getTypeOneErrors().get(displayImage.getFilename());
		if (set == null)
			return null;
		if (set.size() == 0)
			return null;
		for (Point to:set){
			for (Point t:Rings.getRingUsingCenter(RingRad-2,to,overlay.getWidth(),overlay.getHeight()))
				try {overlay.setRGB(t.x,t.y,out.getRGB());} catch (Exception e){}
			for (Point t:Rings.getRingUsingCenter(RingRad-1,to,overlay.getWidth(),overlay.getHeight()))
				try {overlay.setRGB(t.x,t.y,out.getRGB());} catch (Exception e){}
			for (Point t:Rings.getRingUsingCenter(RingRad,to,overlay.getWidth(),overlay.getHeight()))
				try {overlay.setRGB(t.x,t.y,in.getRGB());} catch (Exception e){}
			for (Point t:Rings.getRingUsingCenter(RingRad+1,to,overlay.getWidth(),overlay.getHeight()))
				try {overlay.setRGB(t.x,t.y,out.getRGB());} catch (Exception e){}
			for (Point t:Rings.getRingUsingCenter(RingRad+2,to,overlay.getWidth(),overlay.getHeight()))
				try {overlay.setRGB(t.x,t.y,out.getRGB());} catch (Exception e){}
		}
		return overlay;
	}
}