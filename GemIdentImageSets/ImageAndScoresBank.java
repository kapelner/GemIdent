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

package GemIdentImageSets;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import GemIdentImageSets.Nuance.NuanceImageListInterface;
import GemIdentImageSets.Nuance.SuperNuanceImage;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.EdgeDetection.CannyEdgeDetector;
import GemIdentTools.Matrices.IntMatrix;
import GemIdentTools.Thumbnails;

/**
 * Provides a cache for:
 * <ul>
 * <li>{@link DataImage images}</li>
 * <li>{@link SuperImage superimages}</li>
 * <li>raw Nuance images</li> 
 * <li>{@link Thumbnails thumbnail images}</li>
 * <li>scores objects</li>
 * <li>Binary images</li>
 * </ul>
 * 
 * @author Adam Kapelner
 */
public class ImageAndScoresBank {

	/** 
	 * The maximum number of SuperImages to be cached, and the max number of Scores objects to be cached
	 * The only reason we cache these at all is when user classifies only those trained. In
	 * this case, training data is built from the trained images, and hence superimages and scores have to be built,
	 * then those same images are classified and superimages and scores would have to be built again (without 
	 * the caching)
	 */ 
	private static final int SUPER_AND_SCORES_MAX_SIZE=8; //large training sets?
	/** The maximum number of DataImages to be cached */
	private static final int DATAIMAGES_MAX_SIZE=10; //those of you with low RAM - beware
	/** The maximum number of DataImages to be cached */
	private static final int NUANCE_RAW_IMAGES_MAX_SIZE=108; //SUPER_AND_SCORES_MAX_SIZE * 9, beware of low ram	
	/** The maximum number of SuperImages to be cached */
	private static final int IS_IMAGES_MAX_SIZE=50; //ditto

	/**
	 * A class that combines the functionality of a map and a list
	 * 
	 * @param <E>	The object stored in the MapAndList referenced by Strings
	 * 
	 * @author Adam Kapelner
	 */
	private static class MapAndList<E> {
		private LinkedHashMap<String,E> map;
		public MapAndList(){
			map=new LinkedHashMap<String,E>();
		}
		public MapAndList(int initial_capacity){
			map=new LinkedHashMap<String,E>(initial_capacity);
		}
		public E get(String key){
			return map.get(key);
		}
		public void put(String key,E value){
			map.put(key,value);			
		}
		/**
		 * Ditch elements until this MapAndList is at its proper size
		 * 
		 * @param max_size
		 */
		public void flushToSize(int max_size) {
			synchronized (this){
				for (int i=0; i < this.size() - max_size; i++){
					this.removeFirstElement();
				}
			}
		}		
		public void removeFirstElement(){
			map.remove(getKeyAtIndex(0));
		}
		public String getKeyAtIndex(int i_o){
			int i = 0;
			for (String key : map.keySet()){
				if (i == i_o)
					return key;
				i++;
			}
			return null;
		}
		@SuppressWarnings("unused")
		public E getValueAtIndex(int i_o){
			return map.get(getKeyAtIndex(i_o));
		}
		public int size(){
			return map.size();
		}
	}
	//the cache vars
	private static MapAndList<SuperImage> allSuperImages;
	private static MapAndList<DataImage> allImages;
	private static MapAndList<BufferedImage> allNuanceRawImages;
	private static MapAndList<HashMap<String, IntMatrix>> allScores;
	private static MapAndList<IntMatrix> allEdges;
	private static MapAndList<BufferedImage> thumbnailImages;	
	private static MapAndList<DataImage> isImages;

	/** Initialization is the same as flushing */
	static {
		FlushAllCaches();
	}
	/** reinitialize all caches to free memory */
	public static void FlushAllCaches(){  //just initialize all the ivars:
		allSuperImages = new MapAndList<SuperImage>(SUPER_AND_SCORES_MAX_SIZE);
		allImages = new MapAndList<DataImage>(DATAIMAGES_MAX_SIZE);
		allScores = new MapAndList<HashMap<String, IntMatrix>>(SUPER_AND_SCORES_MAX_SIZE);
		allEdges = new MapAndList<IntMatrix>(SUPER_AND_SCORES_MAX_SIZE);
		thumbnailImages = new MapAndList<BufferedImage>(); //no max!
		isImages = new MapAndList<DataImage>(IS_IMAGES_MAX_SIZE);
		allNuanceRawImages = new MapAndList<BufferedImage>(NUANCE_RAW_IMAGES_MAX_SIZE);
	}
	/**
	 * gets a binary image from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the binary image
	 * @param dir			the directory where the binary image resides
	 * @return				the binary image
	 */
	public static BufferedImage getOrAddIs(String filename,String dir){
		if (dir != null)
			filename=dir+"//"+filename;
		
		DataImage dataImage=isImages.get(filename);
		if (dataImage == null){
			dataImage=new RegularSubImage(filename,false);
//			isImages.put(filename,dataImage);
		}
		if (isImages.size() > IS_IMAGES_MAX_SIZE)
			isImages.flushToSize(IS_IMAGES_MAX_SIZE);
		return dataImage.getAsBufferedImage();
	}	
	/**
	 * gets a {@link SuperImage SuperImage} from the cache. If it
	 * doesn't exist, load it and cache it. If the cache has reached
	 * its limit, flush it to avoid taking too much memory
	 * 
	 * @param filename 		the filename of the superimage
	 * @return				the superimage
	 */
	public static SuperImage getOrAddSuperImage(String filename) {
		SuperImage superImage=allSuperImages.get(filename);
		if (superImage == null){			
			if (Run.it.imageset instanceof NuanceImageListInterface)
				superImage=new SuperNuanceImage((NuanceImageListInterface)Run.it.imageset, filename);
			else
				superImage=new SuperRegularImage(filename);
			synchronized (allSuperImages){
				allSuperImages.put(filename,superImage);
			}
		}
		if (allSuperImages.size() > SUPER_AND_SCORES_MAX_SIZE)
			allSuperImages.flushToSize(SUPER_AND_SCORES_MAX_SIZE);
		return superImage;
	}
	/**
	 * gets a Nuance raw image (tiff file now as a BufferedImage) from 
	 * the cache. If it doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the TIFF image
	 * @return				the TIFF as a BufferedImage
	 */
	public static BufferedImage getOrAddRawNuanceImage(String filename){		
		BufferedImage image=allNuanceRawImages.get(filename);
		if (image == null){	
			try {
				image = IOTools.OpenImage(filename);
			} catch (IOException e) {
				e.printStackTrace();
			}
			synchronized (allNuanceRawImages){
				allNuanceRawImages.put(filename, image);
			}
		}
		if (allNuanceRawImages.size() > NUANCE_RAW_IMAGES_MAX_SIZE)
			allNuanceRawImages.flushToSize(NUANCE_RAW_IMAGES_MAX_SIZE);
		return image;
	}	
	/**
	 * gets a scores object from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the scores object
	 * @return				the scores object
	 */
	public static HashMap<String, IntMatrix> getOrAddScores(String filename) {
		HashMap<String, IntMatrix> scores=allScores.get(filename);
		if (scores == null){
			scores = Run.it.imageset.GenerateStainScores(getOrAddSuperImage(filename));
			synchronized (allScores){
				allScores.put(filename,scores);
			}
		}
		if (allScores.size() > SUPER_AND_SCORES_MAX_SIZE)
			allScores.flushToSize(SUPER_AND_SCORES_MAX_SIZE);
		return scores;
	}
	/**
	 * gets a scores object from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the scores object
	 * @return				the scores object
	 */
	public static IntMatrix getOrAddEdgeScores(String filename) {
		IntMatrix edges = allEdges.get(filename);
		if (edges == null){
			CannyEdgeDetector detector = new CannyEdgeDetector();
			//adjust its parameters as desired
//			detector.setLowThreshold(0.7f);
//			detector.setHighThreshold(1f);
			//apply it to an image
			detector.setSourceImage(getOrAddSuperImage(filename).getAsBufferedImage());
			detector.process();
			edges = detector.getEdgesScores();
//			String edges_filename = IOTools.GetFilenameWithoutExtension(filename) + "_edges.tiff";
//			IOTools.WriteScoreImage(edges_filename, edges);
			synchronized (allEdges){
				allEdges.put(filename, edges);
			}
		}
		if (allEdges.size() > SUPER_AND_SCORES_MAX_SIZE)
			allEdges.flushToSize(SUPER_AND_SCORES_MAX_SIZE);
		return edges;
	}	
	/**
	 * gets a thumbnail (as a BufferedImage) from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the thumbnail
	 * @return				the thumbnail
	 */
	public static BufferedImage getOrAddThumbnail(String filename) {
		
		BufferedImage image = thumbnailImages.get(filename);
		if (image == null){
			try {
				image = IOTools.OpenImage(Thumbnails.getThumbnailFilename(filename));
			} catch (IOException e) {
				image = null;
			} 
			thumbnailImages.put(filename, image);
		}
		return image;
	}
	/**
	 * gets a {@link DataImage DataImage} from the cache. If it
	 * doesn't exist, load it and cache it
	 * 
	 * @param filename 		the filename of the DataImage
	 * @return				the DataImage
	 */
	public static DataImage getOrAddDataImage(String filename){		
		DataImage dataImage=allImages.get(filename);
		if (dataImage == null){	
			dataImage = Run.it.imageset.getDataImageFromFilename(filename);
			synchronized (allImages){
				allImages.put(filename, dataImage);
			}
		}
		if (allImages.size() > DATAIMAGES_MAX_SIZE){
			allImages.flushToSize(DATAIMAGES_MAX_SIZE);
		}
		return dataImage;
	}


	
	/** flush all SuperImages from the cache */
	public static void FlushAllSuperImages(){
		allSuperImages=new MapAndList<SuperImage>(SUPER_AND_SCORES_MAX_SIZE);
	}
	/** flush all scores objects from the cache */
	public static void FlushAllScores(){
		allScores=new MapAndList<HashMap<String, IntMatrix>>(SUPER_AND_SCORES_MAX_SIZE);
	}
	/** flush binary images from the cache */
	public static void FlushAllIsImages() {
		isImages=new MapAndList<DataImage>(IS_IMAGES_MAX_SIZE);	
	}
}