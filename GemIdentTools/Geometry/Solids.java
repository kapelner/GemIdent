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

package GemIdentTools.Geometry;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Provides masks of filled circles of varius radii 
 * similiar to the {@link Rings Rings} class
 * which provides masks of unfilled circles
 * 
 * @author Adam Kapelner
 */
public class Solids {
	
	/** the maxium solid's radius for the initial build */
	public static final int INIT_MAX_SOLID=50;
	/** the mapping from radius to the solid itself stored as a list of {@link java.awt.Point Point} objects */
	private static HashMap<Integer,ArrayList<Point>> solidLists;
	
	/** To construct the static object, call Build */
	static {
		Build();
	}
	/** Build the initial solids for radius = {0, 1, . . ., INIT_MAX_SOLID} */
	public static void Build(){
		//init the mapping
		solidLists=new HashMap<Integer,ArrayList<Point>>(INIT_MAX_SOLID);
		//init the first solid
		ArrayList<Point> zeroList=new ArrayList<Point>();
		zeroList.add(new Point(0,0));
		solidLists.put(0,zeroList);
		//init all other solids
		for (int r=1;r<=INIT_MAX_SOLID;r++)
			GenerateSolid(r);
	}	
	/**
	 * Generate a solid of radius r using an inefficient algorithm
	 * 
	 * @param r			the radius of the solid to be generated
	 * @return			the solid as a list of coordinates
	 */
	private static ArrayList<Point> GenerateSolid(int r){
		int rsq=r*r;
		ArrayList<Point> solidList=new ArrayList<Point>();
		for (int x=-r;x<=r;x++)
			for (int y=-r;y<=r;y++)
				if (x*x + y*y <= rsq)
					solidList.add(new Point(x,y));
		solidLists.put(r,solidList);
		return solidList;
	}
	/**
	 * Return a solid of radius r. If the solid is not yet generated, 
	 * it will autogenerate, cache it, and return it
	 * 
	 * @param r		radius of the solid desired
	 * @return		the solid
	 */
	public static ArrayList<Point> getSolid(int r){
		ArrayList<Point> solid=solidLists.get(r);
		if (solid == null)
			return GenerateSolid(r);
		else
			return solid;
	}
	/**
	 * Return a solid of radius r centered at to (not at the origin). If the solid 
	 * is not yet generated, it will autogenerate, cache it, and return it.
	 * 
	 * @param r			the radius of the solid desired
	 * @param to		the center of the solid
	 * @return			the solid desired
	 */
	public static ArrayList<Point> GetPointsInSolidUsingCenter(int r,Point to){
		ArrayList<Point> solid=getSolid(r);
		ArrayList<Point> points=new ArrayList<Point>(solid.size());
		for (Point t:solid)
			points.add(new Point(to.x+t.x,to.y+t.y));
		return points;
	}
}