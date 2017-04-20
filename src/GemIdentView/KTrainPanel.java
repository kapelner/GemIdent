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
import java.awt.List;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import GemIdentOperations.*;
import GemIdentTools.*;
import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.Nuance.NuanceSubImage;
import GemIdentModel.*;

/**
 * The abstract class responsible for implementing both the {@link 
 * KColorTrainPanel color training panel} and the {@link
 * KPhenotypeTrainPanel phenotype training panel} common
 * internals such as integrating the color / phenotype {@link 
 * KClassInfoBrowser browser}, the {@link KThumbnailPane thumbnail 
 * panel}, the {@link KImagePanel image panel}, the {@link KMagnify
 * image magnifier}, training helpers, and the data model together
 * as one coherent unit. For more information about training in 
 * <b>GemIdent</b>, see section 3 in the manual.
 * 
 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
 * 
 * @author Adam Kapelner and Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public abstract class KTrainPanel extends KPanel implements TrainClickListener,SelectionEmulator{

	/** When the user deletes training points, all points within this radius pixels will be removed */
	public static final int EraseRadius = 5;

	/** the user wishes to mark positive training points for a particular color or phenotype */
	protected JToggleButton markPixelButton;
	/** the user wishes to delete positive training points for a particular color or phenotype */
	protected JToggleButton deletePixelButton;
	/** the user wishes to add a color or phenotype */
	protected JButton addClassInfoButton;
	/** the user wishes to delete a color or phenotype */
	protected JButton deleteClassInfoButton;
	/** the user wishes to add a training image */
	protected JButton AddImageButton;
	/** the user wishes to remove a training image */
	protected JButton RemoveImageButton;
	/** the uses wishes to initialize the training helper (different for color training and phenotype training) */
	protected JButton TrainHelperButton;
	/** the uses wishes to initialize the training sophisticated helper (only available for NuanceImage sets) */
	protected JButton sophisticatedTrainHelperButton;	
	/** whether the user is currently marking new points or deleting old points */
	protected boolean markOrDelete;
	/** the browser that holds all the colors or phenotypes */
	protected KClassInfoBrowser browser;
	/** the pane that hold all the thumbnails of the training set images */
	protected KThumbnailPane thumbnailPane;
	/** the slider that controls the alpha of the circles that appear around the training points */
	protected JSlider helperSlider;
	/** the display name of the {@link #helperSlider helper slider} component */
	protected JLabel helperSliderLabel;
	/** whether or not to display the training points on the panel */
	protected JCheckBox seeTrainCheck;
	/** the box that holds the browser, and the setting for the panel below it in the Western region */
	protected Box westBox;
	/** the box that holds the settings below the browser */
	protected Container settings_box;
	/** the text that displays the position of the cursor on the image panel */
	protected JLabel locationLabel;
	/** the color or phenotype currently being trained */
	protected KClassInfo activeInfo;
	/** the panel's internal mapping between keystrokes and action names */
	protected InputMap input_map;
	/** the panel's internal mapping between action names and actions */
	protected ActionMap action_map;

	//the keystrokes and actions for all the keyboard shortcuts
	private KeyStroke seeTilde;
	private KeyStroke classUpKey;
	private KeyStroke classDownKey;
	private KeyStroke class1;
	private KeyStroke class2;
	private KeyStroke class3;
	private KeyStroke class4;
	private KeyStroke class5;
	private KeyStroke class6;
	private KeyStroke class7;
	private KeyStroke class8;
	private KeyStroke class9;
	private KeyStroke class10;
	private ActionWrap seeAction;
	private ActionWrap classUpAction;
	private ActionWrap classDownAction;
	private ActionWrap class1A;
	private ActionWrap class2A;
	private ActionWrap class3A;
	private ActionWrap class4A;
	private ActionWrap class5A;
	private ActionWrap class6A;
	private ActionWrap class7A;
	private ActionWrap class8A;
	private ActionWrap class9A;
	private ActionWrap class10A;

	protected KTrainPanel that; //I'll use this a couple times

	

	/** gets the input / action maps, resets the image panel, populates the regions of the panel, and adds function listeners */
	public KTrainPanel(){
		super();
		this.that = this;
		
		input_map=getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		action_map=getActionMap();
		
		ResetImagePanelAndAddListeners();
		markOrDelete=true; //default is true
		
		AppendEast();
		CreateWest();
		EditSouth();

		AddFnListeners();
	}
	/** initializes the keystrokes and actions, and wires them in the {@link #input_map input} and {@link #action_map action} maps */
	protected void AddFnListeners(){
		seeTilde=KeyStroke.getKeyStroke('`');
		classUpKey=KeyStroke.getKeyStroke('[');
		classDownKey=KeyStroke.getKeyStroke(']');
		class1=KeyStroke.getKeyStroke('1');
		class2=KeyStroke.getKeyStroke('2');
		class3=KeyStroke.getKeyStroke('3');
		class4=KeyStroke.getKeyStroke('4');
		class5=KeyStroke.getKeyStroke('5');
		class6=KeyStroke.getKeyStroke('6');
		class7=KeyStroke.getKeyStroke('7');
		class8=KeyStroke.getKeyStroke('8');
		class9=KeyStroke.getKeyStroke('9');
		class10=KeyStroke.getKeyStroke('0');
		seeAction=new ActionWrap();
		classUpAction=new ActionWrap();
		classDownAction=new ActionWrap();
		class1A=new ActionWrap();
		class2A=new ActionWrap();
		class3A=new ActionWrap();
		class4A=new ActionWrap();
		class5A=new ActionWrap();
		class6A=new ActionWrap();
		class7A=new ActionWrap();
		class8A=new ActionWrap();
		class9A=new ActionWrap();
		class10A=new ActionWrap();
		input_map.put(seeTilde,"seeAction");
		input_map.put(classUpKey,"classUpAction");
		input_map.put(classDownKey,"classDownAction");
		input_map.put(class1,"class1");
		input_map.put(class2,"class2");
		input_map.put(class3,"class3");
		input_map.put(class4,"class4");
		input_map.put(class5,"class5");
		input_map.put(class6,"class6");
		input_map.put(class7,"class7");
		input_map.put(class8,"class8");
		input_map.put(class9,"class9");
		input_map.put(class10,"class10");

		seeAction.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					seeTrainCheck.doClick();
				}
			}
		);
		classUpAction.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					for (int i=0;i<list.size();i++)
						if (list.get(i) == activeInfo)
							if (i != 0){ //ie the last one
								selectElement(list.get(i-1));
								break;
							}
				}
			}
		);
		classDownAction.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					for (int i=0;i<list.size();i++)
						if (list.get(i) == activeInfo){
							if (i != list.size()-1){ //ie the last one
								selectElement(list.get(i+1));
								break;
							}
						}
				}
			}
		);
		class1A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 1)
						selectElement(list.get(0));
				}
			}
		);
		class2A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 2)
						selectElement(list.get(1));
				}
			}
		);
		class3A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 3)
						selectElement(list.get(2));
				}
			}
		);
		class4A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 4)
						selectElement(list.get(3));
				}
			}
		);
		class5A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 5)
						selectElement(list.get(4));
				}
			}
		);
		class6A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 6)
						selectElement(list.get(5));
				}
			}
		);
		class7A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 7)
						selectElement(list.get(6));
				}
			}
		);
		class8A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 8)
						selectElement(list.get(7));
				}
			}
		);
		class9A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 9)
						selectElement(list.get(8));
				}
			}
		);
		class10A.setAction(
			new AbstractAction(){
				public void actionPerformed( ActionEvent e ) {
					ArrayList<KClassInfo> list = browser.getClassInfosInOrder();
					if (list.size() >= 10)
						selectElement(list.get(9));
				}
			}
		);

		action_map.put("seeAction",seeAction.getAction());
		action_map.put("classUpAction",classUpAction.getAction());
		action_map.put("classDownAction",classDownAction.getAction());
		action_map.put("class1",class1A.getAction());
		action_map.put("class2",class2A.getAction());
		action_map.put("class3",class3A.getAction());
		action_map.put("class4",class4A.getAction());
		action_map.put("class5",class5A.getAction());
		action_map.put("class6",class6A.getAction());
		action_map.put("class7",class7A.getAction());
		action_map.put("class8",class8A.getAction());
		action_map.put("class9",class9A.getAction());
		action_map.put("class10",class10A.getAction());
	}
	/** gets the model behind the {@link #activeInfo currently trained color or phenotype} */
	public TrainSuperclass getActiveTrainer(){
		if (activeInfo == null)
			return null;
		else
			return activeInfo.getTrainer();
	}
	/** resets image panel and then adds the proper listener to it */
	protected void ResetImagePanelAndAddListeners() {
		setImagePanel(imagePanel);
		((KImageTrainPanel)imagePanel).AddTrainClickListener(this);
	}
	
	public DataImage getActiveImageAsDataImage(){
		return imagePanel.displayImage;
	}
	/**
	 * creates the Western panel adding the genering add and delete of colors or phenotypes,
	 * the browser, the add / remove / helper image buttons, the helper slider, various options,
	 * wires them using listeners
	 */
	protected void CreateWest() {
		addClassInfoButton=new JButton();
		addClassInfoButton.setMnemonic(KeyEvent.VK_A);
		deleteClassInfoButton=new JButton();
		deleteClassInfoButton.setMnemonic(KeyEvent.VK_D);
		Container topButtons=Box.createHorizontalBox();
		topButtons.add(addClassInfoButton);
		topButtons.add(deleteClassInfoButton);
		
		helperSlider = new JSlider(JSlider.HORIZONTAL,0,80,0);
		helperSlider.setPreferredSize(new Dimension(80,this.helperSlider.getHeight()));
		helperSlider.addChangeListener(
			new ChangeListener(){	
				public void stateChanged( ChangeEvent e ) {
					double percent = (double)helperSlider.getValue()/helperSlider.getMaximum();
					int alpha = (int)(percent*255);
					
					((KImageTrainPanel)imagePanel).setAlphaLevelForTrainPoints(alpha);
					((KImageTrainPanel)imagePanel).setRedrawImage(true);
					imagePanel.repaint();
				}
			}
		);
		AddImageButton = new JButton();
		AddImageButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("add.image.png")));
		AddImageButton.setDisabledIcon(new ImageIcon(IOTools.OpenSystemImage("add.image.disabled.png")));
		AddImageButton.setMnemonic(KeyEvent.VK_I);
		AddImageButton.setToolTipText("Add a new image");
		AddImageButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					AddNewThumbnail(null);
				}
			}
		);
		RemoveImageButton = new JButton();
		RemoveImageButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("remove.image.gif")));
		RemoveImageButton.setDisabledIcon(new ImageIcon(IOTools.OpenSystemImage("remove.image.disabled.png")));
		RemoveImageButton.setToolTipText("Remove an existing image");
		RemoveImageButton.setEnabled(false);
		RemoveImageButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					SpawnRemoveImageDialog();
				}
			}
		);
		TrainHelperButton = new JButton();
		TrainHelperButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("image.help.png")));
		TrainHelperButton.setDisabledIcon(new ImageIcon(IOTools.OpenSystemImage("image.help.disabled.png")));
		TrainHelperButton.setToolTipText("Activate the training helper");
//		TrainHelperButton.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					if (!(Run.it.imageset instanceof NonGlobalImageSet))
//						JOptionPane.showMessageDialog(Run.it.getGUI(),"The train helper is only available for Bliss image sets");
//				}
//			}
//		);
		sophisticatedTrainHelperButton = new JButton();
		sophisticatedTrainHelperButton.setVisible(false);
		sophisticatedTrainHelperButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("magnify.jpg")));
		sophisticatedTrainHelperButton.setToolTipText("Activate the sophisticated training helper");
		sophisticatedTrainHelperButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					Run.it.imageset.spawnSophisticatedHelper();
				}
			}
		);		
		seeTrainCheck = new JCheckBox(); 
		seeTrainCheck.setSelected(true);
		seeTrainCheck.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
				    if (e.getStateChange() == ItemEvent.DESELECTED)
				    	DisableTrainPoints();
				    else 
				    	EnableTrainPoints();
				}
			}
		);
		westBox=Box.createVerticalBox();
		
		browser=new KClassInfoBrowser();
		westBox.add(topButtons);
		Box browser_box = Box.createVerticalBox();
		browser_box.add(browser.getScrollPane());
		westBox.add(browser_box);
		add(westBox,BorderLayout.WEST);
		
		buildSettingsBoxes();
	}
	/** the training points will now be visible */
	public void EnableTrainPoints() {
    	((KImageTrainPanel)imagePanel).setSeeTrainPoints(true);
    	helperSlider.setEnabled(true);	
    	seeTrainCheck.setSelected(true);
	}
	/** the training points will no longer be visible */
	public void DisableTrainPoints() {
    	((KImageTrainPanel)imagePanel).setSeeTrainPoints(false);
    	helperSlider.setEnabled(false);
    	seeTrainCheck.setSelected(false);
	}
	/** the user just added a new training image - display it on the image panel */
	public KThumbnail AddNewThumbnail(final String filename){
		Run.it.imageset.LOG_AddToHistory("added " + filename + " to training set");
		synchronized (thumbnailPane){
			KThumbnail thumbnail = thumbnailPane.ChooseAndAddThumbnail(filename);		
			if (thumbnail != null){
				final String finalfilename = thumbnail.getFilename();
				DataImage displayImage = ImageAndScoresBank.getOrAddDataImage(finalfilename);
				new Thread(){
					public void run(){
						setPriority(Thread.MIN_PRIORITY);
						ImageAndScoresBank.getOrAddSuperImage(finalfilename);
					}
				}.start();
				if (displayImage instanceof NuanceSubImage) //need to do other special stuff to display it properly
					((NuanceSubImage)displayImage).BuildDisplayAndPixelDistrs();
				imagePanel.setDisplayImage(displayImage);
				RemoveImageButton.setEnabled(true);
				Run.it.GUIsetDirty(true);
			}
			if (Thumbnails.getTotNumImageFiles(thumbnailPane.getThumbnailNames()) == 0)
				AddImageButton.setEnabled(false);
			return thumbnail;
		}
	}
	
	/** a hack to be able to return two things from a function - a list and a button */
	protected class Bundle {
		public JButton remove;
		public List list;
		public Bundle(JButton remove,List list){
			this.remove = remove;
			this.list = list;
		}
	}
	/** Spawns a dialog that allows the user to remove a color or phenotype */
	protected Bundle SpawnRemoveImageDialog() {
		final JDialog removeDialog=new JDialog();
		removeDialog.setTitle("Remove Image");
		
		final List list=new List(3,false);
		for (String filename:thumbnailPane.getThumbnailNames())
			list.add(filename);
		
		final JButton remove=new JButton("Remove");
		remove.setEnabled(false);
		
		JButton cancel=new JButton("Cancel");
		
		list.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e){
					remove.setEnabled(true);
				}
			}
		);		
		remove.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (thumbnailPane.getNumThumbnails() == 0)
						RemoveImageButton.setEnabled(false);
					if (Thumbnails.getTotNumImageFiles(thumbnailPane.getThumbnailNames()) > 0)
						AddImageButton.setEnabled(true);
					Run.it.GUIsetDirty(true);
					removeDialog.dispose();
					browser.repaint();
				}
			}
		);
		cancel.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					removeDialog.dispose();
				}
			}
		);
		removeDialog.setLayout(new BorderLayout());
		removeDialog.add(list,BorderLayout.NORTH);
		Box buttons=Box.createHorizontalBox();
		buttons.add(remove);
		buttons.add(cancel);
		removeDialog.add(buttons,BorderLayout.SOUTH);
		
		Point loc=RemoveImageButton.getLocation();
		Point origin=Run.it.getGuiLoc();
		loc.translate(origin.x,origin.y);
		removeDialog.setLocation(loc);
		removeDialog.setVisible(true);
		removeDialog.pack();
		return new Bundle(remove,list);
	}	
	/** the default text displayed in the locations sensor when the mouse is not within the image panel */
	private static final String LocationLabelDefaultText="(x,y)";
	/** given the mouse location, updates the location sensor text */
	protected void UpdateLocationLabel(Point local){
		String loclabel;
		if (local == null)
			loclabel=LocationLabelDefaultText;
		else
			loclabel="("+local.x+","+local.y+")";
		locationLabel.setText(loclabel);
	}
	/** adds a color or phenotype to the {@link KClassInfoBrowser browser} - to be implemented by daughter classes */
	protected abstract void AddInfo();
	/** checks the name of the newly added color or phenotype */
	protected void AddInfo(String text){
		if ( browser.isInBrowser(text) ) {
			JOptionPane.showMessageDialog(this,"Duplicate Name Exists");
			return;
		}
		deleteClassInfoButton.setEnabled(true);
	}
	/** adds a color or phenotype from the underlying data model - to be implemented by daughter classes */
	protected abstract void AddInfo(TrainSuperclass trainer);
	/** deletes an info and revalidates the delete class button */
	protected void DeleteInfo(String name){
		browser.deleteInfoClass(name);	
		if (browser.getNumInfos() == 0 
				|| 
					(
						browser.getNumInfos() == 1 
						&& 
						browser.isInBrowser(Phenotype.NON_NAME)
					) )
			deleteClassInfoButton.setEnabled(false);
		imagePanel.repaint();
	}
	/** adds the mark / delete button box and the location sensor */
	protected void AppendEast() {
		super.AppendEast();
		
		GUIFrame markDeletePanel=new GUIFrame("Mark / Delete Training Point");
		Box markDeleteBox=Box.createHorizontalBox();
	
		ButtonGroup group=new ButtonGroup();
		markPixelButton=new JToggleButton();
		markPixelButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("cross.gif")));
		deletePixelButton=new JToggleButton();
		deletePixelButton.setIcon(new ImageIcon(IOTools.OpenSystemImage("ex.gif")));
		group.add(markPixelButton);
		group.add(deletePixelButton);
		group.setSelected(markPixelButton.getModel(),true);
	
		markDeleteBox.add(markPixelButton);
		markDeleteBox.add(Box.createHorizontalStrut(3));
		markDeleteBox.add(deletePixelButton);
		Box locBox=Box.createVerticalBox();

		locBox.add(new JLabel("Location:",JLabel.LEFT));
		locationLabel=new JLabel(LocationLabelDefaultText,JLabel.LEFT);
		markDeleteBox.add(Box.createHorizontalStrut(5));
		locBox.add(locationLabel);
		markDeleteBox.add(locBox);
		
		markDeletePanel.add(markDeleteBox);		
		super.appendToEast(markDeletePanel);
	}
	/** spawns a dialog to add a color or phenotype - to be implemented by daughter classes that then call {@link #SpawnAddInfoDialog(String) super's implementation} */
	protected abstract void SpawnAddInfoDialog();
	/** spawns the dialog to add a color or phenotype to the {@link KClassInfoBrowser browser} */
	protected void SpawnAddInfoDialog(String trainer){
		final JDialog addDialog=new JDialog();
		
		addDialog.setTitle("Enter New "+trainer+" Name:");
		
		final JTextField nameField=new JTextField();
		nameField.setPreferredSize(new Dimension(100,20));
		
		final JButton add=new JButton("Add "+trainer);
		add.setEnabled(false);
		
		JButton cancel=new JButton("Cancel");
		
		nameField.addKeyListener(
			new KeyListener(){
				public void keyPressed(KeyEvent e){
					String text=nameField.getText();
					if (e.getKeyCode() == KeyEvent.VK_ENTER && text != null && text.length() > 0){
						AddInfo(nameField.getText());
						addDialog.dispose();
						thumbnailPane.updateInfoLabelForAllThumbnails();
					}
				}
				public void keyReleased(KeyEvent e){
					String text=nameField.getText();
					text = text.replaceAll("\\s", "_");
					text = text.replaceAll("-", "_");

					int old_pos = nameField.getCaretPosition();
					if (!nameField.getText().equals(text)){
						nameField.setText(text);
						nameField.setCaretPosition(old_pos);
					}
					if (text == null || text.length() == 0)
						add.setEnabled(false);
					else
						add.setEnabled(true);
				}
				public void keyTyped(KeyEvent e){}
			}
		);
		add.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					AddInfo(nameField.getText());
					addDialog.dispose();
					thumbnailPane.updateInfoLabelForPresentPic();
				}
			}
		);
		cancel.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					addDialog.dispose();
				}
			}
		);
		addDialog.setLayout(new BorderLayout());
		addDialog.add(nameField,BorderLayout.NORTH);
		Box buttons=Box.createHorizontalBox();
		buttons.add(add);
		buttons.add(cancel);
		addDialog.add(buttons,BorderLayout.SOUTH);
		
		Point loc=addClassInfoButton.getLocation();
		Point origin=Run.it.getGuiLoc();
		loc.translate(origin.x,origin.y);
		addDialog.setLocation(loc);
		addDialog.setVisible(true);
		addDialog.pack();
	}
	/** spawns a dialog to delete a color or phenotype - to be implemented by daughter classes that then call {@link #SpawnDeleteInfoDialog() super's implementation} */
	protected abstract void SpawnDeleteInfoDialog();
	
	/** spawns the dialog to delete a color or phenotype to the {@link KClassInfoBrowser browser} */
	protected void SpawnDeleteInfoDialog(String trainerTitle, Collection<String> trainers){
		final JDialog deleteDialog=new JDialog();
		deleteDialog.setTitle("Which "+trainerTitle+" to Delete?");

		final List list=new List(browser.getNumInfos(),false);

		for (String name:trainers)
			list.add(name);

		final JButton delete=new JButton("Delete "+trainerTitle);
		delete.setEnabled(false);
		
		JButton cancel=new JButton("Cancel");		
		
		list.addItemListener(
			new ItemListener(){
				public void itemStateChanged(ItemEvent e) {
					delete.setEnabled(true);					
				}
			}
		);
		delete.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					DeleteInfo(list.getSelectedItem());
					deleteDialog.dispose();	
					thumbnailPane.updateInfoLabelForAllThumbnails();
				}
			}
		);
		cancel.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					deleteDialog.dispose();
				}
			}
		);
		
		deleteDialog.setLayout(new BorderLayout());
		deleteDialog.add(list,BorderLayout.NORTH);
		Box buttons=Box.createHorizontalBox();
		buttons.add(delete);
		buttons.add(cancel);
		deleteDialog.add(buttons,BorderLayout.SOUTH);
		
		Point loc=deleteClassInfoButton.getLocation();
		Point origin=Run.it.getGuiLoc();
		loc.translate(origin.x,origin.y);
		deleteDialog.setLocation(loc);
		deleteDialog.setVisible(true);
		deleteDialog.pack();
	}
	/** adds appropriate listeners for the add / delete color or phenotype and the mark / delete training point buttons */
	protected void CreateListeners(){
		addClassInfoButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					SpawnAddInfoDialog();
				}
			}
		);
		deleteClassInfoButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					SpawnDeleteInfoDialog();					
				}
			}
		);
		markPixelButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					markOrDelete=true;
					imagePanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					((KImageTrainPanel)imagePanel).setRedrawImage(false);
				}
			}
		);
		deletePixelButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					markOrDelete=false;	
					try {
						imagePanel.setCursor(CursorFactory.getCursor(CursorFactory.CIRCLE_V2));
					} catch ( Exception x ) {}
					((KImageTrainPanel)imagePanel).setRedrawImage(true);
				}
			}
		);
	}
	/** is the user in this training panel marking or deleting training points? */
	public boolean isMarkOrDelete() {
		return markOrDelete;
	}
	/** sets a color or phenotype selected in the {@link KClassInfoBrowser browser} */
	public void selectElement( KClassInfo element ) {
		markPixelButton.doClick();
		activeInfo=element;
		if (element != null)
			for ( KClassInfo class_info: browser.getClassInfos() )			
				class_info.setColorBasedOnSelection(class_info==element);
	}
	/**
	 * The user has just trained a point in the image displayed in the
	 * {@link KPanel#imagePanel image panel}
	 * 
	 * @param filename		the image filename
	 * @param to			the coordinate where the user trained
	 */
	public void NewPoint(String filename,Point to){
		if (markOrDelete)
			((KImageTrainPanel)imagePanel).setRedrawImage(false);
		else
			((KImageTrainPanel)imagePanel).setRedrawImage(true);

		if (activeInfo == null)
			JOptionPane.showMessageDialog(this,"Please select a training element on the left panel");
		else {		
			activeInfo.addEvidence(to,filename,markOrDelete);
			thumbnailPane.updateInfoLabelForPresentPic();			
			thumbnailPane.repaint();
		}
	}
	/**
	 * User just marked a point for deletion
	 */
	public void DeletePoint(String filename, Point to){
		((KImageTrainPanel)imagePanel).setRedrawImage(true);

		for (KClassInfo classinfo : browser.getClassInfos()){
			classinfo.addEvidence(to, filename, false);
		}
		thumbnailPane.updateInfoLabelForPresentPic();			
		thumbnailPane.repaint();		
	}
	/**
	 * User just wanted to add a non point
	 */	
	public void NewNonPoint(String filename, Point to){
		((KImageTrainPanel)imagePanel).setRedrawImage(false);
		
		for (KClassInfo classinfo : browser.getClassInfos()){
			if (classinfo.isNon()){
				classinfo.addEvidence(to, filename, true);
			}
		}
		thumbnailPane.updateInfoLabelForPresentPic();			
		thumbnailPane.repaint();		
	}
	/** gets the filenames of the images in the {@link KThumbnailPane thumbnail pane} */
	public Collection<String> getTrainedImagesFilenames() {
		return thumbnailPane.getThumbnailNames();
	}
	/** gets the DataImage objects of the images in the {@link KThumbnailPane thumbnail pane} */
	public HashMap<String, DataImage> getTrainedImagesAsDataImages(){
		HashMap<String, DataImage> images = new HashMap<String, DataImage>();
		for (String filename : getTrainedImagesFilenames())
			images.put(filename, ImageAndScoresBank.getOrAddDataImage(filename));
		return images;
	}
	/** adds the {@link KThumbnailPane thumbnail pane} to the Southern region of the panel */
	protected void EditSouth(){
		super.EditSouth();
		thumbnailPane=new KThumbnailPane(this);
		southBox.add(thumbnailPane);
	}
	/** set the {@link KPanel#imagePanel image panel} blank */
	public void ClearImage(){
		imagePanel.setDisplayImage(null);
	}
	/** populate the {@link KClassInfoBrowser browser} with the defaults (varies for colors of phenotypes) */
	public abstract void DefaultPopulateBrowser();
	/** 
	 * when the {@link GemIdentModel.TrainSuperclass data models} 
	 * have been {@link GemIdentOperations.Run#OpenProject opened from the saved project}, 
	 * the {@link KThumbnailPane thumbnail pane} is now populated with the appropriate training images 
	 */
	public void PopulateFromOpen(){
		thumbnailPane.updateInfoLabelForAllThumbnails();
	}
	/** revalidate the add / remove image buttons */
	public void RevalidateImageButtons(){
		if (Thumbnails.getTotNumImageFiles(thumbnailPane.getThumbnailNames()) > 0)
			AddImageButton.setEnabled(true);
		else
			AddImageButton.setEnabled(false);
		if (thumbnailPane.getNumThumbnails() == 0)
			RemoveImageButton.setEnabled(false);
	}
	/** toggles the training state between mark / delete */
	public void toggleMarkOrDelete(){
		if (!markOrDelete)
			markPixelButton.doClick();
		else 
			deletePixelButton.doClick();
	}	
	/** sets this thumbnail selected in the {@link KThumbnailPane thumbnail pane} */
	public void setSelectedThumbnail( KThumbnail thumb ) {
		thumbnailPane.setSelectedThumbnail(thumb);
	}	
	/** Resets the KClassInfo preview images */
	public void buildExampleImages() {
		browser.buildExampleImages();
	}	
	/** the daughter classes probide the correct text for {@link #buildSettingsBoxes(String) the super implementation} */
	protected abstract void buildSettingsBoxes();
	/** creates the alpha shadow slider to find points, and the checkbox to toggle the visibility of the training points */
	protected void buildSettingsBoxes(String alphaSliderlabel){
		GUIFrame settings_panel = new GUIFrame("Settings");
		GUIFrame image_panel = new GUIFrame("Image options");
		
		Box image_box = Box.createHorizontalBox();
		image_box.add(AddImageButton);
		image_box.add(RemoveImageButton);
		image_box.add(TrainHelperButton);
		image_box.add(sophisticatedTrainHelperButton);
		
		settings_box = new Container();
		settings_box.setLayout(new GridLayout(0,2,3,0));
		settings_box.add(new JLabel(alphaSliderlabel,JLabel.RIGHT));
		
		settings_box.add(helperSlider);
		settings_box.add(new JLabel("See points:",JLabel.RIGHT));
		settings_box.add(seeTrainCheck);
		
		settings_panel.add(settings_box);
		image_panel.add(image_box);
		
		Box box = Box.createVerticalBox();
		box.add(settings_panel);
		box.add(image_panel);
		
		westBox.add(box);
	}
	/** the training helper is disabled (for some image set types it doesn't make sense) */
	public void DisableTrainingHelper(){
		TrainHelperButton.setEnabled(false);
	}
	public void EnableTrainingHelper(){
		TrainHelperButton.setEnabled(true);
	}
	public KClassInfoBrowser getBrowser(){
		return browser;
	}
	public KThumbnail getSelectedThumbnail(){
		return thumbnailPane.getSelectedThumbnail();
	}
	public void repaintThumbnails(){
		thumbnailPane.repaint();
	}
	public void repaintBrowser(){
		browser.repaint();
	}
	public void buildThumbnailsFromDataImages() {
		synchronized (thumbnailPane){
			thumbnailPane.repaintThumbnailsWithDataImages(getTrainedImagesAsDataImages());
		}
	}
	public void repaintImagePanel() {
		imagePanel.repaint();
	}
	public boolean isInTrainingSet(String filename){
		return thumbnailPane.isInThumbnails(filename);
	}
	public void TurnOnSophisticatedTrainingHelper() {
		sophisticatedTrainHelperButton.setVisible(true);
		AddImageButton.setVisible(false);		
	}
	public void repaintMagnifier(){
		imagePanel.requestFocus();
		imagePanel.repaintMagnifier();		
	}
	
	public String getCurrentTrainingImageName(){
		if (imagePanel.displayImage != null){
			return imagePanel.displayImage.getFilename();
		}
		return null;
	}
}