package GemIdentStatistics.DeepLearning;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;
import GemIdentOperations.Run;
import GemIdentStatistics.Classifier;
import GemIdentView.JProgressBarAndLabel;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.awt.image.RasterFormatException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeepLearningCNNClassifier extends Classifier {
	private static final long serialVersionUID = 5794871567404918608L;
    /** For progress bar display for the creation of this DL4J Learner, this records the total progress */
    private transient double progress;
    private transient boolean stop;
    /** the deep learning model */
	private DeepLearningCNN cnn;

    public DeepLearningCNNClassifier(DatumSetupForEntireRun initDatumSetupForEntireRun, JProgressBarAndLabel buildProgress, int numPhenotypes) {
		// TODO Auto-generated constructor stub
        super(initDatumSetupForEntireRun, buildProgress);

	}

	@Override
	public void Build(){
		// play with Xy to get it into a Dl4j model
		//build / train the CNN here
        stop = false;

        cnn = new DeepLearningCNN.DeepLearningCNNBuilder()
                .batchSize(Run.it.CNN_batch_num)
                .iterations(Run.it.CNN_iter_num)
                .splitPercentage(Run.it.CNN_split)
                .batchSize(Run.it.CNN_batch_num)
                .epochs(Run.it.CNN_epoch_num)
                .build();

        ExecutorService coupled_threads = Executors.newFixedThreadPool(2);
        coupled_threads.execute(new Runnable(){
            public void run(){
                try {
                    cnn.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        coupled_threads.execute(new Runnable(){
            public void run(){
                while (!stop){

                    buildProgress.setValue((int)Math.round(progress += cnn.getBuildProgress()));
                    if(progress >= 100)
                        stop = true;
                    try {
                        Thread.sleep(500);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        coupled_threads.shutdown();
        /*Builds and train network*/
//        try {
//        	cnn.run(null);
//        }catch (Exception e){
//            e.printStackTrace();
//        }

        try {
        	coupled_threads.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
        } catch (InterruptedException ignored){}



    }

	@Override
	protected void FlushData() {
		// TODO Auto-generated method stub
		//
		
	}

	@Override
	public double Evaluate(double[] record) {
		// UNUSED!!!!!!
		return 0;
	}
	
	@Override
	public double Evaluate(String filename, int i, int j){
        //Step 1: load image
        //Use superImage to get reflection at edges
        SuperImage superImage = ImageAndScoresBank.getOrAddSuperImage(filename);
        Point adjustedPoint = superImage.AdjustPointForSuper(new Point(i,j));
        BufferedImage fullImage = superImage.getAsBufferedImage();
        BufferedImage imageAtPoint = null;
        //not a true fix just want to see how classifications behave. if out of bounds just take center of image.
        int r_max = Run.it.getMaxPhenotypeRadiusPlusMore(null);
        try {
            imageAtPoint = fullImage.getSubimage(adjustedPoint.x - (r_max / 2), adjustedPoint.y - (r_max / 2), r_max* 2, r_max *2);
        } catch(RasterFormatException e){
            System.out.println("Out of bounds raster, Filename is:" + filename + '\n'+
                            "Point IS x:" + adjustedPoint.x + "y" + adjustedPoint.y);
                    imageAtPoint = fullImage.getSubimage(fullImage.getWidth()/2,fullImage.getHeight()/2,r_max* 2,r_max* 2);
        }

        return cnn.feedForwardImage(imageAtPoint);




            //Step 3: write file

            //Step 4: create testDataSet filelist from this one file

            //Step 5: return network.predict(testDataSet).get(0);

            //Running predictions
	}

	@Override
	public void StopBuilding() {
		stop = true;
	}

}
