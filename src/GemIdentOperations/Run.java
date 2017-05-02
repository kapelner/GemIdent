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

import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import GemIdentClassificationEngine.*;
import GemIdentImageSets.*;
import GemIdentImageSets.Bliss.BlissImageList;
import GemIdentImageSets.Nuance.NuanceImageList;
import GemIdentImageSets.Nuance.NuanceImageListInterface;
import GemIdentModel.*;
import GemIdentStatistics.*;
import GemIdentTools.*;
import GemIdentTools.Geometry.Rings;
import GemIdentTools.Geometry.Solids;
import GemIdentView.*;


/**
 * <p>
 * This class, as a static object, begins the program, 
 * provides the functionality for saving and loading, 
 * and defines program constants.
 * </p>
 * <p>
 * The one running instance, saved as the static "it," 
 * is the current <b>GemIdent</b> project and is referenced
 * all over the program. Dependency injection probably would 
 * have been a better idea.
 * </p>
 * <p>
 * The class methods provide for
 * interaction with the model package, the other operations
 * classes, as well as the view package. This is the neural center of the 
 * program - everything passes through this point in order to
 * interact with each other
 * </p>
 * 
 * @author Adam Kapelner
 */
public class Run implements Serializable{ 
	private static final long serialVersionUID = -772668422781511581L;
	
	/** begins the program - sets look and feel, sets this thread to high priority, and puts a {@link InitScreen new / open dialog} on the screen */
	public static void main(String[] args){
		ShowGplMessage();
//		System.setProperty("com.sun.media.jai.disableMediaLib", "true"); //this sometimes can be set to avoid the error
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e){
			try{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e2){}
		}
		
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		new InitScreen();
	}
	
	/** Prints the source code license to the terminal */
	private static void ShowGplMessage() {
		System.out.println("GemIdent v1.3b --- http://gemident.com");
		System.out.println("Interactive Image Segmentation Software via Supervised Statistical Learning");
		System.out.println("");		
		System.out.println("Copyright (C) 2016");
		System.out.println("Adam Kapelner, Queens College, City University of New York and");
		System.out.println("Professor Susan Holmes, Stanford University");
		System.out.println("");
		System.out.println("GemIdent comes with ABSOLUTELY NO WARRANTY. This is free software, and you are ");
		System.out.println("welcome to redistribute it under certain conditions. Please see the GPL v2");
		System.out.println("license for more information: http://www.gnu.org/licenses/gpl-2.0.txt");  
		System.out.println("");
		System.out.println("");
	}
	
	
	/** the program title */
	private static final String ProgramName = "GemIdent";
	/** the string to be used when building the window title (<b>GemIdent</b> is still in beta on the first release) */
	private static final String VER = "1.3b";
	/** The title of the program */
	public static final String Title = ProgramName + " v" + VER + " - ";	
	
	/** the default number of trees in the {@link RandomForest random forest} */
	transient public static final int DEFAULT_NUM_TREES=50;
	/** the default number of threads to use during most program operations - get the number of available processors from the OS */
	transient public static final int DEFAULT_NUM_THREADS=Runtime.getRuntime().availableProcessors();
	/** the default number of pixels to skip when {@link Classify classifying images} */
	transient public static final int DEFAULT_PIXEL_SKIP= 1;
	/** the default number of pixels to classify before {@link Classify updating the screen} */
	transient public static final int DEFAULT_R_BATCH_SIZE = 250;
	/** relevant only for CRI Nuance image sets, this is the default magnification that each picture will be loaded at (see {@link Thumbnails#ScaleImage Scale Image}) */
	transient public static final float DEFAULT_MAGN=1; //so nothing bad happens
		
	/** this stores the current <b>GemIdent</b> project statically for convenient reference anywhere in the program (otherwise it would have to be passed to all classes) */
	transient public static Run it;
	/** while the user is choosing between starting a new project or opening an old one, this stores the GUI being loaded in the background */
	transient private static KFrame preloadedGUI;

	/** the minimum number of training points required for phenotypes in order to {@link Classify run a classification on this image set for this phenotype} */
	private static final int MinimumPhenotypePointsNeeded = 10;

	//the project-specific data that is not saved / loaded:
	/** stores the entire {@link KFrame GUI} for the project} */
	transient private KFrame gui;
	/** the class responsible for coordinating classification, post-processing, or both */
	transient private SetupClassification setupClassification;
	/** did the user make a change to the data model? If so, then the user is able to save */
	transient private boolean isDirty;
	
	/** HUGE HACK: maps the phenotype id# to the phenotype name - better off making the whole thing a {@link GemIdentImageSets.ImageAndScoresBank.MapAndList MapAndList}*/
	public static HashMap<Integer,String> classMapBck;
	/** HUGE HACK: maps the phenotype name to the phenotype id# - better off making the whole thing a {@link GemIdentImageSets.ImageAndScoresBank.MapAndList MapAndList}*/
	public static HashMap<String,Integer> classMapFwd;
	/** Initializes the mappings from phenotype id# to phenotype name and vice versa */
	public static void InitializeClassPhenotypeMap() {
		classMapBck=new HashMap<Integer,String>(Run.it.numPhenotypes());
		classMapBck.put(0,Phenotype.NON_NAME);
		classMapFwd=new HashMap<String,Integer>(Run.it.numPhenotypes());
		classMapFwd.put(Phenotype.NON_NAME,0);
		int c=0;
		
		Set<String> pixelFindList=Run.it.getPhenotyeNamesSaveNONAndFindPixels();
		for (String name:Run.it.getPhenotyeNamesSaveNON()){
			if (pixelFindList.contains(name)){
				c++;
				classMapBck.put(c,name);
				classMapFwd.put(name,c);
			}
			else
				classMapFwd.put(name,0);
		}
	}
	//the data that is saved / loaded:
	
	//training data
	/** an order-preserving mapping from name of the phenotypes to the actual phenotype object */
	private LinkedHashMap<String,Phenotype> phenotypes;
	
	//other critical variables
	/** the image set */
	public ImageSetInterface imageset;	
	/** the user's project title (chosen at the {@link InitScreen New Project screen} */
	public String projectName;
	/** the digital expansion factor of each of the input images (only relevant when analyzing CRI Nuance image sets) */
	private float magn;
	
	//classify panel settings / user customizations

	/**DL4J Specific classifiers */

	/**NO LONGER NEED HEIGHT/WIDTH USING RMAX

	public int CNN_imageWid;

	public int CNN_imageHei;
	*/


	/** Number of channels */
//	public int CNN_channels;
	/** Number of examples */
	public int CNN_num_examples;
	/** Number of labels */
	public int CNN_num_labels;
	/** Batch Size         */
	public int CNN_batch_num;
	/** Iteration Number   */
	public int CNN_iter_num;
	/** Epoch Number       */
	public int CNN_epoch_num;
	/**Split train %       */
	public double CNN_split;






	/** the number of classification trees to use during classification*/
	public int num_trees;
	/** the number of threads to use during classification */
	public int num_threads;
	/** the number of pixels to skip during classification */
	public int pixel_skip;
	/** the number of pixels to process before updating the screen during classification */
	public int num_pixels_to_batch_updates;
	/** the setting for the pics to classify (see {@link KClassifyPanel#CLASSIFY_ALL all}, {@link KClassifyPanel#CLASSIFY_RANGE range}, {@link KClassifyPanel#CLASSIFY_TRAINED trained}, {@link KClassifyPanel#TEN_RANDOM 10 random}, {@link KClassifyPanel#TWENTY_RANDOM 20 random}, {@link KClassifyPanel#N_RANDOM N random} */
	public int pics_to_classify;
//	/** the text the user wrote in the {@link KClassifyPanel#rangeText classify specific images} field */
//	public String RANGE_TEXT;
	/** the text the user wrote in the {@link KClassifyPanel#nRandomText classify N random} field */
	public Integer N_RANDOM;
	/** the setting for which classification method */
	public int classification_method;
	/** the types of classification methods */
	public static final int CLASSIFIER_RF = 1;
	public static final int CLASSIFIER_CNN = 2;

	//results
	/** a mapping from the phenotype (where centroids are sought) to its total count in the last classification */
	private LinkedHashMap<String,Long> totalCounts;
	/** a mapping from the phenotype (where centroids are sought) to its false negative rate (Type I error rate) in the last classification */
	private LinkedHashMap<String,Double> errorRates;
	/** a mapping from image file to a set of coordinates in that image where the classification failed to find the user's training point (Type I error) */
	private HashMap<String,HashSet<Point>> typeOneErrors;
	
	/** preloads the {@link Solids Solids reference}, the {@link Rings Rings reference} as well as the GUI while the user is selecting to {@link InitScreen Open / Create New project} */
	public static void Preload(){
		//preload solids and rings vectors
		Thread buildStatics=(
			new Thread(){
				public void run(){
					Solids.Build();
					Rings.Build();
				}
			}
		);
		buildStatics.setPriority(Thread.MIN_PRIORITY);
				
		//preload gui components
		Thread preloadGui=(
			new Thread(){
				public void run(){
					preloadedGUI=new KFrame(); //create shell (just the pointer)
					preloadedGUI.DrawAll(); //put everything in gui (i need to ptr to exist prior to this functiobn that's why it's decomped)
				}
			}
		);
		preloadGui.setPriority(Thread.NORM_PRIORITY);
		
		buildStatics.start();
		preloadGui.start();
	}
	
	public Run(){} //serializable is happy
	
	/**
	 * Creates a new <b>GemIdent</b> project
	 * 
	 * @param projectName		the project's title
	 * @param imageset 			the image set describing the analysis
	 * @param magn				the artificial magnification (only relevant in CRI Nuance image sets)
	 */
	public Run(String projectName,ImageSetInterface imageset, float magn){
		this.projectName=projectName;
		this.imageset = imageset;
		this.magn=magn;
		
		typeOneErrors=new HashMap<String,HashSet<Point>>();
	}
	/** after the project is {@link #Run 
	 * initialized}, objects and parameters can be initialized and
	 * the gui can {@link KClassifyPanel #DefaultPopulateWindows()
	 * set up default views and the project title}
	 */
	private void BuildRunObjectFromDefaults(){			
				
		//create transient info
		CreateTransientsAndThumbnails();
		//create default info		
		phenotypes=new LinkedHashMap<String,Phenotype>();
		num_trees=Run.DEFAULT_NUM_TREES;
		num_threads=Run.DEFAULT_NUM_THREADS;
		pixel_skip=Run.DEFAULT_PIXEL_SKIP;
		num_pixels_to_batch_updates=Run.DEFAULT_R_BATCH_SIZE;
		pics_to_classify=KClassifyPanel.CLASSIFY_TRAINED;
		classification_method = CLASSIFIER_RF;
//		RANGE_TEXT="";
		
		gui.DefaultPopulateWindows(imageset);			
	}
	/**
	 * Creates the {@link Thumbnails#CreateAll thumbnails} then
	 * sets the preloaded gui to the true gui if we are loading from scratch
	 */
	private void CreateTransientsAndThumbnails(){
		gui = preloadedGUI; //preload gui
		if (!(imageset instanceof NuanceImageListInterface))
			Thumbnails.CreateAll(gui);		
		GUIsetDirty(false);	
	}	
	
	//// save / save as / load / new / exit

	/**
	 * Opens a project from the hard disk. Prompts the user
	 * to choose the "gem" file that contains the project. "gem" 
	 * files cannot be moved to other directories because they 
	 * must have the image files and the project directories
	 * that <b>GemIdent</b> creates. It then creates a Run object
	 * (see {@link #it it}) by hydrating it from the {@link IOTools#openFromXML(String)
	 * opened XML file}. It then populates the GUI, creates thumbnails
	 * if necessary, and initializes, and {@link #InitializeBasedOnImageSetType()
	 * initializes the data structure specific to the image set}
	 * 
	 * @param initscreen 		the initialization screen
	 */
	public static boolean OpenProject(InitScreen initscreen){
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		chooser.setFileFilter(
			new FileFilter(){
				public boolean accept(File f){
					if (f.isDirectory())
						return true;
					String name=f.getName();
					String[] pieces=name.split("\\.");
					if (pieces.length != 2)
						return false;
					if (pieces[1].toLowerCase().equals("gem"))
						return true;
					return false;
				}
				public String getDescription(){
					return "gem";
				}
			}
		);		
		int result = chooser.showOpenDialog(null);
		initscreen.setVisible(false);
		if ( result == JFileChooser.APPROVE_OPTION ) {
			File load_file = chooser.getSelectedFile();			
			try {
				it=(Run)IOTools.openFromXML(load_file.getAbsolutePath());
				it.imageset.setHomedir(load_file.getParent());
				if (it.typeOneErrors == null)					
					it.typeOneErrors=new HashMap<String,HashSet<Point>>();
				it.CreateTransientsAndThumbnails();				
				it.gui.OpenPopulateWindows(it.imageset);
				it.GUIsetDirty(false);
				it.gui.AddOrUpdateRetrainTab(); //if we're loading chances are we don't really care about the stain selection as a priority (saves user a click) it also sets immediately retraining ability
				it.gui.setSeeTrainingPoints(true);
				InitializeBasedOnImageSetType();
				it.gui.DisableButtons();
				it.gui.repaint();
			} catch (Exception e){
				e.printStackTrace();
				initscreen.setVisible(true);
				JOptionPane.showMessageDialog(null,"The file \""+load_file.getName()+"\" is not a GemIdent project or it is corrupted","File Error",JOptionPane.ERROR_MESSAGE);	
				return false;
			}
			it.imageset.presave();
			return true;
		}
		initscreen.setVisible(true);
		return false;
	}

	/** If the user is working with a Bacus Labs set, it
	 * initializes a {@link BlissImageList BlissImageList},
	 * if a CRI Nuance, it initializes a {@link NuanceImageList NuanceImageList}, etc
	 */
	private static void InitializeBasedOnImageSetType() {
		if (it.imageset instanceof BlissImageList){
			it.gui.buildExampleImages(true);
			try {
				it.gui.buildGlobalsViewsInAnalysis();
			} catch (Exception e){}
		}
		else if (it.imageset instanceof NuanceImageListInterface){
			((NuanceImageListInterface)it.imageset).setTrainPanel(it.gui.getTrainingPanel());
			it.gui.HideColorTrainTab();
//			it.gui.DisableTrainingHelpers();
			it.gui.buildExampleImages(false);
			it.gui.AddAdjustColorButton();
			//only for old image sets:
			it.gui.TurnOnSophisticatedTrainingHelper();
		}
		else {
			it.gui.KillAnalysisTab();
			it.gui.buildExampleImages(true);
			it.gui.DisableTrainingHelpers();
		}
	}

	/**
	 * Creates a new <b>GemIdent</b> project from the {@link InitScreen user's inputs}
	 * 
	 * @param projectName		the title of the project
	 * @param imageset			the image set object that describes the analysis set
	 * @param magn				the artificial magnification for CRI Nuance projects
	 */
	public static void NewProject(String projectName,ImageSetInterface imageset, float magn){
		it=new Run(projectName,imageset,magn);
		it.GUIsetDirty(true);		
		it.BuildRunObjectFromDefaults(); //need pointer to definitely be there...
		InitializeBasedOnImageSetType();
		//this has to run in the same thread
		it.imageset.RunUponNewProject();		
		imageset.LOG_AddToHistory("Begun Project");
	}
	
	/** 
	 * Saves the project to the hard disk by {@link IOTools#saveToXML(Object, String) 
	 * dumping to an XML file}. See section 2.2 in the manual for more information.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public static void SaveProject(){
		new Thread(){ //thread off the presave . . .
			public void run(){
				it.imageset.presave();
			}
		}.start();
		try {
			IOTools.saveToXML(it,it.imageset.getFilenameWithHomePath(it.projectName+".gem"));
		} catch (Exception e){
			JOptionPane.showMessageDialog(Run.it.getGUI(),"Error saving file","Error",JOptionPane.ERROR_MESSAGE);							
			return;
		}
		it.GUIsetDirty(false);
	}
	/** exits <b>GemIdent</b>, prompts user to save first */
	public void ExitProgram(){
		if ( it.GUIisDirty() ) {
			int result = JOptionPane.showConfirmDialog(null,
														"Would you like to save your changes?",
														"Document changed",
														JOptionPane.YES_NO_OPTION);
			if ( result == JOptionPane.YES_OPTION)
				SaveProject();
		}
		System.exit(0);
	}	
	
	/////frame stuff
	/** repaint entire GUI screen */
	public void FrameRepaint() {
		if (gui != null) // sometimes it's null
			gui.repaint();		
	}
	/** get the location of the GUI on the user's desktop */
	public Point getGuiLoc() {
		if (gui == null)
			return null;
		else
			return gui.getLocation();
	}
	public KFrame getGUI() {
		return gui;
	}
	
	/////all methods for phenotypes
	
	public int numPhenotypes(){
		return phenotypes.size();
	}
	public int numPhenotypesSaveNON(){
		return phenotypes.size()-1;
	}
	public void AddPhenotype(Phenotype phenotype) {
		phenotypes.put(phenotype.getName(),phenotype);		
	}
	public Phenotype getPhenotype(String name) {
		return phenotypes.get(name);
	}
	
	public int numPhenTrainingPoints(){
		int N=0;
		for (Phenotype phenotype:phenotypes.values())
			N+=phenotype.getTotalPoints();
		return N;
	}
	public int numPhenTrainingImages(){
		return getPhenotypeTrainingImages().size();
	}
	/** gets all filenames of images used for training phenotype examples */
	public Set<String> getPhenotypeTrainingImages(){
		HashSet<String> set=new HashSet<String>();
		for (Phenotype phenotype:phenotypes.values())
			for (TrainingImageData I:phenotype.getTrainingImages())
				set.add(I.getFilename());
		return set;
	}
	
	/** gets the "NON" phenotype */
	public Phenotype getNONPhenotype() {
		for (Phenotype phenotype:phenotypes.values())
			if (phenotype.isNON())
					return phenotype;
		return null;
	}
	public Collection<Phenotype> getPhenotypeObjects() {
		return phenotypes.values();
	}

	public Collection<Phenotype> getPhenotypesSaveNON(){
		HashSet<Phenotype> phenotypesSaveNON=new HashSet<Phenotype>();
		for (Phenotype phenotype:phenotypes.values())
			if (!phenotype.isNON())
				phenotypesSaveNON.add(phenotype);
		return phenotypesSaveNON;		
	}
	public Set<String> getPhenotyeNames(){
		return phenotypes.keySet();
	}
	public Set<String> getPhenotyeNamesSaveNON(){
		Set<String> set=phenotypes.keySet();
		Set<String> setc=new HashSet<String>();
		for (String s:set)
			if (!Phenotype.isNONNAME(s))
				setc.add(s);
		return setc;
	}
	/** gets the phenotypes that the user wants classified */
	public Set<String> getPhenotyeNamesWithFindPixels(){
		Set<String> set=phenotypes.keySet();
		Set<String> setc=new HashSet<String>();
		for (String s:set)
			if ((phenotypes.get(s)).isFindPixels())
				setc.add(s);
		return setc;
	}
	/** gets the phenotypes that the user wants classified save the NON - the negative phenotype */
	public Set<String> getPhenotyeNamesSaveNONAndFindPixels(){
		Set<String> set=getPhenotyeNamesSaveNON();
		Set<String> setc=new HashSet<String>();
		for (String s:set)
			if ((phenotypes.get(s)).isFindPixels())
				setc.add(s);
		return setc;
	}
	/** gets the phenotypes that the user wants classified and post processed for centroids (save the NON - the negative phenotype) */
	public Set<String> getPhenotyeNamesSaveNONAndFindCenters(){
		Set<String> set=getPhenotyeNamesSaveNON();
		Set<String> setc=new HashSet<String>();
		for (String s:set)
			if ((phenotypes.get(s)).isFindCentroids())
				setc.add(s);
		return setc;
	}
	public Color getPhenotypeDisplayColor(String name){
		return phenotypes.get(name).getDisplayColor();
	}
	/** the default radius multiple that <b>GemIdent</b> checks when identifying pixels */
	private static final double InfluenceRadius=1.5;
	/** the radius (in pixels) that <b>GemIdent</b> checks around when identifying pixels */
	public int getMaxPhenotypeRadiusPlusMore(Double influence_radius){
		int M=0;
		for (Phenotype phenotype:phenotypes.values()){
			if (!phenotype.isNON()){
				int Mo=phenotype.getRmax();
//				System.out.println("phenotype: " + phenotype.getName() + " rmax: " + Mo);
				if (Mo > M)
					M=Mo;
			}
		}
//		System.out.println("max radius before multiple: " + M);
		M=(int)Math.round((influence_radius == null ? InfluenceRadius : influence_radius) * M);
//		System.out.println("max radius after multiple: " + M);
		return M;
	}
	public void DeletePhenotype(String key) {
		phenotypes.remove(key);	
	}
	/** Checks if the user supplied <b>GemIdent</b> with sufficient number of
	 * training points for all phenotypes to go ahead with classification.
	 */
	public HashSet<String> EnoughPhenotypeTrainPoints() {
		HashSet<String> deliquents = new HashSet<String>();
		for (Phenotype phenotype:phenotypes.values())
			if (phenotype.getTotalPoints() < MinimumPhenotypePointsNeeded)
				deliquents.add(phenotype.getName());
		return deliquents;
	}
	/** removes a training image used for phenotype training (if there is data in it, confirms with the user) */
	public boolean RemoveImageFromPhenotypeSet(String filename) {
		boolean dialog=false;
		for (Phenotype phenotype:phenotypes.values()){
			if (phenotype.hasImage(filename)){
				TrainingImageData image=phenotype.getTrainingImage(filename);
				if (image.getNumPoints() > 0)
					dialog=true;
			}
		}
		int result=JOptionPane.YES_OPTION;
		if (dialog)
			result = JOptionPane.showConfirmDialog(it.getGUI(),
					"This image contains training data. Are you sure you want to remove it?",
					"Points deletion",
					JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION){
			for (Phenotype phenotype:phenotypes.values())
				if (phenotype.hasImage(filename))
					phenotype.RemoveTrainingImage(filename);
			GUIsetDirty(true);
			return true;
		}
		return false;
	}
	
	//////actual functional methods

	public LinkedHashMap<String,Long> getTotalCounts(){
		return totalCounts;
	}
	public LinkedHashMap<String,Double> getErrorRates(){
		return errorRates;
	}
	/** gets the collection of files to classify based on the user's choice */
	public Collection<String> GetImageListToClassify() {
		Collection<String> files=null;
		if (pics_to_classify == KClassifyPanel.CLASSIFY_ALL)
			files=imageset.GetImages();
//		else if (PICS_TO_CLASSIFY == KClassifyPanel.CLASSIFY_RANGE){
//			files=gui.getClassifyPanel().RangeToFilenames();
//			files.addAll(gui.getTrainingImageSet());
//		}
		else if (pics_to_classify == KClassifyPanel.CLASSIFY_TRAINED)
			files=gui.getTrainingImageSet();
		else if (pics_to_classify == KClassifyPanel.CLICKED_ON)
			files = imageset.getClickedonimages();
		else if (pics_to_classify == KClassifyPanel.CLASSIFY_REMAINING)
			files=imageset.getImagesNotClassifiedYet();
		else {
			ArrayList<String> all = imageset.GetImages();
			Collection<String> trained = gui.getTrainingImageSet();
			all.removeAll(trained);
			files=new HashSet<String>();
			if (pics_to_classify == KClassifyPanel.TEN_RANDOM){
				if (all.size() < 10)
					files.addAll(all);
				else {
					for (int i=0;i<10;i++){
						int k=(int)Math.floor(Math.random()*all.size());
						files.add(all.remove(k));
					}
				}
			}
			else if (pics_to_classify == KClassifyPanel.TWENTY_RANDOM){
				if (all.size() < 20)
					files.addAll(all);
				else {
					for (int i=0;i<20;i++){
						int k=(int)Math.floor(Math.random()*all.size());
						files.add(all.remove(k));
					}
				}
			}
			else if (pics_to_classify == KClassifyPanel.N_RANDOM){
				if (N_RANDOM != null){
					if (all.size() < N_RANDOM)
						files.addAll(all);
					else {
						for (int i=0;i<N_RANDOM;i++){
							int k=(int)Math.floor(Math.random()*all.size());
							files.add(all.remove(k));
						}
					}
				}
			}
			//add those trained back in only if it's not a "clicked-on set"
			files.addAll(trained); 
		}
		return files;
	}

	/**
	 * Run a classification and a post-processing
	 * 
	 * @param openProgress			the open color cubes progress bar
	 * @param trainingProgress		the create training data progress bar
	 * @param buildProgress			the construct machine learning classifiers progress bar
	 * @param classifier 			did the user load a classifier from the HD?
	 */
	public void DoBothOnSepThread(JProgressBarAndLabel openProgress,JProgressBarAndLabel trainingProgress,JProgressBarAndLabel buildProgress, Classifier classifier){
		setupClassification = new SetupClassification(this, openProgress, trainingProgress, buildProgress, true, true, classifier);		
	}
	/**
	 * Run just a classification
	 * 
	 * @param openProgress			the open color cubes progress bar
	 * @param trainingProgress		the create training data progress bar
	 * @param buildProgress			the construct machine learning classifiers progress bar
	 * @param classifier 			did the user load a classifier from the HD?
	 */
	public void DoClassificationOnSepThread(JProgressBarAndLabel openProgress,JProgressBarAndLabel trainingProgress,JProgressBarAndLabel buildProgress, Classifier classifier){
		setupClassification = new SetupClassification(this, openProgress, trainingProgress, buildProgress, true, false, classifier);		
	}
	/**
	 * Run just a post-processing
	 * 
	 * @param openProgress			the open color cubes progress bar
	 * @param trainingProgress		the create training data progress bar
	 * @param buildProgress			the construct machine learning classifiers progress bar
	 * @param classifier 			did the user load a classifier from the HD?
	 */
	public void DoPostProcessOnSepThread(JProgressBarAndLabel openProgress,JProgressBarAndLabel trainingProgress,JProgressBarAndLabel buildProgress, Classifier classifier){
		setupClassification = new SetupClassification(this, openProgress, trainingProgress, buildProgress, false, true, classifier);		
	}

	/** stop the classification or post-processing at whatever stage it's at */
	public void StopClassifying(){
		setupClassification.Stop();
	}
	
	public boolean GUIisDirty() {
		return isDirty;
	}	
	public void GUIsetDirty(boolean isDirty) {
		this.isDirty = isDirty;
		if (gui != null){
			if (!isDirty)
				gui.DisableSave();
			else
				gui.EnableSave();
		}
	}
	/** given a previous time in ms, returns the time since elapsed in hr/min/s format as a string */
	public static String TimeElapsed(long time){
		return FormatSeconds((int)(System.currentTimeMillis()-time)/1000);
	}
	/** given a time in ms, return the time in hr/min/s format as a string */
	public static String FormatSeconds(int s){
		int h=(int)Math.floor(s/((double)3600));
		s-=(h*3600);
		int m=(int)Math.floor(s/((double)60));
		s-=(m*60);
		return ""+h+"hr "+m+"m "+s+"s";
	}
		
	//getters and setters for serializable to work:
	public int getNUM_THREADS() {
		return num_threads;
	}
	public void setNUM_THREADS(int num_threads) {
		this.num_threads = num_threads;
	}
	public int getNUM_TREES() {
		return num_trees;
	}
	public void setNUM_TREES(int num_trees) {
		this.num_trees = num_trees;
	}
	public int getPICS_TO_CLASSIFY() {
		return pics_to_classify;
	}
	public void setPICS_TO_CLASSIFY(int pics_to_classify) {
		this.pics_to_classify = pics_to_classify;
	}
	public int getPIXEL_SKIP() {
		return pixel_skip;
	}
	public void setPIXEL_SKIP(int pixel_skip) {
		this.pixel_skip = pixel_skip;
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public int getR_BATCH_SIZE() {
		return num_pixels_to_batch_updates;
	}
	public void setR_BATCH_SIZE(int r_batch_size) {
		this.num_pixels_to_batch_updates = r_batch_size;
	}
//	public String getRANGE_TEXT() {
//		return RANGE_TEXT;
//	}
//	public void setRANGE_TEXT(String range_text) {
//		RANGE_TEXT = range_text;
//	}

	public void setPhenotypes(LinkedHashMap<String,Phenotype> phenotypes){
		this.phenotypes=phenotypes;
	}

	public LinkedHashMap<String,Phenotype> getPhenotypes() {
		return phenotypes;
	}


	public Integer getN_RANDOM() {
		return N_RANDOM;
	}

	public void setN_RANDOM(Integer n_random) {
		N_RANDOM = n_random;
	}

	public void setErrorRates(LinkedHashMap<String, Double> errorRates) {
		this.errorRates = errorRates;
	}
	public void resetErrorRates() {
		errorRates = null;
	}	

	public void setTotalCounts(LinkedHashMap<String, Long> totalCounts) {
		this.totalCounts = totalCounts;
	}
	public void resetTotalCounts() {
		totalCounts = null;
	}	
	public float getMagn() {
		return magn;
	}
	public void setMagn(float magn) {
		this.magn = magn;
	}

	public HashMap<String, HashSet<Point>> getTypeOneErrors() {
		return typeOneErrors;
	}

	public void setTypeOneErrors(HashMap<String, HashSet<Point>> typeOneErrors) {
		this.typeOneErrors = typeOneErrors;
	}
	public void resetTypeOneErrors() {
		typeOneErrors = new HashMap<String,HashSet<Point>>();
	}	

	public ImageSetInterface getImageset() {
		return imageset;
	}

	public void setImageset(ImageSetInterface imageset) {
		this.imageset = imageset;
	}
	
	//some convenience methods of image set types
	public ImageSetInterfaceWithUserColors getUserColorsImageset() {
		if (imageset instanceof ImageSetInterfaceWithUserColors)
			return (ImageSetInterfaceWithUserColors)imageset;
		return null;
	}	
}