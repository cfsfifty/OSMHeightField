import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collection;

/**
 * Polygon with point ids.
 *
 * @author  Christoph Fuenfzig
 * @version 1.0
 */
public class Polygon {

  private ArrayList<Integer>    pid = new ArrayList<Integer>();
  private ArrayList<MapPoint>   pc  = new ArrayList<MapPoint>() ; 
  private String name;
  private String landuse;
  private int    id;
  private static TreeMap<Integer,Byte> mid = new TreeMap<Integer,Byte>();

  public Polygon () {
  }

  public void setId (int id, byte cid) {
      this.mid.put(new Integer(id), new Byte(cid));
      this.id = id;
  }
  public int  getId () {
      return this.id;
  }
  public static int  getIndex (int id) {
      return (int)mid.get(id);
  }

  /** */
  public boolean contains (MapPoint p, 
			   double sx, double ox,
			   double sy, double oy) {
      boolean inside = false;
      int     j = pc.size()-1; 
      for (int i=0; i<pc.size(); j=i, i++) { 
	  if (((pc.get(i).getLon()>(p.getLon()*sy+oy)) != (pc.get(j).getLon()>(p.getLon()*sy+oy)))
	      && (p.getLat()*sx+ox < pc.get(i).getLat()
		  +(p.getLon()*sy+oy-pc.get(i).getLon())/(pc.get(j).getLon()-pc.get(i).getLon())*(pc.get(j).getLat()-pc.get(i).getLat()))) {
	      // (px, py)+t*(1, 0)=p intersects edge iff. py = yi+t*(yj-yi) gives t=(py-yi)/(yj-yi) >=0, 
	      // ex = xi+t*(xj-xi) 
	      inside = !inside; 
	  }
      }
      return inside;
  }

  static public MapPoint findPoint (int id, ArrayList<MapPoint> p) {
      for (int i=0; i<p.size(); ++i) {
	  if (p.get(i).getId() == id) {
	      return p.get(i);
	  }
      }
      return null;
  }
  public ArrayList<Integer> getPointId    () {
      return this.pid;
  }
  public ArrayList<MapPoint> getPointCoord  () {
      return this.pc;
  }
  public ArrayList<MapPoint> getPointCoord (ArrayList<MapPoint> p) {
      this.pc.clear();
      for (int i=0; i<pid.size(); ++i) {
	  this.pc.add(findPoint(pid.get(i), p));
      }
      return this.pc;
  }

  public int  getLength () {
      return this.pid.size();
  }
  public void addPid  (int value) {
      this.pid.add(value);
  }
  public void setPid (int i, int value) {
      this.pid.set(i, value);
  }
  public int  getPid (int i) {
      return this.pid.get(i);
  }
  public MapPoint getPc (int i) {
      return this.pc.get(i);
  }

  public void   setName (String value) {
      this.name = value;
  }
  public String getName () {
      return this.name;
  }
  /** Set landuse string. */
  public void   setLanduse (String value) {
      this.landuse = value;
  }
  /** Get landuse string. */
  public String getLanduse () {
      return this.landuse;
  }
  /** Generate a landuse id from cid. */
  public byte getLanduseId () {
      Byte id = this.mid.get(getId());
      if  (id != null) {
	  return (byte)(10*(int)this.getIndex(getId()));
      }
      // not found
      return (byte)255;
  }

  /** Debug output. */
  @Override
  public String toString() {
      String rep = this.name + "/" + this.id + "/" + this.getIndex(getId()) + " (";
      if (pc.size() > 0) {
	  rep = rep + pc.get(0);
	  for (int i=1; i<pid.size(); ++i) {
	      rep = rep + "," + pc.get(i);
	  }
	  rep = rep + ")";
      } else {
	  rep = rep + pid.get(0);
	  for (int i=1; i<pid.size(); ++i) {
	      rep = rep + "," + pid.get(i);
	  }
	  rep = rep + ")";
      }
      return rep;
  }
}