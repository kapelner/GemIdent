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

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JScrollPane;

import GemIdentTools.IOTools;

/**
 * A convenience wrapper for {@link JScrollPane JScrollPane}
 * that includes the capability to take a "screenshot" of whatever is currently visible
 * in the panel, and save it to the hard disk
 * 
 * @author Adam Kapelner
 */
@SuppressWarnings("serial")
public class JScrollPaneWithScreenshot extends JScrollPane {
	
	/**
	 * Save the image currently visible to the hard disk as a JPEG
	 * 
	 * @param filename	the filename to save the image to
	 */
	public void saveScreenshot(String filename){
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		paint(g);
		IOTools.WriteImage(filename + ".jpg", "JPEG", image);
	}
}