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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import GemIdentTools.Matrices.StringMatrix;
import GemIdentTools.Thumbnails;

/**
 * An image set that contains images that do not belong to an overall global image context.
 * Since most of the abstract functions in the super classes deal with global image context
 * functionality, there's not much to implement in this class
 * 
 * @author Adam Kapelner
 */
public class NonGlobalImageSet extends ImageSetInterfaceWithUserColors implements Serializable {
	private static final long serialVersionUID = 7928568299846106407L;

	public NonGlobalImageSet(){}
	
	public NonGlobalImageSet(String homedir) {
		super(homedir);
	}
	
	public DataImage getDataImageFromFilename(String filename){
		return new RegularSubImage(filename, false);
	}

	@Override
	public void CreateHTMLForComposite(int width, int height) {}

	@Override
	public ArrayList<String> GetImages() {
		return Thumbnails.GetImageListAsCollection(this.getHomedir());
	}
	
	@Override //return null to force SuperImage to make reflections
	public StringMatrix GetLocalPics(String filename, Integer notused) {return null;}

	@Override
	public BufferedImage getGlobalImageSlice(int rowA, int rowB) {return null;}

	@Override
	public Set<String> getFilterNames() {
		return stains.keySet();
	}

	@Override
	public int getGlobalHeight(boolean excise) {return 0;}

	@Override
	public int getGlobalWidth(boolean excise) {return 0;}

	@Override
	public String getInitializationFilename() {return null;}

	@Override
	public StringMatrix getPicFilenameTable() {return null;}

	@Override
	public Point getTrueLocation(String filename, Point to, boolean excise) {
		return to;
	}

	@Override
	public int getXf() {return 0;}

	@Override
	public int getXo() {return 0;}

	@Override
	public int getYf() {return 0;}

	@Override
	public int getYo() {return 0;}

	@Override
	public BufferedImage getGlobalThumbnailImage(Float approxGlobalScaledWidth) {
		return null;
	}

	@Override
	public HashSet<String> getClickedonimages() {
		return null;
	}

	@Override
	public void presave() {}

	@Override
	public void spawnSophisticatedHelper() {}

	@Override
	public void RunUponNewProject() {}
	
	public void ThumbnailsCompleted(){}	
	
}