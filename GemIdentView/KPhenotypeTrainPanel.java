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

package GemIdentView;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import GemIdentClassificationEngine.Classify;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.OverviewImageAbsentException;
import GemIdentImageSets.Nuance.NuanceImageListInterface;
import GemIdentImageSets.Nuance.NuanceSubImage;
import GemIdentModel.Phenotype;
import GemIdentModel.TrainSuperclass;
import GemIdentOperations.ImageSetHeuristics;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentTools.Thumbnails;

/**
 * An extension of {@link KTrainPanel the generic training panel}
 * with specific functionality for phenotype training as well as
 * phenotype retraining. See step 3 in the Algorithm section of the IEEE paper for this
 * panel's purpose and see fig. 4 in the for an example
 * of the user training. Also provides added functionality for retraining over
 * already-classified images (see step 9 in the Algorithm section of the
 * IEEE paper). For discussion on the purpose of phenotype training, consult section
 * 3.2 of the manual and for discussion on phenotype retraining, consult section 5 of
 * the manual.
 * 
 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 * 
 * @author Adam Kapelner
 *
 */
@SuppressWarnings("serial")
public class KPhenotypeTrainPanel extends KTrainPanel {

	/** the mapping from phenotype name to the alpha slider to control the mask of its classification results */
	private HashMap<String,AlphaSlider> alphaSlidersIdent;
	/** the mapping from phenotype name to the alpha slider to control the mask of its post-processing / centroids results */
	private HashMap<String,AlphaSlider> alphaSlidersCentroid;
	/** the mapping from phenotype name to the current value of the alpha slider that controls the mask of its classification results */
	private HashMap<String,Integer> alphaSlidersIdentValues;
	/** blinker checkboxes for these alpha sliders (pixels) */
	private HashMap<String, JCheckBox> alphaSlidersIdentBlinkers;
	/** blinker checkboxes for these alpha sliders (centroids) */
	private HashMap<String, JCheckBox> alphaSlidersCentroidBlinkers;
	/** the mapping from phenotype name to the current value of the alpha slider to control the mask of its post-processing / centroids results */
	private HashMap<String,Integer> alphaSlidersCentroidValues;
	/** the box that stores all the slider objects */
	private JFrame allSliderDialog;
	/** does the user want to display the type I errors found? */
	protected JCheckBox errorFinder;
	
	//the actions and keystrokes for the masks (for classification and post-processing) to in/decrement its opacity
	protected ActionWrap phen1_plus_is;
	protected ActionWrap phen1_minus_is;
	protected ActionWrap phen1_plus_isC;
	protected ActionWrap phen1_minus_isC;
	protected ActionWrap phen2_plus_is;
	protected ActionWrap phen2_minus_is;
	protected ActionWrap phen2_plus_isC;
	protected ActionWrap phen2_minus_isC;
	protected ActionWrap phen3_plus_is;
	protected ActionWrap phen3_minus_is;
	protected ActionWrap phen3_plus_isC;
	protected ActionWrap phen3_minus_isC;
	protected ActionWrap phen4_plus_is;
	protected ActionWrap phen4_minus_is;
	protected ActionWrap phen4_plus_isC;
	protected ActionWrap phen4_minus_isC;
	private KeyStroke phen1_plus_is_key;
	private KeyStroke phen1_minus_is_key;
	private KeyStroke phen1_plus_isC_key;
	private KeyStroke phen1_minus_isC_key;
	private KeyStroke phen2_plus_is_key;
	private KeyStroke phen2_minus_is_key;
	private KeyStroke phen2_plus_isC_key;
	private KeyStroke phen2_minus_isC_key;
	private KeyStroke phen3_plus_is_key;
	private KeyStroke phen3_minus_is_key;
	private KeyStroke phen3_plus_isC_key;
	private KeyStroke phen3_minus_isC_key;
	private KeyStroke phen4_plus_is_key;
	private KeyStroke phen4_minus_is_key;
	private KeyStroke phen4_plus_isC_key;
	private KeyStroke phen4_minus_isC_key;
		
	//the actions and keystrokes for choosing the next/previous boosting image 
	private ActionWrap forwBoostA;
	private ActionWrap backBoostA;
	private KeyStroke forwBoostkey;
	private KeyStroke backBoostkey;
	
	//the actions+keystrokes for altering the opacity of all sliders
	protected ActionWrap opacity0;
	protected ActionWrap opacity75pixels;
	protected ActionWrap opacity100centroids;
	protected ActionWrap opacity100both;
	private KeyStroke opacity0key;
	private KeyStroke opacity33key;
	private KeyStroke opacity66key;
	private KeyStroke opacity100key;

	//the actions and keystrokes for toggling the display of the type I errors in this image
	private KeyStroke seeTypeIerrkey;
	private ActionWrap seeTypeIerrA;
	
	/** the button for choosing the next boosting image */
	private JButton forwBoostButton;
	/** the button for choosing the previous boosting image */
	private JButton backBoostButton;
	/** the label displaying the current boosting file */
	private JLabel boostLabelImageFile;
	/** the label displaying information about the boosting collection */
	private JLabel boostLabelAll;
	/** the portion of the training panel that houses the boosting functions */
	private GUIFrame boostPanel;
	/** the ordered list of image files in the boosting collection */
	private ArrayList<String> boostList;
	/** the randomized list of image files in the boosting collection */
	private ArrayList<String> boostListR;
	/** the current boosting image */
	private int boostImageNum;
	/** the toggle for ordering / randomizing the order of the boosting collection */
	private JToggleButton randomize;
	/** the label that displays information about the number of type I errors in the current image */
	private JLabel typeILabel;

	/** default constructor and sets up boosting panel */
	public KPhenotypeTrainPanel(){
		super();		
		SetUpBoostingPanel();		
	}
	/** adds listeners for opacity changing and shortcuts for buttons */
	private static final int FullOpacity = 255;
	private static final int SeventyFivePercentOpacity = (int)Math.round(255 * .75);
	protected void AddFnListeners() {
		super.AddFnListeners();
		opacity0key=KeyStroke.getKeyStroke("F1");
		opacity33key=KeyStroke.getKeyStroke("F2");
		opacity66key=KeyStroke.getKeyStroke("F3");
		opacity100key=KeyStroke.getKeyStroke("F4");
		forwBoostkey=KeyStroke.getKeyStroke('x');
		backBoostkey=KeyStroke.getKeyStroke('z');
		seeTypeIerrkey=KeyStroke.getKeyStroke('\\');
		opacity0=new ActionWrap();
		opacity75pixels=new ActionWrap();
		opacity100centroids=new ActionWrap();
		opacity100both=new ActionWrap();
		forwBoostA=new ActionWrap();
		backBoostA=new ActionWrap();
		seeTypeIerrA=new ActionWrap();
		input_map.put(opacity0key,"opacity0");
		input_map.put(opacity33key,"opacity33");
		input_map.put(opacity66key,"opacity66");
		input_map.put(opacity100key,"opacity100");
		input_map.put(forwBoostkey,"forwBoostA");
		input_map.put(backBoostkey,"backBoostA");
		input_map.put(seeTypeIerrkey,"seeTypeIerrA");
		
		opacity0.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					if (alphaSlidersIdent != null)
						for (AlphaSlider s:alphaSlidersIdent.values())
							if (s.isEnabled())
								s.setValue(0);
					if (alphaSlidersCentroid != null)
						for (AlphaSlider s:alphaSlidersCentroid.values())
							if (s.isEnabled())
								s.setValue(0);
				}
			}
		);
		opacity75pixels.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					if (alphaSlidersIdent != null)
						for (AlphaSlider s:alphaSlidersIdent.values())
							if (s.isEnabled())
								s.setValue(SeventyFivePercentOpacity);
					if (alphaSlidersCentroid != null)
						for (AlphaSlider s:alphaSlidersCentroid.values())
							if (s.isEnabled())
								s.setValue(0);
				}
			}
		);
		opacity100centroids.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					if (alphaSlidersIdent != null)
						for (AlphaSlider s:alphaSlidersIdent.values())
							if (s.isEnabled())
								s.setValue(0);
					if (alphaSlidersCentroid != null)
						for (AlphaSlider s:alphaSlidersCentroid.values())
							if (s.isEnabled())
								s.setValue(FullOpacity);
				}
			}
		);
		opacity100both.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					if (alphaSlidersIdent != null)
						for (AlphaSlider s:alphaSlidersIdent.values())
							if (s.isEnabled())
								s.setValue(FullOpacity);
					if (alphaSlidersCentroid != null)
						for (AlphaSlider s:alphaSlidersCentroid.values())
							if (s.isEnabled())
								s.setValue(FullOpacity);
				}
			}
		);
		forwBoostA.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					forwBoostButton.doClick();
				}
			}
		);
		backBoostA.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					backBoostButton.doClick();
				}
			}
		);
		seeTypeIerrA.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					errorFinder.doClick();
				}
			}
		);
		action_map.put("opacity0",opacity0.getAction());
		action_map.put("opacity33",opacity75pixels.getAction());
		action_map.put("opacity66",opacity100centroids.getAction());
		action_map.put("opacity100",opacity100both.getAction());
		action_map.put("forwBoostA",forwBoostA.getAction());
		action_map.put("backBoostA",backBoostA.getAction());
		action_map.put("seeTypeIerrA",seeTypeIerrA.getAction());
	}
	/** resets image panel */
	protected void ResetImagePanelAndAddListeners(){
		imagePanel=new KImagePhenotypeTrainPanel(this);
		super.ResetImagePanelAndAddListeners();
	}
	/** 
	 * Creates the Add / Delete phenotype buttons and sets the correct training helper.
	 * For discussion on adding and removing phenotypes from the training, consult
	 * section 3.2.1 of the manual
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	protected void CreateWest() {		
		super.CreateWest();
		
		addClassInfoButton.setText("+ Phen");
		deleteClassInfoButton.setText("- Phen");
		
		TrainHelperButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (!(Run.it.imageset instanceof NuanceImageListInterface))
						SpawnTrainHelperDialog();					 
					else {
						try {
							((NuanceImageListInterface)Run.it.imageset).SpawnOverviewImage(that).requestFocus();
						} catch (OverviewImageAbsentException exc){
							JOptionPane.showMessageDialog(Run.it.getGUI(), "There was an error opening the overview image. Are all the files available? Try the sophisticated helper.");
						}
					}
				}	
			}
		);
	}	
	/** 
	 * in the current trained image, find all type I errors and
	 * draw a red circle around them. For more information, please
	 * consult section 5.2 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */ 
	public void EnableErrorFinding() {
    	((KImageTrainPanel)imagePanel).setSeeErrors(true);	
    	errorFinder.setSelected(true);
	}
	/** disable the error finding feature, hide the red circles */
	public void DisableErrorFinding() {
    	((KImageTrainPanel)imagePanel).setSeeErrors(false);
    	errorFinder.setSelected(false);
	}
	/** deletes a phenotype by name */
	protected void DeleteInfo(String name){
		Run.it.DeletePhenotype(name);
		super.DeleteInfo(name);
	}
	/** adds a thumbnail to the thumbnail browser of this training panel */
	public KThumbnail AddNewThumbnail(String filename){
		KThumbnail thumbnail = super.AddNewThumbnail(filename);
		if (thumbnail != null)
			UpdateThumbnailIfClassified(thumbnail,false);
		return thumbnail; //don't need to but who cares
	}
	/** 
	 * During retraining, thumbnails should indicate if the 
	 * underlying image has been classified by <b>GemIdent</b>. 
	 * See section 5.1 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private void UpdateThumbnailIfClassified(KThumbnail thumbnail,boolean add){
		String filename=thumbnail.getFilename();
		if (Run.it.imageset.PicWasClassified(filename)){
			if (IOTools.FileExists(Classify.GetIntermediateName(filename))){ //in Nuance sets they don't exist
				thumbnail.setImage(Thumbnails.makeIntoThumbnailWithI(Classify.GetIntermediateName(filename)));
			}
			thumbnail.setClassified(true);
		}
		if (add)
			thumbnailPane.addThumbnail(thumbnail,false);
		
		if (Run.it.imageset instanceof NuanceImageListInterface){
			buildThumbnailsFromDataImages();
		}
	}
	/** 
	 * spawn the special training helper for phenotype training. The user chooses
	 * the number of clusters to split the images into and then the {@link
	 * GemIdentOperations.ImageSetHeuristics ImageSetHeuristics} is computed
	 */
	protected void SpawnTrainHelperDialog() {
		final JFrame trainHelpDialog=new JFrame();
		trainHelpDialog.setTitle("Training Image Selection Aid");

		final JLabel buildingLabel=new JLabel("Building Image Set Information");

		final JProgressBar progress=new JProgressBar(0,100);
		progress.setStringPainted(true);
		
		Box north=Box.createVerticalBox();
		north.add(buildingLabel);
		north.add(progress);
		
		JLabel numLabel=new JLabel("Number of Subsets to Split Entire Set");
		final JSlider slider=new JSlider(JSlider.HORIZONTAL,1,50,10);
		slider.setMajorTickSpacing(5);
		slider.setSnapToTicks(true); 
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		
		Box center=Box.createVerticalBox();
		center.add(Box.createVerticalStrut(15));
		center.add(numLabel);
		center.add(slider);
		center.add(Box.createVerticalStrut(15));
		
		final JButton compute=new JButton("Compute");
		compute.setEnabled(false);
		
		progress.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					if (progress.getValue() == 100){
						compute.setEnabled(true);	
						progress.setVisible(false);
						trainHelpDialog.repaint();
					}
				}
			}			
		);
		progress.addComponentListener(
			new ComponentListener(){
				public void componentHidden(ComponentEvent e) {
					compute.setEnabled(true);
					buildingLabel.setVisible(false);
				}
				public void componentMoved(ComponentEvent e){}
				public void componentResized(ComponentEvent e){}
				public void componentShown(ComponentEvent e){}
			}
		);		
		final JButton cancel=new JButton("Cancel");
		cancel.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					trainHelpDialog.dispose();
				}
			}
		);		
		final ImageSetHeuristics heuristics=new ImageSetHeuristics(progress);
	
		compute.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					compute.setEnabled(false);
					heuristics.Compute(slider.getValue());	
					trainHelpDialog.dispose();
					compute.setEnabled(true);
					cancel.setVisible(false);
				}
			}
		);

		trainHelpDialog.setLayout(new BorderLayout());
		trainHelpDialog.add(north,BorderLayout.NORTH);
		trainHelpDialog.add(center,BorderLayout.CENTER);
		Box buttons=Box.createHorizontalBox();
		buttons.add(compute);
		buttons.add(cancel);
		trainHelpDialog.add(buttons,BorderLayout.SOUTH);
		
		trainHelpDialog.setVisible(true);
		trainHelpDialog.pack();		
	}
	/** Spawns a dialog that allows the user to remove a phenotype */
	protected Bundle SpawnRemoveImageDialog() {
		final Bundle bundle=super.SpawnRemoveImageDialog();	
		bundle.remove.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (Run.it.RemoveImageFromPhenotypeSet(bundle.list.getSelectedItem())){
						thumbnailPane.RemoveThumbnail(bundle.list.getSelectedItem());
						imagePanel.setDisplayImage(null);
						imagePanel.repaint();
					}					
				}
			}
		);
		return bundle;
	}
	/** adds a generic phenotype */
	protected void AddInfo(){
		AddInfo("Phenotype"+browser.getNumInfos());
	}
	/** adds a phenotype with a specific name */
	protected void AddInfo(String text){
		super.AddInfo(text);
		KPhenotypeInfo info = new KPhenotypeInfo(((KImageTrainPanel)this.imagePanel),text,this);
		this.browser.addInfoClass(info);
		Run.it.AddPhenotype((Phenotype)info.getTrainer());
	}
	/** adds a phenotype based on a {@link GemIdentModel.TrainSuperclass training model} */
	protected void AddInfo(TrainSuperclass trainer){
		KPhenotypeInfo info=new KPhenotypeInfo(((KImageTrainPanel)imagePanel),(Phenotype)trainer,this);
		browser.addInfoClass(info);
	}
	/** creates a delete info dialog with the appropriate title */
	protected void SpawnDeleteInfoDialog(){
		super.SpawnDeleteInfoDialog("Phenotype",Run.it.getPhenotyeNamesSaveNON());
	}
	
	/** when a new project has begun, the training panel is populated with the NON phenotype and one other */
	public void DefaultPopulateBrowser(){
		AddInfo(Phenotype.NON_NAME);
		AddInfo();
		CreateListeners();		
	}
	/** when a project is opened, the phenotypes are populated from the gem file's phenotype objects */
	public void PopulateFromOpen() {
		AddInfo(Run.it.getNONPhenotype());
		for (Phenotype phenotype:Run.it.getPhenotypesSaveNON())
			AddInfo(phenotype);
		CreateListeners();
		for (final String filename:Run.it.getPhenotypeTrainingImages()){
			try {
				thumbnailPane.addThumbnail(new KThumbnail(this,filename),false);
			} catch (Exception e){} //don't care if file doesn't exit
			new Thread(){ //faster loading of images
				public void run(){
					setPriority(Thread.MIN_PRIORITY);
					ImageAndScoresBank.getOrAddDataImage(filename);
//					ImageAndScoresBank.getOrAddSuperImage(filename); //this is too ambitious for now
				}
			}.start();
		}
		//need to make sure an image doesn't immediately appear/selected when project begins
		thumbnailPane.setSelectedThumbnail(null);
		imagePanel.setDisplayImage(null);
		super.PopulateFromOpen();
	}
	/** select the NON phenotype in the panel */
	public void SelectNON() {
		selectElement(browser.getFirstClassInfo());		
	}
	/** create an add phenotype dialog with the appropriate title */
	protected void SpawnAddInfoDialog(){
		super.SpawnAddInfoDialog("phenotype");
	}
	/** create the settings frame with the appropriate title */
	protected void buildSettingsBoxes(){
		super.buildSettingsBoxes("Visualize:");

	}
	/** only for Nuance sets */
	public void AddAdjustColorButton(){
		settings_box.add(new JLabel("Image Colors:",JLabel.RIGHT));
		JButton adjust_button = new JButton();
		adjust_button.setText("Adjust");
		adjust_button.setToolTipText("Adjust colors and intensity ranges for each of the chromagens");
		adjust_button.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (imagePanel.displayImage == null)
						JOptionPane.showMessageDialog(Run.it.getGUI(),"You must have an image open.");
					else {
						((NuanceImageListInterface)Run.it.imageset).SpawnAdjustColorDialog((NuanceSubImage)imagePanel.displayImage, that);
					}
				}
			}
		);
		settings_box.add(adjust_button);		
	}
	/** cycles through the thumbnails and marks them if they're classified */
	public void AdjustThumbnailPane() {
		for (KThumbnail thumbnail:thumbnailPane.getThumbnails().values()){			
			RemoveImageButton.setEnabled(true);
			UpdateThumbnailIfClassified(thumbnail,false);
		}
		Run.it.FrameRepaint(); //to show new iamges
	}
	/** sets up keys for the opacity sliders */
	public void SetUpKeyStrokesForAlphaSliders(){
		
		phen1_plus_is_key = KeyStroke.getKeyStroke('w');
		phen1_minus_is_key = KeyStroke.getKeyStroke('q');
		phen1_plus_isC_key = KeyStroke.getKeyStroke('s');
		phen1_minus_isC_key = KeyStroke.getKeyStroke('a');
		
		phen2_plus_is_key = KeyStroke.getKeyStroke('r');
		phen2_minus_is_key = KeyStroke.getKeyStroke('e');
		phen2_plus_isC_key = KeyStroke.getKeyStroke('f');
		phen2_minus_isC_key = KeyStroke.getKeyStroke('d');
		
		phen3_plus_is_key = KeyStroke.getKeyStroke('y');
		phen3_minus_is_key = KeyStroke.getKeyStroke('t');
		phen3_plus_isC_key = KeyStroke.getKeyStroke('g');
		phen3_minus_isC_key = KeyStroke.getKeyStroke('f');
		
		phen4_plus_is_key = KeyStroke.getKeyStroke('i');
		phen4_minus_is_key = KeyStroke.getKeyStroke('u');
		phen4_plus_isC_key = KeyStroke.getKeyStroke('k');
		phen4_minus_isC_key = KeyStroke.getKeyStroke('j');
		
		alphaSlidersIdentValues=new HashMap<String,Integer>();
		alphaSlidersCentroidValues=new HashMap<String,Integer>();
	}
	
	/** if none of the masks are blinking */
	private static boolean ident_or_centroid_not_blinking = true;
	/** the period of blinking for masks in milliseconds */
	private static final long BlinkerPeriod = 1000; //one second
	
	/**
	 * A convenience class that groups a check box and an {@link AlphaSlider alpha slider}
	 * and listens for changes. Each phenotype will get one of these classes.
	 * 
	 * @author Adam Kapelner
	 */
	private class AlphaIdentBlinkerListener implements ItemListener {
		
		/** should we blink this phenotype? */
		private JCheckBox check_box;
		/** what degree of transparency should we blink? */
		private AlphaSlider slider;

		public AlphaIdentBlinkerListener(JCheckBox check_box, AlphaSlider slider){
			this.check_box = check_box;
			this.slider = slider;
		}

		/** handle the checking or unchecking of a blinker checkbox */
		public void itemStateChanged(ItemEvent e) {	
			//must initialize it here to ensure all components exist!
			HashSet<Component> all_components = new HashSet<Component>();
			all_components.addAll(alphaSlidersIdent.values());
			all_components.addAll(alphaSlidersIdentBlinkers.values());
			all_components.addAll(alphaSlidersCentroid.values());
			all_components.addAll(alphaSlidersCentroidBlinkers.values());
			
			//now go for the deselection, selection
			if (e.getStateChange() == ItemEvent.DESELECTED){
		    	Deselected(all_components);					    	
		    }				    	
		    else {
		    	Selected(all_components);
		    }
			
	    	//set imagePanel to take focus next time
	    	imagePanel.ifMouseEntersTakeFocus();			
		}

		/**
		 * If user checks a phenotype to be blinked, all other components must
		 * be disabled (you can only blink one at a time).
		 * 
		 * The blinking is set up on its own thread in a while(true) loop. Upon
		 * exit, the alpha returns to what it was before.
		 * 
		 * @param all_components	all the blinker checkboxes
		 */
		private void Selected(HashSet<Component> all_components) {
	    	ident_or_centroid_not_blinking = false; //set blinking var
	    	//need to make sure others are disabled
	    	for (Component c : all_components){
	    		if (!c.equals(check_box)){
	    			c.setEnabled(false);
	    		}
	    	}

			new Thread(){
				public void run(){
					while (true){
						slider.ChangeAlphaToZero();
						try {
							Thread.sleep(BlinkerPeriod);
						} catch (InterruptedException e){}
						if (ident_or_centroid_not_blinking){
							return;
						}
						slider.ChangeAlphaBackToPreviousValue();
						try {
							Thread.sleep(BlinkerPeriod);
						} catch (InterruptedException e){}
						if (ident_or_centroid_not_blinking){
							return;
						}						
					}
				}
			}.start();
		}

		/**
		 * Upon unchecking the blinking for a phenotype, the flag gets set so the while(true) 
		 * loop is discontinued, all other checkboxes become enabled, and the alpha value gets set
		 * back to its original
		 * 
		 * @param all_components		all the blinker checkboxes
		 */
		private void Deselected(HashSet<Component> all_components) {
			ident_or_centroid_not_blinking = true; //set blinking var
	    	for (Component c : all_components){
	    		c.setEnabled(true);
	    	}
	    	//reset screen
	    	slider.ChangeAlphaBackToPreviousValue();
		}		
	}
	
	/** append the opacity sliders to the panel and link the appropriate keyboard listeners to them */
	private static final String TitleForOverlayDialog = "Classification Overlay Adjustment for Retraining";
	
	/** creates the alpha sliders and blinker checkboxes */
	protected void AddOrEditSliders(){
		
//		JLabel identLabel=new JLabel("identified pixels visibility");
		Box alphaBoxIdent=Box.createVerticalBox();
		
//		JLabel centroidLabel=new JLabel("centroid visibility");
		Box alphaBoxCentroid=Box.createVerticalBox();
		
		Collection<Phenotype> phenotypes = Run.it.getPhenotypesSaveNON();
		
		alphaSlidersIdent=new HashMap<String,AlphaSlider>(phenotypes.size());
		alphaSlidersIdentBlinkers = new HashMap<String, JCheckBox>(phenotypes.size());
		alphaSlidersCentroid=new HashMap<String,AlphaSlider>(phenotypes.size());
		alphaSlidersCentroidBlinkers = new HashMap<String, JCheckBox>(phenotypes.size());
		
		int phenNum=0;
		for (Phenotype phenotype : phenotypes){
			phenNum++;
			
			AlphaSlider ident_slider=new AlphaSlider(phenotype,false,imagePanel,alphaSlidersIdentValues);
			ident_slider.setValue(alphaSlidersIdentValues.get(phenotype.getName()));
			alphaSlidersIdent.put(phenotype.getName(),ident_slider);
			Box shortbox_ident = Box.createHorizontalBox();
			shortbox_ident.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel islabel=new JLabel(phenotype.getName() + " identified pixels visibility" + "     (blink: ");
			JCheckBox identblinker = new JCheckBox();
			identblinker.addItemListener(new AlphaIdentBlinkerListener(identblinker, ident_slider));			
			alphaSlidersIdentBlinkers.put(phenotype.getName(), identblinker);
			shortbox_ident.add(islabel);
			shortbox_ident.add(identblinker);
			shortbox_ident.add(new JLabel(")"));
			alphaBoxIdent.add(shortbox_ident);
			alphaBoxIdent.add(Box.createVerticalStrut(5));
			alphaBoxIdent.add(ident_slider);
			alphaBoxIdent.add(Box.createVerticalStrut(5));
			AlphaSlider centroid_slider=new AlphaSlider(phenotype,true,imagePanel,alphaSlidersCentroidValues);
			centroid_slider.setValue(alphaSlidersCentroidValues.get(phenotype.getName()));
			alphaSlidersCentroid.put(phenotype.getName(),centroid_slider);			
			Box shortbox_centroids = Box.createHorizontalBox();
			shortbox_centroids.setAlignmentX(Component.LEFT_ALIGNMENT);
			JLabel isCentroidlabel=new JLabel(phenotype.getName()+" centroids visibility" + "    (blink: ");
//			isCentroidlabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			JCheckBox centroidblinker = new JCheckBox();
			centroidblinker.addItemListener(new AlphaIdentBlinkerListener(centroidblinker, centroid_slider));
//			centroidblinker.setAlignmentX(Component.RIGHT_ALIGNMENT);
			alphaSlidersCentroidBlinkers.put(phenotype.getName(), centroidblinker);
			shortbox_centroids.add(isCentroidlabel);
			shortbox_centroids.add(centroidblinker);
			shortbox_centroids.add(new JLabel(")"));
			alphaBoxCentroid.add(shortbox_centroids);			
			alphaBoxCentroid.add(Box.createVerticalStrut(5));
			alphaBoxCentroid.add(centroid_slider);
			alphaBoxCentroid.add(Box.createVerticalStrut(5));
			
			switch (phenNum){
				case 1: 
					phen1_plus_is=new ActionWrap();
					phen1_minus_is=new ActionWrap();
					phen1_plus_isC=new ActionWrap();
					phen1_minus_isC=new ActionWrap();
					input_map.put(phen1_plus_is_key,"phen1_plus_is");
					input_map.put(phen1_minus_is_key,"phen1_minus_is");
					input_map.put(phen1_plus_isC_key,"phen1_plus_isC");
					input_map.put(phen1_minus_isC_key,"phen1_minus_isC");
					alphaSlidersIdent.get(phenotype.getName()).addGlobalKeyListeners(phen1_plus_is,phen1_minus_is);
					alphaSlidersCentroid.get(phenotype.getName()).addGlobalKeyListeners(phen1_plus_isC,phen1_minus_isC);
					action_map.put("phen1_plus_is",phen1_plus_is.getAction());
					action_map.put("phen1_minus_is",phen1_minus_is.getAction());
					action_map.put("phen1_plus_isC",phen1_plus_isC.getAction());
					action_map.put("phen1_minus_isC",phen1_minus_isC.getAction());
					break;
				case 2:
					phen2_plus_is=new ActionWrap();
					phen2_minus_is=new ActionWrap();
					phen2_plus_isC=new ActionWrap();
					phen2_minus_isC=new ActionWrap();
					input_map.put(phen2_plus_is_key,"phen2_plus_is");
					input_map.put(phen2_minus_is_key,"phen2_minus_is");
					input_map.put(phen2_plus_isC_key,"phen2_plus_isC");
					input_map.put(phen2_minus_isC_key,"phen2_minus_isC");
					alphaSlidersIdent.get(phenotype.getName()).addGlobalKeyListeners(phen2_plus_is,phen2_minus_is);
					alphaSlidersCentroid.get(phenotype.getName()).addGlobalKeyListeners(phen2_plus_isC,phen2_minus_isC);
					action_map.put("phen2_plus_is",phen2_plus_is.getAction());
					action_map.put("phen2_minus_is",phen2_minus_is.getAction());
					action_map.put("phen2_plus_isC",phen2_plus_isC.getAction());
					action_map.put("phen2_minus_isC",phen2_minus_isC.getAction());
					break;
				case 3:
					phen3_plus_is=new ActionWrap();
					phen3_minus_is=new ActionWrap();
					phen3_plus_isC=new ActionWrap();
					phen3_minus_isC=new ActionWrap();
					input_map.put(phen3_plus_is_key,"phen3_plus_is");
					input_map.put(phen3_minus_is_key,"phen3_minus_is");
					input_map.put(phen3_plus_isC_key,"phen3_plus_isC");
					input_map.put(phen3_minus_isC_key,"phen3_minus_isC");
					alphaSlidersIdent.get(phenotype.getName()).addGlobalKeyListeners(phen3_plus_is,phen3_minus_is);
					alphaSlidersCentroid.get(phenotype.getName()).addGlobalKeyListeners(phen3_plus_isC,phen3_minus_isC);
					action_map.put("phen3_plus_is",phen3_plus_is.getAction());
					action_map.put("phen3_minus_is",phen3_minus_is.getAction());
					action_map.put("phen3_plus_isC",phen3_plus_isC.getAction());
					action_map.put("phen3_minus_isC",phen3_minus_isC.getAction());
					break;
				case 4:
					phen4_plus_is=new ActionWrap();
					phen4_minus_is=new ActionWrap();
					phen4_plus_isC=new ActionWrap();
					phen4_minus_isC=new ActionWrap();
					input_map.put(phen4_plus_is_key,"phen4_plus_is");
					input_map.put(phen4_minus_is_key,"phen4_minus_is");
					input_map.put(phen4_plus_isC_key,"phen4_plus_isC");
					input_map.put(phen4_minus_isC_key,"phen4_minus_isC");
					alphaSlidersIdent.get(phenotype.getName()).addGlobalKeyListeners(phen4_plus_is,phen4_minus_is);
					alphaSlidersCentroid.get(phenotype.getName()).addGlobalKeyListeners(phen4_plus_isC,phen4_minus_isC);
					action_map.put("phen4_plus_is",phen4_plus_is.getAction());
					action_map.put("phen4_minus_is",phen4_minus_is.getAction());
					action_map.put("phen4_plus_isC",phen4_plus_isC.getAction());
					action_map.put("phen4_minus_isC",phen4_minus_isC.getAction());
					break;
			}			
			
		}
		
		//generate the window for the sliders, first kill the previous one:
		Point previous_dialog_location = null;
		if (allSliderDialog != null){
			previous_dialog_location = allSliderDialog.getLocation();
			allSliderDialog.dispose();
		}
		allSliderDialog = new JFrame(TitleForOverlayDialog + " (" + getCurrentTrainingImageName() + ")");
		JPanel container = new JPanel();
		Box allSliderBox = Box.createHorizontalBox();
		allSliderBox.add(alphaBoxIdent);
		allSliderBox.add(alphaBoxCentroid);
		InputMap dialog_input_map = container.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		dialog_input_map.put(opacity0key,"opacity0");
		dialog_input_map.put(opacity33key,"opacity33");
		dialog_input_map.put(opacity66key,"opacity66");
		dialog_input_map.put(opacity100key,"opacity100");
		ActionMap dialog_action_map = container.getActionMap();
		dialog_action_map.put("opacity0",opacity0.getAction());
		dialog_action_map.put("opacity33",opacity75pixels.getAction());
		dialog_action_map.put("opacity66",opacity100centroids.getAction());
		dialog_action_map.put("opacity100",opacity100both.getAction());	
		container.add(allSliderBox);
		allSliderDialog.setContentPane(container);
		allSliderDialog.pack();
		if (previous_dialog_location == null){ //put it on the top right corner
			allSliderDialog.setLocation(Run.it.getGuiLoc().x + Run.it.getGUI().getWidth(), Run.it.getGuiLoc().y);
		}
		else { //put it wherever it was before
			allSliderDialog.setLocation(previous_dialog_location);
		}
		allSliderDialog.setResizable(false);
		allSliderDialog.setVisible(true);		
		//for all the thumbnails on the bottom:
		Run.it.FrameRepaint();
		imagePanel.requestFocus(); //we want to stay focused on the image
	}
	/** remove the opacity sliders from the panel */
	public void RemoveSliders() {
		if (allSliderDialog != null){
			allSliderDialog.dispose();	
		}
	}	
	/** the label for the boosting image when boosting is not being used */
	private static final String NONE_IMAGE="<none>";
	/** add the boosting panel and initialize all its buttons and listeners */
	private void SetUpBoostingPanel(){
		boostPanel=new GUIFrame("Boost on classified images");
		Box boostBox=Box.createHorizontalBox();

		forwBoostButton=new JButton();
		forwBoostButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("forward.png")));
		forwBoostButton.setDisabledIcon(new ImageIcon(IOTools.OpenSystemImage("forwarddis.png")));
		forwBoostButton.setSize(forwBoostButton.getIcon().getIconWidth(),forwBoostButton.getIcon().getIconHeight());
		backBoostButton=new JButton();
		backBoostButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("backward.png")));
		backBoostButton.setDisabledIcon(new ImageIcon(IOTools.OpenSystemImage("backwarddis.png")));
		backBoostButton.setSize(backBoostButton.getIcon().getIconWidth(),backBoostButton.getIcon().getIconHeight());
		boostBox.add(backBoostButton);
		boostBox.add(Box.createHorizontalStrut(3));
		boostBox.add(forwBoostButton);
		boostLabelImageFile=new JLabel(NONE_IMAGE,JLabel.LEFT);
		boostLabelAll=new JLabel("",JLabel.LEFT);
		
		randomize=new JToggleButton();
		final ImageIcon up=new ImageIcon(IOTools.OpenSystemImage("order.bmp"));
		final ImageIcon down=new ImageIcon(IOTools.OpenSystemImage("random.bmp"));
		randomize.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent arg0) {
					if (randomize.isSelected())
						randomize.setIcon(down);
					else
						randomize.setIcon(up);
				}				
			}
		);
		randomize.setMaximumSize(new Dimension(30,30));		
		randomize.setIcon(up);
		boostBox.add(Box.createHorizontalStrut(3));
		boostBox.add(randomize);

		Box labelBox=Box.createVerticalBox();
		labelBox.add(boostLabelImageFile);
		labelBox.add(boostLabelAll);
		boostBox.add(Box.createHorizontalStrut(5));
		boostBox.add(labelBox);
				
		boostPanel.add(boostBox);
		
		forwBoostButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						if (boostImageNum < boostList.size()){
							boostImageNum++;
							ChangeBoostImage();
						}					
					}				
				}
			);
			backBoostButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						if (boostImageNum > 1){
							boostImageNum--;
							ChangeBoostImage();
						}					
					}				
				}
			);
	}
	/** 
	 * initializes the boosting image list and validates 
	 * the forward / backward buttons. For more information
	 * about boosting in <b>GemIdent</b>, please consult
	 * section 5.3 in the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public void EnableBoostingFeature(){
		
		boostList=Classify.AllClassified();
		boostList.removeAll(thumbnailPane.getThumbnailNames());
		ValidateBoostPanel();
		boostListR=RandomizeList(boostList);
		
		boostImageNum=0;
		boostLabelAll.setText("Image "+boostImageNum+"/"+boostList.size());
		
		super.appendToEast(boostPanel);
	}
	/** given a list of strings, returns the list elements in a random order */
	private ArrayList<String> RandomizeList(ArrayList<String> list){
		int n=list.size();
		ArrayList<String> copy=new ArrayList<String>(n);
		for (String s:list)
			copy.add(new String(s));
		ArrayList<String> random=new ArrayList<String>(n);
		while(true){
			if (copy.size() == 0)
				break;
			int i=(int)Math.floor(Math.random()*copy.size());
			random.add(copy.remove(i));
		}
		return random;
	}
	/** ensure the boosting panel buttons are dis/enabled in a consistent way */
	private void ValidateBoostPanel(){
		if (boostList.size() > 0){
			if (boostImageNum == boostList.size()){
				forwBoostButton.setEnabled(false);
				backBoostButton.setEnabled(true);
			}
			else if (boostImageNum <= 1){
				forwBoostButton.setEnabled(true);
				backBoostButton.setEnabled(false);
			}
			else {
				forwBoostButton.setEnabled(true);
				backBoostButton.setEnabled(true);
			}				
		}
		else {
			forwBoostButton.setEnabled(false);
			backBoostButton.setEnabled(false);
		}
	}
	/** selects the {@link #boostImageNum boost image} and displays the image to the screen, displays the filename, and validates the buttons */
	protected void ChangeBoostImage(){
		thumbnailPane.setSelectedThumbnail(null);
		thumbnailPane.repaint();
		String filename=null;
		try {
			if (randomize.isSelected())
				filename=boostListR.get(boostImageNum-1);
			else
				filename=boostList.get(boostImageNum-1);
		} catch (Exception e){
			filename=NONE_IMAGE;
		}
		((KImagePhenotypeTrainPanel)imagePanel).setDisplayImage(ImageAndScoresBank.getOrAddDataImage(filename),true);
		boostLabelAll.setText("Image "+boostImageNum+"/"+boostList.size());
		boostLabelImageFile.setText(filename);
		ValidateBoostPanel();
	}
	/**
	 * The user has just clicked a new point and the {@link TrainClickListener training listener}
	 * was fired.
	 * 
	 * @param filename		the user has just trained this image
	 * @param to			the user just clicked this point
	 */
	public void NewPoint(String filename,Point to){
		AddBoostImageToTrainingSet(filename);
		super.NewPoint(filename,to);
	}
	/** the user trained one of the boost images and it is added to the set of training images */
	protected void AddBoostImageToTrainingSet(String filename){
		if (boostList != null){
			if (boostList.remove(filename) && boostListR.remove(filename)){
				if (boostImageNum > 0)
					boostImageNum--;
				boostLabelImageFile.setText("<None>");
				boostLabelAll.setText("Image "+boostImageNum+"/"+boostList.size());
				ValidateBoostPanel();
				if (!thumbnailPane.isInThumbnails(filename)){
					KThumbnail thumb=new KThumbnail(this,filename);
					UpdateThumbnailIfClassified(thumb,false);
					thumbnailPane.addThumbnail(thumb,true);	
				}
			}
		}
	}
	/** the title of the "see error" checkbox */
	private static final String typeIerr="See Type I Errors: ";
	/** during retraining, the error locating feature is enabled */
	public void EnableErrorFinderFeature() {
		if (errorFinder == null){
			errorFinder = new JCheckBox(); 
			errorFinder.setSelected(false);
			errorFinder.setEnabled(false);
			errorFinder.addItemListener(
				new ItemListener(){
					public void itemStateChanged(ItemEvent e) {
					    if (e.getStateChange() == ItemEvent.DESELECTED)
					    	DisableErrorFinding();
					    else 
					    	EnableErrorFinding();
					}
				}
			);
			typeILabel=new JLabel(typeIerr,JLabel.RIGHT);
			typeILabel.setEnabled(false);
			settings_box.setLayout(new GridLayout(0,2,4,0));
			settings_box.add(typeILabel);
			settings_box.add(errorFinder);
		}
		revalidateTypeIErrors();
	}

	/** the user switched an image and the panel needs to check if there are errors on the new image */
	public void revalidateTypeIErrors(){
		if (errorFinder != null){
			if (imagePanel.displayImage != null){
				String filename=imagePanel.displayImage.getFilename();
				HashSet<Point> set = Run.it.getTypeOneErrors().get(filename);
				if (set == null)
					invalidateTypeIErrors();
				else if (set.size() == 0)
					invalidateTypeIErrors();
				else {
					typeILabel.setText(typeIerr+"("+set.size()+")");
					typeILabel.setEnabled(true);
					errorFinder.setEnabled(true);
				}				
			}
			else 
				invalidateTypeIErrors();
		}
	}
	/** there are no errors in this image - gray out the error-finding feature */
	private void invalidateTypeIErrors(){
		typeILabel.setText(typeIerr);
		typeILabel.setEnabled(false);
		errorFinder.setEnabled(false);
	}
	
	/** open the first thumbnail as a convenience to the user */
	public void ClickFirstThumbnailIfExistsAndIsNecessary() {
		thumbnailPane.ClickFirstThumbnailIfExistsAndIsNecessary();
	}
}