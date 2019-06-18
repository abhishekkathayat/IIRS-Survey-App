package com.iirs.iirssurveyapp;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.iirs.iirssurveyapp.Adapters.LayersPagerAdapter;
import com.iirs.iirssurveyapp.Models.LayersModel;
import com.iirs.iirssurveyapp.Models.PopulationModel;
import com.iirs.iirssurveyapp.Rest.ApiClient;
import com.iirs.iirssurveyapp.Rest.ApiInterface;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements PermissionsListener {

    public TabLayout tabLayout;
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private List<LayersModel> layerslist = new ArrayList<>();
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;


    private MainActivityLocationCallback callback = new MainActivityLocationCallback(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_main);

        RelativeLayout relativeLayout = findViewById(R.id.main_bottom_sheet);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(relativeLayout);
        bottomSheetBehavior.setFitToContents(false);
        final ImageButton closeopenbutton = findViewById(R.id.closeopen_button);
        bottomSheetBehavior.setHideable(false);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch(newState) {
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        closeopenbutton.setImageResource(R.drawable.ic_arrow_down);
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        closeopenbutton.setImageResource(R.drawable.ic_arrow_up);
                    }
                    break;
                    case BottomSheetBehavior.STATE_HALF_EXPANDED: {
                        closeopenbutton.setImageResource(R.drawable.ic_dash);
                    }
                    break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }
            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });

        closeopenbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    closeopenbutton.setImageResource(R.drawable.ic_dash);
                }
                else if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    closeopenbutton.setImageResource(R.drawable.ic_arrow_up);
                }
                else if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                    closeopenbutton.setImageResource(R.drawable.ic_dash);
                }
            }
        });

        ViewPager viewPager = findViewById(R.id.layers_pager);
        tabLayout = findViewById(R.id.layers_tab);
        tabLayout.setupWithViewPager(viewPager);
        getLayers();
        PagerAdapter pagerAdapter = new LayersPagerAdapter(getSupportFragmentManager(), layerslist);
        viewPager.setAdapter(pagerAdapter);

        SupportMapFragment mapFragment;
        if(savedInstanceState == null) {
            final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            MapboxMapOptions options = new MapboxMapOptions();
            options.camera(new CameraPosition.Builder()
                    .target(new LatLng(30.340979,78.043996))
                    .zoom(12)
                    .build());

            mapFragment = SupportMapFragment.newInstance(options);
            transaction.add(R.id.location_fragment_container, mapFragment, "com.mapbox.map");
            transaction.commit();
        }
        else {
            mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentByTag("com.mapbox.map");
        }

        Objects.requireNonNull(mapFragment).getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                MainActivity.this.mapboxMap = mapboxMap;
                mapboxMap.setStyle(Style.MAPBOX_STREETS,
                        new Style.OnStyleLoaded() {
                            @Override
                            public void onStyleLoaded(@NonNull Style style) {
                                enableLocationComponent(style);
                            }
                        });
            }
        });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {

        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            locationComponent.activateLocationComponent(locationComponentActivationOptions);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        }
        else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        LocationEngine locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            if (mapboxMap.getStyle() != null) {
                enableLocationComponent(mapboxMap.getStyle());
            }
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void getLayers() {
        LayersModel layersModel = new LayersModel("Soil");
        layerslist.add(layersModel);
        layersModel = new LayersModel("Geomorphology");
        layerslist.add(layersModel);
        layersModel = new LayersModel("Lithology");
        layerslist.add(layersModel);
        layersModel = new LayersModel("Slope");
        layerslist.add(layersModel);
        layersModel = new LayersModel("Aspect");
        layerslist.add(layersModel);
        layersModel = new LayersModel("Population");
        layerslist.add(layersModel);
        layersModel = new LayersModel("Drainage");
        layerslist.add(layersModel);
    }

    private static class MainActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private int layernumber = 0;
        private double latitude = 0.0;
        private double longitude = 0.0;
        private final WeakReference<MainActivity> activityWeakReference;

        MainActivityLocationCallback(MainActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }


        @Override
        public void onSuccess(LocationEngineResult result) {
            final MainActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }

                /*
                Toast.makeText(activity, String.format(activity.getString(R.string.new_location),
                        String.valueOf(result.getLastLocation().getLatitude()), String.valueOf(result.getLastLocation().getLongitude())),
                        Toast.LENGTH_SHORT).show();
                */

                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                    latitude = result.getLastLocation().getLatitude();
                    longitude = result.getLastLocation().getLongitude();
                    final ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
                    activity.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(TabLayout.Tab tab) {
                            layernumber = tab.getPosition();
                            Call<PopulationModel> call = apiInterface.getLocationDetails(Objects.requireNonNull(Objects.requireNonNull(activity.tabLayout.getTabAt(layernumber)).getText()).toString(), (float)latitude, (float)longitude);
                            call.enqueue(new Callback<PopulationModel>() {
                                @Override
                                public void onResponse(@NonNull Call<PopulationModel> call, @NonNull Response<PopulationModel> response) {
                                    if(response.body() != null) {
                                        Toast.makeText(activity, response.body().getAreatype(), Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        Toast.makeText(activity, "The Response body is empty", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(@NonNull Call<PopulationModel> call, Throwable t) {
                                    Toast.makeText(activity, "The Response has failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onTabUnselected(TabLayout.Tab tab) {
                        }

                        @Override
                        public void onTabReselected(TabLayout.Tab tab) {
                        }
                    });
                }
            }
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d("LocationChangeActivity", exception.getLocalizedMessage());
            MainActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}