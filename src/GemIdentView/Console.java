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
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import GemIdentAnalysisConsole.ConsoleParser;

/**
 * The object that controls the display of the 
 * data analysis page - the console, the image
 * indicators on the left, and the command textbox
 * 
 * @author Adam Kapelner
 */
public class Console {

	/** the height of the command textbox */
	private static final int TextHeight=25;
	/** the message that displays when the analysis page is first opened */
	private static final String WELCOME_MESSAGE="Welcome to GemIdent Analysis\n" +
			"To begin, first construct the global images by typing \"build\" at the prompt\n" +
			"For a list of available functions, type \"help\"\n\n";
	
	/** the color, text size, and font information for text that is entered by the user */
	public static SimpleAttributeSet entered;
	/** the color, text size, and font information for text in help messages */
	public static SimpleAttributeSet help;
	/** the color, text size, and font information for the text of error messages */
	public static SimpleAttributeSet error;
	/** the color, text size, and font information for the text of indicative messages*/
	public static SimpleAttributeSet neutral;
	/** the color, text size, and font information for the text of resulting messages of successfully executed functions */
	public static SimpleAttributeSet output;
	/** the color, text size, and font information for text of time elapsed messages */
	public static SimpleAttributeSet time;
	/** the color, text size, and font information for text of warning messages */
	public static SimpleAttributeSet warning;
	/** sets up the color, text size, and font information for all the attribute types */
	static {
		entered=new SimpleAttributeSet();
//		StyleConstants.setBold(entered,true);
		StyleConstants.setForeground(entered,Color.BLACK);
		StyleConstants.setFontSize(entered,17);
		StyleConstants.setFontFamily(entered,"Courier");
		error=new SimpleAttributeSet();
		StyleConstants.setForeground(error,Color.RED);
		StyleConstants.setFontSize(error,13);
		StyleConstants.setFontFamily(error,"Courier");
		warning=new SimpleAttributeSet();
		StyleConstants.setForeground(warning,new Color(192,98,0));
		StyleConstants.setFontSize(warning,13);
		StyleConstants.setFontFamily(warning,"Courier");
		neutral=new SimpleAttributeSet();
		StyleConstants.setForeground(neutral,new Color(0,0,192));
		StyleConstants.setFontSize(neutral,14);
		StyleConstants.setFontFamily(neutral,"Courier");
		output=new SimpleAttributeSet();
		StyleConstants.setForeground(output,new Color(0,144,0));
		StyleConstants.setFontSize(output,14);
		StyleConstants.setFontFamily(output,"Courier");
		time=new SimpleAttributeSet();
		StyleConstants.setForeground(time,Color.GRAY);
		StyleConstants.setFontSize(time,12);
		StyleConstants.setFontFamily(time,"Courier");
		help=new SimpleAttributeSet();
		StyleConstants.setForeground(help,Color.DARK_GRAY);
		StyleConstants.setFontSize(help,13);
		StyleConstants.setFontFamily(help,"Courier");
	}
	
	/** the console display area */
	private JTextPane area;
	/** the command textbox */
	private JTextField enter;
	/** the Container that contains the scrollboxed display area and its command textbox */
	private Container container;
	/** the list of commands entered by the user */
	private LinkedList<String> commands;
	/** a order-preserving mapping from user command to its result */
	private LinkedHashMap<String,Object> calls;
	/** the preserved place in the list of {@link #commands commands} when the user is using the up/down keys to access previous commands */
	private int place;
	
	/** initializes the console by initializing all 
	 * components, setting up the listener 
	 * for accessing previous commands using up/down, 
	 * setting up the listener for allowing the user to 
	 * type commands into the command textbox while
	 * focused on the console display area, then organizes
	 * components onto the display page
	 */
	public Console(final ConsoleParser parser){

		area=new JTextPane();
		area.setEditable(false);
		append_to_area(WELCOME_MESSAGE,null,help);
		parser.addConsole(this);

		
		commands=new LinkedList<String>();
		calls=new LinkedHashMap<String,Object>();
		place=-1;
		
		enter=new JTextField();
		enter.setPreferredSize(new Dimension(KFrame.frameSize.width,TextHeight));
		enter.addKeyListener(
			new KeyListener(){
				public void keyPressed(KeyEvent e){}
				public void keyReleased(KeyEvent e){
					if (e.getKeyCode() == KeyEvent.VK_ENTER){
//						temp=null; //rest
						place=-1;
						final String command=enter.getText();
						if (!command.equals("")){
							commands.addFirst(command); //place this entered command in list
							append_to_area("\n",null,time); //just put some space
							append_to_area(command+"\n",null,entered);
					        enter.setText(""); //make it blank
					        new Thread(){ //dont' hog swing
					        	public void run(){
					        		parser.Parse(command.trim()); //parse it
					        	}
					        }.start();		
						}
					}	
					else if (e.getKeyCode() == KeyEvent.VK_UP){
//						if (temp == null) //save
//							temp=enter.getText();						
						if (place < commands.size()-1)
							place++;
						if (commands.size() > 0)
							enter.setText(commands.get(place));
					}
					else if (e.getKeyCode() == KeyEvent.VK_DOWN){
						if (place > 0){
							place--;
							enter.setText(commands.get(place));	
						}
						else if (place == 0){
							place=-1;
							enter.setText("");
						}
//						else if (temp != null)
//							enter.setText(temp);
						else
							enter.setText("");
					}
				}
				public void keyTyped(KeyEvent e){}
			}
		);		
		area.addKeyListener(
			new KeyListener(){
				public void keyPressed(KeyEvent e){
					if (!e.isControlDown()){
//						if (!(e.getKeyCode() == 'C' && (e.getModifiers() & InputEvent.CTRL_MASK) != 0)) {				      
							char key=e.getKeyChar();
							enter.setText(enter.getText()+key);
							enter.requestFocus();
//						}
					}
				}
				public void keyReleased(KeyEvent arg0){}
				public void keyTyped(KeyEvent arg0){}				
			}
		);
		
		container=new Container();
		
		container.setLayout(new BorderLayout());
        JScrollPane scrollPane=new JScrollPane(area,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(KFrame.frameSize.width,KFrame.frameSize.height*4/5));
        //Add Components to this panel.
//        GridBagConstraints c = new GridBagConstraints();
//        c.gridwidth = GridBagConstraints.REMAINDER;
//
//
//
//        c.fill = GridBagConstraints.BOTH;
//        c.weightx = 1.0;
//        c.weighty = 1.0;
        container.add(scrollPane,BorderLayout.NORTH);
//        c.fill = GridBagConstraints.HORIZONTAL;
        container.add(enter,BorderLayout.SOUTH);		
	}
	public Container getInnards() {
		return container;
	}
	/** when the user clicks on an icon of the stored image, it adds that text to the command textbox */
	public void addImageNameToEnter(String name){
		enter.setText(enter.getText()+name);
	}
	/**
	 * Adds the result of a call to the console area
	 * 
	 * @param message	the result of a call
	 * @param call		the user's call to be saved in the list of calls
	 * @param attr		the attribute to format the text
	 */
	public void append_to_area(String message,String call,SimpleAttributeSet attr){
		Document doc = area.getDocument();
        try {
			doc.insertString(doc.getLength(),message,attr);
		} catch (BadLocationException e){
			e.printStackTrace();
		}
		area.setCaretPosition(doc.getLength());			
		if (attr == output) //save calls . . .
			calls.put(call,message);
	}
	/**
	 * Adds the result of a call to the console area when that call produces
	 * image output
	 * 
	 * @param call		the call to be saved	
	 * @param image		the image to be displayed to the screen
	 */
	public void appendImage(String call,BufferedImage image){
		area.insertIcon(new ImageIcon(image));
		SwingUtilities.updateComponentTreeUI(area);
		append_to_area("\n",null,time); //just put some space
		calls.put(call,image);
	}
	public LinkedHashMap<String,Object> getCalls(){
		return calls;
	}
	/** focues on the command textbox and displays the cursor */
	public void FocusEnter(){
		enter.requestFocus();
	}
}