package uk.ac.ed.inf.heatmap;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/* ------ Marks Achieved = 24/25 ------ */

/**
 * A class that generates a heat map (GeoJSON) to visualise the predictions made for the highest
 * sensor reading which will be seen in each area of the drone confinement area, partitioned
 * into a 10 x 10 grid. The rectangles are colour-coded based on value.
 * input: text file containing 10 lines, each of 10 comma-separated integers.
 */
public class App 
{
	// Dimensions of the grid, hard-coded as they are fixed requirements
	public final static int ROWS = 10;
	public final static int COLUMNS = 10;
	
	// Read a text file and create the grid full of predictions
	// If a prediction value is missing or not an integer, an error message is displayed
	public static int[][] readFile(String filename) throws Exception {
		var file = new File(filename);
		var scanner = new Scanner(file);
		var grid = new int[ROWS][COLUMNS];
		
		while (scanner.hasNextLine()) {
			for (int i = 0; i < grid.length; i++) {
				// Remove any whitespace, so the integers are all in the form int,int,int ...
				String rowContents = scanner.nextLine().replaceAll("\\s+","");
				
				var row = rowContents.split(",");
				for (int j = 0; j < grid.length; j++) {
					try {
						grid[i][j] = Integer.parseInt(row[j]);
					} catch (NumberFormatException | ArrayIndexOutOfBoundsException e){
						scanner.close();
						throw new Exception("The input file must not contain missing"
								+ " prediction values or non-integer values.");
					}
				}
			}
  	    }
		scanner.close();
		return grid;
	}
	
	public static ArrayList<Feature> createRectangles(int[][] grid, Point topLeftConfinement, 
    			Point bottomRightConfinement) throws Exception {
		var features = new ArrayList<Feature>();
		
		// Calculate the dimensions of each rectangle/polygon inside the confinement area
		final double RECTANGLE_WIDTH = (bottomRightConfinement.longitude() 
				- topLeftConfinement.longitude()) / COLUMNS;
		final double RECTANGLE_HEIGHT = (topLeftConfinement.latitude() 
				- bottomRightConfinement.latitude()) / ROWS;
    	
		// Starting from top left corner of confinement, calculate each rectangle's four points.
		// Then create a Feature to represent this rectangle
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid.length; j++) {
				Point topLeft = Point.fromLngLat(
						topLeftConfinement.longitude() + (j * RECTANGLE_WIDTH), 
						topLeftConfinement.latitude() - (i * RECTANGLE_HEIGHT));
				Point topRight = Point.fromLngLat(topLeft.longitude() + RECTANGLE_WIDTH, 
						topLeft.latitude());
				Point bottomLeft = Point.fromLngLat(topLeft.longitude(), 
						topLeft.latitude() - RECTANGLE_HEIGHT);
				Point bottomRight = Point.fromLngLat(topLeft.longitude() + RECTANGLE_WIDTH, 
						topLeft.latitude() - RECTANGLE_HEIGHT);
				List<Point> points= Arrays.asList(topRight, topLeft, bottomLeft, bottomRight);
				var polygonPoints = new ArrayList<List<Point>>();
				polygonPoints.add(points);
				// Create a polygon representing the rectangle
				Polygon rectangle = Polygon.fromLngLats(polygonPoints);
				// Represent as Geometry
				Geometry geometryRectangle = (Geometry) rectangle;
				// Represent as Feature
				Feature featureRectangle = Feature.fromGeometry(geometryRectangle);
				// Get the maximum sensor reading value for the current rectangle
				int value = grid[i][j];
				setProperties(featureRectangle, value);
				features.add(featureRectangle);	
			}
		}
		return features;
	}
	
	// Set the properties: fill-opacity, fill, RGB-string, of a feature based on integer value
	public static void setProperties(Feature featureRectangle, int value) throws Exception {
		featureRectangle.addNumberProperty("fill-opacity", 0.75);
		var rgbValue = getRGBString(value);
		featureRectangle.addStringProperty("rgb-string", rgbValue);
		featureRectangle.addStringProperty("fill", rgbValue);
	}
	
	// Returns corresponding RGB value for the colour mapping based on sensor reading value
	public static String getRGBString(int value) throws Exception {
		if (0 <= value && value < 32) {
			return "#00ff00";
		} else if (32 <= value && value < 64) {
			return "#40ff00";
		} else if (64 <= value && value < 96) {
			return "#80ff00";
		} else if (96 <= value && value < 128) {
			return "#c0ff00";
		} else if (128 <= value && value < 160) {
			return "#ffc000";
		} else if (160 <= value && value < 192) {
			return "#ff8000";
		} else if (192 <= value && value < 224) {
			return "#ff4000";
		} else if (224 <= value && value < 256) {
			return "#ff0000";
		} else {
			// This is the case for if the prediction value is not in range
			throw new Exception("The prediction value must be between 0 and 255 (inclusive)");
		}
	}
	
	// Write the resulting JSON to a text file
	public static void writeFile(String filename, String json) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		try {
			writer.write(json);
		} catch (IOException e) {
			e.printStackTrace();
		}
		writer.close();
	}
	
	public static void main( String[] args ) throws Exception {
		// Read the contents of the file into a 2D grid
		String filename = args[0];
		int[][] grid = readFile(filename);
		
		// Create the top left (Forrest Hill) point of the confinement zone (the starting point)
		// and the bottom right (Buccleuch St Bus Stop) point (to give the full grid dimensions)
		Point topLeftConfinement = Point.fromLngLat(-3.192473, 55.946233);
		Point bottomRightConfinement = Point.fromLngLat(-3.184319, 55.942617);
		
		// Create the Features of each of the rectangles in the 10 x 10
		ArrayList<Feature> features = createRectangles(grid, topLeftConfinement, bottomRightConfinement);
		FeatureCollection allRectangles = FeatureCollection.fromFeatures(features);
		
		// Write the geoJSON file
		writeFile("heatmap.geojson", allRectangles.toJson());
	}
}
