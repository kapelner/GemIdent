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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
//import javax.swing.JSpinner;
import javax.swing.JTextField;
//import javax.swing.SpinnerNumberModel;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;

import GemIdentImageSets.*;
import GemIdentImageSets.Bliss.BlissImageList;
import GemIdentImageSets.Bliss.BlissImageListNanoZoomer;
import GemIdentImageSets.Bliss.BlissImageListOld;
import GemIdentImageSets.Nuance.NuanceImageList;
import GemIdentImageSets.Nuance.NuanceImageListInterface;
import GemIdentImageSets.Nuance.NuanceImageListNew;
import GemIdentOperations.Run;
import GemIdentTools.IOTools;

/**
 * Responsible for the initial dialog window 
 * when <b>GemIdent</b> is executed. It gives the
 * user the option to open a saved project or begin
 * a new one
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class InitScreen extends JFrame{
	
	/** the internals of the dialog window */
	private ContentPane contentPane;
	/** a pointer to the instantiation of this class so it can be referenced from within an anonymous inner class */
	private InitScreen that;
	
	/** the width of the New Project / Open Project icon */
	public static final int IconWidth=100;
	/** the height of the New Project / Open Project icon */
	public static final int IconHeight=60;

	/** the panel that handles the internals of the InitScreen dialog box */
	private class ContentPane extends JPanel{
		
		/** the new project button */
		private JButton newProject;
		/** the open project button */
		private JButton openProject;
		/** in the new project dialog, the textfield that holds the name of the new project */
		private JTextField name;
		/** in the new project dialog, the textfield that holds the absolute file path of the new project */
		private JTextField location;
		//radio buttons for choosing the image set type
		private JRadioButton bacusBliss;
		private JRadioButton bacusNano;
		private JRadioButton nuance_old;
		private JRadioButton nuance_new;
//		private JRadioButton other_global;
		private JRadioButton other_non_global;
		/** the ButtonGroup object that links the image set type radio buttons */
		private ButtonGroup typeGroup;
		/** the okay button on the bottom of the new project dialog */
		private JButton okay;
		/** the "..." button that launches a directory chooser */
		private JButton findLocation;
		// various labels in the new project dialog
		private JLabel typeLabel;
//		private JLabel magnLabel;
		private JLabel locationLabel;
		private JLabel nameLabel;
		///** the spinner to choose the magnification level for the Nuance image set type */
		//private JSpinner magnification;
		/** the container that holds the New Project / Open Project icon buttons */
		private Container picsBox;	
		
		/** the variables pertinent to beginning a new project - they are passed to {@link Run#NewProject the new project creation function} */
		private String projectName;
		private String homeDir;
		private ImageSetInterface imageset;
//		private float magn;
		
		
		/** initializes all components, adds the New Project / Open Project icons to dialog, sets up mouse & keyboard listeners, and sets variables to default values */
		public ContentPane(){
			super();			
			InitializeAll();
			CreateIcons();
			SetUpListeners();
			setResizable(false);
//			magn=Run.DEFAULT_MAGN;//default
		}			
		/** initializes all the components of the dialog window */
		private void InitializeAll() {
			newProject=new JButton();
			newProject.setIcon(new ImageIcon(IOTools.OpenSystemImage("NewProject.png")));
			openProject=new JButton();
			openProject.setIcon(new ImageIcon(IOTools.OpenSystemImage("OpenProject.png")));
			
			nameLabel=new JLabel("Project Name:");
			name=new JTextField();
			
			locationLabel=new JLabel("Location:");
			location=new JTextField();
			location.setEditable(false);
//			location.setFocusable(false);
			location.setEnabled(false);
			findLocation=new JButton("...");
			findLocation.setPreferredSize(new Dimension(20,10));
			findLocation.setEnabled(false);
			
			typeLabel=new JLabel("Image Set Type:");
			typeLabel.setEnabled(false);			
			String bacusBlissString="Bacus Labs Old Bliss";
			bacusBliss=new JRadioButton(bacusBlissString,false);
			bacusBliss.setActionCommand(bacusBlissString);	
			bacusBliss.setEnabled(false);
			String bacusNanoString="Bacus Labs New Bliss / NanoZoomer";
			bacusNano=new JRadioButton(bacusNanoString,false);
			bacusNano.setActionCommand(bacusNanoString);
			bacusNano.setEnabled(false);
			String nuanceStringOld = "CRI Nuance (old set)";
			nuance_old=new JRadioButton(nuanceStringOld, false);
			nuance_old.setActionCommand(nuanceStringOld);
			nuance_old.setEnabled(false);
			String nuanceStringNew = "CRI Nuance (new set)";
			nuance_new = new JRadioButton(nuanceStringNew, false);
			nuance_new.setActionCommand(nuanceStringNew);
			nuance_new.setEnabled(false);			
//			magnLabel=new JLabel("       digital magn:");
//			magnLabel.setEnabled(false);
//			magnification=new JSpinner(new SpinnerNumberModel(Run.DEFAULT_MAGN,1,10,.2));
//			magnification.setEnabled(false);
//			String other_global_string="Proprietary global set";
//			other_global=new JRadioButton(other_global_string,false);
//			other_global.setActionCommand(other_global_string);
//			other_global.setEnabled(false);
			String other_non_global_string="Non-global image set";
			other_non_global=new JRadioButton(other_non_global_string,false);
			other_non_global.setActionCommand(other_non_global_string);
//			other_non_global.setSelected(true);
			other_non_global.setEnabled(false);			
			typeGroup=new ButtonGroup();
			typeGroup.add(bacusBliss);
			typeGroup.add(bacusNano);
			typeGroup.add(nuance_old);
			typeGroup.add(nuance_new);
//			typeGroup.add(other_global);
			typeGroup.add(other_non_global);
			okay=new JButton("Okay");
			okay.setEnabled(false);
		}
		/** sets up mouse listeners for the New Project / Open Project buttons,
		 * then for the directory chooser buttons, then for the image set type buttons.
		 * If the user chooses an image set type where the necessary files are not in
		 * the directory chosen, <b>GemIdent</b> will prompt and disallow such a project
		 * from being created. The okay button then checks if all fields are filled out
		 * legally. For instructions on beginning a new project or opening a saved project, 
		 * see section 2.1 & 2.3 in the manual.
		 * 
		 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
		 */
		private void SetUpListeners() {
			newProject.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						OrganizeComponents();
					}
				}
			);	
			openProject.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){							
						if (Run.OpenProject(that))
							that.dispose(); //just kill window completely
					}
				}
			);
			findLocation.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));	
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						int result = chooser.showOpenDialog(that);
						if ( result == JFileChooser.APPROVE_OPTION ) {
							File load = chooser.getSelectedFile();
							homeDir=load.getAbsolutePath();
							location.setText(homeDir);
							
//							String[] images=Thumbnails.GetImageList(homeDir);
//							if (images.length >= 1)
								EnableImageTypeFields();
//							else {
//								JOptionPane.showMessageDialog(null,"There aren't any images in that directory. Choose another location.","Directory error",JOptionPane.ERROR_MESSAGE);
//								location.setText("");								
//							}
							other_non_global.setSelected(true);
							other_non_global.doClick(); //want to fire this to ensure imageset is created
							okay.setEnabled(true);
							//nuance.setSelected(true);
						}
					}
				}
			);
			bacusBliss.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						imageset = new BlissImageListOld(homeDir);
						if (!imageset.doesInitializationFileExist()){
							JOptionPane.showMessageDialog(null,"A Bliss Image set requires \""+((BlissImageList)imageset).getInitializationFilename()+"\" which does not exist in this directory.","Directory error",JOptionPane.ERROR_MESSAGE);	
							bacusBliss.setSelected(false);
							other_non_global.setSelected(true);
							other_non_global.doClick();
						}	
						else {
							okay.setEnabled(true);
						}
//						magnification.setEnabled(false);
//						magnLabel.setEnabled(false);
					}
				}
			);
			bacusNano.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						imageset = new BlissImageListNanoZoomer(homeDir);
						if (!imageset.doesInitializationFileExist()){
							JOptionPane.showMessageDialog(null,"A NanoZoomer Image set requires \""+((BlissImageListNanoZoomer)imageset).getInitializationFilename()+"\" which does not exist in this directory.","Directory error",JOptionPane.ERROR_MESSAGE);	
							bacusNano.setSelected(false);
							other_non_global.setSelected(true);
							other_non_global.doClick();
						}
						else {
							okay.setEnabled(true);
						}
//						magnification.setEnabled(false);
//						magnLabel.setEnabled(false);
					}
				}
			);
			nuance_old.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){	
						imageset = new NuanceImageList(homeDir);
						String error = ((NuanceImageListInterface)imageset).getErrorstring();
						if (error != null){
							JOptionPane.showMessageDialog(null, error, "Directory error",JOptionPane.ERROR_MESSAGE);	
							nuance_old.setSelected(false);
							other_non_global.setSelected(true);
							other_non_global.doClick();
						}
						else {
							okay.setEnabled(true);
							//now we're going to ask about the wavelengths
							((NuanceImageListInterface)imageset).askAboutValidWavelengths(that);
						}
//						magnification.setEnabled(true);
//						magnLabel.setEnabled(true);
					}
				}
			);
			nuance_new.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){	
						imageset = new NuanceImageListNew(homeDir);
						String error = ((NuanceImageListInterface)imageset).getErrorstring();
						if (error != null){
							JOptionPane.showMessageDialog(null, error, "Directory error",JOptionPane.ERROR_MESSAGE);	
							nuance_new.setSelected(false);
							other_non_global.setSelected(true);
							other_non_global.doClick();
						}
						else {
							okay.setEnabled(true);
							//now we're going to ask about the wavelengths
							((NuanceImageListInterface)imageset).askAboutValidWavelengths(that);
						}
//							magnification.setEnabled(true);
//							magnLabel.setEnabled(true);
					}
				}
			);
//			magnification.addChangeListener(
//				new ChangeListener(){
//					public void stateChanged(ChangeEvent e){					
//						magn=(float)((double)((Double)magnification.getValue())); //cast away starring Tom Hanks	
//					}
//				}
//			);
//			other_global.addActionListener(
//				new ActionListener(){
//					public void actionPerformed(ActionEvent e){
//						imageset = new GlobalImageSet(homeDir);
//						okay.setEnabled(true);
////						magnification.setEnabled(false);
////						magnLabel.setEnabled(false);
//					}
//				}
//			);
			other_non_global.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						imageset = new NonGlobalImageSet(homeDir);
						okay.setEnabled(true);
//						magnification.setEnabled(false);
//						magnLabel.setEnabled(false);
					}
				}
			);			
			name.addKeyListener(
				new KeyListener(){					
					public void keyPressed(KeyEvent e){}
					public void keyReleased(KeyEvent e){
						projectName=name.getText();
						if (name.getText().trim().length() > 0)
							EnableLocationFields();
						else {
							DisableImageTypeFieldsAndOkay();
							DisableLocationFields();
						}
					}
					public void keyTyped(KeyEvent e){}
				}
			);
			okay.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						String tailoredName=name.getText().trim();
						if (tailoredName.contains("-"))
							JOptionPane.showMessageDialog(that,"Names cannot include hyphens");	
						else if (tailoredName.length() > 0 && homeDir != null){
							that.dispose();
							Run.NewProject(projectName, imageset, Run.DEFAULT_MAGN);
						}
					}
				}
			);
		}
		/** the location fields are enabled upon the entering of a project name */
		protected void EnableLocationFields(){
			location.setEnabled(true);
			findLocation.setEnabled(true);
		}
		/** the location fields are disabled if the project name is blank */
		protected void DisableLocationFields(){
			location.setEnabled(false);
			findLocation.setEnabled(false);
		}
		/** after the choosing of a directory, the rest of the new project dialog is ennabled */
		protected void EnableImageTypeFields(){
			bacusBliss.setEnabled(true);
			typeLabel.setEnabled(true);			
			bacusNano.setEnabled(true);
			nuance_old.setEnabled(true);
			nuance_new.setEnabled(true);
//			if (nuance.isSelected()){
//				magnLabel.setEnabled(true);
//				magnification.setEnabled(true);
//			}
//			other_global.setEnabled(true);
			other_non_global.setEnabled(true);
		}
		/** if the location is invalid, the image set type choosers as well as the submit button is disabled */
		protected void DisableImageTypeFieldsAndOkay(){
			bacusBliss.setEnabled(false);
			typeLabel.setEnabled(false);
//			magnLabel.setEnabled(false);
			bacusNano.setEnabled(false);
			nuance_old.setEnabled(false);
			nuance_new.setEnabled(false);
//			magnification.setEnabled(false);
			other_non_global.setEnabled(false);	
			okay.setEnabled(false);
		}
		/** the New Project / Open project icons are added to the dialog window */
		private void CreateIcons(){
			setLayout(new BorderLayout());

			newProject.setPreferredSize(new Dimension(IconWidth,IconHeight));
			openProject.setPreferredSize(new Dimension(IconWidth,IconHeight));
			
			picsBox=new Container();
			picsBox.setLayout(new BorderLayout());
			picsBox.add(newProject,BorderLayout.WEST);
			picsBox.add(openProject,BorderLayout.EAST);
						
			add(picsBox,BorderLayout.NORTH);
			
			setVisible(true);
			setResizable(true);
			pack();
		}
		/** the components are organized into Box objects and then added to the dialog window */
		private void OrganizeComponents(){
			Box dataBox=Box.createVerticalBox();
			
			Box nameBox=Box.createHorizontalBox();
			nameBox.add(nameLabel);
			nameBox.add(name);
			dataBox.add(nameBox);
			
			Box locationBox=Box.createHorizontalBox();
			locationBox.add(locationLabel);
			locationBox.add(location);
			locationBox.add(findLocation);
			dataBox.add(locationBox);
			
			Box typeBox=Box.createVerticalBox();
//			magnification.setPreferredSize(new Dimension(20,20));
			typeBox.add(typeLabel);
			typeBox.add(bacusBliss);
			typeBox.add(bacusNano);
//			Box magnBox=Box.createHorizontalBox();
			typeBox.add(nuance_old);
			typeBox.add(nuance_new);
//			typeBox.add(magnLabel);
//			magnBox.add(new JLabel("                         "));
//			magnBox.add(magnification);
//			typeBox.add(magnBox);
//			typeBox.add(other_global);
			typeBox.add(other_non_global);
			dataBox.add(typeBox);
			
			remove(picsBox);
			add(dataBox,BorderLayout.CENTER);
			add(okay,BorderLayout.SOUTH);
			pack();
			name.requestFocus();
		}
	}	
	/** launches and sets up the dialog window. Also triggers the {@link Run#Preload() preloading of data} to speed up the opening of the program */
	public InitScreen(){
		super();
		this.setTitle(Run.Title);
		Dimension currentRes=Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(currentRes.width/2-currentRes.width/10,currentRes.height/2-currentRes.height/10);
		that=this;
		contentPane=new ContentPane();
        setContentPane(contentPane);
                
        Run.Preload(); //now that user sees something -- preload data
        addWindowListener(
        	new WindowListener(){
				public void windowActivated(WindowEvent e){}
				public void windowClosed(WindowEvent e){}
				public void windowClosing(WindowEvent e){
					System.exit(0);
				}
				public void windowDeactivated(WindowEvent e){}
				public void windowDeiconified(WindowEvent e){}
				public void windowIconified(WindowEvent e){}
				public void windowOpened(WindowEvent e){}
			}
        );
        pack();
        setVisible(true);
	}
	public void disableOkayButton() {
		contentPane.okay.setEnabled(false);
	}
	public void enableOkayButton() {
		contentPane.okay.setEnabled(true);
		//also request the focus
		contentPane.okay.requestFocus();
	}
}