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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import GemIdentImageSets.NonGlobalImageSet;
import GemIdentModel.Stain;
import GemIdentModel.TrainSuperclass;
import GemIdentOperations.Run;
import GemIdentTools.Thumbnails;

/**
 * An extension of {@link KTrainPanel the generic training panel}
 * with specific functionality for color training. See step 1 in the 
 * Algorithm section of the IEEE paper for this
 * panel's purpose and see fig. 3 in the for an example
 * of the user training. Also, for a more detailed discussion on the
 * purpose of color training, see section 3.1 in the manual.
 * 
 * @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class KColorTrainPanel extends KTrainPanel {

	/** Deletes a training color */
	protected void DeleteInfo(String name){
		Run.it.getUserColorsImageset().DeleteStain(name);
		super.DeleteInfo(name);
	}
	/** resets the panel */
	protected void ResetImagePanelAndAddListeners() {
		imagePanel=new KImageColorTrainPanel(this);
		super.ResetImagePanelAndAddListeners();
	}
	/** 
	 * sets up the Add / Delete color buttons and connects the training helper button.
	 * The addition and deletion of colors is dicussed in section 3.1.1 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	protected void CreateWest(){
		super.CreateWest();
		addClassInfoButton.setText("+ Color");
		deleteClassInfoButton.setText("- Color");
		TrainHelperButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (!(Run.it.imageset instanceof NonGlobalImageSet)){
						Run.it.imageset.CreateHTMLForComposite(Thumbnails.tWidth,Thumbnails.tHeight);					
					}
				}
			}
		);
	}
	
	/** spawns a dialog where user selects which image to delete, then deletes it */
	protected Bundle SpawnRemoveImageDialog() {
		final Bundle bundle=super.SpawnRemoveImageDialog();
	
		bundle.remove.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (Run.it.getUserColorsImageset().RemoveImageFromStainSet(bundle.list.getSelectedItem())){
						thumbnailPane.RemoveThumbnail(bundle.list.getSelectedItem());
						imagePanel.setDisplayImage(null);
						imagePanel.repaint();
					}					
				}
			}
		);
		return bundle;
	}
	/** adds a new color to training panel */
	protected void AddInfo(){
		AddInfo("Color_" + (browser.getNumInfos() + 1));
	}
	/** adds a color with a specific name */
	protected void AddInfo(String text){
		super.AddInfo(text);
		KStainInfo info = new KStainInfo(((KImageTrainPanel)this.imagePanel),text,this);
		Run.it.getUserColorsImageset().AddStain(info.getStain());
		this.browser.addInfoClass(info);
	}	
	/** adds a color based the {@link GemIdentModel.TrainSuperclass underlying model} of that color */
	protected void AddInfo(TrainSuperclass trainer){
		KStainInfo info=new KStainInfo(((KImageTrainPanel)imagePanel),(Stain)trainer,this);
		browser.addInfoClass(info);
	}
	/** Deletes a color from the training panel */
	protected void DeleteInfo(int type,String name){
		Run.it.getUserColorsImageset().DeleteStain(name);	
		super.DeleteInfo(name);
	}
	/** Creates a delete info dialog with the appropriate title for deleting a color */
	protected void SpawnDeleteInfoDialog(){
		super.SpawnDeleteInfoDialog("Color", browser.getClassInfoNames());
	}
	/** When the user beings a new project, four generic colors are added to the panel */
	private static final int NumColorInfosToPopulateInitially = 2;
	public void DefaultPopulateBrowser(){
		for (int i = 0; i < NumColorInfosToPopulateInitially; i++){
			AddInfo();
		}
		CreateListeners();	
	}
	/** When the user opens a saved project, the panel is populated from the stain objects from the gem file */
	public void PopulateFromOpen(){
		for (Stain stain:Run.it.getUserColorsImageset().getStainObjects())
			AddInfo(stain);
		CreateListeners();
		for (String filename:Run.it.getUserColorsImageset().getStainTrainingImages())
			thumbnailPane.addThumbnail(new KThumbnail(this,filename),false);
		super.PopulateFromOpen();
	}
	/** create an add color dialog with the appropriate title for adding colors */
	protected void SpawnAddInfoDialog(){
		super.SpawnAddInfoDialog("color");
	}
	/** creates the settings box and appropriately titles the slider object */
	protected void buildSettingsBoxes(){
		super.buildSettingsBoxes("Alpha:");
	}
}