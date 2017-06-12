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
import java.awt.image.BufferedImage;

import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;

/**
 * Controls the displaying of result images during {@link 
 * GemIdentClassificationEngine.Classify classification} and
 * {@link GemIdentCentroidFinding.PostProcess post-processing}.
 * For discussion on viewing the classification via watching the
 * image panel, see section 4.1.3 of the manual.
 * 
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class KImageClassifyPanel extends KImagePanel{
	
	/** the alpha level of the displayed result masks */
	private static final int overAlpha=127;
	/** the master panel incorporating this image panel */
	private KClassifyPanel classifyPanel;

	/** default constructor */
	public KImageClassifyPanel(KClassifyPanel classifyPanel){
		super();
		this.classifyPanel=classifyPanel;		
	}
	/**
	 * Paints the image panel. First {@link KImagePanel#paintComponent(Graphics)
	 * draw the actual image}. If there is no underlying image, display
	 * nothing and return. Otherwise, for each of the phenotypes being classified,
	 * {@link GemIdentTools.Matrices.BoolMatrix#IllustrateAsMask(BufferedImage image,Color color)
	 * create an overlay mask} and draw it on top of the underlying image.
	 */
	public void paintComponent(Graphics g){
		super.paintComponent(g); //first draw the actual image
		// quit if there's no image to load...
		if ( super.displayImage == null ){ 
			g.drawImage(null,0,0,getWidth(),getHeight(),null);
			return;		
		}
		
		try {	
			if (classifyPanel.is != null){
				for (String phenotype:classifyPanel.is.keySet()){
					BufferedImage overDisplay=new BufferedImage(displayImage.getWidth(),displayImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
					Color color=Run.it.getPhenotype(phenotype).getDisplayColorWithAlpha(overAlpha);
					
					BoolMatrix B=classifyPanel.is.get(phenotype);
					if (B != null){
						B.IllustrateAsMask(overDisplay,color);
						g.drawImage(overDisplay,x_0,y_0,i_width,i_height,null);//draw on top of original image
					}
				}
			}
		} catch (Exception e){}
		UpdateMagnifier(getMousePosition());
	}
}