/*
    GemIdent v1.1b
    Interactive Image Segmentation Software via Supervised Statistical Learning
    http://gemident.com
    
    Copyright (C) 2009 Professor Susan Holmes & Adam Kapelner, Stanford University

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details:
    
    http://www.gnu.org/licenses/gpl-2.0.txt

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package GemIdentView;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentModel.Phenotype;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;

/**
 * A type of {@link KClassInfo class info} that's specially designed
 * to represent a {@link GemIdentModel.Phenotype phenotype}
 * 
 * @author Adam Kapelner
 *
 */
@SuppressWarnings("serial")
public class KPhenotypeInfo extends KClassInfo {

	/** allows the user to select a maximum radius for this phenotype */
	private JSpinner rmaxSpinner;
	/** allows the user to specify if <b>GemIdent</b> should look for centroids of this phenotype in {@link GemIdentCentroidFinding.PostProcess post-processing} */
	private JCheckBox findCentroids;
	/** allows the user to specify if <b>GemIdent</b> should classify this phenotype at all during {@link GemIdentClassificationEngine.Classify classification} */
	private JCheckBox findPixels;
	
	/**
	 * Creates a new class info representing a phenotype. The "NON" phenotype is set up differently.
	 * It's name field is uneditable and unfocusable.
	 * 
	 * @param imageTrainPanel		the linked image Panel
	 * @param name					the name of the underlying phenotype
	 * @param owner					simplifies selection of this class info
	 */
	public KPhenotypeInfo(KImageTrainPanel imageTrainPanel,String name,SelectionEmulator owner ) {
		super(imageTrainPanel,owner);

		rmaxSpinner = new JSpinner(new SpinnerNumberModel(8,0,Solids.INIT_MAX_SOLID,1));
		findPixels=new JCheckBox(); 
		findPixels.setSelected(true);
		findCentroids=new JCheckBox(); 
		findCentroids.setSelected(true);
		
		trainer = new Phenotype();
		trainer.setName(name);
		nameField.setText(name);
		if (Phenotype.isNONNAME(name)){
			nameField.setEditable(false);
			nameField.setFocusable(false);
		}
		
		EditWestBox();
		SetUpListeners();
	}
	/**
	 * Creates a new class info representing the given {@link GemIdentModel.Phenotype phenotype}. 
	 * The display components are set up according to the preferences of the given phenotype.
	 * 
	 * @param imageTrainPanel		the linked image Panel
	 * @param trainer				the {@link GemIdentModel.Phenotype phenotype} model to build the info according to
	 * @param owner					simplifies selection of this class info
	 */
	public KPhenotypeInfo(KImageTrainPanel imageTrainPanel,Phenotype trainer,SelectionEmulator owner ){
		super(imageTrainPanel,owner);
		this.trainer=trainer;
		if (trainer.isNON()){
			nameField.setEditable(false);
			nameField.setFocusable(false);
		}
		rmaxSpinner = new JSpinner(new SpinnerNumberModel(8,0,100,1));
		findPixels=new JCheckBox(); 
		findCentroids=new JCheckBox(); 
		
		nameField.setText(trainer.getName());
		rminSpinner.setValue(trainer.getRmin());
		rmaxSpinner.setValue(trainer.getRmax());
		if (trainer.isFindPixels())
			findPixels.setSelected(true);
		else
			findPixels.setSelected(false);
		if (trainer.isFindCentroids())
			findCentroids.setSelected(true);
		else
			findCentroids.setSelected(false);

		colorChooseButton.repaint();

		EditWestBox();
		SetUpListeners();
		UnSelect();
	}
	
	
	/** adds the specialized phenotype components to the Western region of the class info */
	protected void EditWestBox() {
		if (!((Phenotype)trainer).isNON()){
			JFormattedTextField textfield = ((JSpinner.DefaultEditor)this.rmaxSpinner.getEditor()).getTextField();
			textfield.setEditable(true);
			textfield.setFocusable(true);
			
			JLabel rmaxLabel = new JLabel("rMax:",JLabel.RIGHT);
			
			JLabel findLabel = new JLabel("Pix/Cent:",JLabel.RIGHT);

			Box check=Box.createHorizontalBox();
			check.add(findPixels);
			check.add(findCentroids);
			
			super.westBox.add(rmaxLabel);
			super.westBox.add(rmaxSpinner);
			super.westBox.add(findLabel);
			super.westBox.add(check);
		}
	}
	/** sets up the listeners for the components that change the underlying model */
	protected void SetUpListeners() {
		nameField.addKeyListener(
			new KeyListener(){
				public void keyPressed(KeyEvent e){}
				public void keyReleased(KeyEvent e){
					String previous=trainer.getName();
					String present=nameField.getText();
					present = present.replaceAll(" ", "_");
					present = present.replaceAll("-", "_");
					
					int old_pos = nameField.getCaretPosition();
					if (!nameField.getText().equals(present)){
						nameField.setText(present);
						nameField.setCaretPosition(old_pos);
					}
					if (!previous.equals(present)){
						Run.it.DeletePhenotype(previous);
						trainer.setName(present);
						Run.it.AddPhenotype((Phenotype)trainer);
					}
					Run.it.GUIsetDirty(true);
				}
				public void keyTyped(KeyEvent e){}
			}
		);
		rminSpinner.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					trainer.setRmin((Integer)rminSpinner.getValue());
					Run.it.GUIsetDirty(true);
				}
			}
		); 
		rmaxSpinner.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					Select();					
					trainer.setRmax((Integer)rmaxSpinner.getValue());	
					ImageAndScoresBank.FlushAllSuperImages();
					imageTrainPanel.setRedrawImage(true);
					imageTrainPanel.repaint();
					Run.it.GUIsetDirty(true);
					buildExampleImageFromImageSnippets();
				}
			}
		);
		JFormattedTextField textfield = ((JSpinner.DefaultEditor)this.rminSpinner.getEditor()).getTextField();
		textfield.addMouseListener(
			new MouseAdapter(){
				public void mouseClicked( MouseEvent e ) {
					Select();
				}
			}
		);
		findCentroids.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
				    if (e.getStateChange() == ItemEvent.DESELECTED)
				    	((Phenotype)trainer).setFindCentroids(false);
				    else 
				    	((Phenotype)trainer).setFindCentroids(true);
				    Run.it.GUIsetDirty(true);
				}
			}
		);
		findPixels.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
				    if (e.getStateChange() == ItemEvent.DESELECTED){
				    	((Phenotype)trainer).setFindPixels(false);
				    	((Phenotype)trainer).setFindCentroids(false);
				    	findCentroids.setSelected(false);
				    	findCentroids.setEnabled(false);
				    }
				    else {
				    	((Phenotype)trainer).setFindPixels(true);
				    	findCentroids.setEnabled(true);
				    }
				    Run.it.GUIsetDirty(true);
				}
			}
		);
	}
	/** a dumb struct to hold information about one coordinate in one of the image files */
	private class ImageAndPoint {
		public String filename;
		public Point point;
		public ImageAndPoint(String filename,Point point){
			this.filename=filename;
			this.point=point;
		}
	}
	
	/** builds a preview image from random training points (portions should probably be decomped eventually) */
	private void buildExampleImageFromImageSnippets(){
		BufferedImage image=new BufferedImage(identifierButton.getWidth(),identifierButton.getHeight(),BufferedImage.TYPE_INT_RGB);

		//take measurements
		int nrow=(int)Math.round(identifierButton.getWidth()/((double)(trainer.getRmax()*2+1)));
		int ncol=(int)Math.round(identifierButton.getHeight()/((double)(trainer.getRmax()*2+1)));
		int area=nrow*ncol;
		int sub_width=(int)Math.round(image.getWidth()/((double)nrow));
		int sub_height=(int)Math.round(image.getHeight()/((double)ncol));
		int xdiff=image.getWidth()-sub_width*nrow;
		int ydiff=image.getHeight()-sub_height*ncol;
		
		//find points that are okay ie within bounds
		ArrayList<ImageAndPoint> allPhenotypePoints=new ArrayList<ImageAndPoint>();		
		for (String filename:Run.it.getPhenotypeTrainingImages()){
			DataImage trainingImage=ImageAndScoresBank.getOrAddDataImage(filename);
			ArrayList<Point> pointsInImage=trainer.getPointsInImage(filename);
			if (pointsInImage != null)
				for (Point t:pointsInImage)
					if ((t.x-sub_width/2-1 > 0) && (t.x+sub_width/2+1 < trainingImage.getWidth()) && (t.y-sub_height/2-1 > 0) && (t.y+sub_height/2+1 < trainingImage.getHeight()))
						allPhenotypePoints.add(new ImageAndPoint(filename,t));
		}		
		int length = allPhenotypePoints.size();
		
		//if we don't have any image snippets return a blank image, otherwise get each snippet
		if (length == 0){
			super.setIdentifierImage(null);
			this.identifierButton.repaint();
		}
		else {
			int[] indices=new int[area];
			for (int i=0;i<area;i++)
				indices[i]=(int)Math.floor(Math.random()*length);
	
			for (int i=0;i<nrow;i++){
				for (int j=0;j<ncol;j++){
					ImageAndPoint ip=allPhenotypePoints.get(indices[i*ncol+j]);
					BufferedImage temp=(ImageAndScoresBank.getOrAddDataImage(ip.filename)).getAsBufferedImage();
					for (int x=0;x<sub_width;x++){
						for (int y=0;y<sub_height;y++){
							try {
								image.setRGB(xdiff/2+i*sub_width+x,ydiff/2+j*sub_height+y,temp.getRGB(ip.point.x-sub_width/2+x,ip.point.y-sub_height/2+y));
							} catch (Exception e){}
						}
					}
				}
			}	
			//draw gridlines to separate the snippets
			if (trainer.getRmax() > 0){
				for (int i=0;i<nrow;i++)
					drawVerticalLine(image,sub_width*i);
				for (int i=0;i<ncol;i++)
					drawHorizontalLine(image,sub_height*i);
			}
			super.setIdentifierImage(image);
			this.identifierButton.repaint();
		}
	}	
	/**
	 * Draws a vertical black line on an image.
	 * @param image		the image
	 * @param x			the x-value to draw the line
	 */
	private void drawVerticalLine( BufferedImage image, int x ) {
		for ( int y = 0; y < image.getHeight(); ++y ) {
			image.setRGB(x,y,0);
		}
	}	
	/**
	 * Draws a horizontal black line on an image.
	 * @param image		the image
	 * @param y			the y-value to draw the line
	 */
	private void drawHorizontalLine( BufferedImage image, int y ) {
		for ( int x = 0; x < image.getWidth(); ++x ) {
			image.setRGB(x,y,0);
		}
	}	
	/** the generic function to build the example image for this phenotype info.
	 * If it's the "NON" phenotype, {@link KClassInfo#buildExampleImageFromColors() 
	 * build it from colors}, otherwise {@link #buildExampleImageFromImageSnippets()
	 * build it from image snippets}
	 */
	public void buildExampleImage(){
		if (((Phenotype)trainer).isNON())
			buildExampleImageFromColors();
		else
			buildExampleImageFromImageSnippets();
	}
}