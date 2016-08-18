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

package GemIdentAnalysisConsole;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.SimpleAttributeSet;

import GemIdentCentroidFinding.LabelViaSmartErosions;
import GemIdentCentroidFinding.PostProcess;
import GemIdentClassificationEngine.Classify;
import GemIdentImageSets.*;
import GemIdentImageSets.Nuance.NuanceImageListInterface;
import GemIdentOperations.Run;
import GemIdentTools.HistogramADK;
import GemIdentTools.IOTools;
import GemIdentTools.ReportInPDF;
import GemIdentTools.Geometry.Solids;
import GemIdentTools.Matrices.BoolMatrix;
import GemIdentTools.Matrices.DoubleMatrix;
import GemIdentView.*;

/**
 * Provides all the functionality for the data analysis console
 * 
 * @author Adam Kapelner
 */
public class ConsoleParser {

	/** a number of spaces */
	private static final String TAB="    ";
	/** number of allowable concurrent tasks inside the console */
	private static final int NUM_CONCURRENT_TASKS=100;
	/** the {@link GemIdentView.ElementView view pane} for the current analysis images in memroy and on the hard disk */
	private ElementView elementView;
	/** the thread pool responsible for executing all console tasks */
	private ExecutorService pool;
	/** the master map from the image name to the current analysis matrix */
	private HashMap<String, BoolMatrix> images;
	/** a mapping from the name of the binary image function to the class responsible for processing that function */
	private LinkedHashMap<String,RunnableFunction> binaryImageFunctions;
	/** a mapping from the name of the basic image function to the class responsible for processing that function */
	private LinkedHashMap<String,RunnableFunction> imageFunctions;
	/** a mapping from the name of the analysis function to the class responsible for processing that function */
	private LinkedHashMap<String,RunnableFunction> analysisFunctions;
	/** the master function mapping - from the name of the function to the class responsible for processing that function */
	private HashMap<String,RunnableFunction> allFunctions;
	/** a pointer to the {@link GemIdentView.Console Console} */
	private Console console;
	/** a mapping from the image segment number to the appropriate correction element (for type II error correction function) */
	private HashMap<Integer,CorrectElement> correct;
	/** a dummy object used for synchronizing around (see the {@link #Join() Join} operation */
	private Object sychronize;
	private ConsoleParser that;
	
//	private ArrayList<ArrayList<Point>> allBlobs;
	
	/**
	 * Load a global result matrix into memory from the hard disk. If already 
	 * in memory, do nothing. Indicate to the user that it's in memory
	 * by turning its icon in the left panel of the analysis page green.
	 * 
	 * @param name			the name of the global result matrix to load
	 * @return				the global result matrix as a {@link GemIdentTools.Matrices.BoolMatrix BoolMatrix}
	 */
	protected BoolMatrix Load(String name){
		BoolMatrix B=images.get(name);
		if (B == null){
			try {
				try {
					WriteToScreen("loading "+name+" from HD into memory",null,Console.neutral);
					B=new BoolMatrix(1,1); //just to ensure image isn't loaded from HD twice simultaneously
//					B=new BoolMatrix(IOTools.OpenImage(BuildGlobals.GetGlobalFilename(name)));
					B=BoolMatrix.Load(BuildGlobals.GetGlobalFilename(name));
					images.put(name,B);	
					elementView.loadElement(name,true);
				} catch (Exception e){
					B=null;
					e.printStackTrace();
					WriteToScreen("image \""+name+"\" is corrupted",null,Console.error);
				}
			} catch (Throwable t){
				B=null;
				t.printStackTrace();
				WriteToScreen("out of memory",null,Console.error);
			}
		}
		return B;
	}
	/** 
	 * Class responsible for the "erode" function (see its help message for explanation).
	 * Also see section 6.4.1 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Erode extends RunnableFunction {
		
		/** the number of times to erode (null implies once) */
		private Integer n;
		public String HelpMessage(){
			String help=TAB+"Erode"+TAB+TAB+"call: Ie=Erode(I)  --OR--  Ien=Erode(I,n)\n" +
				TAB+TAB+"Erodes image \"I\" using the N4 erosion and saves resulting image in \"Ie\".\n" +
				TAB+TAB+"Erode is overloaded to accept an integer, \"n,\" which repeats the erosion \"n\" times\n";
			return help;
		}
		
		protected String checkIfOkay(){
			if (returnValue == null)
				return "Erode must return an image";
			if (params.length != 1 && params.length != 2)
				return "Erode only accepts one image OR one image and an integer as parameters";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			if (params.length == 2)
				try {
					n=Integer.parseInt(params[1]);
					if (n <= 0)
						return "\"n\" must be greater than 0";
				} catch (Exception e){
					return "\""+params[1]+"\" is not an integer";
				}
			return null;
		}
		/**
		 * Loads the matrix of interest, attempts to do n erosion(s),
		 * by calling the {@link GemIdentTools.Matrices.BoolMatrix#Erode() Erode} function
		 * adds the new image to the master map, adds the new image
		 * to the left pane of the analysis page
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix B=Load(params[0]);
					try {	
						BoolMatrix val=B.Erode();
						if (n != null)
							for (int i=0;i<(n-1);i++)
								val=val.Erode();
						images.put(returnValue,val);
						elementView.addElement(returnValue,ElementView.OTHER,true,false);
						if (n != null)
							WriteToScreen("Erode ("+n+" iterations) completed in "+Run.TimeElapsed(time),null,Console.time);
						else
							WriteToScreen("Erode completed in "+Run.TimeElapsed(time),null,Console.time);
					} catch (Throwable t){
						t.printStackTrace();
						WriteToScreen("out of memory",null,Console.error);
					}
				}
			};
		}
		public Erode(String returnValue,String[] params,Integer n){
			this.returnValue=returnValue;
			this.params=params;
			this.n=n;
		}
		public RunnableFunction clone(){
			return new Erode(returnValue,params,n);
		}
	}
	/** 
	 * Class responsible for the "dilate" function (see its help message for explanation).
	 * Also see section 6.4.2 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Dilate extends RunnableFunction {
		
		/** the number of times to dilate (null implies once) */
		private Integer n;
		
		public String HelpMessage(){
			String help=TAB+"Dilate"+TAB+TAB+"call: Id=Dilate(I)  --OR--  Idn=Dilate(I,n)\n" +
				TAB+TAB+"Dilates image \"I\" using the N4 dilation and saves resulting image in \"Id\".\n" +
				TAB+TAB+"Dilate is overloaded to accept an integer, \"n,\" which repeats the dilation \"n\" times.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue == null)
				return "Dilate must return an image";
			if (params.length != 1 && params.length != 2)
				return "Dilate only accepts either one image OR one image and an integer as parameters";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			if (params.length == 2)
				try {
					n=Integer.parseInt(params[1]);
					if (n <= 0)
						return "\"n\" must be greater than 0";
				} catch (Exception e){
					return "\""+params[1]+"\" is not an integer";
				}
			return null;
		}
		/**
		 * Loads the matrix of interest, attempts to do n dilation(s),
		 * by calling the {@link GemIdentTools.Matrices.BoolMatrix#Dilate() Dilate} function
		 * adds the new image to the master map, adds the new image
		 * to the left pane of the analysis page
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix B=Load(params[0]);					
					try {						
						BoolMatrix val=B.Dilate();
						if (n != null)
							for (int i=0;i<(n-1);i++)
								val=val.Dilate();
						images.put(returnValue,val);
						elementView.addElement(returnValue,ElementView.OTHER,true,false);
						if (n != null)
							WriteToScreen("Dilate ("+n+" iterations) completed in "+Run.TimeElapsed(time),null,Console.time);
						else
							WriteToScreen("Dilate completed in "+Run.TimeElapsed(time),null,Console.time);
					} catch (Throwable t){
						t.printStackTrace();
						WriteToScreen("out of memory",null,Console.error);
					}
				}
			};
		}
		public Dilate(String returnValue,String[] params,Integer n){
			this.returnValue=returnValue;
			this.params=params;
			this.n=n;
		}
		public RunnableFunction clone(){
			return new Dilate(returnValue,params,n);
		}
	}
	/** 
	 * Class responsible for the "scale" function (see its help message for explanation).
	 * Also see section 6.4.4 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Scale extends RunnableFunction {
		
		/** the inverse scale factor */
		private Double scale;
		
		public String HelpMessage(){
			String help=TAB+"Scale"+TAB+TAB+"call: Iscaled=Scale(I,scale)\n" +
				TAB+TAB+"Scales image \"I\" down by scale and saves resulting image in \"Iscaled\".\n" +
				TAB+TAB+"For example a \"scale=2\" would half the size of image \"I\"\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue == null)
				return "Scale must return an image";
			if (params.length != 2)
				return "Scale only accepts one image and one double as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			try {
				scale=Double.parseDouble(params[1]);
			} catch (Exception e){
				return "\""+params[1]+"\" is not an integer";
			}
			return null;
		}
		/**
		 * Loads the matrix of interest, attempts to scale it, using the {@link GemIdentTools.Matrices.BoolMatrix#Scale(double) scale} function
		 * adds the new image to the master map, and adds the new image
		 * to the left pane of the analysis page
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix B=Load(params[0]);					
					try {
						BoolMatrix val=B.Scale(scale);
						images.put(returnValue,val);
						elementView.addElement(returnValue,ElementView.OTHER,true,false);
						WriteToScreen("Scale completed in "+Run.TimeElapsed(time),null,Console.time);
					} catch (Throwable t){
						t.printStackTrace();
						long factor=(long)Math.pow(1/scale,2);
						long mem=B.Area()*factor/8;
						double gigs=mem/Math.pow(10,9);
						NumberFormat format=NumberFormat.getInstance();
						format.setMaximumFractionDigits(2);
						if (gigs > 1)
							WriteToScreen("out of memory - need "+format.format(gigs)+"G",null,Console.error);
						else {
							double megs=mem/Math.pow(10,6);
							WriteToScreen("out of memory - need "+format.format(megs)+"M",null,Console.error);
						}
					}
				}
			};
		}
		public Scale(String returnValue,String[] params,Double scale){
			this.returnValue=returnValue;
			this.params=params;
			this.scale=scale;
		}
		public RunnableFunction clone(){
			return new Scale(returnValue,params,scale);
		}
	}
	/** 
	 * Class responsible for the combining "or" function (see its help message for explanation).
	 * Also see section 6.4.3 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Or extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"Or"+TAB+TAB+"call: Iboth=Or(Ia,Ib)\n" +
				TAB+TAB+"Performs binary or on each pixel in image \"Ia\" and \"Ib\" and saves the result\n" +
				TAB+TAB+"in image \"Iboth\", essentially combining \"Ia\" and \"Ib\" together\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue == null)
				return "Or must return an image";
			if (params.length != 2)
				return "Or only accepts two images as parameters";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			if (!isLegalVariable(params[1]))
				return "\""+params[1]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Loads both matrices of interest, attempts to OR them together,
		 * using the {@link GemIdentTools.Matrices.BoolMatrix#Or(BoolMatrix, BoolMatrix) Or} function
		 * adds the resulting image to the master map, as well as 
		 * to the left pane of the analysis page
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix A=Load(params[0]);
					BoolMatrix B=Load(params[1]);
					try {
						BoolMatrix val=BoolMatrix.Or(A,B);
						images.put(returnValue,val);
						elementView.addElement(returnValue,ElementView.OTHER,true,false);
						WriteToScreen("Or completed in "+Run.TimeElapsed(time),null,Console.time);
					} catch (Throwable t){
						t.printStackTrace();
						WriteToScreen("out of memory",null,Console.error);
					}
				}
			};
		}
		public Or(String returnValue,String[] params){
			this.returnValue=returnValue;
			this.params=params;
		}
		public RunnableFunction clone(){
			return new Or(returnValue,params);
		}
	}
	/** 
	 * Class responsible for the "count" function (see its help message for explanation).
	 * Also see section 6.4.6 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Count extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"Count"+TAB+TAB+"call: Count(I)\n" +
				TAB+TAB+"Counts the number of positives in image \"I\" and outputs to screen.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Count does not return an image";
			if (params.length != 1)
				return "Count only accepts one image as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Loads the matrix of interest, calls the {@link GemIdentTools.Matrices.BoolMatrix#NumberPoints() NumberPoints} function,
		 * and prints output to the screen
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix A=Load(params[0]);
					long num=A.NumberPoints();
					NumberFormat format=NumberFormat.getInstance();
					WriteToScreen("total positives in image "+params[0]+": "+format.format(num),null,Console.neutral);
					WriteToScreen("Count completed in "+Run.TimeElapsed(time),null,Console.time);
				}
			};
		}
		public Count(String returnValue,String[] params,String text){
			this.returnValue=returnValue;
			this.params=params;
			this.text=text;
		}
		public RunnableFunction clone(){
			return new Count(returnValue,params,text);
		}
	}
	/** 
	 * Class responsible for the "label" function (see its help message for explanation).
	 * Also see section 6.5.4 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Label extends RunnableFunction {
		private Double min;
		public String HelpMessage(){
			String help=TAB+"Label"+TAB+TAB+"call: Label(I,MinSize)\n" +
				TAB+TAB+"Counts the number of contiguous regions in image \"I\" using a floodfill\n" +
				TAB+TAB+"algorithm and outputs to screen if region is greater than \"MinSize\" "+Run.it.imageset.getMeasurement_unit()+"\u00B2\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Label does not return an image";
			if (params.length != 2)
				return "Label only accepts one image and one double as parameters";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			try {
				min=Double.parseDouble(params[1]);
				if (min <= 0)
					return "\"n\" must be greater than 0";
			} catch (Exception e){
				return "\""+params[1]+"\" is not a double";
			}
			return null;
		}
		/**
		 * Loads the matrix of interest, calls the {@link GemIdentCentroidFinding.CentroidFinder#FloodfillLabelPoints Floodfill Label} function,
		 * prints output to the screen, and dumps output to a file in the output folder
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					double locmin=min;
					long time=StartTimer();
					BoolMatrix B=Load(params[0]);
					NumberFormat formatLoc=NumberFormat.getNumberInstance();
					formatLoc.setMinimumFractionDigits(1);
					formatLoc.setMaximumFractionDigits(1);
					NumberFormat formatSize=NumberFormat.getNumberInstance();
					formatSize.setMinimumFractionDigits(2);
					formatSize.setMaximumFractionDigits(2);
					
					try {
						ArrayList<ArrayList<Point>> allBlobs = LabelViaSmartErosions.FloodfillLabelPoints(B, "a label blob analysis", params[0], null);
						final ArrayList<ArrayList<Point>> correctSizeBlobs=new ArrayList<ArrayList<Point>>();
						
	
	//					long size=B.SizeInPixels();
	//					double scale=size/((double)1000000);
	//					BoolMatrix display=
						Timestamp t=new Timestamp(System.currentTimeMillis());
						String filename=dumpFilenameOutputFull("Label","txt",t);
						PrintWriter out=null;
						try {
							out=new PrintWriter(new BufferedWriter(new FileWriter(filename)));
						} catch (IOException e) {
							System.out.println(filename+" cannot be edited in CSV appending");
						}
						out.print("Size_in_"+Run.it.imageset.getMeasurement_unit()+"\u00B2,x_center,y_center\n");
					
						int counter=1;
						for (ArrayList<Point> blob:allBlobs){
							if (blob.size() >= locmin){
								correctSizeBlobs.add(blob);
								DoubleMatrix c=LabelViaSmartErosions.GetCenterOfBlob(blob);
								double size=blob.size()*Math.pow(Run.it.imageset.ToNativeFromPixels(1),2);
								String sizeStr=formatSize.format(size);
								String xcent=formatLoc.format(c.get(0,0));
								String ycent=formatLoc.format(c.get(1,0));
								WriteToScreen(TAB+"Blob "+counter+"\t\t size:"+sizeStr+Run.it.imageset.getMeasurement_unit()+"\u00B2\t\t("+xcent+" , "+ycent+")",null,Console.neutral);
								counter++;
								out.print(sizeStr+","+xcent+","+ycent+"\n");
							}
						}
						out.close();
						WriteToScreen("total contiguous regions in image "+params[0]+": "+allBlobs.size(),text,Console.output);
						WriteToScreen("Data written to "+dumpFilenameOutputFull("label","txt",t),null,Console.neutral);
						WriteToScreen("Label completed in "+Run.TimeElapsed(time),null,Console.time);
				
					} catch (Throwable t){
						WriteToScreen("Not enough stack memory",null,Console.error);
					}
				}	
			};
		}
		public Label(String returnValue,String[] params,Double min,String text){
			this.returnValue=returnValue;
			this.params=params;
			this.min=min;
			this.text=text;
		}
		public RunnableFunction clone(){
			return new Label(returnValue,params,min,text);
		}
	}
	/** 
	 * Class responsible for the "NumWithinRadius" function (see its help message for explanation).
	 * Also see section 6.5.1 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class NumWithinRadius extends RunnableFunction {
		private Double radius;
		public String HelpMessage(){
			String help=TAB+"NumWithinRadius"+TAB+TAB+"call: NumWithinRadius(Ia,Ib,radius)\n" +
				TAB+TAB+"Counts all positives in image \"Ia\" within \"radius\""+Run.it.imageset.getMeasurement_unit()+" of a\n" +
				TAB+TAB+"positive in image \"Ib\". The counts and percentage is outputted to screen.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "NumWithinRadius does not return an image";
			if (params.length != 3)
				return "NumWithinRadius only accepts two same-size images and a double as parameters";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			if (!isLegalVariable(params[1]))
				return "\""+params[1]+"\" is not a recognized image";
			try {
				radius=Double.parseDouble(params[2]);
			} catch (Exception e){
				return "\""+params[2]+"\" is not a double";
			}
			return null;
		}
		/**
		 * Loads the matrices of interest, calls the {@link GemIdentTools.Matrices.BoolMatrix#NumWithinRadius(BoolMatrix, BoolMatrix, int) NumWithinRadius} function,
		 * and prints the output to the screen
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix A=Load(params[0]);
					BoolMatrix B=Load(params[1]);
					Point ratio=BoolMatrix.NumWithinRadius(A,B,Run.it.imageset.ToPixelsFromNativeAsInteger(radius));
					NumberFormat format=NumberFormat.getPercentInstance();
					format.setMinimumFractionDigits(2);
					format.setMaximumFractionDigits(2);
					double percent=ratio.x/((double)ratio.y);
					WriteToScreen("of the "+ratio.y+" of "+params[0]+"'s positives, "+ratio.x+" were within "+radius+Run.it.imageset.getMeasurement_unit()+" of "+params[1]+"'s positives, ie "+format.format(percent),text,Console.output);
					WriteToScreen("NumWithinRadius completed in "+Run.TimeElapsed(time),null,Console.time);
				}
			};	
		}
		public NumWithinRadius(String returnValue,String[] params,Double radius,String text){
			this.returnValue=returnValue;
			this.params=params;
			this.radius=radius;
			this.text=text;
		}
		public RunnableFunction clone(){
			return new NumWithinRadius(returnValue,params,radius,text);
		}
	}
	/** 
	 * Class responsible for the "distanceAwayDistr" function (see its help message for explanation).
	 * Also see section 6.5.3 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class DistanceAwayDistr extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"DistanceAwayDistr"+TAB+TAB+"call: DistanceAwayDistr(Ia,Ib)\n" +
				TAB+TAB+"For all positives in image \"Ia\", this function finds how far each one is from the closest\n" +
				TAB+TAB+"positive in image \"Ib\" (in "+Run.it.imageset.getMeasurement_unit()+"). The distribution (as a histogram) is outputted to the screen\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "DistanceAwayDistr does not return an image";
			if (params.length != 2)
				return "DistanceAwayDistr only accepts two same-size images as parameters";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			if (!isLegalVariable(params[1]))
				return "\""+params[1]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Loads the matrices of interest, calls the {@link GemIdentTools.Matrices.BoolMatrix#DistanceAwayDistr(BoolMatrix, BoolMatrix) DistanceAwayDistr} function
		 * to get the raw data, writes the data to a CVS file in the output directory, then converts the data 
		 * into an array, creates a new {@link GemIdentTools.HistogramADK#getHistogram(double[]) histogram}
		 * from the data, and displays it to console
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix A=Load(params[0]);
					BoolMatrix B=Load(params[1]);
					HashMap<Point,Double> distances=BoolMatrix.DistanceAwayDistr(A,B);
					
					double[] distancesInNativeMeasure=new double[distances.size()];
					int i=0;
					for (double d:distances.values()){
						distancesInNativeMeasure[i]=Run.it.imageset.ToNativeFromPixels(d);
						i++;
					}
					
					BufferedImage histogram=HistogramADK.getHistogram(distancesInNativeMeasure);
					if (histogram != null){
						
						NumberFormat formatDistance=NumberFormat.getNumberInstance();
						formatDistance.setMinimumFractionDigits(2);
						formatDistance.setMaximumFractionDigits(2);						
						
						Timestamp t=new Timestamp(System.currentTimeMillis());
						String filename=dumpFilenameOutputFull("Distances_From_"+params[0]+"_to_"+params[1],"txt",t);
						PrintWriter out=null;
						try {
							out=new PrintWriter(new BufferedWriter(new FileWriter(filename)));
						} catch (IOException e) {
							System.out.println(filename+" cannot be edited in CSV appending");
						}
						out.print("distances_in_"+Run.it.imageset.getMeasurement_unit());
						for (double d:distancesInNativeMeasure)
							out.print("\n"+formatDistance.format(d));
						out.close();
						WriteToScreen("the distribution of the distances (in "+Run.it.imageset.getMeasurement_unit()+") of the "+distances.size()+" of "+params[0]+"'s positives to the closest "+params[1]+" positive:",text,Console.output); //it is stored in calls and then overwritten by the image...
						WriteImageToScreen(text,histogram);
						WriteToScreen("Data written to "+dumpFilenameOutputFull("Distances_From_"+params[0]+"_to_"+params[1],"txt",t),null,Console.neutral);
						WriteToScreen("DistanceAwayDistr completed in "+Run.TimeElapsed(time),null,Console.time);
					}
					else
						WriteToScreen("Not enough points in image \""+params[0]+"\" to create distribution",null,Console.error);
				}
			};	
		}
		public DistanceAwayDistr(String returnValue,String[] params,String text){
			this.returnValue=returnValue;
			this.params=params;
			this.text=text;
		}
		public RunnableFunction clone(){
			return new DistanceAwayDistr(returnValue,params,text);
		}
	}
	/** 
	 * Class responsible for the "NumWithinRing" function (see its help message for explanation).
	 * Also see section 6.5.2 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class NumWithinRing extends RunnableFunction {
		private Double radiusA;
		private Double radiusB;
		public String HelpMessage(){
			String help=TAB+"NumWithinRing"+TAB+TAB+"call: NumWithinRing(Ia,Ib,radiusA,radiusB)\n" +
				TAB+TAB+"Counts all positives in image \"Ia\" within the ring defined by a circle of \"radiusA\"\n" +
				TAB+TAB+Run.it.imageset.getMeasurement_unit()+" minus a circle of \"radiusB\""+Run.it.imageset.getMeasurement_unit()+" of a positive in image \"Ib\".\n" +
				TAB+TAB+"The counts and percentage is outputted to screen.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "NumWithinRing does not return an image";
			if (params.length != 4)
				return "NumWithinRing only accepts two same-size images and two doubles as parameters";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			if (!isLegalVariable(params[1]))
				return "\""+params[1]+"\" is not a recognized image";
			try {
				radiusA=Double.parseDouble(params[2]);
			} catch (Exception e){
				return "\""+params[2]+"\" is not an double";
			}
			try {
				radiusB=Double.parseDouble(params[3]);
			} catch (Exception e){
				return "\""+params[3]+"\" is not an double";
			}
			return null;
		}
		/**
		 * Loads the matrices of interest, calls the {@link GemIdentTools.Matrices.BoolMatrix#NumWithinRing(BoolMatrix, BoolMatrix, int, int)
		 * NumWithinRing} function, and prints the output to the screen
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix A=Load(params[0]);
					BoolMatrix B=Load(params[1]);
					Point ratio=BoolMatrix.NumWithinRing(A,B,Run.it.imageset.ToPixelsFromNativeAsInteger(radiusA),Run.it.imageset.ToPixelsFromNativeAsInteger(radiusB));
					NumberFormat format=NumberFormat.getPercentInstance();
					format.setMinimumFractionDigits(2);
					format.setMaximumFractionDigits(2);
					double percent=ratio.x/((double)ratio.y);
					WriteToScreen("of the "+ratio.y+" of "+params[0]+"'s positives, "+ratio.x+" were within the ring defined by "+radiusA+" and "+radiusB+Run.it.imageset.getMeasurement_unit()+" of "+params[1]+"'s positives, ie "+format.format(percent),text,Console.output);
					WriteToScreen("NumWithinRing completed in "+Run.TimeElapsed(time),null,Console.time);
				}
			};	
		}
		public NumWithinRing(String returnValue,String[] params,Double radiusA,Double radiusB,String text){
			this.returnValue=returnValue;
			this.params=params;
			this.radiusA=radiusA;
			this.radiusB=radiusB;
			this.text=text;
		}
		public RunnableFunction clone(){
			return new NumWithinRing(returnValue,params,radiusA,radiusB,text);
		}
	}
	/** 
	 * Class responsible for the "Display" function (see its help message for explanation).
	 * See section 6.3.2 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Display extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"Display"+TAB+TAB+"call: Display(I)\n" +
				TAB+TAB+"Displays image \"I\" in a separate window at its native resolution.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Display does not return an image";
			if (params.length != 1)
				return "Display only accepts one image as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			return null;
		}
		/** 
		 * Loads the matrix of interest, checks to see whether or not it can be displayed based on {@link GemIdentTools.IOTools#MAX_NUM_PIXELS_BUFFERED_IMAGE_TO_DISPLAY max number of pixels to display},
		 * converts the {@link GemIdentTools.Matrices.BoolMatrix BoolMatrix} to a {@link java.awt.image.BufferedImage BufferedImage} 
		 * by calling the then calls the {@link GemIdentTools.Matrices.BoolMatrix#getBinaryImage() getBinaryImage} function, then calls 
		 * the {@link Display Display} function on it
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();					

					BoolMatrix B=Load(params[0]);
//			        view.setLayout(new ScrollPaneLayout());
					if (B.Area() > IOTools.MAX_NUM_PIXELS_BUFFERED_IMAGE_TO_DISPLAY * 1.1){
						double scale=B.Area()/IOTools.MAX_NUM_PIXELS_BUFFERED_IMAGE_TO_DISPLAY;
						scale=Math.sqrt(scale);
						NumberFormat format=NumberFormat.getInstance();
						format.setMaximumFractionDigits(2);
						WriteToScreen("The total global image size of "+params[0]+" exceeds 100Mpx which is the maximum Java",null,Console.error);
						WriteToScreen("can handle when displaying. Please scale the global image down by at least",null,Console.error);
						WriteToScreen(format.format(scale)+" and retry displaying.",null,Console.error);			
					}
					else
						IOTools.GenerateScrollablePicElement(params[0], time, B.getBinaryImage(), null, that, true);
				}
			};
		}
		public Display(String returnValue,String[] params){
			this.returnValue=returnValue;
			this.params=params;
		}
		public RunnableFunction clone(){
			return new Display(returnValue,params);
		}
	}

	/** Class responsible for the "Clear" function (see its help message for explanation) */
	private class Clear extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"Clear"+TAB+TAB+"call: Clear(I)\n" +
				TAB+TAB+"Clears image \"I\" from memory.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Clear does not return an image";
			if (params.length != 1)
				return "Clear only accepts one image as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Sets this result matrix to null in the master image map, then clears it from
		 * the left panel in the analysis page (either turning it red if its saved on the 
		 * hard disk or removing it completely)
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					BoolMatrix B=images.get(params[0]);
					if (B == null)
						WriteToScreen("image "+params[0]+" not in memory",null,Console.error);
					else {
						images.put(params[0],null);
						B=null;
						elementView.loadElement(params[0],false);
						WriteToScreen(params[0]+" cleared from memory",null,Console.neutral);
					}
				}
			};
		}
		public Clear(String returnValue,String[] params){
			this.returnValue=returnValue;
			this.params=params;
		}
		public RunnableFunction clone(){
			return new Clear(returnValue,params);
		}
	}
	/** 
	 * Class responsible for the "Load" function (see its help message for explanation).
	 * Also see section 6.3.1 of the manual
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Load extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"Load"+TAB+TAB+"call: Load(I)\n" +
				TAB+TAB+"Loads image \"I\" to memory where I is a raw image.\n" +
				TAB+TAB+"There usually does not exist a need to call load because other \n" +
				TAB+TAB+"functions will call it automatically upon needing an \n" +
				TAB+TAB+"image from the hard drive\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Load does not return an image";
			if (params.length != 1)
				return "Load only accepts one image as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Calls the {@link ConsoleParser#Load(String) Load} function
		 * and indicates a message on the screen
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					if (images.get(params[0]) != null)
						WriteToScreen(params[0]+" already in memory",null,Console.error);
					else {
						Load(params[0]);
						WriteToScreen(params[0]+" loaded from memory in "+Run.TimeElapsed(time),null,Console.time);
					}
				}
			};
		}
		public Load(String returnValue,String[] params){
			this.returnValue=returnValue;
			this.params=params;
		}
		public Load(String name){
			String[] params={name};
			this.params=params;
		}
		public RunnableFunction clone(){
			return new Load(returnValue,params);
		}
	}
	/** 
	 * Class responsible for the "Size" function (see its help message for explanation).
	 * Also see section 6.4.5 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Size extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"Size"+TAB+TAB+"call: Size(I)\n" +
				TAB+TAB+"Displays the width, height, and the size (in number of pixels) of image \"I\"\n" +
				TAB+TAB+"and outputs to screen.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Size does not return an image";
			if (params.length != 1)
				return "Size only accepts one image as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Loads image of interest and queries it for its dimensions
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					BoolMatrix B=Load(params[0]);
					NumberFormat format=NumberFormat.getInstance();
					if (B != null)
						WriteToScreen("Image "+params[0]+" has width: "+format.format(B.getWidth())+" height: "+format.format(B.getHeight())+" total pixels: "+format.format(B.Area()),null,Console.neutral);
				}
			};
		}
		public Size(String returnValue,String[] params){
			this.returnValue=returnValue;
			this.params=params;
		}
		public RunnableFunction clone(){
			return new Size(returnValue,params);
		}
	}
	/** Class responsible for the "SaveAsBMP" function (see its help message for explanation) */
	private class SaveAsTIFF extends RunnableFunction {
		private String dir;
		public String HelpMessage(){
			String help=TAB+"SaveAsBMP"+TAB+TAB+"call: SaveAsTIFF(I)\n" +
					TAB+TAB+"Saves image \"I\" to the HD as a binary bitmap in the project output folder.\n" +
					TAB+TAB+"Please note that GemIdent can open only raw images. Use this function to export\n" +
					TAB+TAB+"images to other programs\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "SaveAsBMP does not return an image";
			if (params.length != 1)
				return "SaveAsBMP only accepts one image as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Loads image of interest, checks whether its size exceeds the {@link GemIdentTools.IOTools#MAX_NUM_PIXELS_BUFFERED_IMAGE number of pixel limitation} for 
		 * bitmap images, then saves it as a .bmp file in the output directory
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix B=Load(params[0]);
					if (B.Area() > IOTools.MAX_NUM_PIXELS_BUFFERED_IMAGE){
						double scale=B.Area()/IOTools.MAX_NUM_PIXELS_BUFFERED_IMAGE;
						WriteToScreen("The total global image size of "+params[0]+" exceeds 1.6Gpx which is the maximum Java",null,Console.error);
						WriteToScreen("can handle when saving. Please scale the global image down by "+scale+" and retry save.",null,Console.error);
						WriteToScreen("Please account for this factor in your calculations.",null,Console.error);				
					}
					else {
						try {
							IOTools.WriteImage(BuildGlobals.GetGlobalFilenameWithoutBit(params[0], dir)+".tiff","TIFF",B.getBinaryImage());
//							elementView.setSaved(params[0],true); //wont' save because it can't load it up
							WriteToScreen("Save completed in "+Run.TimeElapsed(time),null,Console.time);
						} catch (Exception e){
							WriteToScreen("Error writing file",null,Console.error);
						}
					}
				}
			};
		}
		public SaveAsTIFF(String returnValue,String[] params, String dir){
			this.returnValue=returnValue;
			this.params=params;
			this.dir = dir;
		}
		public RunnableFunction clone(){
			return new SaveAsTIFF(returnValue, params, dir);
		}
	}
	/** Class responsible for the "Save" function (see its help message for explanation) */
	private class Save extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"Save"+TAB+TAB+"call: Save(I)\n" +
					TAB+TAB+"Saves image \"I\" to the HD in raw format in the project output folder.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Save does not return an image";
			if (params.length != 1)
				return "Save only accepts one image as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Loads image of interest, attempts to {@link GemIdentTools.Matrices.BoolMatrix#Save(BoolMatrix, String) Save} it
		 * in proprietary "bit" format, and sets its hard drive icon on in the left panel in the analysis page to
		 * indicate to the user its saved
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					BoolMatrix B=Load(params[0]);
					try {
						BoolMatrix.Save(B,BuildGlobals.GetGlobalFilename(params[0]));
						elementView.setSaved(params[0],true);
						WriteToScreen("Save completed in "+Run.TimeElapsed(time),null,Console.time);
					} catch (Exception e){
						WriteToScreen("Error writing file",null,Console.error);
					}
				}
			};
		}
		public Save(String returnValue,String[] params){
			this.returnValue=returnValue;
			this.params=params;
		}
		public Save(String name){
			String[] params={name};
			this.params=params;
		}
		public RunnableFunction clone(){
			return new Save(returnValue,params);
		}
	}
	/** Class responsible for the "Delete" function (see its help message for explanation) */
	private class Delete extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"Delete"+TAB+TAB+"call: Delete(I)\n" +
					TAB+TAB+"Deletes image \"I\" from the workspace and the project output folder.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Delete does not return an image";
			if (params.length != 1)
				return "Delete only accepts one image as a parameter";
			if (!isLegalVariable(params[0]))
				return "\""+params[0]+"\" is not a recognized image";
			return null;
		}
		/**
		 * Removes the image of interest from the master image map, removes the image from the
		 * hard disk, removes the image icon from the left panel in the analysis page
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					long time=StartTimer();
					images.remove(params[0]);
					elementView.setSaved(params[0],false);
					elementView.loadElement(params[0],false);
					WriteToScreen(params[0]+" cleared from memory",null,Console.neutral);
					try {
						(new File(Run.it.imageset.getFilenameWithHomePath(BuildGlobals.GetGlobalFilename(params[0])))).delete();
						WriteToScreen(params[0]+" removed from disk",null,Console.neutral);
						WriteToScreen("Delete completed in "+Run.TimeElapsed(time),null,Console.time);
					} catch (Exception e){
						WriteToScreen("error removing "+params[0]+" from disk",null,Console.error);
					}
				}
			};
		}
		public Delete(String returnValue,String[] params){
			this.returnValue=returnValue;
			this.params=params;
		}
		public RunnableFunction clone(){
			return new Delete(returnValue,params);
		}
	}
	/** 
	 * Class responsible for the "Delete" function (see its help message for explanation).
	 * See section 6.3.3 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class Correct extends RunnableFunction {
		
		/** the correction segment number */
		private Integer segmentNum;
		public String HelpMessage(){
			String help=TAB+"Correct"+TAB+TAB+"call: Correct(i)   --OR--  Correct(i,Phen1Centroids,Phen2Centroids,...)\n" +
					TAB+TAB+"Opens segment \"i\" of the global image for correction. With one parameter,\n" +
					TAB+TAB+"it will allow correction of all phenotype centroids. With arbitrary parameters,\n" +
					TAB+TAB+"it will allow correction of only those phenotype centroids specified.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "Correct does not return an image";
			if (params.length == 0)
				return "Correct must have an integer as the first parameter";
			if (params.length > 7)
				return "Correct only supports up to six phenotypes. For correction of all, call correct with just a segment number";
			if (correct == null)
				return "Please run \"CorrectErrors\" to initialize correction process";
			try {
				segmentNum=Integer.parseInt(params[0]);
				if (correct.size() == 1 && segmentNum != 1)
					return "\"i\" must be 1";
				if (segmentNum <= 0 || segmentNum > correct.size())
					return "\"i\" must be a valid segment betweeen 1 and "+correct.size();
			} catch (Exception e){
				return "Correct must have an integer as the first parameter, \""+params[0]+"\" is not an integer";
			}
			Set<String> totalSet=Run.it.getPhenotyeNamesSaveNONAndFindCenters();
			if (params.length >= 2)
				if (!(totalSet.contains(params[1]) && images.containsKey(params[1])))
					return "\""+params[1]+"\" is not a valid phenotype centroid image";
			if (params.length >= 3)
				if (!(totalSet.contains(params[2]) && images.containsKey(params[2])))
					return "\""+params[2]+"\" is not a valid phenotype centroid image";
			if (params.length >= 4)
				if (!(totalSet.contains(params[3]) && images.containsKey(params[3])))
					return "\""+params[3]+"\" is not a valid phenotype centroid image";
			if (params.length >= 5)
				if (!(totalSet.contains(params[4]) && images.containsKey(params[4])))
					return "\""+params[4]+"\" is not a valid phenotype centroid image";
			if (params.length >= 6)
				if (!(totalSet.contains(params[5]) && images.containsKey(params[5])))
					return "\""+params[5]+"\" is not a valid phenotype centroid image";
			if (params.length >= 7)
				if (!(totalSet.contains(params[6]) && images.containsKey(params[6])))
					return "\""+params[6]+"\" is not a valid phenotype centroid image";				
			return null;
		}
		/**
		 * Loads the {@link ConsoleParser.CorrectElement CorrectElement} according
		 * to the specified segment number, extracts its image, embodies it in a {@link GemIdentView.ScrollablePicture ScrollablePicture}
		 * object, creates an appropriate title for the window it appear in, adds mouse listeners
		 * to detect clicks that will erase phenotypes from the result matrices, and draws red "X"'s over
		 * these locations
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					if (Run.it.imageset instanceof NuanceImageListInterface){
						WriteToScreen("Correct function not supported for Nuance Image Sets",null,Console.error);
					}
					else {
						long time=StartTimer();
						
						final int red=Color.RED.getRGB();
						
						final CorrectElement segment=correct.get(segmentNum);
						
						
						String title="- correct TypeII errors on segment "+segmentNum+" ";
						final Set<String> set;
						if (params.length == 1){
							set=Run.it.getPhenotyeNamesSaveNONAndFindCenters();
							title+="for all phenotypes";
						}
						else {
							set=new HashSet<String>(params.length-1);
							title+="for ";
							for (int i=1;i<params.length;i++){
								set.add(params[i]);
								title+=params[i]+", ";
							}
							title=title.substring(0,title.length()-2);
						}
						
						final BufferedImage I=segment.getImageSegment(set);
						final ScrollablePicture pic=IOTools.GenerateScrollablePicElement(title, time, I, null, that, true);
						
						pic.addMouseListener(
							new MouseListener(){
								public void mouseClicked(MouseEvent arg0){}
								public void mouseEntered(MouseEvent arg0){}
								public void mouseExited(MouseEvent arg0){}
								public void mousePressed(MouseEvent e){
									Point to=e.getPoint();	
									for (Point t:Solids.GetPointsInSolidUsingCenter(3, 0D, to)){
										Point test=new Point(t.x,t.y+segment.getrowA());
										for (String phenotype:set){
											BoolMatrix centroids=Load(phenotype);
											if (centroids.get(test)){
												centroids.set(test,false);
												try { //try to draw a RED ex
													for (int i=-5;i<=5;i++){
														I.setRGB(t.x+i,t.y+i,red);
														I.setRGB(t.x+i,t.y-i,red);
														I.setRGB(t.x+i+1,t.y+i,red);
														I.setRGB(t.x+i+1,t.y-i,red);
														I.setRGB(t.x+i-1,t.y+i,red);
														I.setRGB(t.x+i-1,t.y-i,red);
													}
												} catch (Exception exc){}
											}
										}
									}								
								}
								public void mouseReleased(MouseEvent arg0){
									pic.repaint();
								}							
							}						
						);
					}
				}
			};
		}
		public Correct(String[] params,Integer segment){
			this.segmentNum=segment;
			this.params=params;
		}
		public RunnableFunction clone(){
			return new Correct(params,segmentNum);
		}
	}
	/** 
	 * Class responsible for the "DataDump" function (see its help message for explanation).
	 * Also see section 6.5.5 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private class DataDump extends RunnableFunction {
		public String HelpMessage(){
			String help=TAB+"DataDump"+TAB+TAB+"call: DataDump(Phen1Centroids,Phen2Centroids,...)\n" +
					TAB+TAB+"Dumps the coordinates of centroids of the global images to a CSV text file.\n" +
					TAB+TAB+"The function can be overloaded to take up to seven global images.\n";
			return help;
		}
		protected String checkIfOkay(){
			if (returnValue != null)
				return "DataDump does not return an image";
			if (params.length == 0)
				return "DataDump must have at least one image as a parameter";
			if (params.length > 7)
				return "Correct only supports up to seven phenotypes.";
			Set<String> totalSet=Run.it.getPhenotyeNamesSaveNONAndFindCenters();
			if (params.length >= 1)
				if (!(totalSet.contains(params[0]) && images.containsKey(params[0])))
					return "\""+params[1]+"\" is not a valid phenotype centroid image";
			if (params.length >= 2)
				if (!(totalSet.contains(params[1]) && images.containsKey(params[1])))
					return "\""+params[1]+"\" is not a valid phenotype centroid image";
			if (params.length >= 3)
				if (!(totalSet.contains(params[2]) && images.containsKey(params[2])))
					return "\""+params[2]+"\" is not a valid phenotype centroid image";
			if (params.length >= 4)
				if (!(totalSet.contains(params[3]) && images.containsKey(params[3])))
					return "\""+params[3]+"\" is not a valid phenotype centroid image";
			if (params.length >= 5)
				if (!(totalSet.contains(params[4]) && images.containsKey(params[4])))
					return "\""+params[4]+"\" is not a valid phenotype centroid image";
			if (params.length >= 6)
				if (!(totalSet.contains(params[5]) && images.containsKey(params[5])))
					return "\""+params[5]+"\" is not a valid phenotype centroid image";
			if (params.length >= 7)
				if (!(totalSet.contains(params[6]) && images.containsKey(params[6])))
					return "\""+params[6]+"\" is not a valid phenotype centroid image";				
			return null;
		}
		/**
		 * For each result matrix specified, open it, invoke the {@link GemIdentTools.Matrices.BoolMatrix#getPositivePoints() getPositivePoints}
		 * to get a list of the positive coordinates, then create a dump the coordinates to a text file
		 */
		public Runnable getOperation(){
			return new Runnable(){
				public void run(){
					
					long time=StartTimer();
					
					int P=params.length;
					String name="Coordinates_of";
					
					//get data from images
					int maxNumPts=Integer.MIN_VALUE;
					LinkedHashMap<String,ArrayList<Point>> phenotypeLocations=new LinkedHashMap<String,ArrayList<Point>>(P);
					for (String param:params){
						name+="_"+param;
						BoolMatrix B=Load(param);
						ArrayList<Point> points = B.getPositivePoints();
						if (points.size() > maxNumPts)
							maxNumPts=points.size();
						phenotypeLocations.put(param,points);
					}
					
					//establish file writer
					Timestamp timestamp=new Timestamp(System.currentTimeMillis());
					String filename=dumpFilenameOutputFull(name,"txt",timestamp);
					PrintWriter out=null;
					try {
						out=new PrintWriter(new BufferedWriter(new FileWriter(filename)));
					} catch (IOException e) {
						System.out.println(filename+" cannot be edited in CSV appending");
					}
					
					//print fileheader
					out.print(params[0]+"_x,"+params[0]+"_y");
					for (int p=1;p<P;p++)
						out.print(","+params[p]+"_x,"+params[p]+"_y");
					out.print("\n");
					
					//write data out
					ArrayList<Point> list=null;
					for (int i=0;i<maxNumPts;i++){
						list=phenotypeLocations.get(params[0]);
						try {
							Point t=list.get(i);
							out.print(t.x+","+t.y);
						} catch (Exception e){
							out.print(",");
						}
						for (int p=1;p<P;p++){
							list=phenotypeLocations.get(params[p]);
							try {
								Point t=list.get(i);
								out.print(","+t.x+","+t.y);
							} catch (Exception e){
								out.print(",,");
							}
						}
						out.print("\n");
					}
					out.close();
					
					//give the user an update of what's going on
					WriteToScreen("Data written to "+dumpFilenameOutputFull(name,"txt",timestamp),null,Console.neutral);
					WriteToScreen("DataDump completed in "+Run.TimeElapsed(time),null,Console.time);
				}
			};
		}
		public DataDump(String[] params){
			this.params=params;
		}
		public RunnableFunction clone(){
			return new DataDump(params);
		}
	}
	
	/**
	 * Initializes the Console Parser by initializing the thread pool, and initializes 
	 * each function inside the function mappings 
	 * 
	 * @param images			the mapping from image names to the images themselves
	 * @param elementView		the object that controls the icon display on the left panel of the analysis page
	 */
	public ConsoleParser(HashMap<String,BoolMatrix> images,ElementView elementView){
		this.that = this;
		this.images=images;
		this.elementView=elementView;
		
		sychronize=new Object();
		pool=Executors.newFixedThreadPool(NUM_CONCURRENT_TASKS);
		
		allFunctions=new HashMap<String,RunnableFunction>();
		
		binaryImageFunctions=new LinkedHashMap<String,RunnableFunction>();
		binaryImageFunctions.put("Erode",new Erode(null,null,null));
		binaryImageFunctions.put("Dilate",new Dilate(null,null,null));		
		binaryImageFunctions.put("Or",new Or(null,null));

		
		analysisFunctions=new LinkedHashMap<String,RunnableFunction>();		
		analysisFunctions.put("Label",new Label(null,null,null,null));
		analysisFunctions.put("NumWithinRadius",new NumWithinRadius(null,null,null,null));
		analysisFunctions.put("NumWithinRing",new NumWithinRing(null,null,null,null,null));		
		analysisFunctions.put("DistanceAwayDistr",new DistanceAwayDistr(null,null,null));
		analysisFunctions.put("DataDump",new DataDump(null));
		
		imageFunctions=new LinkedHashMap<String,RunnableFunction>();
		imageFunctions.put("Display",new Display(null,null));
		imageFunctions.put("SaveAsTIFF",new SaveAsTIFF(null,null,null));
		imageFunctions.put("Save",new Save(null,null));
		imageFunctions.put("Clear",new Clear(null,null));		
		imageFunctions.put("Load",new Load(null,null));
		imageFunctions.put("Delete",new Delete(null,null));
		imageFunctions.put("Correct",new Correct(null,null));
		imageFunctions.put("Scale",new Scale(null,null,null));
		imageFunctions.put("Size",new Size(null,null));
		imageFunctions.put("Count",new Count(null,null,null));
		
		allFunctions.putAll(binaryImageFunctions);
		allFunctions.putAll(analysisFunctions);
		allFunctions.putAll(imageFunctions);
	}
	/** is this image name contained in the master image map? */
	public boolean isLegalVariable(String var){
		if (images.containsKey(var))
			return true;
		else 
			return false;
	}
	public void addConsole(Console console) {
		this.console=console;		
	}
	/**
	 * Parse text from the user. The function internal is synchronized so the {@link #Join() Join}
	 * function will prevent new functions from being invoked. This is useful when executing
	 * {@link #RunScript(File) script files} as well.
	 * The text is first checked to see if it is a {@link #SystemFunction(String) system function},
	 * then it is checked for illegal symbol characters, then it is checked to see if 
	 * it contains the text for all the functions in the master map, if so, that text
	 * is checked for consistency within that function using the {@link RunnableFunction#ParseFunctionParamsAndReturn(String, String) setText}.
	 * If there is an error, that error is displayed, if not, the function is invoked by cloning the function object
	 * and executing it. This strategy was chosen so that each instance of a function execution is
	 * independent.
	 * 
	 * @param text					the text entered by user or supplied by one line from a script file
	 */
	public void Parse(String text){
		synchronized(sychronize){
			if (SystemFunction(text))
				return;
			Pattern p;
			Matcher m;
			p=Pattern.compile("[\\W&&[^=(),.]]",Pattern.CASE_INSENSITIVE);
			m=p.matcher(text);		
			if (!m.find()){ //reject if invalid characters
				String[] parenparts = text.split("\\(");				
				if (parenparts.length == 2){
					String relevant_text = parenparts[0];
					String[] eqparts = relevant_text.split("=");
					if (eqparts.length == 2)
						relevant_text = eqparts[1];
					for (String function:allFunctions.keySet()){
						p=Pattern.compile(function,Pattern.CASE_INSENSITIVE);
						m=p.matcher(relevant_text); //match only that before
						if (m.find()){
							RunnableFunction f=allFunctions.get(function);
							String message=f.ParseFunctionParamsAndReturn(text,function);
							if (message == null){
								WriteToScreen("Processing \""+text+"\"",null,Console.neutral);
								pool.execute((f.clone()).getOperation()); //the cloning ensures no problems with simultaneity of functions (it does waste a bit of memory as the original object is never used any longer)					
							}
							else 
								WriteToScreen(message,null,Console.error);
							return;
						}
					}
				}
			}
			WriteToScreen("Command \""+text+"\" is unrecognized",null,Console.error);
		}
	}
	/**
	 * Checks to see if a given text corresponds to one of the system functions 
	 * (functions that do not take parameters that usually do something general)
	 * and if so, invokes that function
	 * 
	 * @param text		the text to check
	 * @return			whether or not this text was a system function
	 */
	private boolean SystemFunction(String text){
		if ("mem".equals(text.toLowerCase())){
			MemoryUsed();
			return true;
		}
		if ("help".equals(text.toLowerCase())){
			Help();
			return true;
		}
		if ("clear".equals(text.toLowerCase())){
			Clear();
			return true;
		}
		if ("build".equals(text.toLowerCase())){
			pool.execute(Build());
			return true;
		}
		if ("clean".equals(text.toLowerCase())){
			pool.execute(Clean());
			return true;
		}
		if ("thumb".equals(text.toLowerCase())){
			pool.execute(Thumb());
			return true;
		}
		if ("report".equals(text.toLowerCase())){
			pool.execute(Report());
			return true;
		}
		if ("saveall".equals(text.toLowerCase())){
			SaveAll();
			return true;
		}
		if ("loadall".equals(text.toLowerCase())){
			LoadAll();
			return true;
		}
		if ("correcterrors".equals(text.toLowerCase())){
			pool.execute(CorrectErrors());
			return true;
		}
		if ("join".equals(text.toLowerCase())){
			Join();
			return true;
		}
		if ("sanitycheck".equals(text.toLowerCase())){
			SanityCheck();
			return true;
		}		
		return false;
	}
	/** forces all jobs executing in the thread pool to finish before parsing new text.
	 * See section 6.2.10 of the manual
	 *  
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a> 
	 */
	private void Join(){
		synchronized(sychronize){
			pool.shutdown();
			try {	         
		         pool.awaitTermination(Long.MAX_VALUE,TimeUnit.SECONDS); //effectively infinity
		    } catch (InterruptedException ignored){}
		    pool=Executors.newFixedThreadPool(NUM_CONCURRENT_TASKS);
		    WriteToScreen("Awaiting next task . . .",null,Console.time);
		}
	}
	/** builds and creates thumbnail overviews of each phenotype
	 */	
	private static final String scaled_indicator = "_scaled";
	private static final String sanitycheck_indicator = "_sanitycheck";
	private static final double sanitycheckscalefactor = 0.5;
	public void SanityCheck(){
		NumberFormat format=NumberFormat.getInstance();
		format.setMaximumFractionDigits(2);	
		
		//first do a build
		pool.execute(Build());
		Join();
		
		WriteToScreen("Determining how best to reduce images . . .", null, Console.neutral);
		//first find mins and maxs of the images
		int xo = Integer.MAX_VALUE;
		int yo = Integer.MAX_VALUE;
		int xf = Integer.MIN_VALUE;
		int yf = Integer.MIN_VALUE;
		int true_width = 0;
		int true_height = 0;
		for (String imagename : elementView.getAllElementNamesOfType(ElementView.PIXEL)){
			BoolMatrix image = images.get(imagename);
			true_width = image.getWidth();
			true_height = image.getHeight();
			for (int i = 0; i < true_width; i++){
				for (int j = 0; j < true_height; j++){
					if (image.get(i, j)){
						if (i < xo){
							xo = i;
						}
						else if (i > xf){
							xf = i;
						}
						if (j < yo){
							yo = j;
						}
						else if (j > yf){
							yf = j;
						}
					}
				}			
			}
		}
		long num_cropped_pixels = ((xf - xo) * (yf - yo));
		WriteToScreen("Cropping images at (" + xo + "," + xf + "," + yo + "," + yf + ") of (" + true_width + "," +  true_height + ") or " + format.format(num_cropped_pixels / ((double)(true_height * true_width)) * 100) + "% . . .", null, Console.neutral);
		//now crop all the images
		for (BoolMatrix image : images.values()){
			image = image.crop(xo, xf, yo, yf);
		}
		
		//now determine the scale
		double scale = num_cropped_pixels / (IOTools.MAX_NUM_PIXELS_BUFFERED_IMAGE_TO_DISPLAY * sanitycheckscalefactor);
		if (scale < 1){
			scale = 1;
		}

		WriteToScreen("Images will be scaled at " + format.format(1 / scale * 100) + "%", null, Console.neutral);
		
		//now for each image in the hash, run a scale at a magnitude of scale_factor_for_sanity_check_images
		WriteToScreen("Scaling all classification results . . .", null, Console.neutral);
		HashSet<String> scaled_images = new HashSet<String>(images.size()); //we're going to delete these later
		for (String name : images.keySet()){
			String[] params = new String[1]; //Java is so dumb
			params[0] = name;
			pool.execute((new Scale(name + scaled_indicator, params, scale)).getOperation());
			scaled_images.add(name + scaled_indicator);
		}
		Join();
		
		WriteToScreen("Creating composite images and saving to HD . . .",null,Console.neutral);
		//now for each phenotype, create a cool composite image with stars where the centroids are
		HashSet<String> comp_images = new HashSet<String>(images.size()); //we're going to delete these later
		for (String phenotype : Run.it.getPhenotyeNamesSaveNON()){
			BoolMatrix is = images.get(phenotype + BuildGlobals.Pixels + scaled_indicator);
			if (Run.it.getPhenotype(phenotype).isFindCentroids()){
				is.DrawStarsAt(images.get(phenotype + BuildGlobals.Centroids + scaled_indicator));
			}
			//now save this image as a bmp
			String name = phenotype + sanitycheck_indicator;
			images.put(name, is);	
			elementView.addElement(name, ElementView.OTHER, true, false);
			String[] params = new String[1]; //Java is so dumb	
			params[0] = name;
			pool.execute((new SaveAsTIFF(null, params, ImageSetInterface.checkDir)).getOperation());
			comp_images.add(name);
		}
		Join();
		WriteToScreen("Done . . . now displaying . . .",null,Console.neutral);		
		
		//now show them on the screen:
		for (String comp_image : comp_images){
			String[] params = new String[1]; //Java is so dumb	
			params[0] = comp_image;
			pool.execute((new Display(null, params)).getOperation());
		}
		Join();
		WriteToScreen("Cleaning up . . .",null,Console.neutral);
		for (String comp_image : comp_images){
			String[] params = new String[1]; //Java is so dumb	
			params[0] = comp_image;
			pool.execute((new Delete(null, params)).getOperation());
		}
		for (String scaled_image : scaled_images){
			String[] params = new String[1]; //Java is so dumb	
			params[0] = scaled_image;
			pool.execute((new Delete(null, params)).getOperation());
		}
		Join();
		//now delete all images because they're all the cropped ones
		Clear();		
		WriteToScreen("Done with sanity check . . . you can find the images in the \"/checks\" folder",null,Console.neutral);
	}
	/** 
	 * saves all images in the master map to the hard disk by invoking 
	 * {@link ConsoleParser.Save Save} on each. See section 6.2.3 of the
	 * manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private void SaveAll() {
		WriteToScreen("Saving all images . . .",null,Console.neutral);
		for (String name:images.keySet())
			pool.execute((new Save(name)).getOperation());
	}
	/** 
	 * loads all images into the master map from the hard disk by invoking 
	 * {@link ConsoleParser.Load Load} on each. See section 6.2.4 of the manual.
	 * 
	 *  @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private void LoadAll() {
		WriteToScreen("Loading all images . . .",null,Console.neutral);
		for (String name:images.keySet())
			if (elementView.isSaved(name))
				pool.execute((new Load(name)).getOperation());
	}
	/** 
	 * Creates a thumbnail of the global image, 
	 * displays it, and saves it to the hard disk. See
	 * section 6.2.7 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private Runnable Thumb() {
		return new Runnable(){
			public void run(){
				long time=StartTimer();
				WriteToScreen("Processing \"thumb\"",null,Console.neutral);
				BufferedImage I=Run.it.imageset.getGlobalThumbnailImage(ReportInPDF.thumbWidth);
				IOTools.GenerateScrollablePicElement(ImageSetInterface.globalFilenameAlone, time, I, Run.it.imageset.getFilenameWithHomePath((ImageSetInterface.globalFilenameAndPath)), that, true);
			}
		};
	}
	/** 
	 * Deletes all files in the "processed" directory - individual result 
	 * files from the classification. See section 6.2.5 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private Runnable Clean(){
		return new Runnable(){
			public void run(){
				WriteToScreen("Deleting all files in \\processed . . .",null,Console.neutral);
				Classify.PromptToDeleteAlProjectFiles();
				WriteToScreen("Done",null,Console.neutral);
			}
		};
	}
	/** 
	 * Builds the global result matrices from the individual result 
	 * files by using the {@link BuildGlobals BuildGlobals} class. It then
	 * adds those newly built images to the master image map as well as to the
	 * left panel in the analysis page as icons. Also see section 6.2.1 of the
	 * manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private Runnable Build(){
		return new Runnable(){
			public void run(){
				WriteToScreen("Building global images . . .",null,Console.neutral);
				long time_o=StartTimer();		
				try {
					BuildGlobals build=new BuildGlobals();
					HashMap<String,BoolMatrix> globals=build.getGlobals();
					HashMap<String,Integer> counts=build.getCounts();
					int N=build.getTotNumPics();
					for (String phenotype:globals.keySet()){
						elementView.SetUpInitialElement(phenotype);				
						images.put(phenotype,globals.get(phenotype));
						WriteToScreen(TAB+"built "+phenotype+" with "+counts.get(phenotype)+" of the "+N+" total images",null,Console.neutral);
					}
					WriteToScreen("Done in "+Run.TimeElapsed(time_o),null,Console.time);
			} catch (Exception e){
					WriteToScreen("Classification or centroid files not found",null,Console.error);
				}
			}
		};
	}
	/** 
	 * Sets all result matrices to null, counts those that have been 
	 * cleared, and updates the left panel in the analysis page. See
	 * section 6.2.2 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private void Clear(){
		int count=0;
		for (String name:images.keySet()){
			BoolMatrix B=images.get(name);
			if (B != null){
				images.put(name,null);
				B=null;
				count++;
				elementView.loadElement(name,false);
			}				
		}
		if (count > 1)
			WriteToScreen(count+" images cleared from memory",null,Console.neutral);	
		else if (count == 1)
			WriteToScreen("1 image cleared from memory",null,Console.neutral);	
		else
			WriteToScreen("no images in memory",null,Console.error);	
	}
	/** 
	 * Displays the help message to the screen. For each function, it calls and prints
	 * the {@link RunnableFunction#HelpMessage() HelpMessage} function.
	 */
	private void Help(){
		WriteToScreen(TAB+"The following system functions are supported:\n",null,Console.neutral);
		WriteToScreen(
				TAB+"\"Build\" - build global images from identified subimages\n" +
			TAB+TAB+"\"SanityCheck\" - build global phenotype images and show scaled views of the \n" +
			TAB+TAB+"                composite of pixel/center classification for each phenotype\n" +
			TAB+TAB+"\"Clear\" - clear all images from memory\n" +
			TAB+TAB+"\"SaveAll\" - save all images to hard drive\n" +
			TAB+TAB+"\"LoadAll\" - load all images from hard drive to memory\n" +
			TAB+TAB+"\"Clean\" - delete all intermediate classification files (cannot be undone, ensure globals built)\n" +
			TAB+TAB+"\"Mem\" - how much memory is currently being used by images\n" +
			TAB+TAB+"\"Thumb\" - generate a small thumbnail image of image set\n" +
			TAB+TAB+"\"CorrectErrors\" - generate segments of the global image. Type II errors can be removed\n" +
			TAB+TAB+"                  from the results by calling function \"correct\" on the given segment\n" +
			TAB+TAB+"\"Report\" - generate a PDF report including a thumbnail of the image,\n" +
			TAB+TAB+"           an identification summary, and a transcript of analyses\n" +
			TAB+TAB+"\"Join\" - force computer to finish all tasks currently being processed before acception new\n" +
			TAB+TAB+"         tasks (useful during scripting)\n",
			null,Console.help);
			
		WriteToScreen(TAB+"The following basic image operations are supported:\n",null,Console.neutral);			
		for (RunnableFunction function:imageFunctions.values())
			WriteToScreen(function.HelpMessage(),null,Console.help);		
		WriteToScreen(TAB+"The following binary image manipulations are supported:\n",null,Console.neutral);	
		for (RunnableFunction function:binaryImageFunctions.values())
			WriteToScreen(function.HelpMessage(),null,Console.help);	
		WriteToScreen(TAB+"The following analysis functions are supported:\n",null,Console.neutral);	
		for (RunnableFunction function:analysisFunctions.values())
			WriteToScreen(function.HelpMessage(),null,Console.help);	
	}
	/**
	 * Generates a {@link GemIdentTools.ReportInPDF#ReportInPDF(LinkedHashMap, LinkedHashMap, LinkedHashMap, Timestamp) PDF report} complete with a thumbnail, counts of the phenotypes,
	 * type I error rates, and a transcript of the analyses run with their output. See section 6.2.8
	 * of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private Runnable Report(){
		return new Runnable(){
			public void run(){
				WriteToScreen("Attempting to generate report . . .",null,Console.neutral);
				Timestamp t=new Timestamp(System.currentTimeMillis());
				try {
					LinkedHashMap<String,Long> totalCounts = Run.it.getTotalCounts();
					LinkedHashMap<String,Double> errorRates = Run.it.getErrorRates();
					if (totalCounts == null || errorRates == null){
						WriteToScreen("Report will not include total counts nor error rates because",null,Console.warning);
						WriteToScreen("a full classifications with centers has not been run",null,Console.warning);
					}					
					new ReportInPDF(totalCounts,errorRates,console.getCalls(),t);
					WriteToScreen("Report generated in "+dumpFilenameOutputFull("report","pdf",t)+", opening . . .",null,Console.neutral);
				}
				catch (Exception e){
					e.printStackTrace();
					WriteToScreen("Error generating report "+dumpFilenameOutputFull("report","pdf",t),null,Console.error);
					WriteToScreen("If the file is currently open, please close it",null,Console.error);
				}
			}
		};
	}
	/** 
	 * cycles through the images in the image map and adds up the 
	 * memory being used and writes it to the console. See section
	 * 6.2.6 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private void MemoryUsed(){
		int numImages=0;
		long mem=0;
		for (BoolMatrix B:images.values())
			if (B != null){
				mem+=(long)B.Area();	
				numImages++;
			}
		if (numImages == 0){
			WriteToScreen("no images in memory",null,Console.error);
			return;
		}
		mem/=8;
		double gigs=mem/Math.pow(10,9);
		NumberFormat format=NumberFormat.getInstance();
		format.setMaximumFractionDigits(2);
		if (gigs > 1){
			WriteToScreen("Total memory used by "+numImages+" images: "+format.format(gigs)+"G",null,Console.neutral);
			return;
		}
		double megs=mem/Math.pow(10,6);
		WriteToScreen("Total memory used by "+numImages+" images: "+format.format(megs)+"M",null,Console.neutral);			
	}
	/**
	 * A framework to support deletion of false positive points
	 * from a "slice" of the entire global image
	 */
	private class CorrectElement {

		/** the row of the global image that represents the first row in this slice */
		private int rowA;
		/** the row of the global image that represents the last row in this slice */
		private int rowB;

		/** default constructor */
		public CorrectElement(int rowA,int rowB){
			this.rowA=rowA;
			this.rowB=rowB;
		}
		public int getrowA(){
			return rowA;
		}
		/**
		 * Retrieves a slice of the global image beginning at rowA and ending at rowB by
		 * invoking the {@link GemIdentImageSets.ImageSetInterface#getGlobalImageSlice(int, int) getGlobalImageSlice}
		 * function. Then it paints little markers where each of the phenotypes' positives are in
		 * the color of the respective phenotype's {@link GemIdentModel.TrainSuperclass#getDisplayColor() display color}
		 * 
		 * @param phenotypes		the set of phenotypes in which to mark positives on the image slice	
		 * @return					the global image slice painted with markers for each of the phenotypes
		 */
		public BufferedImage getImageSegment(Set<String> phenotypes){
			BufferedImage trueImage=Run.it.imageset.getGlobalImageSlice(rowA,rowB);
			for (String phenotype:phenotypes){
				BoolMatrix centroids=Load(phenotype);
				if (centroids == null)
					WriteToScreen("built image \""+phenotype+"\" not in built",null,Console.error);
				else {
					int color=Run.it.getPhenotypeDisplayColor(phenotype).getRGB();
					int black=Color.BLACK.getRGB();
					for (int j=rowA;j<=rowB;j++){
						for (int i=0;i<Run.it.imageset.getGlobalWidth(true);i++){
							if (centroids.get(i,j)){
								for (Point t:Solids.GetPointsInSolidUsingCenter(3, 0D, new Point(i,j))){
									try {
										trueImage.setRGB(t.x,t.y-rowA,black);
									} catch (Exception e){}
								}
								for (Point t:Solids.GetPointsInSolidUsingCenter(2, 0D, new Point(i,j))){
									try {
										trueImage.setRGB(t.x,t.y-rowA,color);
									} catch (Exception e){}
								}
								try {
									trueImage.setRGB(i,j-rowA,black);
								} catch (Exception e){}								
							}
						}
					}
				}
			}
			return trueImage;
		}		
	}
	/**
	 * Initialize the error correction capability. It splits the global image into
	 * "k" segments for later correcting based on the {@link IOTools#MAX_NUM_PIXELS_BUFFERED_IMAGE_TO_DISPLAY maximum
	 * number of pixels to display}. See section 6.2.9 of the manual.
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	private Runnable CorrectErrors(){
		return new Runnable(){
			public void run(){
				boolean okay=true;
				long time=StartTimer();
				BoolMatrix example;
				long numP = 0;
				for (String phenotype:Run.it.getPhenotyeNamesSaveNONAndFindCenters()){
					example=Load(phenotype);
					if (example == null){
						WriteToScreen("built image \""+phenotype+"\" not in built",null,Console.error);
						okay=false;
					}
					else
						numP=example.Area();
				}
				if (okay){
					int num=(int)Math.ceil(numP/IOTools.MAX_NUM_PIXELS_BUFFERED_IMAGE_TO_DISPLAY);
					
					correct=new HashMap<Integer,CorrectElement>(num);
		//			int totrows=BlissImageList.getPicNumTable().getWidth()*(BlissImageList.getYf()-BlissImageList.getYo());
					int totrows=Run.it.imageset.getGlobalHeight(true);
					int row=0;
					int numrowsper=(int)Math.ceil(totrows/((double)num));
					for (int i=1;i<=num;i++){
						int rowB=row+numrowsper;
						if (rowB > totrows)
							rowB=totrows;
						correct.put(i,new CorrectElement(row,rowB));
						row+=numrowsper;
					}
					switch (num){
						case 1: WriteToScreen("created a single correction segment in "+Run.TimeElapsed(time),null,Console.time); break;
						case 2: WriteToScreen("created correction segments 1,2 in "+Run.TimeElapsed(time),null,Console.time); break;
						case 3: WriteToScreen("created correction segments 1,2,3 in "+Run.TimeElapsed(time),null,Console.time); break;
						case 4: WriteToScreen("created correction segments 1,2,3,4 in "+Run.TimeElapsed(time),null,Console.time); break;
						default:WriteToScreen("created correction segments 1,2, . . .,"+num+" in "+Run.TimeElapsed(time),null,Console.time);
					}			
				}
				else
					WriteToScreen("rebuild by executing \"build\"",null,Console.error);	
			}
		};
	}
	/**
	 * Write text to the console
	 * 
	 * @param message		the text to write
	 * @param call			the call / function invokation associated with this text
	 * @param attr			the formatting to display this text
	 */
	public void WriteToScreen(String message,String call,SimpleAttributeSet attr){
		console.append_to_area(TAB+message+"\n",call,attr);
	}
	/**
	 * Display an image to the console by calling {@link GemIdentView.Console#appendImage(String, BufferedImage)
	 * append image}
	 * 
	 * @param call		the call / function invokation associated with the image
	 * @param image		the image to be displayed
	 */
	private void WriteImageToScreen(String call,BufferedImage image){
		console.append_to_area("\n"+TAB,null,null);
		console.appendImage(call,image);
	}
	/** get the current time */
	private long StartTimer(){
		return System.currentTimeMillis();
	}
	/**
	 * Run a script in the console where every line represents a command.
	 * The function internal is threaded in order not to hog swing. See 
	 * section 6.6 of the manual for more information.
	 * 
	 * @param file			the file to be executed
	 * 
	 * @see <a href="http://www.gemident.com/manual.html">the GemIdent manual</a>
	 */
	public void RunScript(final File file) {
		
		console.append_to_area("Running script \""+file.getAbsolutePath()+"\" . . .\n",null,Console.neutral);
		
		new Thread(){
			public void run(){
				long time=StartTimer();
				
				BufferedReader in=null;
				try {
					in = new BufferedReader(new FileReader(file));
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				while (true){
					String command=null;
					try {
						command = in.readLine();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					if (command == null)
						break;
					else {
						console.append_to_area(command+"\n",null,Console.entered);
						Parse(command);
					}
				}
				Join();				
				WriteToScreen("Script completed in "+Run.TimeElapsed(time),null,Console.time);
			}							
		}.start();		
	}
	/**
	 * Gets the filename and absolute path of data dumped from the result of a function
	 * 
	 * @param function		the function that created the data
	 * @param ext			the extension of the dump file (usually "txt")
	 * @param t				the timestamp when the file was generated
	 * @return				the absolute path and filename
	 */
	public static String dumpFilenameOutputFull(String function,String ext,Timestamp t){
		return Run.it.imageset.getFilenameWithHomePath(dumpFilenameOutput(function,ext,t));
	}
	/**
	 * Gets the filename and relative path (to the project folder) of data dumped from the result of a function
	 * 
	 * @param function		the function that created the data
	 * @param ext			the extension of the dump file (usually "txt")
	 * @param t				the timestamp when the file was generated
	 * @return				the absolute path and filename
	 */
	@SuppressWarnings("deprecation")
	public static String dumpFilenameOutput(String function,String ext,Timestamp t){
		String timestamp=t.toString();
		String date=timestamp.split(" ")[0];
		return PostProcess.analysisDir+File.separator+Run.it.projectName+"-"+function+"--"+date+"--"+t.getHours()+"-"+t.getMinutes()+"--"+t.getSeconds()+"."+ext;
	}
}