package GemIdentStatistics.DeepLearning;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentStatistics.Classifier;
import GemIdentView.JProgressBarAndLabel;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

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

        cnn = new DeepLearningCNN();

        ExecutorService coupled_threads = Executors.newFixedThreadPool(1);
        coupled_threads.execute(new Runnable(){
            public void run(){
                try {
                    cnn.run(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        coupled_threads.execute(new Runnable(){
            public void run(){
                while (!stop){

                    buildProgress.setValue((int)Math.round(progress += cnn.getBuildProgress()));
                    //System.err.println(ConvNet.getBuildProgress());
                    if(progress >= 100)
                        stop = true;
                    try {
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }


        });

        coupled_threads.shutdown();
        /*Builds and train network*/
        try {
        	cnn.run(null);
        }catch (Exception e){
            e.printStackTrace();
        }

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
	public double Evaluate(String filename2, int i, int j){

	    String filename = "C:/Users/stefh/Project GemIdent/GemIdent/examples_image_sets/tissue/Da120.jpg";

	    if( (i>10 && j >10) && (i<47 && j<47)) {
            //Step 1: load image
            BufferedImage file;
            BufferedImage region;
            try {
                file = ImageIO.read(new File(filename));

                //Step 2: extract region of interest +- w
                region = file.getSubimage(i, j, 10, 10);

                File training = new File(System.getProperty("user.dir") + "/Evaluation/X:" + i + "Y:" + j + ".jpg");
                //if(training.createNewFile()){System.out.println("CREATED FILE");}

                ImageIO.write(region,"JPEG",training);
                cnn.newEvaluate(training);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Step 3: write file

            //Step 4: create testDataSet filelist from this one file

            //Step 5: return network.predict(testDataSet).get(0);

            //Running predictions

        }
		
		return 0;
	}

	@Override
	public void StopBuilding() {
		stop = true;
	}

}
