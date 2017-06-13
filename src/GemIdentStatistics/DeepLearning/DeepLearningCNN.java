package GemIdentStatistics.DeepLearning;

import GemIdentOperations.Run;
import GemIdentTools.IOTools;
import GemIdentView.JProgressBarAndLabel;

import org.apache.commons.io.FilenameUtils;
import org.datavec.api.io.filters.BalancedPathFilter;
import org.datavec.api.io.filters.PathFilter;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.records.Record;
import org.datavec.api.records.listener.impl.LogRecordListener;
import org.datavec.api.split.CollectionInputSplit;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.datavec.image.transform.ImageTransform;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.MultipleEpochsIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.distribution.Distribution;
import org.deeplearning4j.nn.conf.distribution.GaussianDistribution;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.inputs.InvalidInputTypeException;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;


public class DeepLearningCNN {
    protected static final Logger log = LoggerFactory.getLogger(DeepLearningCNN.class);
    protected static long seed = 124;
    protected static Random rng = new Random(seed);
    
    protected int height;
    protected int width;
    protected int channels;
    protected int numExamples;
    protected int numLabels;
    protected int batchSize;
    protected int iterations;
    protected int epochs;
    protected double splitTrainTest = 0.7;

    public static class DeepLearningCNNBuilder{
        private int height;
        private int width;
        private int channels;
        private int numExamples;
        private int numLabels;
        private int iterations;
        private int epochs;
        private double splitTrainTest;
		private JProgressBarAndLabel buildProgressBar;

        public DeepLearningCNNBuilder(){
            height = Run.it.getPhenotypeCoreImageSemiWidth() * 2 + 1;
            width = height;
            numExamples = Run.it.numPhenTrainingPoints();
            numLabels =  Run.it.numPhenotypes();
            channels = 3; //Default RGB type 

        }
        
        public DeepLearningCNNBuilder iterations(int iterations){
            this.iterations = iterations;
            return this;
        }

        public DeepLearningCNNBuilder epochs(int epochs){
            this.epochs = epochs;
            return this;
        }

        public DeepLearningCNNBuilder splitPercentage(double splitTrainTest){
            this.splitTrainTest = splitTrainTest;
            return this;
        }

        public DeepLearningCNN build(){
            return new DeepLearningCNN(height,width,channels,numExamples,numLabels,iterations,epochs, splitTrainTest, buildProgressBar);
        }

		public DeepLearningCNNBuilder setProgressBar(JProgressBarAndLabel buildProgressBar) {
            this.buildProgressBar = buildProgressBar;
            return this;
		}


    }
    
    protected boolean save = false;
    
    protected int listenerFreq = 1;
    protected double buildProgress = 0; //progress for model training
    DataNormalization scaler;

    protected static String modelType = "custom"; // LeNet, AlexNet or Custom but you need to fill it out
    private MultiLayerNetwork network;
	private JProgressBarAndLabel buildProgressBar;

    public DeepLearningCNN(int height, int width, int channels, int numExamples, int numLabels, int iterations, int epochs, double splitTrainTest, JProgressBarAndLabel buildProgressBar){
    	this.channels = channels;
    	this.numExamples = numExamples;
    	this.numLabels = numLabels;
    	this.iterations = iterations;
    	this.epochs = epochs;
//    	this.splitTrainTest = splitTrainTest;
        this.height = height;
        this.width = width;
        this.buildProgressBar = buildProgressBar;
    }
    

    public void run() throws Exception {
        System.out.print(height + " " + width);
        
        //set CNN params from user here
        

        log.info("Load data....");
        /**cd
         * Data Setup -> organize and limit data file paths:
         *  - mainPath = path to image files
         *  - fileSplit = define basic dataset split with limits on format
         *  - pathFilter = define additional file load filter to limit size and balance batch content
         **/
        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator();
        //Class Labels path
        File mainPath = new File(Run.it.imageset.getFilenameWithHomePath("ClassLabels" +"_" + Run.it.getProjectName()));
        FileSplit fileSplit = new FileSplit(mainPath, NativeImageLoader.ALLOWED_FORMATS, rng);
        
        batchSize = numExamples;//Run.it.numPhenTrainingPoints(); //Run.it.CNN_
    	System.out.println("BATCH SIZE:" + batchSize);
        BalancedPathFilter pathFilter = new BalancedPathFilter(rng, labelMaker, numExamples, numLabels, batchSize);
        
//    	System.out.println("fileSplit:" + fileSplit.length());
        /**
         * Data Setup -> train test split
         *  - inputSplit = define train and test split
         **/
        //null pathFilter because we want to use all training examples users provided for training
        InputSplit[] inputSplit = fileSplit.sample(pathFilter, splitTrainTest, 1 - splitTrainTest);
        InputSplit trainData = inputSplit[0];
        InputSplit testData = inputSplit[1];

        

        /**
         * Data Setup -> transformation
         *  - Transform = how to tranform images and generate large dataset to train on
         **/
//        ImageTransform flipTransform1 = new FlipImageTransform(rng);
//        ImageTransform flipTransform2 = new FlipImageTransform(new Random(123));
//        ImageTransform warpTransform = new WarpImageTransform(rng, 42);
//	        ImageTransform colorTransform = new ColorConversionTransform(new Random(seed), COLOR_BGR2YCrCb);
//        List<ImageTransform> transforms = Arrays.asList(new ImageTransform[]{flipTransform1,
//                warpTransform, flipTransform2});
        List<ImageTransform> transforms = Arrays.asList();

        //Formula to get number of times we go through model during training
//        System.out.println("Epochs: " + epochs + "\n BatchSize: " + batchSize + "\n Iterations: " + iterations +
//        "\n Transforms: " +transforms.size() + "\n incrementor: " +progressIncrementor);
        /**
         * Data Setup -> normalization
         *  - how to normalize images and generate large dataset to train on
         **/
        scaler = new ImagePreProcessingScaler(0, 1);

        log.info("Build model....");

        // Uncomment below to try AlexNet. Note change height and width to at least 100
//	        MultiLayerNetwork network = new AlexNet(height, width, channels, numLabels, seed, iterations).init();

//        network:
        switch (modelType) {
            case "LeNet":
                network = lenetModel();
                break;
            case "AlexNet":
                network = alexnetModel();
                break;
            case "custom":
                network = customModel();
                break;
            default:
                throw new InvalidInputTypeException("Incorrect model provided.");
        }
        network.init();
        
        //setup display ability at http://localhost:9000/train 
    	UIServer uiServer = UIServer.getInstance();
        //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
        StatsStorage statsStorage = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later
        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        uiServer.attach(statsStorage);
        //Then add the StatsListener to collect this information from the network, as it trains
        network.setListeners(new StatsListener(statsStorage));
//        double increment = 100.0 / ((transforms.size() == 0 ? 1 : transforms.size()) * epochs * trainData.length() * iterations); //FUDGE *2!
        double increment = 100.0 / (epochs * iterations);
        network.setListeners(new CNNProgressListener(increment, buildProgressBar));
        /**
         * Data Setup -> define how to load data into net:
         *  - recordReader = the reader that loads and converts image data pass in inputSplit to initialize
         *  - dataIter = a generator that only loads one batch at a time into memory to save memory
         *  - trainIter = uses MultipleEpochsIterator to ensure model runs through the data for all epochs
         **/
        ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMaker);
        DataSetIterator dataIter;
        MultipleEpochsIterator trainIter;


        log.info("Train model....");
        // Train without transformations


        recordReader.initialize(trainData, null);
        recordReader.setListeners(new LogRecordListener());
        //System.out.println(recordReader.getCurrentFile());
        dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);


/** Image iterations
        for(int i=0; i<3; i++){

            DataSet testDataSet2 = dataIter.next();
            System.out.println(testDataSet2);
            System.out.println(dataIter.getLabels());

        }
*/

        scaler.fit(dataIter);
        dataIter.setPreProcessor(scaler);
        trainIter = new MultipleEpochsIterator(epochs, dataIter, Run.it.num_threads);
//        if(trainIter.hasNext()){
//            System.out.println("trainIter is not empty");
//        }
//        else System.out.println("trainIter is empty");

        network.fit(trainIter);

        // Train with transformations
        /**
        for (ImageTransform transform : transforms) {
            System.out.print("\nTraining on transformation: " + transform.getClass().toString() + "\n\n");
            recordReader.initialize(trainData, transform);
            dataIter = new RecordReaderDataSetIterator(recordReader, Run.it.CNN_batch_num, 1, Run.it.CNN_num_labels);
            scaler.fit(dataIter);
            dataIter.setPreProcessor(scaler);
            trainIter = new MultipleEpochsIterator(epochs, dataIter, Run.it.CNN_nCores);
            network.fit(trainIter);
            buildProgress += progressIncrementor;

        }
        */

        log.info("Evaluate model....");
        recordReader.initialize(testData);
        dataIter = new RecordReaderDataSetIterator(recordReader, batchSize, 1, numLabels);
        scaler.fit(dataIter);
      
        
        dataIter.setPreProcessor(scaler);
        Evaluation eval = network.evaluate(dataIter);
        System.out.println(eval.stats(true));

        
        //now look at all the test data and what happens
        dataIter.reset();
        Iterator<URI> locations = testData.locationsIterator();
        int i = 0;
        while (dataIter.hasNext()){
        	
            DataSet dataset = dataIter.next();
            Iterator<DataSet> iter = dataset.asList().iterator();
            
			while (iter.hasNext()){
				i++;
				DataSet t = iter.next();
				URI uri = locations.next();
				
				System.out.println("uri: " + uri.toString());
            
	            System.out.println("predicting #" + i + " id: " + t.id());
	            if (t.getExampleMetaData() != null){
	                for (Serializable s : t.getExampleMetaData()){
	                	System.out.println("  metadata: " + s.toString());
	                }
	            }
	
	            System.out.println("  hasMaskArrays: " + t.hasMaskArrays());
	            System.out.println("  numExamples: " + t.numExamples());
//	            System.out.println("  getFeatureMatrix cols: " + t.getFeatureMatrix());
//	            if (i == 1){
		            System.out.println("  getFeatures: " + t.getFeatures());	            	
//	            }
	            for (String lab : t.getLabelNamesList()){
	            	System.out.println("  label name: " + lab);
	            }
	            
	            System.out.println("  labels: " + t.getLabels());
//	            System.out.println("  toString: " + t.toString());
	            
	            
	            
//	            String expectedResult = t.getLabelName(0);
	            int[] predict = network.predict(t.getFeatures());
	            for (int prediction :  predict){
	            	System.out.println("  prediction: " + prediction);
	            }
			}
        }


        // Example on how to get predict results with trained model
        



        if (save) {
            log.info("Save model....");
            String basePath = FilenameUtils.concat(System.getProperty("user.dir"), "src/main/resources/");
            ModelSerializer.writeModel(network, basePath, true);
        }
        log.info("****************Example finished********************");


    }

    /**
     *Feeds Buffered Image to model
     * @param image bufferedImage at point of interest
     * @param filename 
     * @param j 
     * @param i 
     * @return classlabel
     * @throws IOException 
     */
    public double classify(BufferedImage image, int i, int j, String filename) throws IOException{
    	int prediction_vector[] = null;
		//    	System.out.println("classify for image " + image + " i " + i + " j " + j + " filename " + filename);
//        File outputimagefile = new File(
//        		Run.it.imageset.getFilenameWithHomePath("ClassLabels" +"_" + Run.it.getProjectName()) + 
//        		File.separator +
//        		"TEST" + 
//        		File.separator +
//               "subimage_"+i+"_"+j+
//               ".bmp");
    	File outputimagefile = new File("e:/test" + Thread.currentThread().getName() + ".bmp");
    	synchronized (this){
    		ImageIO.write(image, "BMP", outputimagefile);
    	}
//    	synchronized (this){
    	       INDArray d = new NativeImageLoader(image.getHeight(), image.getWidth(), 3).asMatrix(image);
    	    	scaler.transform(d);
    	    	System.out.println("features of original bufferedimage:\n" + d);
//
//    	    	System.out.println("type of original bufferedimage:\n" + image.getType());
    	    	
//    	    	BufferedImage clone = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
//    	    	for (int i0 = 0; i0 < clone.getWidth(); i0++){
//    	        	for (int j0 = 0; j0 < clone.getWidth(); j0++){
//    	        		clone.setRGB(i0, j0, image.getRGB(i0, j0));
//    	        	}
//    	    	}
//    	    	System.out.println("type of clone:\n" + clone);
//    	        d = new NativeImageLoader(clone.getHeight(), clone.getWidth(), 3).asMatrix(clone);
//    	    	scaler.transform(d);
    	    	
    	    	
//				BufferedImage image_read = ImageIO.read(outputimagefile);
				
//				System.out.println("original:\n" + image);
//				System.out.println("clone:\n" + clone);
//				System.out.println("image read:\n" + image_read);
//				System.out.println("image read type:\n" + image_read.getType());
    	    	
//    			d = new NativeImageLoader(image.getHeight(), image.getWidth(), 3).asMatrix(outputimagefile); 
//     	    	scaler.transform(d);
//     	    	System.out.println("features of written and loaded image:\n" + d);
//    	    	
    	        prediction_vector = network.predict(d);
//    	}
 
//        for (int prediction :  prediction_vector){
////        	if (prediction == 1){
//
//            	System.out.println("  prediction: " + prediction);
////        	}
//
//        }
        return (double)prediction_vector[0];
    }
    



    private ConvolutionLayer convInit(String name, int in, int out, int[] kernel, int[] stride, int[] pad, double bias) {
        return new ConvolutionLayer.Builder(kernel, stride, pad).name(name).nIn(in).nOut(out).biasInit(bias).build();
    }

    private ConvolutionLayer conv3x3(String name, int out, double bias) {
        return new ConvolutionLayer.Builder(new int[]{3,3}, new int[] {1,1}, new int[] {1,1}).name(name).nOut(out).biasInit(bias).build();
    }

    private ConvolutionLayer conv5x5(String name, int out, int[] stride, int[] pad, double bias) {
        return new ConvolutionLayer.Builder(new int[]{5,5}, stride, pad).name(name).nOut(out).biasInit(bias).build();
    }

    private SubsamplingLayer maxPool(String name,  int[] kernel) {
        return new SubsamplingLayer.Builder(kernel, new int[]{2,2}).name(name).build();
    }

    private DenseLayer fullyConnected(String name, int out, double bias, double dropOut, Distribution dist) {
        return new DenseLayer.Builder().name(name).nOut(out).biasInit(bias).dropOut(dropOut).dist(dist).build();
    }

    public MultiLayerNetwork lenetModel() {
        /**
         * Revisde Lenet Model approach developed by ramgo2 achieves slightly above random
         * Reference: https://gist.github.com/ramgo2/833f12e92359a2da9e5c2fb6333351c5
         **/
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(Run.it.CNN_iter_num)
                .regularization(false).l2(0.005) // tried 0.0001, 0.0005
                .activation(Activation.RELU)
                .learningRate(0.0001) // tried 0.00001, 0.00005, 0.000001
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(Updater.RMSPROP).momentum(0.9)
                .list()
                .layer(0, convInit("cnn1", channels, 50 ,  new int[]{5, 5}, new int[]{1, 1}, new int[]{0, 0}, 0))
                .layer(1, maxPool("maxpool1", new int[]{2,2}))
                .layer(2, conv5x5("cnn2", 100, new int[]{5, 5}, new int[]{1, 1}, 0))
                .layer(3, maxPool("maxool2", new int[]{2,2}))
                .layer(4, new DenseLayer.Builder().nOut(500).build())
                .layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(Run.it.CNN_num_labels)
                        .activation(Activation.SOFTMAX)
                        .build())
                .backprop(true).pretrain(false)
                .setInputType(InputType.convolutional(height,width, channels))
                .build();

        return new MultiLayerNetwork(conf);

    }

    public MultiLayerNetwork alexnetModel() {
        /**
         * AlexNet model interpretation based on the original paper ImageNet Classification with Deep Convolutional Neural Networks
         * and the imagenetExample code referenced.
         * http://papers.nips.cc/paper/4824-imagenet-classification-with-deep-convolutional-neural-networks.pdf
         **/

        double nonZeroBias = 1;
        double dropOut = 0.5;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.DISTRIBUTION)
                .dist(new NormalDistribution(0.0, 0.01))
                .activation(Activation.RELU)
                .updater(Updater.NESTEROVS)
                .iterations(iterations)
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer) // normalize to prevent vanishing or exploding gradients
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(1e-2)
                .biasLearningRate(1e-2*2)
                .learningRateDecayPolicy(LearningRatePolicy.Step)
                .lrPolicyDecayRate(0.1)
                .lrPolicySteps(100000)
                .regularization(true)
                .l2(5 * 1e-4)
                .momentum(0.9)
                .miniBatch(false)
                .list()
                .layer(0, convInit("cnn1", channels, 96, new int[]{11, 11}, new int[]{4, 4}, new int[]{3, 3}, 0))
                .layer(1, new LocalResponseNormalization.Builder().name("lrn1").build())
                .layer(2, maxPool("maxpool1", new int[]{3,3}))
                .layer(3, conv5x5("cnn2", 256, new int[] {1,1}, new int[] {2,2}, nonZeroBias))
                .layer(4, new LocalResponseNormalization.Builder().name("lrn2").build())
                .layer(5, maxPool("maxpool2", new int[]{3,3}))
                .layer(6,conv3x3("cnn3", 384, 0))
                .layer(7,conv3x3("cnn4", 384, nonZeroBias))
                .layer(8,conv3x3("cnn5", 256, nonZeroBias))
                .layer(9, maxPool("maxpool3", new int[]{3,3}))
                .layer(10, fullyConnected("ffn1", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
                .layer(11, fullyConnected("ffn2", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
                .layer(12, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .name("output")
                        .nOut(numLabels)
                        .activation(Activation.SOFTMAX)
                        .build())
                .backprop(true)
                .pretrain(false)
                .setInputType(InputType.convolutional(height, width, channels))
                .build();

        return new MultiLayerNetwork(conf);

    }

    public MultiLayerNetwork customModel() {
        /**
         * AlexNet model interpretation based on the original paper ImageNet Classification with Deep Convolutional Neural Networks
         * and the imagenetExample code referenced.
         * http://papers.nips.cc/paper/4824-imagenet-classification-with-deep-convolutional-neural-networks.pdf
         **/

        double nonZeroBias = 1;
        double dropOut = 0.5;

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .weightInit(WeightInit.DISTRIBUTION)
                .dist(new NormalDistribution(0.0, 0.01))
                .activation(Activation.RELU)
                .updater(Updater.NESTEROVS)
                .iterations(iterations)
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer) // normalize to prevent vanishing or exploding gradients
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(1e-2)
                .biasLearningRate(1e-2*2)
                .learningRateDecayPolicy(LearningRatePolicy.Step)
                .lrPolicyDecayRate(0.1)
                .lrPolicySteps(100000)
                .regularization(true)
                .l2(5 * 1e-4)
                .momentum(0.9)
                .miniBatch(false)
                .list()
                .layer(0, convInit("cnn1", channels, 96, new int[]{11, 11}, new int[]{4, 4}, new int[]{3, 3}, 0))
                .layer(1, new LocalResponseNormalization.Builder().name("lrn1").build())
                .layer(2, maxPool("maxpool1", new int[]{3,3}))
                .layer(3, conv5x5("cnn2", 256, new int[] {1,1}, new int[] {2,2}, nonZeroBias))
                .layer(4, new LocalResponseNormalization.Builder().name("lrn2").build())
                .layer(5, maxPool("maxpool2", new int[]{2,2}))
                .layer(6,conv3x3("cnn3", 384, 0))
                .layer(7,conv3x3("cnn4", 384, nonZeroBias))
                .layer(8,conv3x3("cnn5", 256, nonZeroBias))
                .layer(9, maxPool("maxpool3", new int[]{1,1}))
                .layer(10, fullyConnected("ffn1", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
                .layer(11, fullyConnected("ffn2", 4096, nonZeroBias, dropOut, new GaussianDistribution(0, 0.005)))
                .layer(12, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .name("output")
                        .nOut(numLabels)
                        .activation(Activation.SOFTMAX)
                        .build())
                .backprop(true)
                .pretrain(false)
                .setInputType(InputType.convolutional(height, width, channels))
                .build();

        return new MultiLayerNetwork(conf);
    }

    public double getBuildProgress(){
        return buildProgress;
    }

}