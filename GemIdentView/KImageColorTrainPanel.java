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
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import GemIdentModel.Stain;
import GemIdentModel.TrainingImageData;
import GemIdentOperations.Run;
import GemIdentTools.Geometry.Solids;

/**
 * Controls the displaying of images during {@link KColorTrainPanel color training}
 * 
 * @author Adam Kapelner
 *
 */
@SuppressWarnings("serial")
public class KImageColorTrainPanel extends KImageTrainPanel {

	/** default constructor */
	public KImageColorTrainPanel(KColorTrainPanel trainPanel) {
		super(trainPanel);
	}
	/** Creates the mask that displays the training points and the alpha shadows from scratch */
	protected void ReDrawOverImageFromScratch() {		
		trainPointsOverImage=new BufferedImage(displayImage.getWidth(),displayImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
		//draw alpha shadows first
		if (alphaLevelForTrainPoints > ALPHA_VISIBILITY_THRESHOLD){ //draw shadows only if it's noticeable:
			Color back=new Color(0,0,0,alphaLevelForTrainPoints);
			for (Stain stain:Run.it.getUserColorsImageset().getStainObjects()){
				TrainingImageData trainingImagedata=stain.getTrainingImage(displayImage.getFilename());
				if (trainingImagedata != null){ //ie it has it
					ArrayList<Point> points=trainingImagedata.getPoints();
					for (Point to:points)
						for (Point t:Solids.GetPointsInSolidUsingCenter(14,to)){
							try {trainPointsOverImage.setRGB(t.x,t.y,back.getRGB());} catch (Exception e){}
						}
				}
			}
		}
		//then draw actual dots on top of the shadows:
		for (Stain stain:Run.it.getUserColorsImageset().getStainObjects()){
			TrainingImageData trainingImagedata=stain.getTrainingImage(displayImage.getFilename());
			if (trainingImagedata != null){ //ie it has it
				ArrayList<Point> points=trainingImagedata.getPoints();
				Color display=stain.getDisplayColor();
				for (Point to:points)
					for (Point t:Solids.GetPointsInSolidUsingCenter(stain.getRmin(),to))
						try {trainPointsOverImage.setRGB(t.x,t.y,display.getRGB());} catch (Exception e){}
			}
		}		
	}
	/**
	 * When the user selects a new training point, this function updates the
	 * image overlay mask and displays that new point. The addition of new 
	 * points to the colors training set is discussed in section 3.1.3 of
	 * the manual.
	 * 
	 * @param filename		the filename of the image the user is training
	 * @param to			the new coordinate
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public void NewPoint(String filename,Point to){		
		Stain stain=(Stain)trainPanel.getActiveTrainer();
		if (stain == null) return;
		if (alphaLevelForTrainPoints > ALPHA_VISIBILITY_THRESHOLD){ //draw shadows only if it's noticeable:
			Color back=new Color(0,0,0,alphaLevelForTrainPoints);
			for (Point t:Solids.GetPointsInSolidUsingCenter(stain.getRmax(),to)){
				if (t.x >= 0 && t.y >= 0 && t.x < displayImage.getWidth() && t.y < displayImage.getHeight())
					if ((new Color(trainPointsOverImage.getRGB(t.x,t.y),true)).equals(new Color(0,0,0,0)))
						trainPointsOverImage.setRGB(t.x,t.y,back.getRGB());
			}
		}
		for (Point t:Solids.GetPointsInSolidUsingCenter(stain.getRmin(),to))
			try {trainPointsOverImage.setRGB(t.x,t.y,stain.getDisplayRGB());} catch (Exception e){}
	}
	/**
	 * No such thing in the color training example
	 */	
	public void NewNonPoint(String filename, Point to){}
}