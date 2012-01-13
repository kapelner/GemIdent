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

package GemIdentClassificationEngine;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentModel.Phenotype;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentView.JProgressBarAndLabel;

/**
 * Responsible for generating the training data from the user's
 * training set. For every training point the user enters, a
 * {@link Datum#Datum Datum} object is created
 * and stored in the {@link #allData data matrix}
 * 
 * @author Adam Kapelner
 */
public class TrainingData {
	
	/** the thread pool responsible for {@link TrainingData.TrainingDataMaker generating the training data from individual images} */
	private ExecutorService trainPool;
	/** the progress bar that keeps track of the training data creation */
	private JProgressBarAndLabel trainingProgress;
	/** the progress bar will be incremented this amount after each image's training data is created */
	private double increment;
	/** the total progress thus far */
	private double totalvalue;
	
	/** the data matrix as a list of {@link Datum Datum} objects */
	private ArrayList<Datum> allData;
	/** whether or not to stop the training data creation */
	private boolean stop;
	/** the number of threads to utilize when constructing the training data */
	private int nthreads;
	private DatumSetupForEntireRun datumSetupForEntireRun;

	/**
	 * Initializes shared objects, then generates the data from the training set
	 * 
	 * @param nthreads				the number of threads to use when generating the data
	 * @param trainingProgress		the progress bar that keeps track of the progress
	 */
	public TrainingData(DatumSetupForEntireRun datumSetupForEntireRun, int nthreads,JProgressBarAndLabel trainingProgress){
		this.datumSetupForEntireRun = datumSetupForEntireRun;
		this.trainingProgress=trainingProgress;
		this.nthreads=nthreads;
		increment=100 / ((double)Run.it.numPhenTrainingPoints());
		totalvalue=0;
		stop=false;	
		allData=new ArrayList<Datum>();
		//go for it:
		GenerateData();
	}
	/** Initializes the thread pool and adds a {@link TrainingDataMaker TrainingDataMaker} 
	 * for each image to it. */
	private void GenerateData() {
		trainPool=Executors.newFixedThreadPool(nthreads);
		for (String filename:Run.it.getPhenotypeTrainingImages()){
			trainPool.execute(new TrainingDataMaker(filename));
		}
		trainPool.shutdown();
		try {
	        trainPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	}

	/** shuts down the training data creation */
	public void Stop() {
		trainPool.shutdownNow();
		stop=true;
	}
	/** Framework for building training data in one image and can be threaded in a thread pool */
	private class TrainingDataMaker implements Runnable{
		
		/** the filename of the image whose training data is being created */
		private String filename;
		/** the object that contains common information for each Datum */
		private DatumSetupForImage datumSetupForImage;		

		/** default constructor */
		public TrainingDataMaker(String filename){
			this.filename = filename;
			datumSetupForImage = new DatumSetupForImage(datumSetupForEntireRun, filename);
		}	
		/**
		 * For all phenotypes, if the user has trained in this image, then
		 * for all training points, create a {@link Datum Datum}
		 * for each point within {@link GemIdentModel.TrainSuperclass#rmin rmin}
		 * of the user's point and set its class value equal to the phenotype. If
		 * the user decides not to look for this phenotype (by clicking the
		 * {@link GemIdentView.KPhenotypeInfo#findPixels findPixels} checkbox),
		 * then set the Datum's class value to zero, that of the {@link 
		 * GemIdentModel.Phenotype#NON_NAME NON} phenotype. For a more formal
		 * description, see step 5c in the Algorithm section of the IEEE paper.
		 * 
		 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
		 */
		public void run(){
			for (Phenotype phenotype:Run.it.getPhenotypeObjects()){
				if (phenotype.hasImage(filename)){
					for (Point to:phenotype.getPointsInImage(filename)){	
						if (stop)
							return;
						String name=phenotype.getName();
						int Class = 0;
						if (phenotype.isFindPixels()){
							Class=Run.classMapFwd.get(name);
						}
						
						for (Point t : Solids.GetPointsInSolidUsingCenter(phenotype.getRmin(), to)){
							Datum d = datumSetupForImage.generateDatumAtPoint(t);
							d.setClass(Class);
							allData.add(d);
						}
						//update progress bar
						trainingProgress.setValue((int)Math.round(totalvalue += increment));
					}
				}
			}
		}	
	}
	/**
	 * Gets the data matrix as a list of int arrays convenient for 
	 * learning in the {@link GemIdentStatistics.RandomForest
	 * Random Forest machine learning classifier}
	 * 
	 * @return		the {@link #allData allData list} as a list of int[]'s
	 */
	public ArrayList<double[]> getData(){
		ArrayList<double[]> allData=new ArrayList<double[]>((this.allData).size());
		for (Datum d : this.allData)
			allData.add(d.getRecord());
		return allData;
	}
	/**
	 * For each training image, and each phenotype, this function creates a
	 * {@link GemIdentTools.Matrices.BoolMatrix BoolMatrix} object where
	 * the positive coordinates represent the user's training points.
	 * 
	 * @param allTrainingPoints		the mapping from a training image to a mapping from phenotype to matrix whose positives are the user's training points
	 * @param postProcessSet		the phenotypes to be included (those whose centroids will be found via post-processing)
	 */
	public static void GetAllTrainingPoints(HashMap<String,HashMap<String,BoolMatrix>> allTrainingPoints,Set<String> postProcessSet){
		for (String filename:Run.it.getPhenotypeTrainingImages()){
			DataImage I=ImageAndScoresBank.getOrAddDataImage(filename);
			HashMap<String,BoolMatrix> isTrainingPoint=new HashMap<String,BoolMatrix>(postProcessSet.size());
			for (String name:postProcessSet){
				Phenotype phenotype=Run.it.getPhenotype(name);
				if (phenotype.hasImage(filename)){
					BoolMatrix B=new BoolMatrix(phenotype.getPointsInImage(filename),I.getWidth(),I.getHeight());
					isTrainingPoint.put(phenotype.getName(),B);
				}
			}
			allTrainingPoints.put(filename,isTrainingPoint);
		}
	}
}