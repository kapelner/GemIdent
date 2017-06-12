package GemIdentClassificationEngine;

import java.awt.Point;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;
import GemIdentImageSets.Nuance.NuanceImageListInterface;
import GemIdentOperations.Run;
import GemIdentOperations.SetupClassification;
import GemIdentStatistics.Classifier;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentView.ClassifyProgress;
import GemIdentView.KClassifyPanel;

/**
 * Class responsible for classifying one image, threaded in a thread pool
 */
public class SingleImageClassicClassifier implements Runnable {
	
	/** the image being classified */
	protected String filename;
	/** the beginning time of the classification */
	protected long time_o;
	/** the expanded {@link GemIdentImageSets.SuperImage SuperImage} of this image */
	protected SuperImage superImage;
	/** a copy of the original image */
	protected DataImage actualImageInter;
	/** a mapping from the phenotypes to their results matrix for this image */
	protected HashMap<String,BoolMatrix> is;
	/** the width of this image */
	protected int width;
	/** the height of this image */
	protected int height;
	/** the name of the thread this image is being classified on (for debugging purposes only) */
	private String threadName;
	/** common information for all datums in this image */
	protected DatumSetupForImage datumSetupForImage;
	/** the progress bars associated with this classification */
	protected ClassifyProgress progress;
	/** the panel this classification is taking place in */
	protected KClassifyPanel classifyPanel;
	/** the machine learning classifier that will classify the image pixels */
	protected Classifier classifier;
	/** should the classification be stopped? */
	protected AtomicBoolean stop;
	
	/** initializes a new Classifier worker
	 * 
	 * @param filename		the filename of the image
	 */
	public SingleImageClassicClassifier(String filename, ClassifyProgress progress, KClassifyPanel classifyPanel, Classifier classifier, AtomicBoolean stop){
		this.filename = filename;
		this.progress = progress;
		this.classifyPanel = classifyPanel;
		this.classifier = classifier;
		this.stop = stop;
	}

	/**
	 * Starts the classification on this image. Starts the timer, adds a progress bar
	 * to the user's panel, {@link #PreProcess() preprocesses}, initializes the
	 * images inside the user's panel, {@link #ClassifyPixels() classifies all pixels},
	 * writes the results to the processed directory, and then writes debug images to the processed
	 * directory
	 */
	public void run(){
		StartTimer();
		
		threadName=Thread.currentThread().getName();
		progress.NewClassifyBar(threadName, filename);			
		classifyPanel.AddOrEditImageProgressBars(progress.getBox());
		classifyPanel.setDisplayImage(null);
		
		PreProcess();
		datumSetupForImage = new DatumSetupForImage(SetupClassification.initDatumSetupForEntireRun(), filename);	
		
		
		
		progress.preprocessed(threadName);
		progress.setTime(threadName,TimeElapsed());
//			classifyPanel.setDisplayImage(actualImageInter); //LEAK!!!!!
//			classifyPanel.setIs(is);
//			classifyPanel.repaintImagePanel();
		progress.setNPixels(threadName,actualImageInter.numPixels()/Run.it.pixel_skip);

		System.out.println("begin classifying pixels in image "+filename+" . . . ");
		ClassifyPixels();		
		System.out.println("done classifying pixels in image "+filename);
		
		if (!stop.get()){
			progress.WritingImages(threadName);
			for (String phenotype:Run.it.getPhenotyeNamesSaveNONAndFindPixels()){
				//System.out.println("about to write ismatrix for file " + filename + " and phenotype " + phenotype + " and is matrix " + is.get(phenotype).toString());
				IOTools.WriteIsMatrix(Classify.GetIsName(filename,phenotype),is.get(phenotype));
			}
			
			if (!(Run.it.imageset instanceof NuanceImageListInterface)){ //too complicated to do this if its Nuance
				MarkIntermediatePixels();
				IOTools.WriteImage(Classify.GetIntermediateName(filename),"TIFF",actualImageInter);
			}
			progress.finished(threadName,TimeElapsed());
		}
	}
	
	/** 
	 * creates the necessary data for the classification: creates the {@link GemIdentImageSets.SuperImage SuperImage}
	 * of the image, creates a copy (for the debug image), computes the scores for
	 * the image, and then initializes the results matrix
	 */
	private void PreProcess(){
		is=new HashMap<String,BoolMatrix>(Run.it.numPhenotypes());
		superImage=ImageAndScoresBank.getOrAddSuperImage(filename);
		actualImageInter=superImage.getCenterImage().clone();
		width=actualImageInter.getWidth();
		height=actualImageInter.getHeight();
		for (String phenotype:Run.it.getPhenotyeNamesSaveNON())
			is.put(phenotype,new BoolMatrix(width,height));
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
		int counter=0;
		classify_all_pixels : {
			for (int j=0;j<height;j++){
				for (int i=0;i<width;i++){
					if (((i+j) % Run.it.pixel_skip) == 0){
						if (stop.get())
							break classify_all_pixels;
						counter++;

//						datumSetupForImage = new DatumSetupForImage(datumSetupForEntireRun, filename);
						Datum d = datumSetupForImage.generateDatumAtPoint(new Point(i, j));
						int resultClass = (int)classifier.Evaluate(d.getRecord());
						if (resultClass != 0)
							(is.get(Run.classMapBck.get(resultClass))).set(i,j,true);
						if (counter == Run.it.num_pixels_to_batch_updates && counter != 0){
							counter=0;
							new Thread(){ //thread this off to make classification faster
								public void run(){
									UpdateProgressBar();
									UpdateImagePanel();							
								}
							}.start();							
						}
					}
				}
			}
		}
	}
	/** update the progress bar for the user */
	protected void UpdateProgressBar(){
		progress.update(threadName);
		progress.setTime(threadName,TimeElapsed());			
	}
		/** show the current image being classified as well as the real time results */
	protected void UpdateImagePanel(){
		classifyPanel.setDisplayImage(superImage.getCenterImage()); //LEAK!!!!!
		classifyPanel.setIs(is);
		classifyPanel.repaintImagePanel();			
	}
	/** paint the results over a copy of the original image */
	private void MarkIntermediatePixels(){
		for (int i=0;i<width;i++){
			for (int j=0;j<height;j++){
				HashMap<String,Boolean> isij=new HashMap<String,Boolean>(Run.it.numPhenotypes());
				for (String phenotype:Run.it.getPhenotyeNamesSaveNON())
					isij.put(phenotype,is.get(phenotype).get(i,j));
				
				for (String phenotype:Run.it.getPhenotyeNamesSaveNON()){
					if (isij.get(phenotype)){
						actualImageInter.MarkPheno(i,j,phenotype);
					}	
				}
			}
		}			
	}
	/** start the timer for this image's classification */
	private void StartTimer() {
		time_o=System.currentTimeMillis();			
	}
	/** return the time elapsed thus far in seconds */
	private int TimeElapsed(){
		return (int)(System.currentTimeMillis()-time_o)/1000;
	}
}