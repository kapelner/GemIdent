package GemIdentClassificationEngine.DeepLearning;

import GemIdentClassificationEngine.Datum;
import GemIdentClassificationEngine.DatumSetupForImage;
import GemIdentClassificationEngine.TrainingData;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;
import GemIdentModel.Phenotype;
import GemIdentOperations.Run;
import GemIdentOperations.SetupClassification;
import GemIdentTools.IOTools;
import GemIdentTools.Geometry.Solids;
import GemIdentView.JProgressBarAndLabel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class DeepLearningTrainingData extends TrainingData{
    private String projectClassLabelDir;

    public DeepLearningTrainingData(int nthreads, JProgressBarAndLabel trainingProgress){
        super(nthreads,trainingProgress);
    }


    protected void GenerateData() {
        createPhenotypeDirectories();

        trainPool= Executors.newFixedThreadPool(nthreads);

        for (String filename:Run.it.getPhenotypeTrainingImages()){

            trainPool.execute(new TrainingDataMaker(filename));
        }
        trainPool.shutdown();
        try {
            trainPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
        } catch (InterruptedException ignored){}
    }

    //possibly move this functionality to IOTools
        public void createPhenotypeDirectories() {
            //Create ClassLabel for project
            projectClassLabelDir = Run.it.imageset.getFilenameWithHomePath("ClassLabels" +"_" + Run.it.getProjectName());
            new File(projectClassLabelDir).mkdir();

            for (String phenotypeName : Run.it.getPhenotyeNames()) {
                new File(projectClassLabelDir,phenotypeName).mkdir();
            }
        }

    private void addImagetoProperDirectories(DatumSetupForImage datumSetupForImage, Point t, Phenotype phenotype){
        BufferedImage superImageCore = coreOutSuperImage(
        		ImageAndScoresBank.getOrAddSuperImage(datumSetupForImage.filename()), 
        		Run.it.getPhenotypeCoreImageSemiWidth(), 
        		t
        );
        File outputimagefile = new File(projectClassLabelDir+File.separator+phenotype.getName()+File.separator+
                IOTools.GetFilenameWithoutExtension(datumSetupForImage.filename())+"_"+t.x+"_"+t.y+".bmp");
        /** Insert file into Directory*/
        try {
            ImageIO.write(superImageCore, "BMP", outputimagefile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static BufferedImage coreOutSuperImage(SuperImage super_image, int semiwidth, Point t) {
    	Point t_adj = super_image.AdjustPointForSuper(t);
//    	System.out.println("tadj: " + t_adj + " semi: " + semiwidth);
		return super_image.getAsBufferedImage().getSubimage(t_adj.x - semiwidth, t_adj.y - semiwidth, semiwidth * 2 + 1, semiwidth * 2 + 1);
	}

	/** Framework for building training data in one image and can be threaded in a thread pool */
    private class TrainingDataMaker implements Runnable{

        /** the filename of the image whose training data is being created */
        private String filename;
        /** the object that contains common information for each Datum */
        private DatumSetupForImage datumSetupForImage;

        /** default constructor */
        public TrainingDataMaker(String filename){
            this.filename = filename;
            datumSetupForImage = new DatumSetupForImage(SetupClassification.initDatumSetupForEntireRun(), filename);
        }
        /**
         * For all phenotypes, if the user has trained in this image, then
         * for all training points, create a {@link Datum Datum}
         * for each point within {@link GemIdentModel.TrainSuperclass#rmin rmin}
         * of the user's point and set its class value equal to the phenotype. If
         * the user decides not to look for this phenotype (by clicking the
         * {@link GemIdentView.KPhenotypeInfo#findPixels findPixels} checkbox),
         * then set the Datum's class value to zero, that of the {@link
         * GemIdentModel.Phenotype#NON_NAME NON} phenotype. For a more formal
         * description, see step 5c in the Algorithm section of the IEEE paper.
         *
         * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
         */
        public void run(){
        	//calculate increment first
        	for (Phenotype phenotype: Run.it.getPhenotypeObjects()){
        		increment += phenotype.getTotalPoints() * Solids.getSolid(phenotype.getRmin()).size();
        	}
        	increment = 100.0 / increment;
        	
            for (Phenotype phenotype: Run.it.getPhenotypeObjects()){
                if (phenotype.hasImage(filename)){
                    for (Point to : phenotype.getPointsInImage(filename)){
                        if (stop)
                            return;
	                        for (Point t : Solids.GetPointsInSolidUsingCenter(phenotype.getRmin(), to)){
	                            addImagetoProperDirectories(datumSetupForImage, t, phenotype);
	                            //update the bar
	                            totalvalue += increment;
	                            trainingProgress.setValue((int)Math.round(totalvalue));                        
	                        }
                    }
                }
            }
        }
    }
}


