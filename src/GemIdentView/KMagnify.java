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
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** 
 * Responsible for the magnifying window in the
 * {@link KColorTrainPanel color train panel}, the
 * {@link KPhenotypeTrainPanel phenotype train panel}, and the
 * {@link KClassifyPanel classification panel}. The magnifier
 * window is only active when the user's mouse hovers over
 * the analyzed image.
 *  
 * @author Adam Kapelner and Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public class KMagnify extends JPanel{
	
	/** the internal Java class responsible for taking screenshots */
	private Robot robot;
	/** the slider that allows the user to control the zoom level */
	private JSlider zoomLevelSlider;
	/** the current zoom level */
	private int zoomLevel;
	/** the location of the mouse */
	private Point cursorLocation;
	/** the image of the local vicinity of the mouse */
	private BufferedImage magnifiedScreenPortion;
	/** the true dimension of the image window to capture, adjusting for the zoom level */
	private Dimension actualDimension;
	/** the size of the magnified image pane - NOT the whole window including the slider */
	public static final Dimension magnifyPaneSize=new Dimension(251,451);
	/** the location of the mouse inside the image to be magnified */
	private Point interior_location;
	/** the bottom right corner of the image - used to test if the mouse is close to an edge */
	private Point outer_bound;
	
	/** takes the screenshot and displays the magnified image */
	private class MagnifyPanel extends JPanel {
		/** used to detect if cursor is near an edge */
		private Point offset;
		
		public MagnifyPanel(){
			super();
			setPreferredSize(magnifyPaneSize);
			offset = new Point(0,0);
		}		
		public void setOffset( Point offset ) {
			this.offset = offset;
		}	
		public Point getOffset() {
			return offset;
		}
		/** draws the magnified portion */
		public void paintComponent(Graphics g){
			try {
				magnifiedScreenPortion = GetMagnifiedPic(cursorLocation,interior_location,outer_bound);
				g.drawImage(magnifiedScreenPortion,0,0,magnifyPaneSize.width,magnifyPaneSize.height,null);
				
				int xo=(int)Math.round(magnifyPaneSize.width/((double)2))-zoomLevel-1;
				int yo=(int)Math.round(magnifyPaneSize.height/((double)2))-zoomLevel-1;
				
				int d_x = offset.x*zoomLevel;
				int d_y = offset.y*zoomLevel;
				
				//correct the black box in center --
				if (zoomLevel == 8 || zoomLevel == 12)
					g.drawRect(xo-zoomLevel/2+d_x,yo+d_y,zoomLevel,zoomLevel);	
				else if (zoomLevel == 10 || zoomLevel == 20 || zoomLevel == 4)
					g.drawRect(xo-zoomLevel/2+d_x,yo-zoomLevel/2+d_y,zoomLevel,zoomLevel);	
				else if (zoomLevel == 18 || zoomLevel == 6)
					g.drawRect(xo+d_x,yo-zoomLevel/2+d_y,zoomLevel,zoomLevel);
				else
					g.drawRect(xo+d_x,yo+d_y,zoomLevel,zoomLevel);
			} catch (Exception e){
				g.drawString("error", 0, 0);
			}
		}
		/** takes the screenshot and returns the appropriate region as a BufferedImage */
		private BufferedImage GetMagnifiedPic(Point loc, Point interior, Point outer ){
			if ( loc == null ) {
				BufferedImage image = new BufferedImage(KMagnify.magnifyPaneSize.width,KMagnify.magnifyPaneSize.height,BufferedImage.TYPE_INT_RGB);
				Graphics g = image.createGraphics();
				
				g.setColor(Color.BLACK);
				g.drawRect(0,0,image.getWidth(),image.getHeight());
				
				g.setColor(Color.GRAY);
				g.setFont(new Font("monospaced",Font.PLAIN,10));
				g.drawString("cursor out of range",image.getWidth()/2-54,image.getHeight()/2-5);
				
				return image;
			}
			
			actualDimension = new Dimension(
				(int)Math.round(magnifyPaneSize.width/((double)zoomLevel)),
				(int)Math.round(magnifyPaneSize.height/((double)zoomLevel))
			);
			
			int d_x = (int)Math.floor(-actualDimension.width/2)+1;
			int d_y = (int)Math.floor(-actualDimension.height/2)+1;
		
			loc.translate(d_x,d_y);
			interior.translate(d_x,d_y);
			setOffset(new Point(0,0));
			if ( interior.x < 0 ) {
				// reset...
				setOffset(new Point(interior.x,0));
				loc.x -= interior.x;
			}
			
			if ( interior.y < 0 ) {
				setOffset(new Point(getOffset().x,interior.y));
				loc.y -= interior.y;
			}

			if ( interior.x-2*d_x+3 >= outer_bound.x ) {
				setOffset(new Point(-outer_bound.x+interior.x-2*d_x+3,getOffset().y));
				loc.x -= (interior.x-2*d_x-outer_bound.x+3);
			}
			
			if ( interior.y-2*d_y+3 >= outer_bound.y ) {
				setOffset(new Point(getOffset().x,interior.y-outer_bound.y-2*d_y+3));
				loc.y -= (interior.y-2*d_y-outer_bound.y+3);
			}

			return robot.createScreenCapture(
				new Rectangle(
					loc,
					actualDimension
				)
			);
		}
	}

	/** initializes the components as well as the {@link #robot robot} and adds them to the main container */
	public KMagnify(){
		super();
				
		try {robot=new Robot();} catch (AWTException e){e.printStackTrace();}
		
		//now create contents of Panel and throw everything into it:
		Container c=Box.createVerticalBox();
		c.add(CreateZoomSliderContainer());
		c.add(new MagnifyPanel());		
		add(c);
	}
	/** creates the zoom slider with its appropriate listener in a frame with a title */
	private Component CreateZoomSliderContainer() {
		GUIFrame settings_panel=new GUIFrame("Zoom Level");
		Container settings_box=new Container();
		
		zoomLevel=4; //initialize
		zoomLevelSlider=new JSlider(JSlider.HORIZONTAL,1,10,zoomLevel/2);
		zoomLevelSlider.addChangeListener(
			new ChangeListener(){	
				public void stateChanged(ChangeEvent e) {
			       zoomLevel=((JSlider)e.getSource()).getValue()*2;
			       repaint();
				}
			}
		);
		settings_box.setLayout(new GridLayout(0,1,1,0));
		settings_box.add(zoomLevelSlider);
		settings_panel.add(settings_box);
		return settings_panel;
	}
	/** sets the location of the cursor */
	public void setCursorLocation(Point cursorLocation,Point interior_location, Point outer_bound ){
		this.cursorLocation=cursorLocation;
		this.interior_location=interior_location;
		this.outer_bound=outer_bound;
	}	
	/** increase the zoom level by one - bounds checking is done by the slider */
	public void incrementZoomLevel() {
		zoomLevelSlider.setValue(zoomLevelSlider.getValue()+1);	
		repaint();
	}
	/** decrease the zoom level by one - bounds checking is done by the slider */
	public void decrementZoomLevel() {
		zoomLevelSlider.setValue(zoomLevelSlider.getValue()-1);		
		repaint();
	}
}