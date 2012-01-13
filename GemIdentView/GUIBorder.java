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

import javax.swing.border.*;
import java.awt.*;

/**
 * Creates a titled border around a GUI component
 * 
 * @author Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public class GUIBorder extends AbstractBorder {
	
	/** the title of the component being bordered */
	private String title;

	/** default constructor */
	public GUIBorder( String title ) {
		this.title = title;
	}
	
	/** paints the border around a component */
	public void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
		int x_0 = x+5;
		int x_1 = x+width-5;
		int y_0 = y+7;
		int y_1 = y+height-5;
		
		FontMetrics fm = g.getFontMetrics();
		
		//g.drawLine(x_0,y_0,x_1,y_0);
		g.drawLine(x_0,y_0,x_0+6,y_0);
		g.drawLine(x_0+(int)fm.getStringBounds(title,g).getWidth()+12,y_0,x_1,y_0);
		g.drawLine(x_1,y_0,x_1,y_1);
		g.drawLine(x_1,y_1,x_0,y_1);
		g.drawLine(x_0,y_1,x_0,y_0);
		
		g.drawString(title,x+15,y+11);
	}
}