import java.net.*;
import java.io.*;
import java.lang.*;

/**
 * Point with id/lat/lon/ele attributes. MapPoint.invalidEle standsfor undefined elevation.
 *
 * @author  Christoph Fuenfzig
 * @version 1.0
 */
public class MapPoint {

  private int    id;
  private double lat;
  private double lon;
  private double ele;
  static public double invalidEle = -1.79769313486231E+308;

  public MapPoint () {
  }
  public MapPoint (double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
  }

  public void setId(int id) {
    this.id = id;
  }
  public void setLat (double lat) {
    this.lat = lat;
  }
  public void setLon (double lon) {
    this.lon = lon;
  }
  public void setEle (double ele) {
    this.ele = ele;
  }

  public int getId() {
    return this.id;
  }
  public double getLat () {
    return this.lat;
  }
  public double getLon () {
    return this.lon;
  }
  public double getEle () {
    return this.ele;
  }

  @Override
  public String toString() {
    return "[[" + this.id + "] ["+ this.lat + ", " + this.lon + ", " + this.ele + " ]]";
  }
}