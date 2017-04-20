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

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;

/**
 * Basically a wrapper around the super class, adding no new functionality of its
 * own. This class is intended to wrap images that don't have any strings attached.
 * 
 * @author Adam Kapelner
 */
public class RegularSubImage extends DataImage {
	
	public RegularSubImage(){} //makes daughter class happy
	
	public RegularSubImage(String filename, boolean crop) {
		super(filename, crop);
	}
	
	public RegularSubImage(String filename, BufferedImage clone, boolean crop) {
		super(filename, clone, crop);
	}
	
	public RegularSubImage(BufferedImage image) {
		super(image);
	}

	public Color getColorAt(Point t) {
		return new Color(displayimage.getRGB(t.x,t.y));
	}

	public DataImage clone(){
		BufferedImage clone = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int i=0;i<getWidth();i++)
			for (int j=0;j<getHeight();j++)
				clone.setRGB(i,j,displayimage.getRGB(i,j));
		return new RegularSubImage(filename,clone,false);
	}
	
	public int getHeight() {
		return displayimage.getHeight();
	}

	public int getWidth() {
		return displayimage.getWidth();
	}
}