package com.example.krisma.architek;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GroundOverlay groundOverlay;
    private JSONArray coordinatesHash;
    private LatLng lastPosition = new LatLng(0,0);
    private int currentOverlayId;
    private int currentFloorNumbers;
    private int currentFloor;
    private LatLng currentBuildingLocation;
    private JSONArray currentBuildingMaps;
    private Map<LatLng, GroundOverlay> overlaysHash;

    private void resetOverlay() {

        groundOverlay.remove();
        currentFloor = 0;
        currentBuildingMaps = null;
        currentFloorNumbers = 0;
        currentBuildingLocation = null;
        Button upButton = (Button) findViewById(R.id.button);
        Button downButton = (Button) findViewById(R.id.button2);
        upButton.setVisibility(View.INVISIBLE);
        downButton.setVisibility(View.INVISIBLE);

    }
    private LatLng getLagLngFromLngLat(JSONArray coordinate) {
        try {
            return new LatLng(coordinate.getDouble(1), coordinate.getDouble(0));
        } catch (JSONException e) {
            e.printStackTrace();
        };
        return null;
    };
    private LatLngBounds getBoundsFromJSONObject(JSONObject twoCoordinates) {
        LatLng sw = null;
        LatLng ne = null;
        try {
            JSONArray coordinatesw = twoCoordinates.getJSONArray("coordinatesw");
            JSONArray coordinatene = twoCoordinates.getJSONArray("coordinatene");
            sw = new LatLng(coordinatesw.getDouble(0), coordinatesw.getDouble(1));
            ne = new LatLng(coordinatene.getDouble(0), coordinatene.getDouble(1));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new LatLngBounds(sw, ne);
    };
    //buildings.getJSONObject(0).getJSONObject("twoCoordinates").getJSONArray("coordinatesw")

    private void drawOverlaysNearby() {
        Log.i("Called: drawOverlaysNearby", "Called: drawOverlaysNearby");
        for (int i=0; i<coordinatesHash.length(); i++) {
            JSONObject thisBuilding = null;
            try {
                thisBuilding = coordinatesHash.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            new LongOperation(thisBuilding).execute();
        };
    };

    private void setFocusBuilding(LatLng location) {
        Log.i("Called: setFocusBuilding", "Called: setFocusBuilding");
        URL url;
        final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String token = getPrefs.getString("token", "");
        InputStream is = null;
        HttpURLConnection con = null;
        String param = String.valueOf(location.latitude) + "," + String.valueOf(location.longitude);
        try {
            url = new URL("https://architek-server.herokuapp.com/getbuildingmaps?" +
                    "location=" + param);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.connect();
            int responseCode = con.getResponseCode();
            is = con.getInputStream();
            JSONObject jObject = null;
            try {
                jObject = new JSONObject(convertStreamToString(is));
            } catch (JSONException f) {
                f.printStackTrace();
            }
            ;
            Log.d("test", jObject.toString());
            currentBuildingMaps = jObject.getJSONArray("floors");
            currentFloor = 0;
            currentFloorNumbers = currentBuildingMaps.length();
            Button upButton = (Button) findViewById(R.id.button);
            Button downButton = (Button) findViewById(R.id.button2);
            upButton.setVisibility(View.VISIBLE);
            downButton.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                con.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Boolean detectOverlay(LatLng currentLocation) {
        Log.i("Called: detectOverlay", "Called: detectOverlay");
        for (int i=0; i<coordinatesHash.length(); i++) {
            try {
                LatLngBounds bounds = getBoundsFromJSONObject(coordinatesHash.getJSONObject(i)
                        .getJSONObject("twoCoordinates"));
                Log.d("coordinates", bounds.toString());
                if (bounds.contains(currentLocation)) {
                    currentBuildingLocation = getLagLngFromLngLat(coordinatesHash.getJSONObject(i).getJSONArray("location"));
                    Log.d("location", currentBuildingLocation.toString());
                    setFocusBuilding(currentBuildingLocation);
                    return true;
                };
            } catch (JSONException e) {
                e.printStackTrace();
            }
        };
        return false;
    };

    private int getDrawableIdByName(String name) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(name, "drawable", packageName);
        return resId;
    }
    private boolean lastMoveFarEnough(LatLng currentLocation) {
        return Math.sqrt((currentLocation.latitude - lastPosition.latitude) *
                (currentLocation.latitude - lastPosition.latitude) +
                (currentLocation.longitude - lastPosition.longitude) *
                (currentLocation.longitude - lastPosition.longitude)) > 0.0005;
    };
    private boolean lastMoveFarFarEnough(LatLng currentLocation) {
        return Math.sqrt((currentLocation.latitude - lastPosition.latitude) *
                (currentLocation.latitude - lastPosition.latitude) +
                (currentLocation.longitude - lastPosition.longitude) *
                        (currentLocation.longitude - lastPosition.longitude)) > 0.005;
    };
    private void updateCoordinatesHash(LatLng target) {
        Log.i("Called: updateCoordinateHash", "Called: updateCoordinateHash");
        URL url;
        HttpURLConnection con = null;
        final SharedPreferences getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String token = getPrefs.getString("token", "");
        String coordinateParam = Double.toString(target.latitude) + "," + Double.toString(target.longitude);
        InputStream is = null;

        try {
            url = new URL("https://architek-server.herokuapp.com/getbuildingsnearby?" +
                    "coordinate=" + coordinateParam + "&" + "token=" + token);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.connect();
            int responseCode = con.getResponseCode();
            is = con.getInputStream();
            JSONObject jObject = null;
            try {
                jObject = new JSONObject(convertStreamToString(is));
            } catch (JSONException f) {
                f.printStackTrace();
            }
            ;
            JSONArray buildings = jObject.getJSONArray("buildings");
            coordinatesHash = buildings;
//            Log.d("building", buildings.getJSONObject(0).getJSONObject("twoCoordinates").getJSONArray("coordinatesw").toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                con.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void updateOverlaysHash() {
        for (int i=0; i<coordinatesHash.length(); i++) {
            
        };
    };

    public String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "/n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        Button upButton = (Button) findViewById(R.id.button);
        Button downButton = (Button) findViewById(R.id.button2);
        Button ucbButton = (Button) findViewById(R.id.button3);
        ucbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraPosition position = new CameraPosition.Builder().target(new LatLng(37.871223, -122.259060))
                        .zoom(18f)
                        .bearing(0)
                        .tilt(0)
                        .build();
//
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
            }
        });
        upButton.setVisibility(View.INVISIBLE);
        downButton.setVisibility(View.INVISIBLE);

//
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFloor + 1 == currentFloorNumbers) {
                    return;
                }
                currentFloor += 1;
                URL url = null;
                try {
                    Log.d("asdasd",currentBuildingMaps.toString());
                    url = new URL(currentBuildingMaps.getJSONObject(currentFloor).getString("map"));
                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    groundOverlay.setImage(BitmapDescriptorFactory.fromBitmap(bmp));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ;
            }

            ;
        });

        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFloor == 0) {
                    return;
                }
                ;
                currentFloor -= 1;
                URL url = null;
                try {
                    url = new URL(currentBuildingMaps.getJSONObject(currentFloor).getString("map"));
                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    groundOverlay.setImage(BitmapDescriptorFactory.fromBitmap(bmp));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ;
            };
        });

    };

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);

        mMap.setOnCameraChangeListener(cameraListener);

//        LatLng NEWARK = new LatLng(37.871305, -122.259165);
//        GroundOverlayOptions newarkMap = new GroundOverlayOptions()
//                .image(BitmapDescriptorFactory.fromResource(R.drawable.wheeler1))
//                .position(NEWARK, 65f, 58f);
//        groundOverlay = mMap.addGroundOverlay(newarkMap);
//        groundOverlay.setBearing(-16);
//        BitmapDescriptor overlayBitmap = BitmapDescriptorFactory.fromResource(R.drawable.saintjean1);
//        currentOverlayId = R.drawable.saintjean1;
//        LatLng Saint = new LatLng(43.708827, 7.288305);
//        GroundOverlayOptions saintMap = new GroundOverlayOptions()
//                .image(BitmapDescriptorFactory.fromResource(currentOverlayId))
//                .position(Saint, 90f, 75f);
//        groundOverlay = mMap.addGroundOverlay(saintMap);
//        groundOverlay.setBearing(90);



    }
//
//    private Location currentLocation;
//    private boolean setUp;
//    private LatLng buildingX = new LatLng(37.870979, -122.259416);
//    private LatLng buildingY = new LatLng(37.871640, -122.258892);
//    private LatLngBounds wheelerBound = new LatLngBounds(buildingX, buildingY);
//    private LatLng buildingA = new LatLng(43.708451, 7.287812);
//    private LatLng buildingB = new LatLng(43.709096, 7.288738);
//    private LatLngBounds saintjeanBound = new LatLngBounds(buildingA, buildingB);
//    private int numberOfFloors = 2;
//    int currentFloor = 0;


    private GoogleMap.OnCameraChangeListener cameraListener = new GoogleMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            if (lastMoveFarFarEnough(cameraPosition.target)) {
                if (groundOverlay != null) {
                    resetOverlay();
                };

                updateCoordinatesHash(cameraPosition.target);
                drawOverlaysNearby();

            };
            if (lastMoveFarEnough(cameraPosition.target)) {
                detectOverlay(cameraPosition.target);
            };
//            if (detectOverlay(cameraPosition.target)) {
//                Button upButton = (Button) findViewById(R.id.button);
//                Button downButton = (Button) findViewById(R.id.button2);
//                upButton.setVisibility(View.INVISIBLE);
//                downButton.setVisibility(View.INVISIBLE);
//            }
        }
    };

    public int getCurrentOverlayId() {
        return currentOverlayId;
    }

    private class LongOperation extends AsyncTask<String, Void, Bitmap> {

        private final JSONObject thisBuilding;

        public LongOperation(JSONObject thisBuilding){
            this.thisBuilding = thisBuilding;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            URL url = null;
            Bitmap bmp = null;
            try {
                url = new URL(thisBuilding.getString("defaultfloor"));
                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("test", url.toString());

            return bmp;

        }

        @Override
        protected void onPostExecute(Bitmap result) {
            try {
                LatLng anchor = new LatLng((Double)thisBuilding.getJSONArray("location").get(1),
                        (Double)thisBuilding.getJSONArray("location").get(0));
                Float height = new Float(thisBuilding.getDouble("height"));
                Float width = new Float(thisBuilding.getDouble("width"));
                Float bearing = new Float(thisBuilding.getDouble("bearing"));
                Log.d("test", bearing.toString());
                GroundOverlayOptions overlayOptions = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(result))
                        .position(anchor, width, height);
                groundOverlay = mMap.addGroundOverlay(overlayOptions);
                groundOverlay.setBearing(bearing);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}