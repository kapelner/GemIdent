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
import java.io.File;

import javax.swing.*;
import javax.swing.event.*;

import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentModel.*;
import GemIdentOperations.*;
import GemIdentTools.*;

/**
 * A type of {@link KClassInfo class info} that's specially designed
 * to represent a {@link GemIdentModel.Stain color} and provide
 * support for {@link GemIdentOperations.StainMaker computing
 * Mahalanobis cubes}
 * 
 * @author Adam Kapelner
 *
 */
@SuppressWarnings("serial")
public class KStainInfo extends KClassInfo{
	
	/** the user chooses to {@link GemIdentOperations.StainMaker compute a Mahalanobis cube} for this color */
	private JButton makeColorInfo;
	/** the progress bar that updates the user of the cube's construction */
	private JProgressBar progress;

	/**
	 * Instantiates a new color info
	 * 
	 * @param imageTrainPanel		the image panel the info is connected to
	 * @param name					the name of the new info
	 * @param owner					simplifies selection of infos
	 */
	public KStainInfo(KImageTrainPanel imageTrainPanel,String name,SelectionEmulator owner ) {
		super(imageTrainPanel,owner);
		
		trainer=new Stain();
		trainer.setName(name);
		nameField.setText(name);
		
		EditWestBox();
		SetUpListeners();
	}
	/**
	 * Instantiates a new color info from an existing {@link GemIdentModel.Stain stain}
	 * 
	 * @param imageTrainPanel		the image panel the info is connected to
	 * @param trainer				the {@link GemIdentModel.Stain stain} model to build the info according to
	 * @param owner					simplifies selection of infos
	 */
	public KStainInfo(KImageTrainPanel imageTrainPanel,Stain trainer,SelectionEmulator owner ){
		super(imageTrainPanel,owner);
		
		this.trainer=trainer;
		this.nameField.setText(trainer.getName());
		this.rminSpinner.setValue(trainer.getRmin());
		colorChooseButton.repaint();
		EditWestBox();
		SetUpListeners();
		UnSelect();
	}


	/** appends the color-specific controls to the Western region of the class info */
	protected void EditWestBox() {
		Box makeColorBox = Box.createHorizontalBox();
		
		makeColorInfo = new JButton("Compute");
		
		makeColorBox.add(makeColorInfo);
		
		west_side.add(makeColorBox);
		west_side.add(Box.createVerticalStrut(3));
	}
	/** 
	 * sets up the listeners for the components that change the underlying model.
	 * For discussion on the creation of color info via the compute button, consult
	 * section 3.1.4 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	protected void SetUpListeners() {
		nameField.addKeyListener(
			new KeyListener(){
				public void keyPressed(KeyEvent e){}
				public void keyReleased(KeyEvent e){
					String previous=trainer.getName();
					String present=nameField.getText().trim();
					if (!previous.equals(present)){
						Run.it.getUserColorsImageset().DeleteStain(previous);
						trainer.setName(present);
						Run.it.getUserColorsImageset().AddStain((Stain)trainer);
					}
					
				}
				public void keyTyped(KeyEvent e){}
			}
		);
		rminSpinner.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					trainer.setRmin((Integer)rminSpinner.getValue());	
				}
			}
		); 
		
		makeColorInfo.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (IOTools.FileExists(StainMaker.colorSubDir+File.separator+trainer.getName())){
						int result=JOptionPane.showConfirmDialog(Run.it.getGUI(),
								"Are you sure you want to recompute the color information and replace the pre-existing file?",
								"Warning",
								JOptionPane.YES_NO_OPTION);
						
						if ( result == JOptionPane.NO_OPTION ) return;				
					}
					if (trainer.getTotalPoints() < ImageSetInterfaceWithUserColors.MinimumColorPointsNeeded)
						JOptionPane.showMessageDialog(Run.it.getGUI(),"Need at least " + ImageSetInterfaceWithUserColors.MinimumColorPointsNeeded + " training points");
					else
						MakeCube();	
				}
				
				private void MakeCube(){
					progress=new JProgressBar(0,100);
					Run.it.getUserColorsImageset().addToCubeComputePool(((Stain)trainer).getStainMaker(progress,null));					
					MakeProgressBar();					
				}
			}
		);
	}
	/** when the user clicks "Compute," the {@link #progress progress bar} that reflects the construction
	 * of the cube is added to the info. The other controls are disabled at this time.
	 * When the computation is done, the bar is removed.
	 */
	private void MakeProgressBar() {
		progress.setBorderPainted(true); 
		progress.setStringPainted(true); 
		//lets first disable all other components:		
		nameField.setEnabled(false);
		colorChooseButton.setEnabled(false);
		rminSpinner.setEnabled(false);
		identifierButton.setEnabled(false);
		//add listener
		progress.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					if(progress.getValue() == 100)
						DestroyProgressBar();
				}
			}			
		);
		
		Dimension button_size = this.makeColorInfo.getSize();
		Insets button_insets = this.makeColorInfo.getMargin();
		
		button_size.width -= button_insets.left + button_insets.right;
		button_size.height -= button_insets.top + button_insets.bottom;
		
		this.progress.setPreferredSize(button_size);
		makeColorInfo.add(progress);
		makeColorInfo.setEnabled(false);
		repaint();
		Run.it.FrameRepaint();
	}	
	/** removes the {@link #progress progress bar} and reenables the other controls */
	protected void DestroyProgressBar() {
		makeColorInfo.remove(progress);
		makeColorInfo.setEnabled(true);
//		setPreferredSize(new Dimension(classInfoSize.width,classInfoSize.height));
		repaint();
		//re-enable components
		nameField.setEnabled(true); //we don't want this one re-enabled
		colorChooseButton.setEnabled(true);
		rminSpinner.setEnabled(true);
		identifierButton.setEnabled(true);
		Run.it.FrameRepaint();
	}	
	/** gets the underlying Stain */
	public Stain getStain() {
		return ((Stain)trainer);
	}
	/** all color info's identifier images are {@link KClassInfo#buildExampleImageFromColors() built from colors} */	
	public void buildExampleImage() {
		buildExampleImageFromColors();
	}
}