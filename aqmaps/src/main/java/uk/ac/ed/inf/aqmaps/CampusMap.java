package uk.ac.ed.inf.aqmaps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

public class CampusMap {
    
    private String day;
    private String month;
    private String year;
    public List<Sensor> sensors = new ArrayList<Sensor>();
    public List<Feature> noFlyZones = new ArrayList<Feature>();
    public static double[][] distanceMatrix = new double [34][34];
    
    public CampusMap(String day, String month, String year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }
    
    /*
     * Get the list of all sensors to be visited on the given date (from input) 
     */
    public void getSensorListFromServer(int portNumber) throws IOException, InterruptedException {
        System.out.println("Getting sensor list from server");
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://localhost:%d/maps/%s/%s/%s/air-quality-data.json", 
                        portNumber,this.year,this.month,this.day)))
                .build();
        var response = client.send(request, BodyHandlers.ofString());
        var listType = new TypeToken<ArrayList<Sensor>>(){}.getType();
        List<Sensor> sensorsForThisDay = new Gson().fromJson(response.body(), listType);
        // Adds each sensor to be visited into the sensors list
        for (var sensor: sensorsForThisDay) {
            sensors.add(sensor);
        }
        System.out.println("Number of sensors " + sensors.size());
    }
    
    /*
     * Get the list of all polygons representing the buildings in the no-fly zone 
     */
    public void getNoFlyZonesFromServer(int portNumber) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + portNumber + "/buildings/no-fly-zones.geojson"))
                .build();
        var response = client.send(request, BodyHandlers.ofString());
        var featureCollection = FeatureCollection.fromJson(response.body());
        List<Feature> featureList = featureCollection.features();
        // Adds each feature to the list of No Fly Zones
        for (var feature : featureList) {
            noFlyZones.add(feature);
        }
    }
    
    /*
     * Fills a 34 x 34 grid with the distances from all sensors to each other sensor, and all sensors
     * to the start.
     */
    public void calculateDistanceMatrix(Coordinate droneStartPosition) throws IOException, InterruptedException {
        for (int i = 0; i < distanceMatrix.length; i++) {
            for (int j = 0; j < distanceMatrix.length; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 100;
                } else {
                    Coordinate destination;
                    Coordinate startFrom;
                    if (i == 0) {
                        destination = sensors.get(j - 1).getPosition();
                        startFrom = droneStartPosition;
                    } else {
                        if (j == 0) {
                            destination = droneStartPosition;
                            startFrom = sensors.get(i - 1).getPosition();
                        } else {
                            startFrom = sensors.get(i - 1).getPosition();
                            destination = sensors.get(j - 1).getPosition();
                        }
                    }
                    distanceMatrix[i][j] = startFrom.getEuclideanDistance(destination);
                }
            }
        }
    }
}
