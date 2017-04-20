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

/**
 * A "SuperImage" is only relevant when each subimage has a global context.
 * It is a spruced up DataImage, including the original data image 
 * in the center with a number of pixels ("c") from the surrounding subimages.
 * 
 * c is determined by 150% of the maximum radius of all phenotypes of interest.
 * 
 * If an image does not exist in a certain direction from this subimage, the center 
 * image is mirror-reflected (either through the line for N,S,E,W or through
 * the corner for NW,NE,SW,SE) to provide an estimate as to what it may look like.
 * 
 * If only Java had multiple-inheritance, there would not have to be any code
 * duplication
 * 
 * @author Adam Kapelner
 */
public interface SuperImage {
	
	/** Gets the true seed (center) image
	 *
	 * @return			The center image
	*/
	public DataImage getCenterImage();
	
	/** It is necessary to translate a coordinate in the
	 * image of interest to the correct coorindate in
	 * the superimage. Basically just the point is translated up
	 * and to the right by c.
	 *
	 * @param t			The point to be translated
	 * @return			The translated point
	*/
	public Point AdjustPointForSuper(Point t);
	
	/** Java is dump due to lack of multiple inheritance - this is a method 
	 * in DataImage as well
	 */
	public BufferedImage getAsBufferedImage();
	
	public String filename();
	
}