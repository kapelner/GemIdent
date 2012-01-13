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

package GemIdentView;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;

/** 
 * a window that allows the user to choose 
 * a display color for the training points 
 */
@SuppressWarnings("serial")
public class ColorChooser extends JFrame {
	
	/** the built-in Java class does all the work for us */ 
	private JColorChooser color_chooser;
	private JButton button_ok;
	
	/** sets the title, adds "okay" and "cancel" buttons, and determines what to do upon submission */
	public ColorChooser() {
		super();
		setTitle("Select display color");
		color_chooser = new JColorChooser();
		setVisible(true);
		Container frame_contents = Box.createVerticalBox();
		frame_contents.add(color_chooser);
		
		button_ok = new JButton("OK");		
		button_ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);					
			}
		});
		JButton button_cancel = new JButton("Cancel");
		button_cancel.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				setVisible(false);
			}
		});
		Container button_row = Box.createHorizontalBox();
		button_row.add(button_ok);
		button_row.add(button_cancel);
		frame_contents.add(button_row);
		
		add(frame_contents);
		pack();
	}
	
	public void addOkayListener(ActionListener okaylistener) {
		button_ok.addActionListener(okaylistener);
	}
	
	public Color getChosenColor(){
		return color_chooser.getColor();
	}
}