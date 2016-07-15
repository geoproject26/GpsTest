package com.example.gpstest;

import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import java.util.List;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.http.GET;
import retrofit.http.Query;

public class MainActivity extends FragmentActivity {

    SupportMapFragment mapFragment;
    GoogleMap map;

    private LocationManager locationManager;
    private LocationListener locationListener;
    Marker label;
    boolean fromMarker, toMarker;
    String from, to,result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fromMarker = false;
        toMarker = false;
        //запрашиваем карту на вывод
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setMyLocationEnabled(true);

        if (map == null) {
            return;
        }
        //Обработка клика на карту
        //Если нет маркеров, то ставим А, если есть А - ставим B, если есть оба - сбрасываем и вводим заново
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latlng) {
                if ((fromMarker == false) && (toMarker == false)) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latlng);
                    markerOptions.title("" + latlng.latitude + " " + latlng.longitude);
                    BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.a_marker);
                    markerOptions.icon(bitmapDescriptor);
                    from=""+latlng.latitude+","+latlng.longitude;
                    map.addMarker(markerOptions);
                    fromMarker = true;
                } else {
                    if ((fromMarker == true) && (toMarker == false)) {
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latlng);
                        markerOptions.title("" + latlng.latitude + " " + latlng.longitude);
                        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.b_marker);
                        markerOptions.icon(bitmapDescriptor);
                        to=""+latlng.latitude+","+latlng.longitude;
                        map.addMarker(markerOptions);
                        toMarker = true;
                    } else {
                        if ((fromMarker == true) && (toMarker == true)) {
                            map.clear();
                            fromMarker = false;
                            toMarker = false;
                        }
                    }
                }
            }
        });
    }


    //Класс точки маршрута движения
    public class RouteResponse {

        public List<Route> routes;

        public String getPoints() {
            return this.routes.get(0).overview_polyline.points;
        }

        class Route {
            OverviewPolyline overview_polyline;
        }

        class OverviewPolyline {
            String points;
        }
    }
    //Интерфейс для запросак маршрута
    public interface RouteApi {
        @GET("/maps/api/directions/json")
        void getRoute(
                @Query(value = "origin", encodeValue = false) String position,
                @Query(value = "destination", encodeValue = false) String destination,
                @Query("sensor") boolean sensor,
                @Query("language") String language,
                Callback<RouteResponse> cb
        );
    }

    // метод показа маршрута
    public void showRoute(View view ) {
        //Переход от интерфейса к API
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://maps.googleapis.com")
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();
        RouteApi routeService = restAdapter.create(RouteApi.class);

        //Вызов запроса на маршрут (асинхрон)
        routeService.getRoute(from, to, true, "ru", new Callback<RouteResponse>() {
            public void success(RouteResponse arg0, retrofit.client.Response arg1) {
                //Если прошло успешно, то декодируем маршрут в точки LatLng
                List<LatLng> mPoints = PolyUtil.decode(arg0.getPoints());
                //Строим полилинию
                PolylineOptions line = new PolylineOptions();
                line.width(4f).color(R.color.colorPrimary);
                LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
                for (int i = 0; i < mPoints.size(); i++) {

                    line.add((LatLng) mPoints.get(i));
                    latLngBuilder.include((LatLng) mPoints.get(i));
                }
                map.addPolyline(line);
                int size = getResources().getDisplayMetrics().widthPixels;
                LatLngBounds latLngBounds = latLngBuilder.build();
                CameraUpdate track = CameraUpdateFactory.newLatLngBounds(latLngBounds, size, size, 25);
                map.moveCamera(track);
                }
            //Если запрос прошел неудачно
            public void failure(RetrofitError arg0) {
            }
        });
    }
}