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

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.Nuance.NuanceSubImage;
import GemIdentOperations.*;

/**
 * The father class that controls display of
 * all images during training and classification.
 * The class is abstract because it provides common 
 * functionality; but it, itself is never instantiated
 * 
 * @author Adam Kapelner and Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public abstract class KImagePanel extends JPanel{
	
	/** the scrollbar increment (in pixels) */
	private static final int INCREMENT=200;
	/** the pixel width of the scrollbars */
	private static final int ScrollBarWidth=11;
	/** the magnifier associated with this image panel */
	protected KMagnify magnify;
	/** the image currently being displayed (null if no image is displayed) */
	protected DataImage displayImage;
	/** a pointer to the current instantiation of this panel - useful for referencing "this" inside anonymous inner classes */
	protected KImagePanel that;
	/** the scrollPane that the image panel is housed and displayed in. If the image is larger than the alotted window, the scrollPane will display scrollbars */
	private JScrollPane scrollPane;
	/** next time the mouse enters this component, focus is taken */
	private boolean ifMouseEntersTakeFocus;
	protected int x_0;
	protected int y_0;
	protected int i_width;
	protected int i_height;

	/** sets up the scrollPane, the magnifier, and general listeners associated with all image panels */
	public KImagePanel(){
		super();
		
		scrollPane=new JScrollPane(this);
		JScrollBar vbar = scrollPane.getVerticalScrollBar();
		vbar.setUnitIncrement(INCREMENT);
		vbar.setPreferredSize(new Dimension(ScrollBarWidth,vbar.getHeight()));
		JScrollBar hbar = scrollPane.getHorizontalScrollBar();
		hbar.setUnitIncrement(INCREMENT);
		hbar.setPreferredSize(new Dimension(hbar.getWidth(),ScrollBarWidth));

		magnify=new KMagnify();
		that = this;
		
		SetUpListeners();
		repaint();
	}
	
	/** adds the listener to trigger the magnifier, enable the image panel specific keystrokes (spacebar,[,],1,2,...,etc) */
	private void SetUpListeners() {
		addMouseListener(
			new MouseListener(){
				public void mouseClicked(MouseEvent e){}					
				public void mouseEntered(MouseEvent e){
					UpdateMagnifierMouseEvent(e);
					//now handle if mouse enters, take focus:
					if (ifMouseEntersTakeFocus){
						//reset it
						ifMouseEntersTakeFocus = false;
						that.requestFocus();
					}
				}	
				public void mouseExited(MouseEvent e){
					if (Run.it != null){
						if (Run.it.getGUI().isActive()){
							that.transferFocus();
							UpdateMagnifier(null);	
						}
					}
				}	
				public void mousePressed(MouseEvent e){
					if (Run.it.getGUI().isActive())
						that.requestFocus();
				}	
				public void mouseReleased(MouseEvent e){}
			}
		);
		addMouseMotionListener(
			new MouseMotionListener(){
				public void mouseDragged(MouseEvent e){}	
				public void mouseMoved(MouseEvent e){
					if (Run.it != null){
						if (Run.it.getGUI().isActive())
							that.requestFocus();
					}
					UpdateMagnifierMouseEvent(e);
				}
			}
		);
	}
	/** checks for a null mouse event, otherwise passes the location to {@link #UpdateMagnifier(Point) the update function} */
	protected void UpdateMagnifierMouseEvent(MouseEvent e){
		if (e == null)
			UpdateMagnifier(null);
		else 
			UpdateMagnifier(e.getPoint());
	}
	/** given the location of the mouse, converts it to true screen location, and then asks the magnifier to update */
	protected void UpdateMagnifier( Point mousePosition ) {
		if ( displayImage != null ) {
			Point outer_bound = new Point(getWidth(),getHeight());			
			if ( mousePosition == null )
				magnify.setCursorLocation(null,null,outer_bound);			
			else if ( Run.it.getGUI().isActive() ) {
				Point temp= (Point)mousePosition.clone();
				SwingUtilities.convertPointToScreen(temp,that);
				magnify.setCursorLocation(temp,mousePosition,outer_bound);				
			}
			magnify.repaint();
		}		
	}
	/** 
	 * paints the image panel by displaying the current image
	 * in the scrollpane. If no image exists, the panel is painted
	 * black and a message is displayed.
	 */
	public void paintComponent( Graphics g ) {
		super.paintComponent(g);
		
		int p_width = this.getWidth();
		int p_height = this.getHeight();
		g.setColor(Color.BLACK);
		g.fillRect(0,0,p_width,p_height);

		if ( displayImage != null ) {
			i_width = displayImage.getWidth();
			i_height = displayImage.getHeight();
			
			x_0 = (p_width-i_width)/4;
			y_0 = (p_height-i_height)/4;
			
			if ( x_0 < 0 ) x_0 = 0;
			if ( y_0 < 0 ) y_0 = 0;

			if (displayImage instanceof NuanceSubImage){ //draw the Nuance filters atop the blank background
				g.drawImage(GetWhiteImage(i_width, i_height), x_0, y_0, i_width, i_height, null);
				for (BufferedImage I : ((NuanceSubImage)displayImage).getWaveImages()){
					g.drawImage(I,x_0,y_0,i_width,i_height,null);
				}
			}
			else {
				g.drawImage(displayImage.getAsBufferedImage(),x_0,y_0,i_width,i_height,null);
			}
		} else {
			g.setColor(Color.WHITE);
			g.drawString("[no image selected]",p_width/2-50,p_height/2-5);
		}
		scrollPane.revalidate();
					
	}
	/** gets the current mouse position on the screen */
	private Point getMousePos(){
		Point po=getMousePosition();
		if (po != null){
			Point p=(Point)po.clone();
			SwingUtilities.convertPointToScreen(p,that);
			return p;
		}
		else
			return null;
	}
	/** the magnifier is repainted by using the location of the mouse */
	public void repaintMagnifier() {
		Point outer_bound = new Point(this.getWidth(),this.getHeight());			
		magnify.setCursorLocation(getMousePos(),getMousePosition(),outer_bound);
		magnify.repaint();
	}
	/** increment or decrement the zoom level for the magnifier connected to this image panel */
	public void adjustMagnifierIntensity( int direction ) {
		Point outer_bound = new Point(this.getWidth(),this.getHeight());
		
		this.magnify.setCursorLocation(getMousePos(),getMousePosition(),outer_bound);
		if ( direction < 0 )
			this.magnify.decrementZoomLevel();
		else if ( direction > 0 )
			this.magnify.incrementZoomLevel();
	}
	public KMagnify getMagnifier(){
		return magnify;
	}
	protected void setDisplayImage( DataImage displayImage) {
		setDisplayImage(displayImage,1);
	}
	/** sets the display image to the panel, if resetPosition, then center it */
	protected void setDisplayImage( DataImage displayImage, int resetPosition) {
		this.displayImage = displayImage;
		if ( displayImage != null ){
			setPreferredSize(new Dimension(displayImage.getWidth(),displayImage.getHeight()));
			//center image within scrollpane
			if(resetPosition == 1){
				int new_horizontal_scroll_position = displayImage.getWidth() - scrollPane.getViewport().getWidth();
				int new_vertical_scroll_position = displayImage.getHeight() - scrollPane.getViewport().getHeight();
				if (new_horizontal_scroll_position > 0)
					scrollPane.getHorizontalScrollBar().setValue(new_horizontal_scroll_position / 2);
				if (new_vertical_scroll_position > 0)
					scrollPane.getVerticalScrollBar().setValue(new_vertical_scroll_position / 2);
			}
		}
	}	
	public BufferedImage getActiveImage() {
		return displayImage.getAsBufferedImage();
	}
	protected JScrollPane getScrollPane() {
		return scrollPane;
	}
	/** take a "screenshot" of whatever is currently being displayed */
	public BufferedImage saveScreenshot(){
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		paint(g);
		return image;
	}

	public void ifMouseEntersTakeFocus() {
		ifMouseEntersTakeFocus = true;
	}
	
	private static BufferedImage WHITE_IMAGE;
	/** create a cached white image of a certain size */
	private static BufferedImage GetWhiteImage(int w, int h){
		if (WHITE_IMAGE == null){
			WHITE_IMAGE = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			int white = Color.WHITE.getRGB();
			for (int i = 0; i < w; i++){
				for (int j = 0; j < h; j++){
					WHITE_IMAGE.setRGB(i, j, white);
				}
			}
		}
		return WHITE_IMAGE;
	}
}