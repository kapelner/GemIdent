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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;


import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import GemIdentCentroidFinding.CentroidFinder;
import GemIdentClassificationEngine.Classify;
import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentImageSets.Nuance.NuanceImageListInterface;
import GemIdentOperations.Run;
import GemIdentStatistics.VisualizeClassifierImportances;
import GemIdentStatistics.RandomForest.RandomForest;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.BoolMatrix;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
/**
 * Controls and houses the classification & post-processing panel. For discussion on
 * the execution of a classification via the classify button, see section 4.1 of the manual.
 * For discussion on setting the classification parameters, see section 4.1.1 of the manual.
 * For discussion on picking a subset of the images to classify, see section 4.1.2 of the 
 * manual. And for discussion on reclassification, see section 5.5 of the manual.
 * 
 * @author Adam Kapelner
 *
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 */
@SuppressWarnings("serial")
public class KClassifyPanel extends KPanel{

	private boolean all_cnn_params_are_valid;

	/** the panel that holds all the parameter options and functional buttons in the Western region */
	private JPanel options;

	/** the spinner that allows the user to choose the accuracy level */
	private JSpinner numTreesSpinner;
	/** the spinner that allows the user to choose the number of threads the classification will use */
	private JSpinner numThreadsSpinner;
	/** the spinner that allows the user to choose the number of pixels to skip during classification */
	private JSpinner numPixelSkipSpinner;
//	/** the spinner that allows the user to choose the number of pixels in a bacth for screen updates */
//	private JSpinner numBatchSpinner;
	/** the user chooses to classify all images */
	private JRadioButton allButton;
	/** the user chooses to classify only the training set to classify */
	private JRadioButton onlyButton;
//	/** the user chooses to classify the training set plus a specific range */
//	private JRadioButton rangeButton;

	private ButtonGroup classifyMethod;

	/** the button group that links the above buttons together */
	private ButtonGroup classifyGroup;
//	/** the text where the user enters the range of images (@see {@link #rangeButton range option})*/
//	private JTextField rangeText;
//	/** the filenames entered into the {@link #rangeText rangeText field} that are not recognized image filenames */
//	private String ProblemFiles;
//	/** the user chooses to classify those trained plus ten random images */
//	private JRadioButton tenRandomButton;
//	/** the user chooses to classify those trained plus twenty random images */
//	private JRadioButton twentyRandomButton;
//	/** the user chooses to classify those training plus N random images */
//	private JRadioButton nRandomButton;
//	/** the user chooses to classify those training plus N random images */
//	private JRadioButton thoseClickedOnButton;	
//	/** the text where the user enters the N number of random images (see {@link #nRandomButton random option}) */
//	private JTextField nRandomText;
//	/** the text in {@link #nRandomText random text field} parsed to an integer */
//	private Integer nRand;

	/** the user chooses to classify the images */
	private JButton RunClassifyButton;
	/** the user chooses to run a classification and immediately following, a post-processing */
	private JButton RunClassifyAndPostProcessButton;
	/** the user only wishes to run a post-processing to find centroids */
	private JButton RunFindCenters;
	/** the user chooses to stop the current operation */
	private JButton StopButton;
	/** the user chooses to do a retraining */
	private JButton RunRetrainButton;
	/** user chooses a classifier */
	private JButton ChooseClassifierButton;
	/** text field displays classifier name */
	private JTextField ClassifierName;
	/** clear the loaded classifier */
	private JButton ClearClassifierButton;

	
	/** user chooses a classifier */
	private JButton SetExclusionsButton;	

	/** a mapping from phenotype name to its [partial] result matrix */
	protected HashMap<String,BoolMatrix> is; //display
	/** the Box that holds all the pre-processing progress bars */
	protected Box preProcessingBarsBox;
	/** the progress bar that reflects the progress of the training data creation (in pre-processing) */
	protected JProgressBarAndLabel trainingProgress;
	/** the progress bar that reflects the progress of the {@link GemIdentOperations.StainOpener#run() opening of the Mahalanobis cubes} (in pre-processing) */
	protected JProgressBarAndLabel openProgress;
	/** the progress bar that reflects the creation of the {@link GemIdentStatistics.Classifier machine learning classifier} */
	private JProgressBarAndLabel buildProgress;
	/** the box that holds all progress bars whether they be pre-processing, classification, or post-processing indicators */
	private Box classifyBox;
	
	// labels for the classification parameters
	private JLabel choiceLabel;
	private JLabel titleLabel;
	private JLabel numTreesLabel;
	private JLabel numThreadsLabel;
	private JLabel numPixelSkipLabel;
	private JLabel picturesToClassifyLabel;

	//Entry values for components
	private JButton register_values_button;

	//labels for DL4J parameters or RF
	private JRadioButton CNN_selected;
	private JRadioButton RF_selected;

	//private JLabel imageHeiLabel;
	//private JLabel imageWidLabel;
	private JLabel channelsLabel;
	private JLabel numExamplesLabel;
	private JLabel numLabelsLabel;
	private JLabel batchSizeLabel;
	private JLabel iterationsLabel;
	private JLabel epochsLabel;
	private JLabel splitTrainTestLabel;

	private JTextField channels2;

	//private JSpinner imageHeight;
	//private JSpinner imageWidth;
//	private JSpinner channels;
//	private JTextField numExamples;
	private JTextField numLabels;
	private JTextField batchSize;
	private JTextField iterations;
	private JTextField epochs;
	private JTextField splitTrainTest;

	//boxes for individual textfields/spinners
    private Box numLabelsBox;
    private Box channelsBox;
    private Box batchSizeBox;
    private Box iterationsBox;
    private Box epochsBox;
    private Box splitTrainTestBox;

    private Box numTreesBox;

	private JRadioButton remainButton;

	/** the dimension of the Western region that holds all options and buttons */
	private static final Dimension optionsDim=new Dimension(150,500);
	
	//classification parameters
	public static final int CLASSIFY_ALL=1;
	public static final int CLASSIFY_TRAINED=2;
	public static final int CLASSIFY_REMAINING = 20;
	public static final int TEN_RANDOM=3;
	public static final int TWENTY_RANDOM=4;
	public static final int CLASSIFY_RANGE=5;
    public static final int N_RANDOM=6;
    public static final int CLICKED_ON=7;
	private static final int MAX_NUM_TREES=320;

	//the classifier
	private RandomForest classifier;

	private JRadioButton erosion_method_radio_button;

//	private JLabel useEdgesLabel ;

//	private JCheckBox useEdgesCheckBox;


	/** initializes the image panel, sets up the option box in the west, sets listeners to the option box */
	public KClassifyPanel(){
		super();
		imagePanel=new KImageClassifyPanel(this);
		setImagePanel(imagePanel);
		//SetUpChoices();
		SetUpOptions();
		SetUpListeners();		
		add(options,BorderLayout.WEST);	
	}
//	/**
//	 * Parses the content of the {@link #rangeText rangeText} (which
//	 * is now stored inside Run.it) and returns a list of files that
//	 * do exist, as well as storing the files that do not exist (for
//	 * the creation of an error message)
//	 * 
//	 * @return		the list of files that exist
//	 */
//	public ArrayList<String> RangeToFilenames(){
//		ProblemFiles="";
//		ArrayList<String> notFound=new ArrayList<String>();
//		ArrayList<String> found=new ArrayList<String>();
//		String[] fileArray=Run.it.RANGE_TEXT.split(",");
//		if (fileArray.length == 1)
//			if (!IOTools.FileExists(Run.it.RANGE_TEXT.trim()))
//				return null;
//			else {
//				found.add(Run.it.RANGE_TEXT);
//				return found;
//			}
//		for (String file:fileArray)
//			if (!IOTools.FileExists(file.trim()))
//				notFound.add(file.trim());
//			else
//				found.add(file.trim());
//		if (notFound.size() > 0){
//			for (String file:notFound)
//				ProblemFiles+=file+"\n";
//			return null;
//		}
//		return found;
//	}
	/** sets an image to be displayed in the image panel (see {@link KImagePanel#setDisplayImage(DataImage) setDisplayImage}*/
	public void setDisplayImage(DataImage displayImage){
		imagePanel.setDisplayImage(displayImage);
	}
	private static final Font FontForLabels = new Font(null,Font.PLAIN,10);




	/** populate the Western region after instantiating all the options components */
	private void SetUpOptions() {

		options=new JPanel();
		options.setPreferredSize(optionsDim);
		options.setLayout(new BorderLayout());
		
		Box optionBox=Box.createVerticalBox();
		optionBox.add(Box.createVerticalStrut(10)); //give it some margin
		optionBox.add(Box.createVerticalStrut(10));


		


		JFormattedTextField textfield;
		
		
		Box numThreadsBox=Box.createHorizontalBox();		
		numThreadsLabel=new JLabel("Number of CPUs");
		numThreadsLabel.setFont(FontForLabels);
		numThreadsBox.add(numThreadsLabel);
		numThreadsSpinner=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_NUM_THREADS,1,256,1));
		textfield = ((JSpinner.DefaultEditor)numThreadsSpinner.getEditor()).getTextField();
		textfield.setEditable(false);
		textfield.setFocusable(false);
		numThreadsBox.add(numThreadsSpinner);
		optionBox.add(numThreadsBox);
		numThreadsBox.setVisible(true);		



		Box numPixelSkipBox=Box.createHorizontalBox();		
		numPixelSkipLabel=new JLabel("Pixel Skip");
		numPixelSkipLabel.setFont(FontForLabels);
		numPixelSkipBox.add(numPixelSkipLabel);
		numPixelSkipSpinner=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,50,1));
		textfield=((JSpinner.DefaultEditor)numPixelSkipSpinner.getEditor()).getTextField();
		textfield.setEditable(false);
		textfield.setFocusable(false);
		numPixelSkipBox.add(numPixelSkipSpinner);
		optionBox.add(numPixelSkipBox);
		numPixelSkipBox.setVisible(true);
		

		choiceLabel=new JLabel("<html><u>Classification Algorithm</u></html>");
		Box choiceLabelBox=Box.createHorizontalBox();
		choiceLabelBox.add(choiceLabel);
		optionBox.add(choiceLabelBox);		

		RF_selected = new JRadioButton("Random Forest");
		CNN_selected = new JRadioButton("CNN (Deep Learning)");

		classifyMethod = new ButtonGroup();
		RF_selected.setEnabled(true);
		CNN_selected.setEnabled(true);
		classifyMethod.add(RF_selected);
		classifyMethod.add(CNN_selected);
		Box classification_method_box=Box.createVerticalBox();
		classification_method_box.setAlignmentX(LEFT_ALIGNMENT);
		classification_method_box.add(RF_selected);
		classification_method_box.add(CNN_selected);
		optionBox.add(classification_method_box);
		

		titleLabel=new JLabel("<html><u>Classification Parameters</u></html>");
		Box titleLabelBox=Box.createHorizontalBox();
		titleLabelBox.add(titleLabel);
		optionBox.add(titleLabelBox);	 



        numTreesBox=Box.createHorizontalBox();
		numTreesLabel=new JLabel("Accuracy Level");
		numTreesLabel.setFont(FontForLabels);
		numTreesBox.add(numTreesLabel);
		numTreesSpinner=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_NUM_TREES,5,MAX_NUM_TREES,5));
		textfield=((JSpinner.DefaultEditor)numTreesSpinner.getEditor()).getTextField();
		textfield.setEditable(false);
		textfield.setFocusable(false);
		numTreesBox.add(numTreesSpinner);
		optionBox.add(numTreesBox);
        numTreesBox.setEnabled(true);
		numTreesBox.setVisible(false);



//		Box useEdgesBox = Box.createHorizontalBox();		
//		useEdgesLabel = new JLabel("Use Edges?");
//		useEdgesLabel.setFont(FontForLabels);
//		useEdgesBox.add(useEdgesLabel);
//		numPixelSkipBox.setVisible(false);

//		useEdgesCheckBox = new JCheckBox();
//		useEdgesCheckBox.addItemListener(
//			new ItemListener(){
//				public void itemStateChanged(ItemEvent e) {					
//				    //this will be done later
//				}
//			}
//		);


		/**
		Box imageWidBox=Box.createHorizontalBox();
		imageWidLabel=new JLabel("Image Width");
		imageWidLabel.setFont(FontForLabels);
		imageWidBox.add(imageWidLabel);
		imageWidth=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,50,1));
		textfield=((JSpinner.DefaultEditor)imageWidth.getEditor()).getTextField();
		textfield.setEditable(false);
		textfield.setFocusable(false);
		imageWidBox.add(imageWidth);
		optionBox.add(imageWidBox);
		imageWidBox.setVisible(false);

		Box imageHeiBox=Box.createHorizontalBox();
		imageHeiLabel=new JLabel("Image Height");
		imageHeiLabel.setFont(FontForLabels);
		imageHeiBox.add(imageHeiLabel);
		imageHeight=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,50,1));
		textfield=((JSpinner.DefaultEditor)imageHeight.getEditor()).getTextField();
		textfield.setEditable(false);
		textfield.setFocusable(false);
		imageHeiBox.add(imageHeight);
		optionBox.add(imageHeiBox);
		imageHeiBox.setVisible(false);
		*/

        channelsBox=Box.createHorizontalBox();
		channelsLabel=new JLabel("NumChannels");
		channelsLabel.setFont(FontForLabels);
		channelsBox.add(channelsLabel);
		//channels=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,3,1));
		//textfield=((JSpinner.DefaultEditor)channels.getEditor()).getTextField();
		//textfield.setEditable(false);
		//textfield.setFocusable(false);
		channels2 = new JTextField("", 10);

		channelsBox.add(channels2);
		optionBox.add(channelsBox);
		channelsBox.setVisible(false);

//		Box numExamplesBox=Box.createHorizontalBox();
//		numExamplesLabel=new JLabel("Number Examples");
//		numExamplesLabel.setFont(FontForLabels);
//		numExamplesBox.add(numExamplesLabel);
//		//numExamples=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,1000,10));
//		//textfield=((JSpinner.DefaultEditor)numExamples.getEditor()).getTextField();
//		//textfield.setEditable(false);
//		//textfield.setFocusable(false);
////		numExamples = new JTextField("", 10);
//
//		numExamplesBox.add(numExamples);
//		optionBox.add(numExamplesBox);
//		numExamplesBox.setVisible(false);

        numLabelsBox=Box.createHorizontalBox();
		numLabelsLabel=new JLabel("Number Labels");
		numLabelsLabel.setFont(FontForLabels);
		numLabelsBox.add(numLabelsLabel);
		//numLabels=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,50,1));
		//textfield=((JSpinner.DefaultEditor)numLabels.getEditor()).getTextField();
		//textfield.setEditable(false);
		//textfield.setFocusable(false);
		numLabels = new JTextField("",10);
		numLabelsBox.add(numLabels);
		optionBox.add(numLabelsBox);
		numLabelsBox.setVisible(false);

        batchSizeBox=Box.createHorizontalBox();
		batchSizeLabel=new JLabel("Number Batches");
		batchSizeLabel.setFont(FontForLabels);
		batchSizeBox.add(batchSizeLabel);
		//batchSize=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,100,1));
		//textfield=((JSpinner.DefaultEditor)batchSize.getEditor()).getTextField();
		//textfield.setEditable(false);
		//textfield.setFocusable(false);
		batchSize = new JTextField("",10);

		batchSizeBox.add(batchSize);
		optionBox.add(batchSizeBox);
		batchSizeBox.setVisible(false);

        iterationsBox=Box.createHorizontalBox();
		iterationsLabel=new JLabel("Iterations ");
		iterationsLabel.setFont(FontForLabels);
		iterationsBox.add(iterationsLabel);
		//iterations=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,100,1));
		//textfield=((JSpinner.DefaultEditor)iterations.getEditor()).getTextField();
		//textfield.setEditable(false);
		//textfield.setFocusable(false);
		iterations = new JTextField("",10);

		iterationsBox.add(iterations);
		optionBox.add(iterationsBox);
		iterationsBox.setVisible(false);

        epochsBox=Box.createHorizontalBox();
		epochsLabel=new JLabel("Epochs ");
		epochsLabel.setFont(FontForLabels);
		epochsBox.add(epochsLabel);
		//epochs=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,100,1));
		//textfield=((JSpinner.DefaultEditor)epochs.getEditor()).getTextField();
		//textfield.setEditable(false);
		//textfield.setFocusable(false);
		epochs = new JTextField("",10);

		epochsBox.add(epochs);
		optionBox.add(epochsBox);
		epochsBox.setVisible(false);

		splitTrainTestBox=Box.createHorizontalBox();
		splitTrainTestLabel=new JLabel("Split Train %");
		splitTrainTestLabel.setFont(FontForLabels);
		splitTrainTestBox.add(splitTrainTestLabel);
		//splitTrainTest=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_PIXEL_SKIP-1,0,100,1));
		//textfield=((JSpinner.DefaultEditor)splitTrainTest.getEditor()).getTextField();
		//textfield.setEditable(false);
		//textfield.setFocusable(false);
		splitTrainTest = new JTextField("100",10); //default 100

		splitTrainTestBox.add(splitTrainTest);
		optionBox.add(splitTrainTestBox);
		splitTrainTestBox.setVisible(false);

		register_values_button = new JButton("Register Values");
		optionBox.add(register_values_button);




//		useEdgesBox.add(useEdgesCheckBox);		
//		optionBox.add(useEdgesBox);
//		Box numBatchBox=Box.createHorizontalBox();		
//		numBatchLabel=new JLabel("Pix/Update");
//		numBatchLabel.setFont(FontForLabels);
//		numBatchBox.add(numBatchLabel);
//		numBatchSpinner=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_R_BATCH_SIZE,5000,97500,2500));
//		textfield=((JSpinner.DefaultEditor)numBatchSpinner.getEditor()).getTextField();
//		textfield.setEditable(false);
//		textfield.setFocusable(false);
//		numBatchBox.add(numBatchSpinner);
//		optionBox.add(numBatchBox);
		
		optionBox.add(Box.createVerticalStrut(10));
		
//		rotationalLabel=new JLabel("<html><u>Rotaional Invariance Parameters</u></html>");
//		optionBox.add(rotationalLabel);		
		
//		Box numPiecesBox=Box.createHorizontalBox();		
//		numPiecesLabel=new JLabel("Number of Split Pieces");
//		numPiecesBox.add(numPiecesLabel);
//		numPiecesSpinner=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_NUM_SPLIT_PIECES,1,8,1));
//		textfield=((JSpinner.DefaultEditor)numPiecesSpinner.getEditor()).getTextField();
//		textfield.setEditable(false);
//		textfield.setFocusable(false);
//		numPiecesBox.add(numPiecesSpinner);
//		optionBox.add(numPiecesBox);
//		
//		Box numRotationsBox=Box.createHorizontalBox();		
//		numRotationsLabel=new JLabel("Number of Rotations");
//		numRotationsBox.add(numRotationsLabel);
//		numRotationsSpinner=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_NUM_ROTATIONS,1,50,1));
//		numRotationsLabel.setEnabled(false);
//		numRotationsLabel.setFocusable(false);
//		textfield=((JSpinner.DefaultEditor)numRotationsSpinner.getEditor()).getTextField();
//		textfield.setEditable(false);
//		textfield.setFocusable(false);
//		numRotationsSpinner.setEnabled(false); //initially it's disabled
//		numRotationsBox.add(numRotationsSpinner);
//		optionBox.add(numRotationsBox);
		
		optionBox.add(Box.createVerticalStrut(10));
		
		//these features will be held for a later release
//		rotationalLabel.setVisible(false);
//		numPiecesLabel.setVisible(false);
//		numPiecesSpinner.setVisible(false);
//		numRotationsLabel.setVisible(false);
//		numRotationsSpinner.setVisible(false);
		
		picturesToClassifyLabel=new JLabel("<html><u>Images To Classify</u></html>");
		Box picturesToClassifyLabelBox=Box.createHorizontalBox();
		picturesToClassifyLabelBox.add(picturesToClassifyLabel);
		
		optionBox.add(picturesToClassifyLabelBox);

		
		allButton=new JRadioButton("Classify All");	    
	    onlyButton=new JRadioButton("Classify Trained");
	    onlyButton.setSelected(true);	    
	    remainButton=new JRadioButton("Classify Remaining");
	    
//	    tenRandomButton=new JRadioButton(ten);
//	    tenRandomButton.setActionCommand(ten);
	    
//	    twentyRandomButton=new JRadioButton(twenty);
//	    twentyRandomButton.setActionCommand(twenty);
	    
//	    nRandomButton=new JRadioButton(nrand);
//	    nRandomButton.setActionCommand(nrand);

//	    rangeButton=new JRadioButton(range);
//	    rangeButton.setActionCommand(range);
	    
//	    thoseClickedOnButton=new JRadioButton(clickedon);

	    classifyGroup=new ButtonGroup();
	    classifyGroup.add(allButton);
	    classifyGroup.add(onlyButton);
	    classifyGroup.add(remainButton);
	    //classifyGroup.add(tenRandomButton);
	    //classifyGroup.add(twentyRandomButton);
//	    classifyGroup.add(nRandomButton);
//	    classifyGroup.add(rangeButton);
//	    classifyGroup.add(thoseClickedOnButton);
	    
		Box picturesToClassifyBox = Box.createVerticalBox();
		picturesToClassifyBox.setAlignmentX(LEFT_ALIGNMENT);
	    picturesToClassifyBox.add(allButton);
	    picturesToClassifyBox.add(onlyButton);
	    picturesToClassifyBox.add(remainButton);
	   // picturesToClassifyBox.add(tenRandomButton);
	   // picturesToClassifyBox.add(twentyRandomButton);
//	    picturesToClassifyBox.add(nRandomButton);
	    
	    
//	    rangeText=new JTextField();
//	    rangeText.setEnabled(false);
//	    rangeText.setPreferredSize(new Dimension(100,30));
//	    nRandomText=new JTextField();
//	    nRandomText.setEnabled(false);
//	    nRandomText.setPreferredSize(new Dimension(100,30));

//	    picturesToClassifyBox.add(nRandomText);
//	    picturesToClassifyBox.add(rangeButton);
//	    picturesToClassifyBox.add(rangeText);
//	    picturesToClassifyBox.add(thoseClickedOnButton);
	    
	    optionBox.add(picturesToClassifyBox);
	    
	    optionBox.add(Box.createVerticalStrut(10));
	    
	    Box buttonBox = Box.createVerticalBox();
	    
	    ChooseClassifierButton=new JButton("Classifier...");
	    ClearClassifierButton = new JButton("x");
	    ClearClassifierButton.setMargin(new Insets(2,2,2,2));
//	    ClearClassifierButton.setPreferredSize(new Dimension(30,15));
//	    ClearClassifierButton.setEnabled(false);
	    ClassifierName=new JTextField();
	    ClassifierName.setEditable(false);
	    ClassifierName.setColumns(15);
	    RunClassifyAndPostProcessButton=new JButton("<html>Classify, Centers,<br>& Sanity Check</html>");
	    RunClassifyButton=new JButton("Classify");
	    RunFindCenters=new JButton("Find Centers");
	    StopButton=new JButton("Stop");
	    StopButton.setEnabled(false);
	    RunRetrainButton=new JButton("Retrain");
	    RunRetrainButton.setEnabled(false);
	    
	    SetExclusionsButton = new JButton("<html>Exclusion<br>Rules...</html>");
	    optionBox.add(SetExclusionsButton);
	    
	    //hide this option for now
	    Box horiz=Box.createHorizontalBox();
	    horiz.add(ChooseClassifierButton);
	    horiz.add(ClearClassifierButton);
	    optionBox.add(Box.createVerticalStrut(30));
	    optionBox.add(horiz);
	    optionBox.add(ClassifierName);

	    buttonBox.add(RunClassifyAndPostProcessButton);
	    buttonBox.add(Box.createVerticalStrut(10));
	    buttonBox.add(RunClassifyButton);
	    buttonBox.add(Box.createVerticalStrut(10));
//	    buttonBox.add(find_centers_method_panel);	    
	    buttonBox.add(RunFindCenters);
	    buttonBox.add(Box.createVerticalStrut(10));
	    buttonBox.add(StopButton);
	    buttonBox.add(Box.createVerticalStrut(10));
	    buttonBox.add(RunRetrainButton);
	    
	    GUIFrame find_centers_method_panel = new GUIFrame("Find Centers Method");
	    Container find_centers_method_panel_box = new Container();
	    find_centers_method_panel_box.setLayout(new GridLayout(0, 1, 2, 0));
	    erosion_method_radio_button = new JRadioButton("Smart Erosions");
	    erosion_method_radio_button.setSelected(true); //it's the default as of now
	    find_centers_method_panel_box.add(erosion_method_radio_button);    
	    ButtonGroup findCentersMethodsGroup = new ButtonGroup();
	    findCentersMethodsGroup.add(erosion_method_radio_button);
	    find_centers_method_panel.add(find_centers_method_panel_box);
	    
	    options.add(buttonBox, BorderLayout.SOUTH);
	    options.add(find_centers_method_panel, BorderLayout.CENTER);
	    options.add(optionBox, BorderLayout.NORTH);

		RF_selected.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
                        Run.it.classification_choice = "RF_select";
						EnableRFButtons();

					}
				}
		);

		CNN_selected.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
                        Run.it.classification_choice = "CNN_select";
						EnableCNNButtons();

					}
				}
		);



	}
	/** sets up appropriate listeners for all the options and buttons in the Western region */
	private void SetUpListeners(){

		register_values_button.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){

						//Only update values if all are okay
						all_cnn_params_are_valid = true;

						//Channels
//						try{
//							Integer.parseInt(channels2.getText());
//						}
//
//						catch(NumberFormatException ne){
//							JOptionPane.showMessageDialog(null,
//									"Error: Please enter an integer (1 or 3) for Channels...", "Error Massage",
//									JOptionPane.ERROR_MESSAGE);
//							all_cnn_params_are_valid = false;
//
//						}

//						//NumberExamples
//						try{
//							Integer.parseInt(numExamples.getText());
//						}
//
//						catch(NumberFormatException ne){
//							JOptionPane.showMessageDialog(null,
//									"Error: Please enter an integer for Number Examples...", "Error Massage",
//									JOptionPane.ERROR_MESSAGE);
//							all_cnn_params_are_valid = false;

//						}

						//Number Labels
//						try{
//							Integer.parseInt(numLabels.getText());
//						}
//
//						catch(NumberFormatException ne){
//							JOptionPane.showMessageDialog(null,
//									"Error: Please enter an integer for Number of labels...", "Error Massage",
//									JOptionPane.ERROR_MESSAGE);
//							all_cnn_params_are_valid = false;
//
//						}

						//Batches
//						try{
//							Integer.parseInt(batchSize.getText());
//						}
//
//						catch(NumberFormatException ne){
//							JOptionPane.showMessageDialog(null,
//									"Error: Please enter an integer for Batches...", "Error Massage",
//									JOptionPane.ERROR_MESSAGE);
//							all_cnn_params_are_valid = false;

//						}

						//Iterations
						try{
							Integer.parseInt(iterations.getText());
						}

						catch(NumberFormatException ne){
							JOptionPane.showMessageDialog(null,
									"Error: Please enter an integer for Iterations...", "Error Massage",
									JOptionPane.ERROR_MESSAGE);
							all_cnn_params_are_valid = false;

						}

						//Epochs
						try{
							Integer.parseInt(epochs.getText());
						}

						catch(NumberFormatException ne){
							JOptionPane.showMessageDialog(null,
									"Error: Please enter an integer for Epochs...", "Error Massage",
									JOptionPane.ERROR_MESSAGE);
							all_cnn_params_are_valid = false;

						}

						//Split Train
						try{
							Integer.parseInt(splitTrainTest.getText());
						}

						catch(NumberFormatException ne){
							JOptionPane.showMessageDialog(null,
									"Error: Please enter an integer between 0 & 100 for Split Train...", "Error Massage",
									JOptionPane.ERROR_MESSAGE);
							all_cnn_params_are_valid = false;

						}
						/**
						if(Integer.parseInt(channels2.getText())!=1 && Integer.parseInt(channels2.getText())!=3 ){
							JOptionPane.showMessageDialog(null,
									"Error: Channels must be 1 or 3...", "Error Massage",
									JOptionPane.ERROR_MESSAGE);
							all_cnn_params_are_valid = false;
						}
						*/
						if(Integer.parseInt(splitTrainTest.getText())<=0 && Integer.parseInt(splitTrainTest.getText())>=100 ){
							JOptionPane.showMessageDialog(null,
									"Error: Split train must be between 0 and 100...", "Error Massage",
									JOptionPane.ERROR_MESSAGE);

							all_cnn_params_are_valid = false;
						}

						if (all_cnn_params_are_valid){
//							Run.it.CNN_num_examples = Integer.parseInt(numExamples.getText());
							Run.it.CNN_batch_num = Integer.parseInt(batchSize.getText());
							Run.it.CNN_iter_num = Integer.parseInt(iterations.getText());
							Run.it.CNN_epoch_num = Integer.parseInt(epochs.getText());
							Run.it.CNN_split = Integer.parseInt(splitTrainTest.getText());
							Run.it.GUIsetDirty(true);
                            RunClassifyAndPostProcessButton.setEnabled(true);
                            RunClassifyButton.setEnabled(true);
						}
						else {
						    RunClassifyAndPostProcessButton.setEnabled(false);
						    RunClassifyButton.setEnabled(false);
						}



					}
				}
		);


		numTreesSpinner.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e){					
					Run.it.num_trees=(Integer)numTreesSpinner.getValue();
					Run.it.GUIsetDirty(true);
				}
			}
		);
		numThreadsSpinner.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					Run.it.num_threads=(Integer)numThreadsSpinner.getValue();	
					Run.it.GUIsetDirty(true);
				}			
			}
		);
		numPixelSkipSpinner.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					Run.it.pixel_skip=(Integer)numPixelSkipSpinner.getValue()+1;
					Run.it.GUIsetDirty(true);
				}
			}
		);



		/**
		imageWidth.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_imageWid=(Integer)imageWidth.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);

		imageHeight.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_imageHei=(Integer)imageHeight.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);
		*/

		/**
		channels.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_channels=(Integer)channels.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);
		 */



		/** Document listener for user input for values, rather than spinners
		channels2.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				warn();
			}
			public void removeUpdate(DocumentEvent e) {
				warn();
			}
			public void insertUpdate(DocumentEvent e) {
				warn();
			}

			public void warn() {

				try{
					Integer.parseInt(channels2.getText());
				}

				catch(NumberFormatException e){
					JOptionPane.showMessageDialog(null,
							"Error: Please enter an integer...", "Error Massage",
							JOptionPane.ERROR_MESSAGE);

				}

				if (Integer.parseInt(channels2.getText())!=1 && Integer.parseInt(channels2.getText())!=3){
					JOptionPane.showMessageDialog(null,
							"Error: Please enter 1 or 3 channels...", "Error Massage",
							JOptionPane.ERROR_MESSAGE);
					}
				else {
					Run.it.CNN_channels = Integer.parseInt(channels2.getText());
					Run.it.GUIsetDirty(true);
					}
			}
		});
		*/

		/**
		channels2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {


				if (Integer.parseInt(channels2.getText())!=3 && Integer.parseInt(channels2.getText())!=1){
					JOptionPane.showMessageDialog(null,
							"Error: Please enter 1 or 3 channels.", "Error Massage",
							JOptionPane.ERROR_MESSAGE);
				}
				else {
					Run.it.CNN_channels = Integer.parseInt(channels2.getText());
					Run.it.GUIsetDirty(true);
				}
			}
		});


		numExamples.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_num_examples=(Integer)numExamples.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);

		numLabels.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_num_labels=(Integer)numLabels.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);

		batchSize.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_batch_num=(Integer)batchSize.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);

		iterations.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_iter_num=(Integer)iterations.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);

		epochs.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_epoch_num=(Integer)epochs.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);

		splitTrainTest.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_split=(double)((Integer)splitTrainTest.getValue()/100);
						Run.it.GUIsetDirty(true);
					}
				}
		);

		nCores.addChangeListener(
				new ChangeListener(){
					public void stateChanged(ChangeEvent e){
						Run.it.CNN_nCores=(Integer)nCores.getValue();
						Run.it.GUIsetDirty(true);
					}
				}
		);
		*/

//		numBatchSpinner.addChangeListener(
//			new ChangeListener(){
//				public void stateChanged(ChangeEvent e) {
//					Run.it.num_pixels_to_batch_updates=(Integer)numBatchSpinner.getValue();
//					Run.it.GUIsetDirty(true);
//				}
//			}
//		);
//		numPiecesSpinner.addChangeListener(
//			new ChangeListener(){
//				public void stateChanged(ChangeEvent e) {
//					Run.it.NUM_SPLIT_PIECES=(Integer)numPiecesSpinner.getValue();	
//					if (Run.it.NUM_SPLIT_PIECES > 1){
//						numRotationsLabel.setEnabled(true);
//						numRotationsLabel.setFocusable(true);
//						numRotationsSpinner.setEnabled(true); //initially it's disabled
//					}
//					else {
//						numRotationsLabel.setEnabled(false);
//						numRotationsLabel.setFocusable(false);
//						numRotationsSpinner.setEnabled(false); //initially it's disabled
//					}
//					Run.it.GUIsetDirty(true);
//				}
//			}
//		);
//		numRotationsSpinner.addChangeListener(
//			new ChangeListener(){
//				public void stateChanged(ChangeEvent e) {
//					Run.it.NUM_ROTATIONS=(Integer)numRotationsSpinner.getValue();	
//					Run.it.GUIsetDirty(true);
//				}
//			}
//		);

		
		allButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
//					rangeText.setEnabled(false);
//					nRandomText.setEnabled(false);
					RunClassifyAndPostProcessButton.setEnabled(true);
					Run.it.pics_to_classify=CLASSIFY_ALL;
					Run.it.GUIsetDirty(true);
				}
			}
		);
		onlyButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
//					rangeText.setEnabled(false);
//					nRandomText.setEnabled(false);
					RunClassifyAndPostProcessButton.setEnabled(true);
					Run.it.pics_to_classify=CLASSIFY_TRAINED;
					Run.it.GUIsetDirty(true);
				}
			}
		);
		remainButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
//					rangeText.setEnabled(false);
//					nRandomText.setEnabled(false);
					RunClassifyAndPostProcessButton.setEnabled(true);
					Run.it.pics_to_classify=CLASSIFY_REMAINING;
					Run.it.GUIsetDirty(true);
				}
			}
		);
//		tenRandomButton.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
////					rangeText.setEnabled(false);
//					nRandomText.setEnabled(false);
//					RunClassifyAndPostProcessButton.setEnabled(true);
//					Run.it.pics_to_classify=TEN_RANDOM;
//					Run.it.GUIsetDirty(true);
//				}
//			}
//		);
//		twentyRandomButton.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
////					rangeText.setEnabled(false);
//					nRandomText.setEnabled(false);
//					RunClassifyAndPostProcessButton.setEnabled(true);
//					Run.it.pics_to_classify=TWENTY_RANDOM;
//					Run.it.GUIsetDirty(true);
//				}
//			}
//		);
//		nRandomButton.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
////					rangeText.setEnabled(false);
//					nRandomText.setEnabled(true);
//					RunClassifyAndPostProcessButton.setEnabled(true);
//					Run.it.pics_to_classify=N_RANDOM;
//					Run.it.GUIsetDirty(true);
//				}
//			}
//		);
//		thoseClickedOnButton.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
////					rangeText.setEnabled(false);
//					nRandomText.setEnabled(false);
//					RunClassifyAndPostProcessButton.setEnabled(true);
//					Run.it.pics_to_classify=CLICKED_ON;
//					Run.it.GUIsetDirty(true);
//				}
//			}
//		);
//		rangeButton.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e) {
//					Run.it.PICS_TO_CLASSIFY=CLASSIFY_RANGE;
//					RunClassifyAndPostProcessButton.setEnabled(true);
//					nRandomText.setEnabled(false);
////					rangeText.setEnabled(true);
//					if (RangeToFilenames() == null)
//						RunClassifyButton.setEnabled(false);
//					else
//						RunClassifyButton.setEnabled(true);
//					Run.it.GUIsetDirty(true);
//				}
//			}
//		);
//	    rangeText.addKeyListener(
//			new KeyListener(){
//				public void keyPressed(KeyEvent e){}
//				public void keyReleased(KeyEvent e){
//					Run.it.RANGE_TEXT=rangeText.getText();	
//					if (RangeToFilenames() == null){
//						RunClassifyButton.setEnabled(false);
//						RunFindCenters.setEnabled(false);
//						RunClassifyAndPostProcessButton.setEnabled(false);
//					}
//					else {
//						RunClassifyButton.setEnabled(true);
//						RunFindCenters.setEnabled(true);
//						RunClassifyAndPostProcessButton.setEnabled(true);
//					}
//					Run.it.GUIsetDirty(true);					
//				}
//				public void keyTyped(KeyEvent e){}
//			}
//		);
//	    rangeText.addFocusListener(
//	    	new FocusListener(){
//				public void focusGained(FocusEvent e){}
//				public void focusLost(FocusEvent e){
//					if (!ProblemFiles.equals(""))
//						JOptionPane.showMessageDialog(Run.it.getGUI(),"The following image(s) do not exist:\n"+ProblemFiles,"Image file(s) do not exist",JOptionPane.ERROR_MESSAGE);	
//				}			
//			}
//	    );
//	    nRandomText.addKeyListener(
//			new KeyListener(){
//				public void keyPressed(KeyEvent e){}
//				public void keyReleased(KeyEvent e){	
//					try {
//						nRand=Integer.parseInt(nRandomText.getText());
//						Run.it.N_RANDOM=nRand;
//						ChooseClassifierButton.setEnabled(true);
//						RunClassifyButton.setEnabled(true);
//						RunFindCenters.setEnabled(true);
//						RunClassifyAndPostProcessButton.setEnabled(true);
//					} catch (Exception ex){
//						nRand=null;
//						ChooseClassifierButton.setEnabled(true);
//						RunClassifyButton.setEnabled(false);
//						RunFindCenters.setEnabled(false);
//						RunClassifyAndPostProcessButton.setEnabled(false);
//					}
//					Run.it.GUIsetDirty(true);					
//				}
//				public void keyTyped(KeyEvent e){}
//			}
//		);
//	    nRandomText.addFocusListener(
//	    	new FocusListener(){
//				public void focusGained(FocusEvent e){}
//				public void focusLost(FocusEvent e){
//					if (nRand == null)
//						JOptionPane.showMessageDialog(Run.it.getGUI(),"Please enter a valid integer in the random number of images box","Invalid entry",JOptionPane.ERROR_MESSAGE);	
//				}			
//			}
//	    );
		StopButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					StopProcessing();
				}
			}
		);
		RunRetrainButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					ChooseClassifierButton.setEnabled(true);
					RunClassifyButton.setEnabled(true);
					RunFindCenters.setEnabled(true);
					RunClassifyButton.setText("Reclassify");
					setDisplayImage(null);
					repaintImagePanel();
					RunRetrainButton.setEnabled(false);
					Run.it.getGUI().AddOrUpdateRetrainTab();
				}
			}
		);
		
		SetExclusionsButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					Run.it.imageset.RespawnExclusionRuleDialog();
				}
			}
		);
		
		ChooseClassifierButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					classifier = null;
					JFileChooser chooser = new JFileChooser();
					chooser.setFileFilter(
						new FileFilter(){
							public boolean accept(File f){
								if (f.isDirectory())
									return true; //need to navigate through directories
								String name=f.getName();
								String[] pieces=name.split("\\.");
								if (pieces.length != 2)
									return false;
								if (pieces[1].toLowerCase().equals("classifier"))
									return true;
								return false;
							}
							public String getDescription(){
								return "classifier";
							}
						}
					);		
					int result = chooser.showOpenDialog(null);
					if ( result == JFileChooser.APPROVE_OPTION ) {
						File load_file = chooser.getSelectedFile();
						try {
							String classifiername = load_file.getName().split("\\.")[0];
							classifier = (RandomForest)IOTools.openFromXML(load_file.getAbsolutePath());
							System.out.println("loaded random forest: " + classifiername + " ("  + classifier.getNumTrees() + " trees, " + VisualizeClassifierImportances.ThreeDecimalDigitFormat.format(classifier.getError() * 100) + "% error)");
							ClassifierName.setText(classifiername);
							ClearClassifierButton.setEnabled(true);
							ChooseClassifierButton.setEnabled(false);
						} catch (Exception exc){
							exc.printStackTrace();							
							ClearClassifier();
						}
					}
					else {
						ClearClassifier();
					}
				}
			}
		);
		ClearClassifierButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					ClearClassifier();
				}
			}
		);
		RunClassifyButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					if (classifier == null){ //we don't do any checks if a classifier is supplied
						HashSet<String> delinquents = Run.it.EnoughPhenotypeTrainPoints();
						if ((Run.it.getPhenotyeNamesWithFindPixels()).size() == 0){
							JOptionPane.showMessageDialog(Run.it.getGUI(),"No phenotypes set to find Pixels");
							return;
						}
						else if (delinquents.size() > 0){
							JOptionPane.showMessageDialog(Run.it.getGUI(),"Not enough training points in phenotype(s): " + IOTools.StringJoin(delinquents, ", "));
							return;
						}
						if(Run.it.classification_choice != "CNN_select") {
                            ImageSetInterfaceWithUserColors set = Run.it.getUserColorsImageset();
                            if (set != null) {
                                if (!set.EnoughStainTrainPoints()) {
                                    JOptionPane.showMessageDialog(Run.it.getGUI(), "Not enough training points in one or more stains");
                                    return;
                                } else if (!set.StainCubesComputed()) {
                                    JOptionPane.showMessageDialog(Run.it.getGUI(), "Stain information not computed");
                                    return;
                                }
                            }
                        }
						ClassificationBegun();	
						AddInitialProgressBars();
					}
					else
						ClassificationBegun();										
					Run.it.DoClassificationOnSepThread(openProgress, trainingProgress, buildProgress, classifier);
					repaint();
						
				}
			}
		);		
		RunClassifyAndPostProcessButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					if (classifier == null){ //we don't do any checks if a classifier is supplied
						HashSet<String> delinquents = Run.it.EnoughPhenotypeTrainPoints();
						if ((Run.it.getPhenotyeNamesWithFindPixels()).size() == 0){
							JOptionPane.showMessageDialog(Run.it.getGUI(),"No phenotypes set to find Pixels");
							return;
						}
						else if (delinquents.size() > 0){
							JOptionPane.showMessageDialog(Run.it.getGUI(),"Not enough training points in phenotype(s): " + IOTools.StringJoin(delinquents, ", "));
							return;
						}
						else if ((Run.it.getPhenotyeNamesSaveNONAndFindCenters()).size() == 0)
							JOptionPane.showMessageDialog(Run.it.getGUI(),"No phenotypes set to find centers");
						if(Run.it.classification_choice != "CNN_select") { //With CNN there is no need for color information
                            ImageSetInterfaceWithUserColors set = Run.it.getUserColorsImageset();
                            if (set != null) {
                                if (!set.EnoughStainTrainPoints()) {
                                    JOptionPane.showMessageDialog(Run.it.getGUI(), "Not enough training points in one or more stains");
                                    return;
                                } else if (!set.StainCubesComputed()) {
                                    JOptionPane.showMessageDialog(Run.it.getGUI(), "Stain information not computed");
                                    return;
                                }
                            }
                            ClassificationBegun();
                            AddInitialProgressBars();
                        }
					}
					else
						ClassificationBegun();										
					Run.it.DoBothOnSepThread(openProgress, trainingProgress, buildProgress, classifier);
					repaint();						
				}
			}
		);		
		RunFindCenters.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if ((Classify.AllClassified()).size() == 0)
						JOptionPane.showMessageDialog(Run.it.getGUI(),"No images were fully classified. Please reclassify.");
					else if ((Run.it.getPhenotyeNamesSaveNONAndFindCenters()).size() == 0)
						JOptionPane.showMessageDialog(Run.it.getGUI(),"No phenotypes set to find centers");	
					else {
						ClassificationBegun();	
//						ReenableStopButton();
						Run.it.DoPostProcessOnSepThread(openProgress, trainingProgress, buildProgress, classifier);
						repaint();
					}						
				}
			}
		);
		erosion_method_radio_button.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent arg0) {
						Run.it.imageset.setCentroidFinderMethod(CentroidFinder.LabelViaSmartErosionsMethod);
						Run.it.GUIsetDirty(true);
					}
				}
			);		
	}
	/** the user has chosen to stop the current action. The stop command is passed along to
	 * {@link Run#StopClassifying() the stop function in Operations's Run object}, the
	 * display image is set blank, the classification bars are removed, the options are reenabled
	 */
	public void StopProcessing(){
		Run.it.StopClassifying();
		setDisplayImage(null);
		repaintImagePanel();
		RemoveAllBars();
		EnableButtonsByClassificationChoice();
//		ReenableMostButtons();
		ChooseClassifierButton.setEnabled(true);
		RunClassifyButton.setEnabled(true);
		RunClassifyAndPostProcessButton.setEnabled(true);
		StopButton.setEnabled(false);
		RunFindCenters.setEnabled(true);
		RunRetrainButton.setEnabled(true);	
	}
	/** the user has chosen to begin a classification. Previous progress bars are removed,
	 * and most options are disabled
	 */
	protected void ClassificationBegun(){
		RemoveAllBars();
		ChooseClassifierButton.setEnabled(false);
		RunClassifyButton.setEnabled(false);
		RunClassifyAndPostProcessButton.setEnabled(false);
		RunFindCenters.setEnabled(false);
		StopButton.setEnabled(true); //to be enabled later after Mahalanobis cubes opened (see MasterClassify in Run)
		RunRetrainButton.setEnabled(false);
		DisableMostButtons();
	}
	
	/** during a classification and/or post-processing the user is unable to manipulate options */
	protected void DisableMostButtons(){		
		numTreesSpinner.setEnabled(false);
		numThreadsSpinner.setEnabled(false);
		numPixelSkipSpinner.setEnabled(false);
//		useEdgesLabel.setEnabled(false);
//		useEdgesCheckBox.setEnabled(false);
		
//		numBatchSpinner.setEnabled(false);
//		numPiecesSpinner.setEnabled(false);
//		numRotationsSpinner.setEnabled(false);
		allButton.setEnabled(false);
		onlyButton.setEnabled(false);
		remainButton.setEnabled(false);
//		rangeButton.setEnabled(false);
//		tenRandomButton.setEnabled(false);
//		twentyRandomButton.setEnabled(false);
//		nRandomButton.setEnabled(false);
//		thoseClickedOnButton.setEnabled(false);
//		nRandomText.setEnabled(false);
//		rangeText.setEnabled(false);

		titleLabel.setEnabled(false);
		//numTreesBox.setVisible(false);
		//numThreadsLabel.setEnabled(false);
		//numPixelSkipLabel.setEnabled(false);
//		rotationalLabel.setEnabled(false);
//		numPiecesLabel.setEnabled(false);
		picturesToClassifyLabel.setEnabled(false);
	}
	/** the classification has completed and options should be reenabled */
	public void ClassificationDone(){
		RunFindCenters.setEnabled(true);
		RunRetrainButton.setEnabled(true);
		StopButton.setEnabled(false);
		ChooseClassifierButton.setEnabled(true);
		RunClassifyButton.setEnabled(true);
		RunClassifyAndPostProcessButton.setEnabled(true);
		EnableButtonsByClassificationChoice();
	}



	private void EnableButtonsByClassificationChoice(){
		RF_selected.setEnabled(true);
		CNN_selected.setEnabled(true);

		if (RF_selected.isSelected()){
            Run.it.classification_choice = "RF_select";
			EnableRFButtons();

		}

		if(CNN_selected.isSelected()){
            Run.it.classification_choice = "CNN_select";
			EnableCNNButtons();

		}

	}

	private void EnableCNNButtons() {


		//imageHeight.setEnabled(true);
		//imageWidth.setEnabled(true);
//		channels.setEnabled(true);
//		numExamples.setEnabled(true);
		//numLabels.setEnabled(true);
		//numLabels.setVisible(true);

		batchSize.setEnabled(true);
		batchSizeBox.setVisible(true);

		iterations.setEnabled(true);
		iterationsBox.setVisible(true);

		epochs.setEnabled(true);
		epochsBox.setVisible(true);

		//splitTrainTest.setEnabled(true);
		register_values_button.setVisible(true);

		allButton.setEnabled(true);
		onlyButton.setEnabled(true);
		remainButton.setEnabled(true);
//		rangeButton.setEnabled(true);
//		tenRandomButton.setEnabled(true);
//		twentyRandomButton.setEnabled(true);
//		nRandomButton.setEnabled(true);
//		thoseClickedOnButton.setEnabled(true);
//		nRandomText.setEnabled(true);
//		rangeText.setEnabled(true);
//		rangeText.setEnabled(true);

		titleLabel.setEnabled(true);
		numTreesBox.setEnabled(false);
        numTreesBox.setVisible(false);

		numThreadsLabel.setEnabled(true);
		numPixelSkipLabel.setEnabled(true);
//		rotationalLabel.setEnabled(true);
//		numPiecesLabel.setEnabled(true);
		picturesToClassifyLabel.setEnabled(true);
		if (allButton.isSelected())// || rangeButton.isSelected())
			RunClassifyAndPostProcessButton.setEnabled(true);
		
	    RunClassifyAndPostProcessButton.setEnabled(false);
	    RunClassifyButton.setEnabled(false);		
	    Run.it.getGUI().HideColorTrainTab();
	}







	/** option buttons are reenabled */
	private void EnableRFButtons(){

		numTreesBox.setEnabled(true);
		numTreesBox.setVisible(true);
        //numTreesSpinner.setVisible(true);

        batchSize.setEnabled(true);
        batchSizeBox.setVisible(false);

        iterations.setEnabled(true);
        iterationsBox.setVisible(false);

        epochs.setEnabled(true);
        epochsBox.setVisible(false);

		//numThreadsSpinner.setEnabled(true);
		//numThreadsSpinner.setVisible(true);

		//numPixelSkipSpinner.setEnabled(true);
		//numPixelSkipSpinner.setVisible(true);
//		useEdgesLabel.setEnabled(true);
//		useEdgesCheckBox.setEnabled(true);		
//		numBatchSpinner.setEnabled(true);
//		numPiecesSpinner.setEnabled(true);
//		if ((Integer)numPiecesSpinner.getValue() > 0) 
//			numRotationsSpinner.setEnabled(true);
		allButton.setEnabled(true);
		onlyButton.setEnabled(true);
		remainButton.setEnabled(true);

		register_values_button.setVisible(false);
//		rangeButton.setEnabled(true);
//		tenRandomButton.setEnabled(true);
//		twentyRandomButton.setEnabled(true);
//		nRandomButton.setEnabled(true);
//		thoseClickedOnButton.setEnabled(true);
//		nRandomText.setEnabled(true);
//		rangeText.setEnabled(true);
//		rangeText.setEnabled(true);
		
		titleLabel.setEnabled(true);
		numTreesLabel.setEnabled(true);
		numThreadsLabel.setEnabled(true);
		numPixelSkipLabel.setEnabled(true);
//		rotationalLabel.setEnabled(true);
//		numPiecesLabel.setEnabled(true);
		picturesToClassifyLabel.setEnabled(true);
		if (allButton.isSelected())// || rangeButton.isSelected())
			RunClassifyAndPostProcessButton.setEnabled(true);

	    RunClassifyAndPostProcessButton.setEnabled(true);
	    RunClassifyButton.setEnabled(true);		
	    Run.it.getGUI().ShowColorTrainTab();
	}

	public void setIs(HashMap<String,BoolMatrix> is){
		this.is=is;		
	}
	/** passes the {@link KImagePanel#repaint() repaint on to the image panel} */
	public void repaintImagePanel() {
		imagePanel.repaint();		
	}
	/** resets the progress bars to a supplied box */
	public void AddOrEditImageProgressBars(Box box){
		remove(centerBox);
		
		for (Component c:centerBox.getComponents())
			if (c == classifyBox)
				centerBox.remove(classifyBox);
		
		classifyBox=box;
		centerBox.add(classifyBox);
		add(centerBox,BorderLayout.CENTER);
		Run.it.FrameRepaint();
	}
	/** removes all progress bars from the South-Central region */
	public void RemoveAllBars(){
		remove(centerBox);
		for (Component c:centerBox.getComponents())
			if (c == preProcessingBarsBox)
				centerBox.remove(preProcessingBarsBox);
			else if (c == classifyBox)
				centerBox.remove(classifyBox);
		add(centerBox,BorderLayout.CENTER);
		Run.it.FrameRepaint();
	}
	/** adds the initial progress bars that reflect pre-processing progress 
	 * (ie opening Mahalanobis cubes, building training data, and building the
	 * machine learning classifiers)
	 */ 
	private void AddInitialProgressBars(){
		remove(centerBox);
		for (Component c:centerBox.getComponents())
			if (c == preProcessingBarsBox)
				centerBox.remove(preProcessingBarsBox);
			else if (c == classifyBox)
				centerBox.remove(classifyBox);
		
		preProcessingBarsBox=Box.createVerticalBox();
		
//		JLabel openLabel=new JLabel();
		openProgress=new JProgressBarAndLabel(0,100,"opening color information . . .");
//		openProgress.setStringPainted(true); 
//		JLabel trainingLabel=new JLabel("creating phenotype data . . .");
		trainingProgress=new JProgressBarAndLabel(0,100,"creating phenotype data . . .");
//		trainingProgress.setStringPainted(true); 
//		JLabel buildLabel=new JLabel("building classifiers . . .");
		buildProgress=new JProgressBarAndLabel(0,100,"building classifiers . . .");
//		buildProgress.setStringPainted(true); 
		
//		final Box openBox=Box.createVerticalBox();
//		openBox.add(openLabel);
//		openBox.add(openProgress);
		if (!(Run.it.imageset instanceof NuanceImageListInterface))
			preProcessingBarsBox.add(openProgress.getBox());		
//		final Box trainBox=Box.createVerticalBox();
//		trainBox.add(trainingLabel);
//		trainBox.add(trainingProgress);
		preProcessingBarsBox.add(trainingProgress.getBox());		
//		final Box buildBox=Box.createVerticalBox();
//		buildBox.add(buildLabel);
//		buildBox.add(buildProgress);
		preProcessingBarsBox.add(buildProgress.getBox());
		
		buildProgress.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					if(buildProgress.getValue() == 100){
//						try {
//							Thread.sleep(2000);
//						} catch (InterruptedException excpetion) {
//							excpetion.printStackTrace();
//						}
						preProcessingBarsBox.remove(openProgress.getBox());
						preProcessingBarsBox.remove(trainingProgress.getBox());
						preProcessingBarsBox.remove(buildProgress.getBox());
						Run.it.FrameRepaint();						
					}
				}
			}			
		);
		centerBox.add(preProcessingBarsBox);
		add(centerBox,BorderLayout.CENTER);
		Run.it.FrameRepaint();
	}
	public void ReenableClassifyButton() {
		ChooseClassifierButton.setEnabled(true);
		RunClassifyButton.setEnabled(true);	
	}
	/** when a project is loaded from the hard-disk, the user's preferences are populated into the Western region's option box */
	public void SetValuesToOpenProject(){
		numTreesSpinner.setValue((Integer)Run.it.num_trees);
		numThreadsSpinner.setValue((Integer)Run.it.num_threads);
		numPixelSkipSpinner.setValue((Integer)Run.it.pixel_skip - 1);
        epochs.setText(Integer.toString(Run.it.CNN_epoch_num));
        iterations.setText(Integer.toString(Run.it.CNN_iter_num));
        batchSize.setText(Integer.toString(Run.it.CNN_batch_num));
//		numBatchSpinner.setValue((Integer)Run.it.num_pixels_to_batch_updates);

        if(!Run.it.classification_choice.isEmpty() && Run.it.classification_choice!=null) {
            switch (Run.it.classification_choice) {
                case "RF_select":
                    RF_selected.setSelected(true);
                    // EnableRFButtons();
                    break;

                case "CNN_select":
                    CNN_selected.setSelected(true);
                    //EnableCNNButtons();
                    break;

            }
        }

		switch (Run.it.pics_to_classify){
			case CLASSIFY_ALL: 			allButton.setSelected(true); 			break;
			case CLASSIFY_TRAINED: 		onlyButton.setSelected(true);   		break;
			case CLASSIFY_REMAINING:	remainButton.setSelected(true);			break;
//			case TEN_RANDOM: 			tenRandomButton.setSelected(true);		break;
//			case TWENTY_RANDOM: 		twentyRandomButton.setSelected(true);	break;
//			case N_RANDOM:				nRandomButton.setSelected(true);		break;
//			case CLICKED_ON:			thoseClickedOnButton.setSelected(true);	break;
			case CLASSIFY_RANGE: 		//rangeButton.setSelected(true);
										//rangeText.setEnabled(true);
										RunClassifyAndPostProcessButton.setEnabled(true);
//										if (RangeToFilenames() == null)
//											RunClassifyButton.setEnabled(false);
//										else
											RunClassifyButton.setEnabled(true);
											ChooseClassifierButton.setEnabled(true);
										break;
		}
		switch (Run.it.imageset.getCentroidFinderMethod()){
			default: 
				erosion_method_radio_button.setSelected(true);
		}
	}
	private void ClearClassifier() {
		classifier = null;
		ClearClassifierButton.setEnabled(false);
		ChooseClassifierButton.setEnabled(true);
		ClassifierName.setText("");
	}
}