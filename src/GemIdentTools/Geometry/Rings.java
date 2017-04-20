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
 * This is kept around basically as a 
 * static final class which is referenced
 * when "masks" are needed for attribute
 * generation. A "mask" is the locus of 
 * points a certain radius from the center
 * (represented as Lists of Point objects).
 * The center of the ring is always coordinate (0,0)
 * <p>
 * For instance, the "twoRing" would be:
 * <ul>
 * <li>		     0     0     1     0     0
 * <li>		     0     1     0     1     0
 * <li>	 	     1     0     0     0     1
 * <li>		     0     1     0     1     0
 * <li>		     0     0     1     0     0
 * </ul>
 *  where the ones represent points in the
 *  mask two pixels from the center.
 *  </p>
 *  <p>
 *  The rings are generated using the Bresenham 
 *  discrete circle generation algorithm.
 *  See step 4b in the Algorithm section of the IEEE 
 *  paper for a formal mathematical description.
 *  </p>
 *  
 *  @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
 *  @see <a href="">Bresenham circle algorithm</a>
 * 
 * @author Adam Kapelner
 */
public class Rings {
	
	/** initially the number of rings to autogenerate */
	public static final int INIT_MAX_RING=75;

	/** the mapping from radius to the ring itself stored as a list of {@link java.awt.Point Point} objects */
	private static HashMap<Integer,ArrayList<Point>> allRingSet;
	
	/** To construct the static object, call Build */
	static {
		Build();
	}
	/** Build the initial rings for radius = {0, 1, . . ., INIT_MAX_RING} */
	public static void Build(){
		
		allRingSet=new HashMap<Integer,ArrayList<Point>>(INIT_MAX_RING+1);
		
		//build radius=0 ring
		ArrayList<Point> points=new ArrayList<Point>();
		points.add(new Point(0,0));
		allRingSet.put(0,points);
		
		//build radius=1 ring
		points=new ArrayList<Point>();		
		points.add(new Point(0,-1));
		points.add(new Point(-1,0));
		points.add(new Point(1,0));
		points.add(new Point(0,1));
		points.add(new Point(1,-1));
		points.add(new Point(-1,1));
		points.add(new Point(1,1));
		points.add(new Point(-1,-1));
		allRingSet.put(1,points);

		//build all other radii rings
		for (int r=2;r<=INIT_MAX_RING;r++)
			GenerateRing(r);
	}
	/**
	 * Generate a ring of radius r using the Bresenham algorithm
	 * 
	 * @param r			the radius of the ring to be generated
	 * @return			the ring as a list of coordinates
	 * @see <a href="http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm">Bresenham algorthim</a>
	 */
	private static ArrayList<Point> GenerateRing(int r) {
		ArrayList<Point> points = new ArrayList<Point>();
		int d=3-2*r;
		int y=r;
		
		for (int x=0;x<r;x++){
			//add point in each of the 8 octants
			points.add(new Point(x,y));
			points.add(new Point(x,-y));
			points.add(new Point(-x,y));
			points.add(new Point(-x,-y));
			points.add(new Point(y,x));
			points.add(new Point(y,-x));
			points.add(new Point(-y,x));
			points.add(new Point(-y,-x));
			
			//adjust d
			if (d < 0)
				d=d+4*x+6;
			else {
				d=d+4*(x-y)+10;
				y--;
			}
			//if we've finished 45degrees, stop
			if (x == y)
				break;
		}	
		//now rotate and save:
		allRingSet.put(r,points);
		
		return points;
	}
	/**
	 * Return a ring of radius r. If the ring is not yet generated, 
	 * it will autogenerate, cache it, and return it
	 * 
	 * @param r		radius of the ring desired
	 * @return		the ring
	 */
	public static ArrayList<Point> getRing(int r){
		ArrayList<Point> ring = allRingSet.get(r);
		if (ring == null)
			ring = GenerateRing(r);
		return ring;
	}
	/**
	 * Return a ring of radius r centered at to (not at the origin) whose coordinates
	 * are included in the rectangle bounded by (0,0) and (xMax,yMax). If the ring 
	 * is not yet generated, it will autogenerate, cache it, and return it.
	 * 
	 * @param r			the radius of the ring desired
	 * @param to		the center of the ring
	 * @param xMax		the max x value of the coordinate
	 * @param yMax		the max y value of the coordinate
	 * @return			the ring desired
	 */
	public static ArrayList<Point> getRingUsingCenter(int r,Point to,int xMax,int yMax){
		ArrayList<Point> ring=getRing(r);
		ArrayList<Point> ringc=new ArrayList<Point>(ring.size());
		for (Point t:ring){
			int x=t.x+to.x;
			int y=t.y+to.y;
			if (x >= 0 && y >= 0 && x < xMax && y < yMax)
				ringc.add(new Point(t.x+to.x,t.y+to.y));
		}
		return ringc;
	}
}