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

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.IterationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Score iteration listener
 *
 * @author Adam Gibson
 */
public class OurScoreIterationListener implements IterationListener {
    /**
	 * 
	 */
	private static final long serialVersionUID = -7189895102160770707L;
	
	
	private int printIterations = 10;
    private static final Logger log = LoggerFactory.getLogger(OurScoreIterationListener.class);
    private boolean invoked = false;

    private volatile long iterCount = 0;

    /**
     * @param printIterations    frequency with which to print scores (i.e., every printIterations parameter updates)
     */
    public OurScoreIterationListener(int printIterations) {
        this.printIterations = printIterations;
    }

    /** Default constructor printing every 10 iterations */
    public OurScoreIterationListener() {}

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
        if (printIterations <= 0)
            printIterations = 1;
        if (iterCount % printIterations == 0) {
            invoke();
            double result = model.score();
            log.info("Score at iteration " + iterCount + " is " + result);
        }
        iterCount++;
        System.out.println(iterCount);
    }

    public long getIterCount() {
        return iterCount;
    }

}
