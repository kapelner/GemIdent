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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import GemIdentImageSets.DataImage;
import GemIdentModel.Phenotype;
import GemIdentModel.TrainingImageData;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;

/**
 * Controls the displaying of images during {@link KPhenotypeTrainPanel phenotype training}
 * 
 * @author Adam Kapelner
 *
 */
@SuppressWarnings("serial")
public class KImagePhenotypeTrainPanel extends KImageTrainPanel {

	/** default constructor */
	public KImagePhenotypeTrainPanel(KPhenotypeTrainPanel trainPanel) {
		super(trainPanel);
	}
	/** draws the current training image with an overlay of the user's training points and the alpha masks */
	protected void ReDrawOverImageFromScratch() {
		trainPointsOverImage=new BufferedImage(displayImage.getWidth(),displayImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
		if (alphaLevelForTrainPoints > ALPHA_VISIBILITY_THRESHOLD){ //draw shadows only if it's noticeable:
			Color back=new Color(0,0,0,alphaLevelForTrainPoints);
			for (Phenotype phenotype:Run.it.getPhenotypeObjects()){
				TrainingImageData trainingImagedata=phenotype.getTrainingImage(displayImage.getFilename());
				if (trainingImagedata != null){ //ie it has it
					ArrayList<Point> points=trainingImagedata.getPoints();						
					for (Point to:points)
						for (Point t:Solids.GetPointsInSolidUsingCenter(phenotype.getRmax(), to))
							try {trainPointsOverImage.setRGB(t.x,t.y,back.getRGB());} catch (Exception e){}
				}
			}
		}
		for (Phenotype phenotype:Run.it.getPhenotypeObjects()){
			TrainingImageData trainingImagedata=phenotype.getTrainingImage(displayImage.getFilename());
			if (trainingImagedata != null){ //ie it has it
				ArrayList<Point> points=trainingImagedata.getPoints();
				Color display=phenotype.getDisplayColor();
				for (Point to:points)
					for (Point t:Solids.GetPointsInSolidUsingCenter(phenotype.getRmin(), to))
						try {trainPointsOverImage.setRGB(t.x,t.y,display.getRGB());} catch (Exception e){}
			}
		}

	}

	public void paintComponent(Graphics g){
		super.paintComponent(g);
	}
	/**
	 * When the user selects a new training point, this function updates the
	 * image overlay mask and displays that new point. The addition of new 
	 * points to the phenotypes training set is discussed in section 3.2.4 of
	 * the manual.
	 * 
	 * @param filename		the filename of the image the user is training
	 * @param to			the new coordinate
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public void NewPoint(String filename,Point to){
		Phenotype phenotype=(Phenotype)trainPanel.getActiveTrainer();
		abstractPaintNewPoint(filename, to, phenotype);
	}
	/**
	 * User just wanted to add a non point
	 */	
	public void NewNonPoint(String filename, Point to){
		abstractPaintNewPoint(filename, to, Run.it.getNONPhenotype());
	}	
	
	/**
	 * A general function used to paint new training points atop the points mask
	 * 
	 * @param filename		the image filename currently being painted upon
	 * @param to			the point where the user clicked
	 * @param phenotype		the phenotype the user is training
	 */
	private void abstractPaintNewPoint(String filename, Point to, Phenotype phenotype){
		if (phenotype == null){ 
			return;
		}
		if (alphaLevelForTrainPoints > 5){ //draw shadows only if it's noticeable:
			Color back=new Color(0,0,0,alphaLevelForTrainPoints);
			for (Point t:Solids.GetPointsInSolidUsingCenter(phenotype.getRmax(), to)){
				if (t.x >= 0 && t.y >= 0 && t.x < displayImage.getWidth() && t.y < displayImage.getHeight())
					if ((new Color(trainPointsOverImage.getRGB(t.x,t.y),true)).equals(new Color(0,0,0,0)))
						trainPointsOverImage.setRGB(t.x,t.y,back.getRGB());
			}
		}			
		for (Point t:Solids.GetPointsInSolidUsingCenter(phenotype.getRmin(), to))
			try {trainPointsOverImage.setRGB(t.x,t.y,phenotype.getDisplayRGB());} catch (Exception e){}
			
	}

	/** the basic setter with checks for null images, and if the image has been classified */
	protected void setDisplayImage(DataImage displayImage,boolean classified){
		super.setDisplayImage(displayImage);
		if (displayImage == null)
			((KPhenotypeTrainPanel)trainPanel).RemoveSliders();
		if (trainPanel instanceof KPhenotypeTrainPanel){
			if (classified)
				((KPhenotypeTrainPanel)trainPanel).AddOrEditSliders();
			else
				((KPhenotypeTrainPanel)trainPanel).RemoveSliders();
		}
	}
}