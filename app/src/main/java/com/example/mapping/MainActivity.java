package com.example.mapping;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    String[] perms = {Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION};
    private static final String ROUTE_LAYER_ID = "route-layer-id";
    private static final String ROUTE_SOURCE_ID = "route-source-id";
    private static final String ICON_SOURCE_ID = "icon-source-id";
    MapView mapView;
    Point origin;
    Point destination;
    DirectionsRoute currentRoute;
    MapboxDirections client;
    MapboxMap mapbox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this,
                "pk.eyJ1Ijoic3RhcnRvLXRheGkiLCJhIjoiY2tlNDZ6amxjMHE2azJ0bzRvcmVhcTZkcyJ9.RmcFBGdhI8rqjl-suodU0A");

        setContentView(R.layout.activity_main);


        mapView = findViewById(R.id.mapView);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);



    }

    @Override
    public void onMapReady ( @NonNull MapboxMap mapboxMap ) {

        mapbox = mapboxMap;
        mapboxMap.setStyle ( Style.MAPBOX_STREETS , style -> {
            origin = Point.fromLngLat ( 80.89901000 , 26.80882000 );
            destination = Point.fromLngLat ( 80.87320000 , 26.88745000 );
            initSource ( style );
            initLayers ( style );
            getRoute ( mapbox , origin , destination );
            IconFactory iconFactory = IconFactory.getInstance ( this );

            Icon pickup = iconFactory.fromBitmap ( bitmapDescriptorFromVector ( this , R.drawable.pin ) );
            Icon dropoff = iconFactory.fromBitmap ( bitmapDescriptorFromVector ( this , R.drawable.pin ) );

            mapboxMap.animateCamera ( CameraUpdateFactory.newLatLngZoom ( new LatLng( origin.latitude ( ) , origin.longitude ( ) ) , 11 ) , 1200 );

            Marker marker = mapboxMap.addMarker (
                    new MarkerOptions( ).title ( "Current Location" ).position ( new LatLng ( origin.latitude ( ) , origin.longitude ( ) ) ).icon ( pickup ) );
            marker.getInfoWindow ( );

            Marker marker2 = mapboxMap.addMarker (
                    new MarkerOptions ( ).title ( "Pickup Location" )
                            .position ( new LatLng ( destination.latitude ( ) , destination.longitude ( ) ) ).icon ( dropoff ) );
            marker2.getInfoWindow ( );

        } );
    }

    private Bitmap bitmapDescriptorFromVector(MainActivity mainActivity, int pin) {
        Drawable background = ContextCompat.getDrawable ( mainActivity , pin );
        background.setBounds ( 0 , 0 , background.getIntrinsicWidth ( ) , background.getIntrinsicHeight ( ) );
        Bitmap bitmap = Bitmap.createBitmap ( background.getIntrinsicWidth ( ) , background.getIntrinsicHeight ( ) , Bitmap.Config.ARGB_8888 );
        Canvas canvas = new Canvas ( bitmap );
        background.draw ( canvas );
        return bitmap;
    }

    private void initSource ( @NonNull Style loadedMapStyle ) {
        loadedMapStyle.addSource ( new GeoJsonSource ( ROUTE_SOURCE_ID ) );
        GeoJsonSource iconGeoJsonSource = new GeoJsonSource ( ICON_SOURCE_ID , FeatureCollection.fromFeatures ( new Feature[] {
                Feature.fromGeometry ( Point.fromLngLat ( origin.longitude ( ) , origin.latitude ( ) ) ) ,
                Feature.fromGeometry ( Point.fromLngLat ( destination.longitude ( ) , destination.latitude ( ) ) ) } ) );
        loadedMapStyle.addSource ( iconGeoJsonSource );
    }

    private void initLayers ( @NonNull Style loadedMapStyle ) {
        LineLayer routeLayer = new LineLayer ( ROUTE_LAYER_ID , ROUTE_SOURCE_ID );

        routeLayer.setProperties (
                lineCap ( Property.LINE_CAP_ROUND ) ,
                lineJoin ( Property.LINE_JOIN_ROUND ) ,
                lineWidth ( 5f ) ,
                lineColor ( Color.parseColor ( "#ffbc01" ) )
        );
        loadedMapStyle.addLayer ( routeLayer );


    }

    private void getRoute (final MapboxMap mapboxMap , Point origin , Point destination ) {
        Toast.makeText ( this , "get route" , Toast.LENGTH_SHORT ).show ( );
        client = MapboxDirections.builder ( )
                .origin ( origin )
                .destination ( destination )
                .overview ( DirectionsCriteria.OVERVIEW_FULL )
                .profile ( DirectionsCriteria.PROFILE_DRIVING )
                .accessToken ( getString ( R.string.map_box_key ) )
                .build ( );

        client.enqueueCall ( new Callback< DirectionsResponse >( ) {
            @Override
            public void onResponse ( Call < DirectionsResponse > call , retrofit2.Response < DirectionsResponse > response ) {
                Log.d ( getClass ( ).getSimpleName ( ) , "Response code: " + response.code ( ) );
                if ( response.body ( ) == null ) {
                    Log.d ( getClass ( ).getSimpleName ( ) , "No routes found, make sure you set the right user and access token." );
                    return;
                }
                else if ( response.body ( ).routes ( ).size ( ) < 1 ) {
                    Log.d ( getClass ( ).getSimpleName ( ) , "No routes found" );
                    return;
                }
                currentRoute = response.body ( ).routes ( ).get ( 0 );


                if ( mapboxMap != null ) {
                    mapboxMap.getStyle ( style -> {
                        GeoJsonSource source = style.getSourceAs ( ROUTE_SOURCE_ID );
                        if ( source != null ) {
                            source.setGeoJson ( LineString.fromPolyline ( currentRoute.geometry ( ) , 6 ) );
                            Toast.makeText ( MainActivity.this , "Current Route : " + currentRoute.distance ( ) , Toast.LENGTH_SHORT ).show ( );
                        }
                    } );
                }
            }

            @Override
            public void onFailure (Call<DirectionsResponse> call , Throwable throwable ) {
                Log.d ( getClass ( ).getSimpleName ( ) , "Error: " + throwable.getMessage ( ) );
                Toast.makeText ( MainActivity.this , "Error: " + throwable.getMessage ( ) , Toast.LENGTH_SHORT ).show ( );
            }
        } );
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {

    }
}