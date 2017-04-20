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

import java.awt.Point;

/**
 * An interface that simplifies what happens when the 
 * user clicks on a point in a {@link KTrainPanel training panel}
 * 
 * @author Adam Kapelner
 *
 */
public interface TrainClickListener {
	/**
	 * The user has just clicked on a point in an image
	 * 
	 * @param filename		the image filename
	 * @param t				the coordinate where the user clicked
	 */
	public void NewPoint(String filename,Point t);
	
	/**
	 * The user just deleted a point
	 * 
	 * @param filename		the image filename
	 * @param t				the coordinate where the user clicked
	 */
	public void DeletePoint(String filename, Point t);

	/**
	 * The user just added a point to the set of NON's
	 * 
	 * @param filename		the image filename
	 * @param t				the coordinate where the user clicked
	 */
	public void NewNonPoint(String filename, Point t);
}