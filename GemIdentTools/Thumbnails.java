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

package GemIdentTools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.List;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import GemIdentImageSets.ImageAndScoresBank;
import GemIdentOperations.Run;

/**
 * Automatically generates thumbnails for every image
 * in the project folder upon project initialization.
 * Also provides support for generating a thumbnail
 * upon request
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class Thumbnails {
	
	/** The directory the thumbnails are kept in */
	public static final String thumbnailDir="thumbs";
	/** The list of images in the project directory */
	private static String[] filelist;

	/**
	 * Given a name of an image, it will get its thumbnail path and filename
	 * 
	 * @param imagefile		the name of an image
	 * @return				the path of its thumbnail relative to the project folder
	 */
	public static String getThumbnailFilename(String imagefile){
		return thumbnailDir+File.separator+"t"+imagefile.split("\\.")[0]+".jpg";
	}
	/**
	 * Given a directory, returns all the image files located inside
	 * 
	 * @param dir			the directory to search for images
	 * @return				the list of the image files
	 */
	public static String[] GetImageList(String dir){
		return (new File(dir+File.separator)).list(new ImageFileFilter());
	}
	/**
	 * The same as {@link #GetImageList(String) GetImageList} except
	 * this returns a collection instead of a Java array
	 * 
	 * @param dir		the directory to search for images
	 * @return			the collection of the image files
	 */
	public static ArrayList<String> GetImageListAsCollection(String dir){
		String[] files=GetImageList(dir);
		ArrayList<String> filesC=new ArrayList<String>(files.length);
		for (String file:files)
			filesC.add(file);
		return filesC;
	}
	/**
	 * This {@link java.io.FilenameFilter file filter} returns
	 * only image files of type "jpg", "tif", "tiff, and "bmp"
	 *
	 */
	public static class ImageFileFilter implements FilenameFilter{
		/**
		 * Given a file, returns true if it is an image
		 * 
		 * @param dir		the directory the file is located in
		 * @param name		the file itself
		 * @return			whether or not the file is an image
		 */
		public boolean accept(File dir, String name) {
			String[] fileparts=name.split("\\.");
			if (fileparts.length >= 2){
				String ext=fileparts[fileparts.length - 1].toLowerCase();
				if (ext.equals("jpg") || ext.equals("tif") || ext.equals("tiff") || ext.equals("TIFF") || ext.equals("bmp") || ext.equals("png"))
					return true;
				else 
					return false;
			}
			else return false;
		}		
	}
	
	/** the width of all thumbnails auto-generated */
	public static final int tWidth = 250;
	/** the height of all thumbnails auto-generated */
	public static final int tHeight = 150;
	/** whether or not the thumbnails have been created */
	private static boolean thumbs_loaded = false;
	
	
	/**
	 * Checks to see if thumbnails have already been loaded. If not,
	 * threads the creation process.
	 * 
	 * @param frame_to_center_on		the frame to center the progress bar around
	 */
	public static void CreateAll(JFrame frame_to_center_on) {
		if (!thumbs_loaded)
			new Thumbnails.ThumbnailBackgroundLoader(frame_to_center_on).start();

		thumbs_loaded=true;
	}
	
	
	/**
	 * Inner class to control thumbnail load and creation
	 * (without a separate class controlling this Swing was
	 * slowing down and <b>GemIdent</b> was unable to be used.
	 *
	 */
	public static class ThumbnailBackgroundLoader extends Thread {
		

		/**
		 * An object designed to streamline the thumbnail generation process
		 * by being used inside a {java.util.concurrent.ExecutorService threadpool}
		 */
		public static class MakeThumbnail implements Runnable{
			/** the original image */
			private BufferedImage image;
			/** the filename of the original image */
			private String file;
			/** whether or not the thumbnail should be saved */
			private boolean save;
			/** the thumbnail of the original image */
			private BufferedImage thumbnail;
			/** pointer to parent object */
			private ThumbnailBackgroundLoader loader;
			
			/**
			 * Creates a new object by opening the image, and changing the
			 * filename into the thumbnail filename
			 * 
			 * @param file			the filename of the oringal image
			 * @param save			whether or not the thumbnail should be saved
			 */
			public MakeThumbnail(String file, boolean save, ThumbnailBackgroundLoader loader) {
				this.file = file;
				this.save = save;
				this.loader = loader;
			}
			/** scales the image to tWidth x tHeight */
			public void run() {
				try {
					image = IOTools.OpenImage(file);			
					thumbnail = ScaleImage(image, (float)tWidth/image.getWidth(), (float)tHeight/image.getHeight());
					if (save){
						JAI.create("filestore", thumbnail, Run.it.imageset.getFilenameWithHomePath(getThumbnailFilename(file)), "JPEG");
					}
					if (loader != null){
						loader.update += 1 / ((double)filelist.length) * 100; //push up the update						
						loader.UpdateProgressBar();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}			
			}
			/** returns the thumbnail */
			public BufferedImage getResizedImage(){
				return thumbnail;
			}
		}		
		
		/** The current progress (of 100) */
		private double update;
		/** The progress bar */
		private JProgressBar progress;
		/** the dialog that holds the progress bar */
		private JFrame dialog;
		
		/**
		 * Creates a thumbnail directory in the project folder if
		 * necessary
		 * 
		 * @param frame_to_center_on		the frame to center progress bar about
		 */
		public ThumbnailBackgroundLoader(JFrame frame_to_center_on) {
			spawnProgressBarAndDependents(frame_to_center_on);
			//if the default thumbnail directory is nonexistent, create it.
			if ( !IOTools.DoesDirectoryExist(thumbnailDir))
				new File(Run.it.imageset.getFilenameWithHomePath(thumbnailDir)).mkdir();
		}
		
		//parameters for the progress bar
	    private static final String DialogTitle = "Creating Thumbnails (please wait)";
		private static final int dialog_width = 500;
		private static final int dialog_height = 50;
		
		/**
		 * Spawns the progress bar that tracks progress on the thumbnail creation
		 * 
		 * @param frame		the frame to center the progress bar about
		 */
	    private void spawnProgressBarAndDependents(JFrame frame) {
			dialog = new JFrame();
			
			//generate all the stuff for the progress bar frame
			update = 0; //reset the progress bar value
			progress = new JProgressBar();
			progress.setStringPainted(true);
			
			Point origin = frame.getLocation();
			origin.translate(frame.getSize().width / 2 - dialog_width / 2, frame.getSize().height / 2 - dialog_height / 2);
			dialog.setLocation(origin);
			dialog.setLayout(new BorderLayout());
			dialog.setTitle(DialogTitle);		
			dialog.add(progress, BorderLayout.CENTER);
			dialog.pack();
			dialog.setResizable(false);
			dialog.setVisible(false);
			dialog.setSize(new Dimension(dialog_width, dialog_height));
		}
	    
		private void UpdateProgressBar() {
			dialog.setVisible(true);
			progress.setValue((int)Math.round(update));		
		} 		
		/**
		 * Establishes a threadpool to generate each of the thumbnails, gets
		 * a list of the image files, then asks if the thumbnail exists, if so, it 
		 * caches it using {@link ImageAndScoresBank ImageAndScoresBank}, if not, it
		 * generates it using the {@link ThumbnailBackgroundLoader.MakeThumbnail MakeThumbnail} class.
		 */
		public void run() {
			
			final ExecutorService pool = Executors.newFixedThreadPool(Run.DEFAULT_NUM_THREADS);
			filelist=GetImageList(Run.it.imageset.getHomedir());
			
			for (final String filename : filelist) {
				//if the thumbnail doesn't exist yet, we'll queue it to be produced
				if (!IOTools.FileExists(getThumbnailFilename(filename))){
					pool.execute(new MakeThumbnail(filename, true, this));
				}
				else {	
					update += 1 / ((double)filelist.length) * 100; //push up the update
					UpdateProgressBar();
				}
			}
			pool.shutdown();
			//now wait for it to get done, but don't hold up main program:
			new Thread(){
				public void run(){
					try {	         
						pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS); //effectively infinity
				    } catch (InterruptedException ignored){}
				    dialog.dispose();
				    Run.it.imageset.ThumbnailsCompleted();
				}
			}.start();
		}
	}

	
	/**
	 * The meat of the whole class. Takes an image and creates a thumbnail from it.
	 * 
	 * As described in the Javadoc for the ScaleDescriptor (which outlines the specification for the scale operation), this is expected behavior, and thus not a bug. Here is a relevant excerpt from the Javadoc:
	 *
	 *	 * When interpolations which require padding the source such as Bilinear
	 *	 * or Bicubic interpolation are specified, the source needs to be extended
	 *	 * such that it has the extra pixels needed to compute all the destination
	 *	 * pixels. This extension is performed via the BorderExtender
	 *	 * class. The type of Border Extension can be specified as a
	 *	 * RenderingHint to the JAI.create method.
	 *	 *
	 *	 * If no Border Extension is specified, the source will not be extended.
	 *	 * The scaled image size is still calculated according to the formula
	 *	 * specified above. However since there isn't enough source to compute all the
	 *	 * destination pixels, only that subset of the destination image's pixels,
	 *	 * which can be computed, will be written in the destination. The rest of the
	 *	 * destination will not be written.
     *	
	 *	As described above, it is the part of the image that will not be written, that shows up as 
	 *	"black pixels" in the image. A "BorderExtender" RenderingHint can be supplied to provide 
	 *	extra source, so that all the pixels that fall within the destination bounds are written. 
	 *	No black pixels will result in this case. Accordingly, this bug is being closed as not a bug.
	 *	
	 *	Note that these issues have also been extensively discussed in the jai-interest mailing 
	 *	list, and are archived. The archive is a good resource to consult and may help quickly 
	 *	resolve issues. 
     *
	 * @param image				the original image
	 * @param scaledWidth		the width scale (required)
	 * @param scaledHeight		the height scale (not required, if null uses aspect ratio)
	 * @return					the thumbnail
	 */
	public static BufferedImage ScaleImage(BufferedImage image, Float scaledWidth, Float scaledHeight){
		
		if (scaledWidth == 1 && scaledHeight == 1) //duh
			return image;
		
		if (scaledHeight == null){
			float scale=scaledWidth/((float)image.getWidth());
			scaledHeight=((float)image.getHeight())*scale;
		}
		
		ParameterBlock pb=new ParameterBlock(); //I don't know how this works, don't ask
		pb.addSource(image);    // The source image
		pb.add(scaledWidth);    // The xScale
		pb.add(scaledHeight);   // The yScale
		pb.add(0.0F);           // The x translation
		pb.add(0.0F);           // The y translation
		pb.add(Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2));
		
		RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,BorderExtender.createInstance(BorderExtender.BORDER_WRAP));
		
		return JAI.create("scale", pb, hints).getAsBufferedImage();
	}
	/** an convenience method to create thumbnails */
	public static BufferedImage makeIntoThumbnail(String filename) {
		ThumbnailBackgroundLoader.MakeThumbnail thumb = new ThumbnailBackgroundLoader.MakeThumbnail(filename, false, null);
		thumb.run(); //obviously
		return thumb.getResizedImage();
	}
	/** a convenience method to create thumbnails that display "identified" across them */
	public static BufferedImage makeIntoThumbnailWithI(String filename){
		BufferedImage thumb=makeIntoThumbnail(filename);
		Graphics g=thumb.getGraphics();
		g.setColor(Color.BLACK);
		g.setFont(new Font("SansSerif",Font.BOLD,50));
		g.drawString("identified",15,tHeight/2-5);
		return thumb;
	}	
	
	/**
	 * Creates the thumbnail chooser dialog box when user selects "Add Image"
	 * in the {@link GemIdentView.KTrainPanel#AddNewThumbnail training panel}
	 */
	private static class ThumbnailChooser extends JDialog{

		/** The entire viewable pane */
		private ContentPane contentPane;
		/** the width of the list component that shows images */
//		public static final int ListWidth=250;
		/** the height of the list component is tHeight plus this */
//		public static final int ListHeightAddl=100;
		/** the dimension of the list component */
//		public static final Dimension ListSize=new Dimension(ListWidth,tHeight+ListHeightAddl);
		/** the dimension of the thumbnail viewer */
		public static final Dimension ViewerSize=new Dimension(tWidth+10,tHeight+10);
		
		/**
		 * An extension of JPanel to display images over the panel
		 */
		private class PicPanel extends JPanel {
			/** the image to paint over the panel */
			private BufferedImage image;
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
			}			
			/**
			 * Overrides the natural JPanel paintComponent to paint the image
			 * over the entire panel
			 */
			public void paintComponent( Graphics g ) {
				// set the color and fill to create the border.
				g.setColor(contentPane.getBackground());
				g.fillRect(0,0,ThumbnailChooser.ViewerSize.width,ThumbnailChooser.ViewerSize.height);
				
				// now actually draw the image
				g.drawImage(image,5,5,
					ViewerSize.width-10,
					ViewerSize.height-10,null);
			}
		}
		
		/**
		 * An extension of JPanel to house a list component and a PicPanel
		 * and to provide all the functionality
		 */
		private class ContentPane extends JPanel{
			
			/** the list of images */
			private Collection<String> filenames;
			/** the load button that will close the dialog and load the image as a thumbnail */
			private JButton load;
			/** the cancel button which will void all actions */
			private JButton cancel;
			/** the list component - responsible for displaying the images to the user */
			private List list;
			/** the panel responsible for displaying the thumbnail selected */
			private PicPanel viewer;
			/** the user's current selection */
			private String imageToLoad;
			
			
			/**
			 * Creates the pane.
			 * 
			 * @param filenames		the list of image files
			 */
			public ContentPane( Collection<String> filenames ) {
				super();
				
				this.filenames = filenames;
				setLayout(new BorderLayout());
				setVisible(true);
				setResizable(false);
				this.setUpWest();
				SetUpViewer();
				pack();
			}
			
			/** creates the west portion - the list and the buttons */
			private void setUpWest() {
				this.SetUpButtons();
				this.SetUpList();
				
				Box outer = Box.createVerticalBox();
				Box inner_bottom = Box.createHorizontalBox();
				
				inner_bottom.add(this.load);
				inner_bottom.add(this.cancel);
				
				outer.add(this.list);
				outer.add(inner_bottom);
				
				this.add(outer,BorderLayout.WEST);
			}
			
			
			/** Add buttons to layout */
			private void SetUpButtons() {
				this.load = new JButton("Load");
				this.load.setEnabled(false);
				this.load.addActionListener(
					new ActionListener() {
						public void actionPerformed( ActionEvent e ) {
							Load();						
						}
					}
				);
				
				this.cancel = new JButton("Cancel");
				this.cancel.addActionListener(
					new ActionListener() {
						public void actionPerformed( ActionEvent e ) {
							Cancel();							
						}
					}
				);
			}
			/** Initializes the list and adds appropriate listeners */
			private void SetUpList() {
				list=new List(5,false);
				for (String filename:filenames)
					list.add(filename);
				
//				this.list.setMinimumSize(ShowAllThumbnails.ListSize);
//				list.setPreferredSize(ShowAllThumbnails.ListSize);
				list.addItemListener(
					new ItemListener(){
						public void itemStateChanged(ItemEvent e){
							load.setEnabled(true);
							String filename=list.getSelectedItem();
							try {
								viewer.setImage(ImageAndScoresBank.getOrAddThumbnail(filename));
							} catch (IllegalArgumentException ex){} //for Nuance image sets, there are no thumbnails yet
						}
					}
				);
				list.addKeyListener(
					new KeyListener(){
						public void keyPressed(KeyEvent e){
							String selected=list.getSelectedItem();
							if (e.getKeyCode() == KeyEvent.VK_ENTER && selected != null)
								Load();	
						}
						public void keyReleased(KeyEvent e){}
						public void keyTyped(KeyEvent e){}
					}
				);
			}			
			/** Builds the image preview pane. */
			public void SetUpViewer() {
				this.viewer = new PicPanel();
				this.viewer.setPreferredSize(ThumbnailChooser.ViewerSize);
				
				Box b = Box.createHorizontalBox();
				b.add(viewer); //hopefully center it
				b.setAlignmentX(Box.CENTER_ALIGNMENT);
				b.setAlignmentY(Box.CENTER_ALIGNMENT);
				
				this.add(b,BorderLayout.CENTER);				
			}
			
			
			/**
			 * Close the selection dialog.
			 *
			 */
			private void Cancel() {
				this.imageToLoad = null;
				dispose();	
			}
			
			
			/**
			 * Sets the selection response and closes
			 * the selection dialog.
			 *
			 */
			private void Load() {			
				imageToLoad = list.getSelectedItem();
				dispose();
			}
			
			
			/**
			 * Returns the selection response.
			 * @return 	the image to load
			 */
			public String getAnswer() {
				return imageToLoad;
			}		
		}
		
		/**
		 * Initialize a thumbnail chooser dialog box
		 * 
		 * @param filenames		the images to choose from
		 */
		public ThumbnailChooser(Collection<String> filenames){
			super(Run.it.getGUI(),"Pick New Image",true);			
			contentPane=new ContentPane(filenames);
            setContentPane(contentPane);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            pack();
            setLocationRelativeTo(Run.it.getGUI());
            setVisible(true);		
		}
		
		
		/**
		 * Returns the selection choice.
		 * @return	the image to load
		 */
		public String getAnswer() {
			return contentPane.getAnswer();			
		}
	}

	
	/**
	 * Provides the user with a dialog box of images to load. Returns
	 * the filename corresponding to the selection (or null if none)
	 * @param namesAlreadyPresent		the images not to display in the list
	 * @return							the image to load
	 */
	public static String ChooseThumbnail( Collection<String> namesAlreadyPresent ) {
		return new ThumbnailChooser(getAllAvailableImageFiles(namesAlreadyPresent)).getAnswer();
	}
	/**
	 * Get all available image files (the Nuance set is different)
	 * 
	 * @param namesAlreadyPresent 		images not to display
	 * @return							the collection of images to display
	 */
	private static Collection<String> getAllAvailableImageFiles(Collection<String> namesAlreadyPresent){
		Collection<String> all = new ArrayList<String>();
		for ( String filename:Run.it.imageset.GetImages() )
			all.add(filename);
		all.removeAll(namesAlreadyPresent);
		return all;
	}
	/**
	 * Gets the number of image files not loaded thus far
	 * 
	 * @param namesAlreadyPresent		the images already loaded
	 * @return							the number of images left in the directory
	 */
	public static int getTotNumImageFiles(Collection<String> namesAlreadyPresent){		
		return getAllAvailableImageFiles(namesAlreadyPresent).size();
	}
}