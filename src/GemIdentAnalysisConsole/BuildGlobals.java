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

package GemIdentAnalysisConsole;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import GemIdentCentroidFinding.PostProcess;
import GemIdentClassificationEngine.Classify;
import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.IOTools;

/**
 * Class responsible for assembling the subimage results in the
 * image set into the global images found in the {@link GemIdentView.KAnalysisPanel analysis}
 * panel.
 * 
 * @author Adam Kapelner
 */
public class BuildGlobals extends Thread{

	/** the suffix to the centroid data object names */
	public static final String Centroids = "_cent";
	/** the suffix to the pixel data object names */
	public static final String Pixels = "_pix";
	/** the mapping from phenotype name to the global result matrix */
	private HashMap<String,BoolMatrix> globals;
	/** the mapping from phenotype name to the total number of counts of that phenotype globally */
	private HashMap<String,Integer> counts;
	/** the threadpool responsible for assembling the subimages into the global result matrix */
	private ExecutorService buildGlobalPool;
	/** the relevant phenotypes - all but the NON */
	private Collection<String> phenotypes;
	/** the total number of images in the image set */
	private int totNumPics;
	
	/**
	 * The constructor begins the thread that executes the construction. This
	 * was threaded because it was slowing down swing.
	 * 
	 * @throws InterruptedException 	if there is a thread problem (should never happen)
	 */
	public BuildGlobals() throws InterruptedException {

		phenotypes=Run.it.getPhenotyeNamesSaveNON();
		globals=new HashMap<String,BoolMatrix>();
		counts=new HashMap<String,Integer>();
		
		start();
		join();
	}
	/**
	 * Initializes the result matrices, initializes the thread pool, then loads the 
	 * thread pool with tasks for each subimage
	 */
	public void run(){

		for (String phenotype:phenotypes){
			if (Run.it.getPhenotype(phenotype).isFindCentroids()){
				globals.put(phenotype + Centroids, new BoolMatrix(Run.it.imageset.getGlobalWidth(true),Run.it.imageset.getGlobalHeight(true)));	
				counts.put(phenotype + Centroids, 0);
			}
			globals.put(phenotype + Pixels, new BoolMatrix(Run.it.imageset.getGlobalWidth(true),Run.it.imageset.getGlobalHeight(true)));
			counts.put(phenotype + Pixels, 0);
		}

		buildGlobalPool=Executors.newFixedThreadPool((int)Math.round(1.5*Run.it.num_threads));
		Collection<String> filenames=Run.it.imageset.GetImages();
		totNumPics=filenames.size();
		for (String filename:filenames)
			buildGlobalPool.execute(new BuildGlobalPiece(filename));
		buildGlobalPool.shutdown();
		try {	         
			buildGlobalPool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
	}
	/**
	 * responsible for assembling one image file's results (all the phenotypes's results
	 * matrix) into the global result matrices
	 */
	private class BuildGlobalPiece implements Runnable{

		/** the image being processed by this object */
		private String filename;
		
		/** default constructor */
		public BuildGlobalPiece(String filename){
			this.filename=filename;
		}
		/**
		 * Load every result matrix from this phenotype and copy it into its proper
		 * location in the result matrix
		 */
		public void run() {
			for (String phenotype : phenotypes){
				if (Run.it.getPhenotype(phenotype).isFindCentroids()){
					MarkUp(phenotype, PostProcess.GetIsCentroidName(filename,phenotype), Centroids);
				}
				MarkUp(phenotype, Classify.GetIsName(filename, phenotype), Pixels);
			}
		}
		private void MarkUp(String phenotype, String imagefilename, String centroids_or_pixels) {
			//open the raw image
			BufferedImage isC=null;
			try {
				isC=IOTools.OpenImage(imagefilename);
				counts.put(phenotype + centroids_or_pixels, counts.get(phenotype + centroids_or_pixels) + 1);
			} catch (Exception e){}
			//add its values to the global image if the subimage was able to opened			
			if (isC != null){
				BoolMatrix global=globals.get(phenotype + centroids_or_pixels);
				for (int i=0;i<isC.getWidth();i++){
					for (int j=0;j<isC.getHeight();j++){
//						if (i == 0 || j == 0 || i == isC.getWidth() - 1 || j == isC.getHeight() - 1)
//							isC.setRGB(i, j, BoolMatrix.WhiteRGB);
						if (isC.getRGB(i,j) == BoolMatrix.WhiteRGB){
							Point t=Run.it.imageset.getTrueLocation(filename,new Point(i,j),true);
							if (t != null)
								global.set(t,true);
						}
					}
				}
			}
		}
	}	
	/**
	 * This {@link java.io.FilenameFilter file filter} returns
	 * only global result matrix files of type "bit"
	 */
	private static class BitFilter implements FilenameFilter{
		/**
		 * Given a file, returns true if it is a global result matrix
		 *  
		 * @param dir		the directory the file is located in
		 * @param name		the name of the file
		 * @return			whether or not the file is an global result matrix
		 */
		public boolean accept(File dir, String name){
			String[] fileparts=name.split("\\.");
			if (fileparts.length == 2){
				String ext=fileparts[1].toLowerCase();
				if (ext.equals("bit")){
					return true;
				}
				else 
					return false;
			}
			else return false;
		}		
	}
	/**
	 * Looks in the output directory and returns all global results matrix files (*.bit)
	 * @return		an array of all the global result matrices
	 */
	public static ArrayList<String> GetGlobalFilenames(){
		String[] files=(new File(Run.it.imageset.getFilenameWithHomePath(PostProcess.analysisDir + File.separator))).list(new BitFilter());
		ArrayList<String> list=new ArrayList<String>(files.length);
		for (String file:files){
			String[] nameExt=file.split("\\.");
			if (nameExt.length == 2){
				String name=nameExt[0]; //get filename without ext
				String[] parts=name.split("-"); //gets pieces
				if (parts != null)
					if (parts.length == 3)
						if (parts[0].equals(Run.it.projectName))
							list.add(parts[2]);
			}
		}
		return list;
	}
	/**
	 * Given te name of a phenotype, get its corresponding global result matrix
	 * 
	 * @param phenotype		the name of the phenotype
	 * @return				the filename (without the extension) and path of the global result matrix relative to the project folder
	 */
	public static String GetGlobalFilename(String phenotype){
		return GetGlobalFilenameWithoutBit(phenotype, null)+".bit";
	}
	/**
	 * Given te name of a phenotype, get its corresponding global result matrix
	 * 
	 * @param phenotype		the name of the phenotype
	 * @return				the full filename and path of the global result matrix relative to the project folder
	 */
	public static String GetGlobalFilenameWithoutBit(String phenotype, String dir) {
		if (dir == null){
			dir = PostProcess.analysisDir;
		}
		return dir + File.separator + Run.it.projectName + "-Global-" + phenotype;
	}
	public HashMap<String,BoolMatrix> getGlobals(){
		return globals;
	}
	public HashMap<String,Integer> getCounts(){
		return counts;
	}
	public int getTotNumPics() {
		return totNumPics;
	}
}