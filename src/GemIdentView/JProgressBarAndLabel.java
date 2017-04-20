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

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeListener;

/**
 * Very simple class that combines a progress bar
 * and a label together. Undocumented due to simplicity.
 * 
 * @author Adam Kapelner
 */
public class JProgressBarAndLabel {

	private JProgressBar bar;
	private JLabel label;
	private String title;
	private final Box box;
	
	public JProgressBarAndLabel(int a,int b,String title){
		this.title=title;
		bar=new JProgressBar(a,b);
		bar.setStringPainted(true); 
		label=new JLabel(title);
		
		box=Box.createVerticalBox();
		box.add(label);
		box.add(bar);
	}
	public void AppendTitle(String append){
		label.setText(title+append);
	}
	public Box getBox(){
		return box;
	}
	public void addChangeListener(ChangeListener listener) {
		bar.addChangeListener(listener);		
	}
	public int getValue() {
		return bar.getValue();
	}
	public void setValue(int v){
		bar.setValue(v);		
	}
	public void Disable() {
		bar.setEnabled(false);
		bar.setFocusable(false);
		label.setEnabled(false);
		label.setFocusable(false);
	}
}