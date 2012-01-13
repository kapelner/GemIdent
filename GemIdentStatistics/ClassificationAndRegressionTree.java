package GemIdentStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentView.JProgressBarAndLabel;

public abstract class ClassificationAndRegressionTree extends Classifier {
	private static final long serialVersionUID = 3362976822356958024L;

	/** This is the root of the Decision Tree, the only thing that is 
	 * saved during serialization, you must override this field! */
	protected TreeNode root;
	
	/** serializable happy */
	public ClassificationAndRegressionTree(){}
	
	public ClassificationAndRegressionTree(DatumSetupForEntireRun datumSetupForEntireRun, JProgressBarAndLabel buildProgress){
		super(datumSetupForEntireRun, buildProgress);
	}
	
	/** deletes all records from the tree. Usually a wrapper for a recursive function */
	public abstract void FlushData();

	public void setRoot(TreeNode root) {
		this.root = root;
	}
	
	//convenience methods for all tree building...
	
	/**
	 * Sorts a data matrix by an attribute from lowest record to highest record
	 * 
	 * @param data			the data matrix to be sorted
	 * @param m				the attribute to sort on
	 */
	@SuppressWarnings("unchecked")
	public static void SortAtAttribute(List<double[]> data, int m){
		Collections.sort(data, new AttributeComparator(m));
	}
	
	
	/**
	 * This class compares two data records by numerically comparing a specified attribute
	 * 
	 * @author Adam Kapelner
	 */
	@SuppressWarnings("rawtypes")
	private static class AttributeComparator implements Comparator {
		
		/** the specified attribute */
		private int m;
		/**
		 * Create a new comparator
		 * @param m			the attribute in which to compare on
		 */
		public AttributeComparator(int m){
			this.m = m;
		}
		/**
		 * Compare the two data records. They must be of type int[].
		 * 
		 * @param o1		data record A
		 * @param o2		data record B
		 * @return			-1 if A[m] < B[m], 1 if A[m] > B[m], 0 if equal
		 */
		public int compare(Object o1, Object o2){
			double a = ((double[])o1)[m];
			double b = ((double[])o2)[m];
			if (a < b)
				return -1;
			if (a > b)
				return 1;
			else
				return 0;
		}		
	}
	
	public static int getSplitPoint(List<double[]> data, int splitAttribute, double splitValue){
		//if we started with no data, the split point should just be the zero index for consistency
		if (data.isEmpty()){
			return 0;
		}
		for (int i = 0; i < data.size(); i++){
			if (data.get(i)[splitAttribute] > splitValue){
				return i;
			}
		}
		return data.size() - 1; //all the data is less than this split point
	}
	
	/**
	 * Split a data matrix and return the upper portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records above this index in a sub-data matrix
	 * @return			the upper sub-data matrix
	 */
	public static List<double[]> getUpperPortion(List<double[]> data,int nSplit){
		int N=data.size();
		List<double[]> upper=new ArrayList<double[]>(N-nSplit);
		for (int n=nSplit;n<N;n++)
			upper.add(data.get(n));
		return upper;
	}
	
	/**
	 * Split a data matrix and return the lower portion
	 * 
	 * @param data		the data matrix to be split
	 * @param nSplit	return all data records below this index in a sub-data matrix
	 * @return			the lower sub-data matrix
	 */
	public static List<double[]> getLowerPortion(List<double[]> data,int nSplit){
		List<double[]> lower=new ArrayList<double[]>(nSplit);
		for (int n=0;n<nSplit;n++)
			lower.add(data.get(n));
		return lower;
	}
}
