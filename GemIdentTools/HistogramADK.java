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
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import GemIdentOperations.Run;
import GemIdentTools.Matrices.BoolMatrix;

/**
 * A toolbox class that builds histogram graphics from one-dimensional data
 * 
 * @author Adam Kapelner
 */
public class HistogramADK {
	
	/** The height of the largest bar as a proportion of the height of the y-axis */
	private static final double MaxHeightAsProportion = .9;
	/** The width of the histogram graphic */
	public static final int HistWidth=500;
	/** The height of the histogram graphic */
	public static final int HistHeight=500;
	/** The number of bars in the histogram */
	private static final int NUMBINS=50;
	/** The minimum number of data points required to build the histogram */
	private static final int MIN_SIZE=15;
	/** The minimum value in the data */
	private static double min;
	/** The maximum value in the data */
	private static double max;
	/** The graphics object that the histogram is drawn on */
	private static Graphics g;

	/**
	 * Gets the histogram of the data as a BufferedImage
	 * 
	 * @param data		The one-dimensional data as an array of doubles
	 * @return			The BufferedImage graphic
	 */
	public static BufferedImage getHistogram(double[] data){
		if (data.length < MIN_SIZE)
			return null;
	
		BufferedImage I=new BufferedImage(HistWidth,HistHeight,BufferedImage.TYPE_INT_RGB);
		for (int i=0;i<I.getWidth();i++)
			for (int j=0;j<I.getHeight();j++)
				I.setRGB(i,j,BoolMatrix.WhiteRGB);
		
		g=I.getGraphics();
		

		PaintAxes();
		int[] freq=GetFrequencies(data);
		int maxval=DrawBins(freq);
		DrawLabelsY(maxval);

		return I;
	}

	/** the left and right margin */
	private static final int xMargin=20;
	/** the top and bottom margin */
	private static final int yMargin=20;
	/** the distance from the left and right terminal of the xAxis to the left and right margin */
	private static final int xAxisDist=xMargin+30;
	/** the distance from the top and bottom terminal of the yAxis to the top and bottom margin */
	private static final int yAxisDist=yMargin+30;
	/** the number of labels on the xAxis */
	private static final int NUMXLABS=10;
	/** the number of labels on the yAxis */
	private static final int NUMYLABS=10;
	/** Paints the axes on the graphic */
	private static void PaintAxes(){
		g.setColor(Color.BLACK);
		g.drawLine(xAxisDist,HistHeight-yAxisDist,HistWidth-xAxisDist,HistHeight-yAxisDist);
		g.drawLine(xAxisDist,HistHeight-yAxisDist,xAxisDist,yAxisDist);
		g.drawString("freq",xMargin-15,yAxisDist+(HistHeight-2*yAxisDist)/2);
		g.drawString("distance ("+Run.it.imageset.getMeasurement_unit()+")",xAxisDist+(HistWidth-2*xAxisDist)/2,HistHeight-yAxisDist+30);
	}
	/** Draws the bins on the graphic */
	private static int DrawBins(int[] freq) {
		int maxval=Integer.MIN_VALUE;
		for (int i:freq)
			if (i > maxval)
				maxval=i;
		
		double max_height=(HistHeight-2*yAxisDist)*MaxHeightAsProportion;		
		double bin_width=(HistWidth-2*xAxisDist)/((double)NUMBINS);		
		int x=xAxisDist;
		
		for (int i=0;i<NUMBINS;i++){
			DrawBin(x,bin_width,freq[i]/((double)maxval)*max_height);
			x+=bin_width;
		}
		return maxval;
	}
	/**
	 * Draws one bar on the histogram
	 * 
	 * @param x		the bar's location on the xAxis
	 * @param w		the width of the bar
	 * @param h		the height of the bar
	 */
	private static void DrawBin(int x,double w,double h){
		g.setColor(Color.BLUE);
		g.fillRect(x,HistHeight-yAxisDist-(int)Math.round(h),(int)Math.round(w),(int)Math.round(h));
		g.setColor(Color.BLACK);
		g.drawRect(x,HistHeight-yAxisDist-(int)Math.round(h),(int)Math.round(w),(int)Math.round(h));	
	}
	/**
	 * Bins the data and then draws the labels on the xAxis
	 * @param data		the data
	 * @return			the bins and their respective frequencies
	 */
	private static int[] GetFrequencies(double[] data){
		int[] freqs=new int[NUMBINS];
		
		min=Double.MAX_VALUE;
		max=Double.MIN_VALUE;
		for (double d:data){
			if (d < min)
				min=d;
			if (d > max)
				max=d;
		}
		double conversion=(max-min)/NUMBINS;
		
		for (double d:data){
			double rel=d-min;
			int bin=(int)Math.floor(rel/conversion);
			if (bin == NUMBINS)
				freqs[bin-1]++;
			else
				freqs[bin]++;
		}		
		DrawLabelsX(min,max);		
		return freqs;
	}
	/** Draws labels on the xAxis between min and max */
	private static void DrawLabelsX(double min,double max){
		int[] labels=new int[NUMXLABS];
		
		double space=(max-min)/NUMXLABS;
		for (int i=0;i<NUMXLABS;i++)
			labels[i]=(int)Math.round(min+space*i);
		
		int y=HistHeight-yAxisDist+15;
		int xstep=(int)Math.round((HistWidth-2*xAxisDist)/((double)NUMXLABS));
		for (int i=0;i<NUMXLABS;i++)
			g.drawString(""+labels[i],xAxisDist+xstep*i,y);
		
	}
	/** Draws labels on the yAxis up to maxval */
	private static void DrawLabelsY(int maxval){
		int[] labels=new int[NUMYLABS+1];
		double space=maxval/((double)NUMYLABS);
		for (int i=0;i<NUMYLABS+1;i++)
			labels[i]=(int)Math.round(space*i);
		
		int x=xAxisDist-28;
		int ystep=(int)Math.round(((HistHeight-2*yAxisDist)*MaxHeightAsProportion)/((double)NUMYLABS));
		for (int i=0;i<NUMYLABS+1;i++)
			g.drawString(""+labels[i],x,(HistHeight-yAxisDist)-ystep*i);
		
	}
}