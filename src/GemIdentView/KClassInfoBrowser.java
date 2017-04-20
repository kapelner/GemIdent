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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

/**
 * Controls the panel that displays all the 
 * {@link KClassInfo class info displays}.
 * If there are more displays than height in the
 * browser, a vertical scroll bar will appear (this
 * functionality is buggy and varies from
 * platform to platform).
 * 
 * @author Adam Kapelner and Kyle Woodward
 */
@SuppressWarnings("serial")
public class KClassInfoBrowser extends JPanel{

	/** add extra space to the bottom of the panel -- buggy */
	private static final int ExtraSpace = 1050;
	/** the preferred width of the browser */
	public static final int prefWidth = 200;
	/** the preferred height of the browser */
	public static final int minHeight = 500;
	
	/** the set of the {@link KClassInfo class info displays}. Linked to preserve order during iterations */
	private LinkedHashSet<KClassInfo> infos;
	/** the box that stores all the {@link KClassInfo class info displays} */
	private Box theBox;
	/** the scrollpane that stores the browser */
	private JScrollPane scrollPane;
	/** the vertical scroll bar - a component of the scrollpane */
	private JScrollBar vbar;
	
	/** create the browser, sets the size / layout / scrollbar policies and customizations, and adds the vertical box holder */
	public KClassInfoBrowser(){
		super();
		scrollPane=new JScrollPane(this,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		setPreferredSize(new Dimension(prefWidth,minHeight));
		setLayout(new BorderLayout());
		infos=new LinkedHashSet<KClassInfo>();
		theBox=Box.createVerticalBox();
		add(theBox,BorderLayout.NORTH);
		vbar = scrollPane.getVerticalScrollBar(); //set up vbar
		vbar.setUnitIncrement(75);
		vbar.setPreferredSize(new Dimension(7,vbar.getHeight()));
	}	
	/** adds an info to the browser */
	public void addInfoClass(KClassInfo info){
		infos.add(info);
		theBox.add(info);
		UpdateScrollBar();
	}
	/** deletes an info by its name */
	public void deleteInfoClass(String key){		
		for (KClassInfo info:infos){
			if (info.getTrainName().equals(key)){
				infos.remove(info);
				theBox.remove(info);
				break;
			}
		}
		UpdateScrollBar();
	}
	/** after an addition or deletion, updates the scrollpane. 
	 * Calling "scrollPane.revalidate();" should be enough but 
	 * this implementation is buggy so hacks are needed 
	 */
	public void UpdateScrollBar(){		
//		int H=GetBrowserHeight();
//		scrollPane.revalidate();
//		if (H > infos.size()*10){
//			if (infos.size() > 4){
				setPreferredSize(new Dimension(prefWidth,GetBrowserHeight()+ExtraSpace));
				scrollPane.revalidate();
//			}
//			else {
//				scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
//				setPreferredSize(new Dimension(prefWidth,minHeight+ExtraSpace));
//				scrollPane.revalidate();
//			}
//		}
//		else {
//			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
//			setPreferredSize(new Dimension(prefWidth,minHeight+1000));
//			scrollPane.revalidate();
//		}
	}
	/** get the browser's height by adding the height of its class infos */
	private int GetBrowserHeight() {
		int H=0;
		for (KClassInfo info:infos)
			H+=info.getHeight()+10;
		return H;
	}
	/** gets the number of class infos in this browser */
	public int getNumInfos(){
		return infos.size();
	}
	/** gets the names of the class infos in this browser */
	public ArrayList<String> getClassInfoNames(){
		ArrayList<String> list=new ArrayList<String>(infos.size());
		for (KClassInfo info:infos)
			list.add(info.getTrainName());
		Collections.sort(list);
		return list;		
	}
	/** checks to see if a certain class info is in this browser */
	public boolean isInBrowser(String key){
		return getClassInfoNames().contains(key);
	}
	/** repaints the browser by calling repaint on each info */
	public void repaint(){
		super.repaint();
		if (theBox == null) return;
		for (Component c:theBox.getComponents())
			c.repaint();			
	}	
	/** gets the class infos in this browser in order from top to bottom */
	public ArrayList<KClassInfo> getClassInfosInOrder() {
		ArrayList<KClassInfo> list=new ArrayList<KClassInfo>();
		for (Component c:theBox.getComponents())
			list.add((KClassInfo)c);
		return list;
	}
	/** gets the first class info. In the phenotype train panel, this is the NON phenotype */
	public KClassInfo getFirstClassInfo() {
		return getClassInfosInOrder().get(0);
	}
	/** builds the identifier images for each class info in the browser */
	public void buildExampleImages() {
		for ( KClassInfo info:infos )
			info.buildExampleImage();
	}
	public JScrollPane getScrollPane(){
		return scrollPane;
	}
	public Collection<KClassInfo> getClassInfos() {
		return infos;
	}
}