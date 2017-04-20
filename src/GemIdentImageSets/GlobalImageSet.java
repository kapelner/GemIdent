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

/**
 * An image set that contains images that belong to an overall global image context.
 * This class is unfinished and will be extended in the future
 * 
 * @author Adam Kapelner
 */
public final class GlobalImageSet extends ImageSetInterfaceWithUserColors implements Serializable{
	
	private static final long serialVersionUID = 786397416431297508L;

	public GlobalImageSet(String homedir){
		super(homedir);
	}
	
	public DataImage getDataImageFromFilename(String filename){
		return new RegularSubImage(filename, false);
	}

	@Override
	public void CreateHTMLForComposite(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<String> GetImages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringMatrix GetLocalPics(String filename, Integer notused) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedImage getGlobalImageSlice(int rowA, int rowB) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getFilterNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getGlobalHeight(boolean excise) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getGlobalWidth(boolean excise) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getInitializationFilename() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringMatrix getPicFilenameTable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Point getTrueLocation(String filename, Point to, boolean excise) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getXf() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getXo() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getYf() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getYo() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public BufferedImage getGlobalThumbnailImage(Float approxGlobalScaledWidth) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashSet<String> getClickedonimages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void presave() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void spawnSophisticatedHelper() {}

	@Override
	public void RunUponNewProject() {
		// TODO Auto-generated method stub
		
	}
	
	public void ThumbnailsCompleted(){}

}