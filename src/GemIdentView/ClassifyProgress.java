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

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import GemIdentOperations.Run;

/**
 * Controls the bars that update the user
 * on the progress of the classification. There
 * is one progress bar per thread executing and 
 * a total classification bar
 * 
 * @author Adam Kapelner
 */
public class ClassifyProgress {
	
	/** the percentage of total progress for pre-processing such as creating data structures and post-processsing such as saving files */
	private static final double processUnit=.05;
	/** the percentage of total progress that classification represents */
	private static final double allClassification=.90;
	
	/** an implementation of a JProgressBar specifically tailored to keeping track of a classification for ONE image */
	private class ClassifyBar{
		
		/** the label denoting the image being classified and the time remaining */
		private JLabel label;
		/** the internal progress bar */
		private JProgressBar bar;
		/** after a {@link GemIdentOperations.Run#num_pixels_to_batch_updates batch} of pixels have been classified, this is the unit of the progress bar advanced */
		private double update;
		/** the image whose progress is profiled by this ClassifyBar */
		private String filename;
		/** the number of seconds elapsed during the classification of this image thus far */
		private int sec;
		/** the total progress thus far (1-100) */
		private double upto;
		
		/** default constructor */
		public ClassifyBar(String filename){
			bar=new JProgressBar(0,100);
			bar.setStringPainted(true); 
			this.filename=filename;
			label=new JLabel();
			updateLabel(false);
			upto=0;
		}
		/** sets the time and updates the label */
		public void setTimeElapsed(int sec){
			this.sec=sec;
			updateLabel(true);
		}
		/** updates the label, displays the time elapsed if desired */
		private void updateLabel(boolean time){
			if (time)
				label.setText("Classifying "+filename+" "+sec+"s elapsed");
			else
				label.setText("Classifying "+filename);
		}
		/** the image has now been preprocessed */
		public void preprocessed(){
			upto+=processUnit*100;
			SetBar();			
		}
		/** sets the upto value in the progress bar */
		private void SetBar(){
			bar.setValue((int)Math.round(upto));
		}
		/** updates the progress bar */
		public void update(){	
			upto+=update*100;
			SetBar();
		}
		/** the image is now finished */
		public void finished(){
			upto=100;
			SetBar();
			label.setText(filename+" finished in "+sec+"s");
		}
		/** sets the number of pixels that will be classified in this image */
		public void setNPixels(int nPixels){
			update=Run.it.num_pixels_to_batch_updates*allClassification/((double)nPixels);
		}
		/** returns the components of this classify bar object */
		public Box getBox(){
			Box box=Box.createVerticalBox();
			box.add(label);
			box.add(bar);
			return box;
		}
		/** after an image is done, this sets the new image name and resets the lable and bar */
		public void setFilenameAndReset(String filename) {
			this.filename=filename;
			upto=0;
			SetBar();
			updateLabel(false);			
		}
		public double getUpdatePercent(){
			return update;
		}
		/** the classification results, as binary BMP's, are being written to the hard disk */
		public void WritingImages() {
			upto+=processUnit*100;
			SetBar();			
		}
	}
	
	/** the portion of a PostProcessBar's progress apportioned to the {@link GemIdentCentroidFinding.PostProcess#CreateHeuristicClassifier() creation of the heuristic classifiers} */
	private static final double createdHeuristicClassifier=.05;
	/** the portion of a PostProcessBar's progress apportioned to actually {@link GemIdentCentroidFinding.LabelViaSmartErosions#EvaluateImages pinpointing centroids in classification results} */
	private static final double postProcessing=.95;
	
	/** the class that encapsulates a progress bar and label to give inform the user on the progress of the {@link GemIdentCentroidFinding.PostProcess postprocessing} */
	private class PostProcessBar{
		
		/** the informative label above the progress bar */
		private JLabel label;
		/** the actual progress bar */
		private JProgressBar bar;
		/** the amount to bump up the progress bar by after the post-processing of one image */
		private double update;
//		private long sec; //can't get this to display correctly, so it's left out
		private double upto;
		
		/** default constructor */
		public PostProcessBar(){
			bar=new JProgressBar(0,100);
			bar.setStringPainted(true); 
			label=new JLabel();
			updateLabel(false,null);
//			PostStartTimer();
		}
		/** updates the label - displays the filename currently being processed */
		private void updateLabel(boolean time,String filename){
			if (time)
				label.setText("Finding centroids in "+filename);
//				label.setText("Finding centroids in "+filename+" "+PostTimeElapsed()+"s elapsed");
			else
				label.setText("Post-Processing");
		}
		/** the heuristic rules have now been created, and the post-processing of images can begin */
		public void CreatedClassifier(int n){
			upto+=createdHeuristicClassifier;
			update=postProcessing/n;
			SetBar();
		}
		/** being post-processing a particular image */
		public void BeginPicture(String filename,long sec){
//			this.sec=sec;
			updateLabel(true,filename);
		}
		/** update the progress bar */
		public void update(){			
			upto+=update;
			SetBar();
		}
		/** set the progress bar to the integer rounded upto value */
		private void SetBar(){
			bar.setValue((int)Math.round(100*upto));
		}
		/** return the components of this PostProcessBar */
		public Box getBox(){
			Box box=Box.createVerticalBox();
			box.add(label);
			box.add(bar);
			return box;
		}
//		private void PostStartTimer(){
//			sec=System.currentTimeMillis();
//		}
//		private int PostTimeElapsed(){
//			return (int)(System.currentTimeMillis()-sec)/1000;
//		}		
	}
	
	/** The box that holds all the classify progress bars as well as the total progress bar */
	private Box allBarsBox;
	/** The mapping from thread name to classify bar */
	private HashMap<String,ClassifyBar> classifyBars;
	/** The post-process bar (only active when post-processing */
	private PostProcessBar postProcessBar;
	/** The progress bar that tracks the total progress of the entire classification */
	private JProgressBar totProgressBar;
	/** The label that forms the user of the total progress */
	private JLabel totProgressLabel;
	/** the total number of images to be classified */
	private double totNumPics; //i'm not going to cast, this is more convenient
	/** the list of times (in seconds) for each image classified thus far */
	private ArrayList<Integer> times;
	/** the number of images classified thus far */
	private int numClassifiedThusFar;
	/** the total progress thus far (0-100) */
	private double totProgress;
	/** the time that the classification began */
	private long time_o;
	/** is <b>GemIdent</b> post-processing now? */
	private boolean postprocessing;
	/** message to display after the classification has completed */
	private String finishedMessage;
	/** the total time that the classification has taken */
	private String finishedtime;

	/** default instantiation of objects - there will be {@link Run#num_threads NUM_THREADS} ClassifyBar objects */
	public ClassifyProgress(int totNumPics){	
		this.totNumPics=totNumPics;
		times=new ArrayList<Integer>();
		numClassifiedThusFar=0;
		totProgress=0;
		postprocessing=false;
		classifyBars=new HashMap<String,ClassifyBar>(Run.it.num_threads);
		allBarsBox=Box.createVerticalBox();

		totProgressLabel=new JLabel();
		totProgressBar=new JProgressBar(0,100);
		totProgressBar.setStringPainted(true); 
		
		final Box totProgressBox=Box.createVerticalBox();
		totProgressBox.add(totProgressLabel);
		totProgressBox.add(totProgressBar);
		allBarsBox.add(totProgressBox);	
	}	
	public Box getBox() {
		return allBarsBox;
	}
	/** create a new classify bar for a certain thread and a certain image */
	public void NewClassifyBar(String threadName, String filename) {
		if (classifyBars.containsKey(threadName)){
			classifyBars.get(threadName).setFilenameAndReset(filename);
		}
		else {
			ClassifyBar bar=new ClassifyBar(filename);
			classifyBars.put(threadName,bar);
			allBarsBox.add(bar.getBox());
		}
		UpdateTotProgressLabel();
	}
	/** update the total progress label checks if classification has completed */
	private void UpdateTotProgressLabel(){
		finishedtime=Run.TimeElapsed(time_o);
		if (numClassifiedThusFar == totNumPics && !postprocessing){			
			Run.it.imageset.LOG_AddToHistory("finished classification in " + finishedtime);
			finishedMessage="Finished classification of "+(int)totNumPics+" image(s) in "+finishedtime+" (average "+GetAverageTime()+"s/image)";
			totProgressLabel.setText(finishedMessage);
			totProgressBar.setValue(100);
		}
		else if (numClassifiedThusFar == totNumPics && postprocessing){
			totProgressLabel.setText("Finished classification of "+(int)totNumPics+" image(s) in "+finishedtime+"s now postprocessing . . .");
			Run.it.imageset.LOG_AddToHistory("finished classification in " + finishedtime);
			totProgressBar.setValue(100);
		}
		else if (times.size() == 0)
			totProgressLabel.setText("<html><b>Total Progress</b> - Classifying image "+(numClassifiedThusFar+1)+" of "+(int)totNumPics+" time elapsed: "+Run.TimeElapsed(time_o)+"</html>");
		else
			totProgressLabel.setText("<html><b>Total Progress</b> - Classifying image "+(numClassifiedThusFar+1)+" of "+(int)totNumPics+" time elapsed: "+Run.TimeElapsed(time_o)+" (avg: "+GetAverageTime()+"s/image) Estimated remaining: "+GetEstimatedTimeRemaining()+"</html>");
	}
	/** gets the {@link #GetAverageTime() average classification time per image} and calculates the estimate of the total time remainining */
	private String GetEstimatedTimeRemaining(){
		int a=GetAverageTime();
		int r=(int)totNumPics-numClassifiedThusFar;		
		return Run.FormatSeconds((int)Math.round(a*r/((double)Run.it.num_threads)));
	}
	/** divides the total time elapsed by the number of images classified */
	private int GetAverageTime() {
		return (int)Math.round(GetTotalTimeElapsed()/((double)times.size()));
	}
	private int GetTotalTimeElapsed(){
		int T=0;
		for (int i:times)
			T+=i;
		return T;
	}
	/** updates all classify bars as well as the total progress bar and label */
	public void update(String threadName){
		classifyBars.get(threadName).update();	
		totProgress+=classifyBars.get(threadName).getUpdatePercent()*100/totNumPics;
		UpdateTotProgressBar();
		UpdateTotProgressLabel(); //just to get an accurate time elapsed reading
	}
	/** sets the number of pixels to be classified on this thread */
	public void setNPixels(String threadName,int nPixels) {
		classifyBars.get(threadName).setNPixels(nPixels);
	}
	/** the image on this thread is now preprocessed and classification can begin */
	public void preprocessed(String threadName){
		classifyBars.get(threadName).preprocessed();
		totProgress+=processUnit*100/totNumPics;
		UpdateTotProgressBar();
	}
	/** the image on this thread is now done being classified */
	public void finished(String threadName,int sec){
		classifyBars.get(threadName).finished();
		times.add(sec);
		totProgress+=processUnit*100/totNumPics;
		numClassifiedThusFar++;
		UpdateTotProgressBar();
	}
	/** update the time for a certain classify bar */
	public void setTime(String threadName,int sec) {
		classifyBars.get(threadName).setTimeElapsed(sec);	
	}
	/** update the total progress bar with the {@link #totProgress total progress} */
	private void UpdateTotProgressBar(){
		totProgressBar.setValue((int)Math.round(totProgress));		
	}
	/** after classification is completed, the classify bars are removed from the screen */
	public void RemoveClassifyBars(){
		UpdateTotProgressLabel();
		for (String key:classifyBars.keySet())
			allBarsBox.remove(classifyBars.get(key).getBox());
		Run.it.FrameRepaint();
	}
	/** after post-processing is completed, the post-process bar is removed from the screen */
	public void RemovePostProcessBar(){
		allBarsBox.remove(postProcessBar.getBox());
		Run.it.FrameRepaint();
	}
	public void StartTimer(){
		time_o=System.currentTimeMillis();
	}
	private int TimeElapsed(){
		return (int)(System.currentTimeMillis()-time_o)/1000;
	}
	/** the classification is completed for a given image and <b>GemIdent</b> is now writing the results to the hard disk */
	public void WritingImages(String threadName) {
		synchronized(classifyBars){
			classifyBars.get(threadName).WritingImages();
			totProgress+=processUnit*100/totNumPics;
			UpdateTotProgressBar();		
		}
	}
	/** post-processing begins - the PostProcess bar is added to the screen and the total progress bar is no longer visible */
	public void BeginPostProcessing(){
		StartTimer();
		postprocessing=true;
		postProcessBar=new PostProcessBar();
		allBarsBox.add(postProcessBar.getBox());
		if (finishedMessage == null){
			totProgressLabel.setVisible(false);
			totProgressBar.setVisible(false);
		}
		allBarsBox.repaint();
		Run.it.FrameRepaint();
	}
	public void updatePostProcessLoad() {
		postProcessBar.update();		
	}
	/** the heuristic classified is now completed and post-processing will begin */
	public void CreatedClassifier(int numPostProcess) {
		postProcessBar.CreatedClassifier(numPostProcess);		
	}
	/** the following image will now begin to be post-processed */
	public void BeginImagePostProcessing(String filename) {
		postProcessBar.BeginPicture(filename,TimeElapsed());
		UpdateTotProgressLabelPost(filename);
	}
	/** the lable will reflect completion of the post-processing */
	private void UpdateTotProgressLabelPost(String filename) {
		totProgressLabel.setText(finishedMessage+"  post-processing "+filename);
	}
}