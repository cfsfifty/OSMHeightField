import java.text.ParseException;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * OSMContentHandler used to read OSM file and collect the points defined.
 *
 * @author  Christoph Fuenfzig
 * @version 1.0
 */
public class OSMContentHandler2 implements ContentHandler {

  private ArrayList<MapPoint>   p = new ArrayList<MapPoint>();
  private ArrayList<Polygon>    w = new ArrayList<Polygon>();
  private byte    cid = 0;
  private String  currentValue;
  private Polygon way;
  private boolean inWay;

  /** 
   * Get point list.
   *
   * @return point list collected in this pass.
   */
  public ArrayList<MapPoint> getPointList () {
      return this.p;
  }

  /** */
  public ArrayList<Polygon> filterLandusePolygons () {
		ArrayList<Polygon> landusePolygons = new ArrayList<Polygon>();
		
		for (Polygon polygon : getWayList()) {
			if (polygon.getLanduse() != null && polygon.getLanduse().equals("construction")) {
				polygon.getPointCoord(getPointList());
				landusePolygons.add(polygon);
			}
		}
		
		return landusePolygons;
  }
	
  /** */
  public ArrayList<Polygon> filterBuildingPolygons () {
		ArrayList<Polygon> buildingPolygons = new ArrayList<Polygon>();
		
		for (Polygon polygon : getWayList()) {
			if(polygon.getName() != null && polygon.getName().equals("Gebaeude")) {
				polygon.getPointCoord(getPointList());
				buildingPolygons.add(polygon);
			}
		}
		
		return buildingPolygons;
  }
  /** 
   * Get way list.
   *
   * @return way list collected in this pass.
   */
  public ArrayList<Polygon> getWayList () {
      for (Polygon polygon : this.w) {
         polygon.getPointCoord(this.getPointList());
      }
      return this.w;
  }

  /** */
  public void characters(char[] ch, int start, int length)
      throws SAXException {
      currentValue = new String(ch, start, length);
  }

  /** 
   * Method called for start-tag.
   *
   */
  public void startElement(String uri, String localName, String qName, Attributes atts) 
      throws SAXException {
      {
	if (localName.equals("node")) {
	    // create new node
	    MapPoint wgs = new MapPoint();
	    
	    // fill in node attribute values
	    String attr = atts.getValue("id");
	    if (attr != null) 
	    wgs.setId (Integer.parseInt(attr));
	    attr = atts.getValue("lat");
	    if (attr != null) 
	    wgs.setLat(Double .parseDouble(attr));
	    attr = atts.getValue("lon");
	    if (attr != null) 
	    wgs.setLon(Double .parseDouble(attr));
	    int index = atts.getIndex("ele");
	    if (index >= 0) {
		// "ele" exists
		wgs.setEle(Double.parseDouble(atts.getValue(index)));
	    } else {
		wgs.setEle(MapPoint.invalidEle);
	    }
	    this.p.add(wgs);
	} 
	else if (localName.equals("way")) {
	    // create new node
	    way = new Polygon();
	    
	    // fill in way id
	    String attr = atts.getValue("id");
	    if    (attr != null) 
	    way.setId (Integer.parseInt(attr), cid++);
	    inWay = true;
	} 
	else if (localName.equals("nd") && inWay) {
	    String attr = atts.getValue("ref");
	    if    (attr != null) 
	    way.addPid(Integer.parseInt(attr));
	} 
	else if (localName.equals("tag") && inWay) {
	    String attr = atts.getValue("k");
	    if    (attr != null && attr.equals("name")) {
		attr = atts.getValue("v");
		way.setName(attr);
	    }
	    attr = atts.getValue("k");
		if    (attr != null && attr.equals("landuse")) {
		attr = atts.getValue("v");
		way.setLanduse(attr);
	    }
	} 

    } 
  }

  /** 
   * Method called for end-tag.
   *
   */
  public void endElement(String uri, String localName, String qName)
      throws SAXException {
	if (localName.equals("way")) {
	    inWay = false;

	    if (way.getPointId().get(way.getPointId().size()-1).intValue() 
		== way.getPointId().get(0).intValue()) {
		way.getPointId().remove(way.getPointId().size()-1);
	    }
	    this.w.add(way);
	    way = null;
	} 
  }

  public void endDocument() throws SAXException {}
  public void endPrefixMapping(String prefix) throws SAXException {}
  public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException {}
  public void processingInstruction(String target, String data)
      throws SAXException {}
  public void setDocumentLocator(Locator locator) {  }
  public void skippedEntity(String name) throws SAXException {}
  public void startDocument() throws SAXException {}
  public void startPrefixMapping(String prefix, String uri)
      throws SAXException {}
}