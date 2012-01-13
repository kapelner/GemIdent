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

package GemIdentImageSets;

import java.awt.Point;
import java.awt.image.BufferedImage;

import GemIdentOperations.Run;
import GemIdentTools.Matrices.StringMatrix;

/**
 * An implementation of a {@link RegularSubImage RegularSubImage} that contains pixel information
 * on its edges of the surrounding images
 * 
 * @author Adam Kapelner
 */
public final class SuperRegularImage extends RegularSubImage implements SuperImage {

	/** the center image */
	private RegularSubImage C;
	/** the number of surrounding pixels */
	private int c;
	/** the locality of the center image */
	private StringMatrix localPics;
	/** width of center image */
	private int w;
	/** height of center image */
	private int h;
	
	/**
	 * Create a "super image" - an image that includes pieces of the eight surrounding images
	 * 
	 * This constructor handles all the gymnastics to load the surrounding images and copy in the
	 * relevant portions, and if it does not exist, to reflect the pixel data from the original
	 * image
	 * 
	 * @param filename		the filename of the image to create a super image of
	 */
	public SuperRegularImage(String filename) {
		
		this.C = (RegularSubImage)ImageAndScoresBank.getOrAddDataImage(filename);
		this.filename = filename;
		w = C.getWidth();
		h = C.getHeight();
		
		// get global image context
		localPics = (StringMatrix)Run.it.imageset.GetLocalPics(filename, null);

		//get number of pixels to surround by
		c = Run.it.getMaxPhenotypeRadiusPlusMore(null)+15;
		
		//initialize the SuperImage
		displayimage=new BufferedImage(w+2*c,h+2*c,BufferedImage.TYPE_INT_RGB);
		
		//copy C in the middle
		for (int i=c;i<c+w;i++)
			for (int j=c;j<c+h;j++)
				displayimage.setRGB(i,j,C.getRGB(i-c,j-c));
		
		//copy surrounding images into the superimage
		String NWs = null;
		if (localPics != null){
			NWs=localPics.get(0,0);
		}
		else {
			NWs = ImageSetInterface.PIC_NOT_PRESENT;
		}
		if (NWs.equals(ImageSetInterface.PIC_NOT_PRESENT)){
			for (int i=c;i<c+c;i++)
				for (int j=c;j<c+c;j++)
					displayimage.setRGB(-(i-c)+c,c-j+c,displayimage.getRGB(i,j));
		}
		else {
			RegularSubImage NW=new RegularSubImage(NWs,true);	
			for (int i=w-c;i<w;i++)
				for (int j=h-c;j<h;j++)
					displayimage.setRGB(i-w+c,j-h+c,NW.getRGB(i,j));
		}
		
		String Ns = null;
		if (localPics != null){
			Ns=localPics.get(0,1);
		}
		else {
			Ns = ImageSetInterface.PIC_NOT_PRESENT;
		}
		if (Ns.equals(ImageSetInterface.PIC_NOT_PRESENT)){
			for (int i=c;i<w+c;i++)
				for (int j=c;j<c+c;j++)
					displayimage.setRGB(i,c-j+c,displayimage.getRGB(i,j));
		}
		else {
			RegularSubImage N=new RegularSubImage(Ns,true);
			for (int i=0;i<w;i++)
				for (int j=h-c;j<h;j++)
					displayimage.setRGB(i+c,j-h+c,N.getRGB(i,j));
		}
		
		String NEs = null;
		if (localPics != null){
			NEs=localPics.get(0,2);
		}
		else {
			NEs = ImageSetInterface.PIC_NOT_PRESENT;
		}
		if (NEs.equals(ImageSetInterface.PIC_NOT_PRESENT)){
			for (int i=w;i<w+c;i++)
				for (int j=c;j<c+c;j++)
					displayimage.setRGB(-i+w+c+w+c-1,c-j+c,displayimage.getRGB(i,j));
		}
		else {
			RegularSubImage NE=new RegularSubImage(NEs,true);
			for (int i=0;i<c;i++)
				for (int j=h-c;j<h;j++)
					displayimage.setRGB(i+w+c,j-h+c,NE.getRGB(i,j));
		}
		
		String Ws = null;
		if (localPics != null){
			Ws=localPics.get(1,0);
		}
		else {
			Ws = ImageSetInterface.PIC_NOT_PRESENT;
		}
		if (Ws.equals(ImageSetInterface.PIC_NOT_PRESENT)){
			for (int i=c;i<c+c;i++)
				for (int j=c;j<h+c;j++)
					displayimage.setRGB(-(i-c)+c,j,displayimage.getRGB(i,j));
		}
		else {
			RegularSubImage W=new RegularSubImage(Ws,true);
			for (int i=w-c;i<w;i++)
				for (int j=0;j<h;j++)
					displayimage.setRGB(i-w+c,j+c,W.getRGB(i,j));
		}
		
		String Es = null;
		if (localPics != null){
			Es=localPics.get(1,2);
		}
		else {
			Es = ImageSetInterface.PIC_NOT_PRESENT;
		}
		if (Es.equals(ImageSetInterface.PIC_NOT_PRESENT)){
			for (int i=w;i<w+c;i++)
				for (int j=c;j<h+c;j++)
					displayimage.setRGB(-i+w+c+w+c-1,j,displayimage.getRGB(i,j));
		}
		else { 
			RegularSubImage E=new RegularSubImage(Es,true);
			for (int i=0;i<c;i++)
				for (int j=0;j<h;j++)
					displayimage.setRGB(i+w+c,j+c,E.getRGB(i,j));
		}
		
		String SWs = null;
		if (localPics != null){
			SWs=localPics.get(2,0);
		}
		else {
			SWs = ImageSetInterface.PIC_NOT_PRESENT;
		}
		if (SWs.equals(ImageSetInterface.PIC_NOT_PRESENT)){
			for (int i=c;i<c+c;i++)
				for (int j=h;j<h+c;j++)
					displayimage.setRGB(-(i-c)+c,-j+h+c+h+c-1,displayimage.getRGB(i,j));
		}
		else {
			RegularSubImage SW=new RegularSubImage(SWs,true);
			for (int i=w-c;i<w;i++)
				for (int j=0;j<c;j++)
					displayimage.setRGB(i-w+c,j+h+c,SW.getRGB(i,j));
		}
		
		String Ss = null;
		if (localPics != null){
			Ss=localPics.get(2,1);
		}
		else {
			Ss = ImageSetInterface.PIC_NOT_PRESENT;
		}
		if (Ss.equals(ImageSetInterface.PIC_NOT_PRESENT)){
			for (int i=c;i<w+c;i++)
				for (int j=h;j<h+c;j++)
					displayimage.setRGB(i,-j+h+c+h+c-1,displayimage.getRGB(i,j));
		}
		else {
			RegularSubImage S=new RegularSubImage(Ss,true);
			for (int i=0;i<w;i++)
				for (int j=0;j<c;j++)
					displayimage.setRGB(i+c,j+h+c,S.getRGB(i,j));
		}
		
		String SEs = null;
		if (localPics != null){
			SEs=localPics.get(2,2);
		}
		else {
			SEs = ImageSetInterface.PIC_NOT_PRESENT;
		}
		if (SEs.equals(ImageSetInterface.PIC_NOT_PRESENT)){
			for (int i=w;i<w+c;i++)
				for (int j=h;j<h+c;j++)
					displayimage.setRGB(-i+w+c+w+c-1,-j+h+c+h+c-1,displayimage.getRGB(i,j));
		}
		else {
			RegularSubImage SE=new RegularSubImage(SEs,true);
			for (int i=0;i<c;i++)
				for (int j=0;j<c;j++)
					displayimage.setRGB(i+w+c,j+h+c,SE.getRGB(i,j));
		}		
	}

	public DataImage getCenterImage(){
		return C;
	}
	
	public Point AdjustPointForSuper(Point t){
		return new Point(t.x+c,t.y+c);
	}

	public SuperRegularImage(String filename, BufferedImage clone, boolean crop) {
		super(filename, clone, crop);
	}
}