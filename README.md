OSMHeightField 
developed in 2013

Reading OSM polygons (closed ways) and 
interpolating "el"-values and "id"-values over the polygon area.


Examples:

Height interpolation
java -cp .;jai_imageio-1.1.jar HeightGrid data/KL_Nord3.osm 0

Id interpolation
java -cp .;jai_imageio-1.1.jar HeightGrid 10.0 data/KL_Nord3.osm 1