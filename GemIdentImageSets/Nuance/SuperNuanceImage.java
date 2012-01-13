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

package GemIdentImageSets.Nuance;

import java.awt.Point;

import GemIdentImageSets.DataImage;
import GemIdentImageSets.ImageAndScoresBank;
import GemIdentImageSets.SuperImage;
import GemIdentOperations.Run;

/**
 * An implementation of a {@link NuanceSubImage NuanceSubImage} that contains pixel information
 * on its edges of the surrounding images
 * 
 * @author Adam Kapelner
 */
public final class SuperNuanceImage extends NuanceSubImage implements SuperImage {

	/** the center image */
	private NuanceSubImage C;
	/** the number of surrounding pixels */
	private int c;
	
	public SuperNuanceImage(NuanceImageListInterface imagelist, Object num_or_filename){
		this.imagelist = imagelist;
		
		if (num_or_filename instanceof String){
			filename=(String)num_or_filename;
			num = NuanceImageListInterface.ConvertFilenameToNum(filename);
		}
		else if (num_or_filename instanceof Integer){
			num = (Integer)num_or_filename;
			filename = NuanceImageListInterface.ConvertNumToFilename(num);
		}
		
		C = (NuanceSubImage)ImageAndScoresBank.getOrAddDataImage(filename);

		//get number of pixels to surround by
		c = Run.it.getMaxPhenotypeRadiusPlusMore(null) + 2; //add 2 just in case!
		
		//now we don't worry about the displayimage at all, we need to get
		//the data for all nuance images surrounding this one
		nuanceimages = imagelist.getNuanceImagePixelDataForSuper(num, c, C);
		cropped = false;
		setupDisplayImage();
	}
	
	public DataImage getCenterImage(){
		return C;
	}
	
	public Point AdjustPointForSuper(Point t){
		return new Point(t.x+c,t.y+c);
	}
}