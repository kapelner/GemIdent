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

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is non-operational and not implemented and therefore, undocumented. 
 * It is an attempt to classify 2-d points using agglomerative hierarchical clustering
 * 
 * @author Adam Kapelner
 */
public class SpatialClustering {

	private ArrayList<ArrayList<Cluster>> clusters;
	private HashMap<Cluster,HashMap<Cluster,Double>> diss;

	public SpatialClustering(ArrayList<ArrayList<Point>> data){
		clusters=new ArrayList<ArrayList<Cluster>>(data.size());
		diss=new HashMap<Cluster,HashMap<Cluster,Double>>(data.size());
		
		ArrayList<Cluster> all=new ArrayList<Cluster>(data.size());
		for (ArrayList<Point> points:data){
			Cluster cluster=new Cluster(points);
			all.add(cluster);
			diss.put(cluster,new HashMap<Cluster,Double>());
		}
		clusters.add(all);
		
		CalculateInitialDissimilarities(all);
		AgglomerateUntilOneCluster();
		
		diss=null; //flush it out
	}

	private void CalculateInitialDissimilarities(ArrayList<Cluster> all){
		for (Cluster a:all)
			for (Cluster b:all)
				if (a != b)
					CalcDistanceBetweenAndRecordIt(a,b);					
	}
	private void AgglomerateUntilOneCluster(){
		for (int m=0;m<Integer.MAX_VALUE;m++){
			clusters.add(MergeClosestTwoClusters(clusters.get(m)));
			if (clusters.size() == 1)
				break;
		}
	}
	private ArrayList<Cluster> MergeClosestTwoClusters(ArrayList<Cluster> oldSet){
		
		double min=Double.MAX_VALUE;
		Cluster a=null;
		Cluster b=null;		
		for (Cluster out:oldSet){
			for (Cluster in:oldSet){
				if (out != in){
				double dist=getDistBetweenClusters(out,in);
					if (dist < min){
						min=dist;
						a=out;
						b=in;
					}
				}
			}
		}
		
		Cluster newC=Merge(a,b);
		
		ArrayList<Cluster> newSet=new ArrayList<Cluster>(oldSet.size()-1);
		newSet.addAll(oldSet);
		newSet.remove(a);
		newSet.remove(b);
		
		UpdateDissimilarities(a,b,newC,newSet);
		
		newSet.add(newC);		
		return newSet;
	}
	private double getDistBetweenClusters(Cluster out, Cluster in) {
		double[] avgscoresInner=out.GetAvgScores();
		double[] avgscoresOuter=in.GetAvgScores();
		return CalcEuclideanDistanceSq(avgscoresInner,avgscoresOuter);
	}

	private Cluster Merge(Cluster a,Cluster b){
		ArrayList<Point> data=new ArrayList<Point>(a.getSize()+b.getSize());
		data.addAll(a.data);
		data.addAll(b.data);
		return new Cluster(data);
	}
	private void UpdateDissimilarities(Cluster a,Cluster b,Cluster newC,ArrayList<Cluster> newSet){
		diss.remove(a);
		diss.remove(b);
		diss.put(newC,new HashMap<Cluster,Double>(newSet.size()));
		for (Cluster cluster:newSet)
			if (cluster != newC)
				CalcDistanceBetweenAndRecordIt(cluster,newC);
	}
	public void CalcDistanceBetweenAndRecordIt(Cluster a,Cluster b){
		HashMap<Cluster,Double> map=diss.get(a);
		map.put(b,getDistBetweenClusters(a,b));
	}
	private double CalcEuclideanDistanceSq(double[] rowScore,double[] colScore){
		double S=0;
		for (int i=0;i<rowScore.length;i++){
			double diff=rowScore[i]-colScore[i];
			S+=diff*diff;
		}			
		return S;
	}
	
	private class Cluster {
		private ArrayList<Point> data;
		private double[] avgLoc;	
		public Cluster(ArrayList<Point> data){
			this.data=data;
		}
		@SuppressWarnings("unused")
		public Cluster(Point t) {
			data=new ArrayList<Point>(1);
			data.add(t);
		}
		public int getSize(){
			return data.size();
		}
		private double[] GetAvgScores() {
			if (avgLoc == null){
				double[] avgLoc={0,0};
				for (Point t:data){
					avgLoc[0]+=t.x;
					avgLoc[1]+=t.y;
				}
				avgLoc[0]/=data.size();
				avgLoc[1]/=data.size();
				this.avgLoc=avgLoc;
			}
			return avgLoc;
		}

		public Point getPoint(){
			double[] avgLoc=GetAvgScores();
			return new Point((int)Math.round(avgLoc[0]),(int)Math.round((avgLoc[1])));
		}
	}
	public Point GetCluster(int m,int c){
		ArrayList<Cluster> set = clusters.get(m);
		if (set == null)
			return null;
		if (c >= set.size())
			return null;
		return set.get(c).getPoint();
	}
}
