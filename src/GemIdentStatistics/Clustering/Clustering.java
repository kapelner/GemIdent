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

package GemIdentStatistics.Clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import GemIdentOperations.Run;

/**
 * Clusters S-dimensional color data into numClusters
 * using agglomerative hierarchical clustering
 * 
 * @author Adam Kapelner
 */
public class Clustering {

	/** The number of clusters */
	private int numClusters;
	/** The List of Cluster objects */
	private ArrayList<Cluster> clusters;
	/** The number of dimensions of the data - the number of colors */
	private int S;

	/**
	 * Constructs the clustering object by first normalizing the data, 
	 * creating cluster objects from each data record, finds the initial 
	 * dissimilarity matrix, then begins agglomerative hiearchical 
	 * clustering until numClusters is reached
	 * 
	 * @param data  		a map from the name of the data record to its data
	 * @param numClusters	the number of clusters desired
	 */
	public Clustering(HashMap<String,double[]> data,int numClusters){
		this.numClusters=numClusters;
		S = Run.it.imageset.NumFilters();

		NormalizeData(data);
		GetClustersFromData(data);
		CalculateInitialDissimilarities();
		AgglomerateUntilNumClusters();	
	}
	/**
	 * 
	 * Normalizes each attribute by subtracting the mean and
	 * dividing by the standard deviation
	 * 
	 * @param data 	a map from the name of the data record to its data
	 */
	private void NormalizeData(HashMap<String,double[]> data){
		
		int n=data.size();
		double[] u=new double[S];	
		
		//get mean:
		for (double[] scores:data.values())
			for (int s=0;s<S;s++)
				u[s]+=scores[s];
		for (int s=0;s<S;s++)
			u[s]/=n;
		
		//get SD:
		double[] v=new double[S];
		for (double[] scores:data.values())
			for (int s=0;s<S;s++)
				v[s]+=Math.pow(scores[s]-u[s],2);
		for (int s=0;s<S;s++)
			v[s]=Math.sqrt(v[s]);
		
		//normalize:
		for (double[] scores:data.values())
			for (int s=0;s<S;s++)
				scores[s]=(scores[s]-u[s])/v[s];		
	}
	/**
	 * Creates a "cluster" object for each of the data records
	 * 
	 * @param data 		a map from the name of the data record to its data
	 */
	private void GetClustersFromData(HashMap<String,double[]> data){
		clusters=new ArrayList<Cluster>(data.size());
		for (String key:data.keySet())
			clusters.add(new Cluster(key,data.get(key),data.size()));
	}
	/**
	 * For each cluster, calculate the distance between it 
	 * and every other cluster
	 *
	 */
	private void CalculateInitialDissimilarities(){
		for (Cluster out:clusters)
			for (Cluster in:clusters)
				if (out != in)
					out.CalcDistanceBetweenAndRecordIt(in);					
	}
	/**
	 * Merge the two closest clusters ad infinitum
	 * until there are only numClusters left
	 *
	 */
	private void AgglomerateUntilNumClusters(){
		while (true){
			MergeClosestTwoClusters();
			if (clusters.size() == numClusters)
				break;
		}
	}
	/**
	 * Merges two clusters - iterates over all clusters and finds 
	 * minimum distance, then merges those two, removing one of them
	 * from the permanent cluster list. It then updates the 
	 * dissimilarity values for the new merged cluster.
	 *
	 */
	private void MergeClosestTwoClusters(){
		if (clusters.size() == 1)
			return;
		double min=Double.MAX_VALUE;
		Cluster a=null;
		Cluster b=null;
		for (Cluster out:clusters){
			for (Cluster in:out.getDissClusters()){
				double dist=out.getDistFromThisToOtherCluster(in);
				if (dist < min){
					min=dist;
					a=out;
					b=in;
				}
			}
		}
		a.Merge(b);
		DeleteCluster(b);
		UpdateDissimilarities(a);
	}
	/**
	 * For all clusters, find the distance between this newCluster
	 * and record in the dissimiliary lists of the others
	 * 
	 * @param newCluster	the newCluster to find distances from
	 */
	private void UpdateDissimilarities(Cluster newCluster){
		for (Cluster cluster:clusters)
			if (cluster != newCluster)
				cluster.CalcDistanceBetweenAndRecordIt(newCluster);
	}
	/**
	 * Remove a cluster from the masterList and then remove it
	 * from each cluster's dissimilariy list
	 * 
	 * @param dead 		the cluster to remove
	 */
	private void DeleteCluster(Cluster dead){
		clusters.remove(dead);
		for (Cluster cluster:clusters)
			cluster.RemoveCluster(dead);
	}
	/**
	 * Provides the infrastructure to house a cluster of 
	 * data records
	 * 
	 * @author Adam Kapelner
	 */
	private class Cluster {
		/** Holds the data records - as the cluster expands, this gets larger */
		private HashMap<String,double[]> data;
		/** The dissimiliary list holds the distance measures between the center of this cluster and the center of other clusters - as the number of clusters decreases, this gets smaller */
		private HashMap<Cluster,Double> diss;
		
		/**
		 * Creates a new cluster from a single data record
		 * 
		 * @param file		The name of the initial data record
		 * @param record	The initial data record	
		 * @param n			The initial number of data records
		 */
		public Cluster(String file,double[] record,int n){
			data=new HashMap<String,double[]>();
			data.put(file,record);
			diss=new HashMap<Cluster,Double>(n-1);
		}
		/**
		 * Removes a cluster from the dissimilarity list
		 * @param dead		The cluster to be removed
		 */
		public void RemoveCluster(Cluster dead){
			diss.remove(dead);			
		}
		/**
		 * Returns the distance from this cluster to another
		 * @param another		the other cluster
		 * @return				the distance
		 */
		public double getDistFromThisToOtherCluster(Cluster another){
			return diss.get(another);
		}
		/**
		 * Returns all other clusters
		 * @return			the other clusters
		 */
		public Set<Cluster> getDissClusters(){
			return diss.keySet();
		}
		/**
		 * Merges this cluster with another. It will merge the data records together.
		 * @param b
		 */
		public void Merge(Cluster b){
			data.putAll(b.data);
		}
		/**
		 * Gets the number of data records in this cluster
		 * @return			the number of data records
		 */
		@SuppressWarnings("unused")
		public int getSize(){
			return data.size();
		}	
		/**
		 * Calculates distance between this cluster and another by using the average 
		 * scores and then records it in the dissimilarity list.
		 * @param other		The cluster to measure the distance to
		 */
		public void CalcDistanceBetweenAndRecordIt(Cluster other){
			double[] avgscoresInner=GetAvgScores();
			double[] avgscoresOuter=other.GetAvgScores();
			diss.put(other,CalcEuclideanDistance(avgscoresInner,avgscoresOuter));
		}
		/**
		 * Returns a data record that represents the arithmetic average of each
		 * attribute of every data record contained in the cluster
		 * @return			The average data record
		 */
		private double[] GetAvgScores() {
			double[] avg=new double[S];
			for (String key:data.keySet()){
				double[] score=data.get(key);
				for (int s=0;s<S;s++)
					avg[s]+=score[s];
			}
			for (int s=0;s<S;s++)
				avg[s]/=data.size();
			return avg;
		}
		/**
		 * Calculates the Euclidean distance between vector a and vector b
		 * @param a			The first vector
		 * @param b			The second vector
		 * @return			The distance
		 */
		private double CalcEuclideanDistance(double[] a,double[] b){
			double S=0;
			for (int i=0;i<a.length;i++){
				double diff=a[i]-b[i];
				S+=diff*diff;
			}			
			return Math.sqrt(S);
		}
	}
	/**
	 * Gets the cth cluster. It will return a set of the names of all the 
	 * data records contained in the cluster
	 * 
	 * @param c		the cth cluster
	 * @return		set of the names of each data record contained therein
	 */
	public Set<String> GetCluster(int c){
		return ((clusters.get(c)).data).keySet();
	}
}