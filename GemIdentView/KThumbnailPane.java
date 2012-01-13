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
import java.util.*;

import javax.swing.*;

import GemIdentImageSets.DataImage;
import GemIdentModel.*;
import GemIdentOperations.Run;
import GemIdentTools.*;

/**
 * Controls the panel that houses the thumbnail images 
 * in both the {@link KColorTrainPanel color training panel} 
 * and the {@link KPhenotypeTrainPanel phenotype training panel}
 * 
 * @author Adam Kapelner and Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public class KThumbnailPane extends JPanel {
	
	/** the amount of space (in pixels) between thumbnails */
	private static final int SpaceBetweenThumbs = 10;
	
	/** the mapping from filename to the thumbnail */
	protected HashMap<String,KThumbnail> thumbnails;
	/** the filenames of the thumbnails in the order they appear from left to right */
	private ArrayList<String> ordered_keys;
	/** the training panel that functionally and informationally links this pane with the rest of the program */
	private KTrainPanel trainPanel;
	
	/** the height of the thumbnail pane */
	public static final int prefHeight=80;
	/** the thickness of the left/right scroll buttons */
	public static final int scroll_thickness = 15;
	/** is the mouse over one of the scroll buttons? If so, which one? */
	private int is_mouse_over = 0;
	/** id for the left scroll button */
	private static final int LEFT = 1;
	/** id for the right scroll button */
	private static final int RIGHT = 2;
	/** the number of pixels moved for each scroll */
	private static final int SCROLL_QUANTA = 20;
	
	/** the current selected thumbnail */
	private KThumbnail selectedThumb;
	/** the horizontal offset in which to paint the pane */
	private int scroll_offset = 0;
	/** the amount the user chose to scroll */
	private int scroll_amount = 0;
	/** a pointer to the current instantiation in order reference it inside anonymous classes */
	private KThumbnailPane that = this;

	/** controls scrolling */
	private class ScrollThread extends Thread {
		/** the user chooses an amount to scroll and the pane will periodically update */
		public void run() {
			while ( scroll_amount != 0 ) {
				scroll_offset += that.scroll_amount;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						that.repaint();
					}
				});
				try {
					Thread.sleep(35); //wait a bit
				} catch ( InterruptedException e ) {}
			}
		}
	}
	/**
	 * Creates a thumbnail pane. Initializes the data, sets the size, and adds listeners
	 * 
	 * @param trainPanel		the panel that functionally and informationally links the pane to the rest of the GUI
	 */
	public KThumbnailPane( KTrainPanel trainPanel ) {
		super();
		this.trainPanel = trainPanel;
		thumbnails = new HashMap<String,KThumbnail>();
		ordered_keys = new ArrayList<String>();

		selectedThumb = null;
		scroll_offset=0;
		setPreferredSize(new Dimension(KFrame.frameSize.width,KThumbnailPane.prefHeight));
		addListeners();
	}
	
	/** click the first thumbnail if it exists */
	public void ClickFirstThumbnailIfExistsAndIsNecessary(){
		if (thumbnails.size() > 0 && selectedThumb == null){
			ClickNthThumbnail(0);
		}
	}
	
	/** Click thumbnail number n */
	private void ClickNthThumbnail(int n){
		thumbnails.get(ordered_keys.get(n)).doClick();
		repaint();		
	}
	/** adds mouse listeners - checks if the user is clicking 
	 * on a thumbnail or on a scroll button, also displays 
	 * the scroll buttons correctly when user mouses over
	 */
	public void addListeners(){
		addMouseListener(
			new MouseListener(){
				public void mousePressed( MouseEvent e ) {
					if (NeedToDrawScrollButtons()){
						if ( e.getX() < scroll_thickness+4 ) {
							scroll_amount = KThumbnailPane.SCROLL_QUANTA;
							new ScrollThread().start();
							return;
						} else if ( e.getX() > getWidth()-scroll_thickness-4 ) {
							scroll_amount = -KThumbnailPane.SCROLL_QUANTA;
							new ScrollThread().start();
							return;
						}
					}
					Integer n=FindMouseClick(e);
					// this really should be joined with the more or less identical function below via decomp...
					if (n != null){
						ClickNthThumbnail(n);
					}
				}
				
				public void mouseReleased( MouseEvent e ) {
					that.scroll_amount = 0;
				}
				
				public void mouseExited( MouseEvent e ) {
					if (NeedToDrawScrollButtons()){
						that.is_mouse_over = 0;
						that.scroll_amount = 0;
						repaint(0,0,KThumbnailPane.scroll_thickness+4,getHeight());
						repaint(getWidth()-KThumbnailPane.scroll_thickness-4,0,KThumbnailPane.scroll_thickness+4,getHeight());
					}				
					setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
				
				public void mouseEntered( MouseEvent e ) {}
				public void mouseClicked( MouseEvent e ) {}
			}
		);
		//to check is the mouse is over a button
		addMouseMotionListener(new MouseMotionListener() {
			public void mouseMoved( MouseEvent e ) {
				int n_is_mouse_over = 0;
				if ( e.getX() < KThumbnailPane.scroll_thickness+4 )
					n_is_mouse_over = KThumbnailPane.LEFT;
				else if ( e.getX() > getWidth()-KThumbnailPane.scroll_thickness-4 )
					n_is_mouse_over = KThumbnailPane.RIGHT;
				else {
					Integer n=FindMouseClick(e);
					if (n == null)
						that.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					else
						that.setCursor(new Cursor(Cursor.HAND_CURSOR));
					
//					if ( n_is_mouse_over != is_mouse_over ) return;
				}
				if (NeedToDrawScrollButtons() && n_is_mouse_over != is_mouse_over){
					is_mouse_over = n_is_mouse_over;
					repaint(0,0,KThumbnailPane.scroll_thickness+4,getHeight());
					repaint(getWidth()-KThumbnailPane.scroll_thickness-4,0,KThumbnailPane.scroll_thickness+4,getHeight());
					
					if ( is_mouse_over != 0 )
						setCursor(new Cursor(Cursor.HAND_CURSOR));
					else
						setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
				
			}			
			public void mouseDragged( MouseEvent e ) {}
		});
	}
	/**
	 * Given a user click, checks to see which thumbnail the user clicked on
	 * 
	 * @param e		the user's click
	 * @return		the thumbnail number the user clicked on, null if N/A
	 */
	public Integer FindMouseClick(MouseEvent e){
		if (!NeedToDrawScrollButtons())
			scroll_offset=0;
		int x = e.getX();
		
		x-=GetSymmetricXOffset();

		x-=scroll_offset;

		int n = x / (KThumbnail.THUMB_WIDTH+SpaceBetweenThumbs);
		int b = x % (KThumbnail.THUMB_WIDTH+SpaceBetweenThumbs);
		if ( n < 0 || n >= that.ordered_keys.size() || b < SpaceBetweenThumbs/2 || b >= KThumbnail.THUMB_WIDTH+SpaceBetweenThumbs/2 )
			return null;
		else 
			return n;
	}
	/** paints the pane - {@link #drawThumbnails(Graphics) draws the thumbnails} and if necessary, {@link #drawScrollButtons(Graphics) draws the scroll buttons} */
	public void paintComponent( Graphics g ) {
//		super.paintComponent(g);

		Color background = super.getBackground();
		g.setColor(background);
		g.fillRect(0,0,this.getWidth(),this.getHeight());
		
		// draw thumbnails
		drawThumbnails(g);
		
		// draw scroll buttons
		if (NeedToDrawScrollButtons())
			drawScrollButtons(g);
	}	
	/** are there enough thumbnails in the pane where scroll buttons would have to be drawn? */
	private boolean NeedToDrawScrollButtons(){
		if ((ordered_keys.size()*(KThumbnail.THUMB_WIDTH+SpaceBetweenThumbs)-SpaceBetweenThumbs) > KFrame.frameSize.width)
			return true;
		else
			return false;
	}
	/** Decorates the panel with scroll buttons. */
	private void drawScrollButtons( Graphics g ) {
		// create outer border
		g.setColor(Color.BLACK);
		g.fillRect(0,0,KThumbnailPane.scroll_thickness+4,this.getHeight());
		g.fillRect(this.getWidth()-KThumbnailPane.scroll_thickness-4,0,this.getWidth(),this.getHeight());
		
		// create the inner box
		if ( this.is_mouse_over == LEFT )
			g.setColor(KClassInfo.SELECTED_COLOR);
		else
			g.setColor(Color.WHITE);
		g.fillRect(2,2,KThumbnailPane.scroll_thickness,this.getHeight()-4);
		
		if ( this.is_mouse_over == RIGHT )
			g.setColor(KClassInfo.SELECTED_COLOR);
		else
			g.setColor(Color.WHITE);
		g.fillRect(this.getWidth()-KThumbnailPane.scroll_thickness-2,2,KThumbnailPane.scroll_thickness,this.getHeight()-4);
		
		// create arrows
		// -- begin by creating the point grids
		int[] x_left = new int[3];
		int[] y_left = new int[3];
		int[] x_right = new int[3];
		
		x_left[0] = 2+KThumbnailPane.scroll_thickness/6;
		x_left[1] = 2+KThumbnailPane.scroll_thickness-KThumbnailPane.scroll_thickness/6;
		x_left[2] = x_left[1];
		
		y_left[0] = this.getHeight()/2;
		y_left[1] = y_left[0]-KThumbnailPane.scroll_thickness/3;
		y_left[2] = y_left[0]+KThumbnailPane.scroll_thickness/3;
		
		x_right[0] = this.getWidth()-2-KThumbnailPane.scroll_thickness/6;
		x_right[1] = this.getWidth()-2-KThumbnailPane.scroll_thickness+KThumbnailPane.scroll_thickness/6;
		x_right[2] = x_right[1];
		
		// -- now actually draw them
		g.setColor(Color.BLACK);
		g.fillPolygon(x_left,y_left,3);
		g.fillPolygon(x_right,y_left,3);
	}
	/** {@link KThumbnail#drawThumb(Graphics, int, int, boolean) draws each thumbnail} in the pane, respecting the scroll offset */
	public void drawThumbnails( Graphics g ) {
		int y_0 = (this.getHeight()-KThumbnail.THUMB_HEIGHT)/2;		
		int x_offset=GetSymmetricXOffset();
		if (!NeedToDrawScrollButtons())
			scroll_offset=0;
		
		x_offset+=scroll_offset;	
		x_offset+=(SpaceBetweenThumbs/2);
		for ( String filename: this.ordered_keys ) {
			KThumbnail thumb = this.thumbnails.get(filename);
			thumb.drawThumb(g,x_offset,y_0,this.selectedThumb==thumb);			
			x_offset += KThumbnail.THUMB_WIDTH+SpaceBetweenThumbs;
		}
	}
	/** gets the offset in which to begin the drawing of the panel */
	private int GetSymmetricXOffset(){
		return (this.getWidth()-(ordered_keys.size()*(KThumbnail.THUMB_WIDTH+SpaceBetweenThumbs)-SpaceBetweenThumbs))/2;
	}
	/** 
	 * allows the user to choose a thumbnail by opening the 
	 * {@link GemIdentTools.Thumbnails#ChooseThumbnail(Collection) 
	 * chooose thumbnail dialog window}. Based on the selection, a new thumbnail
	 * is created and set to be selected 
	 */
	public KThumbnail ChooseAndAddThumbnail(String filename){
		if (filename == null)
			filename=Thumbnails.ChooseThumbnail(thumbnails.keySet());
		if (filename != null){
			if (isInThumbnails(filename)) //a check on the right clicking from the Nuance training helper / sophisticated training helper
				return null; //can't add same one twice!
			KThumbnail thumbnail=new KThumbnail(trainPanel,filename);
			addThumbnail(thumbnail,true);
			setSelectedThumbnail(thumbnail);
			return thumbnail;
		}
		return null;
	}
	/**
	 * Adds a thumbnail to the pane
	 * 
	 * @param thumbnail			thumbnail to be added
	 * @param isSelected		whether or not the thumbnail is to be displayed as selected
	 */
	public void addThumbnail(KThumbnail thumbnail,boolean isSelected){
		if (isSelected)
			setSelectedThumbnail(thumbnail);
		String filename=thumbnail.getFilename();
		this.ordered_keys.add(filename);		
		thumbnails.put(filename,thumbnail);	
		thumbnail.doClick();
		this.repaint();
	}
	/** removes a thumbnail from the pane by the filename of the image it represents */
	public void RemoveThumbnail(String filename){
		Run.it.imageset.LOG_AddToHistory("removed " + filename + " from training set");
		thumbnails.remove(filename);
		this.ordered_keys.remove(filename);
		this.repaint();
	}
	/** when a training point change is made, the label on the thumbnail must be updated */
	public void updateInfoLabelForPresentPic(){
		String filename=null;
		try {
			filename=trainPanel.imagePanel.displayImage.getFilename();
		} catch (NullPointerException e){
			return;
		}
		KThumbnail targetThumbnail=thumbnails.get(filename);
		ArrayList<Integer> numbers=new ArrayList<Integer>();
		
		for (KClassInfo info:trainPanel.browser.getClassInfosInOrder()){
			TrainSuperclass trainer=info.getTrainer();
			TrainingImageData imageData=trainer.getTrainingImage(filename);
			if (imageData == null)
				numbers.add(0);
			else
				numbers.add(imageData.getNumPoints());
		}
		targetThumbnail.setNumbersAndRedraw(numbers);	
	}
	/** updates the training info label for all thumbnails in the pane */
	public void updateInfoLabelForAllThumbnails(){
		for (KThumbnail thumbnail:thumbnails.values()){
			ArrayList<Integer> numbers=new ArrayList<Integer>();
			String filename=thumbnail.getFilename();
			for (KClassInfo info:trainPanel.browser.getClassInfosInOrder()){
				if (info instanceof KStainInfo){			
					Stain stain=((KStainInfo)info).getStain();
					TrainingImageData imageData=stain.getTrainingImage(filename);
					if (imageData == null){
						numbers.add(0);
					}
					else {
						numbers.add(imageData.getNumPoints());
					}
				}
				else {
					Phenotype phenotype=(Phenotype)info.getTrainer();
					TrainingImageData imageData=phenotype.getTrainingImage(filename);
					if (imageData == null)
						numbers.add(0);
					else
						numbers.add(imageData.getNumPoints());
				}
			}
			thumbnail.setNumbersAndRedraw(numbers);	
		}
	}	
	public Collection<String> getThumbnailNames() {
		return thumbnails.keySet();
	}	
	public HashMap<String,KThumbnail> getThumbnails() {
		return thumbnails;
	}
	public KThumbnail getSelectedThumbnail(){
		return selectedThumb;
	}
	public void setSelectedThumbnail( KThumbnail selectedThumb ) {
		this.selectedThumb=selectedThumb;
	}	
	public int getNumThumbnails(){
		return thumbnails.size();
	}	
	public boolean isInThumbnails(String filename){
		return thumbnails.containsKey(filename);
	}
	/** load the images from scratch to repaint all the thumbnails, useful for when first loading the project */
	public void repaintThumbnailsWithDataImages(HashMap<String, DataImage> trainedImagesAsDataImages) {
		for (String thumbnail_name : thumbnails.keySet()){
			DataImage image = trainedImagesAsDataImages.get(thumbnail_name);
			thumbnails.get(thumbnail_name).setImage(image.getAsBufferedImage());
		}
	}
}