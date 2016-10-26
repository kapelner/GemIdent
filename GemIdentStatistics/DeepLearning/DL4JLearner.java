package GemIdentStatistics.DeepLearning;

import GemIdentClassificationEngine.DatumSetupForEntireRun;
import GemIdentStatistics.Classifier;
import GemIdentView.JProgressBarAndLabel;

public class DL4JLearner extends Classifier {
	private static final long serialVersionUID = 5794871567404918608L;

	public DL4JLearner(DatumSetupForEntireRun initDatumSetupForEntireRun, JProgressBarAndLabel buildProgress, int numPhenotypes) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void Build() {
		// play with Xy to get it into a Dl4j model
		//build / train the CNN here
		
	}

	@Override
	protected void FlushData() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double Evaluate(double[] record) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void StopBuilding() {
		// TODO Auto-generated method stub
		
	}

}
