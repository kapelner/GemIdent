package GemIdentStatistics.DeepLearning;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentStatistics.Classifier;
import GemIdentView.JProgressBarAndLabel;

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

        cnn = new AnimalsClassification();

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

                    buildProgress.setValue((int)Math.round(progress += ConvNet.getBuildProgress()));
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
            ConvNet.run(null);
        }catch (Exception e){
            e.printStackTrace();
        }

        try {
            treePool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
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
		
		
		return 0;
	}

	@Override
	public void StopBuilding() {
		// TODO Auto-generated method stub
		
	}

}
