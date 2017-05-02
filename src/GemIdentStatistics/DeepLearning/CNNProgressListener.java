/*-
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package GemIdentStatistics.DeepLearning;

import java.util.List;
import java.util.Map;

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.nd4j.linalg.api.ndarray.INDArray;

import GemIdentView.JProgressBarAndLabel;

/**
 * GemIdent Score iteration listener
 *
 * @author Adam Kapelner
 */
public class CNNProgressListener implements TrainingListener {
	private static final long serialVersionUID = -7189895102160770707L;
	
    private boolean invoked = false;
    //Added variable for GUI training bar
    private double currentProgress = 0;
	private double progressIncrementor;
	private JProgressBarAndLabel buildProgressBar;


    /** Default constructor printing every 10 iterations 
     * @param buildProgressBar 
     * @param progressIncrementor */
    public CNNProgressListener(double progressIncrementor, JProgressBarAndLabel buildProgressBar) {
    	this.progressIncrementor = progressIncrementor;
    	this.buildProgressBar = buildProgressBar;
    }

    @Override
    public boolean invoked() {
        return invoked;
    }

    @Override
    public void invoke() {
        this.invoked = true;
    }

    @Override
    public void iterationDone(Model model, int iteration) {
		currentProgress += progressIncrementor;
		buildProgressBar.setValue((int)Math.round(currentProgress));
//		System.out.println("iterationDone currentProgress = " + currentProgress + " progressIncrementor = " + progressIncrementor + " buildProgressBar: "+ buildProgressBar.getValue());
    }

	@Override
	public void onEpochStart(Model model) {
//		System.out.println("onEpochStart");
		
	}

	@Override
	public void onEpochEnd(Model model) {
//		System.out.println("onEpochEnd");
	}

	@Override
	public void onForwardPass(Model model, List<INDArray> activations) {
//		System.out.println("onForwardPass");
	}

	@Override
	public void onForwardPass(Model model, Map<String, INDArray> activations) {
//		System.out.println("onForwardPass");
	}

	@Override
	public void onGradientCalculation(Model model) {
//		System.out.println("onGradientCalculation");
	}

	@Override
	public void onBackwardPass(Model model) {
//		System.out.println("onBackwardPass");
		
	}

}
