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

package GemIdentStatistics.CART;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentOperations.Run;
import GemIdentStatistics.ClassificationAndRegressionTree;
import GemIdentStatistics.RandomForest.RandomForest;
import GemIdentView.JProgressBarAndLabel;

/**
 * Creates a decision tree. This class was also expanded to confrom to 
 * the specifications of random forest trees.
 *
 * @author Adam Kapelner
 * 
 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm">Breiman's Random Forests (UC Berkeley)</a>
 */
public class DTree extends ClassificationAndRegressionTree implements Serializable {
	private static final long serialVersionUID = -3724306076596536141L;
	
	/** Instead of checking each index we'll skip every INDEX_SKIP indices unless there's less than MIN_SIZE_TO_CHECK_EACH*/
	private static final int INDEX_SKIP=3;
	/** If there's less than MIN_SIZE_TO_CHECK_EACH points, we'll check each one */
	private static final int MIN_SIZE_TO_CHECK_EACH=10;
	/** If the number of data points is less than MIN_NODE_SIZE, we won't continue splitting, we'll take the majority vote */
	private static final int MIN_NODE_SIZE=5;

	/** Of the testN, the number that were correctly identified */
	private transient int correct;
	/** an estimate of the importance of each attribute in the data record
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#varimp>Variable Importance</a>
	 */
	private transient int[] importances;
	/** This is a pointer to the Random Forest this decision tree *may* belong to */
	private transient RandomForest forest;
	/** the root node, override field in parent */
	private CARTTreeNode root;

	
	/** Serializable happy */
	public DTree(){}

	public double Evaluate(String string, int a, int b){return 0;}
	

	public DTree(DatumSetupForEntireRun datumSetupForEntireRun, JProgressBarAndLabel buildProgress){
		super(datumSetupForEntireRun, buildProgress);
	}
	
	/**
	 * convenience constructor for when it's called for the service of a random forest.
	 * This will wrap the standard constructor and then
	 * it will take care of the specialized building
	 * 
	 * @param raw_data	the raw training data
	 * @param forest	the random forest this decision tree belongs to
	 */
	public DTree(DatumSetupForEntireRun datumSetupForEntireRun, JProgressBarAndLabel buildProgress, ArrayList<double[]> raw_data, RandomForest forest) {
		super(datumSetupForEntireRun, buildProgress);
		super.setData(raw_data);
		this.forest = forest;
		BuildForRandomForestUsingBootstrapSample();
	}


	/**
	 * This constructs a decision tree from a data matrix.
	 * It first creates a bootstrap sample, the train data matrix, as well as the left out records, 
	 * the test data matrix. Then it creates the tree, then calculates the variable importances (not essential)
	 * and then removes the links to the actual data (to save memory)
	 */		
	protected void BuildForRandomForestUsingBootstrapSample(){
		importances = new int[p];
	
		ArrayList<double[]> train=new ArrayList<double[]>(n); //data becomes the "bootstrap" - that's all it knows
		ArrayList<double[]> test=new ArrayList<double[]>();
		
		BootStrapSample(X_y, train, test);
		correct=0;
		
		root=CreateTree(train);
		CalcTreeVariableImportanceAndError(test);
		FlushData();		
	}
	
	/** builds the decision tree */
	public void Build(){
		root = CreateTree(X_y);
		FlushData();
		buildProgress.setValue(100); //we're done
	}
	
	/**
	 * Responsible for gauging the error rate of this tree and 
	 * calculating the importance values of each attribute
	 * 
	 * @param test	The left out data matrix
	 */
	private void CalcTreeVariableImportanceAndError(ArrayList<double[]> test) {
		correct=CalcTreeErrorRate(test);
		
		for (int j=0; j < p; j++){
			ArrayList<double[]> data=RandomlyPermuteAttribute(CloneData(test), j);
			int correctAfterPermute=0;
			for (double[] arr:data){
				double prediction = Evaluate(arr);
				if (prediction == getResponseFromRecord(arr))
					correctAfterPermute++;
			}
			int diff = correct-correctAfterPermute;
			if (diff > 0)
				importances[j] += diff;
		}		
	}

	/**
	 * Calculates the tree error rate,
	 * displays the error rate to console,
	 * and updates the total forest error rate
	 * 
	 * @param test	the test data matrix
	 * @return	the number correct
	 */
	private int CalcTreeErrorRate(ArrayList<double[]> test){		
		int correct=0;		
		for (double[] record:test){
			double Class=Evaluate(record);
			forest.UpdateOOBEstimate(record, (int)Class);
			if (Class == getResponseFromRecord(record))
				correct++;
		}
		return correct;
	}
	/**
	 * Takes a list of data records, and switches the mth attribute across data records.
	 * This is important in order to test the importance of the attribute. If the attribute 
	 * is randomly permuted and the result of the classification is the same, the attribute is
	 * not important to the classification and vice versa.
	 * 
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#varimp">Variable Importance</a>
	 * @param test		The data matrix to be permuted
	 * @param m			The attribute index to be permuted
	 * @return			The data matrix with the mth column randomly permuted
	 */
	private ArrayList<double[]> RandomlyPermuteAttribute(ArrayList<double[]> test,int m){
		int num=test.size()*2;
		for (int i=0;i<num;i++){
			int a=(int)Math.floor(Math.random()*test.size());
			int b=(int)Math.floor(Math.random()*test.size());
			double[] arrA=test.get(a);
			double[] arrB=test.get(b);
			double temp=arrA[m];
			arrA[m]=arrB[m];
			arrB[m]=temp;
		}
		return test;
	}
	/**
	 * Creates a copy of the data matrix
	 * @param data		the data matrix to be copied
	 * @return			the cloned data matrix
	 */
	private ArrayList<double[]> CloneData(ArrayList<double[]> data){
		ArrayList<double[]> clone=new ArrayList<double[]>(data.size());
		int M=data.get(0).length;
		for (int i=0;i<data.size();i++){
			double[] arr=data.get(i);
			double[] arrClone=new double[M];
			for (int j=0;j<M;j++){
				arrClone[j]=arr[j];
			}
			clone.add(arrClone);
		}
		return clone;
	}
	/**
	 * This creates the decision tree according to the specifications of random forest trees. 
	 * 
	 * @see <a href="http://www.stat.berkeley.edu/~breiman/RandomForests/cc_home.htm#overview">Overview of random forest decision trees</a>
	 * @param train		the training data matrix (a bootstrap sample of the original data)
	 * @return			the TreeNode object that stores information about the parent node of the created tree
	 */
	private CARTTreeNode CreateTree(ArrayList<double[]> train){
		CARTTreeNode root = new CARTTreeNode(train);
		RecursiveSplit(root);
		return root;
	}

	/**
	 * Because Java never passes by reference, this object
	 * is a hack used in order to pass a double value around
	 * by reference.
	 * 
	 * @author Adam Kapelner
	 */
	private class DoubleWrap implements Serializable {
		private static final long serialVersionUID = -9017618157753329621L;
		private double d;
		
		@SuppressWarnings("unused")
		public DoubleWrap(){}
		public DoubleWrap(double d){
			this.d=d;
		}
		@SuppressWarnings("unused")
		public double getD() {
			return d;
		}
		@SuppressWarnings("unused")
		public void setD(double d) {
			this.d = d;
		}		
	}
	/**
	 * This is the crucial function in tree creation. 
	 * 
	 * <ul>
	 * <li>Step A
	 * Check if this node is a leaf, if so, it will mark isLeaf true
	 * and mark Class with the leaf's class. The function will not
	 * recurse past this point.
	 * </li>
	 * <li>Step B
	 * Create a left and right node and keep their references in
	 * this node's left and right fields. For debugging purposes,
	 * the generation number is also recorded. The {@link RandomForest#Ms Ms} attributes are
	 * now chosen by the {@link #GetVarsToInclude() GetVarsToInclude} function
	 * </li>
	 * <li>Step C
	 * For all Ms variables, first {@link #SortAtAttribute(List,int) sort} the data records by that attribute 
	 * , then look through the values from lowest to 
	 * highest. If value i is not equal to value i+1, record i in the list of "indicesToCheck."
	 * This speeds up the splitting. If the number of indices in indicesToCheck >  MIN_SIZE_TO_CHECK_EACH
	 * then we will only {@link #CheckPosition check} the
	 * entropy at every {@link #INDEX_SKIP INDEX_SKIP} index otherwise, we {@link #CheckPosition check}
	 * the entropy for all. The "E" variable records the entropy and we are trying to find the minimum in which to split on
	 * </li>
	 * <li>Step D
	 * The newly generated left and right nodes are now checked:
	 * If the node has only one record, we mark it as a leaf and set its class equal to
	 * the class of the record. If it has less than {@link #MIN_NODE_SIZE MIN_NODE_SIZE}
	 * records, then we mark it as a leaf and set its class equal to the {@link #GetMajorityKlass(List) majority class}.
	 * If it has more, then we do a manual check on its data records and if all have the same class, then it
	 * is marked as a leaf. If not, then we run {@link #RecursiveSplit RecursiveSplit} on
	 * that node
	 * </li>
	 * </ul>
	 * 
	 * @param parent	The node of the parent
	 */
	private void RecursiveSplit(CARTTreeNode parent){
		if (!parent.isLeaf){

			//-------------------------------Step A
			Double klass=CheckIfLeaf(parent.data);
			if (klass != null){
				parent.isLeaf=true;
				parent.klass=klass;
				return;
			}
			
			//-------------------------------Step B
			int Nsub=parent.data.size();
//			PrintOutClasses(parent.data);			
			
			parent.left=new CARTTreeNode();
//			parent.left.generation=parent.generation+1;
			parent.right=new CARTTreeNode();
//			parent.right.generation=parent.generation+1;
			
			ArrayList<Integer> vars=GetVarsToInclude();
			
			DoubleWrap lowestE=new DoubleWrap(Double.MAX_VALUE);

			//-------------------------------Step C
			for (int m:vars){
				
				SortAtAttribute(parent.data, m);
				
				ArrayList<Integer> indicesToCheck=new ArrayList<Integer>();
				for (int n=1;n<Nsub;n++){
					double classA=getResponseFromRecord(parent.data.get(n-1));
					double classB=getResponseFromRecord(parent.data.get(n));
					if (classA != classB)
						indicesToCheck.add(n);
				}
				
				if (indicesToCheck.size() == 0){
					parent.isLeaf=true;
					parent.klass=getResponseFromRecord(parent.data.get(0));
					continue;
				}
				if (indicesToCheck.size() > MIN_SIZE_TO_CHECK_EACH){
					for (int i=0;i<indicesToCheck.size();i+=INDEX_SKIP){
						CheckPosition(m,indicesToCheck.get(i),Nsub,lowestE,parent);
						if (lowestE.d == 0)
							break;
					}
				}
				else {
					for (int n:indicesToCheck){
						CheckPosition(m,n,Nsub,lowestE,parent);
						if (lowestE.d == 0)
							break;
					}
				}
				if (lowestE.d == 0)
					break;
			}

			//-------------------------------Step D
			if (parent.left.data.size() == 1){
				parent.left.isLeaf=true;
				parent.left.klass=getResponseFromRecord(parent.left.data.get(0));							
			}
			else if (parent.left.data.size() < MIN_NODE_SIZE){
				parent.left.isLeaf=true;
				parent.left.klass=GetMajorityKlass(parent.left.data);	
			}
			else {
				klass=CheckIfLeaf(parent.left.data);
				if (klass == null){
					parent.left.isLeaf=false;
					parent.left.klass=null;
				}
				else {
					parent.left.isLeaf=true;
					parent.left.klass=klass;
				}
			}
			if (parent.right.data.size() == 1){
				parent.right.isLeaf=true;
				parent.right.klass=getResponseFromRecord(parent.right.data.get(0));								
			}
			else if (parent.right.data.size() < MIN_NODE_SIZE){
				parent.right.isLeaf=true;
				parent.right.klass=GetMajorityKlass(parent.right.data);	
			}
			else {
				klass=CheckIfLeaf(parent.right.data);
				if (klass == null){
					parent.right.isLeaf=false;
					parent.right.klass=null;
				}
				else {
					parent.right.isLeaf=true;
					parent.right.klass=klass;
				}
			}
			
			if (!parent.left.isLeaf)
				RecursiveSplit(parent.left);
			if (!parent.right.isLeaf)
				RecursiveSplit(parent.right);
		}
	}
	
	/**
	 * Given a data matrix, return the most popular Y value (the class)
	 * @param data	The data matrix
	 * @return		The most popular class
	 */
	private double GetMajorityKlass(List<double[]> data){
		int[] counts=new int[Run.it.numPhenotypes()];
		for (double[] record:data){
			int Class=(int)getResponseFromRecord(record);
			counts[Class]++;
		}
		int index=-99;
		int max=Integer.MIN_VALUE;
		for (int i=0;i<counts.length;i++){
			if (counts[i] > max){
				max=counts[i];
				index=i;
			}				
		}
		return index;
	}

	/**
	 * Checks the {@link #CalcEntropy(double[]) entropy} of an index in a data matrix at a particular attribute (m)
	 * and returns the entropy. If the entropy is lower than the minimum to date (lowestE), it is set to the minimum.
	 * 
	 * The total entropy is calculated by getting the sub-entropy for below the split point and after the split point.
	 * The sub-entropy is calculated by first getting the {@link #GetClassProbs(List) proportion} of each of the classes
	 * in this sub-data matrix. Then the entropy is {@link #CalcEntropy(double[]) calculated}. The lower sub-entropy
	 * and upper sub-entropy are then weight averaged to obtain the total entropy. 
	 * 
	 * @param m				the attribute to split on
	 * @param n				the index to check
	 * @param Nsub			the number of records in the data matrix
	 * @param lowestE		the minimum entropy to date
	 * @param parent		the parent node
	 * @return				the entropy of this split
	 */
	private double CheckPosition(int m,int n,int Nsub,DoubleWrap lowestE,CARTTreeNode parent){
		if (n < 1) //exit conditions
			return 0;
		if (n > Nsub)
			return 0;
		
		List<double[]> lower = getLowerPortion(parent.data,n);
		List<double[]> upper = getUpperPortion(parent.data,n);
		double[] pl=GetClassProbs(lower);
		double[] pu=GetClassProbs(upper);
		double eL=CalcEntropy(pl);
		double eU=CalcEntropy(pu);
	
		double e=(eL*lower.size()+eU*upper.size())/((double)Nsub);
		if (e < lowestE.d){			
			lowestE.d=e;
			parent.splitAttributeM=m;
			parent.splitValue=parent.data.get(n)[m];		
			parent.left.data=lower;	
			parent.right.data=upper;
		}
		return e;
	}

	/**
	 * Given a data matrix, check if all the y values are the same. If so,
	 * return that y value, null if not
	 * 
	 * @param data		the data matrix
	 * @return			the common class (null if not common)
	 */
	private Double CheckIfLeaf(List<double[]> data){
		boolean isLeaf=true;
		double ClassA=getResponseFromRecord(data.get(0));
		for (int i=1;i<data.size();i++){			
			double[] recordB=data.get(i);
			if (ClassA != getResponseFromRecord(recordB)){
				isLeaf=false;
				break;
			}
		}
		if (isLeaf)
			return getResponseFromRecord(data.get(0));
		else
			return null;
	}

	/**
	 * Given a data matrix, return a probabilty mass function representing 
	 * the frequencies of a class in the matrix (the y values)
	 * 
	 * @param records		the data matrix to be examined
	 * @return				the probability mass function
	 */
	private double[] GetClassProbs(List<double[]> records){
		
		double N=records.size();
		
		int[] counts=new int[Run.it.numPhenotypes()];
		
		for (double[] record:records)
			counts[(int)getResponseFromRecord(record)]++;

		double[] ps=new double[Run.it.numPhenotypes()];
		for (int c=0;c<Run.it.numPhenotypes();c++)
			ps[c]=counts[c]/N;
		return ps;
	}
	/** ln(2) */
	private static final double logoftwo=Math.log(2);
	/**
	 * Given a probability mass function indicating the frequencies of 
	 * class representation, calculate an "entropy" value using the method
	 * in Tan Steinbach Kumar's "Data Mining" textbook
	 * 
	 * @param ps			the probability mass function
	 * @return				the entropy value calculated
	 */
	private double CalcEntropy(double[] ps){
		double e=0;		
		for (double p:ps){
			if (p != 0) //otherwise it will divide by zero - see TSK p159
				e+=p*Math.log(p)/logoftwo;
		}
		return -e; //according to TSK p158
	}
	/**
	 * Of the M attributes, select {@link RandomForest#Ms Ms} at random.
	 * If the DTree is being created independently of a random forest, it's all the variables
	 * 
	 * @return		The list of the Ms attributes' indices
	 */
	private ArrayList<Integer> GetVarsToInclude() {
		if (forest == null){ //vanilla DTree
			//return em all
			ArrayList<Integer> shortRecord=new ArrayList<Integer>(p);
 			for (int j=0;j<p;j++){
				shortRecord.add(j);
			}
			return shortRecord;			
		}
		else {
			boolean[] whichVarsToInclude=new boolean[p];
	
			for (int i=0;i<p;i++)
				whichVarsToInclude[i]=false;
			
			while (true){
				int a=(int)Math.floor(Math.random()*p);
				whichVarsToInclude[a]=true;
				int N=0;
				for (int i=0;i<p;i++)
					if (whichVarsToInclude[i])
						N++;
				if (N == forest.getMs())
					break;
			}
			
			ArrayList<Integer> shortRecord=new ArrayList<Integer>(forest.getMs());
			
			for (int i=0;i<p;i++){
				if (whichVarsToInclude[i]){
					shortRecord.add(i);
				}
			}
			return shortRecord;
		}
		
	}

	/**
	 * Create a boostrap sample of a data matrix
	 * 
	 * @param data		the data matrix to be sampled
	 * @param train		the bootstrap sample
	 * @param test		the records that are absent in the bootstrap sample
	 */
	private void BootStrapSample(ArrayList<double[]> data,ArrayList<double[]> train,ArrayList<double[]> test){
		ArrayList<Integer> indices=new ArrayList<Integer>(n);
		for (int i=0;i<n;i++)
			indices.add((int)Math.floor(Math.random()*i));
		ArrayList<Boolean> in=new ArrayList<Boolean>(n);
		for (int i=0;i<n;i++)
			in.add(false); //have to initialize it first
		for (int num:indices){
			train.add((data.get(num)).clone());
			in.set(num,true);
		}
		for (int i=0;i<n;i++)
			if (!in.get(i))
				test.add((data.get(i)).clone());
	}
	
//	// possible to clone trees
//	private DTree(){}
//	public DTree clone(){
//		DTree copy=new DTree();
//		copy.root=root.clone();
//		return copy;
//	}

	/**
	 * Get the importance level of attribute m for this tree
	 */
	public int getImportanceLevel(int m){
		return importances[m];
	}

	public CARTTreeNode getRoot() {
		return root;
	}

	public void setRoot(CARTTreeNode root) {
		this.root = root;
	}
	
	/**
	 * This will classify a new data record by using tree
	 * recursion and testing the relevant variable at each node.
	 * 
	 * This is probably the most-used function in all of <b>GemIdent</b>.
	 * It would make sense to inline this in assembly for optimal performance.
	 * 
	 * @param record 	the data record to be classified
	 * @return			the class the data record was classified into
	 */
	public double Evaluate(double[] record){ //localized RF - where it senses error rates locally (the piece of data you're giving it - the images) then it either creates a new RF in that place if need be. On new localities, it tries each RF to see which one has lowest error rate 
		CARTTreeNode evalNode = root;
		
		while (true){
			if (evalNode.isLeaf)
				return evalNode.klass;
			//all split rules are less than or equals (this is merely a convention)
			if (record[evalNode.splitAttributeM] <= evalNode.splitValue)
				evalNode=evalNode.left;
			else
				evalNode=evalNode.right;
		}
	}	
	
	//not going to write this function, not as useful
	public void StopBuilding() {}
	
	public void FlushData(){
		FlushNodeData(root);
	}
	
	/**
	 * Recursively deletes all data records from the tree. This is run after the tree
	 * has been computed and can stand alone to classify incoming data.
	 * 
	 * @param node		initially, the root node of the tree
	 */
	private void FlushNodeData(CARTTreeNode node){
		node.data=null;
		if (node.left != null)
			FlushNodeData(node.left);
		if (node.right != null)
			FlushNodeData(node.right);
	}
}