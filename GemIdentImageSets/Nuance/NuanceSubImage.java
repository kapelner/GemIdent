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

package GemIdentImageSets.Nuance;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import GemIdentImageSets.DataImage;
import GemIdentTools.Matrices.IntMatrix;
import GemIdentTools.Matrices.ShortMatrix;
import GemIdentView.ColorChooser;
import GemIdentView.GUIFrame;

/**
 * Handles a multispectral image created by a modified CRi Nuance Bliss Image scanner.
 * 
 * see <a href="http://www.cri-inc.com/products/nuance.asp">the Nuance scanner webpage</a>
 * 
 * This class is not fully documented since it implements proprietary features
 * for a proprietary hardware setup
 * 
 * @author Adam Kapelner
 */
public class NuanceSubImage extends DataImage {
	
	private static final int BackgroundColor = Color.WHITE.getRGB();
	
	public static final int HistogramWidth = 600;
	private static final int HistogramHeight = 90; //height really isn't relevant outside of this class
		
	protected HashMap<String, IntMatrix> nuanceimages;
	protected int num;
	protected NuanceImageListInterface imagelist;
	
	private boolean already_built; //did we already build the gizmos below:
	private HashMap<String, PixelDistribution> pixeldistributions;
		
		
	private HashMap<String, BufferedImage> wave_images;
	private NuanceImagePicPanel nuanceimagepicpanel;
	protected boolean cropped;
	
	private HashMap<String, Boolean> blinker_stop_flags;
	private HashMap<String, JCheckBox> blinkers;
	private HashMap<String, JCheckBox> visibles;
	
	public NuanceSubImage(){}
	
	public NuanceSubImage(NuanceImageListInterface imagelist, Object num_or_filename, boolean cropped){
		this.cropped = cropped;
		this.imagelist = imagelist;	
		
		SetUpBlinkerModule();
		
		if (num_or_filename instanceof String){
			filename=(String)num_or_filename;
			num = NuanceImageListInterface.ConvertFilenameToNum(filename);
		}
		else if (num_or_filename instanceof Integer){
			num = (Integer)num_or_filename;
			filename = NuanceImageListInterface.ConvertNumToFilename(num);
		}
		nuanceimages = imagelist.getNuanceImagePixelData(num, cropped);
		setupDisplayImage();
	}
	
	private static final long BlinkerPeriod = 1000; //one second
	private void SetUpBlinkerModule() {
		blinker_stop_flags = new HashMap<String, Boolean>(imagelist.NumFilters());
		blinkers = new HashMap<String, JCheckBox>(imagelist.NumFilters());
		visibles = new HashMap<String, JCheckBox>(imagelist.NumFilters());
		for (final String wave_name : imagelist.getWaveNames()){
			blinker_stop_flags.put(wave_name, false);
		}
	}

	protected void setupDisplayImage(){
		ShortMatrix example = (ShortMatrix)nuanceimages.values().toArray()[0];
		displayimage = new BufferedImage(example.getWidth(), example.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int i=0;i < displayimage.getWidth(); i++)
			for (int j=0;j < displayimage.getHeight(); j++)
				displayimage.setRGB(i, j, BackgroundColor);		
	}

	public void BuildDisplayAndPixelDistrs(){
		if (!already_built){
			already_built = true; //set the flag so they don't get built again
			
			//init some more data structures
			pixeldistributions = new HashMap<String, PixelDistribution>(nuanceimages.size());		
			for (String wavename : nuanceimages.keySet()){
				pixeldistributions.put(wavename, new PixelDistribution(wavename));			
			}
			BuildDisplayImageFromWaves();	
		}
	}

	public NuanceSubImage clone(){ //clones won't need any bells and whistles
		NuanceSubImage clone = new NuanceSubImage();
		clone.imagelist = imagelist;
		clone.filename = filename;
		clone.num = num;
		clone.nuanceimages = imagelist.getNuanceImagePixelData(num, cropped); //duplicate the data
		clone.setupDisplayImage();
		clone.wave_images = wave_images; //this can be shared at the pointer level, no problem
		return clone;
	}

	private static final int MIN_ALPHA = 15;
	public void BuildDisplayImageFromWaves() {
		
		wave_images = new HashMap<String, BufferedImage>(); //reset it each time!
		
		for (String wave_name : imagelist.getWaveNames()){
			if (imagelist.getWaveVisible(wave_name)){
				BufferedImage wave_image = new BufferedImage(displayimage.getWidth(), displayimage.getHeight(), BufferedImage.TYPE_INT_ARGB);
				
				double lowerbound = Math.round(imagelist.getIntensityA(wave_name) / conversion_factor);
				double upperbound = Math.round(imagelist.getIntensityB(wave_name) / conversion_factor);
				if (lowerbound > upperbound) //this is a mistake on the part of the user
					continue;
				double upperlowerrange = upperbound - lowerbound;
				IntMatrix pixels = nuanceimages.get(wave_name);
				Color displaycolor = imagelist.getWaveColor(wave_name);
				
				for (int i=0;i<displayimage.getWidth();i++){
					for (int j=0;j<displayimage.getHeight();j++){
						int val = pixels.get(i, j);
						double percent = 0; //init
						if (val < lowerbound)
							continue;
						else if (val > upperbound) //we'll still display pixels that maxed out, otherwise looks weird (this can be changed though
							percent = 1;
						else
							percent = (val - lowerbound)/upperlowerrange;
						//min alpha is 50
						int alpha = (int)Math.round(MIN_ALPHA + percent * (255 - MIN_ALPHA));
						Color pixel = new Color(displaycolor.getRed(), displaycolor.getGreen(), displaycolor.getBlue(), alpha);
						wave_image.setRGB(i, j, pixel.getRGB());
					}
				}
				wave_images.put(wave_name, wave_image);
			}
			if (nuanceimagepicpanel != null){
				nuanceimagepicpanel.repaintPaintedImage();				
			}
		}
	}
	public static final double conversion_factor = (HistogramWidth + 1) / (double)(40001);
	private static final double SqueezeWrapPercent = 0.9999;
	private class PixelDistribution {
		private int[] bins;
		private BufferedImage representation;
		private int bin_max_value;
		private String wave_name;

		public PixelDistribution(String wave_name) {
			this.wave_name = wave_name;
			
			bins = new int[HistogramWidth + 1]; //add one for the first bin which is irrelevant
			BinPixelValues();
			GenerateRepresentation();
			if (imagelist.intensityRangeUnitialized(wave_name))
				SqueezeWrap();
		}
		
		private static final int MAX_ITERATIONS = 1000;
		private void SqueezeWrap() {
			double total = 0;
			for (int i = 1; i < HistogramWidth; i++)
				total += bins[i];
			
			int a = 1;
			int b = HistogramWidth;	
			int n = 0;
			while (true){
				n++;
				a++;				
				int subtotal_a = 0;
				for (int i = a; i < b; i++)
					subtotal_a += bins[i];
				a--;
				
				b--;
				int subtotal_b = 0;
				for (int i = a; i < b; i++)
					try {
						subtotal_b += bins[i];
					} catch (Exception e){}

				b++;
				
				if (subtotal_a / total > subtotal_b / total){
					a++;
					if (subtotal_a / total < SqueezeWrapPercent)
						break;
				}
				else { //less than or equals (benefit of the doubt we should shrink the higher values)
					b--; 
					if (subtotal_b / total < SqueezeWrapPercent)
						break;
				}
				if (n > MAX_ITERATIONS){
					break;
				}
			}
			imagelist.setIntensityA(wave_name, a);
			imagelist.setIntensityB(wave_name, b);
		}

		private void BinPixelValues() {
			IntMatrix pixel_data = nuanceimages.get(wave_name);
			for (int i=0;i<displayimage.getWidth();i++){
				for (int j=0;j<displayimage.getHeight();j++){
					bins[(int)Math.floor(pixel_data.get(i, j) * conversion_factor)]++;
				}
			}
			
			//logarithmic:
			for (int i = 1; i < bins.length; i++){
				bins[i] = (int)Math.round(Math.log(bins[i] + 1) / Math.log(1000) * 100);				
			}
			
			bin_max_value = Integer.MIN_VALUE;
			for (int i = 1; i < bins.length; i++) //avoid first bin
				if (bins[i] > bin_max_value)
					bin_max_value = bins[i];		
		}

		private void GenerateRepresentation() {
			representation = new BufferedImage(HistogramWidth, HistogramHeight, BufferedImage.TYPE_INT_RGB); //change to binary soon
			
			for (int i = 0; i < MaxRangeOnSlider; i++){
				int height = (int)Math.floor(bins[i + 1] / (double)bin_max_value * HistogramHeight);
				for (int j = HistogramHeight - height; j < HistogramHeight; j++){
					for (int m = 0; m < (int)Math.ceil(MaxRangeOnSlider / ((double)HistogramWidth)); m++){
						if (m * i >= HistogramWidth)
							break;
						representation.setRGB(m * i, j, Color.WHITE.getRGB());
					}
				}
			}
		}

		public BufferedImage getRepresentation() {
			return representation;
		}
		
	}
	
	/** the button that launches the {@link ColorChooser color chooser} and displays the current display color */
	private static final int colorbuttonwidth = 20;
	private static final int colorbuttonheight = 10;
	@SuppressWarnings("serial")
	protected class ColorButton extends JButton {
		
		/** the inner panel that displays the color */
		private class ColorPanel extends JPanel {
			public void paintComponent( Graphics g ) {
				super.paintComponent(g);
				g.setColor(imagelist.getWaveColor(wave_name));
				g.fillRect(0, 0, colorbuttonwidth, colorbuttonheight);
			}
		}
		private String wave_name;
		
		public ColorButton(String wave_name){
			super();
			this.wave_name = wave_name;
			this.setMinimumSize(new Dimension(colorbuttonwidth, colorbuttonheight));
			add(new ColorPanel());
		}
	}
	
	private static boolean not_blinking = true;
	public void CreateAdjustColorDialog(JFrame adjust_color_dialog){
		adjust_color_dialog = new JFrame();
		adjust_color_dialog.setVisible(false);
		Box box = Box.createVerticalBox();
		adjust_color_dialog.setContentPane(box);
		adjust_color_dialog.setTitle("Color adjustment (stage " + num + ")");
		for (final String wave_name : pixeldistributions.keySet()){			
			Container container = new Container();
			container.setLayout(new GridLayout(0,1,4,0));					
			
			Box horiz = Box.createHorizontalBox();
			horiz.add(new JLabel("visible: "));
			final JCheckBox visible_check = new JCheckBox();
			visibles.put(wave_name, visible_check);
			visible_check.setToolTipText("Turn on/off the visibility of " + wave_name);
			visible_check.setSelected(imagelist.getWaveVisible(wave_name));
			visible_check.addItemListener(
				new ItemListener(){
					public void itemStateChanged(ItemEvent e) {
					    if (e.getStateChange() == ItemEvent.DESELECTED){
					    	imagelist.setWaveVisible(wave_name, false);
					    }
					    else {
					    	imagelist.setWaveVisible(wave_name, true);
					    }
					    imagelist.BuildDisplayImagesFromWaves(not_blinking);
					}
				}
			);
			horiz.add(visible_check);	
			
			ColorButton colorChooseButton = new ColorButton(wave_name);
			colorChooseButton.addActionListener(
				new ActionListener(){
					public void actionPerformed(ActionEvent e){
						final ColorChooser chooser = new ColorChooser();
						chooser.addOkayListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								imagelist.setWaveColor(wave_name, chooser.getChosenColor());				
							}
						});
					}
				}
			);
			colorChooseButton.setToolTipText("Change the display color of " + wave_name);
			horiz.add(new JLabel(" color: "));
			horiz.add(colorChooseButton);
			
			//now add the blinker
			horiz.add(new JLabel("  blinker: "));
			JCheckBox blinker = new JCheckBox();
			blinkers.put(wave_name, blinker);
			blinker.setToolTipText("Turn the visibility of " + wave_name + " on blinker mode");
			blinker.setSelected(false); //defaults to not on, state not saved in gem file
			blinker.addItemListener(
				new ItemListener(){
					public void itemStateChanged(ItemEvent e) {
					    if (e.getStateChange() == ItemEvent.DESELECTED){
					    	not_blinking = true; //set blinking var
					    	blinker_stop_flags.put(wave_name, true);
					    	for (String wave_name_o : pixeldistributions.keySet()){
					    		visibles.get(wave_name_o).setEnabled(true);					    		
					    	}					    	
					    	visible_check.setSelected(true); //turn it back on
					    	//turn all of them back on
					    	for (String wave_name_o : pixeldistributions.keySet()){
					    		blinkers.get(wave_name_o).setEnabled(true);				    		
					    	}					    	
					    }				    	
					    else {
					    	not_blinking = false; //set blinking var
					    	//need to make sure others are disabled
					    	for (String wave_name_o : pixeldistributions.keySet()){
					    		if (!wave_name.equals(wave_name_o)){
					    			blinkers.get(wave_name_o).setEnabled(false);
					    		}					    		
					    	}
					    	for (String wave_name_o : pixeldistributions.keySet()){
					    		visibles.get(wave_name_o).setEnabled(false);					    		
					    	}	
					    	blinker_stop_flags.put(wave_name, false); //reset flag

							new Thread(){
								public void run(){
									while (!blinker_stop_flags.get(wave_name)){
										if (visible_check.isSelected()){
											visible_check.setSelected(false);
											try {
												Thread.sleep(BlinkerPeriod);
											} catch (InterruptedException e){}											
										}
										else {
											visible_check.setSelected(true);
											try {
												Thread.sleep(BlinkerPeriod + BlinkerPeriod / 2);
											} catch (InterruptedException e){}											
										}										
									}
								}
							}.start();
					    }
					}
				}
			);
			horiz.add(blinker);				
			
			horiz.add(new JLabel("            "));
						
			container.add(horiz);
		
			JSliderRange range = new JSliderRange(wave_name);
			range.a.setToolTipText("Change the bottom intensity threshold for " + wave_name);
			container.add(range.a);
			PicPanel picpanel = new PicPanel();
			picpanel.setToolTipText("The intensity distribution for " + wave_name);
			picpanel.setImage(pixeldistributions.get(wave_name).getRepresentation());
			container.add(picpanel);
			container.add(range.b);
			range.b.setToolTipText("Change the top intensity threshold for " + wave_name);
			
			GUIFrame wave_panel = new GUIFrame(wave_name); //the Russian dolls . . .
			wave_panel.add(container);
			box.add(wave_panel);
		}
		JButton adjust = new JButton("Adjust...");
		adjust.setToolTipText("Update the display train image with the intensities just adjusted");
		adjust.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				imagelist.BuildDisplayImagesFromWaves(true);
			}			
		});
		box.add(adjust);
		adjust_color_dialog.pack();
		adjust_color_dialog.setResizable(false);
		imagelist.setAdjust_color_dialog(adjust_color_dialog);
	}
	
	public static final int BeginningValOnSliderA = 15;
	private static final int MaxRangeOnSlider = 150;
	private class JSliderRange {
		public JSlider a;
		public JSlider b;
		
		public JSliderRange(final String wave_name){
			a = new JSlider(JSlider.HORIZONTAL, 1, MaxRangeOnSlider, BeginningValOnSliderA);
			b = new JSlider(JSlider.HORIZONTAL, 1, MaxRangeOnSlider, MaxRangeOnSlider);
			//init values to values saved
			if (!imagelist.intensityRangeUnitialized(wave_name)){
				a.setValue(imagelist.getIntensityA(wave_name));
				b.setValue(imagelist.getIntensityB(wave_name));
			}
			//add listeners to respond when changed
			a.addChangeListener(new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					imagelist.setIntensityA(wave_name, ((JSlider)e.getSource()).getValue());
				}				
			});
			b.addChangeListener(new ChangeListener(){
				public void stateChanged(ChangeEvent e) {
					imagelist.setIntensityB(wave_name, ((JSlider)e.getSource()).getValue());
				}				
			});			
		}
	}
	
	@SuppressWarnings("serial")
	private class PicPanel extends JPanel {
		/** the image to paint over the panel */
		protected BufferedImage image;
		/** the default constructor */
		public PicPanel(){
			super();
		}			
		/**
		 * Sets the image held by the preview box.
		 * @param image
		 */
		public void setImage( BufferedImage image ) {
			this.image = image;
			this.repaint();
			this.setMinimumSize(new Dimension(image.getWidth(), image.getHeight()));
		}			
		/**
		 * Overrides the natural JPanel paintComponent to paint the image
		 * over the entire panel
		 */
		public void paintComponent( Graphics g ) {
			// set the color and fill to create the border.
			// now actually draw the image
			g.drawImage(image,0,0,image.getWidth(),image.getHeight(), null);
		}
	}
	
	@SuppressWarnings("serial")
	private class NuanceImagePicPanel extends PicPanel {
		private BufferedImage paintedimage;
		
		public NuanceImagePicPanel(){
			super();
			image = displayimage;
		}
		public void paintComponent( Graphics g ) {
			// set the color and fill to create the border.
			// now actually draw the image
			g.drawImage(image,0,0,image.getWidth(),image.getHeight(), null);
			for (BufferedImage I : getWaveImages()){
				g.drawImage(I,0,0,image.getWidth(),image.getHeight(),null);	
			}
		}
		public BufferedImage getPaintedImage(){
			if (paintedimage == null)
				repaintPaintedImage();
			return paintedimage;
		}
		public void repaintPaintedImage(){
			paintedimage = new BufferedImage(displayimage.getWidth(), displayimage.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics g = paintedimage.getGraphics();
			paintComponent(g);			
		}
	}

	public synchronized Collection<BufferedImage> getWaveImages(){
		if (wave_images == null)
			BuildDisplayAndPixelDistrs();
		return wave_images.values();
	}
	
	public int getIntensityAtPoint(int i, int j, int wavenum){
		return nuanceimages.get(imagelist.getWaveName(wavenum)).get(i, j);
	}
	
	public HashMap<String, IntMatrix> getPixelData() { //basically - get scores
		return nuanceimages;
	}

	@Override
	public int getHeight() {
		return displayimage.getHeight();
	}

	@Override
	public int getWidth() {
		return displayimage.getWidth();
	}
	
	public BufferedImage getAsBufferedImage(){
		if (nuanceimagepicpanel == null)
			nuanceimagepicpanel = new NuanceImagePicPanel();
		return nuanceimagepicpanel.getPaintedImage();		
	}
	
	public Color getColorAt(Point t) {
		return new Color(getAsBufferedImage().getRGB(t.x,t.y));
	}
}