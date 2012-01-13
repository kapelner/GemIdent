package GemIdentStatistics;

import java.io.Serializable;
import java.util.List;

public abstract class TreeNode implements Serializable {
	private static final long serialVersionUID = 2565814909812529390L;
	
	/** is this node a terminal leaf? */
	public boolean isLeaf;
	/** the left daughter node */
	public TreeNode left;
	/** the right daughter node */
	public TreeNode right;
	/** the attribute this node makes a decision on */
	public Integer splitAttributeM;
	/** the value this node makes a decision on */
	public Double splitValue;
	/** if this is a leaf node, then the result of the classification, otherwise null */
	public Double klass;
	/** if this is a leaf node, then the result of the prediction for regression, otherwise null */
	public Double y_prediction;
	/** the remaining data records at this point in the tree construction (freed after tree is constructed) */
	public transient List<double[]> data;
	
	public double[] get_ys_in_data(){
		double[] ys = new double[data.size()];
		for (int i = 0; i < data.size(); i++){
			double[] record = data.get(i);
			ys[i] = record[record.length - 1];
		}
		return ys;
	}
	

	public boolean isLeaf() {
		return isLeaf;
	}
	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}
	public TreeNode getLeft() {
		return left;
	}
	public void setLeft(TreeNode left) {
		this.left = left;
	}
	public TreeNode getRight() {
		return right;
	}
	public void setRight(TreeNode right) {
		this.right = right;
	}
	public int getSplitAttributeM() {
		return splitAttributeM;
	}
	public void setSplitAttributeM(int splitAttributeM) {
		this.splitAttributeM = splitAttributeM;
	}
	public double getSplitValue() {
		return splitValue;
	}
	public void setSplitValue(double splitValue) {
		this.splitValue = splitValue;
	}
	public Double getKlass() {
		return klass;
	}
	public void setKlass(Double klass) {
		this.klass = klass;
	}	
	
	public abstract TreeNode clone();
	
	public String stringID() {
		return toString().split("@")[1];
	}
	
	//left in for debug purposes:
	public static void PrintOutNode(TreeNode parent, String init){
		try {
			System.out.println(init+"node: left"+parent.left.toString());
		} catch (Exception e){
			System.out.println(init+"node: left null");
		}
		try {
			System.out.println(init+" right:"+parent.right.toString());
		} catch (Exception e){
			System.out.println(init+"node: right null");
		}
		try {
			System.out.println(init+" isleaf:"+parent.isLeaf);
		} catch (Exception e){}
		try {
			System.out.println(init+" splitAtrr:"+parent.splitAttributeM);
		} catch (Exception e){}
		try {
			System.out.println(init+" splitval:"+parent.splitValue);
		} catch (Exception e){}
		try {
			System.out.println(init+" class:"+parent.klass);
		} catch (Exception e){}
//		try {
//			System.out.println(init+" data size:"+parent.data.size());
//			PrintOutClasses(parent.data);
//		} catch (Exception e){
//			System.out.println(init+" data: null");
//		}		
	}
//	private void PrintOutClasses(List<int[]> data){
//		try {
//			System.out.print(" (n="+data.size()+") ");
//			for (int[] record:data)
//				System.out.print(GetKlass(record));
//			System.out.print("\n");		
//		}
//		catch (Exception e){
//			System.out.println("PrintOutClasses: data null");	
//		}
//	}
//	public static void PrintBoolArray(boolean[] b) {
//		System.out.print("vars to include: ");
//		for (int i=0;i<b.length;i++)
//			if (b[i])
//				System.out.print(i+" ");
//		System.out.print("\n\n");		
//	}
//
//	public static void PrintIntArray(List<int[]> lower) {
//		System.out.println("tree");
//		for (int i=0;i<lower.size();i++){
//			int[] record=lower.get(i);
//			for (int j=0;j<record.length;j++){
//				System.out.print(record[j]+" ");
//			}
//			System.out.print("\n");
//		}
//		System.out.print("\n");
//		System.out.print("\n");
//	}	

}
