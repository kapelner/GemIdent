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

package GemIdentStatistics.RandomForest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentOperations.Run;
import GemIdentStatistics.Classifier;
import GemIdentStatistics.StatToolbox;
//import GemIdentStatistics.VisualizeClassifierImportances;
import GemIdentStatistics.CART.DTree;
import GemIdentTools.Matrices.ShortMatrix;
import GemIdentView.JProgressBarAndLabel;

/**
 * Houses a Breiman Random Forest
 * 
 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm">Breiman's Random Forests (UC Berkeley)</a>
 * 
 * @author Adam Kapelner
 */
public class RandomForest extends Classifier implements Serializable {
	private static final long serialVersionUID = -8073610839472157131L;
	
	private static final int DEFAULT_NUM_TREES = 200;
	
	/** the collection of the forest's decision trees */
	private ArrayList<DTree> trees;
	/** the number of trees in this random tree */
	private int numTrees;
	/** this is an array whose indices represent the forest-wide importance for that given attribute */
	private int[] importances;
	/** the error rate of this forest */
	private double error;
	
	/** This maps from a data record to an array that records the classifications by the trees where it was a "left out" record (the indices are the class and the values are the counts) */
	private transient HashMap<double[],int[]> estimateOOB;
	/** For progress bar display for the creation of this random forest, this records the total progress */
	private transient double progress;
	/** Of the M total attributes, the random forest computation requires a subset of them
	 * to be used and picked via random selection. "Ms" is the number of attributes in this
	 * subset. The formula used to generate Ms was recommended on Breiman's website.
	 */	
	private transient int Ms;
	/** should we halt the construction of this random forest? */
	private transient boolean stop;
	
	/** Serializable is happy */
	public RandomForest(){}

	public double Evaluate(String string, int a, int b){ return 0;}

	public RandomForest(DatumSetupForEntireRun datumSetupForEntireRun, JProgressBarAndLabel buildProgress){
		super(datumSetupForEntireRun, buildProgress);
		numTrees = DEFAULT_NUM_TREES;
	}
	
	public RandomForest(DatumSetupForEntireRun datumSetupForEntireRun, JProgressBarAndLabel buildProgress, int numTrees){
		super(datumSetupForEntireRun, buildProgress);
		this.numTrees = numTrees;
	}
	/**
	 * Initializes a Breiman random forest creation. This process is described
	 * in more detail and in the context of the overall project in step 6
	 * of the Algorithms section in the IEEE paper.
	 * 
	 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
	 */
	public void Build() {
		//init some variables
		Ms = (int)Math.round(Math.log(p) / Math.log(2) + 1);
		trees = new ArrayList<DTree>(numTrees);
		final double update = 100 / ((double)numTrees);
		progress = 0;
		estimateOOB = new HashMap<double[],int[]>(n);		
		ExecutorService treePool = Executors.newFixedThreadPool(1);//Run.it.NUM_THREADS);
		//go to town
		final RandomForest that = this; //gotta love java
		for (int t = 0; t < numTrees; t++){
			treePool.execute(new Runnable(){
				public void run(){
					if (!stop){
						trees.add(new DTree(datumSetupForEntireRun, buildProgress, X_y, that));
						buildProgress.setValue((int)Math.round(progress += update));
					}
				}
			});
		}
		treePool.shutdown();
		try {	         
			treePool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	    
	    CalcImportances();
	    CalcErrorRate();
	    try {
//	    	new VisualizeClassifierImportances(importances, error, datumSetupForEntireRun).SpawnWindow();
	    } catch (Exception e){
	    	e.printStackTrace();
	    }	    
	}
	/**
	 * This calculates the forest-wide error rate. For each "left out" 
	 * data record, if the class with the maximum count is equal to its actual
	 * class, then increment the number of correct. One minus the number correct 
	 * over the total number is the error rate.
	 */
	private void CalcErrorRate(){
		double N=0;
		int correct=0;
		for (double[] record : estimateOOB.keySet()){
			N++;
			int[] map=estimateOOB.get(record);
			int Class = StatToolbox.FindMaxIndex(map);
			if (Class == getResponseFromRecord(record))
				correct++;
		}
		error = 1-correct/N;
	}
	/**
	 * Update the error map by recording a class prediction 
	 * for a given data record
	 * 
	 * @param record	the data record classified
	 * @param Class		the class
	 */
	public void UpdateOOBEstimate(double[] record,int Class){
		if (estimateOOB.get(record) == null){
			int[] map=new int[Run.it.numPhenotypes()];
			map[Class]++;
			estimateOOB.put(record,map);
		}
		else {
			int[] map=estimateOOB.get(record);
			map[Class]++;
		}
	}
	/**
	 * This calculates the forest-wide importance levels for all attributes.
	 * 
	 * @return 		the array of importances per each attribute
	 */
	private int[] CalcImportances() {
		importances=new int[p];
		for (DTree tree:trees){
			for (int i=0;i<p;i++)
				importances[i]+=tree.getImportanceLevel(i);
		}
		for (int i=0;i<p;i++)
			importances[i]/=numTrees;
		return importances;
	}
	
//	private void PrintImportanceLevels(int[] importances) {
//		System.out.print("importances:\n");
//		ArrayList<String> names=new ArrayList<String>(Run.it.imageset.getDatumSetup().NumberOfFeatures());
//		for (String filter:Run.it.imageset.getFilterNames())
//			for (int r=0;r<=Datum.R;r++)
//				names.add(filter + "_" + r + ",");
//		
//		for (int i=0;i<Run.it.imageset.getDatumSetup().NumberOfFeatures();i++)
//			System.out.println(names.get(i)+importances[i]);
//	}
	
	/**
	 * Evaluates an incoming data record.
	 * It first allows all the decision trees to classify the record,
	 * then it returns the majority vote
	 * 
	 * @param record		the data record to be classified
	 */
	public double Evaluate(double[] record){
//		System.out.println("RF evaluate num phen: " + Run.it.numPhenotypes() + " numtrees: " + numTrees);
		int[] counts=new int[Run.it.numPhenotypes()];
		for (int t=0;t<numTrees;t++){
			double Class=(trees.get(t)).Evaluate(record);
			counts[(int)Class]++;
//			for (int count : counts){
//				System.out.print(count + ",");
//			}
//			System.out.print("\n");
		}
		return StatToolbox.FindMaxIndex(counts);		
	}
	
	private static final int MaxConfusionValue = 255;
	/**
	 * Evaluates an incoming data record.
	 * It first allows all the decision trees to classify the record,
	 * then it returns the majority vote
	 * 
	 * @param record		the data record to be classified
	 * @param j 
	 * @param i 
	 * @param confusion_matrix 
	 */	
	public int Evaluate(double[] record, ShortMatrix confusion_matrix, Integer i, Integer j){
		int[] counts=new int[Run.it.numPhenotypes()];
		for (int t=0;t<numTrees;t++){
			counts[(int)(trees.get(t)).Evaluate(record)]++;
		}
		int result = StatToolbox.FindMaxIndex(counts);
		
		//now handle confusion in the forest's decision:
		int confusion = 0;
		for (int c = 0; c < Run.it.numPhenotypes(); c++){
			if (c != result){
				confusion += counts[c];
			}
		}
		confusion_matrix.set(i, j, (short)Math.floor(confusion / ((double) numTrees) * MaxConfusionValue));
		
		return result;
	}	


//	//ability to clone forests
//	private RandomForest(ArrayList<DTree> trees,int numTrees){
//		this.trees=trees;
//		this.numTrees=numTrees;
//	}
//	public RandomForest clone(){
//		ArrayList<DTree> copy=new ArrayList<DTree>(numTrees);
//		for (DTree tree:trees)
//			copy.add(tree.clone());
//		return new RandomForest(copy,numTrees);
//	}

	public ArrayList<DTree> getTrees() {
		return trees;
	}

	public void setTrees(ArrayList<DTree> trees) {
		this.trees = trees;
	}

	public int getNumTrees() {
		return numTrees;
	}

	public void setNumTrees(int numTrees) {
		this.numTrees = numTrees;
	}

	public int[] getImportances() {
		return importances;
	}

	public void setImportances(int[] importances) {
		this.importances = importances;
	}

	public double getError() {
		return error;
	}

	public void setError(double error) {
		this.error = error;
	}

	//nothing
	protected void FlushData() {}


	public int getMs() {
		return Ms;
	}
}