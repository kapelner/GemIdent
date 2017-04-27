package GemIdentClassificationEngine.Features;

import GemIdentClassificationEngine.Datum;
import GemIdentClassificationEngine.DatumSetupForImage;
import GemIdentClassificationEngine.TrainingData;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;
import GemIdentModel.Phenotype;
import GemIdentOperations.Run;
import GemIdentOperations.SetupClassification;
import GemIdentTools.IOTools;
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
            //if parent label diretory doesn't exist
            if(!IOTools.DoesDirectoryExist(System.getProperty("user.dir")+"LabelsForAllProjects"))
                new File(System.getProperty("user.dir"),"LabelsForAllProjects").mkdir();
            projectClassLabelDir = System.getProperty("user.dir")+File.separator+"LabelsForAllProjects"+File.separator+
                    "ClassLabels"+ Run.it.getProjectName()+File.separator;
            new File(System.getProperty("user.dir"),"LabelsForAllProjects"+File.separator+"ClassLabels"+
                    Run.it.getProjectName()).mkdir();

            //create File Directories for every PhenoType
            for (String phenotypeName : Run.it.getPhenotyeNames()) {
                (new File(projectClassLabelDir,phenotypeName)).mkdir();
            }
        }

    public void addImagestoProperDirectories(DatumSetupForImage datumSetupForImage, Point t, Phenotype phenotype){
        SuperImage super_image = ImageAndScoresBank.getOrAddSuperImage(datumSetupForImage.filename());
        String phenoName = phenotype.getName();
        //possibly have a set of buffered images, instead of allocating an image every time
        BufferedImage whole_image = super_image.getAsBufferedImage();
        //coodinates for point of interest translated to super image
        Point t_adj = super_image.AdjustPointForSuper(t);
        int r_max = phenotype.getRmax();
        int distanceFromCornerToMid = Math.round((int)(Math.sqrt(2)  * r_max)); //round will give us extra information
        BufferedImage subImage = whole_image.getSubimage(t_adj.x - distanceFromCornerToMid,
                t_adj.y - distanceFromCornerToMid, r_max *2, r_max*2);
        System.out.println(phenoName);
        File outputimage = new File(projectClassLabelDir+phenoName+File.separator+
                IOTools.GetFilenameWithoutExtension(datumSetupForImage.filename())+"_"+t.x+"_"+t.y+".jpg");
        /** Insert file into Directory*/
        try {
            ImageIO.write(subImage, "JPEG", outputimage);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            for (Phenotype phenotype: Run.it.getPhenotypeObjects()){
                if (phenotype.hasImage(filename)){
                    for (Point to:phenotype.getPointsInImage(filename)){
                        if (stop)
                            return;
                        String name=phenotype.getName();
                        int Class = 0;
                        if (phenotype.isFindPixels()){
                            Class=Run.classMapFwd.get(name);
                        }
                        Datum d = null;
                        addImagestoProperDirectories(datumSetupForImage, to,phenotype);
                        //d.setClass(Class);
                        //allData.add(d);
                        //update progress bar
                        trainingProgress.setValue((int)Math.round(totalvalue += increment));
                    }
                }
            }
        }
    }
}


