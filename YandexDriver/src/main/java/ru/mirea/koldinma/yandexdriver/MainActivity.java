package ru.mirea.koldinma.yandexdriver;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yandex.mapkit.MapKit;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.Error;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.network.RemoteError;

import java.util.ArrayList;
import java.util.List;

import ru.mirea.koldinma.yandexdriver.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements DrivingSession.DrivingRouteListener{

    ActivityMainBinding binding;
    private Point ROUTE_START_LOCATION;
    private final Point ROUTE_END_LOCATION = new Point(55.749238, 37.583284);

    private Point SCREEN_CENTER;
    private MapView mapView;
    private MapObjectCollection mapObjects;
    private DrivingRouter drivingRouter;
    private DrivingSession drivingSession;
    LocationManager locationManager;
    LocationListener myLocationListener;
    boolean startFlag = true;
    boolean isWork = false;
    private int[] colors = {0xFFFF0000, 0xFF00FF00, 0x00FFBBBB, 0xFF0000FF};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapKitFactory.initialize(this);
        DirectionsFactory.initialize(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mapView = binding.mapview;
        mapView.getMap().setRotateGesturesEnabled(false);

        int loc1Per = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        int loc2Per = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int loc3Per = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);


        if (loc1Per== PackageManager.PERMISSION_GRANTED || loc2Per== PackageManager.PERMISSION_GRANTED) {
            isWork = true;

        } else {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (isWork){

            MapKit mapKit = MapKitFactory.getInstance();

            mapKit.createLocationManager().requestSingleUpdate(new LocationListener() {
                @Override
                public void onLocationUpdated(@NonNull Location location) {
                    if (startFlag) {
                        ROUTE_START_LOCATION = location.getPosition();

                        SCREEN_CENTER = new Point((ROUTE_START_LOCATION.getLatitude() + ROUTE_END_LOCATION.getLatitude()) / 2, (ROUTE_START_LOCATION.getLongitude() + ROUTE_END_LOCATION.getLongitude()) / 2);
                        mapView.getMap().move(new CameraPosition(SCREEN_CENTER, 10, 0, 0));


                        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter();
                        mapObjects = mapView.getMap().getMapObjects().addCollection();

                        submitRequest();
                        startFlag = false;
                    }
                }

                @Override
                public void onLocationStatusUpdated(@NonNull LocationStatus locationStatus) {

                }
            });

        }

        PlacemarkMapObject marker = mapView.getMap().getMapObjects().addPlacemark(ROUTE_END_LOCATION);
        marker.addTapListener(new MapObjectTapListener() {
            @Override
            public boolean onMapObjectTap(@NonNull MapObject mapObject, @NonNull Point
                    point) {
                Toast.makeText(getApplication(),"NICE PRICE", Toast.LENGTH_SHORT).show();

                return false;
            }
        });
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        locationManager.unsubscribe(myLocationListener);
        super.onStop();
    }
    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
        subscribeToLocationUpdate();
    }

    private void subscribeToLocationUpdate() {
        if (locationManager != null && myLocationListener != null) {
            locationManager.subscribeForLocationUpdates(0, 1000,1, false, FilteringMode.OFF, myLocationListener);
        }
    }

    private void submitRequest() {
        DrivingOptions drivingOptions = new DrivingOptions();
        VehicleOptions vehicleOptions = new VehicleOptions();


        drivingOptions.setRoutesCount(4);
        ArrayList<RequestPoint> requestPoints = new ArrayList<>();
        requestPoints.add(new RequestPoint(ROUTE_START_LOCATION,
                RequestPointType.WAYPOINT,
                null));
        requestPoints.add(new RequestPoint(ROUTE_END_LOCATION,
                RequestPointType.WAYPOINT,
                null));
        drivingSession = drivingRouter.requestRoutes(requestPoints, drivingOptions,
                vehicleOptions, this);
    }



    @Override
    public void onDrivingRoutes(@NonNull List<DrivingRoute> list) {
        int color;
        for (int i = 0; i < list.size(); i++) {
            // настроиваем цвета для каждого маршрута
            color = colors[i];
            // добавляем маршрут на карту
            mapObjects.addPolyline(list.get(i).getGeometry()).setStrokeColor(color);
        }
    }



    @Override
    public void onDrivingRoutesError(@NonNull Error error) {
        String errorMessage = getString(R.string.unknown_error_message);
        if (error instanceof RemoteError) {
            errorMessage = getString(R.string.remote_error_message);
        } else if (error instanceof NetworkError) {
            errorMessage = getString(R.string.network_error_message);
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }


}