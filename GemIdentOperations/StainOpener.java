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

package GemIdentOperations;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import GemIdentModel.Stain;
import GemIdentView.JProgressBarAndLabel;

/**
 * Class responsible for opening a "Mahalanobis Cube" from a file
 * on the hard disk. StainOpener can also be threaded in a thread pool.
 * 
 * @author Adam Kapelner
 */
public class StainOpener implements Runnable{

	/** the {@link GemIdentModel.Stain Stain} object that the cube will be applied to once opened */
	private Stain stain;
	/** the progress bar that gives the user updates on the entire opening progress */
	private JProgressBarAndLabel openProgress;
	/** the progress bar increases this amount after the cube is successfully opened */
	private int increment;
	
	/** default constructor */
	public StainOpener(Stain stain, JProgressBarAndLabel openProgress,int increment){
		this.stain=stain;	
		this.openProgress=openProgress;
		this.increment=increment;
	}
	/** 
	 * {@link #openCube(String) opens} the cube and stores 
	 * it within the {@link GemIdentModel.Stain Stain} object 
	 */
	public void run(){
		stain.SetMahalCube(openCube(stain.getName()));
	}
	/**
	 * Opens a 256x256x256 array of shorts (a "Mahalanobis Cube")
	 * from a binary dump (see the {@link StainMaker#saveCube(short[][][] cube, String filename)
	 * save} function) of it to the hard drive
	 * 
	 * @param filename		the filename that stores the dump
	 * @return				the Mahalanobis Cube fully hydrated with the values from the file
	 */
	private short[][][] openCube(String filename) {
		
		short[][][] cube=new short[256][256][256];

		DataInputStream in=null;
		try {
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(Run.it.imageset.getFilenameWithHomePath(StainMaker.colorSubDir+File.separator+filename))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		
		try {			
			for (int i=0;i<256;i++)
				for (int j=0;j<256;j++)
					for (int k=0;k<256;k++){
						if (i == 128 && j == 128 && k == 128)
							if (openProgress != null)
								openProgress.setValue(openProgress.getValue()+increment/2);
						cube[i][j][k]=in.readShort();
					}

			in.close();	
			
		}
		catch (IOException except){
			System.out.println("file error opening "+filename);
		}
		if (openProgress != null)
			openProgress.setValue(openProgress.getValue()+increment/2);
		return (cube);		
	}
}