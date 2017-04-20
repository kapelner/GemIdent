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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import GemIdentClassificationEngine.Classify;
import GemIdentModel.Phenotype;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.IntMatrix;
import GemIdentTools.Matrices.SimpleMatrix;
import GemIdentTools.Thumbnails;
import GemIdentView.GUIFrame;

/**
 * Functions and infrastructure that deal with all types of image sets.
 * By necessity, it contains infrastructure for global image sets. If you 
 * want this shut off, just have a list of non-abstract final classes that you iterate
 * and check instanceof. Because Java does not support multiple inheritance,
 * this design structure was chosen
 * 
 * @author Adam Kapelner
 */
public abstract class ImageSetInterface implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/** the filename of the log history */
	private static final String log_file_name_suffix = "log.txt";
	/** the directory where output from the analysis panel is placed */
	public static final String checkDir = "checks";
	
	/** the directory where this image set resides */
	protected String homedir;
	/** The distance unit - microns, feet, etc */
	protected String measurement_unit;
	/** The format of the images - jpg, bmp, tiff, etc */
	protected String image_format;	
	/** The conversion from distance in native units to distance in pixels */
	protected Double distance_to_pixel_conversion = .47; //defualt value in um/px
	/** the phenotype traning numbers to date */
	protected transient HashMap<String, Integer> phenotype_training;
	/** the phenotype traning numbers to date */
	protected transient HashMap<String, Integer> phenotype_training_previous;
	/** The centroid finding method to use */
	protected int centroidFinderMethod;
	
	/** the exclusion rules: rule name --> "if phenotype name" --> "exluded phenotype name --> num pixel exclusions */
	protected ArrayList<ExclusionRuleStruct> exclusion_rules;
	
	public ImageSetInterface(){}
	
	/** Width of subimages - to be assigned during construction by the daughter class */
	protected static int W;
	/** Height of subimages - to be assigned during construction by the daughter class */
	protected static int H;
	
	/** To indicate an image is not present when investigating the global image */
	public transient static final String PIC_NOT_PRESENT="NOT_HERE";
	/** The universal way of indicating that a picture does not exist at a location */
	public transient static final int BAD_PIC=-1;	
	/** the filename of the global thumbnail */
	public transient static final String globalFilenameAlone="global.jpg";
	/** the filename and path of the global thumbnail */
	public transient static final String globalFilenameAndPath=Thumbnails.thumbnailDir+File.separator+globalFilenameAlone;
	/** The name of the training helper file (without the size) */
	public transient static final String GlobalFilename="Global";

	/**
	 * A convenience for iterating over size information
	 * for training set helper creation
	 * 
	 * @author Adam Kapelner
	 */
	public enum Size {
		XL,L,M,S,XS,XXS
	}	
	
	/** the list of all image objects in this image set that contain info about where the image is in global context */
	protected ArrayList<MiniImageInImageSet> list;
	/** the minimum x-coordinate of an image in the context of the global image */
	protected int xmin;
	/** the minimum y-coordinate of an image in the context of the global image */
	protected int ymin;
	/** the maximum x-coordinate of an image in the context of the global image */
	protected int xmax;
	/** the maximum y-coordinate of an image in the context of the global image */
	protected int ymax;	
	/** the width of the global image */
	protected int width;
	/** the height of the global image */
	protected int height;
	
	/** the dialog that allows the user to create {@link ExclusionRuleStruct exclusion rules} */
	private transient JFrame exclusion_rule_dialog;
	/** the panel that allows the user to create {@link ExclusionRuleStruct exclusion rules} */
	private transient JPanel exclusion_rule_dialog_internals;
	
	public ImageSetInterface(String homedir){
		this.homedir = homedir;
	}
	
	/**
	 * Adds a message to the log
	 * 
	 * @param message	the message to add
	 */
	@SuppressWarnings("deprecation")
	public void LOG_AddToHistory(String message){
		//create a timestamp for this history
		Timestamp t = new Timestamp(System.currentTimeMillis());
		String time = t.getHours() + ":";
		int min = t.getMinutes();
		if (min < 10)
			time += "0" + min;
		else
			time += min;
		time += " " + t.toString().split(" ")[0];
		//write to the file,make dir and file first if they don't exist
		if (!IOTools.DoesDirectoryExist(checkDir))
			(new File(getFilenameWithHomePath(checkDir))).mkdir();
		PrintWriter out=null;
		try {
			out=new PrintWriter(new BufferedWriter(new FileWriter(getFilenameWithHomePath(checkDir + File.separator + Run.it.projectName + "_" + log_file_name_suffix), true)));
		} catch (IOException e) {
			System.out.println("log file can't be edited");
		}
		if (message.charAt(message.length() - 1) != '\n'){
			message += "\n";
		}
		
		out.print(message);
		out.print(time + "\n\n");		
		out.close();		
			
	}
	
	/** common code that translates the point to it's global location */
	protected Point getTrueLocation(Point L, Point to, boolean excise){
		if (!excise)			
			return new Point(
				(L.x*W
				+to.x)
				,
				(L.y*H
				+to.y)
			);
		else
			return new Point(
				(L.x*W
				-(L.x*getXo())
				-((L.x-1)*(getXf()))
				+to.x)
				,
				(L.y*H
				-(L.y*getYo())
				-((L.y-1)*(getYf()))
				+to.y)
			);		
	}

	
	/**
	 * Converts a native distance into a discrete number of pixels
	 * 
	 * @param len		the native distance
	 * @return			the discrete pixel distance
	 */
	public int ToPixelsFromNativeAsInteger(double len){
		return (int)Math.round(ToPixelsFromNative(len));
	}	
	/**
	 * Converts a native distance into a pixel distance
	 * 
	 * @param len		the native distance
	 * @return			the exact pixel distance
	 */
	public double ToPixelsFromNative(double len){
		return len/distance_to_pixel_conversion;
	}
	
	/**
	 * Converts a pixel distance into a native distance
	 * 
	 * @param len		the pixel distance
	 * @return			the native distance
	 */
	public double ToNativeFromPixels(double len){
		return len*distance_to_pixel_conversion;
	}

	/**
	 * Gets the file from the scan that has the geographic information of the global set 
	 * (different for each scanning machine) with its hard drive path
	 * 
	 * @return		the path of the initialization file
	 */
	public String getInitializationFilenameWithPath(){
		return homedir + File.separator + getInitializationFilename();
	}

	/**
	 * does the initialization file exist?
	 * 
	 * @return	true if yes, false if no
	 */
	public boolean doesInitializationFileExist(){
		return IOTools.DoesSystemFileExist(getInitializationFilenameWithPath());
	}

	public String getHomedir() {
		return homedir;
	}

	public void setHomedir(String homedir) {
		this.homedir = homedir;
	}
	
	/**
	 * returns the filename in the context of its system directory on the running
	 * computer
	 * 
	 * @return		the filename with the path of the home directory
	 */
	public String getFilenameWithHomePath(String filename){
		return homedir + File.separator + filename;
	}
	
	/** makes sure the substring function doesn't throw an error */
	protected String safesubstring(String line, int a, int b){
		try {
			return line.substring(a, b);
		} catch (Exception e){
			return "                                                         "; //this a long empty string that hopefully won't be equivalent to anything
		}
	}

	public String getMeasurement_unit() {
		return measurement_unit;
	}

	public void setMeasurement_unit(String measurement_unit) {
		this.measurement_unit = measurement_unit;
	}

	public String getImage_format() {
		return image_format;
	}

	public void setImage_format(String image_format) {
		this.image_format = image_format;
	}

	public Double getDistance_to_pixel_conversion() {
		return distance_to_pixel_conversion;
	}

	public void setDistance_to_pixel_conversion(Double distance_to_pixel_conversion) {
		this.distance_to_pixel_conversion = distance_to_pixel_conversion;
	}

	public int getXmin() {
		return xmin;
	}

	public void setXmin(int xmin) {
		this.xmin = xmin;
	}

	public int getYmin() {
		return ymin;
	}

	public void setYmin(int ymin) {
		this.ymin = ymin;
	}

	public int getXmax() {
		return xmax;
	}

	public void setXmax(int xmax) {
		this.xmax = xmax;
	}

	public int getYmax() {
		return ymax;
	}

	public void setYmax(int ymax) {
		this.ymax = ymax;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Get all the images not yet classified
	 * 
	 * @return	all the images in this set not classified yet
	 */
	public Collection<String> getImagesNotClassifiedYet(){
		Collection<String> all = GetImages();
		ArrayList<String> classified = new ArrayList<String>();
		for (String filename : all)
			if (PicWasClassified(filename))
				classified.add(filename);
		all.removeAll(classified);
		return all;
	}
	
	/**
	 * Check whether or not a given image has been classified
	 * 
	 * @param filename		the image to be checked
	 * @return				whether or not this image has been classified
	 */
	public boolean PicWasClassified(String filename){
		for (String phenotype:Run.it.getPhenotyeNamesSaveNONAndFindPixels())
			if (!IOTools.FileExists(Classify.GetIsName(filename, phenotype)))
				return false;
		return true;
	}

	/** before the imageset is saved, run this function */
	public void presave(){
		LinkedHashMap<String, Phenotype> phenotypes = Run.it.getPhenotypes();
		boolean save = true;
		if (phenotype_training == null){
			phenotype_training = new HashMap<String, Integer>();
			save = false;
		}
		String message = "At Save, Training points for the " + Run.it.getPhenotypeTrainingImages().size() + " images that contain points:\n";
		for (String name : phenotypes.keySet()){
			int total_points = phenotypes.get(name).getTotalPoints();
			if (!phenotype_training.containsKey(name)){
				phenotype_training.put(name, total_points);
			}
			
//			int diff = total_points - phenotype_training.get(name);
//			String plusminus = "";
//			if (diff > 0){
//				plusminus += "+";
//			}
			message += name + ": " + total_points + "\n"; //+ " (" + plusminus + diff + ")\n";
		}
		//now just calculate the sum
		int sum = 0;
		for (Phenotype phenotype : phenotypes.values()){
			sum += phenotype.getTotalPoints();
		}
		message += "______________ Total: " + sum + "\n";
		
		if (save){
			LOG_AddToHistory(message);
		}
	}
	
	private static final int DefaultNumPixelsInExclusionRule = 15;
	/** spawn the dialog that allows for users specifying exclusion rules */
	public void RespawnExclusionRuleDialog() {
		//big reset
		if (exclusion_rule_dialog != null){
			exclusion_rule_dialog.setVisible(false);
			exclusion_rule_dialog.dispose();
		}
		//create it anew:
		exclusion_rule_dialog = new JFrame();
		exclusion_rule_dialog.setTitle("Exclusion Rules");
		exclusion_rule_dialog.setLayout(new BorderLayout());
		ReconstructExclusionDialogInternals();
		exclusion_rule_dialog.add(exclusion_rule_dialog_internals, BorderLayout.CENTER);
		exclusion_rule_dialog.pack();
		exclusion_rule_dialog.setVisible(true);
		exclusion_rule_dialog.requestFocus();
	}

	/** build the internals for the dialog that allows for users to specify exclusion rules */
	private void ReconstructExclusionDialogInternals() {
		final ImageSetInterface that = this;
		
		exclusion_rule_dialog_internals = new JPanel();
		exclusion_rule_dialog_internals.setLayout(new BorderLayout());
		
		//get just the names in order, and in an array
		Set<String> phenotype_names = Run.it.getPhenotyeNamesSaveNON();
	    
	    Box new_rule_box = Box.createVerticalBox();
	     new_rule_box.add(new JLabel("\nif phenotype"));
	    
	    final ButtonGroup if_phenotype_group = new ButtonGroup();
	    for (String name : phenotype_names){
	    	JRadioButton button = new JRadioButton(name);
	    	button.setActionCommand(name);
	    	if_phenotype_group.add(button);
	    	new_rule_box.add(button);
	    }
	    new_rule_box.add(new JLabel("appears then"));
	    new_rule_box.add(new JLabel("exclude phenotype"));
	    
	    final ButtonGroup exclude_phenotype_group = new ButtonGroup();
	    for (String name : phenotype_names){
	    	JRadioButton button = new JRadioButton(name);
	    	button.setActionCommand(name);
	    	exclude_phenotype_group.add(button);
	    	new_rule_box.add(button);
	    }
	    
	    new_rule_box.add(new JLabel("within"));
	    final JSpinner num_pixels_spinner = new JSpinner(new SpinnerNumberModel(DefaultNumPixelsInExclusionRule, 5, 100, 1));
	    new_rule_box.add(num_pixels_spinner);
	    num_pixels_spinner.setSize(40, 10);
	    new_rule_box.add(new JLabel("pixels\n"));
	    JButton create_new_rule = new JButton("Create Rule");
	    create_new_rule.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					that.AddNewExclusionRule(if_phenotype_group.getSelection(), exclude_phenotype_group.getSelection(), (Integer)num_pixels_spinner.getValue());
				}
			}
		);
	    new_rule_box.add(create_new_rule);
	    
	    GUIFrame new_rule_frame = new GUIFrame("Create a new rule");
	    new_rule_frame.add(new_rule_box);
	    exclusion_rule_dialog_internals.add(new_rule_frame, BorderLayout.NORTH);
	    
	    //initialize the data if need be
	    if (exclusion_rules == null){
	    	exclusion_rules = new ArrayList<ExclusionRuleStruct>();
	    }

	    //list rules now if necessary
	    if (exclusion_rules.size() > 0){
	    	Box rules_container = Box.createVerticalBox();
	    	
	    	int n = 1;
	    	for (final ExclusionRuleStruct rule : exclusion_rules){
	    		GUIFrame rule_frame = new GUIFrame("Rule #" + n);
	    		Box box = Box.createHorizontalBox();
	    		JButton kill = new JButton();
	    		kill.setIcon(new ImageIcon(IOTools.OpenSystemImage("ex.gif")));
	    		kill.addActionListener(
    				new ActionListener(){
    					public void actionPerformed(ActionEvent e){
    						that.RemoveExclusionRule(rule);
    					}
    				}
    			);
	    		box.add(kill);
	    		box.add(Box.createHorizontalStrut(10));
	    		String desc = "<html>If <b>" + rule.if_phenotype + "</b> appears<br>then exclude <b>" + rule.exclude_phenotype + "</b><br>within <b>" + rule.num_pixels + "</b> pixels</html>";
	    		box.add(new JLabel(desc));
	    		rule_frame.add(box);
	    		rules_container.add(rule_frame);
	    		n++;
	    	}	    	
	    	exclusion_rule_dialog_internals.add(rules_container, BorderLayout.CENTER);
	    }
	}

	/** remove an exclusion rule and repaint the dialog */
	protected void RemoveExclusionRule(ExclusionRuleStruct rule) {
		exclusion_rules.remove(rule);
		RespawnExclusionRuleDialog();
	}

	/**
	 * Create an exclusion rule, then repaint dialog
	 * 
	 * @param if_phenotype			the button that represents the "if phenotype"
	 * @param exclude_phenotype		the button that represents the "exclude phenotype"
	 * @param num_pixels			the number of pixels to exclude up to	
	 */
	protected void AddNewExclusionRule(ButtonModel if_phenotype, ButtonModel exclude_phenotype, int num_pixels) {
		String error_message = null;
		if (if_phenotype == null){
			error_message = "You must select the \"if phenotype\"";
		}
		else if (exclude_phenotype == null){
			error_message = "You must select the \"exclude phenotype\"";
		}
		else if (if_phenotype.getActionCommand().equals(exclude_phenotype.getActionCommand())){
			error_message = "The two phenotypes cannot be the same";
		}		
		if (error_message != null){
			JOptionPane.showMessageDialog(exclusion_rule_dialog, error_message, "Invalid entry", JOptionPane.ERROR_MESSAGE);
		}
		else {
			ExclusionRuleStruct rule = new ExclusionRuleStruct();
			rule.if_phenotype = if_phenotype.getActionCommand();
			rule.exclude_phenotype = exclude_phenotype.getActionCommand();
			rule.num_pixels = num_pixels;
			exclusion_rules.add(rule);
			RespawnExclusionRuleDialog();
		}
	}

	public ArrayList<ExclusionRuleStruct> getExclusion_rules() {
	    //initialize the data if need be
	    if (exclusion_rules == null){
	    	exclusion_rules = new ArrayList<ExclusionRuleStruct>();
	    }		
		return exclusion_rules;
	}

	public void setExclusion_rules(ArrayList<ExclusionRuleStruct> exclusion_rules) {
		this.exclusion_rules = exclusion_rules;
	}

	public int getCentroidFinderMethod() {
		return centroidFinderMethod;
	}

	public void setCentroidFinderMethod(int centroidFinderMethod) {
		this.centroidFinderMethod = centroidFinderMethod;
	}

	/** run this function after a new project is created */
	public abstract void RunUponNewProject();

	/** run this function after the thumbnails have finished being created */
	public abstract void ThumbnailsCompleted();
	
	
	/**
	 * Get the filenames of all the images in this set
	 * 
	 * @return	a list of the filenames
	 */
	public abstract ArrayList<String> GetImages();

	/**
	 * Get the set of the relevant colors / filters in this image set
	 * 
	 * @return	a set of the names of the filters / colors
	 */
	public abstract Set<String> getFilterNames();

	/**
	 * The number of filters / colors
	 * 
	 * @return	the number of filters / colors
	 */
	public abstract int NumFilters();
	
	/**
	 * Given a coordinate in a certain image, this will return the location in the
	 * global image context
	 *  
	 * @param filename	the image of consideration 
	 * @param to			the coordinate in the image
	 * @param excise		whether or not to excise the edges when calculating the true location
	 * @return				the true global coordinate
	 */
	public abstract Point getTrueLocation(String filename, Point to, boolean excise);
	/**
	 * The left of the image may need to be cropped. This will return the first pixel
	 * @return	the first column from the left
	 */
	public abstract int getXo();
	
	/**
	 * The right of the image may need to be cropped. This will return the last pixel
	 * @return	the last column on the right
	 */
	public abstract int getXf();
	
	/**
	 * The top of the image may need to be cropped. This will return the first pixel
	 * @return	the first row from the top
	 */
	public abstract int getYo();
	
	/**
	 * The bottom of the image may need to be cropped. This will return the last pixel
	 * @return	the last row from the bottom
	 */
	public abstract int getYf();
	
	/**
	 * It may be important to know the image neighborhood in the context of the global image.
	 * If there is no subimage in a certain location, the PIC_NOT_PRESENT will be returned in its
	 * place, otherwise the filename of the image will be returned
	 * 
	 * @param filename		the subimage of interest (could be a String or an Integer depending on image type			
	 * @return				the 3x3 matrix of filenames where the subimage of interest is in the center (either a StringMatrix or IntMatrix depending on image set)
	 */
	public abstract SimpleMatrix GetLocalPics(String filename, Integer picnum);
	
	/**
	 * An array with the filenames of all the subimages in their proper global context is
	 * essential to piecing the images together (to be overriden)
	 * 
	 * @return			a matrix of filenames of the subimages in their global context. If a file
	 * 					is not present at a given location, the PIC_NOT_PRESENT will be in its place
	 */
	public abstract SimpleMatrix getPicFilenameTable();
	
	/**
	 * Gets the file from the scan that has the geographic information of the global set 
	 * (different for each scanning machine)
	 * 
	 * @return		the name of the initialization file
	 */
	public abstract String getInitializationFilename();	

	/**
	 * Gets the width of the entire global image in pixels
	 * 
	 * @param excise		indicates whether or not the subimages are cropped in the global image
	 * @return				the pixel width
	 */
	public abstract int getGlobalWidth(boolean excise);
	
	/**
	 * Gets the height of the entire global image in pixels
	 * 
	 * @param excise		indicates whether or not the subimages are cropped in the global image
	 * @return				the pixel height
	 */
	public abstract int getGlobalHeight(boolean excise);
	
	/**
	 * Returns a global image "slice" - the true global image specified between two rows
	 * @param rowA		the beginning row of the slice
	 * @param rowB		the end row of the slide
	 * @return			a "slice" of the global image
	 */
	public abstract BufferedImage getGlobalImageSlice(int rowA, int rowB);
	
	/**
	 * Creates a viewable HTML that shows thumbnails of the global image in the global image
	 * context. The thumbnails are linked to the actual files for zoomed viewing. The HTML also
	 * gives multiple options for the size of the thumbnails. This "training helper" is discussed
	 * in the manual in section 3.1.2
	 * 
	 * @param tWidth		the original thumbnail width
	 * @param tHeight		the original thumbnail height
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public abstract void CreateHTMLForComposite(int tWidth, int tHeight);
	
	/**
	 * Creates a thumbnail of the global images, not valid in all image sets
	 * 
	 * @param approxGlobalScaledWidth	the width of the thumbnail
	 * @return							the thumbnail itself
	 */
	public abstract BufferedImage getGlobalThumbnailImage(Float approxGlobalScaledWidth);

	/**
	 * Very important function: given a large image, create score matrices for each
	 * relevant filter or color where each pixel corresponds to the pixel in the image
	 * 
	 * @param superImage		the large image to generate scores for
	 * @return					the scores as a map from filter name --> score matrixe
	 */
	public abstract HashMap<String, IntMatrix> GenerateStainScores(SuperImage superImage);
	
	/**
	 * We'll let the image set determine the file --> image interface
	 * 
	 * @param filename   	the filename / codename of the iamge
	 * @return  			the dataimage of interest
	 */
	public abstract DataImage getDataImageFromFilename(String filename);	
	
	/**
	 * Get the list of images that the user specifically selected in a global image view helper
	 * 
	 * @return	a list of the filenames
	 */
	public abstract Collection<String> getClickedonimages();	

	/** spawn a "sophisticated helper" dialog that allows for spotting colocalizations between filters */
	public abstract void spawnSophisticatedHelper();
	
	/**
	 * Given a color / filter, get its representation color
	 * 
	 * @param wave_name		the color / filter name
	 * @return				the color the user designated to represent this color by
	 */
	public abstract Color getWaveColor(String wave_name);		
}