package GemIdentStatistics.DeepLearning;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentClassificationEngine.DeepLearning.DeepLearningTrainingData;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;
import GemIdentOperations.Run;
import GemIdentStatistics.Classifier;
import GemIdentView.JProgressBarAndLabel;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeepLearningCNNClassifier extends Classifier {
	private static final long serialVersionUID = 5794871567404918608L;
    /** the deep learning model */
	private DeepLearningCNN cnn;

    public DeepLearningCNNClassifier(DatumSetupForEntireRun initDatumSetupForEntireRun, JProgressBarAndLabel buildProgress, int numPhenotypes) {
		// TODO Auto-generated constructor stub
        super(initDatumSetupForEntireRun, buildProgress);

	}

	@Override
	public void Build(){

        cnn = new DeepLearningCNN.DeepLearningCNNBuilder()
        		.setProgressBar(buildProgress)
                .iterations(Run.it.CNN_iter_num)
                .splitPercentage(Run.it.CNN_split) //should come from user
                .epochs(Run.it.CNN_epoch_num)
                .build();
        try {
			cnn.run();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    
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
       try {
		return cnn.classify(
				   DeepLearningTrainingData.coreOutSuperImage(
						   ImageAndScoresBank.getOrAddSuperImage(filename), 
						   Run.it.getPhenotypeCoreImageSemiWidth(), 
						   new Point(i,j)
				   ), i, j, filename
			   );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
       return 0;
	}

}
