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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPanel;

import GemIdentAnalysisConsole.BuildGlobals;
import GemIdentModel.Phenotype;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;

/**
 * This class houses the image icons located on the left of the 
 * {@link KAnalysisPanel analysis panel} (the "icon panel").
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class ElementView extends JPanel implements MouseListener{

	/** the display message in the icon if the image is of type unknown */
	public static final String OTHER="other";
	/** the display message in the icon if the image is binary representing positively classified <b>pixels</b> */
	public static final String PIXEL="isPixel";
	/** the display message in the icon if the image is binary representing <b>centroid locations</b> */
	public static final String CENTROID="isCentroid";
	/** the image icon of a "hard disk drive" displayed only on those images saved on the hard drive */
	private static final Image HardDrive=IOTools.OpenSystemImage("hd.png");
	
	/** the mapping from image name to the data structure that controls it */
	private HashMap<String,Element> elements;
	/** the console object embedded in the analysis panel - necessary in order to interact with the rest of the panel */
	private Console console;
	
	/** the background color of the icon when the image is in memory */
	private static final Color LoadedColor=new Color(140,255,140);
	/** the background color of the icon when the image is on the hard drive and not in memory */
	private static final Color UnLoadedColor=new Color(255,140,140);
	
	/** the object that controls how an individual image icon is displayed and interacts with the analysis panel */
	private class Element{
		/** the name of the image */
		private String name;
		/** the type of the image (either {@link ElementView#PIXEL isPixel}, {@link ElementView#CENTROID isCentroid}, or {@link ElementView#OTHER other} */
		private String type;
		/** the current background color of the icon */
		private Color color;
		/** stores the coordinates of this icon's display */
		private Rectangle rect;
		/** is this image saved on the hard disk? */
		private boolean saved;

		/** default constructor */
		public Element(String name,String type,boolean saved){
			setLoaded(false);
			this.saved=saved;
			this.name=name;
			this.type=type;
		}
		public String getImageName(){
			return name;
		}
		public String getImageType(){
			return type;
		}
		public Color getColor(){
			return color;
		}
		/**
		 * Sets whether or not this image is loaded in memory
		 * (just sets the background color)
		 * 
		 * @param loaded			is the image in memory?
		 */
		public void setLoaded(boolean loaded){
			if (loaded)
				color=LoadedColor;		
			else
				color=UnLoadedColor;
		}
		public void setRect(Rectangle rect){
			this.rect=rect;			
		}
		public boolean contains(Point t){
			return rect.contains(t);
		}
		public void setSaved(boolean saved){
			this.saved=saved;
		}
		public boolean isSaved(){
			return saved;
		}
	}
	
	/** initializes data, sets the size of the icon panel, and initializes the mouse listener */
	public ElementView(){
		super();
		elements=new HashMap<String,Element>();
		setPreferredSize(new Dimension(KClassInfoBrowser.prefWidth,KFrame.frameSize.height));
		this.addMouseListener(this);
	}
	/**
	 * Upon loading a project the image filenames are {@link 
	 * GemIdentAnalysisConsole.BuildGlobals#GetGlobalFilenames() 
	 * loaded from the hard disk} and they are set up as icons
	 *  
	 * @param list			the names of the image saved on the hard disk
	 */
	public void SetUpGlobalImagesAfterProjectOpen(ArrayList<String> list){		
		for (String name:list){
			Phenotype phenotype=Run.it.getPhenotype(name);
			if (phenotype == null)
				addElement(name,OTHER,false,true);
			else if (phenotype.isFindCentroids())
				addElement(name,CENTROID,false,true);
			else if (phenotype.isFindPixels())
				addElement(name,PIXEL,false,true);
		}
		repaint();
	}
	/**
	 * After the global images are {@link GemIdentAnalysisConsole.BuildGlobals#BuildGlobals() 
	 * built}, each is set up as an icon
	 * 
	 * @param name		the name of the global images
	 */
	public void SetUpInitialElement(String name){
		if (name.contains(BuildGlobals.Centroids))
			addElement(name, CENTROID, true, false);
		else if (name.contains(BuildGlobals.Pixels))
			addElement(name, PIXEL, true, false);	
		else
			addElement(name, OTHER, true, false);		
		repaint();
	}
	/**
	 * Add a new image icon to the icon panel.
	 * 
	 * @param name			the name of the image
	 * @param type			the image type (either {@link ElementView#PIXEL isPixel}, {@link ElementView#CENTROID isCentroid}, or {@link ElementView#OTHER other}
	 * @param loaded		is the image in memory?		
	 * @param saved			is the image saved to the hard disk?
	 */
	public void addElement(String name,String type,boolean loaded,boolean saved){
		Element e=new Element(name,type,saved);
		e.setLoaded(loaded);
		synchronized(elements){
			elements.put(name,e);
		}
		repaint();
	}
	
	public ArrayList<String> getAllElementNamesOfType(String type){
		ArrayList<String> names = new ArrayList<String>();
		for (Element e : elements.values()){
			if (e.getImageType().equals(type)){
				names.add(e.getImageName());
			}
		}		
		return names;
	}
	
	/** the height of an icon (in pixels) */
	private static final int Height=40;
	/** the padding around the border of the image icon panel (in pixels) */
	private static final int SpaceAround=8;
	/** the padding on the inside of an image icon (in pixels) */
	private static final int BoxMargin=3;
	/** the x-margin for the display of the name of the image (in pixels) */
	private static final int NameMargin=2;
	/** the x-margin for the display of the type of the image (in pixels) */
	private static final int TypeMargin=5;
	/** the font in which to display the image's name */
	private static final Font NameFont=new Font(null,Font.BOLD,14);
	/** the font in which to display the image's type */
	private static final Font TypeFont=new Font(null,Font.ITALIC,12);
	
	/** paints the entire panel by drawing each image icon and then moving down to draw the next */
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		g.setColor(this.getBackground());
		g.fillRect(0,0,this.getWidth(),this.getHeight());
		int h=SpaceAround;
		synchronized(elements){
			for (Element e:elements.values()){
				g.setColor(Color.black);
				Rectangle rect=new Rectangle(SpaceAround-BoxMargin,h-BoxMargin,this.getWidth()-2*SpaceAround+2*BoxMargin,Height+2*BoxMargin);
				e.setRect(rect);
				g.fillRect(rect.x,rect.y,rect.width,rect.height);
				g.setColor(e.getColor());
				g.fillRect(SpaceAround,h,this.getWidth()-2*SpaceAround,Height);
				g.setColor(Color.BLACK);
				g.setFont(NameFont);
				g.drawString(e.getImageName(),SpaceAround+NameMargin,h+15);
				g.setColor(Color.DARK_GRAY);
				g.setFont(TypeFont);
				g.drawString(e.getImageType(),SpaceAround+TypeMargin,h+30);
				if (e.isSaved())
					g.drawImage(HardDrive,this.getWidth()-2*SpaceAround+2*BoxMargin-40,h+(Height-21)/2,35,21,null);
				h+=(Height+2*SpaceAround);
			}
		}
	}
	/**
	 * Marks an image as either in memory or not in memory. If
	 * the image is unloaded and not in memory it is lost and the
	 * icon is removed
	 * 
	 * @param name			the name of the image
	 * @param loaded		is this image loaded in memory?
	 */
	public void loadElement(String name,boolean loaded){
		synchronized(elements){
			Element e=elements.get(name);
			e.setLoaded(loaded);
			if (!loaded && !e.isSaved())
				elements.remove(name);			
		}
		repaint();
	}
	public void mouseClicked(MouseEvent arg0){}
	public void mouseEntered(MouseEvent arg0){}
	public void mouseExited(MouseEvent arg0){}
	public void mouseReleased(MouseEvent arg0){}
	/** the only of the MouseListener functions used - if the user
	 * clicks on an image icon, the name of that image is added to
	 * the command textbox in the {@link Console console}
	 * 
	 * @param event		the event returned from the mouselistener
	 */
	public void mousePressed(MouseEvent event) {
		synchronized(elements){
			for (Element e:elements.values())
				if (e.contains(event.getPoint()))
					console.addImageNameToEnter(e.getImageName());	
		}
	}
	/**
	 * Sets an image icon to the "saved" state - the {@link #HardDrive
	 * icon of the hard disk} will now apper in the icon - or
	 * the unsaved state
	 * 
	 * @param name		the name of the image to be saved
	 * @param saved		is the icon saved?
	 */
	public void setSaved(String name,boolean saved){
		synchronized(elements){
			(elements.get(name)).setSaved(saved);		
		}
		repaint();
	}
	public boolean isSaved(String name){
		return (elements.get(name)).isSaved();
	}
	public void setConsole(Console console){
		this.console=console;		
	}
}