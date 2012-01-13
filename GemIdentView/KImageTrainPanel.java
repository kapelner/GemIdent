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

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import GemIdentImageSets.DataImage;
import GemIdentOperations.FindErrorRateViaReconcilation;

/**
 * The father class that controls display of
 * all images during training. The class is abstract 
 * because it provides common functionality; but it, 
 * itself is never instantiated
 * 
 * @author Adam Kapelner and Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public abstract class KImageTrainPanel extends KImagePanel implements TrainClickListener{

	/** if the alpha for the circular training masks is below this level, the training masks are not displayed */
	public static final int ALPHA_VISIBILITY_THRESHOLD=5;

	/** the listeners that are notified if the user clicks inside the training panel */
	private ArrayList<TrainClickListener> trainClickListeners;
	
	/** the current alpha level of the training masks */
	protected int alphaLevelForTrainPoints;
	/** the image that contains the training points and the training masks but is transparent for all other pixels */
	protected BufferedImage trainPointsOverImage;
	/** the training panel that the image is linked to */
	protected KTrainPanel trainPanel;
	/** should the entire image panel be redrawn from scratch? */
	private boolean redrawImage;
	
	/** during retraining, the images that contain classification results for all phenotypes that were classified */
	private ArrayList<BufferedImage> isOverImages;
	/** during retraining, the images that contain post-processing results for all phenotypes that were post-processed */
	private ArrayList<BufferedImage> isCentroidOverImages;

	/** does the user wish to see his training points? */
	private boolean seeTrainPoints;
	/** does the user wish to see the type I errors? */
	private boolean seeErrors;
	/** the image where the red error rings are displayed, but transparent for all other pixels */
	private BufferedImage errorOverlay;
	
	/** default constructor that also creates listeners for components and adds itself as a training point listener */
	public KImageTrainPanel(KTrainPanel trainPanel){
		super();
		this.trainPanel=trainPanel;
		alphaLevelForTrainPoints=0; //default value
		seeTrainPoints=true; //default value
		trainClickListeners=new ArrayList<TrainClickListener>();
		redrawImage=false; //default
		isOverImages=new ArrayList<BufferedImage>();
		isCentroidOverImages=new ArrayList<BufferedImage>();
		CreateListeners();
		AddTrainClickListener(this);
		that=this; //this may not be needed because it's done in father class
	}
	/** 
	 * adds listeners to the panel that fire the {@link #trainClickListeners 
	 * training point listeners}, update the {@link KMagnify magnifier},
	 * as well as the location label sensor 
	 */
	private void CreateListeners() {
		addMouseListener(
			new MouseListener(){				
				public void mouseClicked(MouseEvent e){}
				public void mouseEntered(MouseEvent e){}
				public void mouseExited(MouseEvent e){}
				public void mousePressed(MouseEvent e){
					if (seeTrainPoints){
						if (displayImage != null){
							Point local=adjustPoint(e.getPoint());
							if (local != null){
								//fire all train click listeners
								switch (e.getButton()){
									case MouseEvent.BUTTON2: //ie middle click
										for (TrainClickListener l:trainClickListeners)
											l.DeletePoint(displayImage.getFilename(), local);
										break;
									case MouseEvent.BUTTON3: //ie right click	
										for (TrainClickListener l:trainClickListeners)
											l.NewNonPoint(displayImage.getFilename(), local);
										break;
										
									default: //ie left click or any other click in OS's that don't recognize the constants above
										for (TrainClickListener l:trainClickListeners)
											l.NewPoint(displayImage.getFilename(), local);										
								}
								

							}
						}
					}
					repaint();
				}
				public void mouseReleased(final MouseEvent e){
					if (seeTrainPoints)
						UpdateMagnifierMouseEvent(e);
				}
			}
		);	
		addMouseMotionListener(
			new MouseMotionListener(){
				public void mouseDragged(MouseEvent e){}	
				public void mouseMoved(MouseEvent e){
					trainPanel.UpdateLocationLabel(adjustPoint(e.getPoint()));
				}
			}
		);
	}
	/** adjust the point to represent the coordinate in the image, controlling for the black borders */
	private Point adjustPoint( Point p ) {
		Point q = new Point(p);

		int p_width = this.getWidth();
		int p_height = this.getHeight();
		try {
			int i_width = this.displayImage.getWidth();
			int i_height = this.displayImage.getHeight();
			int d_x = (p_width-i_width)/4;
			int d_y = (p_height-i_height)/4;
			
			if (d_x > 0)
				q.x -= d_x;
			if (d_y > 0)
				q.y -= d_y;
			if (q.x < displayImage.getWidth() && q.x >=0 && q.y < displayImage.getHeight() && q.y >= 0)		
				return q;
			else 
				return null;
		}
		catch (Exception e){
			return null;
		}
	}
	/** adds a new listener that will be notified about new training points */
	public void AddTrainClickListener(TrainClickListener l){
		trainClickListeners.add(l);
	}	
	/** 
	 * draws the current training image (as well as black borders around it),
	 * then the training point overlay mask, followed by classification and/or 
	 * post-processing results (during retraining) as well as the error mask
	 * if necessary 
	 */
	public void paintComponent(Graphics g){
		super.paintComponent(g); //first draw the actual image		
		if (displayImage == null) // quit if there's no image to load...
			return;
		else
			displayImage.setPanel(this);
		
		int p_width = this.getWidth();
		int p_height = this.getHeight();
		int i_width = this.displayImage.getWidth();
		int i_height = this.displayImage.getHeight();
		
		int x_0 = (p_width-i_width)/4;
		int y_0 = (p_height-i_height)/4;
		
		if ( x_0 < 0 ) x_0 = 0;
		if ( y_0 < 0 ) y_0 = 0;

		for (BufferedImage isOverImage:isOverImages){
        	g.drawImage(isOverImage,x_0,y_0,i_width,i_height,null);//draw on top of original image
		}
        for (BufferedImage isCentroidOverImage:isCentroidOverImages)
        	g.drawImage(isCentroidOverImage,x_0,y_0,i_width,i_height,null);//draw on top of original image
		if (seeTrainPoints){
			if (trainPointsOverImage == null)
				trainPointsOverImage=new BufferedImage(displayImage.getWidth(),displayImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
			if (redrawImage)
				ReDrawOverImageFromScratch();		
	        g.drawImage(trainPointsOverImage,x_0,y_0,i_width,i_height,null);//draw on top of original image
		}
		if (seeErrors)
			g.drawImage(errorOverlay,x_0,y_0,i_width,i_height,null);//draw on top of original image			
	}
	/** redraws the contents from scratch - to be implemented by daughter classes */
	protected abstract void ReDrawOverImageFromScratch();
	
	/** the function that is called via the {@link TrainClickListener listener} when a new point is clicked - to be implemented by daughter classes */ 
	public abstract void NewPoint(String filename,Point to);
	
	/** when a point is deleted, do nothing, just rely on a complete redraw from elsewhere */ 
	public void DeletePoint(String filename,Point to){}
	
	/** sets a new training image. During retraining, creates the {@link #errorOverlay mask} that will display type I errors */
	protected void setDisplayImage(DataImage displayImage) {
		redrawImage = true;
		super.setDisplayImage(displayImage);
		
		isOverImages = new ArrayList<BufferedImage>();
		isCentroidOverImages = new ArrayList<BufferedImage>();
		if (displayImage != null)
			errorOverlay=FindErrorRateViaReconcilation.CreateErrorOverlay(displayImage);
		repaint();		
	}	
	/** adds a new mask that displays classification results */
	public void addIsOverImage(BufferedImage isOverImage){
		isOverImages.add(isOverImage);	
	}
	/** adds a new mask that displays post-processing results */
	public void addIsCentroidOverImage(BufferedImage isCentroidOverImage){
		isCentroidOverImages.add(isCentroidOverImage);		
	}
	public void setSeeTrainPoints(boolean seeTrainPoints){
		this.seeTrainPoints=seeTrainPoints;		
		repaint();
	}
	public void setSeeErrors(boolean seeErrors){
		this.seeErrors=seeErrors;
		repaint();
	}
	public void setRedrawImage(boolean redrawImage) {
		this.redrawImage = redrawImage;
	}
	protected void setAlphaLevelForTrainPoints(int alphaLevelForTrainPoints) {
		this.alphaLevelForTrainPoints=alphaLevelForTrainPoints;		
	}
	public void setSelectedThumbnailImageToCurrentImage() {
		trainPanel.getSelectedThumbnail().setImage(saveScreenshot());		
	}
}