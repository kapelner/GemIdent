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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import GemIdentCentroidFinding.PostProcess.BooleanFlag;
import GemIdentClassificationEngine.TrainingData;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.Matrices.DoubleMatrix;
import GemIdentView.KClassifyPanel;

/**
 * After classification, the blobs must be resolved into centroids.
 * This abstract class contains common functionality to turn a classified
 * binary matrix into a collection of blobs. Extend this class with your
 * method to resolve the blobs into centroids
 * 
 * @author Adam Kapelner
 */
public abstract class CentroidFinder {
	
	/** Each centroid-finding method gets its own constant */
	public static final int LabelViaSmartErosionsMethod = 100;
	
	/** whether or not the rules were created and LabelViaSmartErosions can proceed in finding centroids in evaluating a new image */
	protected boolean canProcess;
	/** has the user pressed stop? if so, the processing will be terminated */
	protected BooleanFlag stop;
	
	
	/** if the phenotype does not have this number of separate, unique blobs that the user identified, LabelViaSmartErosions will not attempt to create rules */
	private static final int MIN_NUM_DISCRETE_BLOBS_NEEDED = 8;
	/** the master mapping from image name to the mapping between phenotypes and its result matrix */
	private HashMap<String,HashMap<String,BoolMatrix>> allTrainingPoints;
	/** the mapping from phenotype to the list of distributions where each entry is the distribution for a given erosion */
	protected HashMap<String,ArrayList<UnivariateDistribution>> uniDistributions;


	
	/** a dumb struct that stores some statistics about a 1-d list of blob sizes */
	public static class UnivariateDistribution{

		/** blobs of size below this percentile in the distribution will be ignored */
		private static final double CUTOFF_PERCENTILE_BOTTOM=.02;


		/** blobs of size above this percentile in the distribution will be further split */
		private static final double CUTOFF_PERCENTILE_TOP=.95;

		/** blobs of size above this percentile in the distribution may be further split */
		private static final double CUTOFF_PERCENTILE_MID=.4;
		/** the median of the blob sizes */
		public double median;
		/** the blob size that corresponds to the {@link #CUTOFF_PERCENTILE_TOP top percentile cutoff} */
		public int cutoffTop;
		/** the blob size that corresponds to the {@link #CUTOFF_PERCENTILE_BOTTOM bottom percentile cutoff} */
		public int cutoffBottom;
		/** the blob size that corresponds to the {@link #CUTOFF_PERCENTILE_MID mid percentile cutoff} */
		public int cutoffMid;
		
		/** Creates univariate statistics from a list of blob sizes */
		public UnivariateDistribution(ArrayList<Integer> nPixelList){
			int n=nPixelList.size();
			Collections.sort(nPixelList);
//			min=nPixelList.get(0);
//			max=nPixelList.get(n-1);
			median=nPixelList.get((int)Math.round((n-1)/((double)2)));
//			seventyFivePercentile=nPixelList.get((int)Math.round((n-1)*.75));
			cutoffTop=nPixelList.get((int)Math.round((n-1)*CUTOFF_PERCENTILE_TOP));
			cutoffBottom=nPixelList.get((int)Math.round((n-1)*CUTOFF_PERCENTILE_BOTTOM));
			cutoffMid=nPixelList.get((int)Math.round((n-1)*CUTOFF_PERCENTILE_MID));
		}
	}
	
	/**
	 * Given a list of {@link CentroidFinder.MiniDatum MiniDatums}, extract
	 * their size information and return that as a list
	 * 
	 * @param datums		the distribution
	 * @return				the number of pixels in the blobs distribution
	 */
	private ArrayList<Integer> getNPixelList(ArrayList<MiniDatum> datums){
		ArrayList<Integer> list=new ArrayList<Integer>(datums.size());
		for (MiniDatum d:datums)
			list.add(d.nPixels);
		return list;			
	}
	
	/** dumb struct that stores information about one blob */
	protected class MiniDatum {
		
		/** the size (in number of pixels) of the blob */
		public int nPixels;
		/** the exact center (as a coordinate) of the blob */
		public DoubleMatrix center;
		
		/** rounds the center to the nearest integer x and y value */
		public Point getCenterAsDiscretePoint(){
			return RoundDoublePoint(center); 
		}
	}
	
	/**
	 * Creates a heuristic classifier that could find centroids from matrices of binary blobs.
	 * If successful, new images can be {@link #EvaluateImages evaluated}. If unsuccessful,
	 * the user must return to training to get more examples. After completion, the main data structure,
	 * the "erosionMapping" is freed (not an ivar)
	 * 
	 * @param allTrainingIs		the mapping from image to its map from phenotype to result matrix
	 * @param postProcessSet	the set of phenotypes that the user wants to find the centroids of
	 * @param classifyPanel		the panel where the post-processing is taking place
	 * @param stop 				the flag to tell the labeller to stop in its tracks
	 */	
	public CentroidFinder(HashMap<String,HashMap<String,BoolMatrix>> allTrainingIs, Set<String> postProcessSet, KClassifyPanel classifyPanel, BooleanFlag stop){
		this.stop = stop;
		canProcess = true;		
		
		allTrainingPoints=new HashMap<String,HashMap<String,BoolMatrix>>();
		TrainingData.GetAllTrainingPoints(allTrainingPoints,postProcessSet);
		
		HashMap<String,ArrayList<ArrayList<ArrayList<Point>>>> erosionMapping=CreateErosionMapping(allTrainingIs,postProcessSet);
		if (erosionMapping == null){
			classifyPanel.StopProcessing();
			return;
		}
		CreateDistributions(CreateTrainingData(erosionMapping));

	}
	
	/** Given a a point whose coordinates are doubles, return the coordinates rounded to the nearest integer */
	public static Point RoundDoublePoint(DoubleMatrix center) {
		return new Point((int)Math.round(center.get(0,0)),(int)Math.round(center.get(1,0)));
	}
	/** has the classifier been constructed without error? can we now {@link #EvaluateImages evaluate} new images? */
	public boolean canProcess(){		
		return canProcess;
	}

	/** Framework for creating a classifier for a given phenotype and can be threaded in a thread pool */
	private class HeuristicClassifier implements Runnable {
		
		/** generate a classifier for this phenotype's blobs */
		private String phenotype;
		/** the mapping from image to its map from phenotypes to their classification results */
		private HashMap<String,HashMap<String,BoolMatrix>> allTrainingIs;
		/** the erosion mapping from phenotype to the erosions list - a list of blobs (where blobs themselves are lists of coordinates) for each erosion 0,...E */
		private HashMap<String,ArrayList<ArrayList<ArrayList<Point>>>> erosionMapping;
		
		/** default constructor */
		public HeuristicClassifier(String phenotype,HashMap<String,ArrayList<ArrayList<ArrayList<Point>>>> erosionMapping,HashMap<String,HashMap<String, BoolMatrix>> allTrainingIs) {
			this.phenotype=phenotype;
			this.erosionMapping=erosionMapping;
			this.allTrainingIs=allTrainingIs;
		}
		/**
		 * This populates the {@link #erosionMapping erosion mapping} for one phenotype with
		 * an "erosion list". First it initializes  the erosion list and stores it in the erosion mapping. Then, it cycles through
		 * the training images's result matrices for this phenotype and pulls out each individual, contiguous blobs
		 * using a {@link #FloodfillLabelPoints floodfill algorithm}. Then it tries
		 * to {@link #ReconcileOnePointInBlob reconcile} each training point
		 * that the user made with an interior point in the blob (ie the classification "found" that object
		 * that the user selected - a true positive). Then it stores these true positive blobs in the zeroth erosion
		 * position in the erosion list. Then, it erodes all the result matrices and repeats the process of
		 * blob finding via floodfill and reconciliation with the same training points. It stores the true positive blobs
		 * in the first erosion position in the erosion list. The erosions continue until every point in the result
		 * matrix is completely eroded away. This process is described in more detail and in the context of the overall 
		 * project in step 8 of the Algorithms section under the header "Simple Blob Analysis" in the IEEE paper.
		 * 
		 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a> 
		 * @see <a href="http://en.wikipedia.org/wiki/Floodfill">Wikipedia - the floodfill algorithm</a>
		 */
		public void run(){			
			ArrayList<ArrayList<ArrayList<Point>>> masterReconciledAllErosionsPerPhenotype=new ArrayList<ArrayList<ArrayList<Point>>>();
			synchronized(erosionMapping){
				erosionMapping.put(phenotype,masterReconciledAllErosionsPerPhenotype);
			}
			for (int e=0;;e++){
				if (stop.get())
					return;
				ArrayList<ArrayList<Point>> masterReconciledPerPhenotypePerErosion=new ArrayList<ArrayList<Point>>();
				for (String filename:Run.it.getPhenotypeTrainingImages()){			
					BoolMatrix Bclassified=allTrainingIs.get(filename).get(phenotype).clone();
					for (int i=0;i<e;i++)
						Bclassified=Bclassified.Erode(); //whittle it down
					long num=Bclassified.NumberPoints();
					if (num > 0){
						BoolMatrix Btraining=allTrainingPoints.get(filename).get(phenotype);
						if (Bclassified != null && Btraining != null)
							masterReconciledPerPhenotypePerErosion.addAll(ReconcileOnePointInBlob(FloodfillLabelPoints(Bclassified, null, null, stop),Btraining));
						masterReconciledAllErosionsPerPhenotype.add(e,masterReconciledPerPhenotypePerErosion);
					}
				}
				if (masterReconciledPerPhenotypePerErosion.size() == 0)
					break;
			}
		}
	}
	
	/**
	 * Initializes the master erosion mapping. Then for each phenotype that <b>GemIdent</b> post-processes,
	 * it threads the creation of their individual {@link HeuristicClassifier 
	 * heuristic classifiers} using a {@link java.util.concurrent.Executors#newFixedThreadPool(int)
	 * thread pool}. After the classifiers are built, the function checks if the
	 * classifiers are okay for {@link #EvaluateImages evaluation of new images} by 
	 * checking whether or not the first erosion list (see {@link HeuristicClassifier#run() 
	 * classifier creation}) has a {@link #MIN_NUM_DISCRETE_BLOBS_NEEDED minimum number of blobs}. If the 
	 * any phenotype's classifier does not pass the test, a dialog box tells the user (for all that failed) and
	 * the master erosion mapping (see function internals) is set to null. This will indicate to
	 * stop processing and disallow the user from continuing.
	 * 
	 * @param allTrainingIs 			the mapping from image to its map from phenotype to result matrix
	 * @return							the master erosion mapping from phenotype to erosion list of blob lists (where each list is the coordinates in the blob)
	 */
	private HashMap<String,ArrayList<ArrayList<ArrayList<Point>>>> CreateErosionMapping(HashMap<String,HashMap<String,BoolMatrix>> allTrainingIs,Set<String> postProcessSet){
		HashMap<String,ArrayList<ArrayList<ArrayList<Point>>>> erosionMapping=new HashMap<String,ArrayList<ArrayList<ArrayList<Point>>>>();
		
		ExecutorService heuristicPool = Executors.newFixedThreadPool(postProcessSet.size()); //finish more evenly who cares about overhead
		for (String phenotype:postProcessSet)
			heuristicPool.execute(new HeuristicClassifier(phenotype,erosionMapping,allTrainingIs));
		heuristicPool.shutdown();
		try {	         
			heuristicPool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
		
	    if (stop.get())
	    	return erosionMapping;
	    
		//now ensure that there are sufficient points in each:
		ArrayList<String> insufficient = new ArrayList<String>();
		ArrayList<String> to_remove = new ArrayList<String>();
		for (String phenotype:erosionMapping.keySet()){
			ArrayList<ArrayList<Point>> list=erosionMapping.get(phenotype).get(0);
			if (list.size() < MIN_NUM_DISCRETE_BLOBS_NEEDED){
				System.out.println("found " + list.size() + " blobs for phenotype " + phenotype + " (need 10)");
				insufficient.add(phenotype);
				to_remove.add(phenotype);
			}
		}	
		for (String removed_phenotype : to_remove){
			erosionMapping.remove(removed_phenotype);
		}
		if (insufficient.size() > 0){
			canProcess=false;
			for (String phenotype:insufficient)
				JOptionPane.showMessageDialog(Run.it.getGUI(),"The \""+phenotype+"\" phenotype does not have enough separate blobs in order to find centroids.\n" +
						"It is possible that not all the trained images were classified. If so, reclassify trained.\n" +
						"Otherwise, retrain this phenotype and ensure the selection of discrete examples");
			return null;
		}

		else
			return erosionMapping;
	}
	/**
	 * Assuming the {@link #CreateErosionMapping(HashMap, Set) construction} of the 
	 * erosion mapping was successful, information (in
	 * the form of a {@link CentroidFinder.MiniDatum MiniDatum} can
	 * be {@link #CreateMiniDatumFromBlob(ArrayList) computed about each blob} 
	 * for each phenotype in each of their erosions. The returned distribution mapping
	 * is from phenotype to erosion list. The erosion list's elements are {@link
	 * CentroidFinder.MiniDatum MiniDatums} - one for each of the blobs.
	 * 
	 * @param erosionMapping	the erosion mapping
	 * @return					the distribution mapping or "training data"
	 */
	private HashMap<String,ArrayList<ArrayList<MiniDatum>>> CreateTrainingData(HashMap<String,ArrayList<ArrayList<ArrayList<Point>>>> erosionMapping){
		HashMap<String,ArrayList<ArrayList<MiniDatum>>> trainingData=new HashMap<String,ArrayList<ArrayList<MiniDatum>>>();
		synchronized (erosionMapping){
			for (String phenotype:erosionMapping.keySet()){
				ArrayList<ArrayList<MiniDatum>> datumsAllErosions=new ArrayList<ArrayList<MiniDatum>>();
				for (int e=0;e<erosionMapping.get(phenotype).size();e++){
					ArrayList<MiniDatum> datums=new ArrayList<MiniDatum>();
					for (ArrayList<Point> blob:erosionMapping.get(phenotype).get(e))
						datums.add(CreateMiniDatumFromBlob(blob));
					datumsAllErosions.add(datums);				
				}
				trainingData.put(phenotype,datumsAllErosions);
			}
		}
		return trainingData;
	}
	/**
	 * Given the {@link #CreateTrainingData(HashMap) training data}, {@link 
	 * CentroidFinder.UnivariateDistribution statistics} can be computed 
	 * for each of the phenotype's erosions. The statistics will
	 * be used when deciding the heuristic rules.
	 *  
	 * @param trainingData		the mapping from phenotype to erosion list - a list of MiniDatum information about the blobs
	 */
	private void CreateDistributions(HashMap<String,ArrayList<ArrayList<MiniDatum>>> trainingData){
//		multiDistributions=new HashMap<String,MultiVariateNormalDistribution>();
		uniDistributions=new HashMap<String,ArrayList<UnivariateDistribution>>();
		for (String phenotype:trainingData.keySet()){
			ArrayList<UnivariateDistribution> distributionsAllErosions=new ArrayList<UnivariateDistribution>();
			for (int e=0;e<trainingData.get(phenotype).size();e++){
				
				ArrayList<MiniDatum> datums=trainingData.get(phenotype).get(e);
				if (datums.size() > 0)
					distributionsAllErosions.add(new UnivariateDistribution(getNPixelList(datums)));
			}
			uniDistributions.put(phenotype,distributionsAllErosions);
		}
		
	}
	

	/**
	 * Given a blob - a collection of coordinates of its pixels, create
	 * a {@link MiniDatum MiniDatum} struct that stores
	 * the number of pixels and its center.
	 * 
	 * @param blob		the blob to investigate
	 * @return			the blob's information in a {@link MiniDatum MiniDatum} struct
	 */
	private MiniDatum CreateMiniDatumFromBlob(ArrayList<Point> blob){
		MiniDatum d=new MiniDatum();
		d.nPixels=blob.size();
//		d.eigenvalueRatio=GetEigenValueRatioFromBlob(blob);
//		d.perimeterPixelsOverTotal=GetPerimeterPixelsFromBlob(blob).size()/((double)blob.size());
		d.center=GetCenterOfBlob(blob);
		return d;
	}	
	
	/**
	 * Given a bunch of coordinates, calculate their center
	 * 
	 * @param blob		the bunch of coordinates
	 * @return			the center as a 2x1 double matrix
	 */
	public static DoubleMatrix GetCenterOfBlob(ArrayList<Point> blob){
		DoubleMatrix u=new DoubleMatrix(2,1);
		
		long ux=0;
		long uy=0;
		
		for (Point t:blob){
			ux+=t.x;
			uy+=t.y;
		}
		
		double dux=ux/((double)blob.size());
		double duy=uy/((double)blob.size());
		
		u.set(0,0,dux);
		u.set(1,0,duy);
		
		return u;
	}
	

	/**
	 * The user has trained for the existence of objects. Of those training points,
	 * <b>GemIdent</b> may have found a blob corresponding to that object - a true positive. 
	 * Search through all blobs to see if the user trained for any of the points 
	 * contained within it. If only one point was found in it, then save the blob.
	 * Return those saves as a list.
	 * 
	 * @param blobs			the list of blobs - bunches of coordinates
	 * @param Btraining		the matrix of points where a positive represents a user's training point
	 * @return				the true positives - blobs where the user trained one point for
	 */
	private ArrayList<ArrayList<Point>> ReconcileOnePointInBlob(ArrayList<ArrayList<Point>> blobs,BoolMatrix Btraining){
		ArrayList<ArrayList<Point>> reconciled=new ArrayList<ArrayList<Point>>();		
		for (ArrayList<Point> blob:blobs){
			int N=0;
			for (Point t:blob)
				if (Btraining.get(t))
					N++;
			if (N == 1)
				reconciled.add(blob);
		}		
		return reconciled;
	}
	
	/**
	 * Given a result matrix use the {@link #RecursiveFloodfillLabel
	 * floodfill algorithm} to located the individual contiguous blobs. Save the coordinates
	 * of the blobs in {@link java.awt.Point Point} objects so a blob becomes an
	 * {@link java.util.ArrayList ArrayList} of Points. 
	 * 
	 * It's static because I use it in the console for a function
	 * 
	 * @param B			the result matrix to investigate
	 * @param phenotype the phenotype being labeled currently 
	 * @param filename  the image being floodfill labeled currently 
	 * @return			a list of the individual, contiguous blobs
	 */
	public static ArrayList<ArrayList<Point>> FloodfillLabelPoints(final BoolMatrix B, final String phenotype, final String filename, final BooleanFlag stop){
		ArrayList<ArrayList<Point>> blobs=new ArrayList<ArrayList<Point>>();
		final BoolMatrix D=new BoolMatrix(B.getWidth(),B.getHeight());
		pictureiterator: {
			for (int i=0;i<B.getWidth();i++){
				for (int j=0;j<B.getHeight();j++){				
					if (B.get(i,j) && !D.get(i,j)){
						if (stop != null && stop.get()){
							return blobs; //get out immediately
						}
						final ArrayList<Point> blob=new ArrayList<Point>();
						try {
							RecursiveFloodfillLabel(i,j,B,D,blob,stop);		
						}	
						catch (StackOverflowError stackoverflowerror){
							new Thread(){
								public void run(){
									if (phenotype == null && filename == null)
										return;
									String message = "Stack overflow error while trying to find centroids for " + phenotype + " in image " + filename + ".\n\nAre you sure you want to find centroids for all those phenotypes you selected?\nSome may not have centroids or have blobs that are too large.\n\nIf you still think GemIdent is in error, restart the program and beef up the Xss and Xoss parameters\n(see the section 2 of the manual).";
									JOptionPane.showMessageDialog(Run.it.getGUI(), message);
								}
							}.start();
							stackoverflowerror.printStackTrace();
							break pictureiterator;
						}					
						blobs.add(blob);
					}				
				}
			}
		}
		return blobs;
	}
	/**
	 * The floodfill algorithm - given a point, if it is true, record in the master 
	 * "done matrix" (D), then try to run the algorithm on all surrounding points.
	 * Eventually, the "done matrix" will become the entire contiguous blob
	 * 
	 * @param i			the x-coordinate of the point being investigated
	 * @param j			the y-coordinate of the point being investigated
	 * @param B			the matrix of blobs being investigated (the input image)
	 * @param D			the matrix of points already investigated
	 * @param blob		the set of points belonging to the blob being investigated
	 */
	private static void RecursiveFloodfillLabel(int i,int j,BoolMatrix B,BoolMatrix D,ArrayList<Point> blob, BooleanFlag stop){
		if (stop != null){
			if (stop.get()){
				return;
			}
		}		
		if (B.get(i,j) && !D.get(i,j)){
			D.set(i,j,true);
			blob.add(new Point(i,j));
			RecursiveFloodfillLabel(i-1,j-1,B,D,blob,stop);
			RecursiveFloodfillLabel(i,j-1,B,D,blob,stop);
			RecursiveFloodfillLabel(i-1,j,B,D,blob,stop);
			RecursiveFloodfillLabel(i-1,j+1,B,D,blob,stop);
			RecursiveFloodfillLabel(i+1,j-1,B,D,blob,stop);
			RecursiveFloodfillLabel(i+1,j+1,B,D,blob,stop);
			RecursiveFloodfillLabel(i+1,j,B,D,blob,stop);
			RecursiveFloodfillLabel(i,j+1,B,D,blob,stop);
		}
	}	
	
	/**
	 * After the classifiers are created (see {@link #CentroidFinder
	 * the constructor}), new binary images are evaluated to find centroids.
	 *  
	 * @param is			the mapping from phenotype to input image (the binary blob images)
	 * @param filename		the image being evaluated currently
	 * @return				the mapping from phenotype to output image (the post-processed binary centroid images)
	 */
	public HashMap<String,BoolMatrix> EvaluateImages(HashMap<String,BoolMatrix> is, String filename){
		HashMap<String,BoolMatrix> isCentroid=new HashMap<String,BoolMatrix>();
		for (String phenotype:Run.it.getPhenotyeNamesSaveNONAndFindCenters()){
			isCentroid.put(phenotype,LabelNewImage(phenotype, is.get(phenotype), filename));
		}
		return isCentroid;
	}




	
	/** if the blob "passes" and is labelled as one centroid */
	private static final int PASS=1;
	/** the blob was too small and "fails" and remains unlabelled */
	private static final int FAIL=0;
	/** the blob neither passes or fails immediately and must be split */
	private static final int SPLIT=2;
	/**
	 * Given an input binary image, it will post-process to find the
	 * centroids. First it will {@link #FloodfillLabelPoints
	 * find the contiguous blobs}, then it will {@link #CreateMiniDatumFromBlob(ArrayList)
	 * create a MiniDatum} for each blob. Then it will try and "classify" each blob
	 * by {@link #RunRule running its rule}
	 * 
	 * @param phenotype		this image represents this classified phenotype
	 * @param I				the input image (where every positive point belongs to one blob)
	 * @param filename 		the image being post processed currently
	 * @return				the output image (where every positive point is one centroid)
	 */
	private BoolMatrix LabelNewImage(String phenotype, BoolMatrix I, String filename) {
		ArrayList<ArrayList<Point>> blobList=FloodfillLabelPoints(I, phenotype, filename, stop);
		BoolMatrix centroids=new BoolMatrix(I.getWidth(),I.getHeight()); //the results
		for (ArrayList<Point> blob:blobList){
			if (!stop.get()){
				MiniDatum d=CreateMiniDatumFromBlob(blob);
	//			WriteDatum(d,phenotype);
				switch (RunRule(d,uniDistributions.get(phenotype).get(0))){
					case PASS: 
						centroids.set(d.getCenterAsDiscretePoint(),true);
						break;
					case SPLIT:
						//here's where the whole abstract class branches:
						GetEstimatedCentersFromSplitBlob(uniDistributions.get(phenotype).get(0).median,centroids,blob,d,phenotype, filename);
						break;
				}
			}
		}
		return centroids;
	}
	
	
	/**
	 * Given a blob to be split, this function determines the number
	 * of centroids contained within, then marks the centroids accordingly
	 * 
	 * @param median		the median number of pixels in blobs of this phenotype in the zeroth erosion
	 * @param centroids		the final output that stores the location of centroids for this phenotype
	 * @param blob			the collection of coordinates that represents the blob to be marked in several places
	 * @param d				the {@link MiniDatum MiniDatum} representation of the blob of interest		
	 * @param phenotype		the phenotype of interest		
	 * @param filename		the image being classified right now  
	 */	
	protected abstract void GetEstimatedCentersFromSplitBlob(double median, BoolMatrix centroids, ArrayList<Point> blob, MiniDatum d, String phenotype, String filename);

	/**
	 * Given a blob and the number of centers, mark each centroid
	 * 
	 * @param blob			the blob to be marked multiple times
	 * @param numCenters	the number of times to mark a center in the blob
	 * @param phenotype		the phenotype this blob is a representative of
	 * @param filename 		the image being classified currently
	 * @return	 			the list of coordinates (size numCenters) that represent the centroid locations in this large blob
	 */	
	protected abstract ArrayList<Point> GetEstimatedCenters(ArrayList<Point> blob,int numCenters,String phenotype, String filename);

	/**
	 * Given a blob and given the distribution classifier (see {@link
	 * #CreateDistributions(HashMap) CreateDistributions}) for this 
	 * phenotype, ignore it if its size is less then the {@link UnivariateDistribution#cutoffBottom
	 * bottom cutoff}, call it one centroid (ie "pass it") if it's
	 * between the {@link UnivariateDistribution#cutoffBottom
	 * bottom cutoff} and the {@link UnivariateDistribution#cutoffTop
	 * top cutoff}, and  designate it to be split into multiple centroids if it's
	 * larger than the {@link UnivariateDistribution#cutoffTop
	 * top cutoff}
	 * 
	 * @param d			the blob to be investiaged
	 * @param dist		the distribution of blobs for the given phenotype than includes heuristic thresholds for use in classification
	 * @return			{@link #PASS pass}, {@link #SPLIT split}, or {@link #FAIL fail}
	 */
	private int RunRule(MiniDatum d,UnivariateDistribution dist){
		if (d.nPixels < dist.cutoffBottom)
			return FAIL;
		else if (d.nPixels > dist.cutoffTop)
			return SPLIT;
		else
			return PASS;
	}	
	
	/** a struct that conveniently holds information about a single blob */
	protected class BlobStruct{
		
		/** the blob as a {@link BoolMatrix BoolMatrix} object */
		public BoolMatrix blobAsMatrix;
		/** the list of coordinates belonging to this blob */
		public ArrayList<Point> blobAsPoints;
		/** the minimum x-coordinate of this blob */
		public int minX;
		/** the minimum y-coordinate of this blob */
		public int minY;
		/** the msximum x-coordinate of this blob */
		public int maxX;
		/** the msximum y-coordinate of this blob */
		public int maxY;
		
		/** create a properly sized {@link BoolMatrix BoolMatrix} for this blob */
		public void CreateBlobAsMatrix(){
			blobAsMatrix=new BoolMatrix(maxX-minX,maxY-minY);
		}
		/** Get the center (rounded to the nearest integer coordinate) */
		public Point GetTrueCenter(ArrayList<Point> blob){
			Point c=RoundDoublePoint(GetCenterOfBlob(blob));
			return GetTrueCenter(c);
		}
		/** Get a random coordinate from this blob */
		public Point getRandPointFromBoolMatrix() {
			int r=(int)Math.floor(Math.random()*blobAsMatrix.NumberPoints());
			Point c=blobAsMatrix.getRthPt(r);
			return GetTrueCenter(c);
		}
		/** Get the true center in the context of the whole image */
		protected Point GetTrueCenter(Point c){
			return new Point(c.x+minX,c.y+minY);
		}
		/** Get the true center in the context of the whole image */
		public Point GetTrueCenter(DoubleMatrix localCenter) {
			int x=(int)Math.round(localCenter.get(0,0))+minX;
			int y=(int)Math.round(localCenter.get(1,0))+minY;
			return new Point(x,y);
		}
		/** set a point in the matrix representation (makes sure to adjust for the proper bounds) */
		public void setPoint(Point t){
			blobAsMatrix.set(t.x-minX,t.y-minY,true);
		}
		/** erode the blob using N4 erosion */
		public void Erode(){
			blobAsMatrix=blobAsMatrix.Erode();
		}
		/** gets the number of pixels in this blob */
		public long getPointsInBoolMatrix(){
			return blobAsMatrix.NumberPoints();
		}
	}
	
	/**
	 * Constructs a {@link BlobStruct BlobStruct} from a traditional blob 
	 * as a list of coordinates
	 * 
	 * @param blob		the blob to construct a {@link BlobStruct BlobStruct} of
	 * @return			the blob as a {@link BlobStruct BlobStruct}
	 */
	protected BlobStruct BlobAsMatrix(ArrayList<Point> blob) {
		
		BlobStruct blobStruct=new BlobStruct();
		blobStruct.blobAsPoints=blob;
		blobStruct.minX=Integer.MAX_VALUE;
		blobStruct.minY=Integer.MAX_VALUE;
		blobStruct.maxX=0;
		blobStruct.maxY=0;
		for (Point t:blob){
			if (t.x < blobStruct.minX)
				blobStruct.minX=t.x;
			if (t.x > blobStruct.maxX)
				blobStruct.maxX=t.x;
			if (t.y < blobStruct.minY)
				blobStruct.minY=t.y;
			if (t.y > blobStruct.maxY)
				blobStruct.maxY=t.y;				
		}

		blobStruct.CreateBlobAsMatrix();
		for (Point t:blob)
			blobStruct.setPoint(t);
		return blobStruct;
	}	
}
