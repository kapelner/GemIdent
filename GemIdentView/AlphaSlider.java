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
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import GemIdentCentroidFinding.PostProcess;
import GemIdentClassificationEngine.Classify;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentModel.Phenotype;
import GemIdentTools.IOTools;
import GemIdentTools.Geometry.Solids;

/**
 * Uses a JSlider to control the opacity
 * of an image mask
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class AlphaSlider extends JSlider{
	
	/** initial alpha level {0,...,255} for an is-pixel image mask */
	private static final int INITIAL_ALPHA_IS=50;
	/** initial alpha level {0,...,255} for an is-centroid image mask */
	private static final int INITIAL_ALPHA_ISCENTROID=255;
	/** the 32-bit ARGB image mask after it is processed to use the appropriate color and support alpha blending */
	private BufferedImage I;
	/** the phenotype the mask is displaying data for */
	private Phenotype phenotype;
	/** is the image mask represent centroids? or pixels? */
	private boolean isCentroid;
	/** the imagePanel to display the mask over */
	private KImagePanel imagePanel;
	/** a copy of the current AlphaSlider object for reference in anonymous inner classes */
	private AlphaSlider that;
	protected int current_value;

	/** 
	 * initializes the slider specific to whether the mask is for pixels or centroids 
	 * 
	 * @param phenotype			the phenotype that the mask represents
	 * @param isCentroid		whether or not it's a mask for centroids or pixels
	 * @param imagePanel		the image panel to display the mask over
	 * @param values			a hash that maps the phenotype name to alpha level
	 */
	public AlphaSlider(final Phenotype phenotype,boolean isCentroid,final KImagePanel imagePanel,final HashMap<String,Integer> values){
		super(JSlider.HORIZONTAL,1,255,1); //don't let it begin at zero - no way to distinguish if it's a + or - point
		this.imagePanel=imagePanel;
		that=this;
		
		Integer val=values.get(phenotype.getName());
		if (val == null)
			if (isCentroid){
				values.put(phenotype.getName(),INITIAL_ALPHA_ISCENTROID);
				super.setValue(INITIAL_ALPHA_ISCENTROID);
			}
			else {
				values.put(phenotype.getName(),INITIAL_ALPHA_IS);
				super.setValue(INITIAL_ALPHA_IS);
			}
		else
			super.setValue(val);
		
		current_value = super.getValue();

		this.phenotype=phenotype;
		this.isCentroid=isCentroid;
		if (isCentroid)
			SetUpIsCentroid();
		else 
			SetUpIs();
		addChangeListener(
			new ChangeListener(){	
				public void stateChanged(ChangeEvent e){
					current_value = ((JSlider)e.getSource()).getValue();
					ChangeAlpha(current_value);
					values.put(phenotype.getName(), current_value);
			    	//set imagePanel to take focus next time
			    	imagePanel.ifMouseEntersTakeFocus();					
				}
			}
		);	
		imagePanel.repaint();
	}
	/** sets up a mask for an is-pixel image - loads it from the hard disk, creates the 32-bit alpha image, and displays it */
	private void SetUpIs(){
//		String name=imagePanel.displayImage.getFilename();
//		String name2=phenotype.getName();
		String isName=Classify.GetIsName(imagePanel.displayImage.getFilename(),phenotype.getName());
		if (IOTools.FileExists(isName)){
			BufferedImage raw=ImageAndScoresBank.getOrAddIs(isName,null);
			I=ProcessRawIs(raw);
			((KImageTrainPanel)imagePanel).addIsOverImage(I);
		}
		else
			setEnabled(false);
	}
	/** sets up a mask for an is-centroid image - loads it from the hard disk, creates the 32-bit alpha image, and displays it */
	private void SetUpIsCentroid(){
		String isCentroidName=PostProcess.GetIsCentroidName(imagePanel.displayImage.getFilename(),phenotype.getName());
		if (IOTools.FileExists(isCentroidName)){
			BufferedImage raw=ImageAndScoresBank.getOrAddIs(isCentroidName,null);
			I=ProcessRawIsCentroid(raw);			
			((KImageTrainPanel)imagePanel).addIsCentroidOverImage(I);
		}
		else
			setEnabled(false);
	}
	/**
	 * Changes the alpha of the mask
	 * 
	 * @param alpha		the new alpha to set every positive pixel to
	 */
	private void ChangeAlpha(int alpha){
		if (I != null){
			if (isCentroid){
				for (int i=0;i<I.getWidth();i++){
					for (int j=0;j<I.getHeight();j++){
						Color prev=new Color(I.getRGB(i,j),true);
						if (!prev.equals(new Color(0,0,0,0))){
							Color present=new Color(prev.getRed(),prev.getGreen(),prev.getBlue(),alpha);
							try {I.setRGB(i,j,present.getRGB());} catch (Exception e){}
						}
					}
				}
			}
			else {
				Color present=phenotype.getDisplayColorWithAlpha(alpha);
				for (int i=0;i<I.getWidth();i++){
					for (int j=0;j<I.getHeight();j++){
						Color prev=new Color(I.getRGB(i,j),true);
						if (!prev.equals(new Color(0,0,0,0))){
							try {I.setRGB(i,j,present.getRGB());} catch (Exception e){}
						}
					}
				}
			}
			//let's do an update
			imagePanel.repaint();
			imagePanel.repaintMagnifier();
		}
	}
	
	public void ChangeAlphaToZero(){
		ChangeAlpha(0);
	}
	
	public void ChangeAlphaBackToPreviousValue(){
		ChangeAlpha(current_value);
	}
	
	/**
	 * Processes the classification mask from a 1-bit binary 
	 * +/- matrix to a 32-bit colored, alpha-blended mask by
	 * drawing a star for each positive point
	 * 
	 * @param raw		the 1-bit binary matrix where positives are centroids
	 * @return			the 32-bit ARGB image with a colored star in the position of every positive
	 */
	private BufferedImage ProcessRawIsCentroid(BufferedImage raw) {
		int rows=raw.getWidth();
		int cols=raw.getHeight();
		BufferedImage I=new BufferedImage(rows,cols,BufferedImage.TYPE_INT_ARGB);
		Color center=new Color(1,1,1,getValue());
		Color around=phenotype.getDisplayColorWithAlpha(getValue());
		for (int i=0;i<rows;i++)
			for (int j=0;j<cols;j++)
				if (raw.getRGB(i,j) != (Color.BLACK).getRGB()){
					for (Point t:Solids.GetPointsInSolidUsingCenter(3,new Point(i,j)))
						try {I.setRGB(t.x,t.y,center.getRGB());} catch (Exception e){}
					for (Point t:Solids.GetPointsInSolidUsingCenter(2,new Point(i,j)))
						try {I.setRGB(t.x,t.y,around.getRGB());} catch (Exception e){}
					try {I.setRGB(i,j,center.getRGB());} catch (Exception e){}
				}
		return I;
	}
	/**
	 * Processes the classification mask from a 1-bit binary 
	 * +/- matrix to a 32-bit colored, alpha-blended mask by
	 * coloring each positive point
	 * 
	 * @param raw		the 1-bit binary matrix where positives are centroids
	 * @return			the 32-bit ARGB image with a colored pixel in the position of every positive
	 */
	private BufferedImage ProcessRawIs(BufferedImage raw) {
		int rows=raw.getWidth();
		int cols=raw.getHeight();
		BufferedImage I=new BufferedImage(rows,cols,BufferedImage.TYPE_INT_ARGB);
		Color around=phenotype.getDisplayColorWithAlpha(getValue());
		for (int i=0;i<rows;i++)
			for (int j=0;j<cols;j++)
				if (raw.getRGB(i,j) != (Color.BLACK).getRGB())
					I.setRGB(i,j,around.getRGB());
		return I;
	}
	/** the alpha change when the user increases or decreases the alpha-blending using the keyboard shortcuts */
	private static final int AlphaChange=40;
	/**
	 * Adds the increase / decrease alpha function to a globally mapped key
	 * via use of the {@link ActionWrap AbstractAction wrapper}
	 * 
	 * @param plus		the actionwrapper for the increase alpha function
	 * @param minus		the actionwrapper for the decrease alpha function
	 */
	@SuppressWarnings("serial")
	public void addGlobalKeyListeners(ActionWrap plus,ActionWrap minus){
		//pointer isn't passed by reference
		plus.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					if (that.isEnabled()){
						that.setValue(that.getValue()+AlphaChange);
						imagePanel.repaintMagnifier();
					}
				}
			}
		);
		minus.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					if (that.isEnabled()){
						that.setValue(that.getValue()-AlphaChange);
						imagePanel.repaintMagnifier();
					}
				}
			}
		);
	}
}