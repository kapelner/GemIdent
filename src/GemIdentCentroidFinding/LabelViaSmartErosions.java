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
import java.util.HashMap;
import java.util.Set;

import GemIdentCentroidFinding.PostProcess.BooleanFlag;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentView.KClassifyPanel;


/**
 * The class that creates a heuristic classifier for each
 * phenotype of interest using training data. When evaluating new images,
 * it will find centroids from the result matrix of binary blobs. 
 * Many functions are commented out - lots
 * of things were tried that didn't pan out. I leave the 
 * commented functions in because they may get used in the future
 * 
 * @author Adam Kapelner
 */
public class LabelViaSmartErosions extends CentroidFinder {
	
	/** Simple wrapper around {@link CentroidFinder#CentroidFinder the super class's constructor} */
	public LabelViaSmartErosions(HashMap<String,HashMap<String,BoolMatrix>> allTrainingIs,Set<String> postProcessSet,KClassifyPanel classifyPanel, BooleanFlag stop){
		super(allTrainingIs, postProcessSet, classifyPanel, stop);
	}

	
	/** median yields too many centers, use this constant to control this */
	private static final double BumpUpMedianConstant = 1.0;

	/**
	 * Implementation of {@link CentroidFinder#GetEstimatedCentersFromSplitBlob(double, BoolMatrix, ArrayList, GemIdentCentroidFinding.CentroidFinder.MiniDatum, String, String)
	 * the abstract function} via dividing the size of the blob my the median modified by a constant,
	 * then if there's only one center, marking it, otherwise, splitting it further
	 */
	protected void GetEstimatedCentersFromSplitBlob(double median, BoolMatrix centroids, ArrayList<Point> blob, MiniDatum d, String phenotype, String filename){		
		int numCenters=(int)Math.floor(blob.size() / (median * BumpUpMedianConstant));
		if (numCenters == 1){
			centroids.set(d.getCenterAsDiscretePoint(),true);
		}
		else 
			for (Point c : GetEstimatedCenters(blob, numCenters, phenotype, filename))
				centroids.set(c,true);
	}
	/**
	 * Given a large blob and the number of centers to mark, try a number
	 * of ways to mark it:
	 * <p>
	 * 1 - {@link #BlobAsMatrix(ArrayList) Convert the blob into a matrix}
	 * then erode the blob down. If the blob breaks into numcenters
	 * subblobs, mark the centers of each. Ideally this would happen 
	 * for all blobs, but if not, move on.
	 * </p>
	 * <p>
	 * 2 - (currently not being used) 
	 * </p>
	 * <p>
	 * 3 - {@link #BlobAsMatrix(ArrayList) Convert the blob into a matrix}
	 * then create a square mask (solid) that divides the blob up evenly into
	 * numCenters. Start carving away at the blob, mark the centers of each
	 * of these carvings.
	 * </p>
	 * 
	 * @param blob			the blob to be marked multiple times
	 * @param numCenters	the number of times to mark a center in the blob
	 * @param phenotype		the phenotype this blob is a representative of
	 * @param filename 		the image being classified currently
	 * @return	 			the list of coordinates (size numCenters) that represent the centroid locations in this large blob
	 */
	protected ArrayList<Point> GetEstimatedCenters(ArrayList<Point> blob,int numCenters,String phenotype, String filename){
		
		BlobStruct b;
		
		b=BlobAsMatrix(blob);

		for (int e=0;e<Integer.MAX_VALUE;e++){
			b.Erode();
			
			if (b.getPointsInBoolMatrix() == 0)
				break;
			
			ArrayList<ArrayList<Point>> blobsAfterAnErosion=FloodfillLabelPoints(b.blobAsMatrix, phenotype, filename, stop);
			int n=blobsAfterAnErosion.size();
			
			if (n >= numCenters){
				ArrayList<Point> centers=new ArrayList<Point>();
				try {
					UnivariateDistribution dist=uniDistributions.get(phenotype).get(e);
					for (int i=0;i<n;i++){
						int nb=blobsAfterAnErosion.get(i).size();
						if (nb >= dist.cutoffBottom && nb <= dist.cutoffTop)
							centers.add(b.GetTrueCenter(blobsAfterAnErosion.get(i)));
					}					
					if (centers.size() == numCenters){
						return centers;
					}
				} catch (Exception exc){
					break;
				}
			}
		}
		
		BoolMatrix B=(BlobAsMatrix(blob)).blobAsMatrix;
		
		ArrayList<Point> centers=new ArrayList<Point>(numCenters);
		int s=(int)Math.round(Math.pow(((blob.size()/((double)numCenters)))/Math.PI,.5));
		
		ArrayList<Point> solid=new ArrayList<Point>(); //take squares instead because it's a large contiguous region
		for (int i=-s;i<=s;i++)
			for (int j=-s;j<=s;j++)
				solid.add(new Point(i,j));
		int N=solid.size();
		
		numCenters-=(int)Math.round(numCenters*.214602); //because squares are taken we discount by 21.4% ((2r)^2-pi*r^2)/(2r)^2=1-pi/4~.214602
		
		for (double p=1;p>=.01;p-=.11){
			for (int i=0;i<B.getWidth();i++){
				for (int j=0;j<B.getHeight();j++){
					double No=B.GetMaskAnd(i,j,solid);
					if (No/N >= p){
						centers.add(b.GetTrueCenter(new Point(i,j)));
						if (centers.size() == numCenters){
							return centers;
						}
						B.DeleteMaskAnd(i,j,solid);
					}
				}
			}
			
		}
		return centers;		
	}



}