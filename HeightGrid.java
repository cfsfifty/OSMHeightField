import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import com.sun.media.imageio.plugins.tiff.*; 
import java.awt.*;
import java.awt.color.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import javax.swing.*; 
import java.net.*;
import java.io.*;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Program for reading OSM files and querying the point elevations from Google Elevation API.
 *
 * @author  Christoph Fuenfzig
 * @version 1.0
 */
public class HeightGrid {
    private static final double bEps = 1e-6;
    /** Generalized barycentric interpolation of Ele-data in poly into point. Any polygon point (px, py)
        is transformed acc to (px*sx+ox, py*sy+oy) into the grid coordinate system.
    */
    static public double genBarycentric (Polygon  poly,  
					 MapPoint point, double sx, double ox, double sy, double oy) {
	int                     n = poly.getLength();
	ArrayList<MapPoint> polyP = poly.getPointCoord();
	double[] dx = new double[n];
	double[] dy = new double[n];
	double[] r  = new double[n];
	double[] A  = new double[n];
	double[] D  = new double[n];	
	for (int i=0; i<n; ++i) {
	    // sx
	    dx[i] = (polyP.get(i).getLat()*sx+ox - point.getLat());
	    // sy
	    dy[i] = (polyP.get(i).getLon()*sy+oy - point.getLon());
	    // ri
	    r[i]  = Math.sqrt(dx[i]*dx[i] + dy[i]*dy[i]);
	}
	for (int i=0; i<n; ++i) {
	    int ii = (i+1) % n;
	    // Ai
	    A[i] = 0.5*(dx[i]*dy[ii] - dy[i]*dx[ii]);
	    // Di
	    D[i] =     (dx[i]*dx[ii] + dy[i]*dy[ii]);
	    //System.out.println(s.get(i)[0] + "/" + s.get(i)[1] + "/" + s.get(i)[2] + "/" + s.get(i)[3] + "/" + s.get(i)[4]);
	    if (r[i] < bEps) { // v = vi 
		return polyP.get(i).getEle();
	    } 
	    if ((-bEps < A[i] && A[i] < bEps) && D[i] < bEps) { // v on ei 
	        return (r[ii]*polyP.get(i).getEle() + r[i]*polyP.get(ii).getEle())/(r[i] + r[ii]); 
	    }
	}
	double ele = 0; 
	double W   = 0; 
	for (int i=0; i<n; ++i) {
	    int ip = (i+n-1) % n;
	    int ii = (i+1)   % n;
	    double w = 0; 
	    if (A[ip] != 0) {
		//if (A[ip] < -bEps || bEps < A[ip]) {
		w = w + (r[ip]-D[ip]/r[i])/A[ip];
	    } 
	    if (A[i] != 0) {
		//if (A[i]  < -bEps || bEps < A[i]) { 
		w = w + (r[ii]-D[i]/r[i])/A[i];
	    }
	    ele = ele + w*polyP.get(i).getEle(); 
	    W   = W   + w; 
	}
	//System.out.print(" " + point + "/" + f);
	return (ele/W);
  }

  public static double mpd = 35313.0;
  public static final double lwidth = 2.0;

  public static float[] heightOffset (int resX, int resY, double ppmX, double ppmY,
				   double minLat, double maxLat,
				   double minLon, double maxLon,
				   double offset, float[] array,
				   int id, ArrayList<Polygon> ways) {
      int      i = Polygon.getIndex(id);
      int      c = 0;
      for (int x=0; x<=resX; ++x) {
	  for (int y=0; y<=resY; ++y, ++c) {
	      MapPoint current = new MapPoint(resX-x, y);
	      // point in GRC84
	      if (ways.get(i).contains(current,
				       1.0/(ppmX*mpd), minLat, 
				       1.0/(ppmY*mpd), minLon)) {
		  array[c] += offset; 
	      }
	  }
	  //System.out.println("");
      }
      return array;
  }

  /** */
  public static float[] heightArray (int resX, int resY, double ppmX, double ppmY,
				      double minLat, double maxLat,
				      double minLon, double maxLon,
				      double minEle, double maxEle, double nn,
				      ArrayList<Polygon> ways) {
      float[] array = new float[(resX+1)*(resY+1)];

      int      c = 0;
      for (int x=0; x<=resX; ++x) {
	  for (int y=0; y<=resY; ++y, ++c) {
	      MapPoint current = new MapPoint(resX-x, y);
	      array[c] = (float)(nn-minEle);
	      // point in GRC84
	      int i;
	      for (i=0; i<ways.size(); ++i) {
		  // select first polygon the point falls into
		  if (ways.get(i).contains(current,
					   1.0/(ppmX*mpd), minLat, 
					   1.0/(ppmY*mpd), minLon)) {
		      // is current in poly i
		      array[c] = 
			  (float)(genBarycentric (ways.get(i), current,
						  (ppmX*mpd), -minLat*(ppmX*mpd), 
						  (ppmY*mpd), -minLon*(ppmY*mpd))
				  - minEle);
		      //System.out.print(" " + current);
		      break;
		  }
	      }
	  }
	  //System.out.println("");
      }
      return array;
  }

  /** */
  public static byte[] idArray (int resX, int resY, double ppmX, double ppmY,
				double minLat, double maxLat,
				double minLon, double maxLon,
				double minEle, double maxEle,
				ArrayList<Polygon> ways) {
      byte[] array = new byte[(resX+1)*(resY+1)];

      int c = 0;
      for (int x=0; x<=resX; ++x) {
	  for (int y=0; y<=resY; ++y, ++c) {
	      // point in GRC84
	      array[c] = 0;
	      MapPoint current = new MapPoint(resX-x, y);
	      for (int i=0; i<ways.size(); ++i) {
		  // select first polygon the point falls into
		  if (ways.get(i).contains(current,
					   1.0/(ppmX*mpd), minLat, 
					   1.0/(ppmY*mpd), minLon)) {
		      array[c] = (byte)ways.get(i).getLanduseId();
		      break;
		  }
	      }
	  }
      }
      return array;
  }

  /** */
  public static byte[] dArray (int resX, int resY, double ppm,
			       double minLat, double maxLat,
			       double minLon, double maxLon,
			       double minEle, double maxEle,
			       ArrayList<Polygon> ways) {
      byte[] array = new byte[(resX+1)*(resY+1)];

      int    c  = 0;
      double sx = (ppm*mpd);
      double ox = -minLat*(ppm*mpd);
      double sy = (ppm*mpd);
      double oy = -minLon*(ppm*mpd);
      for (int x=0; x<=resX; ++x) {
	  for (int y=0; y<=resY; ++y, ++c) {
	      // point in GRC84
	      array[c] = 0;
	      MapPoint current = new MapPoint(resX-x, y);
	      boolean  found   = false;
	      for (int i=0; !found && i<ways.size(); ++i) {
		  Polygon poly = ways.get(i);
		  int     n    = poly.getLength();
		  ArrayList<MapPoint> polyP = poly.getPointCoord();
		  // select first polygon the point falls into
		  for (int j=0; j<n; ++j) {
		      double dx  = (polyP.get(j).getLat()*sx+ox 
				    -(polyP.get((j+n-1)%n).getLat()*sx+ox));
		      double dy  = (polyP.get(j).getLon()*sy+oy 
				    -(polyP.get((j+n-1)%n).getLon()*sy+oy));
		      double len = Math.sqrt(dx*dx+dy*dy);
		      dx /= len; dy /= len;
		      if  (Math.abs(-dy*(x-(polyP.get((j+n-1)%n).getLat()*sx+ox)) 
				    +dx*(y-(polyP.get((j+n-1)%n).getLon()*sy+oy))) <= lwidth
			   && (dx*(x-(polyP.get((j+n-1)%n).getLat()*sx+ox)) 
			       +dy*(y-(polyP.get((j+n-1)%n).getLon()*sy+oy))) > 0.0
			   && (dx*(x-(polyP.get(j).getLat()*sx+ox)) 
			       +dy*(y-(polyP.get(j).getLon()*sy+oy))) < 0.0) {
			  array[c] = 127;
			  found    = true;
			  break;
		      }
		  }
	      }
	  }
      }
      return array;
  }

  /** Write byte array to PBM image with name given by filename. */
  public static void writePGM (int resX, int resY, byte[] image,
			       String filename) throws IOException {
      FileOutputStream fos = new FileOutputStream(filename);
      //PrintWriter      aos = new PrintWriter(fos);
      DataOutputStream dos = new DataOutputStream(fos);

      dos.writeBytes("P5\n");
      dos.writeBytes("" + resX + " " + resY + "\n");
      dos.writeBytes("255\n");
      int c = 0;
      for (int y=0; y<resY; ++y) {
	  for (int x=0; x<resX; ++x, ++c) {
	      dos.writeByte(image[c]);
	  }
	  // write a newline character here?
      }
  }
    
  /** Big-endian to little-endian conversion. Note, that Java uses big-endian byte order by default! */
  public static long bigToLittleEndian (long bigendian) {
      ByteBuffer buf = ByteBuffer.allocate(8);

      buf.order(ByteOrder.BIG_ENDIAN);
      buf.putLong(bigendian);
      buf.order(ByteOrder.LITTLE_ENDIAN);
      return buf.getLong(0);
  }
  /** Write heightfield array to binary file with name given by filename. */
  public static void writeRaw (int resX, int resY, float[] array,
			       String filename) throws IOException {
      FileOutputStream fos = new FileOutputStream(filename);
      DataOutputStream dos = new DataOutputStream(fos);
      for (int i=0; i<array.length; ++i) {
	  dos.writeLong(bigToLittleEndian(Double.doubleToRawLongBits(array[i])));
      }
  }

  /** Convert float-array to short-array. */
  public static short[] convertShort (float minValue, float maxValue, float[] array) {
     short[] result = new short[array.length];
     float   scale  = (Short.MAX_VALUE)/(maxValue-minValue);
     for (int i=0; i<array.length; ++i) {
	 result[i] = (short)(scale*(array[i]-minValue));
     }
     return result;
  }

  /** Output to a string. */
  public static String toString (int resX, int resY, short[] array) {
	//why to use a StringBuilder: http://kaioa.com/node/59
	StringBuilder result = new StringBuilder();
		
	for(int y=(resY-1); y>=0; y--) {
			for(int x=0; x<resX; x++) { 
				result.append(" " + array[x+y*resX]);
			}
			result.append("\n");
	}
		
	return result.toString();
  }

  /** Write heightfield array to 16bpp-TIF image with name given by filename. */    
  public static void writeTif (int resX, int resY, float minEle, float maxEle, float[] array,
			       String filename) throws IOException {
      // generate arrayShort (16 bpp) from array
      //short[]     arrayShort = convertShort((float)minEle, (float)maxEle, array);
      // wrap the data as a displayable image 
      SampleModel    sm = new ComponentSampleModel(DataBuffer.TYPE_FLOAT, resX, resY, 1, resX, new int[] {0});
      DataBuffer     db = new DataBufferFloat(array, array.length);
      WritableRaster wr = Raster.createWritableRaster(sm, db, null);
      ColorSpace     cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
      ColorModel     cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
      BufferedImage  bi = new BufferedImage(cm, wr, false, null);

      // write out the image using ImageIO Tools TIFF encoder
      ImageWriter    iw = (ImageWriter) ImageIO.getImageWritersByMIMEType("image/tiff").next();
      File     tiffFile = new File(filename);
      //System.out.println("writing to " + tiffFile.getAbsolutePath());
      ImageOutputStream ios = new MemoryCacheImageOutputStream(new FileOutputStream(tiffFile));
      iw.setOutput     (ios);
      TIFFImageWriteParam iwp = (TIFFImageWriteParam) iw.getDefaultWriteParam();
      // iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      // iwp.setCompressionType("Deflate");
      IIOMetadata         imd = iw.getDefaultImageMetadata(new ImageTypeSpecifier(bi), iwp);
      iw.write (null, new IIOImage(bi, null, imd), iwp);
      ios.flush();
      ios.close();
  }

  /** 
   * Query point elevations for all points in all. Points are grouped into 10 queries. 
   *
   */
  public static void queryElevations (ArrayList<MapPoint> all) {
      int i = 0;
      while (i%10 == 0 && i<all.size()) {
      // create query string
      // CF there is a length restriction by Google Elevation API not yet respected!
      // see https://developers.google.com/maps/documentation/elevation
      String queryString = "http://maps.googleapis.com/maps/api/elevation/xml?locations=";
      int j = i;
      queryString = queryString.concat(all.get(i).getLat()+","+all.get(i).getLon());
      for (++i; i%10 != 0 && i<all.size(); ++i) {
	  queryString = queryString.concat("|"+all.get(i).getLat()+","+all.get(i).getLon());
      }
      queryString = queryString.concat("&sensor=false");
      //System.out.println(queryString);

      try {
	  // create a URLConnection object for a URL
	  URL            url  = new URL(queryString);
	  URLConnection  conn = url.openConnection();
	  
	  BufferedReader in   = new BufferedReader(
		                new InputStreamReader(conn.getInputStream()));
	  String inputLine;
	  while ((inputLine = in.readLine()) != null) {
	      int si = inputLine.indexOf("elevation");
	      if (si >= 0
		  && j<all.size() && all.get(j).getEle() == MapPoint.invalidEle) {
		  si = inputLine.indexOf(">");
		  if (si >= 0) {
		      // get number substring from inputLine
		      inputLine = inputLine.substring(si+1); // start index included!
		      int se    = inputLine.indexOf("<");  // end index not included!
		      inputLine = inputLine.substring(0, se);
		      all.get(j).setEle(Double.parseDouble(inputLine));
		  }
		  j += 1;
	      }
	  }
	  in.close();
      } catch (IOException ex) {
	  System.out.println(ex);
      }
      }
  }

  /** 
   * Main.
   *
   * @param args command line parameters. 
            args[0] OSM filename.
            args[1] Points per Meter.
            args[2] NN height.
   */
  public static void main (String[] args) {
    try {
      // XMLReader
      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      
      // path for XML file
      if (args.length < 3) {
	 System.out.println("usage: pointsPerMeter file.OSM type");
	 return;
      } 

      String         filename = args[1];
      System.out.println(filename);
      FileReader       reader = new FileReader (filename);
      InputSource inputSource = new InputSource(reader);

      // DTD is optional
      // inputSource.setSystemId(args[1]);

      // OSMContentHandler input
      OSMContentHandler2 readHandler = new OSMContentHandler2();
      xmlReader.setContentHandler(readHandler);
      // start parsing ..
      xmlReader.parse(inputSource);

      // check if "ele" present
      int ri = (int)Math.random()*readHandler.getPointList().size();
      if (readHandler.getPointList().get(ri).getEle() == MapPoint.invalidEle) { // if no elevations in points
	  // then query them
	  queryElevations (readHandler.getPointList());
      }

      // output ways
      System.out.println("\nWays:");
      // calc point coordinates
      // comparison with student's code
      //ArrayList<Polygon> ways = readHandler.filterBuildingPolygons();
      ArrayList<Polygon> ways = readHandler.getWayList ();
      // ouput polygons aka ways
      for (int i=0; i<ways.size(); ++i) {
	 System.out.println(ways.get(i));
      }

      // calculate bounds
      System.out.println("\nPoints:");
      double minLat = Double.MAX_VALUE;
      double maxLat = Double.MIN_VALUE;
      double minLon = Double.MAX_VALUE;
      double maxLon = Double.MIN_VALUE;
      double minEle = Double.MAX_VALUE;
      double maxEle = Double.MIN_VALUE;
      for (int i=0; i<readHandler.getPointList().size(); ++i) {
	  System.out.println(readHandler.getPointList().get(i));
	  minLat = Math.min(minLat, readHandler.getPointList().get(i).getLat());
	  maxLat = Math.max(maxLat, readHandler.getPointList().get(i).getLat());
	  minLon = Math.min(minLon, readHandler.getPointList().get(i).getLon());
	  maxLon = Math.max(maxLon, readHandler.getPointList().get(i).getLon());
	  minEle = Math.min(minEle, readHandler.getPointList().get(i).getEle());
	  maxEle = Math.max(maxEle, readHandler.getPointList().get(i).getEle());
      }

      // normal zero height (in meters): minimum elevation
      double   nn    = minEle; 
      double   diffX = (maxLat - minLat)*mpd;
      double   diffY = (maxLon - minLon)*mpd;
      // number of points per meter
      double   ppmY  = Double.parseDouble(args[0]);
      double   ppmX  = ppmY*diffY/diffX; 
      int      resX  = (int)(diffX*ppmX);
	  //1397; 
      int      resY  = (int)(diffY*ppmY);
	  //2195;
      System.out.println("\n(" + minLat + "/" + maxLat + ")\n"
			 + "(" + minLon + "/" + maxLon + ")\n"
			 + "(" + minEle + "/" + maxEle + "/" + nn + ")\n"
			 + diffX + ", " + diffY + ", ppm=" + ppmX + "*" + ppmY + ", " + (resX+1) + "*" + (resY+1));
      System.out.println("(" + (maxLat-minLat)*mpd*ppmX + "/" + (maxLon-minLon)*mpd*ppmY + ")\n");
      String   basename = filename.substring(0, filename.indexOf("."));
      
      if (args[2].equals("0")) {
	  // array created in heightArray
	  float[] array = heightArray(resX, resY, ppmX, ppmY, 
				      minLat, maxLat, minLon, maxLon, minEle, maxEle, nn,
				      ways);
	  // this is optional to offset polygon named "-168"
	  //heightOffset(resX, resY, ppmX, ppmY, minLat, maxLat, minLon, maxLon, -0.1, array, -168, ways);
	  writeTif(resX+1, resY+1, (float)minEle, (float)maxEle, array, basename+".tif"); 
      } else {
	  // array created in idArray
	  byte[] array = idArray(resX, resY, ppmX, ppmY, 
				 minLat, maxLat, minLon, maxLon, minEle, maxEle, 
				 ways);
	  // write id array to file basename".pgm"
	  writePGM(resX+1, resY+1, array, basename + ".pgm");
      }
    
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }
  }
}