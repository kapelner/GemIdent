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
import java.util.Arrays;
import java.util.Collection;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import GemIdentAnalysisConsole.ConsoleParser;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;

/**
 * Embodies the GUI for <b>GemIdent</b>.
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class KFrame extends JFrame{
	
	/** houses the familiar <b>GemIdent</b> tabs */
	private JTabbedPane tabs;
	/** the dimension of the GUI */
	public static final Dimension frameSize=new Dimension(1280,800);
	
	/** the panel that allows the user to train for colors */
	private KColorTrainPanel colorTrainPanel;
	/** the panel that allows the user to train/retrain for phenotypes */
	private KPhenotypeTrainPanel phenotypeTrainPanel;
	/** the panel that allows the user to classify images */
	private KClassifyPanel classifyPanel;
	/** the panel that allows the user to analyze data */
	private KAnalysisPanel analysisPanel;
	/** the global keyboard shortcut map - from keystroke to action name */
	private InputMap input_map;
	/** the global keyboard shortcut map - from action name to action */
	private ActionMap action_map;
	/** the option in the file menu to save the current project */
	private JMenuItem file_save;
	
	/** is the user using <b>GemIdent</b> at the moment? */
	private boolean active;
	/** the string to append to the title */
	private String toappend;	
	
	// the names of the tabs:
	private static final String colorTrainTabName="Color Selection";
	private static final String phenoTrainTabName="Phenotype Training";
	private static final String classifyTrainTabName="Classification";
	private static final String analysisPanelTabName="Analysis";
	
	/** the filename of the manual */
	public static final String ManualFileName="GemIdentManual.pdf";
	
	/** initializes the frame, adds a listener to check if the user is using the frame */
	public KFrame(){
		super(Run.Title);
		toappend = "";
		active=true;
		this.setIconImage(IOTools.OpenSystemImage("diamondicon.png"));
		setVisible(false);
		setPreferredSize(frameSize);
		setResizable(true);
		addWindowListener(
			new WindowListener(){				
				public void windowActivated(WindowEvent e){
					active=true;
				}
				public void windowClosed(WindowEvent e){}
				public void windowClosing(WindowEvent e){
					Run.it.ExitProgram();
				}
				public void windowDeactivated(WindowEvent e){
					active=false;
				}
				public void windowDeiconified(WindowEvent e){}
				public void windowIconified(WindowEvent e){}
				public void windowOpened(WindowEvent e){}
			}
		);
	}
	/** draw the GUI */
	public void DrawAll(){
		CreateMenu();
		CreateTabbedEnvironment();
		pack();
	}
	/** Initialize the tabbed environment, add +/- keystrokes for magnifier window */
	private void CreateTabbedEnvironment(){		
		tabs=new JTabbedPane();
		addUniversalKeyStrokes();
		colorTrainPanel=new KColorTrainPanel();
		phenotypeTrainPanel=new KPhenotypeTrainPanel();
		classifyPanel=new KClassifyPanel();
		analysisPanel=new KAnalysisPanel();
		add(tabs);		
		tabs.addTab(colorTrainTabName,colorTrainPanel);
		tabs.addTab(phenoTrainTabName,phenotypeTrainPanel);
		tabs.addTab(classifyTrainTabName,classifyPanel);
		tabs.addTab(analysisPanelTabName,analysisPanel);
		tabs.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e){
				    JTabbedPane tabSource = (JTabbedPane)e.getSource();
				    String tab = tabSource.getTitleAt(tabSource.getSelectedIndex());
				    if (tab.equals(analysisPanelTabName))
				    	analysisPanel.FocusEnter();				      
				}
			}
		);	
		//the phenotype tab should be primary now with deep learning
		tabs.setSelectedComponent(phenotypeTrainPanel);
	}
	/** 
	 * morph the phenotype training tab into a phenotype retraining tab
	 * by refreshing the {@link KThumbnailPane thumbnail pane}, adding
	 * new keystrokes for the sliders, adding boosting support along with
	 * error finding support
	 */
	public void AddOrUpdateRetrainTab(){
		phenotypeTrainPanel.AdjustThumbnailPane();
		phenotypeTrainPanel.SetUpKeyStrokesForAlphaSliders();
		phenotypeTrainPanel.SelectNON();
		phenotypeTrainPanel.EnableBoostingFeature();
		phenotypeTrainPanel.EnableErrorFinderFeature();
		phenotypeTrainPanel.ClickFirstThumbnailIfExistsAndIsNecessary();
		tabs.remove(phenotypeTrainPanel);
		tabs.insertTab(phenoTrainTabName,null,phenotypeTrainPanel,null,2);	
		tabs.setSelectedComponent(phenotypeTrainPanel);
		repaint();
	}
	/** during classification, remove the phenotype training panel */
	public void KillPhenotypeTab() {
		tabs.remove(phenotypeTrainPanel);
		phenotypeTrainPanel.ClearImage();	
	}
	/** create a separation in the menu */
	private class MenuSeparator extends Container {
		public MenuSeparator() {
			this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));			
			this.add(Box.createHorizontalStrut(3));
			this.add(new JSeparator(SwingConstants.HORIZONTAL));
			this.add(Box.createHorizontalStrut(3));
		}
	}
	/** create the file menu, help menu, script menu */
	private void CreateMenu() {
		
		JMenuBar menu=new JMenuBar();
		
		JMenu file_menu = new JMenu("File");
		file_menu.setMnemonic('f');

//		JMenuItem file_new = new JMenuItem("New project");
//		file_new.setMnemonic('n');
//		file_new.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,Event.CTRL_MASK));
//		file_new.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					Run.NewProject();
//				}
//			}
//		);
		
		file_save = new JMenuItem("Save project");
		file_save.setMnemonic('s');
		file_save.setEnabled(false);
		file_save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,Event.CTRL_MASK));
		file_save.addActionListener( 
			new ActionListener(){	
				public void actionPerformed(ActionEvent e) {
					Run.SaveProject();
				}
			}
		);
		
//		JMenuItem file_save_as = new JMenuItem("Save project as");
//		file_save_as.setMnemonic('a');
//		file_save_as.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,Event.CTRL_MASK+Event.SHIFT_MASK));
//		file_save_as.addActionListener( 
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					Run.SaveAsProject();
//				}
//			}
//		);
		
//		JMenuItem file_open = new JMenuItem("Open project");
//		file_open.setMnemonic('o');
//		file_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,Event.CTRL_MASK));
//		file_open.addActionListener( 
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					Run.OpenProject(true);				
//				}
//			}
//		);
		
		JMenuItem file_quit = new JMenuItem("Quit");
		file_quit.setMnemonic('q');
		file_quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,Event.CTRL_MASK));
		file_quit.addActionListener( 
			new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					Run.it.ExitProgram();			
				}
			}
		);

//		file_menu.add(file_new);
//		file_menu.add(new MenuSeparator());
		file_menu.add(file_save);
//		file_menu.add(file_save_as);
//		file_menu.add(file_open);
		file_menu.add(new MenuSeparator());
		file_menu.add(file_quit);
		
		menu.add(file_menu);
		
		JMenu script_menu = new JMenu("Script");
		script_menu.setMnemonic('s');
		JMenuItem loadAndRun = new JMenuItem("Load and Run Analysis Script . . .");
		loadAndRun.setMnemonic('A');
		loadAndRun.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,Event.CTRL_MASK));
		loadAndRun.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (Arrays.asList(tabs.getComponents()).contains(analysisPanel)){
						tabs.setSelectedComponent(analysisPanel);
						
						JFileChooser chooser = new JFileChooser();
						chooser.setFileFilter(
							new FileFilter(){
								public boolean accept(File f){
									if (f.isDirectory())
										return true;
									String name=f.getName();
									String[] pieces=name.split("\\.");
									if (pieces.length != 2)
										return false;
									if (pieces[1].toLowerCase().equals("txt"))
										return true;
									return false;
								}
								public String getDescription(){
									return "txt";
								}
							}
						);		
						int result = chooser.showOpenDialog(null);
						if ( result == JFileChooser.APPROVE_OPTION ){						
							final File file = chooser.getSelectedFile();
							analysisPanel.RunScript(file);																	
						}
						else
							JOptionPane.showMessageDialog(Run.it.getGUI(),"Not a valid script file");
					}					
				}
			}
		);
		script_menu.add(loadAndRun);
		
		menu.add(script_menu);
		
		//add the help menu which opens the pdf manual
//		JMenu help_menu = new JMenu("Help");
//		help_menu.setMnemonic('h');
//		JMenuItem manual = new JMenuItem("Manual");
//		manual.setMnemonic('m');
//		manual.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,Event.CTRL_MASK));
//		manual.addActionListener(
//			new ActionListener(){
//				public void actionPerformed(ActionEvent e){
//					IOTools.RunSystemProgramInOS(ManualFileName);
//				}
//			}
//		);
//		help_menu.add(manual);
//		
//		menu.add(help_menu);
		
		//add it finally:
		setJMenuBar(menu);
	}
	/** gets the collection of images in the phenotype training set */
	public Collection<String> getTrainingImageSet(){
		return phenotypeTrainPanel.getTrainedImagesFilenames();
	}
	/** creates the title, and populates the training panels
	 *  
	 * @param imageset		The imageset - need to make sure it's a user color imageset
	 */
	public void DefaultPopulateWindows(ImageSetInterface imageset){
		setTitle(Run.Title+Run.it.projectName);
		if (imageset instanceof ImageSetInterfaceWithUserColors)
			colorTrainPanel.DefaultPopulateBrowser();
		phenotypeTrainPanel.DefaultPopulateBrowser();
		setVisible(true);
	}
	/** creates the title, and populates the training panels from a loaded project */
	public void OpenPopulateWindows(ImageSetInterface imageset){
		setTitle(Run.Title+Run.it.projectName);
		if (imageset instanceof ImageSetInterfaceWithUserColors)
			colorTrainPanel.PopulateFromOpen();
		phenotypeTrainPanel.PopulateFromOpen();
		classifyPanel.SetValuesToOpenProject();
		setVisible(true);
	}
	/** en/disables the add/remove image buttons after an image load */
	public void DisableButtons(){
		colorTrainPanel.RevalidateImageButtons();
		phenotypeTrainPanel.RevalidateImageButtons();
	}
	/** repaints the GUI */
	public void repaint(){
		setTitle(Run.Title+Run.it.projectName+toappend);
		super.repaint();
	}
	/** adds global keystrokes for in/decrementing zoom and toggling between mark / delete in training panels */
	private void addUniversalKeyStrokes() {
		
		input_map=tabs.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		action_map=tabs.getActionMap();
		
		String PLUS = "magnify";
		String MINUS = "demagnify";
		String SPACE = "togglemark";
		
		KeyStroke k_plus1 = KeyStroke.getKeyStroke('+');
		KeyStroke k_plus2 = KeyStroke.getKeyStroke('=');
		KeyStroke k_minus = KeyStroke.getKeyStroke('-');
		KeyStroke k_space = KeyStroke.getKeyStroke(' ');

		
		AbstractAction f_plus = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				KPanel kpanel = (KPanel)tabs.getSelectedComponent();
				kpanel.getImagePanel().adjustMagnifierIntensity(1);
			}
		};
		AbstractAction f_minus = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				KPanel kpanel = (KPanel)tabs.getSelectedComponent();
				kpanel.getImagePanel().adjustMagnifierIntensity(-1);
			}
		};
		AbstractAction f_space = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				KPanel kpanel = (KPanel) tabs.getSelectedComponent();
				if ( kpanel instanceof KTrainPanel )
					if (kpanel.imagePanel.isFocusOwner())
						((KTrainPanel)kpanel).toggleMarkOrDelete();
			}
		};
		
		input_map.put(k_plus1,PLUS);
		input_map.put(k_plus2,PLUS);
		input_map.put(k_minus,MINUS);
		input_map.put(k_space,SPACE);
		action_map.put(PLUS,f_plus);
		action_map.put(MINUS,f_minus);
		action_map.put(SPACE,f_space);
	}
	/** builds the identifier images for color train panel or phenotype train panel */
	public void buildExampleImages(boolean color) {
		this.phenotypeTrainPanel.buildExampleImages();
		if (color)
			this.colorTrainPanel.buildExampleImages();
	}
	/** sets whether the training points in the {@link KPhenotypeTrainPanel phenotype training panel} are visible */
	public void setSeeTrainingPoints(boolean see){
		if (see)
			phenotypeTrainPanel.EnableTrainPoints();		
		else
			phenotypeTrainPanel.DisableTrainPoints();
	}
	/** for some image set types, it is appropriate to not have image helpers */
	public void DisableTrainingHelpers(){
		colorTrainPanel.DisableTrainingHelper();
		phenotypeTrainPanel.DisableTrainingHelper();
	}
	public void EnableTrainingHelpers(){
		colorTrainPanel.EnableTrainingHelper();
		phenotypeTrainPanel.EnableTrainingHelper();
	}	
	public void DisableSave(){
		file_save.setEnabled(false);
	}
	public void EnableSave(){
		file_save.setEnabled(true);
	}
	public boolean isActive() {
		return active;
	}
	public ActionMap getAction_map() {
		return action_map;
	}
	public InputMap getInput_map() {
		return input_map;
	}
	public void buildGlobalsViewsInAnalysis() {
		analysisPanel.addImageIcons();		
	}
	public void KillAnalysisTab(){
		tabs.remove(analysisPanel);		
	}
	public void SwitchToAnalysisTab(){
		System.out.println("SwitchToAnalysisTab analysisPanel:" + analysisPanel);
		tabs.setSelectedComponent(analysisPanel);
	}
	public ConsoleParser getParser(){
		return analysisPanel.getParser();
	}
	public void HideColorTrainTab() {
		if (Arrays.asList(tabs.getComponents()).contains(colorTrainPanel)){
			tabs.remove(colorTrainPanel);
		}
	}
	public void ShowColorTrainTab() {
		if (!Arrays.asList(tabs.getComponents()).contains(colorTrainPanel)){
			tabs.add(colorTrainTabName, colorTrainPanel);
		}
	}
	public KClassifyPanel getClassifyPanel() {
		return classifyPanel;
	}
	public void AddAdjustColorButton() {
		phenotypeTrainPanel.AddAdjustColorButton();		
	}
	public KTrainPanel getTrainingPanel(){
		return phenotypeTrainPanel;
	}	
	public void AppendToTitle(String toappend){
		this.toappend = toappend;
	}
	public void TurnOnSophisticatedTrainingHelper() {
		phenotypeTrainPanel.TurnOnSophisticatedTrainingHelper();		
	}
}