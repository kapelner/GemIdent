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
import java.util.List;

import GemIdentStatistics.TreeNode;


/**
 * A dumb struct to store information about a 
 * node in the decision tree.
 * 
 * @author Adam Kapelner
 */
public class CARTTreeNode extends TreeNode implements Cloneable, Serializable {
	private static final long serialVersionUID = 8632232129710601363L;
	
	/** the left daughter node */
	public CARTTreeNode left;
	/** the right daughter node */
	public CARTTreeNode right;
	
//	/** the generation of this node from the top node (only useful for debugging */
//	public int generation;
	
	/** initializes default values */
	public CARTTreeNode(){
		splitAttributeM=-99;
		splitValue = -99.0;
//		generation=1;
	}
	public CARTTreeNode(List<double[]> data){
		this();
		this.data = data;
	}
	
	/** clones this node */
	public CARTTreeNode clone(){ //"data" element always null in clone
		CARTTreeNode copy=new CARTTreeNode();
		copy.isLeaf=isLeaf;
		if (left != null) //otherwise null
			copy.left=left.clone();
		if (right != null) //otherwise null
			copy.right=right.clone();
		copy.splitAttributeM=splitAttributeM;
		copy.klass=klass;
		copy.splitValue=splitValue;
		return copy;
	}
//	public int getGeneration() {
//		return generation;
//	}
//	public void setGeneration(int generation) {
//		this.generation = generation;
//	}
	
	//override the getters to prevent annoying casting problems
	public CARTTreeNode getLeft() {
		return (CARTTreeNode)left;
	}
	public CARTTreeNode getRight() {
		return (CARTTreeNode)right;
	}	

}