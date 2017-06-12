package GemIdentClassificationEngine;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import GemIdentOperations.Run;
import GemIdentStatistics.Classifier;
import GemIdentTools.IOTools;
import GemIdentView.ClassifyProgress;
import GemIdentView.KClassifyPanel;

public class SingleImageDeepLearningClassifier extends SingleImageClassicClassifier implements Runnable {

	private ExecutorService classifyPool;


	public SingleImageDeepLearningClassifier(ExecutorService classifyPool, String filename, ClassifyProgress progress, KClassifyPanel classifyPanel,
			Classifier classifier, AtomicBoolean stop) {
		super(filename, progress, classifyPanel, classifier, stop);
//		System.out.println("SingleImageDeepLearningClassifier ");
		this.classifyPool = classifyPool;
		run();
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
		final AtomicInteger counter = new AtomicInteger(0);
		
		ArrayList<Point> all_training_points = Run.it.getAllTrainingPointsImage(filename);
		
//		System.out.println("ClassifyPixels " + filename);
		
		
		classify_all_pixels : {
			for (int j = 0; j < height; j++){
				for (int i = 0; i < width; i++){
					if (!all_training_points.contains(new Point(i, j))){
						continue;
					}
					if (stop.get()){
						classifyPool.shutdownNow();
						break classify_all_pixels;
					}
					if (((i+j) % Run.it.pixel_skip) == 0){
						int i_f = i;
						int j_f = j;
						classifyPool.execute(
							new Runnable(){
								public void run(){
										counter.incrementAndGet();
										//Where CNN gives its classification
										int resultClass = (int)classifier.Evaluate(filename, i_f, j_f);
	
										if (resultClass != 0)
											(is.get(Run.classMapBck.get(resultClass))).set(i_f, j_f, true);
										if (counter.get() % Run.it.num_pixels_to_batch_updates == 0){
											System.out.println("pixel " + filename + "(" +  i_f + " " + j_f + ") classified to be a " + resultClass);
											new Thread(){ //thread this off to make classification faster
												public void run(){									
													UpdateProgressBar();
													UpdateImagePanel();									
												}
											}.start();		
											for (String phenotype : Run.it.getPhenotyeNamesSaveNONAndFindPixels()){
												//System.out.println("about to write ismatrix for file " + filename + " and phenotype " + phenotype + " and is matrix " + is.get(phenotype).toString());
												IOTools.WriteIsMatrix(Classify.GetIsName(filename, phenotype),is.get(phenotype));
											}							
										}
									}
								}
							
						);
					}
				}
			}
		}
	}	
}
