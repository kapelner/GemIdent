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

package GemIdentClassificationEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import GemIdentClassificationEngine.DeepLearning.SingleImageDeepLearningClassifier;
import GemIdentOperations.Run;
import GemIdentStatistics.Classifier;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentView.ClassifyProgress;
import GemIdentView.KClassifyPanel;

/**
 * The class responsible for loading the image files and classifying
 * their pixels into the user-defined phenotype classes by using
 * the previously built {@link GemIdentStatistics.Classifier machine learning classifier}.
 * This class is threaded because it was slowing down swing. When classifying lots of images - over 500, there is a small memory
 * leak that compounds. This was fixed but now images won't flash during classification.
 * We hope this will fix in a later release. For discussion
 * about classification in general, consult section 4 of the manual
 * 
 * @author Adam Kapelner
 */
public class Classify {

	/** The directory of the processed images */
	public static final String processedDir="processed";
	/** the thread pool responsible for classifying the images */
	private ExecutorService classifyPool;
	/** the progress bars associated with this classification */
	private ClassifyProgress progress;
	/** the panel this classification is taking place in */
	private KClassifyPanel classifyPanel;
	/** the machine learning classifier that will classify the image pixels */
	private Classifier classifier;
	/** should the classification be stopped? */
	private AtomicBoolean stop;
	/** the images to classify */
	private Collection<String> filenames;

	/**
	 * Initializes the classification of an image set.
	 * @param datumSetupForImage 
	 * 
	 * @param filenames				the images to classify
	 * @param classifier			the machine learning classifier to use
	 * @param progress				the progress bar to use to give the user updates on progress
	 * @param classifyPanel			the panel the classification is taking place in
	 */
	public Classify(Collection<String> filenames, Classifier classifier, ClassifyProgress progress, KClassifyPanel classifyPanel){

//		System.out.println("Classify " + classifier);
		this.classifyPanel = classifyPanel;
		this.progress = progress;
		this.filenames = filenames;		
		this.classifier = classifier;
		stop = new AtomicBoolean();
		//go for the gold
		BeginClassification();
	}
	
	public static enum MODEL_TYPE {CLASSIC_MACHINE_LEARNING, DEEP_LEARNING}
//	public static MODEL_TYPE MODEL;

	/**
	 * Begins the classification. Starts the timer, initializes the thread pool, 
	 * then populates it with a worker class for each image in the set
	 */
	private void BeginClassification() {
//		System.out.println("BeginClassification ");
		progress.StartTimer();
		Run.it.imageset.LOG_AddToHistory("begun classification of " + filenames.size() + " images");

		classifyPool = Executors.newFixedThreadPool(Run.it.num_threads);

		for (String filename:filenames){
			switch (Run.it.classification_method){
				case Run.CLASSIFIER_CNN:
					new SingleImageDeepLearningClassifier(classifyPool, filename, progress, classifyPanel, classifier, stop); break;
				case Run.CLASSIFIER_RF:
				default:
					classifyPool.execute(new SingleImageClassicClassifier(filename, progress, classifyPanel, classifier, stop)); break;
			}
		}	
		classifyPool.shutdown();
		try {	         
	         classifyPool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
	    } catch (InterruptedException ignored){}
		
//		for (String filename:filenames)
//			new SingleImageClassifier(filename).run();
		progress.RemoveClassifyBars();
	}
	/**
	 * First prompts the user then deletes all 
	 * processed files from a the processed directory. If killOutputDir,
	 * it does the same with the output files from the output directory
	 */
	public static void PromptToDeleteAlProjectFiles(){
		String[] allFiles;
		allFiles=(new File(Run.it.imageset.getFilenameWithHomePath(processedDir+File.separator))).list();
		if (allFiles != null){
			if (allFiles.length > 0){
				int result = JOptionPane.showConfirmDialog(null,
						"Would you like to clear all previous identification data for this project?",
						"Warning",
						JOptionPane.YES_NO_OPTION);
				if ( result == JOptionPane.YES_OPTION){
					ArrayList<String> toBeDeleted=new ArrayList<String>();
					for (String file:allFiles)
						if (file.split("-")[0].equals(Run.it.projectName))
							toBeDeleted.add(file);
					
					for (String file:toBeDeleted)
						(new File(Run.it.imageset.getFilenameWithHomePath(processedDir+File.separator+file))).delete();
				}			
			}
		}
	}
	/** Stop the current classification (called when the user presses the "stop" button */
	public void Stop(){
		if (classifyPool != null){
			stop.set(true);
			classifyPool.shutdownNow(); //sends interrupt call to all threads
		}
	}



	/**
	 * Get the path to the marked image copy. The filename includes the project name
	 * (so multiple projects can be stored within one project directory without
	 * conflicts)
	 * 
	 * @param filename		the name of the original image
	 * @return				the path (relative to the project folder) of the marked image
	 */
	public static String GetIntermediateName(String filename) {
		return processedDir+File.separator+Run.it.projectName+"-i"+IOTools.GetFilenameWithoutExtension(filename)+".tif";
	}
	/**
	 * Get the path to the result matrix for a given image and a given phenotype
	 * 
	 * @param filename			the name of the image
	 * @param phenotype			the resulting matrix for this phenotype
	 * @return					the path (relative to the project folder) of the result image
	 */
	public static String GetIsName(String filename, String phenotype) {
		return processedDir+File.separator+Run.it.projectName+"-is"+phenotype+IOTools.GetFilenameWithoutExtension(filename)+".bmp";
	}
	
	public static String GetConfusionName(String filename){
		return processedDir + File.separator + Run.it.projectName + "-confusion-" + filename + ".tif";
	}

	/**
	 * Gets a list of all images classified
	 * 
	 * @return		a list of those images classified previously
	 */
	public static ArrayList<String> AllClassified(){
		ArrayList<String> allFiles=Run.it.imageset.GetImages();
		ArrayList<String> set=new ArrayList<String>();
		for (String filename:allFiles)
			if (Run.it.imageset.PicWasClassified(filename))
				set.add(filename);
		return set;
	}
	/**
	 * After classification, the result matrix is preprocessed, first to clear holes
	 * created from a greater-than-one pixel skip, then it's "closed" (eroded and dilated)
	 * to eliminate noise then "opened" (dilated and eroded) to eliminate holes
	 * 
	 * @param B			the {@link BoolMatrix BoolMatrix} result matrix to be preprocessed
	 * @return			the result matrix preprocessed
	 */
	public static synchronized BoolMatrix PreProcessIsImage(BoolMatrix B){
		for (int i=0;i<Run.it.pixel_skip-1;i++)
			B=B.Dilate();
		for (int i=0;i<Run.it.pixel_skip-1;i++)
			B=B.Erode();
		
		//close and open		
		B=B.Erode(); //get rid of junk		
		B=B.Dilate();
		B=B.Dilate(); //put close non-junk together
		B=B.Dilate();
		B=B.Erode();
		B=B.Erode();
		
		return B;
	}
}