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

/**
 * A convenience interface to express data about each image
 * in the context of an overall global image set
 *  
 * @author Adam Kapelner
 */
public interface MiniImageInImageSet {
	
	/** the data for this image as a string (for debugging purposes) */
	public String toString();
	
	/** the x-location of this image in the context of a global image set */
	public int xLoc();
	
	/** the y-location of this image in the context of a global image set */
	public int yLoc();
}