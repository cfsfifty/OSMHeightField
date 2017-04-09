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
public class OSMContentHandler implements ContentHandler {

  private ArrayList<MapPoint> all = new ArrayList<MapPoint>();
  private String     currentValue;

  /** 
   * Get point list.
   *
   * @return point list collected in this pass.
   */
  public ArrayList<MapPoint> getPointList () {
      return this.all;
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
	    all.add(wgs);
	    System.out.println(wgs);
	} 
    } 
  }

  /** 
   * Method called for end-tag.
   *
   */
  public void endElement(String uri, String localName, String qName)
      throws SAXException {

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