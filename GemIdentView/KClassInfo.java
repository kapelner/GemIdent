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
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.*;

import GemIdentModel.Phenotype;
import GemIdentModel.TrainSuperclass;
import GemIdentTools.Geometry.Solids;

/**
 * The super class of the {@link KStainInfo color info} and the {@link KPhenotypeInfo 
 * phenotype info} - the small boxes that display information about a {@link GemIdentModel.Stain Stain}
 * and a {@link GemIdentModel.Phenotype Phenotype} in the {@link KColorTrainPanel color
 * train panel} and the {@link KPhenotypeTrainPanel phenotype train panel}. The class is abstract because
 * it provides common functionality but it, itself is never instantiated.
 * 
 * @author Adam Kapelner and Kyle Woodward
 */
@SuppressWarnings("serial")
public abstract class KClassInfo extends JPanel{	

	/** the color of the background when this class is selected */
	public static Color SELECTED_COLOR = new Color(150,150,200);
	/** the color of the background when this class is unselected - left to the look & feel of the environment */
	public static Color UNSELECTED_COLOR;
	
	/** one layer of organizing components on the left */
	protected Container westBox;
	/** another layer of organizing components on the left */
	protected Container west_side;
	
	/** the user enters the name of this color / phenotype into this text box */
	protected JTextField nameField;
	/** the user presses this button to launch a {@link ColorChooser color selection window} */
	protected ColorButton colorChooseButton;
	/** a training panel that "owns" this class info - simplifies the selection of class infos */
	private SelectionEmulator owner;
	/** the user choooses a minimum influence radius for this color / phenotype */
	protected JSpinner rminSpinner;	
	/** the icon displayed on the right that is representative of the training for the color / phenotype */
	protected BufferedImage identifierImage;
	/** the object that houses the {@link #identifierImage identifier image} */
	protected IdentifierToggleButton identifierButton;
	/** a link to the imageTrainPanel - allows changes made in options here to reflect display */
	protected KImageTrainPanel imageTrainPanel;
	/** the underlying model object that this class info controls - either a stain or a phenotype */	
	protected TrainSuperclass trainer;
	
	/** the button that launches the {@link ColorChooser color chooser} and displays the current display color */
	protected class ColorButton extends JButton {
		
		/** the inner panel that displays the color */
		private class ColorPanel extends JPanel {
			public void paintComponent( Graphics g ) {
				super.paintComponent(g);
				g.setColor(trainer.getDisplayColor());
				g.fillRect(0,0,getWidth(),getHeight());
			}
		}		
		public ColorButton(){
			super();
			add(new ColorPanel());
		}
	}
	/** houses the {@link KClassInfo#identifierImage identifier image} */
	protected class IdentifierToggleButton extends JPanel {
		/** sets up the listener that will select this class info if clicked 
		 * @param kClassInfo */
		public IdentifierToggleButton(KClassInfo kClassInfo){
			super();
//			setPreferredSize(identifierPanelSize);
			final IdentifierToggleButton that = this;
			
			addMouseListener(
				new MouseAdapter() {
					public void mouseClicked( MouseEvent e ) {
						that.setIsSelectedToggle();
					}
				}
			);
			repaint();
		}
		/** displays the identifier image, framed with a small box in the upper left that reflects the number of training points */
		public void paintComponent(Graphics g){
			identifierImage = getIdentifierImage();
			if (identifierImage == null){
				g.setColor(getBackground());
				g.fillRect(0,0,getWidth(),getHeight());
				g.setColor(Color.black);
			}
			g.drawImage(identifierImage,0,0,getWidth(),getHeight(),null);
			
			int x_min = 0;
			int x_max = getWidth()-1;
			int y_min = 0;
			int y_max = getHeight()-3;
			
			g.setColor(Color.BLACK);
			g.fillRect(0,0,36,16);
			g.setColor(Color.WHITE);
			g.fillRect(0,0,35,15);
			g.setColor(Color.BLACK);
			g.drawString(""+trainer.getTotalPoints(),5,13);
			
			drawBox(x_min,y_min,x_max,y_max,g);
			drawBox(x_min+1,y_min+1,x_max-1,y_max-1,g);
		}
		/** draws the frame around the identifier image */
		private void drawBox( int x_0, int y_0, int x_1, int y_1, Graphics g ) {
			g.drawLine(x_0,y_0,x_0,y_1);
			g.drawLine(x_0,y_1,x_1,y_1);
			g.drawLine(x_1,y_1,x_1,y_0);
			g.drawLine(x_1,y_0,x_0,y_0);
		}
		/** sets this class info as selected */
		public void setIsSelectedToggle() {
			Select();
		}
	}
	/** Creates a new class info and links it to an image train panel as well as its selection owner */
	public KClassInfo(KImageTrainPanel imageTrainPanel,SelectionEmulator owner ){
		super();
		this.imageTrainPanel=imageTrainPanel;
		KClassInfo.UNSELECTED_COLOR=getBackground();
		this.owner = owner;
		setLayout(new BorderLayout());
//		setPreferredSize(classInfoSize);
		setBorder(new GUIBorder(""));
		setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		
		MakeWestBox();
		
		identifierButton = new IdentifierToggleButton(this);
		add(identifierButton,BorderLayout.CENTER);
		
		nameField.setPreferredSize(nameField.getSize());
		
		MouseAdapter set_active_info = new MouseAdapter() {
			public void mousePressed( MouseEvent e ) {
				Select();
			}
		};
		recursiveSetAdapter(this,set_active_info);
	}
	/**
	 * Sets a mouse listener on every component in the class info recursively. This is
	 * useful for selection - if the user changes the rMin or the color, this
	 * action should also select that class info
	 * 
	 * @param c		the component to add a mouse listener to
	 * @param m		the mouselistener to add
	 */
	private void recursiveSetAdapter( JComponent c, MouseAdapter m ) {
		c.addMouseListener(m);		
		for ( Component d: c.getComponents() ) {
			if ( d instanceof JComponent )
				recursiveSetAdapter((JComponent)d,m);
			else
				d.addMouseListener(m);
		}
	}
	/** Creates all the functionality - textfields and buttons in the Western region of the class info */
	private void MakeWestBox() {
		Box west_coast = Box.createHorizontalBox();
		west_side = Box.createVerticalBox();

		westBox = new Container();
		westBox.setLayout(new GridLayout(0,2,5,0));
		
		// STEP 1:
		// create the "Name:" box.
		JLabel nameLabel = new JLabel("Name:",JLabel.RIGHT);
		nameField = new JTextField("name");
		nameField.addMouseListener(
			new MouseAdapter(){
				public void mouseClicked( MouseEvent e ) {
					Select();
				}
			}
		);

		westBox.add(nameLabel);
		westBox.add(nameField);
		
		// STEP 2:
		// create the "Color:" box.
		JLabel colorLabel = new JLabel("Color:",JLabel.RIGHT);
		colorChooseButton = new ColorButton();
		colorChooseButton.addActionListener(
			new ActionListener(){
				public void actionPerformed(ActionEvent e){
					Select();
					final ColorChooser chooser = new ColorChooser();
					chooser.addOkayListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							setSelectedColor(chooser.getChosenColor());
							imageTrainPanel.setRedrawImage(true);
							imageTrainPanel.repaint();					
						}
					});
				}
			}
		);
		westBox.add(colorLabel);
		westBox.add(colorChooseButton);
		
		// STEP 3:
		// create rmin.
		JLabel rminLabel = new JLabel("rMin:",JLabel.RIGHT);
		rminSpinner = new JSpinner(new SpinnerNumberModel(1,0,3,1));
		rminSpinner.addChangeListener(
			new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					Select();
					imageTrainPanel.setRedrawImage(true);
					imageTrainPanel.repaint();
				}
			}
		);
		JFormattedTextField textfield = ((JSpinner.DefaultEditor)rminSpinner.getEditor()).getTextField();
		textfield.setEditable(true);
		textfield.setFocusable(true);
		textfield.addMouseListener(new MouseAdapter() {
			public void mouseClicked( MouseEvent e ) {
				Select();
			}
		});
		westBox.add(rminLabel);
		westBox.add(rminSpinner);
		
		west_side.add(westBox);
		west_coast.add(west_side);
		west_coast.add(Box.createHorizontalStrut(5));
		add(west_coast,BorderLayout.WEST);
	}	
	/** adds functionality to the class info - to be overridden by daughter classes */
	protected abstract void EditWestBox();
	/** set the newly selected display color */
	private void setSelectedColor(Color color){
		trainer.setDisplayColor(color);		
		colorChooseButton.repaint();
	}
	/** unselect this class info */
	protected void UnSelect() {
		owner.selectElement(null);
	}
	/** select this class info */
	protected void Select() {
		owner.selectElement(this);
	}	
	
	/** when the class info is un/selected, this changes its color */
	public void setColorBasedOnSelection(final boolean selected){
		final KClassInfo that = this;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (selected)
					that.setBackground(KClassInfo.SELECTED_COLOR);
				else
					that.setBackground(KClassInfo.UNSELECTED_COLOR);
			}
		});
	}
	/** a function useful for both color and phenotype class infos. 
	 * For colors, this will be called to generate the {@link 
	 * #identifierImage identifier image} for all class infos. For
	 * phenotypes, this will be called to generate the {@link 
	 * #identifierImage identifier image} for the "NON" phenotype.
	 * {@link GemIdentModel.TrainSuperclass#getColors() 
	 * Grabs all the colors} from the model and then draws pixels at 
	 * random to create the image.
	 */
	protected void buildExampleImageFromColors() {
		BufferedImage image=getIdentifierImage();		
		if ( image == null )
			image = new BufferedImage(identifierButton.getWidth(),identifierButton.getHeight(),BufferedImage.TYPE_INT_RGB);
		
		int num_pixels = image.getHeight()*image.getWidth();
		java.util.List<Integer> pixel_locations = new ArrayList<Integer>();
		for ( int i = 0; i < num_pixels; ++i )
			pixel_locations.add(i,i);
		
		
		
		ArrayList<Color> color_list=trainer.getColors();
		int num_points = color_list.size();
		if (num_points == 0)			
			image=null;
		else {
			for ( int i = 0; i < num_points; ++i ) {
				// for each picked color...
				double d = ((double)num_pixels)/num_points;
				int count = (int)d;
				count += (d-count>Math.random()) ? 1 : 0;
				
				for ( int j = 0; j < count; ++j ) {
					if (pixel_locations.size() == 0)
						break;
					int new_index = (int)(Math.floor(Math.random()*pixel_locations.size()));					
					int new_pixel_index = pixel_locations.get(new_index);
					int x = new_pixel_index % identifierButton.getWidth();
					int y = new_pixel_index / identifierButton.getWidth();
					try {
						image.setRGB(x,y,(color_list.get(i)).getRGB());
					} catch(Exception exp){}
					pixel_locations.remove(new_index);
	
				}
			}
		}
		setIdentifierImage(image);
		identifierButton.repaint();
	}	
	/** get the name of this color / phenotype */
	public String getTrainName(){
		return nameField.getText();
	}	
	/** create the {@link #identifierImage identifier image} - to be implemented in daughter classes*/
	public abstract void buildExampleImage();
	/** sets up the listeners for the components that change the underlying model */
	protected abstract void SetUpListeners();
	/**
	 * When the user trains a new point, this function is called to
	 * update the color / phenotype class info. The {@link #identifierImage
	 * identifier image} is then rebuilt (on a separate thread)
	 * to reflect the change to the training set
	 * 
	 * @param point			the coordinate clicked
	 * @param filename		the name of the image
	 * @param mark			was the point marked or deleted?
	 */
	protected void addEvidence(Point point,String filename,boolean mark){
		if ( trainer == null ) 
			return;		
		if ( mark )
			trainer.addPointToTrainingSet(filename,point);
		else
			for ( Point t: Solids.GetPointsInSolidUsingCenter(KTrainPanel.EraseRadius,point) )
				trainer.deletePointFromTrainingSet(filename,t);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				buildExampleImage();
			}
		});
	}
	/** return the height of this component by retrieving the height of the {@link #identifierButton identifier button} */
	public int getHeight() {
		return (int)identifierButton.getSize().getHeight();
	}
	/** returns the name of the color / phenotype */
	public String getName(){
		return trainer.getName();
	}
	protected BufferedImage getIdentifierImage() {
		return identifierImage;
	}	
	protected void setIdentifierImage(BufferedImage identifierImage){
		this.identifierImage=identifierImage;
	}
	public IdentifierToggleButton getToggleButton(){
		return identifierButton;
	}
	public TrainSuperclass getTrainer() {
		return trainer;
	}
	public boolean isNon(){
		if (trainer instanceof Phenotype){
			if (((Phenotype)trainer).isNON()){
				return true;
			}
		}
		return false;
	}
}