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

package GemIdentImageSets.Bliss;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.ImageSetInterface;
import GemIdentImageSets.ImageSetInterfaceWithUserColors;
import GemIdentImageSets.MiniImageInImageSet;
import GemIdentImageSets.RegularSubImage;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.IOTools;
import GemIdentTools.Matrices.StringMatrix;
import GemIdentTools.Thumbnails;

/**
 * Contains information on image sets created
 * with the BLISS workstation - a product of Bacus
 * Laboratories, Inc. The class is able to support
 * both the old image sets and the new ones (depending
 * upon which BLISS workstation created the sets) via
 * two daughter classes.
 * 
 * This class is not documented fully.
 * 
 * @author Adam Kapelner
 */
public abstract class BlissImageList extends ImageSetInterfaceWithUserColors implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String INIT_FILENAME="FinalScan.ini";

	/** The magnification at 20X in microns/pixel (conversion got from Bacus Labs) */
	private static final double MAGN_AT_20X=.47;
	/** The magnification at 40X in microns/pixel (conversion got from Bacus Labs) */
	private static final double MAGN_AT_40X=.23;

	
	protected static int NUM_GRID_PER_IMAGE_X;
	protected static int NUM_GRID_PER_IMAGE_Y;

	
	/**
	 * Contains information on each image created
	 * by the BLISS workstation. This is basically
	 * a dumb struct which relied on the default 
	 * constructor
	 *
	 */
	protected class BlissImage implements MiniImageInImageSet {
		/** The filename of the image */
		public String filename; 
		/** The number of the image (the xx in Daxx.jpg) also called the "picture number" henceforth */
		public int num; 
		/** The raw x location of this image in arbitrary grid units */
		public int rawx;
		/** The raw y location of this image in arbitrary grid units */
		public int rawy;
		/** The raw x value where the minimum has been set to zero */
		public int posrawx; 
		/** The raw y value where the minimum has been set to zero */
		public int posrawy;
		/** The posraw x value flipped about its center value */
		public int standardx;
		/** The posraw y value flipped about its center value */
		public int standardy;
		/** The x value in true pixels */
		public int pixelx;
		/** The y value in true pixels */
		public int pixely;
		/** In the matrix of individual images within the global image, this is the x location of this image */
		public int piclocx;
		/** In the matrix of individual images within the global image, this is the y location of this image */
		public int piclocy;
		
		/** for debugging purposes exclusively
		 * 
		 * @return			all fields of class in a conveniently formatted string
		 */
		public String toString(){
			String S="";
			S=S+"filename:"+filename+"\n";
			S=S+"num:"+num+"\n";
			S=S+"rawx:"+rawx+"\n";
			S=S+"rawy:"+rawy+"\n";
			S=S+"posrawx:"+posrawx+"\n";
			S=S+"posrawy:"+posrawy+"\n";
			S=S+"standardx:"+standardx+"\n";
			S=S+"standardy:"+standardy+"\n";
			S=S+"pixelx:"+pixelx+"\n";
			S=S+"pixely:"+pixely+"\n";
			S=S+"piclocx:"+piclocx+"\n";
			S=S+"piclocy:"+piclocy+"\n";
			return(S);
		}

		public int xLoc() {
			return pixelx;
		}

		public int yLoc() {
			return pixely;
		}
	}
	
	protected ArrayList<BlissImage> list;
	protected StringMatrix picFilenameTable;
	
	protected int xo;
	protected int xf;
	protected int yo;
	protected int yf;
	
	
	/** creates a new BLISS Image list - a list of BlissImage objects */
	public BlissImageList(String homedir){
		super(homedir);
//		char mu=new Character('03BC');
		W=752; //set the width height immediately
		H=480;
		measurement_unit="\u03BCm"; 
		list=new ArrayList<BlissImage>();
		try {
			GetRawInfoFromInitFile(); //list is built in sequential order starting at 0,...,N
		} catch (IOException e){
			return;
		}
		FindMinsAndMaxs();
		ProcessList();
		GetRowsAndCols();
		MakeNumTable();
	}

	/**
	 * Processes the input file to create the list of image data
	 */
	protected abstract void ProcessList();

	private void GetRawInfoFromInitFile() throws IOException{
		boolean imageFormat=false;
		boolean stepSizes=false;
		boolean magn=false;
		BufferedReader in = new BufferedReader(new FileReader(getFilenameWithHomePath(INIT_FILENAME)));
		while (true) {
			String line = in.readLine();
			
			if (line == null) {
				break;
			}
			if (!magn) 
				magn=GetMagnFormat(line,in);
			if (!imageFormat) 
				imageFormat=GetImageFormat(line,in);
			if (!stepSizes)
				stepSizes=GetStepSizes(line,in);
			if (imageFormat && stepSizes)
				ProcessIndividualGroup(line,in);
		}
		in.close();
	}
	private boolean GetMagnFormat(String line, BufferedReader in) {
		if (line.length() > 14){ //		tImageType=.bmp		
			if (line.substring(0,14).equals("dMagnification")){
				int magn=Integer.parseInt(line.substring(15,line.length()));
				switch (magn){
					case 20: distance_to_pixel_conversion=MAGN_AT_20X; break;
					case 40: distance_to_pixel_conversion=MAGN_AT_40X; break;
					default: distance_to_pixel_conversion=null; break;
				}
				return true;
			}
		}
		return false;
	}
	private boolean GetStepSizes(String line, BufferedReader in) {
		if (line.length() > 13){ //		lXStepSize=1594
			if (line.substring(0,10).equals("lXStepSize")){
				NUM_GRID_PER_IMAGE_Y=Integer.parseInt(line.substring(11,line.length())); //BLISS switches x&y also
				return false;
			}
			else if (line.substring(0,10).equals("lYStepSize")){//		lYStepSize=1182
				NUM_GRID_PER_IMAGE_X=Integer.parseInt(line.substring(11,line.length()));//BLISS switches x&y
				return true;
			}
		}
		return false;	
	}
	private boolean GetImageFormat(String line, BufferedReader in) {
		if (line.length() > 14){ //		tImageType=.bmp		
			if (line.substring(0,10).equals("tImageType")){
				image_format=line.substring(12,15);
				return true;
			}
		}
		return false;
	}
	private void ProcessIndividualGroup(String line, BufferedReader in) throws IOException {
		if (line.length() == 0)
			return;
		if (line.substring(0,1).equals("[") && !line.substring(1,2).equals("H")){ //if it's a subtitle of a new filename . . .
			
			BlissImage blissImage=new BlissImage();
			
			blissImage.filename=line.substring(1,line.length()-1)+"."+image_format.toLowerCase();
			if (!IOTools.FileExists(blissImage.filename)){
				System.out.println("Bliss image set incomplete: could not find "+blissImage.filename);
				System.exit(0);
			}
			blissImage.num=Integer.parseInt(line.substring(3,line.length()-1));
			
			
			while (true){
				line = in.readLine();
				if (line.length() > 0)
					break;
			}
			
			
			blissImage.rawy=Integer.parseInt(line.substring(2,line.length())); //BLISS switches x&y
			
			while (true){
				line = in.readLine();
				if (line.length() > 0)
					break;
			}
			
			blissImage.rawx=Integer.parseInt(line.substring(2,line.length())); //BLISS switches x&y
			
			for (int i=0;i<3;i++) //skip three lines
				in.readLine();
			
			list.add(blissImage);
		}	
	}
	private void FindMinsAndMaxs(){

		xmin = ymin = Integer.MAX_VALUE;
		xmax = ymax = Integer.MIN_VALUE;
	
		for (BlissImage I:list){ //find mins
			if (I.rawx < xmin) xmin=I.rawx;
			if (I.rawy < ymin) ymin=I.rawy;
		}
		for (BlissImage I:list){ //save posraw's
			I.posrawx=I.rawx-xmin;
			I.posrawy=I.rawy-ymin;
		}
		for (BlissImage I:list){ //find maxs
			if (I.posrawx > xmax) xmax=I.posrawx;
			if (I.posrawy > ymax) ymax=I.posrawy;
		}
	}

	private void GetRowsAndCols(){
		height=width=Integer.MIN_VALUE;
		for (BlissImage I:list){ //find maxs
			if (I.piclocx > height) height=I.piclocx;
			if (I.piclocy > width) width=I.piclocy;
		}
		width++; //start at zero . . .
		height++; //start at zero . . .		
	}
	private void MakeNumTable() {
		picFilenameTable=new StringMatrix(height,width,PIC_NOT_PRESENT);
		for (BlissImage I:list)
			picFilenameTable.set(I.piclocx,I.piclocy,I.filename);		
	}
	private Point FindLocalLocation(String filename){
		for ( int i = 0; i < height; i++ )
			for ( int j = 0; j < width; j++ )
				if ( filename.equals(picFilenameTable.get(i,j)))
					return new Point(i,j);
		return null;
	}
	
	/** given a picture number, this will 
	 * find the location in the global image 
	 * and return a 3x3 matrix of the surrounding 
	 * picture numbers ("BAD_PIC" is
	 * returned if picture is non-existent or outside
	 * the dimensions of the global image).
	 * 
	 * @return a 3x3 StringMatrix object
	 */
	public StringMatrix GetLocalPics(String filename, Integer notused){
		Point t=FindLocalLocation(filename);
		
		if (t == null)
			return null;
		StringMatrix local=new StringMatrix(3,3);
		local.set(1,1,picFilenameTable.get(t.x,t.y)); 
		try { 
			local.set(0,1,picFilenameTable.get(t.x-1,t.y)); 
		} catch (ArrayIndexOutOfBoundsException e){
			local.set(0,1,PIC_NOT_PRESENT);
		}
		try { 
			local.set(2,1,picFilenameTable.get(t.x+1,t.y)); 
		} catch (ArrayIndexOutOfBoundsException e){
			local.set(2,1,PIC_NOT_PRESENT);
		}
		try { 
			local.set(1,0,picFilenameTable.get(t.x,t.y-1)); 
		} catch (ArrayIndexOutOfBoundsException e){
			local.set(1,0,PIC_NOT_PRESENT);
		}
		try { 
			local.set(1,2,picFilenameTable.get(t.x,t.y+1)); 
		} catch (ArrayIndexOutOfBoundsException e){
			local.set(1,2,PIC_NOT_PRESENT);
		}
		try { 
			local.set(0,0,picFilenameTable.get(t.x-1,t.y-1)); 
		} catch (ArrayIndexOutOfBoundsException e){
			local.set(0,0,PIC_NOT_PRESENT);
		}
		try { 
			local.set(0,2,picFilenameTable.get(t.x-1,t.y+1)); 
		} catch (ArrayIndexOutOfBoundsException e){
			local.set(0,2,PIC_NOT_PRESENT);
		}
		try { 
			local.set(2,0,picFilenameTable.get(t.x+1,t.y-1)); 
		} catch (ArrayIndexOutOfBoundsException e){
			local.set(2,0,PIC_NOT_PRESENT);
		}
		try { 
			local.set(2,2,picFilenameTable.get(t.x+1,t.y+1)); 
		} catch (ArrayIndexOutOfBoundsException e){
			local.set(2,2,PIC_NOT_PRESENT);
		}
		return local;
	}
	/** given a picture number, this create
	 * the string of the filename (xx -> "Daxx.jpg").
	 * If the number is a "BAD_PIC" it will return
	 * "blank.bmp"
	 * 
	 * @return the filename
	 */
	public String getBlissFilename(int n){
		if (n == BAD_PIC || n >= list.size()) 
			return PIC_NOT_PRESENT;
		else 
			return "Da"+n+"."+image_format;
	}
//	public static int getBlissNumber(String filename){
//		return Integer.parseInt(filename.substring(3,filename.length()-1));
//	}
	public int getBlissNumberFullFilename(String filename){
		Pattern p;
		Matcher m;
		p=Pattern.compile("\\d+.",Pattern.CASE_INSENSITIVE);
		m=p.matcher(filename);		
		if (m.find())
			return Integer.parseInt(filename.substring(m.start(),m.end()-1)); //"-1" to get rid of period
		return BAD_PIC;
	}
	/** given a picture number and a coordinate within
	 * the picture, this will return the global coordinate
	 * within the global picture
	 * 
	 * @return the global coordinate
	 */
	public Point getTrueLocation(String filename,Point to,boolean excise){
		Point L=FindLocalLocation(filename);
		if (L.x == BAD_PIC || L.y == BAD_PIC)
			return null;
		else
			return getTrueLocation(L, to, excise);
	}	

	public int getGlobalHeight(boolean excise){
		int H=BlissImageList.H;
		int yo=getYo();
		H-=yo;
		int yf=BlissImageList.H-getYf();
		H-=yf;
		
		if (excise) return height*H;
		else		return height*BlissImageList.H;
	}
	public int getGlobalWidth(boolean excise){
		int W=BlissImageList.W;
		int xo=getXo();
		W-=xo;
		int xf=BlissImageList.W-getXf();
		W-=xf;
		
		if (excise) 
			return width*W;
		else		
			return width*BlissImageList.W;
	}

	public void CreateHTMLForComposite(int tWidth,int tHeight){	
		int factor=0;
		for (ImageSetInterface.Size size:ImageSetInterface.Size.values()){			
			switch (size){
				case XL: 	factor=1; break;
				case L: 	factor=2; break;
				case M: 	factor=4; break;
				case S: 	factor=8; break;
				case XS: 	factor=16; break;
				case XXS: 	factor=32; break;
			}
			
			PrintWriter out=null;
			try {
				out=new PrintWriter(new BufferedWriter(new FileWriter(getFilenameWithHomePath(GlobalFilename+size+".html"))));
			} catch (IOException e) {
				System.out.println(GlobalFilename+size+".html cannot be created");
			}
			out.print("<HTML>");
			out.print("\n");
			out.print("<head>");
			out.print("\n");
			out.print("<title>Global Image</title>");
			out.print("\n");
			out.print("</head>");
			out.print("\n");
			String link="Zoom Level: ";
			for (ImageSetInterface.Size linkSize:ImageSetInterface.Size.values())
				link+="<a href=\""+GlobalFilename+linkSize+".html\">"+linkSize+"</a>&nbsp";
			out.print(link);
			out.print("<br>");
			out.print("\n");
			out.print("<br>");
			out.print("\n");
			out.print("<br>");
			out.print("\n");
			out.print("<table cellspacing=0 cellpadding=0>");
			out.print("\n");
			
			for (int i=0;i<height;i++){
				out.print("<tr>");
				out.print("\n");
				for (int j=0;j<width;j++){
					out.print("<td>");
					String filename=picFilenameTable.get(i,j);
					if (filename != PIC_NOT_PRESENT)
						out.print("<a href=\""+filename+"\"><img src="+Thumbnails.getThumbnailFilename(filename)+" border=0 alt="+filename+" width="+(tWidth/factor)+" height="+(tHeight/factor)+"></a>");
					out.print("</td>");
					out.print("\n");
				}
				out.print("</tr>");
				out.print("\n");
			}
			out.print("</table>");
			out.print("\n");
			out.print("</HTML>");
			out.close();
		}
		IOTools.RunProgramInOS(GlobalFilename+"M.html");
	}

	/**
	 * An array with the filenames of all the subimages in their proper global context is
	 * essential to piecing the images together (to be overriden)
	 * 
	 * @return			a matrix of filenames of the subimages in their global context. If a file
	 * 					is not present at a given location, the PIC_NOT_PRESENT will be in its place
	 */
	public StringMatrix getPicFilenameTable(){
		return picFilenameTable;
	}

	public BufferedImage getGlobalImageSlice(int rowA,int rowB) {
		BufferedImage partial=new BufferedImage(getGlobalWidth(true),rowB-rowA,BufferedImage.TYPE_INT_RGB);
		
		int startImageRow=(int)Math.floor(rowA/((double)(getYf()-getYo())));
		int endImageRow=(int)Math.ceil(rowB/((double)(getYf()-getYo())));
		
		int startPixelRow=rowA-(getYf()-getYo())*startImageRow;
		int endPixelRow=rowB-(getYf()-getYo())*(endImageRow-1);

		int row=0;
		int col=0;
		
		for (int jpic=0;jpic<picFilenameTable.getHeight();jpic++){
			
			String filename=picFilenameTable.get(startImageRow,jpic);
			if (filename != PIC_NOT_PRESENT){
				BufferedImage image=ImageAndScoresBank.getOrAddDataImage(filename).getAsBufferedImage();
				for (int j=0;j<getXf()-getXo();j++){
					for (int i=startPixelRow;i<getYf()-getYo();i++){
						try {
							int rgb=image.getRGB(j,i);
							partial.setRGB(j+col,row+i-startPixelRow,rgb);
						}
						catch (Exception e){}
					}
				}
			}
			col+=(getXf()-getXo());
		}
		row+=(getYf()-getYo());
		col=0;
		for (int ipic=startImageRow+1;ipic<(endImageRow-1);ipic++){
			for (int jpic=0;jpic<picFilenameTable.getHeight();jpic++){
				String filename=picFilenameTable.get(ipic,jpic);
				if (filename != PIC_NOT_PRESENT){
					BufferedImage image=ImageAndScoresBank.getOrAddDataImage(filename).getAsBufferedImage();
					for (int j=0;j<getXf()-getXo();j++)
						for (int i=0;i<getYf()-getYo();i++){
							try {
								partial.setRGB(j+col,row+i-startPixelRow,image.getRGB(j,i));
							}
							catch (Exception e){}
						}
				}
				col+=(getXf()-getXo());
			}
			col=0;
			row+=(getYf()-getYo());
		}
		for (int jpic=0;jpic<picFilenameTable.getHeight();jpic++){
			String filename=picFilenameTable.get(endImageRow-1,jpic);
			if (filename != PIC_NOT_PRESENT){
				BufferedImage image=ImageAndScoresBank.getOrAddDataImage(filename).getAsBufferedImage();
				for (int j=0;j<getXf()-getXo();j++)
					for (int i=0;i<=endPixelRow;i++){					
						try {
							partial.setRGB(j+col,row+i-startPixelRow,image.getRGB(j,i));
						}
						catch (Exception e){}
					}
			}
			col+=(getXf()-getXo());
		}
		return partial;
	}

	@Override
	public String getInitializationFilename(){
		return INIT_FILENAME;
	}
	
	/**
	 * Creates a thumbnail of the global images, not valid in all image sets
	 * 
	 * @param approxGlobalScaledWidth	the width of the thumbnail
	 * @return							the thumbnail itself
	 */
	public BufferedImage getGlobalThumbnailImage(Float approxGlobalScaledWidth){
//		if (IOTools.DoesFileExist(globalFilename))
//			return IOTools.OpenImage(globalFilename);
//		else {
			StringMatrix images=getPicFilenameTable();
			int imagesW=images.getWidth();
			int imagesH=images.getHeight();
			float totW=imagesW*Thumbnails.tWidth;
//			float totH=imagesH*tHeight;
			float scale=approxGlobalScaledWidth/((float)totW);

			int newThumbW=(int)Math.ceil(Thumbnails.tWidth*scale);
			int newThumbH=(int)Math.ceil(Thumbnails.tHeight*scale);
			
			int globalW=newThumbW*imagesH; //dont' ask.....
			int globalH=newThumbH*imagesW;
			BufferedImage global=new BufferedImage(globalW,globalH,BufferedImage.TYPE_INT_RGB);

			for (int i=0;i<imagesW;i++){
				for (int j=0;j<imagesH;j++){
					String filename=images.get(i,j);
					if (filename != PIC_NOT_PRESENT){
						BufferedImage thumb = null;
						try {
							thumb = IOTools.OpenImage(Thumbnails.getThumbnailFilename(filename));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						BufferedImage scaled=Thumbnails.ScaleImage(thumb, newThumbW, newThumbH);
						for (int x=0;x<newThumbW;x++)
							for (int y=0;y<newThumbH;y++)
								try {
									int globalX=j*(newThumbW-1)+x;
									int globalY=i*(newThumbH-1)+y;
									global.setRGB(globalX,globalY,scaled.getRGB(x,y));
								} catch (Exception e){}
					}
				}
			}			
//			white background
			for (int i=0;i<globalW;i++)
				for (int j=0;j<globalH;j++)
					if (global.getRGB(i,j) == BoolMatrix.BlackRGB)
						global.setRGB(i,j,BoolMatrix.WhiteRGB);
			IOTools.WriteImage(globalFilenameAndPath,"JPEG",global); //save for later use
			return global;
//		}
	}

	@Override
	public ArrayList<String> GetImages() {
		return null;
	}

	@Override
	public Set<String> getFilterNames() {
		return stains.keySet();	
	}
	public int getXo() {
		return xo;
	}

	public int getXf() {
		return xf;
	}

	public int getYo() {
		return yo;
	}

	public int getYf() {
		return yf;
	}
	
	public DataImage getDataImageFromFilename(String filename){
		return new RegularSubImage(filename,true);
	}
	
	public ArrayList<BlissImage> getList() {
		return list;
	}

	public void setList(ArrayList<BlissImage> list) {
		this.list = list;
	}
	
	@Override
	public HashSet<String> getClickedonimages() {
		return null;
	}
	
	@Override
	public void spawnSophisticatedHelper() {}
	
	@Override
	public void presave(){}
	
	public Color getWaveColor(String wave_name){
		return null;
	}	
	@Override
	public void RunUponNewProject() {}
	
	public void ThumbnailsCompleted(){}

}