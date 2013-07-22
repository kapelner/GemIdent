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

package GemIdentCentroidFinding;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import GemIdentClassificationEngine.Classify;
import GemIdentImageSets.DataImage;
import GemIdentImageSets.ExclusionRuleStruct;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.NonGlobalImageSet;
import GemIdentImageSets.RegularSubImage;
import GemIdentImageSets.Nuance.NuanceImageListInterface;
import GemIdentOperations.FindErrorRateViaReconcilation;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.IOTools;
import GemIdentView.ClassifyProgress;
import GemIdentView.KClassifyPanel;

/**
 * Post-processing converts the results from classification,
 * the binary {@link GemIdentTools.Matrices.BoolMatrix BoolMatrices} that
 * store locations where parts of phenotypes are thought to be into
 * exact counts of those phenotypes via centroid-location blob 
 * analysis.
 * 
 * @author Adam Kapelner
 */
public class PostProcess {

	/** the directory where final output files are stored changes every classification - composed of the "first name" plus a timestamp */
	public static String outputDir;	
	/** the "first name" of the output dir */
	public static final String outputDirFirstName = "output";
	/** the filename of a "summary" file that gives counts and error rates */
	public static final String summary="summary.txt";
	/** the directory where output from the analysis panel is placed */
	public static final String analysisDir="analyses";
	/** the thread pool responsible for post-processing each image */
	private ExecutorService postprocessPool;
	/** the progress bar updating the user on the post-processing progress */
	private ClassifyProgress progress;
	/** the panel the post-processing is taking place in */
	private KClassifyPanel classifyPanel;
	/** the {@link LabelViaSmartErosions heuristic classifier} that performs the blob analysis and finds centroids */
	private CentroidFinder centroidFinder;
	/** the mapping from phenotype name to the total number of phenotype centroids globally */
	private LinkedHashMap<String,Long> totals;
	/** the specific phenotypes to post-process to find centroids (those whose {@link GemIdentView.KPhenotypeInfo#findCentroids "Cent" checkbox} is checked and not the NON) */
	private Set<String> postProcessSet;
	/** whether or not to stop the current post-processing procedure */
	private BooleanFlag stop;
	/** all the images in the project directory */
	private Collection<String> filenames;
	/** the mapping from phenotype name to its error rates */
	private LinkedHashMap<String,Double> errorRates;
	/** the mapping from image name to the set of coordinates that are false negatives in that image */
	private HashMap<String,HashSet<Point>> typeOneErrors;

	/** dumb struct that contains a boolean */
	public class BooleanFlag {
		private boolean flag;
		public void set(boolean flag){
			this.flag = flag;
		}
		public boolean get(){
			return flag;
		}
	}
	/** Default constructor, also lets the progress bar initialize */
	public PostProcess(Collection<String> filenames,ClassifyProgress progress,KClassifyPanel classifyPanel,HashMap<String,HashSet<Point>> typeOneErrors){
		this.classifyPanel=classifyPanel;
		this.progress=progress;
		this.filenames=filenames;
		this.typeOneErrors=typeOneErrors;
		Run.it.imageset.LOG_AddToHistory("begun centroid-finding of " + filenames.size() + " images");
		progress.BeginPostProcessing();
		stop = new BooleanFlag();
	}	

	/**
	 * Find the centroids in the classified images. Begin by finding
	 * which phenotypes are relevant (if none, then exit), then
	 * creating the {@link #CreateHeuristicClassifier() Heuristic Classifier},
	 * checking if it {@link LabelViaSmartErosions#canProcess() can process},
	 * {@link #PrintFileHeaderSummary() printing the headers} for the files 
	 * that will record the centroid information, initializing the thread
	 * pool and threading a {@link PostProcess.PostProcessor PostProcessor}
	 * for each image, recording the counts in real time, then when completed, the
	 * {@link FindErrorRateViaReconcilation#FindErrorRateViaReconcilation(String, HashMap, HashMap) 
	 * error rates are found}, then the heuristic classifier is deleted and
	 * the {@link #summary summary file} is {@link #WriteTotalsToSummary() written}
	 *
	 */
	public void FindCentroids() {
		
		postProcessSet=Run.it.getPhenotyeNamesSaveNONAndFindCenters();
		if (postProcessSet.size() == 0){
			progress.RemovePostProcessBar();
			classifyPanel.ClassificationDone(); //reenable buttons
			return;
		}
		
		//in order to create the best-looking check images, let's make all waves visible for NuanceImageSets
		if (Run.it.imageset instanceof NuanceImageListInterface){
			((NuanceImageListInterface)Run.it.imageset).setAllVisible();
		}
		
		System.out.println("begin creating centroid-finder classifier . . .");
		CreateHeuristicClassifier();
		System.out.println("done");
		if (stop.get())
			return;
		
		if (!centroidFinder.canProcess())
			return;
		
		progress.CreatedClassifier(filenames.size());
		
		
		
		totals=new LinkedHashMap<String,Long>(postProcessSet.size());
		
		PrintFileHeaderSummary();
		for (String phenotype:postProcessSet){
			PrintFileHeader(phenotype);
			totals.put(phenotype,(long)0);
		}

		postprocessPool=Executors.newFixedThreadPool(Run.it.num_threads);
		for (String filename:filenames)
	    	postprocessPool.execute(new PostProcessor(filename));
		postprocessPool.shutdown();
		try {	         
	         postprocessPool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    
		progress.RemovePostProcessBar();
		classifyPanel.ClassificationDone(); //reenable buttons
		
		Run.it.imageset.LOG_AddToHistory("finished centroid-finding");
		
		errorRates=new LinkedHashMap<String,Double>(postProcessSet.size());
		new FindErrorRateViaReconcilation(GetCountMessage(),errorRates,typeOneErrors);

		//we need to keep postProcess around in order to get counts/errors for report generation, however we no longer need this massive object
		WriteTotalsToSummary();
	}
	public LinkedHashMap<String,Long> getTotalCounts(){
		return totals;
	}
	public LinkedHashMap<String,Double> getErrorRates(){
		return errorRates;
	}
	
	/** The number of characters in one line of the message (a bad hack) */
	public static final int numCharPerMessageLine=40;
	
	/** gets an HTML message that displays each phenotype's name and counts */
	public String GetCountMessage() {
		String message="Total Counts<br><br>";
		for (String phenotype:postProcessSet){
			String a=phenotype;
			int na=a.length();
			String b=""+totals.get(phenotype);
			int nb=b.length();

			message+=a;
			for (int i=0;i<numCharPerMessageLine-na-nb;i++)
				message+="&nbsp;";
			message+=b;
			message+="<br>";
		}
		return message;		
	}
	/**
	 * Responsible for formatting the data in order to create a
	 * {@link CentroidFinder#CentroidFinder
	 * heuristic classifier} that will analyze the blobs and find centroids.
	 * A master map is created from image to a mapping from phenotypes to result matrices.
	 * All the training images are {@link Run#getPhenotypeTrainingImages() retrieved} and
	 * for each, its map from phenotypes to results are {@link #PopulateIs(HashMap, String) 
	 * populated}, and then placed within the master map. The progress bar is updated
	 * during the duration of this step.
	 */
	private void CreateHeuristicClassifier() {
		HashMap<String,HashMap<String,BoolMatrix>> allTrainingIs=new HashMap<String,HashMap<String,BoolMatrix>>();
		Set<String> trainingImages=Run.it.getPhenotypeTrainingImages();
//		Set<String> imagesTrained=Classify.AllClassified();
//		trainingImages.retainAll(imagesTrained);
		for (String filename:trainingImages){
			HashMap<String,BoolMatrix> isCentroid=new HashMap<String,BoolMatrix>(postProcessSet.size());
			try {
				PopulateIs(isCentroid,filename);
			}
			catch (Exception e){
				String message = "In order to find centroids, all images trained on must be classified";
				JOptionPane.showMessageDialog(Run.it.getGUI(), message);
				return;
			}
			progress.updatePostProcessLoad();
			allTrainingIs.put(filename,isCentroid);
		}
		try {
			//below is how to change the centroid finding method with some sample code commented out:
//			switch (Run.it.imageset.getCentroidFinderMethod()){
//				case CentroidFinder.MyCentroidLabelMethod:
//					centroidFinder = new MyCentroidLabelMethod(allTrainingIs, postProcessSet, classifyPanel, stop);
//					break;
//				default:
					centroidFinder = new LabelViaSmartErosions(allTrainingIs, postProcessSet, classifyPanel, stop);
//					break;
//			}			
		} catch (StackOverflowError stackoverflowerror){
			String message = "Stack overflow error while constructing centroid classifier.\n\nAre you sure you want to find centroids for all those phenotypes you selected?\nSome may not have centroids or have blobs that are too large. Return to your training set and check.\n\nIf you still think GemIdent is in error, restart the program and beef up the Xss and Xoss parameters\n(see the section 2 of the manual).";
			JOptionPane.showMessageDialog(Run.it.getGUI(), message);
			stackoverflowerror.printStackTrace();
		}	
		
	}
	/**
	 * Given an image, {@link IOTools#OpenImage(String) loads} all 
	 * the classification results from the {@link Classify#processedDir 
	 * processed folder} on the hard disk, {@link BoolMatrix#BoolMatrix(java.awt.image.BufferedImage)
	 * converts them into BoolMatrices}, {@link Classify#PreProcessIsImage(BoolMatrix) preprocesses}
	 * them to reduce noise and stores them in the "is" map
	 * 
	 * @param is			the map from phenotype to result matrix
	 * @param filename		the image whose results will be loaded and stored
	 */
	private void PopulateIs(HashMap<String,BoolMatrix> is,String filename){
		for (String phenotype:postProcessSet){
			try {
				is.put(phenotype,Classify.PreProcessIsImage(new BoolMatrix(IOTools.OpenImage(Classify.GetIsName(filename,phenotype)))));
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
	}	
	/** attempt to stop the post-processing */
	public void Stop(){
		stop.set(true);
		if (postprocessPool != null){ //ie we started to process the images		
			postprocessPool.shutdownNow();
		}
	}

	/** Framework for finding centroids in one image (for all phenotypes) and can be threaded in a thread pool */
	private class PostProcessor implements Runnable {
		
		/** the image to find centroids for */
		private String filename;
//		/** the image loaded into a {@link DataImage DataImage} object} - the locations of the centroids will be marked */
//		private DataImage actualImageFinal;
		/** the image loaded into a {@link DataImage DataImage} object} - the locations of the centroids AND the classification results will be marked */
		private DataImage actualImageBoth;
		/** the mapping from phenotype to result matrix (from the classification) */
		private HashMap<String,BoolMatrix> is;
		/** the mapping from phenotype to its centroid matrix where a positive coordinate represents one centroid */
		private HashMap<String,BoolMatrix> isCentroid;
		/** the number of rows in the image */
		private int rows;
		/** the number of columns in the image */
		private int cols;
		
		/** default constructor */
		public PostProcessor(String filename){
			this.filename=filename;
			is=new HashMap<String,BoolMatrix>(postProcessSet.size());
			isCentroid=new HashMap<String,BoolMatrix>(postProcessSet.size());			
		}
		
		private static final int ApproxNumCheckImages = 10;
		/**
		 * {@link ImageAndScoresBank#getOrAddDataImage(String) Loads the 
		 * image from the cache}, {@link DataImage#clone() duplicates} it
		 * to form the soon to be marked {@link #actualImageBoth "Both"} images, updates the {@link
		 * PostProcess#classifyPanel panel} and the {@link PostProcess#progress
		 * progress bar}, {@link PostProcess#PopulateIs(HashMap, String) loads the result matrices 
		 * from the hard disk}, find the centroids in the matrices using the 
		 * {@link CentroidFinder#EvaluateImages centroid finder},
		 * creates the processed images for debugging 
		 * (this can be disabled), the {@link PostProcess#AppendDataToCSVFiles(HashMap, String) 
		 * writes the results for each phenotype} and {@link PostProcess#AppendDataToSummary(HashMap, String)
		 * writes the results to the summary}. This process is described
		 * in more detail and in the context of the overall project in step 8
		 * of the Algorithms section in the IEEE paper. For more discussion, see section
		 * 4.2 of the manual. 
		 * 
		 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
		 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
		 */
		public void run(){
			progress.BeginImagePostProcessing(filename);
			classifyPanel.AddOrEditImageProgressBars(progress.getBox());
			classifyPanel.setDisplayImage(null);
//			DataImage actualImageFinal=ImageAndScoresBank.getOrAddDataImage(filename).clone();
			actualImageBoth = ImageAndScoresBank.getOrAddDataImage(filename).clone();
			rows=actualImageBoth.getWidth();
			cols=actualImageBoth.getHeight();
//			classifyPanel.setDisplayImage(ImageAndScoresBank.getOrAddDataImage(filename));
			classifyPanel.setIs(isCentroid);
			
			//all initial stuff is out of the way, begin doing the finding
			System.out.println("begin finding centroids in image "+filename+" . . . ");
			
			PopulateIs(is,filename);
			if (!stop.get()){
				isCentroid=centroidFinder.EvaluateImages(is, filename);
				RunExclusionRules();
				classifyPanel.setIs(isCentroid);
				classifyPanel.repaintImagePanel();
			}
			if (!stop.get()){
				if (new Random().nextDouble() <= ApproxNumCheckImages / (double)filenames.size()){
					CreateBothImage();
					IOTools.WriteImage(GetBothName(filename) + ".tiff", "TIFF", actualImageBoth);
				}
				for (String phenotype:postProcessSet)
					IOTools.WriteIsMatrix(GetIsCentroidName(filename,phenotype),isCentroid.get(phenotype));
				AppendDataToCSVFiles(isCentroid,filename);
				AppendDataToSummary(isCentroid,filename);
				progress.updatePostProcessLoad();
				
				//we're done
				System.out.println("done finding centroids in image "+filename);
			}
		}
		
		/**
		 * Exclusion rules do not allow certain phenotypes to be found in close proximity
		 * to other specified phenotypes. These options are set in the classify panel.
		 */
		private void RunExclusionRules() {
			ArrayList<ExclusionRuleStruct> rules = Run.it.imageset.getExclusion_rules();
			for (String phenotype_name : isCentroid.keySet()){
				for (ExclusionRuleStruct rule : rules){
					if (phenotype_name.equals(rule.if_phenotype)){
						BoolMatrix if_matrix = isCentroid.get(rule.if_phenotype);
						BoolMatrix exclude_matrix = isCentroid.get(rule.exclude_phenotype);
						exclude_matrix.ExcludeAllPositivesWithinRadius(if_matrix, rule.num_pixels);
					}
				}
			}
		}
		//		/**
//		 * {@link IOTools#WriteIsMatrix(String, BoolMatrix) Writes} the centroid 
//		 * matrix (the results of the post-processing) to the hard disk, then
//		 * {@link #MarkFinalPixels() marks the centroids} to create the {@link #actualImageFinal final image}
//		 * and {@link IOTools#WriteImage(String, String, DataImage) writes it to the 
//		 * hard disk}, then {@link #CreateBothImage() marks both the classification results and
//		 * the centroids} to create the {@link #actualImageBoth "both" image} and {@link IOTools#WriteImage(String, String, DataImage) 
//		 * writes it to the hard disk}		 
//		 */
//		private void CreateImageOutput() {			
//			for (String phenotype:postProcessSet)
//				IOTools.WriteIsMatrix(GetIsCentroidName(filename,phenotype),isCentroid.get(phenotype));
//		
////			MarkFinalPixels();
////			IOTools.WriteImage(GetFinalName(filename),"TIFF",actualImageFinal);
////		
////			CreateBothImage();
////			IOTools.WriteImage(GetBothName(filename),"TIFF",actualImageBoth);			
//		}
//		/**
//		 * For all phenotypes, {@link DataImage#MarkPhenoCentroid(int, int, java.awt.Color) 
//		 * mark a phenotype centroid} at its location on the {@link #actualImageFinal final image}
//		 */
//		private void MarkFinalPixels(){
//			for (int i=0;i<rows;i++){
//				for (int j=0;j<cols;j++){
//					HashMap<String,Boolean> isij=new HashMap<String,Boolean>(postProcessSet.size());
//					for (String phenotype:postProcessSet)
//						isij.put(phenotype,isCentroid.get(phenotype).get(i,j));
//					for (String name:postProcessSet)
//						if (isij.get(name))
//							actualImageFinal.MarkPhenoCentroid(i,j,Run.it.getPhenotypeDisplayColor(name));
//				}
//			}				
//		}
		/**
		 * For all phenotypes, {@link DataImage#MarkPhenoCentroid(int, int, java.awt.Color) 
		 * mark a phenotype centroid} at its location and for every pixel that it was classified,
		 */
		private void CreateBothImage() {
//			for (int i=0;i<rows;i++){
//				for (int j=0;j<cols;j++){
//						HashMap<String,Boolean> isij=new HashMap<String,Boolean>(postProcessSet.size());
//						for (String phenotype:postProcessSet)
//							isij.put(phenotype,is.get(phenotype).get(i,j));
//						for (String phenotype:postProcessSet)
//							if (isij.get(phenotype))
//								actualImageBoth.WaterMarkIs(i,j,phenotype);
//				}
//			}
			IOTools.WriteImage(GetBothName(filename) + ".tiff", "TIFF", actualImageBoth);
			try {
				actualImageBoth = new RegularSubImage(IOTools.OpenImage(GetBothName(filename) + ".tiff"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (int i=0;i<rows;i++){
				for (int j=0;j<cols;j++){
					HashMap<String,Boolean> isij=new HashMap<String,Boolean>(postProcessSet.size());
					for (String phenotype:postProcessSet)
						isij.put(phenotype,isCentroid.get(phenotype).get(i,j));
					for (String name:postProcessSet)
						if (isij.get(name))
							actualImageBoth.MarkPhenoCentroid(i,j,Run.it.getPhenotypeDisplayColor(name));
				}
			}			
		}
	}
	/** Prints the header for the summary file - the image name and names of phenotypes */
	private void PrintFileHeaderSummary(){ //this also deletes & recreates the file
		PrintWriter out=null;
		try {
			out=new PrintWriter(new BufferedWriter(new FileWriter(Run.it.imageset.getFilenameWithHomePath(outputDir+File.separator+Run.it.projectName+"-"+summary))));
		} catch (IOException e) {
			System.out.println(outputDir+File.separator+summary+" cannot be created");
		}
		out.print("filename");
		for (String phenotype:postProcessSet){
			out.print(",");
			out.print("Num_"+phenotype);
		}
		out.print("\n");
		out.close();		
	}
	/** Prints the file header for the file that records a given phenotype's coordinates */
	private void PrintFileHeader(String phenotype) {
		PrintWriter out=null;
		try {
			out=new PrintWriter(new BufferedWriter(new FileWriter(Run.it.imageset.getFilenameWithHomePath(outputDir+File.separator+GetOutputFilename(phenotype)))));
		} catch (IOException e) {
			System.out.println(outputDir+File.separator+GetOutputFilename(phenotype)+" cannot be created");
		}
		if (Run.it.imageset instanceof NonGlobalImageSet){
			out.print("filename,locX,locY");
		}
		else {
			out.print("filename,locX,locY,globalX,globalY");
		}		
		out.print("\n");
		out.close();
	}

	/**
	 * After an image has been post-processed, the coordinates of its centroids
	 * are {@link BoolMatrix#PrintPointsToFile(PrintWriter, String) recorded to a file}
	 * (for all phenotypes processed). The method is sychronized to avoid multiple 
	 * threads from writing to the file simultaneously and confusing the formatting
	 * 
	 * @param isCentroid		the mapping from phenotype to its centroid locations in a {@link BoolMatrix BoolMatrix}
	 * @param filename			the name of the image that was post-processed
	 */
	private synchronized void AppendDataToCSVFiles(HashMap<String,BoolMatrix> isCentroid,String filename){
		for (String phenotype:postProcessSet){
			PrintWriter out=null;
			try {
				out=new PrintWriter(new BufferedWriter(new FileWriter(Run.it.imageset.getFilenameWithHomePath(outputDir+File.separator+GetOutputFilename(phenotype)),true)));
			} catch (IOException e) {
				System.out.println(outputDir+File.separator+GetOutputFilename(phenotype)+" cannot be edited in CSV appending");
			}
			(isCentroid.get(phenotype)).PrintPointsToFile(out,filename);
			out.close();
		}
	}
	/**
	 * After an image has been post-processed, the {@link BoolMatrix#NumberPoints() number of centroids}
	 * are recorded to a file (for all phenotypes processed). The method is sychronized 
	 * to avoid multiple threads from writing to the file simultaneously and confusing the formatting
	 * 
	 * @param isCentroid		the mapping from phenotype to its centroid locations in a {@link BoolMatrix BoolMatrix}
	 * @param filename			the name of the image that was post-processed
	 */
	private synchronized void AppendDataToSummary(HashMap<String,BoolMatrix> isCentroid,String filename){

		PrintWriter out=null;
		try {
			out=new PrintWriter(new BufferedWriter(new FileWriter(Run.it.imageset.getFilenameWithHomePath(outputDir+File.separator+Run.it.projectName+"-"+summary),true)));
		} catch (IOException e) {
			System.out.println(outputDir+File.separator+summary+" cannot be edited in CSV appending");
		}
		out.print(filename);
		for (String phenotype:postProcessSet){
			out.print(",");
			long N=(isCentroid.get(phenotype)).NumberPoints();
			out.print(N);			
			totals.put(phenotype,totals.get(phenotype)+N);
		}
		out.print("\n");
		out.close();
	}
	/** after the subtotals for each image are recorded, the total counts for each phenotype is written in the {@link #summary summary file} */
	private void WriteTotalsToSummary(){
		PrintWriter out=null;
		try {
			out=new PrintWriter(new BufferedWriter(new FileWriter(Run.it.imageset.getFilenameWithHomePath(outputDir+File.separator+Run.it.projectName+"-"+summary),true)));
		} catch (IOException e) {
			System.out.println(outputDir+File.separator+summary+" cannot be created");
		}
		out.print("\n");
		out.print("Totals");
		for (String phenotype:postProcessSet){
			out.print(",");
			out.print(totals.get(phenotype)); 
		}
		out.print("\n");
		out.close();		
	}
	/** given a filename and a phenotype, get the centroid matrix file path relative to the project folder */
	public static String GetIsCentroidName(String filename, String phenotype){
		return Classify.processedDir+File.separator+Run.it.projectName+"-isC"+phenotype+IOTools.GetFilenameWithoutExtension(filename)+".bmp";
	}
	/** given a filename get the file path of the image marked with centroids relative to the project folder */
	public static String GetFinalName(String filename){
		return Classify.processedDir+File.separator+Run.it.projectName+"-f"+IOTools.GetFilenameWithoutExtension(filename)+".tif";
	}
	/** given a filename get the file path of the image marked with centroids and classification results relative to the project folder */
	public static String GetBothName(String filename){
		return ImageSetInterface.checkDir+File.separator+Run.it.projectName+"-b"+IOTools.GetFilenameWithoutExtension(filename);
	}
	/** given a phenotype get the file path of the coordinates text file relative to the project folder */
	public static String GetOutputFilename(String phenotype){
		return Run.it.projectName+"-"+phenotype+".txt";
	}
}