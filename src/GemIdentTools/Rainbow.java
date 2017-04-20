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

package GemIdentTools;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

/**
 * Provides many colors spanning the range of 24bit rgb colors.
 * Used specifically for the phenotype training helper, 
 * {@link GemIdentOperations.ImageSetHeuristics ImageSetHeuristics} 
 * 
 * @author Adam Kapelner
 */
public class Rainbow {
	/** the list where the colors are stored */
	private static final ArrayList<Color> colors;
	/**
	 * Given a certain number of colors, N, split the entire range into N colors and get the cth color
	 * 
	 * @param c		get the cth color of N
	 * @param N		collapse entire rainbow in N equally spaced colors
	 * @return		get the c/N color as a {@link java.awt.Color color}
	 */
	public static Color getColor(int c,Integer N){
		int color=(int)Math.floor(c/((double)N)*colors.size());
		return colors.get(color);
	}
	
	public static int getRandomColorInt(){
		return colors.get((int)Math.floor(new Random().nextDouble() * colors.size())).getRGB();
	}
	/** create the color list by traversing a sample of the 16.8 million RGB colors */
	static {
		colors=new ArrayList<Color>();		
		colors.add(new Color(0,0,0));
		colors.add(new Color(0,0,5));
		colors.add(new Color(0,0,10));
		colors.add(new Color(0,0,15));
		colors.add(new Color(0,0,20));
		colors.add(new Color(0,0,25));
		colors.add(new Color(0,0,30));
		colors.add(new Color(0,0,35));
		colors.add(new Color(0,0,40));
		colors.add(new Color(0,0,45));
		colors.add(new Color(0,0,50));
		colors.add(new Color(0,0,55));
		colors.add(new Color(0,0,60));
		colors.add(new Color(0,0,65));
		colors.add(new Color(0,0,70));
		colors.add(new Color(0,0,75));
		colors.add(new Color(0,0,80));
		colors.add(new Color(0,0,85));
		colors.add(new Color(0,0,90));
		colors.add(new Color(0,0,95));
		colors.add(new Color(0,0,100));
		colors.add(new Color(0,0,105));
		colors.add(new Color(0,0,110));
		colors.add(new Color(0,0,115));
		colors.add(new Color(0,0,120));
		colors.add(new Color(0,0,125));
		colors.add(new Color(0,0,130));
		colors.add(new Color(0,0,135));
		colors.add(new Color(0,0,140));
		colors.add(new Color(0,0,145));
		colors.add(new Color(0,0,150));
		colors.add(new Color(0,0,155));
		colors.add(new Color(0,0,160));
		colors.add(new Color(0,0,165));
		colors.add(new Color(0,0,170));
		colors.add(new Color(0,0,175));
		colors.add(new Color(0,0,180));
		colors.add(new Color(0,0,185));
		colors.add(new Color(0,0,190));
		colors.add(new Color(0,0,195));
		colors.add(new Color(0,0,200));
		colors.add(new Color(0,0,205));
		colors.add(new Color(0,0,210));
		colors.add(new Color(0,0,215));
		colors.add(new Color(0,0,220));
		colors.add(new Color(0,0,225));
		colors.add(new Color(0,0,230));
		colors.add(new Color(0,0,235));
		colors.add(new Color(0,0,240));
		colors.add(new Color(0,0,245));
		colors.add(new Color(0,0,250));
		colors.add(new Color(0,0,255));
		colors.add(new Color(0,5,255));
		colors.add(new Color(0,10,255));
		colors.add(new Color(0,15,255));
		colors.add(new Color(0,20,255));
		colors.add(new Color(0,25,255));
		colors.add(new Color(0,30,255));
		colors.add(new Color(0,35,255));
		colors.add(new Color(0,40,255));
		colors.add(new Color(0,45,255));
		colors.add(new Color(0,50,255));
		colors.add(new Color(0,55,255));
		colors.add(new Color(0,60,255));
		colors.add(new Color(0,65,255));
		colors.add(new Color(0,70,255));
		colors.add(new Color(0,75,255));
		colors.add(new Color(0,80,255));
		colors.add(new Color(0,85,255));
		colors.add(new Color(0,90,255));
		colors.add(new Color(0,95,255));
		colors.add(new Color(0,100,255));
		colors.add(new Color(0,105,255));
		colors.add(new Color(0,110,255));
		colors.add(new Color(0,115,255));
		colors.add(new Color(0,120,255));
		colors.add(new Color(0,125,255));
		colors.add(new Color(0,130,255));
		colors.add(new Color(0,135,255));
		colors.add(new Color(0,140,255));
		colors.add(new Color(0,145,255));
		colors.add(new Color(0,150,255));
		colors.add(new Color(0,155,255));
		colors.add(new Color(0,160,255));
		colors.add(new Color(0,165,255));
		colors.add(new Color(0,170,255));
		colors.add(new Color(0,175,255));
		colors.add(new Color(0,180,255));
		colors.add(new Color(0,185,255));
		colors.add(new Color(0,190,255));
		colors.add(new Color(0,195,255));
		colors.add(new Color(0,200,255));
		colors.add(new Color(0,205,255));
		colors.add(new Color(0,210,255));		
		colors.add(new Color(0,215,255));
		colors.add(new Color(0,220,255));
		colors.add(new Color(0,225,255));
		colors.add(new Color(0,230,255));
		colors.add(new Color(0,235,255));
		colors.add(new Color(0,240,255));
		colors.add(new Color(0,245,255));
		colors.add(new Color(0,250,255));
		colors.add(new Color(0,255,255));
		colors.add(new Color(0,255,250));		
		colors.add(new Color(0,255,245));
		colors.add(new Color(0,255,240));
		colors.add(new Color(0,255,235));
		colors.add(new Color(0,255,230));
		colors.add(new Color(0,255,225));
		colors.add(new Color(0,255,220));
		colors.add(new Color(0,255,215));		
		colors.add(new Color(0,255,210));
		colors.add(new Color(0,255,205));
		colors.add(new Color(0,255,200));
		colors.add(new Color(0,255,195));
		colors.add(new Color(0,255,185));
		colors.add(new Color(0,255,180));
		colors.add(new Color(0,255,175));		
		colors.add(new Color(0,255,170));
		colors.add(new Color(0,255,165));
		colors.add(new Color(0,255,160));
		colors.add(new Color(0,255,155));
		colors.add(new Color(0,255,145));
		colors.add(new Color(0,255,140));
		colors.add(new Color(0,255,135));		
		colors.add(new Color(0,255,130));
		colors.add(new Color(0,255,125));
		colors.add(new Color(0,255,120));
		colors.add(new Color(0,255,115));
		colors.add(new Color(0,255,110));
		colors.add(new Color(0,255,105));
		colors.add(new Color(0,255,100));		
		colors.add(new Color(0,255,95));
		colors.add(new Color(0,255,90));
		colors.add(new Color(0,255,85));
		colors.add(new Color(0,255,80));
		colors.add(new Color(0,255,75));
		colors.add(new Color(0,255,70));
		colors.add(new Color(0,255,65));		
		colors.add(new Color(0,255,60));
		colors.add(new Color(0,255,55));
		colors.add(new Color(0,255,50));
		colors.add(new Color(0,255,45));
		colors.add(new Color(0,255,40));
		colors.add(new Color(0,255,35));
		colors.add(new Color(0,255,30));		
		colors.add(new Color(0,255,25));
		colors.add(new Color(0,255,20));
		colors.add(new Color(0,255,15));
		colors.add(new Color(0,255,10));
		colors.add(new Color(0,255,5));
		colors.add(new Color(0,255,0));
		colors.add(new Color(5,255,0));
		colors.add(new Color(10,255,0));
		colors.add(new Color(15,255,0));
		colors.add(new Color(20,255,0));
		colors.add(new Color(25,255,0));
		colors.add(new Color(30,255,0));
		colors.add(new Color(35,255,0));
		colors.add(new Color(40,255,0));
		colors.add(new Color(45,255,0));
		colors.add(new Color(50,255,0));
		colors.add(new Color(55,255,0));
		colors.add(new Color(60,255,0));
		colors.add(new Color(65,255,0));
		colors.add(new Color(70,255,0));
		colors.add(new Color(75,255,0));
		colors.add(new Color(80,255,0));
		colors.add(new Color(85,255,0));
		colors.add(new Color(90,255,0));
		colors.add(new Color(95,255,0));
		colors.add(new Color(100,255,0));
		colors.add(new Color(105,255,0));
		colors.add(new Color(110,255,0));
		colors.add(new Color(115,255,0));
		colors.add(new Color(120,255,0));
		colors.add(new Color(125,255,0));
		colors.add(new Color(130,255,0));
		colors.add(new Color(135,255,0));
		colors.add(new Color(140,255,0));
		colors.add(new Color(145,255,0));
		colors.add(new Color(150,255,0));
		colors.add(new Color(155,255,0));
		colors.add(new Color(160,255,0));
		colors.add(new Color(165,255,0));
		colors.add(new Color(170,255,0));
		colors.add(new Color(175,255,0));
		colors.add(new Color(180,255,0));
		colors.add(new Color(185,255,0));
		colors.add(new Color(190,255,0));
		colors.add(new Color(195,255,0));
		colors.add(new Color(200,255,0));
		colors.add(new Color(205,255,0));
		colors.add(new Color(210,255,0));
		colors.add(new Color(215,255,0));
		colors.add(new Color(220,255,0));
		colors.add(new Color(225,255,0));
		colors.add(new Color(230,255,0));
		colors.add(new Color(235,255,0));
		colors.add(new Color(240,255,0));
		colors.add(new Color(245,255,0));
		colors.add(new Color(250,255,0));
		colors.add(new Color(255,255,0));
		colors.add(new Color(255,255,5));
		colors.add(new Color(255,255,10));
		colors.add(new Color(255,255,15));
		colors.add(new Color(255,255,20));
		colors.add(new Color(255,255,25));
		colors.add(new Color(255,255,30));
		colors.add(new Color(255,255,35));
		colors.add(new Color(255,255,40));
		colors.add(new Color(255,255,45));
		colors.add(new Color(255,255,50));
		colors.add(new Color(255,255,55));
		colors.add(new Color(255,255,60));
		colors.add(new Color(255,255,65));
		colors.add(new Color(255,255,70));
		colors.add(new Color(255,255,75));
		colors.add(new Color(255,255,80));
		colors.add(new Color(255,255,85));
		colors.add(new Color(255,255,90));
		colors.add(new Color(255,255,95));
		colors.add(new Color(255,255,100));
		colors.add(new Color(255,255,105));
		colors.add(new Color(255,255,110));
		colors.add(new Color(255,255,115));
		colors.add(new Color(255,255,120));
		colors.add(new Color(255,255,125));
		colors.add(new Color(255,255,130));
		colors.add(new Color(255,255,135));
		colors.add(new Color(255,255,140));
		colors.add(new Color(255,255,145));
		colors.add(new Color(255,255,150));
		colors.add(new Color(255,255,155));
		colors.add(new Color(255,255,160));
		colors.add(new Color(255,255,165));
		colors.add(new Color(255,255,170));
		colors.add(new Color(255,255,175));
		colors.add(new Color(255,255,180));
		colors.add(new Color(255,255,185));
		colors.add(new Color(255,255,190));
		colors.add(new Color(255,255,195));
		colors.add(new Color(255,255,200));
		colors.add(new Color(255,255,205));
		colors.add(new Color(255,255,210));
		colors.add(new Color(255,255,215));
		colors.add(new Color(255,255,220));
		colors.add(new Color(255,255,225));
		colors.add(new Color(255,255,230));
		colors.add(new Color(255,255,235));
		colors.add(new Color(255,255,240));
		colors.add(new Color(255,255,245));
		colors.add(new Color(255,255,250));
		colors.add(new Color(255,255,255));
		
		colors.add(new Color(255,250,255));
		colors.add(new Color(255,245,255));
		colors.add(new Color(255,240,255));
		colors.add(new Color(255,235,255));
		colors.add(new Color(255,230,255));
		colors.add(new Color(255,225,255));
		colors.add(new Color(255,220,255));
		colors.add(new Color(255,215,255));
		colors.add(new Color(255,210,255));
		colors.add(new Color(255,205,255));
		colors.add(new Color(255,200,255));
		colors.add(new Color(255,195,255));
		colors.add(new Color(255,190,255));
		colors.add(new Color(255,185,255));
		colors.add(new Color(255,180,255));
		colors.add(new Color(255,175,255));
		colors.add(new Color(255,170,255));
		colors.add(new Color(255,165,255));
		colors.add(new Color(255,160,255));
		colors.add(new Color(255,155,255));
		colors.add(new Color(255,150,255));
		colors.add(new Color(255,145,255));
		colors.add(new Color(255,140,255));
		colors.add(new Color(255,135,255));
		colors.add(new Color(255,130,255));
		colors.add(new Color(255,125,255));
		colors.add(new Color(255,120,255));
		colors.add(new Color(255,115,255));
		colors.add(new Color(255,110,255));
		colors.add(new Color(255,105,255));
		colors.add(new Color(255,100,255));
		colors.add(new Color(255,95,255));
		colors.add(new Color(255,90,255));
		colors.add(new Color(255,85,255));
		colors.add(new Color(255,80,255));
		colors.add(new Color(255,75,255));
		colors.add(new Color(255,70,255));
		colors.add(new Color(255,65,255));
		colors.add(new Color(255,60,255));
		colors.add(new Color(255,55,255));
		colors.add(new Color(255,50,255));
		colors.add(new Color(255,45,255));
		colors.add(new Color(255,40,255));
		colors.add(new Color(255,35,255));
		colors.add(new Color(255,30,255));
		colors.add(new Color(255,25,255));
		colors.add(new Color(255,20,255));
		colors.add(new Color(255,15,255));
		colors.add(new Color(255,10,255));
		colors.add(new Color(255,5,255));
		colors.add(new Color(255,0,255));

		colors.add(new Color(255,0,250));
		colors.add(new Color(255,0,245));
		colors.add(new Color(255,0,235));
		colors.add(new Color(255,0,230));
		colors.add(new Color(255,0,225));
		colors.add(new Color(255,0,220));
		colors.add(new Color(255,0,215));
		colors.add(new Color(255,0,210));
		colors.add(new Color(255,0,205));
		colors.add(new Color(255,0,200));
		colors.add(new Color(255,0,195));
		colors.add(new Color(255,0,190));
		colors.add(new Color(255,0,185));
		colors.add(new Color(255,0,180));
		colors.add(new Color(255,0,175));
		colors.add(new Color(255,0,170));
		colors.add(new Color(255,0,165));
		colors.add(new Color(255,0,160));
		colors.add(new Color(255,0,155));
		colors.add(new Color(255,0,150));
		colors.add(new Color(255,0,145));
		colors.add(new Color(255,0,140));
		colors.add(new Color(255,0,135));
		colors.add(new Color(255,0,130));
		colors.add(new Color(255,0,125));
		colors.add(new Color(255,0,120));
		colors.add(new Color(255,0,115));
		colors.add(new Color(255,0,110));
		colors.add(new Color(255,0,105));
		colors.add(new Color(255,0,100));
		colors.add(new Color(255,0,95));
		colors.add(new Color(255,0,90));
		colors.add(new Color(255,0,85));
		colors.add(new Color(255,0,80));
		colors.add(new Color(255,0,75));
		colors.add(new Color(255,0,70));
		colors.add(new Color(255,0,65));
		colors.add(new Color(255,0,60));
		colors.add(new Color(255,0,55));
		colors.add(new Color(255,0,50));
		colors.add(new Color(255,0,45));
		colors.add(new Color(255,0,40));
		colors.add(new Color(255,0,35));
		colors.add(new Color(255,0,30));
		colors.add(new Color(255,0,25));
		colors.add(new Color(255,0,20));
		colors.add(new Color(255,0,15));
		colors.add(new Color(255,0,10));
		colors.add(new Color(255,0,5));
		colors.add(new Color(255,0,0));

		colors.add(new Color(255,0,0));
		colors.add(new Color(250,0,0));
		colors.add(new Color(245,0,0));
		colors.add(new Color(240,0,0));
		colors.add(new Color(235,0,0));
		colors.add(new Color(230,0,0));
		colors.add(new Color(225,0,0));
		colors.add(new Color(220,0,0));
		colors.add(new Color(215,0,0));
		colors.add(new Color(210,0,0));
		colors.add(new Color(205,0,0));
		colors.add(new Color(200,0,0));
		colors.add(new Color(195,0,0));
		colors.add(new Color(190,0,0));
		colors.add(new Color(185,0,0));
		colors.add(new Color(180,0,0));
		colors.add(new Color(175,0,0));
		colors.add(new Color(170,0,0));
		colors.add(new Color(165,0,0));
		colors.add(new Color(160,0,0));
		colors.add(new Color(155,0,0));
		colors.add(new Color(150,0,0));
		colors.add(new Color(145,0,0));
		colors.add(new Color(140,0,0));
		colors.add(new Color(135,0,0));
		colors.add(new Color(130,0,0));
		colors.add(new Color(125,0,0));
		colors.add(new Color(120,0,0));
		colors.add(new Color(115,0,0));
		colors.add(new Color(110,0,0));
		colors.add(new Color(105,0,0));
		colors.add(new Color(100,0,0));
		colors.add(new Color(95,0,0));
		colors.add(new Color(90,0,0));
		colors.add(new Color(85,0,0));
		colors.add(new Color(80,0,0));
		colors.add(new Color(75,0,0));
		colors.add(new Color(70,0,0));
		colors.add(new Color(65,0,0));
		colors.add(new Color(60,0,0));
		colors.add(new Color(55,0,0));
		colors.add(new Color(50,0,0));
		colors.add(new Color(45,0,0));
		colors.add(new Color(40,0,0));
		colors.add(new Color(35,0,0));
		colors.add(new Color(30,0,0));
		colors.add(new Color(25,0,0));
		colors.add(new Color(20,0,0));
		colors.add(new Color(15,0,0));
		colors.add(new Color(10,0,0));
		colors.add(new Color(5,0,0));
		colors.add(new Color(0,0,0));
	}
}