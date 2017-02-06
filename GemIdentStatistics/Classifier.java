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

package GemIdentStatistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentTools.LoggingOutputStream;
import GemIdentTools.StdOutErrLevel;
import GemIdentView.JProgressBarAndLabel;

/**
 * The base class for all machine learning / statistical-learning
 * algorithms. Extend this class to add your own implementation.
 * 
 * Note this implementation is only for instances where y is categorical
 * and the design matrix $\X \in \mathbb{N}^{n \times p}$, i.e. the design
 * matrix is all integer values
 * 
 * @author Adam Kapelner
 */
public abstract class Classifier implements Serializable {
	private static final long serialVersionUID = -2857913059676679308L;	

	/** the raw training data consisting of xi = [xi1,...,xiM, yi] that will be used to construct the classifier */
	protected transient ArrayList<double[]> X_y;
	/** just the responses */
	protected transient double[] y;
	protected transient double[] y_trans;
//	/** just the design matrix */
//	protected transient ArrayList<double[]> X;	
	/** the number of records in the training set */
	protected int n;
	/** the number of features / predictors in the training set */
	protected int p;
	/** the progress bar that gets updated as the classifier is being built */
	protected transient JProgressBarAndLabel buildProgress;
	/** this is the shared information about the datums */
	protected DatumSetupForEntireRun datumSetupForEntireRun;
	
	protected static PrintWriter output;
	protected static final String DEBUG_EXT = ".csv";

	
	/** Serializable happy */
	public Classifier(){}

	public Classifier(DatumSetupForEntireRun datumSetupForEntireRun, JProgressBarAndLabel buildProgress){
		this.datumSetupForEntireRun = datumSetupForEntireRun;
		p = datumSetupForEntireRun.numFeatures();

		System.out.println("classifier has " + p + " total features");
		this.buildProgress = buildProgress;
	}
	
	/** 
	 * adds the data to the classifier - 
	 * data is always a list of int[]'s - call this 
	 * before calling {@link #Build() Build()} 
	 * 
	 * WARNING: It is up to the user to supply raw data with
	 * at least one record, and each record to be in the form
	 * x_i1, x_i2, ..., x_ip, yi
	 * 
	 */
	public void setData(ArrayList<double[]> X_y){
		this.X_y = X_y;
		n = X_y.size();
		y = extractResponseFromRawData(X_y);
		y_trans = new double[y.length];
//		for (int i = 0; i < n; i++){
//			System.out.println("i:" + i + " yi:" + y[i]);
//		}
		transformResponseVariable();
//		X = extractDesignMatrixFromRawData(X_y);
	}
	
//	private ArrayList<double[]> extractDesignMatrixFromRawData(ArrayList<double[]> X_y) {
//		ArrayList<double[]> X = new ArrayList<double[]>(n);
//		for (int i = 0; i < n; i++){
//			double[] x = new double[p];
//			for (int j = 0; j < p; j++){
//				x[j] = X_y.get(i)[j];				
//			}
//			X.add(x);
//		}
//		return X;
//	}

	private double[] extractResponseFromRawData(ArrayList<double[]> X_y) {
		double[] y = new double[X_y.size()];
		for (int i = 0; i < X_y.size(); i++){
			double[] record = X_y.get(i);
			y[i] = record[record.length - 1];
		}
		return y;
	}
	
	public ArrayList<double[]> getData() {
		return X_y;
	}	
	
	/** build the machine learning classifier, you must {@link #setData(ArrayList) set the data} first */
	public abstract void Build();
	
	public void StartBuilding(){
		new Thread(){
			public void run(){
				Build();
			}
		}.start();
	}
	
	public static void writeToDebugLog(){
		//also handle the logging
        LogManager logManager = LogManager.getLogManager();
        logManager.reset();

        // create log file, no limit on size
        FileHandler fileHandler = null;
		try {
			fileHandler = new FileHandler("log", Integer.MAX_VALUE, 1, false);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        fileHandler.setFormatter(new SimpleFormatter());
        Logger.getLogger("").addHandler(fileHandler);
        
        // now rebind stdout/stderr to logger
        Logger logger = Logger.getLogger("stdout");         
        LoggingOutputStream  los = new LoggingOutputStream(logger, StdOutErrLevel.STDOUT);
        System.setOut(new PrintStream(los, true));
        logger = Logger.getLogger("stderr");                                    
        los= new LoggingOutputStream(logger, StdOutErrLevel.STDERR);            
        System.setErr(new PrintStream(los, true)); 		
	}
	
	/** deletes all data that's unneeded to save memory */
	protected abstract void FlushData();
	
	/** After the classifier has been built, new records can be evaluated */
	public abstract double Evaluate(double[] record);
	
	/**
	 * Given a data record, return the Y value - take the last index
	 * 
	 * @param record		the data record
	 * @return				its y value (class)
	 */
	public double getResponseFromRecord(double[] record){
		return record[p];
	}

	/** Stop the classifier in its building phase */
	public abstract void StopBuilding();

	public int getP() {
		return p;
	}
	
	public int getN() {
		return n;
	}	
	
	//useful for debugging
	public void dumpDataToFile(String optional_title){
		PrintWriter out=null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter("data_out" + (optional_title == null ? "" : optional_title) + ".csv")));
		} catch (IOException e) {
			System.out.println("cannot be edited in CSV appending");
		}
		
		//print fileheader
		for (int j = 0; j < p; j++){
			out.print("," + datumSetupForEntireRun.getFeatureName(j));
		}
		out.print(",y");
		out.print("\n");
		//now print the data
		for (int i = 0; i < n; i++){
			double[] record = X_y.get(i);
			for (int j = 0; j <= p; j++){
				out.print("," + record[j]);
			}
			out.print("\n");
		}
		out.close();		
	}
	
	public static enum ErrorTypes {L1, L2, MISCLASSIFICATION};
	/**
	 * Calculates the in-sample error using the specified loss function
	 * @param type_of_error_rate  the loss function 
	 * @return the error rate
	 */	
	public double calculateInSampleLoss(ErrorTypes type_of_error_rate){		
		double loss = 0;

		for (int i=0; i<n; i++){
			double[] record = X_y.get(i);
			double y = getResponseFromRecord(record);
			double yhat = Evaluate(record);
//			System.out.println("y: " + y + " yhat: " + yhat);

			// now add the appropriate quantity to the loss
			switch (type_of_error_rate){
				case L1:
					loss += Math.abs(y - yhat);
					break;
				case L2:
					loss += Math.pow(y - yhat, 2);
					break;
				case MISCLASSIFICATION:
					loss += (yhat == y ? 0 : 1);
					break;
			}
		}
		return loss;
	}
	
	protected void transformResponseVariable() {
		//default is to do nothing... ie just copy the y's into y_trans's
		for (int i = 0; i < n; i++){
			y_trans[i] = y[i];
		}		
	}	
	
	protected double un_transform_y(double y_i) {
		//default:
		return y_i;
	}

	public double calculateMisclassificationRate(){
		return calculateInSampleLoss(ErrorTypes.MISCLASSIFICATION) / (double) n * 100;
	}
	
	public double CalculateCrossValidationError(ErrorTypes type_of_error_rate){
		//TODO
		return 0;
	}
	
	public double calculateLeaveOneOutLoss(ErrorTypes type_of_error_rate){
		double loss = 0;
		for (int i = 0; i < n; i++){
			//TODO
		}
		return loss;
	}
	
	public Classifier clone(){
		return null;
	}
	
	
	public void writeEvaluationDiagnostics() {		
		output.print("y,yhat");
		output.print("\n");
		for (int i=0; i<n; i++){
			double[] record = X_y.get(i);
			double y = getResponseFromRecord(record); //the original response from record does not have to be untransformed
			double yhat = Evaluate(record);
			output.println(y + "," + yhat);
		}		
		output.close();
	}
}
