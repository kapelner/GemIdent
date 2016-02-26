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

import java.io.File;
import java.sql.Timestamp;
import java.util.Collection;

import GemIdentCentroidFinding.PostProcess;
import GemIdentClassificationEngine.Classify;
import GemIdentClassificationEngine.Datum;
import GemIdentClassificationEngine.Features.DatumFeatureSet.FeatureSetName;
import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentClassificationEngine.TrainingData;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentStatistics.Classifier;
import GemIdentStatistics.CART.DTree;
import GemIdentStatistics.RandomForest.RandomForest;
import GemIdentTools.IOTools;
import GemIdentView.ClassifyProgress;
import GemIdentView.JProgressBarAndLabel;
import GemIdentView.KClassifyPanel;
import GemIdentView.KFrame;

/**
 * Class responsible for setting up classifications, centroid-findings, or both. 
 * It is threaded as to not hog swing.
 * 
 * @author Adam Kapelner
 */
public class SetupClassification extends Thread {		
	
	/** Pointer to the project data */
	private Run it;
	/** the progress bar that informs the user of the opening of the color cubes from the hard disk */
	private JProgressBarAndLabel openProgress;
	/** the progress bar that informs the user of the creation of the training data */
	private JProgressBarAndLabel trainingProgress;
	/** the progress bar that informs the user of the building of the machine learning classifiers */
	private JProgressBarAndLabel buildProgress;
	/** should <b>GemIdent</b> do a classification on the specified images? */
	private boolean toClassify;
	/** should <b>GemIdent</b> do a post-process to find centroids on the specified images? */
	private boolean toPostProcess;
	/** generates the user's phenotype training data in order to be used during creation of the machine learning classifiers */
	private TrainingData trainingData;
	/** generates the machine learning classifier, then uses it to evaluate the images */
	private Classifier classifier;
	/** controls the classification */
	private Classify classify;
	/** controls the post-processing to find centroids */
	private PostProcess postProcess;
	/** did the user push the stop button? */
	private boolean stopped;
	/** Pointer to the visual frame */
	private KFrame gui;
	/** pointer to the object that holds the image set data */
	private ImageSetInterface imageset;

	
	/** default constructor - also begins the thread */
	public SetupClassification(Run it, JProgressBarAndLabel openProgress,JProgressBarAndLabel trainingProgress,JProgressBarAndLabel buildProgress,boolean toClassify,boolean toPostProcess, Classifier classifier){
		this.it = it;
		gui = it.getGUI();
		imageset = it.imageset;
		this.toClassify = toClassify;
		this.toPostProcess = toPostProcess;
		this.openProgress = openProgress;
		this.trainingProgress = trainingProgress;
		this.buildProgress = buildProgress;
		this.classifier = classifier;
		stopped = false;
		Run.InitializeClassPhenotypeMap();
		start();
	}
	/** gets the list of images the user specified to classify,
	 * initializes the progress bars for to update the user on
	 * the progress of the classification, then if {@link #toClassify
	 * classify}, it classifies, and/or if {@link #toPostProcess postProcess},
	 * it will post process to find centroids
	 */
	public void run(){
		Collection<String> files = it.GetImageListToClassify();
		ClassifyProgress progress=new ClassifyProgress(files.size());
		gui.KillPhenotypeTab();
		it.resetTypeOneErrors();
		if (toClassify)
			DoTheClassification(files,progress);
		if (toPostProcess)
			PostProcess(files,progress);
		if (toClassify && toPostProcess){ //we should also do the sanity check thing if the user ran a totally automatic classification + centers
			gui.SwitchToAnalysisTab();
			gui.getParser().SanityCheck();
		}
	}
	/** runs a classification. First creates the processed directory (if
	 * needed), then prompts the user if he wants to flush the previous
	 * classification's files, flushes previously classified masks,
	 * as well as the error mapping, then attempts to open the color
	 * cubes from the hard disk. Then it populates the global {@link Datum 
	 * Datum} parameters, creates the {@link TrainingData#TrainingData
	 * training data} from the user's phenotype training points, creates
	 * a classification object, and attempts to run the classification 
	 * 
	 * @param files			the images to classify
	 * @param progress		the progress bars
	 */
	private void DoTheClassification(Collection<String> files, ClassifyProgress progress) {	
		CreateDirectoriesAndFlushRAM();
		
		//if the user didn't supply a classifier, we're going to have to build one:
		if (classifier == null){
			if (imageset instanceof ImageSetInterfaceWithUserColors){ //this is ugly but conceptually it's the only way to go I believe
				if (!((ImageSetInterfaceWithUserColors)imageset).OpenMahalanobisCubes(openProgress)){ //there was a problem with openCubes....
					gui.getClassifyPanel().ReenableClassifyButton();
					gui.getClassifyPanel().RemoveAllBars();
					return;
				}
			}
			//now we're good to go:
			trainingData = new TrainingData(it.num_threads, trainingProgress);

			if (!stopped){
				CreateClassifier();
			}
			trainingData = null; //we no longer need this, let's conserve RAM		
		}
		
		if (!stopped){
			classify = new Classify(files, classifier, progress, gui.getClassifyPanel());
		}
		gui.getClassifyPanel().ClassificationDone();
	}
	
	public static DatumSetupForEntireRun initDatumSetupForEntireRun(){
		//what features are we using? Declare a new datum setup for this run
		DatumSetupForEntireRun datumSetupForEntireRun = new DatumSetupForEntireRun(Run.it.imageset);
		datumSetupForEntireRun.addFeatureSet(FeatureSetName.ColorRingScores);
//		datumSetupForEntireRun.addFeatureSet(FeatureSetName.MaxLineScores);
//		datumSetupForEntireRun.addFeatureSet(FeatureSetName.EdgeRingScores);
		datumSetupForEntireRun.initialize();	
		return datumSetupForEntireRun;
	}
	
	/** The name of the "Random Forest" machine learning classifier */
	public static final String RandomForestSymbol = "Random Forest";
	/** The name of the "Decision Tree" machine learning classifier */
	public static final String CARTSymbol = "Decision Tree";
	/** The name of the "CGM98" BayesianCART machine learning classifier */
	public static final String BayesianCARTSymbol = "BayesianCART";	
	/** The name of the "Bayesian Additive Regression Trees" machine learning classifier */
	public static final String BARTSymbol = "BART";
	
	
	/** The current classifier the user is using */	
	public static String classifierType = RandomForestSymbol; //default is random forests for now
	
	/**
	 * This method creates the machine learning classifier for this analysis
	 */	
	private void CreateClassifier() {
		//this is where you can switch the machine learning engine if desired:
		classifier = null;
		if (classifierType.equals(RandomForestSymbol)){
			classifier = new RandomForest(initDatumSetupForEntireRun(), buildProgress, it.num_trees);
		}
		else if (classifierType.equals(CARTSymbol)){
			classifier = new DTree(initDatumSetupForEntireRun(), buildProgress);
		}
		//COMING SOON
//		else if (classifierType.equals(BayesianCARTSymbol)){
//			classifier = new CGMClassificationTree(datumSetupForEntireRun, buildProgress, Run.it.numPhenotypes());
//		}
//		else if (classifierType.equals(BARTSymbol)){
//			classifier = new CGMBARTClassification(datumSetupForEntireRun, buildProgress, Run.it.numPhenotypes());
//		}		
		classifier.setData(trainingData.getData());
		classifier.Build();	
//		classifier.dumpDataToFile(); //debugging
		System.out.println("all training data dumped");
		//now save the forest to the hd (on another thread to not slow us down):
		SaveClassifierToHardDrive();
	}
	
	/** Creates necessary directories for classification and centroid-finding, and flushes the caches to release memory */
	private void CreateDirectoriesAndFlushRAM() {
		//create directories, delete old files and initialize some vars
		if (!IOTools.DoesDirectoryExist(Classify.processedDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(Classify.processedDir))).mkdir();
		if (!IOTools.DoesDirectoryExist(PostProcess.analysisDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(PostProcess.analysisDir))).mkdir();
		if (!IOTools.DoesDirectoryExist(ImageSetInterface.checkDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(ImageSetInterface.checkDir))).mkdir();
		
		if (it.pics_to_classify != KClassifyPanel.CLASSIFY_REMAINING)
			Classify.PromptToDeleteAlProjectFiles();
		
		//we don't need these in memory any longer
		ImageAndScoresBank.FlushAllIsImages();			
	}
	
	/** Upon construction, dumps the classifier to disk in XML. Threaded as to not hog swing */
	private void SaveClassifierToHardDrive(){
		final Classifier final_classifier = classifier;
		new Thread(){
			public void run(){
				setPriority(Thread.MIN_PRIORITY);
				System.out.println("saving classifier to HD...");
				IOTools.saveToXML(final_classifier, imageset.getFilenameWithHomePath(it.projectName+".classifier"));
				System.out.println("done saving classifier");							
			}
		}.start();
	}
		
	/** runs a post-processing to find centroids. First creates the output 
	 * directory (if needed), flushes the error mapping, creates
	 * a post processing object, and attempts to run a post-processing,
	 * then saves the counts of the phenotypes and their false negative rates 
	 * 
	 * @param files			the images to post-process
	 * @param progress		the progress bars
	 */
	@SuppressWarnings("deprecation")
	private void PostProcess(Collection<String> files,ClassifyProgress progress) {
		Timestamp t = new Timestamp(System.currentTimeMillis());
		String date = t.toString().split(" ")[0];
		PostProcess.outputDir = PostProcess.outputDirFirstName + "--"+date+"-"+t.getHours()+"-"+t.getMinutes()+"-"+t.getSeconds();
		if (!IOTools.DoesDirectoryExist(PostProcess.outputDir))
			(new File(Run.it.imageset.getFilenameWithHomePath(PostProcess.outputDir))).mkdir();

		if (!stopped){
			it.resetTotalCounts();
			it.resetErrorRates();
			postProcess=new PostProcess(files, progress, gui.getClassifyPanel(), it.getTypeOneErrors());	
			postProcess.FindCentroids();
		}
		if (!stopped){
			it.setTotalCounts(postProcess.getTotalCounts());
			it.setErrorRates(postProcess.getErrorRates());
			postProcess=null; //we have to keep this guy around
			if (imageset instanceof ImageSetInterfaceWithUserColors){ //this is ugly but conceptually it's the only way to go I believe
				((ImageSetInterfaceWithUserColors)imageset).FlushCubes();
			}
			ImageAndScoresBank.FlushAllCaches();
			Run.SaveProject();
		}
	}
	/** Stops the current executing process */
	public void Stop(){
		stopped=true;
		try {
			if (trainingData != null)
				trainingData.Stop();
			if (classifier != null)
				classifier.StopBuilding();
			if (classify != null)
				classify.Stop();
			if (postProcess != null)
				postProcess.Stop();
		} catch (NullPointerException e){} //things aren't initialized yet
	}
}
