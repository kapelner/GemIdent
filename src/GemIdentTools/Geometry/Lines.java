package GemIdentTools.Geometry;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This is kept around basically as a 
 * static final class which is referenced
 * when "masks" for lines are needed for attribute
 * generation. A "mask" is the locus of 
 * points that represent a line
 * (represented as Lists of Point objects).
 * The center of the line is always coordinate (0,0)
 * <p>
 * For instance, one of the "twoLine"'s would be:
 * <ul>
 * <li>		     0     0     0     0     1
 * <li>		     0     0     0     1     0
 * <li>	 	     0     0     1     0     0
 * <li>		     0     1     0     0     0
 * <li>		     1     0     0     0     0
 * </ul>
 *  where the ones represent points in the
 *  mask with the half-length of two pixels.
 *  </p>
 *  <p>
 *  The lines are generated using a naive custom algorithm
 *  </p>
 *  
 *  @see <a href="http://www.gemident.com/publication.html">the 2007 IEEE paper</a>
 * 
 * @author Adam Kapelner
 */
public class Lines {
	
	/** initially the number of rings to autogenerate */
	public static final int INIT_MAX_LINE_HALF_WIDTH = 25;
	public static final double THETA_INCREMENT = 1 / (double)INIT_MAX_LINE_HALF_WIDTH;
	
	/** the mapping from the half width of the line to a set of 
	 * lines which have this half width spanning all angles. 
	 * A line is a set of {@link java.awt.Point Point} objects. We use
	 * sets because we'll have lots of duplications, and we want it to be handled
	 * nicely */
	private static HashMap<Integer, ArrayList<ArrayList<Point>>> allLinesSet;
	
	/** To construct the static object, call Build */
	static {
		Build();
//		Diagnostics();
	}
	
	//purely for testing
	public static void main(String[] args){
		Build();
		Diagnostics();
	}
	
	/** Build the masks for all half-lengths up to maximum */
	public static void Build(){
		//build a temporary hashmap to hashsets for convenience (duplication is handled for us nicely)
		HashMap<Integer, HashSet<HashSet<Point>>> allLinesHashes = new HashMap<Integer, HashSet<HashSet<Point>>>();
		//keep this around to ensure no duplicates
		HashSet<HashSet<Point>> all_previous = new HashSet<HashSet<Point>>();
		
		for (int line_half_width = 1; line_half_width <= INIT_MAX_LINE_HALF_WIDTH; line_half_width++){
			//generate a set of sets for this half width
			HashSet<HashSet<Point>> lines_for_half_width_i = new HashSet<HashSet<Point>>();
			
			//we're going to iterate over every possible angle
			for (double theta = 0; theta <= Math.PI; theta += THETA_INCREMENT){
				HashSet<Point> line = new HashSet<Point>();		
				//we need to construct all pieces of the line
				for (int hl = -line_half_width; hl <= line_half_width; hl++){
					int x = (int)Math.round(hl * Math.cos(theta));
					int y = (int)Math.round(hl * Math.sin(theta));
					line.add(new Point(x, y));
				}
				//now we just can add it as long as it wasn't here before
				if (!all_previous.contains(line)){
					all_previous.add(line);
					lines_for_half_width_i.add(line);					
				}
			}
			
			//finally add this set of sets to the master array
			allLinesHashes.put(line_half_width, lines_for_half_width_i);
		}
		//now convert these hashes into arrays for quicker iteration later
		convertHashSetToArray(allLinesHashes);
	}
	
	private static void convertHashSetToArray(HashMap<Integer, HashSet<HashSet<Point>>> allLinesHashes) {
		allLinesSet = new HashMap<Integer, ArrayList<ArrayList<Point>>>();
		
		for (int hl : allLinesHashes.keySet()){
			HashSet<HashSet<Point>> lines_set = allLinesHashes.get(hl);
			ArrayList<ArrayList<Point>> lines_arr = new ArrayList<ArrayList<Point>>(lines_set.size());
			for (HashSet<Point> line_set : lines_set){
				ArrayList<Point> line_arr = new ArrayList<Point>(line_set.size());
				for (Point t : line_set){
					line_arr.add(t);
				}
				lines_arr.add(line_arr);
			}
			allLinesSet.put(hl, lines_arr);
		}
	}

	/**
	 * Return a ring of radius r. If the ring is not yet generated, 
	 * it will autogenerate, cache it, and return it
	 * 
	 * @param r		radius of the ring desired
	 * @return		the ring
	 */
	public static ArrayList<ArrayList<Point>> getLines(int hl){
		return allLinesSet.get(hl);
	}	
	
	//debugging only
	public static void Diagnostics(){
		for (int i = 1; i <= INIT_MAX_LINE_HALF_WIDTH; i++){
			ArrayList<ArrayList<Point>> lines_for_half_width = allLinesSet.get(i);
			if (lines_for_half_width != null){
				System.out.println("half width: " + i + " number of lines: " + lines_for_half_width.size());
//				for (ArrayList<Point> line : lines_for_half_width){
//					System.out.print("  line: ");
//					for (Point p : line){
//						System.out.print("(" + p.x + ", " + p.y + ") ");
//					}
//					System.out.print("\n");
//				}
			}
		}
	}
}
