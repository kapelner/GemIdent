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

package GemIdentStatistics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentTools.IOTools;

/**
 * This class is responsible for the visual popup illustrating the importances of
 * the various features in a {@link Classifier classifier}'s decision
 * 
 * @author Adam Kapelner
 */
public class VisualizeClassifierImportances {

	/** sets up a round to the nearest thousandth object */
	public static final NumberFormat ThreeDecimalDigitFormat = NumberFormat.getInstance();
	static {
		ThreeDecimalDigitFormat.setMaximumFractionDigits(3);
	}
	
	/** the error rate of the {@link Classifier classifier} */
	private String error;
	/** the importances for each feature in the {@link Classifier classifier}, scaled so that the maximum is 1 */
	private double[] scaled_importances;
	/** the illustration image */
	private BufferedImage image;
	/** the maximum x-coordinate in the illustration */
	private int maximum_radius;
	/** the width in pixels of one illustration bar */
	private int width_of_one_bar;
	/** if we're in the line datum setup */
	private Integer num_ticks_for_lines;
	private LinkedHashMap<String, Color> filterNamesToColors;
	
	/**
	 * Create a visualization of the importances of the features in the classifier
	 * 
	 * @param importances		the importances as ints
	 * @param error				the error of the classifier
	 * @param datumSetup 
	 */
	public VisualizeClassifierImportances(int[] importances, double error, DatumSetupForEntireRun datumSetupForEntireRun) {
		
		//need to iterate over all features
		ArrayList<String> feature_names = datumSetupForEntireRun.getFeatureNames();
		feature_names.size();
		
		width_of_one_bar = width_of_one_graph / (maximum_radius + 1);
		
		//create the images
		CreateBlankImage(filterNamesToColors.size());
		ScaleImportances(importances);
		Graphics g = image.getGraphics();
		g.setColor(Color.BLACK);
		int c = 0;
		for (String channel : filterNamesToColors.keySet()){
			CreateGraphAxes(c);
			AddTickMarks(c, g);
			CreateBars(c, channel, filterNamesToColors.get(channel).getRGB());
			DrawTitle(c, channel, g);
			c++;
		}
		//////stuff for lines
	}
	
	//layout params for the title:
	private static final int title_font_size = 15;
	private static final int title_off_center = 50;
	/**
	 * Draw a title for one set of axes
	 * 
	 * @param c				the color number
	 * @param channel		the color name
	 * @param g				the graphics object to draw strings on top of
	 */
	private void DrawTitle(int c, String channel, Graphics g) {
		int x = horizontal_margin + width_of_one_graph / 2 - title_off_center;
		int y = (height_of_one_graph + vertical_spacing) * c + vertical_spacing;
		
		g.setFont(new Font("Arial", Font.BOLD, title_font_size));
		g.drawString(channel + " importances", x, y);
	}

	//layout params for the x-axis labels
	private static final int tick_mark_font_size = 9;
	private static final int tick_mark_distance_under_graph = 10;
	/**
	 * Add numbers as x-axis labels
	 * 
	 * @param c		the color number
	 * @param g		the graphics object to draw strings on top of
	 */
	private void AddTickMarks(int c, Graphics g) {
		double delta = width_of_one_graph / (double)(maximum_radius + 1);
		double io = horizontal_margin + delta / 2;
		int jo = (vertical_spacing + height_of_one_graph) * (c + 1) + tick_mark_distance_under_graph;

		g.setFont(new Font("Times", Font.PLAIN, tick_mark_font_size));
		for (int r = 0; r <= maximum_radius; r++){
			String feature_name = String.valueOf(r);
			if (num_ticks_for_lines != null && r > maximum_radius - num_ticks_for_lines){
				feature_name = "L";
			}
			g.drawString(feature_name, (int)Math.round(io + delta * r), jo);
		}
	}

	/** the color of the axes in the illustration */
	private static final int axes_color = Color.BLACK.getRGB();
	/**
	 * Draw one axis on the illustration
	 * 
	 * @param c		the color number to draw the axis for
	 */
	private void CreateGraphAxes(int c) {
		int io = horizontal_margin;
		int jo = (vertical_spacing + height_of_one_graph) * c + vertical_spacing;
		for (int j = 0; j < height_of_one_graph; j++){
			image.setRGB(io, jo + j, axes_color);
		}
		for (int i = 0; i < width_of_one_graph; i++){
			image.setRGB(io + i, jo + height_of_one_graph, axes_color);
		}	
	}

	//constants that handle the layout of the illustration:
	private static final int horizontal_margin = 30;
	private static final int vertical_spacing = 30;
	private static final int height_of_one_graph = 150;
	private static final int width_of_one_graph = 400;
	
	/** the background color of the illustration */
	private static final int background_color = Color.WHITE.getRGB();
	/**
	 * Create the blank illustration image to certain dimensions based
	 * on the number of colors
	 * 
	 * @param C		the number of colors
	 */
	private void CreateBlankImage(int C) {
		int w = width_of_one_graph + 2 * horizontal_margin;
		int h = (height_of_one_graph + vertical_spacing) * C + vertical_spacing;
		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < w; i++){
			for (int j = 0; j < h; j++){
				image.setRGB(i, j, background_color);
			}
		}
	}

	/**
	 * Creates all the bars for a single color
	 * 
	 * @param c			the color number of the bars to create
	 * @param channel	the color name of the bars to create
	 */
	private void CreateBars(int c, String channel, int representation_color) {
		int io = horizontal_margin;
		int jo = (height_of_one_graph + vertical_spacing) * (c + 1);
		
		
		for (int r = 0; r <= maximum_radius; r++){
			CreateBar(io, jo, scaled_importances[c * (maximum_radius + 1) + r], channel, representation_color);
			io += width_of_one_bar;
		}
		
	}

	/** the feature with relative importance 1 only is drawn at this proportion of the height of the graph */
	private static final double highest_bar_proportion = 0.95;
	
	/**
	 * Draws one bar in the illustration
	 * 
	 * @param io						the x value of the bar
	 * @param jo						the y-coordinate to paint this bar
	 * @param d							the importance of this bar
	 * @param channel					which color does this refer to?
	 * @param representation_color		with which color should we color the bars?
	 */
	private void CreateBar(int io, int jo, double d, String channel, int representation_color) {		
		int h = (int)Math.round(d * height_of_one_graph * highest_bar_proportion);
		
		for (int i = io; i < io + width_of_one_bar; i++){
			for (int j = jo - 1; j > jo - 1 - h; j--){ //shift up by 1px to leave room for x-axis
				try {
					image.setRGB(i, j, representation_color);
				} catch (ArrayIndexOutOfBoundsException e){}
			}
		}
	}

	/**
	 * Scales the importances of each feature so that the maximum is 1 and the values are doubles
	 * 
	 * @param importances	the original importances as ints
	 */
	private void ScaleImportances(int[] importances) {
		scaled_importances = new double[importances.length];
		double max = Integer.MIN_VALUE;
		for (int importance : importances){
			if (importance > max){
				max = importance;
			}
		}
		for (int i = 0; i < importances.length; i++){
			scaled_importances[i] = importances[i] / max;
		}
	}
	
	/** the maximum height of the window */
	private static final int popup_height = 600;
	/** the title of the popup window */
	private static final String title_of_window = "Importances as a function of distance and chromagen";
	/** handles the swing code to spawn the popup after the illustration image was created */
	public void SpawnWindow(){
		JFrame frame = new JFrame();
		frame.setTitle(title_of_window + ", error: " + error);
		frame.add(new JScrollPane(IOTools.GenerateScrollablePicElement(image)));
		frame.setSize(new Dimension(image.getWidth() + 40, image.getHeight() < popup_height ? image.getHeight() : popup_height));
		// now set up the frame to be viewed
		frame.setResizable(true);
		frame.setVisible(true);
		frame.repaint();
//		IOTools.WriteImage("importances.tiff", "TIFF", image);
	}
}
