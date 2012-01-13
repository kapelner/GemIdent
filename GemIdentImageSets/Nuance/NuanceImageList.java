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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import GemIdentTools.Matrices.IntMatrix;

/**
 * Handles image sets created by a modified CRi Nuance Bliss Image scanner (the older setup)
 * 
 * see <a href="http://www.cri-inc.com/products/nuance.asp">the Nuance scanner webpage</a>
 * 
 * This class is not fully documented since it implements proprietary features
 * for a proprietary hardware setup
 * 
 * @author Adam Kapelner
 */
public final class NuanceImageList extends NuanceImageListInterface {
	private static final long serialVersionUID = 247353837457680201L;
	// the overview image
	protected transient NuanceSubImage overview_image;
	
	public NuanceImageList(){}
	
	public NuanceImageList(String homedir) {
		super(homedir);
	}

	protected void BuildImageList(){
		GetNameAndInitFilename();
		if (errorstring == null) {
			GetRawInfoFromInitFile(); // list is built in sequential order
										// starting at 0,...,N
			FindMinsAndMaxs();
			MakeNumTable();
		}		
	}
	
	private void FindMinsAndMaxs() {

		// xmin = ymin = Integer.MAX_VALUE;
		xmax = ymax = Integer.MIN_VALUE;

		for (NuanceRawImageInfo I : list) { // find maxs
			if (I.piclocx > xmax)
				xmax = I.piclocx;
			if (I.piclocy > ymax)
				ymax = I.piclocy;
			// if (I.piclocx < xmin) xmin = I.piclocx;
			// if (I.piclocy < ymin) ymin = I.piclocy;
		}
	}

	private void MakeNumTable() {
		width = xmax + 1;
		height = ymax + 1;
		stageNumberMatrix = new IntMatrix(xmax + 1, ymax + 1, BAD_PIC);
		for (NuanceRawImageInfo I : list) {
			stageNumberMatrix.set(I.piclocx, I.piclocy, I.num);
		}
		// stageNumberMatrix.print();
	}

	private void GetNameAndInitFilename() {
		// get first file in directory
		String[] scanfiles = (new File(homedir + File.separator))
				.list(new NuanceScanFileFilter());
		if (scanfiles.length == 0) {
			errorstring = "No CRI scan file found. Scan files end in \".scan\"";
		} else if (scanfiles.length > 1) {
			errorstring = "More than one CRI scan file found. Please eliminate all but the most accurate scan file.";
		} else {
			INIT_FILENAME = scanfiles[0];
			String[] parts = INIT_FILENAME.split("\\.");
			String[] pieces = parts[0].split("_");
			// pull out set_name and mixing information
			project_name = pieces[0] + "_" + pieces[1] + "_" + pieces[2];
			if (pieces.length == 4) {
				if (pieces[3].equals("Unmixed"))
					mixed = false;
				else
					mixed = true;
			}
		}
	}
	
	/**
	 * This {@link java.io.FilenameFilter file filter} returns only scan files
	 * of type "scan"
	 * 
	 */
	private static class NuanceScanFileFilter implements FilenameFilter {
		/**
		 * Given a file, returns true if it is a scan file
		 * 
		 * @param dir
		 *            the directory the file is located in
		 * @param name
		 *            the file itself
		 * @return whether or not the file is a scan text file
		 */
		public boolean accept(File dir, String name) {
			String[] fileparts = name.split("\\.");
			if (fileparts.length == 2) {
				String ext = fileparts[1].toLowerCase();
				if (ext.equals("scan"))
					return true;
				else
					return false;
			} else
				return false;
		}
	}

	/**
	 * "NDInfoFile", Version 1.0 "Description", Scan of a lymph node at 10x at 8
	 * wavelengths. "DoTimelapse", TRUE "NTimePoints", 1 "DoStage", TRUE
	 * "NStagePositions", 122 "Stage1", "Row0_Col0" . . . "Stage121",
	 * "Row10_Col10" "Stage122", "Overview" "DoWave", TRUE "NWavelengths", 4
	 * "WaveName1", "Methyl Green" "WaveName2", "DAB" "WaveName3", "Ferengi
	 * Blue" "WaveName4", "Vulcan Red" "DoZSeries", TRUE "NZSteps", 1
	 * "ZStepSize", 0 "WaveInFileName", FALSE "EndFile"
	 */
	private void GetRawInfoFromInitFile() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(getFilenameWithHomePath(INIT_FILENAME)));
			while (true) {
				String line = in.readLine();
				if (line == null) {
					break;
				}
				if (safesubstring(line, 1, 12).equals("Description")) {
					project_description = line.split(", ")[1];
					if (Pattern.compile("20x").matcher(project_description)
							.find()) {
						zoomlevel = ZoomLevelTwentyX;
						distance_to_pixel_conversion = MAGN_AT_20X;
					} else {
						zoomlevel = ZoomLevelTenX;
						distance_to_pixel_conversion = MAGN_AT_10X;
					}
				} else if (safesubstring(line, 1, 9).equals("WaveName")) {
					int wave_number = Integer.parseInt(safesubstring(line, 9, 10));
					String wave_name = line.split(", ")[1];
					wave_name = wave_name.replaceAll("\"", "");
					// now initialize all hashmaps that depend on the wave_name
					InitWaveInfo(wave_number, wave_name);
				} else if (safesubstring(line, 1, 6).equals("Stage")) {
					ProcessIndividualGroup(line);
				}
			}
			in.close();
		} catch (IOException e) {
			System.out.println("could not find " + INIT_FILENAME);
			e.printStackTrace();
		}
	}
	


	private void ProcessIndividualGroup(String line) {
		NuanceRawImageInfo subimage = new NuanceRawImageInfo();

		String[] segments = line.split(", "); // ["\"Stage1\"", "\"Row0_Col0\""]
		// get the stage number first:
		String stage = segments[0].substring(1, segments[0].length() - 1); // get rid of quotes
		subimage.num = Integer.parseInt(stage.substring(5, stage.length()));
		// now get the row and col (piclocx/y):
		if (segments[1].equals("\"Overview\"")) // meaningless for GemIdent
			return;
		String[] rowcol = segments[1].substring(1, segments[1].length() - 1).split("_"); // get rid of quotes then split row col string
		subimage.piclocy = Integer.parseInt(rowcol[0].substring(3, rowcol[0].length()));
		subimage.piclocx = Integer.parseInt(rowcol[1].substring(3, rowcol[1].length()));
		list.add(subimage);
	}
	
	private String BeginningFileName() {
		String filename = project_name;
		if (mixed)
			filename += "_Mixed";
		else
			filename += "_Unmixed";
		return filename;
	}

	protected String ConvertNumAndWaveToFileName(int stage, int wavenum) {
		return BeginningFileName() + "_w" + wavenum + "_s" + stage + "_t1.tif";
	}
	
	protected Integer GetStageNumberFromOverviewImageGivenCoordinate(int i, int j){
		double pixels_per_x = overview_image.getWidth() / (double) (width);
		double pixels_per_y = overview_image.getHeight() / (double) (height);
		int x = (int) Math.round((i - pixels_per_x / 2) / pixels_per_x);
		int y = (int) Math.round((j - pixels_per_y / 2) / pixels_per_y);
		return GetStageFromThumbnailCoordinates(x, y);
	}
	
	protected Integer GetStageFromThumbnailCoordinates(int x, int y){
		return x * height + y + 1; // add 1 because first image is "1" not		
	}
	
	public String getInitializationFilename() {
		return INIT_FILENAME;
	}
	
	@Override
	public BufferedImage getGlobalThumbnailImage(Float approxGlobalScaledWidth) {
		// load up image, build distr's and get it as a buffered image
		if (overview_image == null){ //cache it
			overview_image = new NuanceSubImage(this, width * height + 1, false); // not cropped!!
			overview_image.BuildDisplayAndPixelDistrs();
		}
		BufferedImage image = overview_image.getAsBufferedImage();
		DrawGlobalThumbnailOverlay(image.getGraphics());
		return image;
	}	
	
	private static final int XoFor10X = 67;
	private static final int XfFor10X = 68;
	private static final int YoFor10X = 50;
	private static final int YfFor10X = 50;

	private static final int XoFor20X = 58;
	private static final int XfFor20X = 64;
	private static final int YoFor20X = 51;
	private static final int YfFor20X = 61;
	
	public int getXo() {
		if (zoomlevel == ZoomLevelTenX)
			return XoFor10X;
		else
			return XoFor20X;
	}

	public int getXf() {
		if (zoomlevel == ZoomLevelTenX)
			return XfFor10X;
		else
			return XfFor20X;
	}

	public int getYo() {
		if (zoomlevel == ZoomLevelTenX)
			return YoFor10X;
		else
			return YoFor20X;
	}

	public int getYf() {
		if (zoomlevel == ZoomLevelTenX)
			return YfFor10X;
		else
			return YfFor20X;
	}	
	
	//related to overview image:
	protected void DrawGlobalThumbnailOverlay(Graphics g) {
		double font_size;
		if (NumImages() < SmallLargeSetNumImages) {
			font_size = FontSizeForOverlaySmallSet;
		} else {
			font_size = FontSizeForOverlayLargeSet;
		}
		g.setColor(Color.BLACK);
		g.setFont(new Font("Serif", Font.PLAIN, (int) font_size));
		double pixels_per_x = overview_image.getWidth() / (double) (width);
		double pixels_per_y = overview_image.getHeight() / (double) (height);
		int counter = 1; // first image is #1
		for (double i = pixels_per_x / 2 - 1; i < overview_image.getWidth(); i += pixels_per_x) {
			for (double j = pixels_per_y / 2 - 1 + font_size / 2; j < overview_image.getHeight(); j += pixels_per_y) {
				int x = (int) Math.round(i);
				int y = (int) Math.round(j);
				// if it's in the training set, draw a green circle behind it
				if (trainPanel.isInTrainingSet(ConvertNumToFilename(counter))) {
					g.setColor(InTrainingSetBackground);
					if (counter < 100)
						g.fillOval(x - 12, y - 19, 30, 30);
					else if (counter >= 1000)
						g.fillOval(x - 3, y - 19, 30, 30);
					else
						g.fillOval(x - 6, y - 19, 30, 30);
				}

				if (clickedOnImagesAsInts.contains(counter)) {
					g.setColor(InSetColor);
				} else
					g.setColor(NotInSetColor);
				String display_num;
				if (NumImages() > SmallLargeSetNumImages){
					display_num = String.valueOf(counter % 100);
				}
				else {
					display_num = String.valueOf(counter);
				}
				g.drawString(display_num, x, y);
				counter++;
			}
		}
	}
	
	protected void CloseOverviewImageSpecificToSet() {
		if (JOptionPane.showConfirmDialog(overview_image_frame, "Would you like to delete the files that were not clicked on?\n\nThis cannot be undone . . .\n\nBE VERY CAREFUL", "File deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			if (JOptionPane.showConfirmDialog(overview_image_frame, "You have just chosen to delete many files FOREVER.\n\nMake sure you are:\na) at the microscope room\nb) have just imaged this tissue\nc) are positive the images in red are extraneous and unneeded\n\nIF YOU PRESS YES, YOU WILL HAVE TO REIMAGE THE WHOLE TISSUE IF YOU MAKE A MISTAKE", "FINAL CHECK!!!", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				// get all stages
				HashSet<Integer> notClickedOnImagesAsInts = new HashSet<Integer>();
				for (int i = 0; i < width; i++)
					for (int j = 0; j < height; j++)
						notClickedOnImagesAsInts.add(stageNumberMatrix.get(i, j));
				// subtract stages we need
				for (Integer stage : clickedOnImagesAsInts) {
					for (Integer local_stage : GetLocalPics(null, stage).values()) {
						notClickedOnImagesAsInts.remove(local_stage);
					}
				}
				// delete the rest of the files that are unneeded
				for (Integer stage : notClickedOnImagesAsInts) {
					for (Integer wavenum : wave_names.keySet()) {
						new File(homedir + File.separator + ConvertNumAndWaveToFileName(stage, wavenum)).delete();
					}
				}
			}
		}		
	}

	@Override
	public void RunUponNewProject(){}

	@Override
	protected String getExtraPath() {
		return File.separator;
	}	
}
