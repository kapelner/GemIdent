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

package GemIdentImageSets.Nuance;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.IntMatrix;
import GemIdentTools.Thumbnails;

/**
 * Handles image sets created by a modified CRi Nuance Bliss Image scanner (the newer setup)
 * 
 * see <a href="http://www.cri-inc.com/products/nuance.asp">the Nuance scanner webpage</a>
 * 
 * This class is not fully documented since it implements proprietary features
 * for a proprietary hardware setup
 * 
 * @author Adam Kapelner
 */
public final class NuanceImageListNew extends NuanceImageListInterface {
	private static final long serialVersionUID = 2927660292462263602L;


	private LinkedHashMap<Point, HashMap<String, String>> raw_new_nuance_list; //coordinates --> chromagen --> filename
	private transient HashMap<String, Point> raw_coordinate_hash;
	private transient BufferedImage overview_image;

	public NuanceImageListNew(){}
	
	public NuanceImageListNew(String homedir) {
		super(homedir);
	}

	/**
	 * This {@link java.io.FilenameFilter file filter} returns only image files
	 * of type "tif"
	 * 
	 */
	private class ImageFileFilter implements FilenameFilter {
		/**
		 * Given a file, returns true if it is an image
		 * 
		 * @param dir
		 *            the directory the file is located in
		 * @param name
		 *            the file itself
		 * @return whether or not the file is an image
		 */
		public boolean accept(File dir, String name) {
			String[] fileparts = name.split("\\.");
			if (fileparts.length >= 2) {
				String ext = fileparts[fileparts.length - 1].toLowerCase();
				if (ext.equals("tif"))
					return true;
				else
					return false;
			} else
				return false;
		}
	}

	private String[] GetImageList(String dir) {
		return (new File(dir + File.separator)).list(new ImageFileFilter());
	}

	protected void BuildImageList(){
		//initialize some data
		raw_new_nuance_list = new LinkedHashMap<Point, HashMap<String, String>>();
		raw_coordinate_hash = new HashMap<String, Point>();
		// all new image lists are 20x
		zoomlevel = ZoomLevelTwentyX;
		distance_to_pixel_conversion = MAGN_AT_20X;
		
		String[] files = GetImageList(homedir + getExtraPath());
		if (files == null){ //we've already initialized, this is a total hack, but I need to get this done
			files = GetImageList(homedir);
		}
		ExtractInfoFromFiles(files);
		FindMinsAndMaxs();
		MakeNumTable();
	}	

	private void FindMinsAndMaxs() {

		xmin = ymin = Integer.MAX_VALUE;
		xmax = ymax = Integer.MIN_VALUE;

		for (Point coords : raw_new_nuance_list.keySet()) { // find maxs
			if (coords.x > xmax)
				xmax = coords.x;
			if (coords.y > ymax)
				ymax = coords.y;
			if (coords.x < xmin) 
				xmin = coords.x;
			if (coords.y < ymin) 
				ymin = coords.y;
		}
	}

	private void MakeNumTable() {
		width = xmax - xmin + 1;
		height = ymax - ymin + 1;
		stageNumberMatrix = new IntMatrix(width, height, BAD_PIC);
		int i = 1;
		for (Point coords : raw_new_nuance_list.keySet()) { //this is the reason it needs to be linked
			stageNumberMatrix.set(coords.x - xmin, coords.y - ymin, i);	
			i++;
		}
	}	
	
	private static final int x_step = 692;
	private static final int y_step = 508;
	private void ExtractInfoFromFiles(String[] files) {
		if (files.length == 0) {
			errorstring = "No image files found. Are you sure you picked the right directory?";
			return;
		}
		
		//this is the regex that parses the file name
		Pattern file_regex = Pattern.compile("HP_RGB_\\d+_\\[(.*)\\]_Unmixed_(.*).tif",Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
		for (String file : files){
			Matcher file_regex_matcher = file_regex.matcher(file);
			
			//match the filename to regex so we can parse it correctly
			while (file_regex_matcher.find()){
				//first pull out the coordinates
				String[] raw_coordinates = file_regex_matcher.group(1).split(",");
				double raw_x = Double.parseDouble(raw_coordinates[0]);
				double raw_y = Double.parseDouble(raw_coordinates[1]);
				int adjusted_x = (int)Math.round(raw_x / x_step);
				int adjusted_y = (int)Math.round(raw_y / y_step);
				Point coords = new Point(adjusted_x, adjusted_y);
				//next handle the chromagen
				String chromagen = file_regex_matcher.group(2);
				//now keep track of info
				//keep track of all chromagens
				InitWaveInfo(wave_names.size() + 1, chromagen);
				//keep track of the coordinates --> chromagen --> filename
				HashMap<String, String> stage_bundle = raw_new_nuance_list.get(coords);
				if (stage_bundle == null){ //it wasn't here before
					stage_bundle = new HashMap<String, String>();					
					raw_new_nuance_list.put(coords, stage_bundle);
					raw_coordinate_hash.put(file_regex_matcher.group(1), coords);
				}
				stage_bundle.put(chromagen, file);
				
			}			
		}		
	}

	public String getInitializationFilename() {
		return null; // the new Nuance doesn't have one!
	}

	@Override
	protected String ConvertNumAndWaveToFileName(int stage, int wavenum) {
		Point coords = FindLocalLocation(stage);
		coords.translate(xmin, ymin);
		String chromagen = wave_names.get(wavenum);
		return raw_new_nuance_list.get(coords).get(chromagen);
	}	
	
	public LinkedHashMap<Point, HashMap<String, String>> getRaw_new_nuance_list() {
		return raw_new_nuance_list;
	}

	public void setRaw_new_nuance_list(LinkedHashMap<Point, HashMap<String, String>> raw_new_nuance_list) {
		this.raw_new_nuance_list = raw_new_nuance_list;
	}	
	
	private static final int XoFor20X = 0;
	private static final int XfFor20X = 0;
	private static final int YoFor20X = 0;
	private static final int YfFor20X = 0;	
	
	public int getXo() {
		return XoFor20X;
	}

	public int getXf() {
		return XfFor20X;
	}

	public int getYo() {
		return YoFor20X;
	}

	public int getYf() {
		return YfFor20X;
	}

	//related to overview image:

	@Override
	public BufferedImage getGlobalThumbnailImage(Float approxGlobalScaledWidth) {
		if (overview_image == null){
			try {
				overview_image = IOTools.OpenImage(OverviewImageFilename);
			} catch (FileNotFoundException e){ //overview image not generated yet
				//now try to build it once again from raw files
				GenerateAndSaveOverViewImageFromRawSpectralFiles();
			}
		}
		if (overview_image != null){ //ie it's been initialized
			DrawGlobalThumbnailOverlay(overview_image.getGraphics());
		}
		return overview_image;
	}
	
	protected Integer GetStageNumberFromOverviewImageGivenCoordinate(int x, int y){
		//reverse the calculation in the next function:
		int i = (int)Math.round((x - OverviewThumbWidth / 2) / (double)OverviewThumbWidth);
		int j = (int)Math.round((y - OverviewThumbHeight / 2) / (double)OverviewThumbHeight);		
		return GetStageFromThumbnailCoordinates(i, j);
	}
	
	protected Integer GetStageFromThumbnailCoordinates(int i, int j){
		try {
			return stageNumberMatrix.get(i , j);	
		} catch (ArrayIndexOutOfBoundsException e){
			return BAD_PIC;
		}
	}	
	
	private static final int OverviewThumbWidth = 96;
	private static final int OverviewThumbHeight = 75;
	@Override
	protected void DrawGlobalThumbnailOverlay(Graphics g) {
		double font_size;
		if (NumImages() < SmallLargeSetNumImages) {
			font_size = FontSizeForOverlaySmallSet;
		} else {
			font_size = FontSizeForOverlayLargeSet;
		}
		g.setColor(Color.BLACK);
		g.setFont(new Font("Serif", Font.PLAIN, (int) font_size));

		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				int x = OverviewThumbWidth / 2 + i * OverviewThumbWidth;
				int y = OverviewThumbHeight / 2 + j * OverviewThumbHeight;
				int stage = stageNumberMatrix.get(i, j);
				//as long as it's a valid pic...
				if (stage != BAD_PIC){
					if (trainPanel.isInTrainingSet(ConvertNumToFilename(stage))) {
						g.setColor(InTrainingSetBackground);
						if (stage < 100)
							g.fillOval(x - 12, y - 19, 30, 30);
						else if (stage >= 1000)
							g.fillOval(x - 3, y - 19, 30, 30);
						else
							g.fillOval(x - 6, y - 19, 30, 30);
					}

					if (clickedOnImagesAsInts.contains(stage)) {
						g.setColor(InSetColor);
					} else {
						g.setColor(NotInSetColor);
					}
					String display_num;
					if (NumImages() > SmallLargeSetNumImages){
						display_num = String.valueOf(stage % 100);
					}
					else {
						display_num = String.valueOf(stage);
					}
					g.drawString(display_num, x, y);					
				}
			}
		}
	}

	@Override
	public void RunUponNewProject() {
		//only do this if overview image is null, otherwise it was run prior
		try {
			overview_image = IOTools.OpenImage(OverviewImageFilename);
		} catch (FileNotFoundException e){ //overview image not generated yet
			//two options - we've got the full directory structure or not
			if (new File(homedir + File.separator + "HPF" + File.separator + "fullres").list() == null){
				GenerateAndSaveOverViewImageFromRawSpectralFiles();
			}
			else {
				//first genereate an overview image from the hpf fullres files
				GenerateAndSaveOverviewImageFromHpfFullRes();			
				//first move all files from /hpf/raw to root
				String[] list = new File(homedir + getExtraPath()).list();
				//if these directories were already deleted, bail:
				if (list == null){
					return;
				}
				for (String filename : list){
					File to_move = new File(homedir + getExtraPath() + filename);
					to_move.renameTo(new File(homedir + File.separator + filename));
				}
				//now delete the rest of the files
				IOTools.deleteDirectory(new File(homedir + File.separator + "HPF"));
				IOTools.deleteDirectory(new File(homedir + File.separator + "LPF"));
				IOTools.deleteDirectory(new File(homedir + File.separator + "Monochrome"));
				//delete im3 files
				String[] im3_files = new File(homedir + File.separator).list(new FilenameFilter(){
					public boolean accept(File dir, String name){
						String[] fileparts=name.split("\\.");
						if (fileparts.length >= 2){
							if (fileparts[fileparts.length - 1].toLowerCase().equals("im3")){
								return true;
							}
						}
						return false;
					}		
				});
				for (String im3_file : im3_files){
					new File(homedir + File.separator + im3_file).delete();
				}
			}
		}
	}	

	private static final String OverviewImageFilename = "overview.jpg";
	
	
	private void GenerateAndSaveOverViewImageFromRawSpectralFiles(){
		overview_image = IOTools.InitializeImage(width * OverviewThumbWidth, height * OverviewThumbHeight, null, null); 
		Run.it.getGUI().setCursor(new Cursor(Cursor.WAIT_CURSOR));
		
		final NuanceImageListNew that = this;
		ExecutorService buildOverviewPool = Executors.newFixedThreadPool(Run.it.num_threads);
		for (int x = 0; x < width; x++){
			for (int y = 0; y < height; y++){
				final int stage = stageNumberMatrix.get(x, y);
				final int final_x = x;
				final int final_y = y;
				if (stage != BAD_PIC){
					buildOverviewPool.execute(new Runnable(){
						public void run(){
							BufferedImage image = new NuanceSubImage(that, stage, false).getAsBufferedImage();
							BufferedImage thumbnail = Thumbnails.ScaleImage(image, OverviewThumbWidth / (float)image.getWidth(), OverviewThumbHeight / (float)image.getHeight());
							for (int i = 0; i < OverviewThumbWidth; i++){
								for (int j = 0; j <OverviewThumbHeight; j++){
									overview_image.setRGB(final_x * OverviewThumbWidth + i, final_y * OverviewThumbHeight + j, thumbnail.getRGB(i, j));
								}
							}
						}
					});
				}
			}
		}
		buildOverviewPool.shutdown();
		try {	         
			buildOverviewPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}		
		
	    IOTools.WriteImage(OverviewImageFilename, "JPEG", overview_image);
	    Run.it.getGUI().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));	
	}
	
	private void GenerateAndSaveOverviewImageFromHpfFullRes() {
		Run.it.getGUI().setCursor(new Cursor(Cursor.WAIT_CURSOR));
		//initialize overview image:
		final BufferedImage overview = new BufferedImage(width * OverviewThumbWidth, height * OverviewThumbHeight, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < overview.getWidth(); i++){
			for (int j = 0; j < overview.getHeight(); j++){
				overview.setRGB(i, j, Color.WHITE.getRGB());
			}
		}
		//first get all full-res images:
		String[] list = new File(homedir + File.separator + "HPF" + File.separator + "fullres").list();
		final HashMap<String, String> coordinates_to_filename = new HashMap<String, String>(list.length);
		//yeah I'm dupicating some code, I don't really care, I need this to work:
		Pattern file_regex = Pattern.compile("HP_RGB_\\d+_\\[(.*)\\].bmp", Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
		for (String filename : list){
			Matcher file_regex_matcher = file_regex.matcher(filename);
			while (file_regex_matcher.find()){
				Point coordinates = raw_coordinate_hash.get(file_regex_matcher.group(1));
				coordinates_to_filename.put(coordinates.x + "," + coordinates.y, filename);
			}
		}
		
		//thread the building of it:
		ExecutorService buildOverviewPool = Executors.newFixedThreadPool(Run.it.num_threads);		
		for (final Point coords : raw_new_nuance_list.keySet()) {
			final String filename = coordinates_to_filename.get(coords.x + "," + coords.y);
			buildOverviewPool.execute(new Runnable(){
				public void run(){
					BufferedImage image = null;
					try {
						image = IOTools.OpenImage("HPF" + File.separator + "fullres" + File.separator + filename);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					int stage_x = coords.x - xmin;
					int stage_y = coords.y - ymin;
					BufferedImage thumbnail = Thumbnails.ScaleImage(image, OverviewThumbWidth / (float)image.getWidth(), OverviewThumbHeight / (float)image.getHeight());
					for (int i = 0; i < OverviewThumbWidth; i++){
						for (int j = 0; j <OverviewThumbHeight; j++){
							overview.setRGB(stage_x * OverviewThumbWidth + i, stage_y * OverviewThumbHeight + j, thumbnail.getRGB(i, j));
						}						
					}
				}	
			});
		}
		buildOverviewPool.shutdown();
		try {	         
			buildOverviewPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}			    
	    IOTools.WriteImage(OverviewImageFilename, "JPEG", overview);
	    Run.it.getGUI().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	//don't do anything special when closing:
	protected void CloseOverviewImageSpecificToSet(){}

	@Override
	protected String getExtraPath() {
		return File.separator + "HPF" + File.separator + "raw" + File.separator;
	}
}