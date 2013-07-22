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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import javax.swing.JPanel;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.Nuance.NuanceSubImage;
import GemIdentTools.*;

/**
 * Controls a thumbnail image that serves as an
 * informative and clickable icon inside the {@link
 * KThumbnailPane thumbnail pane} in both the {@link
 * KColorTrainPanel color training panel} and the {@link
 * KPhenotypeTrainPanel phenotype training panel}
 * 
 * @author Adam Kapelner and Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public class KThumbnail extends JPanel{
	
	/** the thumbnail of the underlying image this thumbnail represents */
	protected BufferedImage identifierImage;
	/** the number of training point examples for each of the {@link KClassInfo class infos} */
	private ArrayList<Integer> numbers;
	/** the title on the top of the thumbnail that informs the user of the number of training points for each of the {@link KClassInfo class infos} */
	private String counts;
	/** the filename of the image this thumbnail represents */
	protected String filename;
	/** The display on the bottom renders this string */
	private String filename_display;
	/** has this image been classified? (only applicable in the {@link KPhenotypeTrainPanel phenotype training panel} */
	private boolean classified;
	/** the training panel that functionally and informationally links this thumbnail with the rest of the program */
	private KTrainPanel trainPanel;
	

	/** the width of the thumbnail */
	public static final int THUMB_WIDTH = 80;
	/** the height of the thumbnail */
	public static final int THUMB_HEIGHT = 80;
	/** the default font size displayed in the thumbnail text */
	private static final int DEFAULT_FONT_SIZE=12;
	
	/** creates a thumbnail, initializes data, opens the true image from the hard disk, and adds the mouse listener */
	public KThumbnail(KTrainPanel trainPanel,String filename){
		super();
		
		this.filename = filename;
		this.filename_display = filename; //if you have a special image set, you can change the display here
		this.trainPanel=trainPanel;
		try {
			identifierImage = IOTools.OpenImage(Thumbnails.getThumbnailFilename(filename));
		} catch (IOException e){
			identifierImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		}
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

		classified=false;
		
		addMouseListener(
			new MouseListener(){	
				public void mouseClicked(MouseEvent e){}					
				public void mouseEntered(MouseEvent e){}	
				public void mouseExited(MouseEvent e){}				
				public void mousePressed( MouseEvent e ){
					doClick();
				}				
				public void mouseReleased(MouseEvent e){}
			}
		);
	}
	/** creates the {@link #counts title} from the {@link #numbers number of training points in each class info} */
	private void BuildAndSetTitle(){
		String title="";
		for (Integer n:numbers)
			title+=n+"-";
		if (numbers.size() > 0)
			counts=title.substring(0,title.length()-1);	
		else
			counts=title;

	}
	/** updates the {@link #numbers number of training points in each class info} and repaints thumbnail */
	public void setNumbersAndRedraw(ArrayList<Integer> numbers) {
		this.numbers=numbers;	
		BuildAndSetTitle();
		repaint();
	}
	/**
	 * Draws the thumbnail. If selected, paints a red border, if not, a black border, then
	 * it shrinks and draws the representative image, adjusts the font size in
	 * order to display the title nicely, draws it, then finally draws the filename
	 *  
	 * @param g					the Thumbnail's graphics object
	 * @param offset_x			the x-offset in the {@link KThumbnailPane pane} to draw it at
	 * @param offset_y			the y-offset in the {@link KThumbnailPane pane} to draw it at
	 * @param is_selected		did the user select this thumbnail?
	 */
	public void drawThumb( Graphics g, int offset_x, int offset_y, boolean is_selected ) {
		if ( !is_selected )
			g.setColor(Color.BLACK);
		else
			g.setColor(Color.RED);
		
		g.fillRect(offset_x,offset_y,THUMB_WIDTH,THUMB_HEIGHT);
		g.setColor(Color.WHITE);
		g.fillRect(offset_x+2,offset_y+2,THUMB_WIDTH-4,THUMB_HEIGHT-4);
		
		g.setColor(Color.BLACK);
		g.drawImage(identifierImage,offset_x+2,offset_y+20,THUMB_WIDTH-4,THUMB_HEIGHT/2,null);
		
		if (counts != null){
			int L=counts.length();
			if (L > 12)
				g.setFont(new Font("serif",Font.PLAIN,180/(L+2)));
			else
				g.setFont(new Font("serif",Font.PLAIN,DEFAULT_FONT_SIZE));
	
			g.drawString(counts,offset_x+4,offset_y+14);
		}
		if (filename_display != null){
			int L=filename_display.length();
			if (L > 12)
				g.setFont(new Font("serif",Font.PLAIN,180/(L+2)));
			else
				g.setFont(new Font("serif",Font.PLAIN,DEFAULT_FONT_SIZE));
	
			g.drawString(filename_display,offset_x+4,offset_y+THUMB_HEIGHT-4);
		}
	}
	/** when the user clicks on the thumbnail,
	 * it is selected in the {@link #trainPanel training panel}. If
	 * the user is retraining for phenotypes and this image was classified, 
	 * the image set in the panel draws the results mask over the image and
	 * the {@link KPhenotypeTrainPanel#revalidateTypeIErrors() type I errors are 
	 * revalidated}. If not, then only the image is displayed.
	 */
	public void doClick() {
		trainPanel.imagePanel.setDisplayImage(null); //temporarily blank it out
		trainPanel.setSelectedThumbnail(this);
		if (trainPanel instanceof KPhenotypeTrainPanel){
			((KImagePhenotypeTrainPanel)((KPhenotypeTrainPanel)trainPanel).imagePanel).setDisplayImage(ImageAndScoresBank.getOrAddDataImage(filename),classified);
			((KPhenotypeTrainPanel)trainPanel).revalidateTypeIErrors();
		}
		else
			trainPanel.imagePanel.setDisplayImage(ImageAndScoresBank.getOrAddDataImage(filename));
		DataImage image = trainPanel.imagePanel.displayImage;
		if (image instanceof NuanceSubImage)
			setImage(((NuanceSubImage)image).getAsBufferedImage());
	}
	public String getFilename() {
		return filename;
	}
	public void setClassified(boolean classified) {
		this.classified=classified;			
	}
	public void setImage(BufferedImage identifierImage){
		this.identifierImage=identifierImage;
		trainPanel.repaintThumbnails();
	}
}