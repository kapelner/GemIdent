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

package GemIdentOperations;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JProgressBar;

import GemIdentCentroidFinding.PostProcess;
import GemIdentImageSets.*;
import GemIdentStatistics.Clustering.Clustering;
import GemIdentTools.IOTools;
import GemIdentTools.Rainbow;
import GemIdentTools.Thumbnails;
import GemIdentTools.Matrices.IntMatrix;
import GemIdentTools.Matrices.StringMatrix;

/**
 * Provides the internals for the phenotype training helper.
 * Color data is collected for each subimage and based on these
 * attributes, the subimages are clustered. This is helpful because 
 * it aids the user in selecting a diverse sample
 * 
 * @author Adam Kapelner
 */
public class ImageSetHeuristics extends Thread implements Serializable{ 
	private static final long serialVersionUID = 128566113635034885L;
		
	/** the file which stores the data for clustering */
	public transient static final String imageDataFile="imagedata.xml";
	/** the name of the HTML file that displays the thumbnails of the clustered subimages */
	public transient static final String Subsets="Subsets";
	/** the thread pool that creates attribute information for each subimage to be used in clustering */
	private transient ExecutorService postprocessPool;
//	/** the time the data attribute collection process began */
//	private transient long time_o;
	/** the progress bar for the entire data collection process */
	private transient JProgressBar progress;
	/** the amount to update the progress bar by when one image completes */
	private transient double update;
	/** the current progress level */
	private transient double upto;
	/** the number of clusters the user desires */
	private transient int NumClusters;
	/** the {GemIdentStatistics.Clustering Clustering} object that will cluster the images together using agglomerative hierarchical clustering */
	private transient Clustering hierarchical;
	/** the mapping from image to its color information - the only variable saved when serialized */
	private HashMap<String,double[]> data;	
	
	/** so Serializable is happy */
	public ImageSetHeuristics(){}
	
	/**
	 * The whole process is threaded as to not hog swing.
	 * 
	 * @param progress		the progress bar to record data collection progress
	 */
	public ImageSetHeuristics(final JProgressBar progress){
		this.progress=progress;

		upto=0;
//		StartTimer();
		
		start();
		try {
			join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}	
	/**
	 * If the file containing the data information does exist, open the data from 
	 * the {@link #imageDataFile XML file}, otherwise, generate the data: first 
	 * open the color information ({@link StainMaker Mahalanobis Cubes}), then
	 * retrieve the list of project images ({@link GemIdentTools.Thumbnails#GetImageList(String)}),
	 * then initialize data and the thread pool, then thread a {@link ImageSetHeuristics.ImageInfoGatherer
	 * ImageInfoGatherer} to collect data for all project images, and finally,
	 * it saves the data by serializing itself to the {@link #imageDataFile XML file}. For discussion 
	 * on the usefulness of this phenotype training helper, consult section 3.2.3 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public void run(){
		if (!IOTools.FileExists(imageDataFile)){
			if (Run.it.imageset instanceof ImageSetInterfaceWithUserColors){ //this is ugly but conceptually it's the only way to go I believe
				((ImageSetInterfaceWithUserColors)Run.it.imageset).OpenMahalanobisCubes(null);
			}		
			String[] filenames=Thumbnails.GetImageList(Run.it.imageset.getHomedir());	
			data=new HashMap<String,double[]>(filenames.length);
			
			postprocessPool=Executors.newFixedThreadPool(Run.it.num_threads);			
			update=100/((double)filenames.length);
			
			
			for (String filename:filenames)
		    	postprocessPool.execute(new ImageInfoGatherer(filename));
			postprocessPool.shutdown();
			try {	         
		         postprocessPool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
		    } catch (InterruptedException ignored){}
		    postprocessPool = null;
		    
		    IOTools.saveToXML(this,Run.it.imageset.getFilenameWithHomePath(imageDataFile));
		}
		else {
			progress.setVisible(false);
			ImageSetHeuristics open=(ImageSetHeuristics)IOTools.openFromXML(Run.it.imageset.getFilenameWithHomePath(imageDataFile));
			data=open.data;
		}
	}
	/**
	 * Assuming the data has already been generated or loaded from the hard disk,
	 * cluster the images together using the data via {@link GemIdentStatistics.Clustering
	 * agglomerative hierarchical clustering} then display the clusters
	 * in {@link #CreateHTMLOutputAndOpen() HTML format}.
	 * 
	 * @param NumClusters		the number of clusters desired
	 */
	public void Compute(int NumClusters){
		this.NumClusters=NumClusters;
		
		hierarchical=new Clustering(data,NumClusters);
		CreateHTMLOutputAndOpen();
	}
	/** When an image finishes, bump the (@link #upto progress} up by {@link #update update} */
	private void UpdateProgressBar(){
		upto+=update;
		progress.setValue((int)Math.round(upto));
		progress.repaint();
	}
	
	/** the width of the html table to display the clustered thumbnails */
	private static final int TableWidth=1000;
	/**
	 * Creates a set of HTML files (one for each size in {@link GemIdentImageSets.ImageSetInterface.Size Size}
	 * with a global view (if the image set is a large
	 * global image) where the thumbnails have colored borders where each cluster
	 * shares a common color. After, each cluster's thumbnails are displayed 
	 * without the global context. All thumbnails link to the raw image.
	 */
	private void CreateHTMLOutputAndOpen(){		
		
		int tWidth=Thumbnails.tWidth/2;
		int tHeight=Thumbnails.tHeight/2;
		int numThumbPerRow=(int)Math.round(TableWidth/(double)tWidth);
		
		ArrayList<Set<String>> clusters=GetImageListsFromClusters();
		
		for (ImageSetInterface.Size size:ImageSetInterface.Size.values()){	
			int factor=0;
			switch (size){
				case XL: 	factor=1; break;
				case L: 	factor=2; break;
				case M: 	factor=4; break;
				case S: 	factor=8; break;
				case XS: 	factor=16; break;
				case XXS: 	factor=32; break;
			}
			
			PrintWriter out=null;
			try {
				out=new PrintWriter(new BufferedWriter(new FileWriter(Run.it.imageset.getFilenameWithHomePath(Subsets+NumClusters+size+".html"))));
			} catch (IOException e) {
				System.out.println(Subsets+NumClusters+".html"+" cannot be created");
			}
			out.print("<HTML>");
			CreateHTMLHeader(out);
			if (!(Run.it.imageset instanceof NonGlobalImageSet))
				CreateHTMLForGlobal(out,factor,clusters,tWidth,tHeight);		
			for (int c=1;c<=NumClusters;c++)
				CreateHTMLCluster(out,clusters.get(c-1),c,numThumbPerRow,tWidth,tHeight);
			out.print("</HTML>");
			out.close();
		}

		IOTools.RunProgramInOS(Subsets+NumClusters+"M.html");
	}
	
	/** the width of the color border around the thumbnails */
	private static final int borderSizeXL=6;
	/**
	 * Creates the global image composed of the thumbnails of the subimages, not valid for all image sets
	 * 
	 * @param out				writes the html file to the hard disk
	 * @param factor			the {@link GemIdentImageSets.ImageSetInterface.Size Size} factor
	 * @param clusters			a list of the cluster sets	
	 * @param tWidth			the width of the thumbnails
	 * @param tHeight			the height of the thumbnails
	 */
	private void CreateHTMLForGlobal(PrintWriter out,int factor,ArrayList<Set<String>> clusters,int tWidth,int tHeight){
		
		StringMatrix picFilenameTable=(StringMatrix)Run.it.imageset.getPicFilenameTable();
		
		int borderSize=Math.max(1,(int)Math.round(borderSizeXL/((double)factor)));
		
		out.print("<br>");
		out.print("\n");
		out.print("<br>");
		out.print("\n");
		out.print("<table cellspacing=0 cellpadding=0 border=1 bordercolor=BLACK>");
		out.print("\n");
		
		for (int i=0;i<picFilenameTable.getWidth();i++){
			out.print("<tr>");
			out.print("\n");
			for (int j=0;j<picFilenameTable.getHeight();j++){
				String filename=picFilenameTable.get(i,j);
				if (filename != ImageSetInterface.PIC_NOT_PRESENT){
					String rgbColorString=GetClusterColor(filename,clusters);
					out.print("<td height="+((tHeight/factor)+2*borderSize)+" width="+((tWidth/factor)+2*borderSize)+" bgcolor=rgb("+rgbColorString+") border=5 bordercolor=white>");
					if (Run.it.imageset.PicWasClassified(filename))
						out.print("<a href=\""+PostProcess.GetBothName(filename)+".jpg\"><img src="+Thumbnails.getThumbnailFilename(filename)+" alt="+filename+" width="+(tWidth/factor)+" height="+(tHeight/factor)+" border=0 hspace="+borderSize+" align=center valign=center)></a>");
					else
						out.print("<a href=\""+filename+"\"><img src="+Thumbnails.getThumbnailFilename(filename)+" alt="+filename+" width="+(tWidth/factor)+" height="+(tHeight/factor)+" border=0 hspace="+borderSize+" align=center valign=center)></a>");
				}
				else
					out.print("<td>");
				out.print("</td>");
				out.print("\n");
			}
			out.print("</tr>");
			out.print("\n");
		}
		out.print("</table>");
		out.print("\n");
	}


	/** Print the header of the html - title and links to the other {@link GemIdentImageSets.ImageSetInterface.Size Sizes} */
	private void CreateHTMLHeader(PrintWriter out){
		out.print("\n");
		out.print("<head>");
		out.print("\n");
		out.print("<title>Image Set By Like Image Subsets</title>");
		out.print("\n");
		out.print("</head>");
		out.print("\n");
		out.print("<H1>Global view in "+NumClusters+" subsets</H1>");
		out.print("<br>");
		String link="Zoom Level: ";
		for (ImageSetInterface.Size linkSize:ImageSetInterface.Size.values())
			link+="<a href=\""+Subsets+NumClusters+linkSize+".html\">"+linkSize+"</a>&nbsp";
		out.print(link);
		out.print("\n");
		
	}
	/**
	 * Creates the portion of the html file that displays thumbnails for a given cluster
	 * 
	 * @param out					writes the html file to the hard disk
	 * @param images				the images in this cluster
	 * @param c						the cluster number
	 * @param numThumbPerRow		the maximum number of thumbnails to put on a row
	 * @param tWidth				the width of the thumbnails
	 * @param tHeight				the height of the thumbnails
	 */
	private void CreateHTMLCluster(PrintWriter out,Set<String> images,int c,int numThumbPerRow,int tWidth,int tHeight){
		out.print("<br>");
		out.print("\n");
		out.print("<br>");
		out.print("\n");
		out.print("<H3 align=center>Subset #"+c+"</H3>");
		out.print("\n");
		
		out.print("<table width="+TableWidth+" align=center>");
		out.print("\n");

		Object[] imageArray=images.toArray();
		int numImages=imageArray.length;
		for (int i=0;i<numImages;){

			out.print("<tr>");
			out.print("\n");
			for (int w=0;w<numThumbPerRow;w++){	
				if (i+w < numImages){
					out.print("<td width="+tWidth+" height="+tHeight+" align=center>");
					out.print("\n");
					out.print("<a href=\""+imageArray[i+w]+"\"><img src="+Thumbnails.getThumbnailFilename((String)imageArray[i+w])+" alt="+imageArray[i+w]+" width="+tWidth+" height="+tHeight+" hspace=0 vspace=0 border=0></a>");
					out.print("\n");
					out.print("</td>");
					out.print("\n");
				}
			}
			out.print("</tr>");
			out.print("\n");
			
			out.print("<tr>");
			out.print("\n");
			for (int w=0;w<numThumbPerRow;w++){	
				if (i+w < numImages){
					out.print("<td width="+tWidth+" align=center>");
					out.print("\n");
					out.print("<a href=\""+imageArray[i+w]+"\">"+imageArray[i+w]+"</a>");
					out.print("\n");
					out.print("</td>");
					out.print("\n");
				}
			}
			out.print("</tr>");
			out.print("\n");
			i+=numThumbPerRow;
		}		
		out.print("</table>");
		out.print("\n");	
	}
	/**
	 * Given an image, find which cluster it resides in and return the cluster color
	 * as a string that HTML can understand
	 * 
	 * @param filename			the image to lookup
	 * @param clusters			the list of clusters as image sets
	 * @return					the color in the format "R,G,B" where R,G,B are in {0,...,255} 
	 */
	private String GetClusterColor(String filename,ArrayList<Set<String>> clusters) {
		int co=-999; //so rainbow will crash
		for (int c=0;c<NumClusters;c++){
			Set<String> cluster=clusters.get(c);
			for (String image:cluster)
				if (filename.equals(image))
					co=c;			
		}
		Color color=Rainbow.getColor(co,NumClusters);
		return ""+color.getRed()+","+color.getGreen()+","+color.getBlue();
	}
	/**
	 * After the clustering is completed, we extract the information
	 * from the {GemIdentStatistics.Clustering Clustering} object
	 * in a convenient format
	 * 
	 * @return		the clusters as a List of sets of filenames
	 */
	private ArrayList<Set<String>> GetImageListsFromClusters() {
		ArrayList<Set<String>> clusters=new ArrayList<Set<String>>(NumClusters);
		for (int c=0;c<NumClusters;c++)
			clusters.add(hierarchical.GetCluster(c));		
		return clusters;
	}

	/** when calculating the scores, only consider pixels whose color score is below this */
	private static final int LOW_THRESHOLD=10;
	/** Framework for gathering color information in one image and can be threaded in a thread pool */
	private class ImageInfoGatherer implements Runnable {	
		
		/** the image whose color information is being extracted */
		private String filename;
		/** its color information */
		private double[] colorInfo;
		
		/** default constructor also initializes the colorInfo to be the size of the number of colors */
		public ImageInfoGatherer(String filename){
			this.filename=filename;
			colorInfo=new double[Run.it.imageset.NumFilters()];
		}
		/**
		 * For each relevant color, {@link GemIdentModel.Stain#FilterScoring(DataImage) score}
		 * the image, then check each pixel to see if it's less than the {@link ImageSetHeuristics#LOW_THRESHOLD 
		 * low threshold}. If so {@link GemIdentTools.Matrices.ShortMatrix#ThresholdLowerThan(int) add up the scores}
		 * and record them in {@link #colorInfo colorInfo}. Upon completion, store the scores
		 * in the {@link ImageSetHeuristics#data data} master map.
		 */
		public void run(){
			DataImage I=ImageAndScoresBank.getOrAddDataImage(filename);
			int a=0;
			for (String filter:Run.it.imageset.getFilterNames()){
				IntMatrix scores = Run.it.getUserColorsImageset().getStain(filter).FilterScoring(I);
				colorInfo[a] = scores.ThresholdLowerThan(LOW_THRESHOLD);
				a++;
			}
			data.put(filename,colorInfo);
			UpdateProgressBar();
		}
	}
//	/** Begin the timing */
//	private void StartTimer(){
//		time_o=System.currentTimeMillis();			
//	}
	public HashMap<String,double[]> getData() {
		return data;
	}
	public void setData(HashMap<String,double[]> data) {
		this.data=data;
	}
}