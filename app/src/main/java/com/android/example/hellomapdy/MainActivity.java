package com.android.example.hellomapdy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoBox;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoCorridor;
import com.here.sdk.core.GeoPolygon;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Metadata;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.GestureState;
import com.here.sdk.gestures.GestureType;
import com.here.sdk.gestures.TapListener;
import com.here.sdk.mapview.MapCamera;
import com.here.sdk.mapview.MapCameraObserver;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapPolygon;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.mapview.MapViewBase;
import com.here.sdk.mapview.PickMapItemsResult;
import com.here.sdk.routing.AvoidanceOptions;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.HazardousGood;
import com.here.sdk.routing.OptimizationMode;
import com.here.sdk.routing.PedestrianOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RouteOptions;
import com.here.sdk.routing.RouteTextOptions;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.AddressQuery;
import com.here.sdk.search.CategoryQuery;
import com.here.sdk.search.Details;
import com.here.sdk.search.Place;
import com.here.sdk.search.PlaceCategory;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.SuggestCallback;
import com.here.sdk.search.Suggestion;
import com.here.sdk.search.TextQuery;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.provider.Telephony.Mms.Part.TEXT;


public class MainActivity extends AppCompatActivity {

    private MapView mapView;

    private int styleCounter =0;
    private MapScheme schene = MapScheme.NORMAL_DAY;

    private int cameraCounter = 0;
    private GeoCoordinates cameraCoordinates;
    private double bearingInDegrees;
    private double tiltInDegrees;
    private MapCamera.OrientationUpdate cameraOrientation;
    private MapCamera.OrientationUpdate routecameraOrientation;
    private MapCamera.OrientationUpdate circlecameraOrientation;
    private double distanceInMeters;

    private SearchEngine searchEngine;

    // For routing service
    private Context context;
    private TextView routeTextView;
    private RoutingEngine routingEngine;
    //long press to generate the waypoint list and marker
    private List<Waypoint> waypoints = new ArrayList<>();
    private List<MapMarker> waypointMarkers = new ArrayList<>();
    private List<MapPolyline> routeMapPolylines = new ArrayList<>();
    private List<GeoCoordinates> verticeses;


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get aMapView instance from the layout
        Log.i("TAG", "Map initialize");
        mapView =findViewById(R.id.mapview);
        routeTextView = findViewById(R.id.routeTextView);
        mapView.onCreate(savedInstanceState);


        context = getApplicationContext();


//        //ASK FOR PERMISSION！
//        handleAndroidPermissions();

        loadMapScene();
        setLongPressGestureHandler();
        //PinchRotateGestureHandler();
        try{

            searchEngine=new SearchEngine();
        }catch (InstantiationErrorException e){
            throw new RuntimeException("oh something went wrong");
        }

        try{
            routingEngine =new RoutingEngine();
        }catch (InstantiationErrorException e){
            throw new RuntimeException("oh something wrong");
        }
    }

//    private void handleAndroidPermissions(){
//
//    }

    private void loadMapScene() {
        //Load a schene from the HERE SDK to render the map with a map scheme
        mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapError mapError) {
                bearingInDegrees = 0 ;
                tiltInDegrees = 0;
                cameraOrientation = new MapCamera.OrientationUpdate(bearingInDegrees,tiltInDegrees);
                if (mapError == null) {
                    mapView.getCamera().lookAt(new GeoCoordinates(25.0782, 121.3884),cameraOrientation, 10000);
                    //mapView.getGestures().disableDefaultAction(GestureType.TWO_FINGER_PAN);
                    //mapView.getGestures().disableDefaultAction(GestureType.PINCH_ROTATE);
                    Log.d("TAG", "Map Set location");
                }else {
                    Log.d("TAG", "Loading map failed :map Error: "+ mapError.name());
                }
            }
        });
        //set up the maximum and minimum camera zoom  level
        MapCamera camera = mapView.getCamera();
        MapCameraObserver MapCameraObserver = new MapCameraObserver() {
            @Override
            public void onCameraUpdated(MapCamera.State cameraState){
                if (cameraState.distanceToTargetInMeters >= 8550000){
                    camera.setDistanceToTarget(8550000);
                } else if (cameraState.distanceToTargetInMeters < 1000) {
                    camera.setDistanceToTarget(1000);
                }
            }
        };
        camera.addObserver(MapCameraObserver);
    }


    public void changeStyle(View view) {
        // Button Click to change the map scheme
        styleCounter++;
        if(styleCounter == 5) styleCounter = 0;

        if (styleCounter == 0) schene = MapScheme.NORMAL_DAY;
        if (styleCounter == 1) schene = MapScheme.NORMAL_NIGHT;
        if (styleCounter == 2) schene = MapScheme.GREY_DAY;
        if (styleCounter == 3) schene = MapScheme.GREY_NIGHT;
        if (styleCounter == 4) schene = MapScheme.SATELLITE;

        mapView.getMapScene().loadScene(schene, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(MapError mapError) {
                if(mapError == null) {
                    //do something
                }else{
                    //error handling
                }
            }
        });

    }

    public void loadStyle(View view){
        // customized map schene
        String filename = "Berlin_Style.json";
        AssetManager assetManager = this.getAssets();

        try{
            InputStream inputStream = assetManager.open(filename);
            byte[] bytes =new byte[0];
            bytes =new byte[inputStream.available()];
            inputStream.read(bytes);
            String str = new String(bytes);
            Log.i("TAG","HiHIHIHHI");
            Log.d("TAG",str);
            Log.d("TAG", "str.length() :" +str.length());

        }catch (IOException e) {
            Log.v("TAG","HiHIHIHHI");
            Log.e("TAG", "ERROR : Map Style not found!");
        }

        mapView.getMapScene().loadScene("" + filename, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(MapError mapError) {
                if(mapError == null) {
                    //do something
                }else{
                    //error handling
                }
            }
        });
    }

    public void changeCamera(View view) {
        //buttom click to change the camera (tilt/bearing)
        cameraCounter++;
        if (cameraCounter == 4) cameraCounter = 0;
        cameraCoordinates = new GeoCoordinates(25.0568438,121.580323);
        bearingInDegrees = 0 ;
        tiltInDegrees = 0;
        cameraOrientation = new MapCamera.OrientationUpdate(bearingInDegrees,tiltInDegrees);
        distanceInMeters = 10000;

        if(cameraCounter == 0) {

            mapView.getCamera().lookAt(cameraCoordinates,cameraOrientation,distanceInMeters);

        }else if (cameraCounter == 1){
            bearingInDegrees = 90;
            distanceInMeters = 5000;
            cameraOrientation = new MapCamera.OrientationUpdate(bearingInDegrees,tiltInDegrees);
            mapView.getCamera().lookAt(cameraCoordinates,cameraOrientation,distanceInMeters);

        }else if (cameraCounter == 2){
            bearingInDegrees = 180;
            tiltInDegrees = 70;
            distanceInMeters = 5000;
            cameraOrientation = new MapCamera.OrientationUpdate(bearingInDegrees,tiltInDegrees);
            mapView.getCamera().lookAt(cameraCoordinates,cameraOrientation,distanceInMeters);

        }else if (cameraCounter == 3){
            GeoBox cameraBox = new GeoBox(new GeoCoordinates(25.01666, 121.47574), new GeoCoordinates(25.08784, 121.59968));

            mapView.getCamera().lookAt(cameraBox, cameraOrientation);

        }


    }

    public void addMarker(View view){
        //Create MapImage
        MapImage mapImage = MapImageFactory.fromResource(this.getResources(), R.drawable.waypoint);
        //Create Anchor (generally the anchor is placed on the middle of the image rather on buttom of image)
        Anchor2D anchor2D = new Anchor2D(0.5f,1.1f);

        //add metadata
        Metadata meta = new Metadata();
        meta.setString("dino","stegosaurus");

        //Create MapMaker
        MapMarker mapMarker = new MapMarker(new GeoCoordinates(25.077717,121.385880),mapImage,anchor2D);
        // set up the marker have the meta information
        mapMarker.setMetadata(meta);
        //Add the marker to map
        mapView.getMapScene().addMapMarker(mapMarker);

        setTapGestureHandler();
    }

    public void addPolyline(View view) {
        //Create GeoPolyline
        ArrayList<GeoCoordinates> polylineCoordinates = new ArrayList();
        polylineCoordinates.add(new GeoCoordinates(25.08003, 121.3846));
        polylineCoordinates.add(new GeoCoordinates(25.08089, 121.38297));
        polylineCoordinates.add(new GeoCoordinates(25.07766, 121.38087));
        polylineCoordinates.add(new GeoCoordinates(25.07455, 121.37791));
        polylineCoordinates.add(new GeoCoordinates(25.07972, 121.37079));

        GeoPolyline geoPolyline;
        try {
            geoPolyline = new GeoPolyline(polylineCoordinates);
        } catch (InstantiationErrorException e) {
            geoPolyline = null;
        }

        //Define the style of the polyline
        float widthInPixels = 30;
        // SDK's Color Class
        Color lineColor = new Color((short) 72, (short) 218, (short) 208, (short) 255);

        //Create MapPolyline
        MapPolyline mapPolyline = new MapPolyline(geoPolyline, widthInPixels, lineColor);

        //Add Polyline to the map
        mapView.getMapScene().addMapPolyline(mapPolyline);
    }

    public void addCircle (View view) {
        float radiusInMeters = 1500;
        float radiusInMeters1 = 1510;
        GeoCircle geoCircle;
        //GeoCircle geoCircle1;
        GeoPolygon geoPolygon = null;
        //GeoPolygon geoPolygon1 = null;
        GeoCoordinates geoCoordinates = new GeoCoordinates(25.08003,121.3846);
        GeoBox geobox;

        try {
            geoCircle = new GeoCircle(geoCoordinates,radiusInMeters);
            geoPolygon = new GeoPolygon(geoCircle);
            //create the List(geoCoordinates) of vertices
            geobox = GeoBox.containing(geoPolygon.vertices);
            //verticeses.add(geoPolygon.vertices.get(0));
            Log.v("vertices", String.valueOf(geobox));

            //add mapMarker on the centre of circle
            //Create MapImage
            MapImage mapImage = MapImageFactory.fromResource(this.getResources(), R.drawable.poi);
            //Create Anchor (generally the anchor is placed on the middle of the image rather on buttom of image)
            Anchor2D anchor2D = new Anchor2D(0.5f,1.0f);
            //Create MapMaker
            MapMarker mapMarker = new MapMarker(geoCoordinates,mapImage,anchor2D);
            //Add the marker to map
            mapView.getMapScene().addMapMarker(mapMarker);


            // adjust view camera to match the whole geoPolygon
            mapView.getCamera().lookAt(geobox.expandedBy(30,500,30,500),new MapCamera.OrientationUpdate(0.0,0.0));

            //geoCircle1 = new GeoCircle(geoCoordinates,radiusInMeters1);
            //geoPolygon1 = new GeoPolygon(geoCircle1) ;
        } catch (InstantiationError | InstantiationErrorException e) {
            geoCircle = null;
        }

        //Define the style of the circle
        float widthInPixels = 30;
        // SDK's Color Class
        Color fillColor = new Color((short) 72, (short) 218, (short) 208, (short) 100);
        Color lineColor = new Color((short) 0, (short) 0, (short) 8, (short) 50);

        //Create MapPolyline
        MapPolygon mapPolygon  = new MapPolygon(geoPolygon,fillColor);
        //MapPolygon mapPolygon1 = new MapPolygon(geoPolygon1,lineColor);

        //Add Polyline to the map
        mapView.getMapScene().addMapPolygon(mapPolygon);
        //mapView.getMapScene().addMapPolygon(mapPolygon1);

        // adjust view camera to match the whole  route
        //circlecameraOrientation = new MapCamera.OrientationUpdate(0.0,0.0);
        //mapView.getCamera().lookAt(geoCoordinates,5000);

    }

    public void addPin(View view){
        TextView textView = new TextView(getApplicationContext());
        textView.setTextColor(android.graphics.Color.parseColor("#FFC0CB"));
        textView.setText("Dinosaurs rock!");

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setBackgroundResource(R.color.colorPrimary);
        linearLayout.setPadding(10,10,10,10);
        linearLayout.addView(textView);

        mapView.pinView(linearLayout, new GeoCoordinates(25.07972, 121.37079));
    }

    public void setTapGestureHandler(){
        mapView.getGestures().setTapListener(new TapListener() {
            @Override
            public void onTap(Point2D touchPoint) {
                //v:2 means far away touchPoint 2 pixel
                mapView.pickMapItems(touchPoint, 2, new MapViewBase.PickMapItemsCallback() {
                    @Override
                    public void onPickMapItems(PickMapItemsResult pickMapItemsResult) {
                        List<MapMarker> mapMarkerList = pickMapItemsResult.getMarkers();
                        MapMarker pickedMarker = mapMarkerList.get(0);
                        Metadata meta = pickedMarker.getMetadata();
                        if (meta == null){
                            Toast toast = Toast.makeText(getApplicationContext(), "No I am not here :( ",Toast.LENGTH_SHORT);
                            toast.show();
                        }else if (meta.getString("dino").equals("stegosaurus")){
                            Toast toast = Toast.makeText(getApplicationContext(), "you found me",Toast.LENGTH_SHORT);
                            toast.show();
                            //remove object
                            mapView.getMapScene().removeMapMarker(pickedMarker);
                        }
                    }
                });
            }
        });
    }

    public void searchPlaces(View view){
        //set up the search criteria
        int maxiItems = 10;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.ZH_TW, maxiItems);

        //Fetch the search text from input bar and search by center of screen
        EditText editText = findViewById(R.id.searchText);
        TextQuery textQuery = new TextQuery(editText.getText().toString(), getScreenCenter());

        searchEngine.search(textQuery, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                for (Place result : list) {

                    TextView textView = new TextView(getApplicationContext());
                    textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                    textView.setText(result.getTitle() + "\n" + result.getAddress().addressText);

                    LinearLayout linearLayout = new LinearLayout(getApplicationContext());
                    linearLayout.setBackgroundResource(R.color.colorPrimary);
                    linearLayout.setPadding(10,10,10,10);
                    linearLayout.addView(textView);

                    mapView.pinView(linearLayout, result.getGeoCoordinates());
                    //move the camera to the result location
                    mapView.getCamera().lookAt(result.getGeoCoordinates());
                }
            }
        });
    }

    public void searchPlacesCategory(View view){

        List<PlaceCategory> categoryList = new ArrayList<>();
        categoryList.add(new PlaceCategory(PlaceCategory.BUSINESS_AND_SERVICES_FUELING_STATION));
        categoryList.add(new PlaceCategory(PlaceCategory.EAT_AND_DRINK));
        categoryList.add(new PlaceCategory(PlaceCategory.GOING_OUT_CINEMA));
        categoryList.add(new PlaceCategory(PlaceCategory.BUSINESS_AND_SERVICES_BANKING));
        CategoryQuery categoryQuery = new CategoryQuery(categoryList, new GeoCoordinates(25.0782904,121.3884666));

        int maxItems = 30;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.ZH_TW, maxItems);

        searchEngine.search(categoryQuery, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                for (Place result : list) {

                    TextView textView = new TextView(getApplicationContext());
                    textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
//                    Convert Category List to String
                    Details details = result.getDetails();
                    String placeCategoryIds = "";
                    for (PlaceCategory placeCategory: details.categories){
                        placeCategoryIds = placeCategoryIds + "\n";
                        placeCategoryIds = placeCategoryIds + placeCategory.getId();
//                        Print the information on console log
                        Log.i("SDK", result.getId() +" / "+ placeCategory.getId());
                    };
                    textView.setText(result.getTitle()+ placeCategoryIds);
                    LinearLayout linearLayout = new LinearLayout(getApplicationContext());
                    linearLayout.setBackgroundResource(R.color.colorPrimary);
                    linearLayout.setPadding(10,10,10,10);
                    linearLayout.addView(textView);

                    mapView.pinView(linearLayout, result.getGeoCoordinates());
                    //move the camera to the result location
                    mapView.getCamera().lookAt(result.getGeoCoordinates());
                }
            }
        });
    }

    public void searchAddress(View view){
        int maxiItems = 1;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.ZH_TW, maxiItems);

        //Fetch the search text from input bar and search by center of screen
        EditText editText = findViewById(R.id.searchText);
        AddressQuery addressQuery = new AddressQuery(editText.getText().toString(), getScreenCenter());

        searchEngine.search(addressQuery, searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                for (Place result : list) {

                    TextView textView = new TextView(getApplicationContext());
                    textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                    textView.setText(result.getTitle());

                    LinearLayout linearLayout = new LinearLayout(getApplicationContext());
                    linearLayout.setBackgroundResource(R.color.colorPrimary);
                    linearLayout.setPadding(10,10,10,10);
                    linearLayout.addView(textView);

                    mapView.pinView(linearLayout, result.getGeoCoordinates());
                    //move the camera to the result location
                    mapView.getCamera().lookAt(result.getGeoCoordinates());
                }
            }
        });

    }

    public void searchRevAddress(View view){
        int maxiItems = 1;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.ZH_TW, maxiItems);

        //Fetch the search text from input bar and search by center of screen
        EditText editText = findViewById(R.id.searchText);
        AddressQuery addressQuery = new AddressQuery(editText.getText().toString(), getScreenCenter());
        //new GeoCoordinates(25.077746,121.386128) or function -> getScreenCenter()
        searchEngine.search(getScreenCenter(), searchOptions, new SearchCallback() {
            @Override
            public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
                for (Place result : list) {

                    TextView textView = new TextView(getApplicationContext());
                    textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                    textView.setText(result.getTitle());

                    LinearLayout linearLayout = new LinearLayout(getApplicationContext());
                    linearLayout.setBackgroundResource(R.color.colorPrimary);
                    linearLayout.setPadding(10,10,10,10);
                    linearLayout.addView(textView);

                    mapView.pinView(linearLayout, result.getGeoCoordinates());
                    //move the camera to the result location
                    mapView.getCamera().lookAt(result.getGeoCoordinates());
                }
            }
        });

    }

    public void searchAutoSuggest(View view){
        int maxiItems = 5;
        SearchOptions searchOptions = new SearchOptions(LanguageCode.ZH_TW, maxiItems);

        //Fetch the search text from input bar and search by center of screen
        EditText editText = findViewById(R.id.searchText);
        TextQuery textQuery = new TextQuery(editText.getText().toString(), getScreenCenter());

//        //Category Search uncomplete
//        CategoryQuery categoryQuery = new CategoryQuery();

        searchEngine.suggest(textQuery, searchOptions, new SuggestCallback() {
            @Override
            public void onSuggestCompleted(@Nullable SearchError searchError, @Nullable List<Suggestion> list) {
                // Error

                ArrayList<String> arrayList = new ArrayList<>();
                for (Suggestion suggestionResult : list) {
                    Place place = suggestionResult.getPlace();
                    if (place != null) {
                        arrayList.add(place.getTitle());
                    }else {
                        arrayList.add(suggestionResult.getTitle());
                    }
                }
                //put array on the list bar
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_expandable_list_item_1, arrayList);

                ListView listView =findViewById((R.id.suggestionListView));
                listView.setAdapter(arrayAdapter);
            }
        });

    }

    public void calculateRoute(View view) {

        RouteOptions routeOptions = new RouteOptions();
        // how many route return
        routeOptions.alternatives =3 ;
        routeOptions.optimizationMode = OptimizationMode.FASTEST;

        CarOptions options = new CarOptions(routeOptions, new RouteTextOptions(), new AvoidanceOptions());

        routingEngine.calculateRoute(
                waypoints,
                options,  // or use "new CarOptions() as car default
                new CalculateRouteCallback() {
                    @Override
                    public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                        if(routingError == null) {
                            // obtain the first route via get(0) instead of the alternative route
                            Route route = routes.get(0);
                            try {
                                drawRoute(route);
                            } catch (InstantiationErrorException e) {
                                e.printStackTrace();
                            }
                        }else {
                            // check what's the error
                        }
                    }
                }
        );
    }

    private void drawRoute(Route route) throws InstantiationErrorException {
        GeoPolyline routeGeoPolyline;

        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        }catch (InstantiationErrorException e) {
            // need to return because of the route must have more than two points
            return ;
        }
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, 20, new Color((short) 0x00,(short) 0x90,(short) 0x8A,(short) 0xA0));

        mapView.getMapScene().addMapPolyline(routeMapPolyline);

        // add routeMapPolyline to routeMapPolylines array
        routeMapPolylines.add(routeMapPolyline);

        // add route info into text field
        routeTextView.append("You will arrive at your destination in " + route.getDurationInSeconds() + " seconds.\n");
        routeTextView.append("Currently " + route.getTrafficDelayInSeconds() + " seconds slow due to traffic jam.\n");
        routeTextView.append("Your destination is " + route.getLengthInMeters() + " meters away. ");

        // adjust view camera to match the whole  route
        routecameraOrientation = new MapCamera.OrientationUpdate(0.0,0.0);
        mapView.getCamera().lookAt(route.getBoundingBox().expandedBy(1500.0,0,1500.0,0), routecameraOrientation);

    }

    public void calculateRoutePedestrian(View view) {
        routingEngine.calculateRoute(
                waypoints,
                // default car routing
                new PedestrianOptions(),
                new CalculateRouteCallback() {
                    @Override
                    public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                        if(routingError == null) {
                            // obtain the first route via get(0) instead of the alternative route
                            Route route = routes.get(0);
                            drawRoutePedestrian(route);
                        }else {
                            // check what's the error
                        }
                    }
                }
        );

    }

    private void drawRoutePedestrian(Route route) {
        GeoPolyline routeGeoPolyline;

        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        }catch (InstantiationErrorException e) {
            // need to return because of the route must have more than two points
            return ;
        }

        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline, 25, new Color((short) 0x00,(short) 0x00,(short) 0x8A,(short) 0xAA));

        mapView.getMapScene().addMapPolyline(routeMapPolyline);

        // add routeMapPolyline to routeMapPolylines array
        routeMapPolylines.add(routeMapPolyline);

        // add route info into text field
        routeTextView.append("You will arrive at your destination in " + route.getDurationInSeconds() + " seconds.\n");
        routeTextView.append("Currently " + route.getTrafficDelayInSeconds() + " seconds slow due to traffic jam.\n");
        routeTextView.append("Your destination is " + route.getLengthInMeters() + " meters away. ");

        // adjust view camera to match the whole  route
        routecameraOrientation = new MapCamera.OrientationUpdate(0.0,0.0);
        mapView.getCamera().lookAt(route.getBoundingBox(), routecameraOrientation);

    }

    public void clearMap(View view) {
        for (MapMarker marker : waypointMarkers) {
            mapView.getMapScene().removeMapMarker(marker);
        }
        for (MapPolyline polyline : routeMapPolylines) {
            mapView.getMapScene().removeMapPolyline(polyline);
        }

        waypoints.clear();
    }

    private void setLongPressGestureHandler () {
        mapView.getGestures().setLongPressListener(((gestureState, touchPoint) -> {
            if (gestureState == GestureState.BEGIN) {
                //SOMETHING happen as below Marker ADD
                // define marker image from drawable folder
                MapImage waypointImage = MapImageFactory.fromResource(context.getResources(), R.drawable.poi);
                // Add Market by pixels location by viewToGeoCoordinates method
                MapMarker waypointMarker = new MapMarker (mapView.viewToGeoCoordinates(touchPoint),waypointImage);
                mapView.getMapScene().addMapMarker(waypointMarker);

                // add markers to marker array list
                waypointMarkers.add(waypointMarker);

                // add touchPoint to waypoints array list
                waypoints.add(new Waypoint(mapView.viewToGeoCoordinates(touchPoint)));
            }
        }));
    }

//    private void PinchRotateGestureHandler() {
//        mapView.getGestures().setPinchRotateListener(((gestureState, point2D, point2D2, d, angle) -> {
//            if (gestureState == GestureState.BEGIN) {
//                MapCamera.OrientationUpdate(bearingInDegrees,tiltInDegrees),
//                //SOMETHING happen as below Marker ADD
//                // define marker image from drawable folder
//                MapImage waypointImage = MapImageFactory.fromResource(context.getResources(), R.drawable.poi);
//                // Add Market by pixels location by viewToGeoCoordinates method
//                MapMarker waypointMarker = new MapMarker (mapView.viewToGeoCoordinates(touchPoint),waypointImage);
//                mapView.getMapScene().addMapMarker(waypointMarker);
//
//                // add markers to marker array list
//                waypointMarkers.add(waypointMarker);
//
//                // add touchPoint to waypoints array list
//                waypoints.add(new Waypoint(mapView.viewToGeoCoordinates(touchPoint)));
//            }
//        }));
//    }

    public GeoCoordinates getScreenCenter() {
        int screenWidthInPixels = mapView.getWidth();
        int screenHeightInPixels =mapView.getHeight();
        Point2D point = new Point2D(screenWidthInPixels*0.5, screenHeightInPixels*0.5);
        //convert the middle of screen to coordinate
        return mapView.viewToGeoCoordinates(point);
    }

   @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
