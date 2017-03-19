package GemIdentClassificationEngine;

import java.awt.Point;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;


import GemIdentOperations.Run;
import GemIdentStatistics.Classifier;
import GemIdentView.ClassifyProgress;
import GemIdentView.KClassifyPanel;

public class SingleImageDeepLearningClassifier extends SingleImageClassicClassifier implements Runnable {

	public SingleImageDeepLearningClassifier(String filename, ClassifyProgress progress, KClassifyPanel classifyPanel,
			Classifier classifier, AtomicBoolean stop) {
		super(filename, progress, classifyPanel, classifier, stop);
		// TODO Auto-generated constructor stub
	}


	/**
	 * Classifies the every PIXEL_SKIP pixel in the image.
	 * For each pixel, create the evaluation data for it by
	 * constructing a {@link Datum Datum}
	 * object, then evaluate that data using the machine learning
	 * classifier and record the result in the respective result matrix.
	 * If the number of pixels classified is the same as the update 
	 * "batch size" then refresh the screen for the user and show
	 * the current results. This process is described
	 * in more detail and in the context of the overall project in step 7
	 * of the Algorithms section in the IEEE paper.
	 * 
	 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
	 */
	protected void ClassifyPixels(){
		new File((System.getProperty("user.dir")+"/Evaluation/")).mkdirs();
		int counter=0;
		classify_all_pixels : {
			for (int j=0;j<height;j++){
				for (int i=0;i<width;i++){
					if (((i+j) % Run.it.pixel_skip) == 0){
						if (stop.get())
							break classify_all_pixels;
						counter++;
						
						int resultClass = (int)classifier.Evaluate(filename, i, j);
						if (resultClass != 0)
							(is.get(Run.classMapBck.get(resultClass))).set(i,j,true);
						if (counter == Run.it.num_pixels_to_batch_updates && counter != 0){
							counter=0;
							new Thread(){ //thread this off to make classification faster
								public void run(){
//									UpdateImagePanel();
									UpdateProgressBar();									
								}
							}.start();							
						}
					}
				}
			}
		}
	}	
}
