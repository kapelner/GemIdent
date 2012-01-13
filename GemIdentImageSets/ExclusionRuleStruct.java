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

import java.io.Serializable;

/**
 * A dumb struct that holds information about an exclusion rule - 
 * a rule that excludes one phenotype from being a certain distance 
 * from another phenotype
 * 
 * @author Adam Kapelner
 */
public class ExclusionRuleStruct implements Serializable {
	private static final long serialVersionUID = -2796466806558208962L;
	
	/** the phenotype that if found ... */
	public String if_phenotype;
	/** ... then exclude this phenotype if ... */
	public String exclude_phenotype;
	/** ... this distance away or less */
	public int num_pixels;
	
	public String getIf_phenotype() {
		return if_phenotype;
	}
	public void setIf_phenotype(String if_phenotype) {
		this.if_phenotype = if_phenotype;
	}
	public String getExclude_phenotype() {
		return exclude_phenotype;
	}
	public void setExclude_phenotype(String exclude_phenotype) {
		this.exclude_phenotype = exclude_phenotype;
	}
	public int getNum_pixels() {
		return num_pixels;
	}
	public void setNum_pixels(int num_pixels) {
		this.num_pixels = num_pixels;
	}
}