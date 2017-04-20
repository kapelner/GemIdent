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

import java.awt.*;
import javax.swing.*;

/**
 * Frames components together and displays a frame title. 
 * Not documented.
 * 
 * @author Kyle Woodward
 *
 */
@SuppressWarnings("serial")
public class GUIFrame extends JPanel {

	private class PointRect {
		public int x1;
		public int x2;
		public int y1;
		public int y2;
		
		public PointRect( int x1, int y1, int x2, int y2 ) {
			this.x1 = x1;
			this.x2 = x2;
			this.y1 = y1;
			this.y2 = y2;
		}
	}
	private String frame_title;
	
	public GUIFrame( String s ) {
		frame_title = s;
		
		this.setLayout(new BorderLayout());
		this.add(Box.createHorizontalStrut(10),BorderLayout.WEST);
		this.add(Box.createHorizontalStrut(10),BorderLayout.EAST);
		this.add(Box.createVerticalStrut(15),BorderLayout.NORTH);
		this.add(Box.createVerticalStrut(13),BorderLayout.SOUTH);
	}
	
	public GUIFrame() {
		new GUIFrame("");
	}
	
	public Component add( Component c ) {
		this.add(c,BorderLayout.CENTER);
		return c;
	}
	
	public void paintComponent( Graphics g ) {
		super.paintComponent(g);
		
		Rectangle r = this.getBounds();
		PointRect s = new PointRect(xLeft(r),yTop(r),xRight(r),yBottom(r));
		FontMetrics fm = g.getFontMetrics();
		
		int string_length = (int)fm.getStringBounds(frame_title,g).getWidth();
		
		g.setColor(Color.BLACK);
		g.drawLine(s.x2,s.y1,s.x2,s.y2);
		g.drawLine(s.x1,s.y2,s.x2,s.y2);
		g.drawLine(s.x1,s.y1,s.x1,s.y2);
		g.drawLine(s.x1,s.y1,11,8);
		g.drawLine(string_length+17,s.y1,s.x2,s.y1);
		g.drawString(frame_title,15,11);
	}
	
	public void setTitle( String title ) {
		this.frame_title = "Page inspector - "+title;
	}
	
	private int xLeft( Rectangle r ) {
		return 5;
	}
	
	private int xRight( Rectangle r ) {
		return 5+r.width-10;
	}
	
	private int yBottom( Rectangle r ) {
		return r.height-7;
	}
	
	private int yTop( Rectangle r ) {
		return 8;
	}
}