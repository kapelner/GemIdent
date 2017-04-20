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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.JScrollPaneWithScreenshot;
import GemIdentImageSets.MiniImageInImageSet;
import GemIdentImageSets.OverviewImageAbsentException;
import GemIdentImageSets.SuperImage;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.IntMatrix;
import GemIdentTools.Matrices.StringMatrix;
import GemIdentView.GUIFrame;
import GemIdentView.InitScreen;
import GemIdentView.KFrame;
import GemIdentView.KTrainPanel;
import GemIdentView.ScrollablePicture;

/**
 * Handles image sets created by a modified CRi Nuance Bliss Image scanner. The hardware has
 * gone through two revisions, so this base class contains the common functionality for both.
 * 
 * see <a href="http://www.cri-inc.com/products/nuance.asp">the Nuance scanner webpage</a>
 * 
 * This class is not fully documented since it implements proprietary features
 * for a proprietary hardware setup
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public abstract class NuanceImageListInterface extends ImageSetInterface implements Serializable {

	protected static final ArrayList<Color> ColorList = new ArrayList<Color>(6);
	/**
	 * I chose not to make two more classes and make this abstract instead I use
	 * tokens to depict whether this cri set is a 10x or 20x scan, I may change
	 * it to two daughter classes
	 */
	public static final int ZoomLevelTenX = 10, ZoomLevelTwentyX = 20;
	/**
	 * The magnification at 20X in microns/pixel (conversion got from Bacus Labs -
	 * change this soon)
	 */
	protected static final double MAGN_AT_20X = .47;
	/**
	 * The magnification at 10X in microns/pixel (conversion got from Bacus Labs -
	 * change this soon)
	 */
	protected static final double MAGN_AT_10X = .47 * 2;
	/** token that represents that this range element has not been initialized */
	protected static final int UnitializedRange = -999;

	static {
		W = 1392;
		H = 1024;
		ColorList.add(new Color(0, 150, 0)); // green
		ColorList.add(new Color(150, 0, 0)); // red
		ColorList.add(new Color(0, 0, 150)); // blue
		ColorList.add(new Color(150, 120, 20)); // brown
		ColorList.add(new Color(150, 0, 150)); // magenta
		ColorList.add(new Color(0, 150, 150)); // turquoise
		ColorList.add(new Color(0, 75, 200)); // other stuff:
		ColorList.add(new Color(75, 0, 200));
		ColorList.add(new Color(200, 75, 0));
		ColorList.add(new Color(200, 0, 75));
	}

	/**
	 * Contains information on each image created by the Nuance workstation.
	 * This is basically a dumb struct which relies on the default constructor
	 */
	protected class NuanceRawImageInfo implements MiniImageInImageSet {

		/** The stage number of the image, also called the "picture number" */
		public int num;
		/**
		 * In the matrix of individual images within the global image, this is
		 * the x location of this image
		 */
		public int piclocx;
		/**
		 * In the matrix of individual images within the global image, this is
		 * the y location of this image
		 */
		public int piclocy;

		public int xLoc() {
			return piclocx * W - getXo() - getXf();
		}

		public int yLoc() {
			return piclocy * H - getYo() - getYf();
		}
	}

	protected String INIT_FILENAME;

	/** the zoom level of this scan - either 10 or 20x */
	protected int zoomlevel;

	protected transient ArrayList<NuanceRawImageInfo> list;
	protected String project_name;
	protected String project_description;
	protected boolean mixed;
	protected IntMatrix stageNumberMatrix;
	protected LinkedHashMap<Integer, String> wave_names;
	protected LinkedHashMap<String, Color> wave_color_representation;
	protected LinkedHashMap<String, Boolean> wave_visible;
	// stores the intensities
	protected HashMap<String, int[]> intensityranges;
	// the images to classify
	protected HashSet<Integer> clickedOnImagesAsInts;

	protected transient String errorstring;
	protected transient JFrame adjust_color_dialog;

	// the frame and the panel that stores the aforementioned overview image
	protected transient JFrame overview_image_frame;
	protected transient JScrollPaneWithScreenshot viewpane_for_overview_image;
	// the flag that indicates whether or not the user has touched the click set
	protected transient boolean click_set_changed_flag;

	protected KTrainPanel trainPanel;

	protected transient NuanceDetailedTrainingHelper detailed_trainer;

	public NuanceImageListInterface(){}

	public NuanceImageListInterface(String homedir) { //upon a new load
		super(homedir);
		measurement_unit = "\u03BCm";

		// data structures are initialized here and populated during the reading
		// of the scan file
		list = new ArrayList<NuanceRawImageInfo>();
		wave_names = new LinkedHashMap<Integer, String>();
		wave_color_representation = new LinkedHashMap<String, Color>();
		wave_visible = new LinkedHashMap<String, Boolean>();
		intensityranges = new HashMap<String, int[]>();
		clickedOnImagesAsInts = new HashSet<Integer>();	
		
		//immediately we need to build the image files:
		BuildImageList();
	}
	
	protected abstract void BuildImageList();

	protected abstract String ConvertNumAndWaveToFileName(int stage, int wavenum);

	protected void InitWaveInfo(Integer wave_number, String wave_name){
		//if wave name is not unique, ditch immediately:
		for (String wave_name_in_set : wave_names.values()){
			if (wave_name_in_set.equals(wave_name)){
				return;
			}
		}
		wave_names.put(wave_number, wave_name);
		wave_color_representation.put(wave_name, ColorList.get(wave_number - 1));
		wave_visible.put(wave_name, true);
		int[] init = { NuanceSubImage.BeginningValOnSliderA, UnitializedRange}; // java is dumb that I can't do this in one line
		intensityranges.put(wave_name, init);		
	}


	public HashMap<String, IntMatrix> getNuanceImagePixelData(int num, boolean cropped) {
		HashMap<String, IntMatrix> map = new HashMap<String, IntMatrix>(wave_names.size());
		for (int wavenum : wave_names.keySet())
			map.put(wave_names.get(wavenum), getNuanceImagePixelDataForOneWave(num, wavenum, cropped));
		return map;
	}

	public HashMap<String, IntMatrix> getNuanceImagePixelDataForSuper(int num, Integer c, NuanceSubImage C) {
		HashMap<String, IntMatrix> map = new HashMap<String, IntMatrix>(wave_names.size());
		for (int wavenum : wave_names.keySet())
			map.put(wave_names.get(wavenum), getNuanceImagePixelDataForSuperForOneWave(num, wavenum, c, C));
		return map;
	}
	
	public IntMatrix getNuanceImagePixelDataForOneWave(int num, String wave_name, boolean cropped) {
		return getNuanceImagePixelDataForOneWave(num, getWaveNum(wave_name), cropped);
	}

	public IntMatrix getNuanceImagePixelDataForOneWave(int num, int wavenum, boolean cropped) {
		Raster raster = ImageAndScoresBank.getOrAddRawNuanceImage(ConvertNumAndWaveToFileName(num, wavenum)).getData();
		// I use raster width/height because overview image is bigger than W,H
		if (cropped) {
			IntMatrix pixel_data = new IntMatrix(raster.getWidth() - getXo() - getXf(), raster.getHeight() - getYo() - getYf());
			for (int i = getXo(); i < raster.getWidth() - getXf(); i++) {
				for (int j = getYo(); j < raster.getHeight() - getYf(); j++) {
					int[] a = raster.getPixel(i, j, new int[1]);
					pixel_data.set(i - getXo(), j - getYo(), a[0]);
				}
			}
			return pixel_data;
		} 
		else {
			IntMatrix pixel_data = new IntMatrix(raster.getWidth(), raster.getHeight());
			for (int i = 0; i < raster.getWidth(); i++) {
				for (int j = 0; j < raster.getHeight(); j++) {
					try { //this is giving me crap, so just let it fail, it will only be off by one pixel anyway
						int[] a = raster.getPixel(i, j, new int[1]);
						pixel_data.set(i, j, a[0]);
					} catch (ArrayIndexOutOfBoundsException e){}
				}
			}
			return pixel_data;
		}
	}

	private IntMatrix getNuanceImagePixelDataForSuperForOneWave(int num, int wavenum, Integer c, NuanceSubImage C) {
		int W = C.getWidth();
		int H = C.getHeight();
		IntMatrix pixel_data = new IntMatrix(W + 2 * c, H + 2 * c);

		// copy C in the middle
		for (int i = c; i < c + W; i++) {
			for (int j = c; j < c + H; j++) {
				pixel_data.set(i, j, C.getIntensityAtPoint(i - c, j - c, wavenum));
				// if (i == c || j == c || i == c+W-1 || j == c+H-1) //draw some
				// line separators
				// pixel_data.set(i, j, 0);
			}
		}

		// copy surrounding images into the superimage
		IntMatrix localPics = GetLocalPics(null, num);

		int NWs = localPics.get(0, 0);
		//test if NW is here
		boolean NW_absent = false;
		NuanceSubImage NW = null;
		try {
			NW = new NuanceSubImage(this, NWs, true);
		} catch (Exception e){
			NW_absent = true;
		}
		//build it
		if (NWs == BAD_PIC || NW_absent) {
			for (int i = c; i < c + c; i++)
				for (int j = c; j < c + c; j++)
					pixel_data.set(-(i - c) + c, c - j + c, pixel_data.get(i, j));
		} else {			
			for (int i = W - c; i < W; i++)
				for (int j = H - c; j < H; j++)
					pixel_data.set(i - W + c, j - H + c, NW.getIntensityAtPoint(i, j, wavenum));
		}

		int Ns = localPics.get(1, 0);
		//test if N is here
		boolean NS_absent = false;
		NuanceSubImage N = null;
		try {
			N = new NuanceSubImage(this, Ns, true);
		} catch (Exception e){
			NS_absent = true;
		}
		//build it		
		if (Ns == BAD_PIC || NS_absent) {
			for (int i = c; i < W + c; i++)
				for (int j = c; j < c + c; j++)
					pixel_data.set(i, c - j + c, pixel_data.get(i, j));
		} else {
			for (int i = 0; i < W; i++)
				for (int j = H - c; j < H; j++)
					pixel_data.set(i + c, j - H + c, N.getIntensityAtPoint(i, j, wavenum));
		}

		int NEs = localPics.get(2, 0);
		//test if NE is here
		boolean NE_absent = false;
		NuanceSubImage NE = null;
		try {
			NE = new NuanceSubImage(this, NEs, true);
		} catch (Exception e){
			NE_absent = true;
		}
		//build it			
		if (NEs == BAD_PIC || NE_absent) {
			for (int i = W; i < W + c; i++)
				for (int j = c; j < c + c; j++)
					pixel_data.set(-i + W + c + W + c - 1, c - j + c, pixel_data.get(i, j));
		} else {
			for (int i = 0; i < c; i++)
				for (int j = H - c; j < H; j++)
					pixel_data.set(i + W + c, j - H + c, NE.getIntensityAtPoint(i, j, wavenum));
		}

		int Ws = localPics.get(0, 1);
		//test if W is here
		boolean W_absent = false;
		NuanceSubImage West = null;
		try {
			West = new NuanceSubImage(this, Ws, true);
		} catch (Exception e){
			W_absent = true;
		}
		//build it			
		if (Ws == BAD_PIC || W_absent) {
			for (int i = c; i < c + c; i++)
				for (int j = c; j < H + c; j++)
					pixel_data.set(-(i - c) + c, j, pixel_data.get(i, j));
		} else {
			for (int i = W - c; i < W; i++)
				for (int j = 0; j < H; j++)
					pixel_data.set(i - W + c, j + c, West.getIntensityAtPoint(i, j, wavenum));
		}

		int Es = localPics.get(2, 1);
		//test if E is here
		boolean E_absent = false;
		NuanceSubImage E = null;
		try {
			E = new NuanceSubImage(this, Es, true);
		} catch (Exception e){
			E_absent = true;
		}
		//build it		
		if (Es == BAD_PIC || E_absent) {
			for (int i = W; i < W + c; i++)
				for (int j = c; j < H + c; j++)
					pixel_data.set(-i + W + c + W + c - 1, j, pixel_data.get(i, j));
		} else {
			for (int i = 0; i < c; i++)
				for (int j = 0; j < H; j++)
					pixel_data.set(i + W + c, j + c, E.getIntensityAtPoint(i, j, wavenum));
		}

		int SWs = localPics.get(0, 2);
		//test if SW is here
		boolean SW_absent = false;
		NuanceSubImage SW = null;
		try {
			SW = new NuanceSubImage(this, SWs, true);
		} catch (Exception e){
			SW_absent = true;
		}
		//build it		
		if (SWs == BAD_PIC || SW_absent) {
			for (int i = c; i < c + c; i++)
				for (int j = H; j < H + c; j++)
					pixel_data.set(-(i - c) + c, -j + H + c + H + c - 1, pixel_data.get(i, j));
		} else {
			for (int i = W - c; i < W; i++)
				for (int j = 0; j < c; j++)
					pixel_data.set(i - W + c, j + H + c, SW.getIntensityAtPoint(i, j, wavenum));
		}

		int Ss = localPics.get(1, 2);
		//test if S is here
		boolean S_absent = false;
		NuanceSubImage S = null;
		try {
			S = new NuanceSubImage(this, Ss, true);
		} catch (Exception e){
			S_absent = true;
		}
		//build it		
		if (Ss == BAD_PIC || S_absent) {
			for (int i = c; i < W + c; i++)
				for (int j = H; j < H + c; j++)
					pixel_data.set(i, -j + H + c + H + c - 1, pixel_data.get(i,
							j));
		} else {
			for (int i = 0; i < W; i++)
				for (int j = 0; j < c; j++)
					pixel_data.set(i + c, j + H + c, S.getIntensityAtPoint(i,
							j, wavenum));
		}

		int SEs = localPics.get(2, 2);
		//test if S is here
		boolean SE_absent = false;
		NuanceSubImage SE = null;
		try {
			SE = new NuanceSubImage(this, SEs, true);
		} catch (Exception e){
			SE_absent = true;
		}
		//build it			
		if (SEs == BAD_PIC || SE_absent) {
			for (int i = W; i < W + c; i++)
				for (int j = H; j < H + c; j++)
					pixel_data.set(-i + W + c + W + c - 1, -j + H + c + H + c - 1, pixel_data.get(i, j));
		} else {
			for (int i = 0; i < c; i++)
				for (int j = 0; j < c; j++)
					pixel_data.set(i + W + c, j + H + c, SE.getIntensityAtPoint(i, j, wavenum));
		}

		return pixel_data;
	}

	protected Point FindLocalLocation(Integer picnum) {
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if (picnum == stageNumberMatrix.get(i, j))
					return new Point(i, j);
			}
		}
		return null;
	}

	@Override
	public IntMatrix GetLocalPics(String notused, Integer picnum) {
		Point t = FindLocalLocation(picnum);

		IntMatrix local = new IntMatrix(3, 3);
		local.set(1, 1, stageNumberMatrix.get(t.x, t.y));
		try {
			local.set(0, 1, stageNumberMatrix.get(t.x - 1, t.y));
		} catch (ArrayIndexOutOfBoundsException e) {
			local.set(0, 1, BAD_PIC);
		}
		try {
			local.set(2, 1, stageNumberMatrix.get(t.x + 1, t.y));
		} catch (ArrayIndexOutOfBoundsException e) {
			local.set(2, 1, BAD_PIC);
		}
		try {
			local.set(1, 0, stageNumberMatrix.get(t.x, t.y - 1));
		} catch (ArrayIndexOutOfBoundsException e) {
			local.set(1, 0, BAD_PIC);
		}
		try {
			local.set(1, 2, stageNumberMatrix.get(t.x, t.y + 1));
		} catch (ArrayIndexOutOfBoundsException e) {
			local.set(1, 2, BAD_PIC);
		}
		try {
			local.set(0, 0, stageNumberMatrix.get(t.x - 1, t.y - 1));
		} catch (ArrayIndexOutOfBoundsException e) {
			local.set(0, 0, BAD_PIC);
		}
		try {
			local.set(0, 2, stageNumberMatrix.get(t.x - 1, t.y + 1));
		} catch (ArrayIndexOutOfBoundsException e) {
			local.set(0, 2, BAD_PIC);
		}
		try {
			local.set(2, 0, stageNumberMatrix.get(t.x + 1, t.y - 1));
		} catch (ArrayIndexOutOfBoundsException e) {
			local.set(2, 0, BAD_PIC);
		}
		try {
			local.set(2, 2, stageNumberMatrix.get(t.x + 1, t.y + 1));
		} catch (ArrayIndexOutOfBoundsException e) {
			local.set(2, 2, BAD_PIC);
		}
		// local.print();
		return local;
	}

	@Override
	public ArrayList<String> GetImages() {
		ArrayList<String> images = new ArrayList<String>();
		for (int i = 0; i < width; i++){
			for (int j = 0; j < height; j++){
				int stage = stageNumberMatrix.get(i, j);
				if (stage != BAD_PIC){
					images.add(ConvertNumToFilename(stage));
				}
			}
		}
		Collections.sort(images);
		return images;
	}

	private static final String code_prefix_identifier_for_filename = "stage_";

	public static int ConvertFilenameToNum(String filename) {
		return Integer.parseInt(filename.replaceAll(code_prefix_identifier_for_filename, ""));
	}

	public static String ConvertNumToFilename(Integer num) {
		String leading_zeroes = "";
		if (num < 10)
			leading_zeroes = "000";
		else if (num < 100)
			leading_zeroes = "00";
		else if (num < 1000)
			leading_zeroes = "0";
		return code_prefix_identifier_for_filename + leading_zeroes + num;
	}

	@Override
	public Set<String> getFilterNames() {
		return new HashSet<String>(wave_names.values());
	}

	public DataImage getDataImageFromFilename(String codename) {
		return new NuanceSubImage(this, codename, true); // all we not be
															// cropped
	}

	@Override
	public int NumFilters() {
		return wave_names.size();
	}

	public int NumImages() {
		return width * height;
	}

	@Override
	public HashMap<String, IntMatrix> GenerateStainScores(SuperImage superImage) {
		return ((SuperNuanceImage) superImage).getPixelData(); // pretty simple - the benefits of no color training!
	}

	public boolean isMixed() {
		return mixed;
	}

	public void setMixed(boolean mixed) {
		this.mixed = mixed;
	}

	public Collection<String> getWaveNames() {
		return wave_names.values();
	}

	public Color getWaveColor(String wave_name) {
		return wave_color_representation.get(wave_name);
	}

	public void setWaveColor(String wave_name, Color color) {
		wave_color_representation.put(wave_name, color);
	}

	public boolean getWaveVisible(String wave_name) {
		return wave_visible.get(wave_name);
	}

	public void setWaveVisible(String wave_name, boolean visible) {
		wave_visible.put(wave_name, visible);
	}
	
	public void setAllVisible(){
		for (String wave_name : wave_visible.keySet()){
			wave_visible.put(wave_name, true);
		}
	}

	public String getWaveName(int wavenum) {
		return wave_names.get(wavenum);
	}
	private Integer getWaveNum(String wave_name_to_seek){
		for (int wave_num : wave_names.keySet()){
			String wave_name = wave_names.get(wave_num);
			if (wave_name.equals(wave_name_to_seek))
				return wave_num;
		}
		return null;
	}

	@Override
	// we don't need to implement this for Nuance lists now
	public BufferedImage getGlobalImageSlice(int rowA, int rowB) {
		return null;
	}

	@Override
	public int getGlobalHeight(boolean excise) {
		if (excise)
			return (H - getYo() - getYf()) * height;
		return H * height;
	}

	@Override
	public int getGlobalWidth(boolean excise) {
		if (excise)
			return (W - getXo() - getXf()) * width;
		return W * width;
	}

	@Override
	public StringMatrix getPicFilenameTable() {
		return null;
	}

	@Override
	public Point getTrueLocation(String filename, Point to, boolean excise) {
		return getTrueLocation(FindLocalLocation(ConvertFilenameToNum(filename)), to, excise);
	}
	
	public Point getTrueLocation(Point L, Point to, boolean excise) {
		if (L == null)
			return null;
		else
			return super.getTrueLocation(L, to, excise);
	}	
	
	public Point getTrueLocation(int stage, Point to, boolean excise) {
		return getTrueLocation(FindLocalLocation(stage), to, excise);
	}

	@Override
	// we don't have to implement this - there's no color training
	public void CreateHTMLForComposite(int width, int height) {}
	
	public void setIntensityA(String wave_name, int a) {
		intensityranges.get(wave_name)[0] = a;
	}

	public void setIntensityB(String wave_name, int b) {
		intensityranges.get(wave_name)[1] = b;
	}

	public int getIntensityA(String wave_name) {
		return intensityranges.get(wave_name)[0];
	}

	public int getIntensityB(String wave_name) {
		return intensityranges.get(wave_name)[1];
	}

	public boolean intensityRangeUnitialized(String wave_name) {
		int[] range = intensityranges.get(wave_name);
		if (range[0] == UnitializedRange || range[1] == UnitializedRange)
			return true;
		return false;
	}

	public IntMatrix getStageNumberMatrix() {
		return stageNumberMatrix;
	}

	public void setStageNumberMatrix(IntMatrix stageNumberMatrix) {
		this.stageNumberMatrix = stageNumberMatrix;
	}

	public LinkedHashMap<Integer, String> getWave_names() {
		return wave_names;
	}

	public void setWave_names(LinkedHashMap<Integer, String> wave_names) {
		this.wave_names = wave_names;
	}

	public LinkedHashMap<String, Color> getWave_color_representation() {
		return wave_color_representation;
	}

	public void setWave_color_representation(
			LinkedHashMap<String, Color> wave_color_representation) {
		this.wave_color_representation = wave_color_representation;
	}

	public LinkedHashMap<String, Boolean> getWave_visible() {
		return wave_visible;
	}
	
	public void setWave_visible(LinkedHashMap<String, Boolean> wave_visible) {
		this.wave_visible = wave_visible;
	}

	public HashMap<String, int[]> getIntensityranges() {
		return intensityranges;
	}

	public void setIntensityranges(HashMap<String, int[]> intensityranges) {
		this.intensityranges = intensityranges;
	}

	public String getErrorstring() {
		return errorstring;
	}

	public String getProject_name() {
		return project_name;
	}

	public void setProject_name(String project_name) {
		this.project_name = project_name;
	}

	public String getProject_description() {
		return project_description;
	}

	public void setProject_description(String project_description) {
		this.project_description = project_description;
	}

	public int getZoomlevel() {
		return zoomlevel;
	}

	public void setZoomlevel(int zoomlevel) {
		this.zoomlevel = zoomlevel;
	}

	public void SpawnAdjustColorDialog(NuanceSubImage nuanceSubImage, KTrainPanel trainPanel) {
		this.trainPanel = trainPanel;
		if (adjust_color_dialog == null || !adjust_color_dialog.isVisible()) {
			nuanceSubImage.CreateAdjustColorDialog(adjust_color_dialog);
			adjust_color_dialog.setVisible(true);
		}
		adjust_color_dialog.requestFocus(); // switch to this window please
	}

	public void BuildDisplayImagesFromWaves(boolean update_browser_and_thumbnails) {
		final DataImage current_image = trainPanel.getActiveImageAsDataImage();
		((NuanceSubImage) current_image).BuildDisplayImageFromWaves();
		trainPanel.repaintImagePanel();
		trainPanel.repaintMagnifier();
		
		if (update_browser_and_thumbnails){
			new Thread() { // we thread this away not to interrupt the user
				public void run() {
					setPriority(Thread.MIN_PRIORITY);
					Collection<DataImage> images = trainPanel.getTrainedImagesAsDataImages().values();
					images.remove(current_image);
					for (DataImage image : images)
						((NuanceSubImage) image).BuildDisplayImageFromWaves();
					trainPanel.buildExampleImages();
					trainPanel.buildThumbnailsFromDataImages();
					trainPanel.repaintBrowser();
					trainPanel.repaintThumbnails();
	//				trainPanel.repaintMagnifier();
				}
			}.start();
		}
	}

	public void setAdjust_color_dialog(JFrame adjust_color_dialog) {
		this.adjust_color_dialog = adjust_color_dialog;
	}

	public void addOrRemoveClickedOnImage(Integer i) {
		if (clickedOnImagesAsInts.contains(i))
			clickedOnImagesAsInts.remove(i);
		else
			clickedOnImagesAsInts.add(i);
	}

	public static final int ScrollbarThickness = 30;
	private static transient Point begin_drag;
	private static transient Point current_drag;
	
	public void TrashOverviewImage(){
		if (overview_image_frame != null) {
			overview_image_frame.setVisible(false);
			overview_image_frame.dispose();
		}
		overview_image_frame = new JFrame();
	}
	
	public JFrame SpawnOverviewImage(final KTrainPanel trainPanel) throws OverviewImageAbsentException {
		this.trainPanel = trainPanel;
		
		//kill sophisticated training image
		if (detailed_trainer != null){
			detailed_trainer.trashImageWindow();
		}
		
		if (clickedOnImagesAsInts == null){
			clickedOnImagesAsInts = new HashSet<Integer>();
		}
		
		if (overview_image_frame == null || !overview_image_frame.isVisible()) {
			overview_image_frame = new JFrame();
			final String title_string = "Overview of entire scan (double click on those that you want to add to the training set and left click on those images you wish to be classified,";
			overview_image_frame.setTitle(title_string + " " + clickedOnImagesAsInts.size() + " total)");

			// set up scrollable pic with the overview image
			BufferedImage overview_image = getGlobalThumbnailImage(null);
			if (overview_image == null){
				throw new OverviewImageAbsentException();
			}
			final ScrollablePicture scrollable = IOTools.GenerateScrollablePicElement(overview_image);
			// set up listeners
			scrollable.addMouseListener(new MouseListener(){
				public void mouseClicked(MouseEvent e){}
				public void mouseEntered(MouseEvent e){}
				public void mouseExited(MouseEvent e){}
				public void mousePressed(MouseEvent e) {
					begin_drag = e.getPoint();
					current_drag = e.getPoint();
				}
				public void mouseReleased(MouseEvent e) {
					ClickedOnOrDraggedOnOverviewImage(title_string, scrollable, e);
					begin_drag = null;
					current_drag = null;
					UpdateThumbnailOverlay(scrollable);					
				}
			});
			scrollable.addMouseMotionListener(new MouseMotionListener(){
				public void mouseDragged(MouseEvent e) {
					if (!e.getPoint().equals(current_drag)){
						current_drag = e.getPoint();
						scrollable.repaint();
					}
					
//					UpdateThumbnailOverlay(scrollable);	
					DrawDragBox(scrollable.getGraphics());
					
				}
				public void mouseMoved(MouseEvent e){}				
			});
			overview_image_frame.addWindowListener(new WindowListener(){
				public void windowActivated(WindowEvent arg0){}
				public void windowClosed(WindowEvent arg0){}
				public void windowClosing(WindowEvent arg0){
					CloseOverviewImage(overview_image_frame);
				}
				public void windowDeactivated(WindowEvent arg0){}
				public void windowDeiconified(WindowEvent arg0){}
				public void windowIconified(WindowEvent arg0){}
				public void windowOpened(WindowEvent arg0){}
			});

			// now give the frame an optimal size
			Dimension view_size = scrollable.getDimensionOfImage();
			Dimension new_size = new Dimension();
			if (view_size.width > KFrame.frameSize.width) {
				new_size.width = KFrame.frameSize.width;
			} else {
				new_size.width = view_size.width + ScrollbarThickness;
			}
			if (view_size.height > KFrame.frameSize.height) {
				new_size.height = KFrame.frameSize.height;
			} else {
				new_size.height = view_size.height + ScrollbarThickness;
			}

			// set up the scrollpane inside the frame
			viewpane_for_overview_image = new JScrollPaneWithScreenshot();
			viewpane_for_overview_image.setViewportView(scrollable);
			overview_image_frame.add(viewpane_for_overview_image);
			// now set up the frame to be viewed
			overview_image_frame.setResizable(true);
			overview_image_frame.setSize(new_size);	
			overview_image_frame.setVisible(true);
			overview_image_frame.repaint();
			//center it
			viewpane_for_overview_image.getHorizontalScrollBar().setValue((scrollable.getWidth() - new_size.width) / 2);
			viewpane_for_overview_image.getVerticalScrollBar().setValue((scrollable.getHeight() - new_size.height) / 2);
			//save the image
			UpdateThumbnailOverlay(scrollable);
		}	
		return overview_image_frame;
	}
	
	protected abstract Integer GetStageNumberFromOverviewImageGivenCoordinate(int i, int j);

	protected abstract Integer GetStageFromThumbnailCoordinates(int x, int y);
	
	public String CheckIfStageExists(ScrollablePicture scrollable, Integer stage){		
		// now check if the files associated with this stage and the surrounding stages exist
		for (Integer local_stage : GetLocalPics(null, stage).values()) {
			if (local_stage == BAD_PIC) // if there's nothing bordering it in this direction, ignore it
				continue;
			for (Integer wavenum : wave_names.keySet()) {
				String filename = ConvertNumAndWaveToFileName(local_stage, wavenum);
				if (!IOTools.FileExists(filename)) {
					return filename;
				}
			}
		}
		return null;
	}

	private void ClickedOnOrDraggedOnOverviewImage(String title_string, ScrollablePicture scrollable, MouseEvent e) {

		//two cases:
		//a) user just clicked
		//b) user made a rectangle
		int[] dims = DimsOfUsersRectangle();
		if (Math.abs(dims[0] - dims[1]) < 5 && Math.abs(dims[2] - dims[3]) < 7){ //a
			int stage = GetStageNumberFromOverviewImageGivenCoordinate(dims[0], dims[2]);
			//if it aint valid, bail:
			if (stage == BAD_PIC){
				return;
			}
			String filename_if_it_exists = CheckIfStageExists(scrollable, stage);
			if (filename_if_it_exists != null){
				JOptionPane.showMessageDialog(overview_image_frame, "the file \"" + filename_if_it_exists + "\" does not exist.\nYou must've deleted it previously . . .");
				clickedOnImagesAsInts.remove(stage); // make sure it is indeed removed
				UpdateThumbnailOverlay(scrollable);
				return; // we're not going to do anything else
			}
			// now handle the click event:
			if (e.getClickCount() == 2) { // ie "double" click
				overview_image_frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				trainPanel.AddNewThumbnail(ConvertNumToFilename(stage));
				overview_image_frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}			
			//always do "single" click
			click_set_changed_flag = true;
			addOrRemoveClickedOnImage(stage);
			overview_image_frame.setTitle(title_string + " " + clickedOnImagesAsInts.size() + " total)"); 
			//now do an update
			UpdateThumbnailOverlay(scrollable);
		}
		else { //b
			// get stages for all points inside the bounds of the rectangle
			HashSet<Integer> in_box_stages = new HashSet<Integer>();			
			for (int i = dims[0]; i <= dims[1]; i++){
				for (int j = dims[2]; j <= dims[3]; j++){
					in_box_stages.add(GetStageNumberFromOverviewImageGivenCoordinate(i, j));
				}
			}
			//if the user selected a file that doesn't exist, add it to the bad_stages
			HashMap<Integer, String> bad_stages = new HashMap<Integer, String>();
			for (int stage : in_box_stages){
				for (Integer local_stage : GetLocalPics(null, stage).values()) {
					if (local_stage == BAD_PIC) // if there's nothing bordering it in this direction, ignore it
						continue;
					for (Integer wavenum : wave_names.keySet()) {
						String filename = ConvertNumAndWaveToFileName(local_stage, wavenum);
						if (!IOTools.FileExists(filename)) {
							bad_stages.put(stage, filename);
						}
					}
				}
			}
			//now add all valid stages to the click set and update the image
			in_box_stages.removeAll(bad_stages.keySet());
			for (Integer good_stage : in_box_stages){
				addOrRemoveClickedOnImage(good_stage);
			}
			UpdateThumbnailOverlay(scrollable);
			//let the user know that there were bad files
			if (bad_stages.size() > 0){
				String filenames = "";
				for (String filename : bad_stages.values()){
					filenames += filename + "\n";
				}
				JOptionPane.showMessageDialog(overview_image_frame, "the file(s)\n\n" + filenames + "\n\n do not exist. You must've deleted them previously . . .");
			}
			click_set_changed_flag = true;		
		}
	}

	private void UpdateThumbnailOverlay(ScrollablePicture scrollable) {
		// always give an updated image to the user
		DrawGlobalThumbnailOverlay(scrollable.getImageIconGraphics());	
		scrollable.repaint();
		//save the image
		new Thread(){
			public void run(){
				if (!IOTools.DoesDirectoryExist(checkDir))
					(new File(getFilenameWithHomePath(checkDir))).mkdir();				
				viewpane_for_overview_image.saveScreenshot(checkDir + File.separator + Run.it.projectName + "_" + "training_set_helper");
			}
		}.start();
		
		// you can save after this
		Run.it.getGUI().EnableSave();
	}
	
	protected static final double FontSizeForOverlaySmallSet = 12;
	protected static final double FontSizeForOverlayLargeSet = 10;
	protected static final int SmallLargeSetNumImages = 3000;
	// those included in the "clicked set" appear green, otherwise reddish
	protected static final Color NotInSetColor = new Color(150, 10, 10, 220);
	protected static final Color InSetColor = new Color(10, 150, 10, 220);
	public static final Color InTrainingSetBackground = new Color(0, 50, 0, 200);	
	protected abstract void DrawGlobalThumbnailOverlay(Graphics g);

	private void CloseOverviewImage(JFrame overview_image_frame) {
		if (click_set_changed_flag) {
			CloseOverviewImageSpecificToSet();
			click_set_changed_flag = false; // reset the flag
			LOG_AddToHistory("created / updated click set (" + clickedOnImagesAsInts.size() + ") total");
		}
	}
	
	protected abstract void CloseOverviewImageSpecificToSet();
	
	private int[] DimsOfUsersRectangle(){
		//calculate the coordinates of the box
		int[] dims = new int[4]; // xo, yo, xf, yf;
		if (begin_drag.x <= current_drag.x){
			dims[0] = begin_drag.x;
			dims[1] = current_drag.x;
		}
		else {
			dims[0] = current_drag.x;
			dims[1] = begin_drag.x;			
		}
		if (begin_drag.y <= current_drag.y){
			dims[2] = begin_drag.y;
			dims[3] = current_drag.y;
		}
		else {
			dims[2] = current_drag.y;
			dims[3] = begin_drag.y;			
		}
		return dims;
	}
	
	private static final Color DragBoxColor = new Color(100, 255, 100, 160);
	private void DrawDragBox(Graphics graphics){
		//if there is a drag box to display . . .
		if (begin_drag != null && current_drag != null){
			int[] dims = DimsOfUsersRectangle();
			//draw the drag box
			graphics.setColor(DragBoxColor);
			graphics.fillRect(dims[0], dims[2], dims[1] - dims[0], dims[3] - dims[2]);
		}
	}

	public HashSet<Integer> getClickedOnImagesAsInts() {
//		if (clickedOnImagesAsInts.size() == 0){
//			PopulateClickSetInLieuOfOverviewImage();
//		}
		return clickedOnImagesAsInts;
	}

	public HashSet<Integer> GetClickedOnImagesAsIntsAndCreateIfNecessary() {
		if (clickedOnImagesAsInts == null){
			clickedOnImagesAsInts = new HashSet<Integer>();
		}
		return clickedOnImagesAsInts;
	}	

	public void setClickedOnImagesAsInts(HashSet<Integer> clickedOnImagesAsInts) {
		this.clickedOnImagesAsInts = clickedOnImagesAsInts;
	}

	@Override
	public Collection<String> getClickedonimages() {
		LinkedHashSet<String> set = new LinkedHashSet<String>();
		ArrayList<Integer> templist = new ArrayList<Integer>(
				clickedOnImagesAsInts.size());
		for (Integer i : clickedOnImagesAsInts)
			templist.add(i);
		Collections.sort(templist); // sort them to be in order, therefore
									// classification goes faster due to caching
									// superimages
		for (Integer i : templist)
			set.add(ConvertNumToFilename(i));
		return set;
	}

	public void askAboutValidWavelengths(final InitScreen that) {
		that.disableOkayButton();
		final JFrame valid_wave_window = new JFrame();
		valid_wave_window.setLayout(new BorderLayout());
		valid_wave_window.setTitle("Chromagen selection");
		Container container = new Container();
		container.setLayout(new GridLayout(0, 2, wave_names.size() + 1, 0));
		for (final Integer wavenum : wave_names.keySet()) {
			final String wavename = wave_names.get(wavenum);
			container.add(new JLabel(wavename + ": "));
			final JCheckBox keepwave = new JCheckBox();
			keepwave.setToolTipText("Keep the chromagen " + wavename + " for data analysis");
			keepwave.setSelected(true);
			keepwave.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.DESELECTED) {
						keepwave.setEnabled(false);
						if (JOptionPane.showConfirmDialog(valid_wave_window, "Are you sure you want to discard image information for \"" + wavename + "\"? All the corresponding files will be deleted. BE VERY CAREFUL!!!", "File deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
							// first delete the files
							for (int i = 0; i < width; i++) {
								for (int j = 0; j < height; j++) {
									int stage = stageNumberMatrix.get(i, j);
									if (stage == ImageSetInterface.BAD_PIC){
										continue;
									}
									File to_delete = new File(homedir + getExtraPath() + ConvertNumAndWaveToFileName(stage, wavenum));
									to_delete.delete();
								}
							}
							// then kill the references to this wave
							wave_names.remove(wavenum);
							wave_color_representation.remove(wavename);
							wave_visible.remove(wavename);							
						} else {
							keepwave.setEnabled(true);
							keepwave.setSelected(true);
						}
					}
				}
			});
			container.add(keepwave);
		}
		// now organize it into the Jframe
		GUIFrame border = new GUIFrame("Keep the following waves");
		border.add(container);
		valid_wave_window.add(border, BorderLayout.NORTH);

		// now take care of the okay button
		JButton okay = new JButton("Okay");
		okay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				that.enableOkayButton();
				valid_wave_window.setVisible(false);
				valid_wave_window.dispose();
			}
		});
		valid_wave_window.add(okay, BorderLayout.SOUTH);

		// now setup the JFrame for viewing
		valid_wave_window.pack();
		valid_wave_window.setLocation(that.getLocation());
		valid_wave_window.setVisible(true);
	}

	public void spawnSophisticatedHelper() {
//		JOptionPane.showMessageDialog(Run.it.getGUI(),"The sophisticated training helper is not done yet. Be patient . . . ");
		final NuanceImageListInterface that = this;
		new Thread(){
			public void run(){
				Run.it.getGUI().setCursor(new Cursor(Cursor.WAIT_CURSOR));
				if (detailed_trainer == null){
					detailed_trainer = new NuanceDetailedTrainingHelper(that);
				}
				detailed_trainer.display();
				Run.it.getGUI().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));				
			}
		}.start();
	}

	public void setTrainPanel(KTrainPanel trainPanel) {
		this.trainPanel = trainPanel;
	}
	
	protected abstract String getExtraPath();
	
	public void ThumbnailsCompleted(){}		
}