package com.example.pyojihye.smart2gps;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

import static com.example.pyojihye.smart2gps.Const.ConnectionTrue;
import static com.example.pyojihye.smart2gps.Const.DEFAULT_ZOOM_LEVEL;
import static com.example.pyojihye.smart2gps.Const.Dronelocation;
import static com.example.pyojihye.smart2gps.Const.IP;
import static com.example.pyojihye.smart2gps.Const.MY_PERMISSIONS_REQUEST_LOCATION;
import static com.example.pyojihye.smart2gps.Const.PORT;
import static com.example.pyojihye.smart2gps.Const.PROTO_DVTYPE_KEY;
import static com.example.pyojihye.smart2gps.Const.PROTO_MSG_TYPE_KEY;
import static com.example.pyojihye.smart2gps.Const.PROTO_DVTYPE;
import static com.example.pyojihye.smart2gps.Const.PROTO_MSGTYPE;
import static com.example.pyojihye.smart2gps.Const.bufferedReader;
import static com.example.pyojihye.smart2gps.Const.client;
import static com.example.pyojihye.smart2gps.Const.first;
import static com.example.pyojihye.smart2gps.Const.last;
import static com.example.pyojihye.smart2gps.Const.location;
import static com.example.pyojihye.smart2gps.Const.marker;
import static com.example.pyojihye.smart2gps.Const.printWriter;
import static com.example.pyojihye.smart2gps.Const.start;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Button buttonFlight;
    Button buttonDrone;
    Button buttonStart;

    int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        buttonFlight = (Button) findViewById(R.id.buttonFlight);
        buttonDrone = (Button) findViewById(R.id.buttonDrone);
        buttonStart = (Button) findViewById(R.id.buttonStart);

        ConnectionTrue = false;
        first = false;
        last = false;
        start = false;
        i = 0;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ChatOperator chatOperator = new ChatOperator();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            chatOperator.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        } else {
            chatOperator.execute();
        }

        buttonDrone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (start) {
                    setGpsCurrent(marker.getPosition().latitude, marker.getPosition().longitude);
                } else {
                    Toast.makeText(MapsActivity.this, R.string.unknown_drone, Toast.LENGTH_LONG).show();
                }
            }
        });

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = "";
                if (((Button) v).getText().toString().equals("Start")) {
                    if (ConnectionTrue) {
                        ChatOperator chatOperator = new ChatOperator();
                        data = dataSet("116", first);
                        chatOperator.MessageSend(data);

                        buttonStart.setText("Landing");
                        buttonFlight.setEnabled(true);
                    }
                } else {
                    if (ConnectionTrue) {
                        ChatOperator chatOperator = new ChatOperator();
                        data = dataSet("32", first);
                        chatOperator.MessageSend(data);

                        buttonStart.setText("Start");
                        buttonFlight.setEnabled(false);
                    }
                }
            }
        });
    }

    private void setGpsCurrent(double latitude, double longitude) {
        LatLng latLng = new LatLng(latitude, longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setOnMapLongClickListener(this);
            }
        } else {
            buildGoogleApiClient();
            mMap.setOnMapLongClickListener(this);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {

                }
                return;
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng point) {
        mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(point.toString())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        location.add(i, point.latitude + "/" + point.longitude);
        i++;

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m) {
                for (int i = 0; i < location.size(); i++) {
                    String markerLocation = m.getPosition().latitude + "/" + m.getPosition().longitude;
                    if (markerLocation.equals(location.get(i))) {
                        location.set(i, "");
                    }
                }
                if (!m.equals(marker)) {
                    m.remove();
                }
                return true;
            }
        });
    }

    private class Sender extends AsyncTask<String, String, Void> {
        private String message;

        @Override
        protected Void doInBackground(String... params) {
            message = params[0];
            printWriter.write(message + "\n");
            printWriter.flush();
            return null;
        }
    }

    private class Receiver extends AsyncTask<Void, Void, Void> {
        private String message;

        @Override
        protected Void doInBackground(Void... params) {
            while (true) {
                try {
                    if (bufferedReader.ready()) {
                        message = bufferedReader.readLine();
                        publishProgress(null);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (message != null) {
                StringTokenizer tokens = new StringTokenizer(message);

                String MSGTYPE = tokens.nextToken("%%");

                if (MSGTYPE.equals("MSGTYPE=GPS")) {
                    String DATA = tokens.nextToken("%%");
                    String latitude = DATA.substring((DATA.indexOf("=")) + 1, DATA.indexOf("/"));
                    String longitude = DATA.substring((DATA.indexOf("/")) + 1);
                    Dronelocation = new LatLng(Float.parseFloat(latitude), Float.parseFloat(longitude));

                    if (!start) {
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(Dronelocation)
                                .title(location.toString())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone));
                        marker = mMap.addMarker(markerOptions);
                        start = true;
                    } else {
                        marker.setPosition(Dronelocation);
                    }
                }
            }
        }
    }

    private class ChatOperator extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                client = new Socket(IP, PORT);
//                Log.d("Log","IP : "+IP+"\nPORT:"+PORT);

                if (client != null) {
                    ConnectionTrue = true;
                    printWriter = new PrintWriter(client.getOutputStream(), true);
                    InputStreamReader inputStreamReader = new InputStreamReader((client.getInputStream()));
                    bufferedReader = new BufferedReader(inputStreamReader);
                    String text = protocolSet("", first);
                    MessageSend(text);
                    first = true;
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.snack_bar_server_port), Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), getString(R.string.snack_bar_server_connect), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            return null;
        }

        private void MessageSend(final String text) {
            final Sender messageSender = new Sender();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                messageSender.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, text);
            } else {
                messageSender.execute(text);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            buttonFlight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ConnectionTrue) {
                        if (location.size() > 0) {
                            String gps = "";
                            for (int i = 0; i < location.size(); i++) {
                                if (location.get(i) != "") {
                                    gps += location.get(i) + "&&";
                                }
                            }
                            String text = protocolSet(gps.substring(0, gps.length() - 2), first);
                            MessageSend(text);
                            location.clear();
                            i = 0;
                            mMap.clear();

                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(Dronelocation)
                                    .title(location.toString())
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.drone));
                            marker = mMap.addMarker(markerOptions);
                        } else {
                            Toast.makeText(MapsActivity.this, R.string.snack_bar_no_gps, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MapsActivity.this, R.string.snack_bar_server_connect, Toast.LENGTH_LONG).show();
                    }
                }
            });

            if (client != null) {
                Receiver receiver = new Receiver();
                receiver.execute();
            }
        }
    }

    private String protocolSet(String str, boolean first) {
        String msg = "";
        if (!first) { //첫 연결
            msg = PROTO_DVTYPE_KEY + "=" + PROTO_DVTYPE.PHONE.ordinal() + "%%" + PROTO_MSG_TYPE_KEY + "=" + PROTO_MSGTYPE.HELLO.ordinal();
        } else {
            if (last) { //연결 종료
                msg = PROTO_DVTYPE_KEY + "=" + PROTO_DVTYPE.PHONE.ordinal() + "%%" + PROTO_MSG_TYPE_KEY + "=" + PROTO_MSGTYPE.CMD.ordinal() + "%%DATA=" + str;
            } else { //목적지 위치 알려주기
                msg = PROTO_DVTYPE_KEY + "=" + PROTO_DVTYPE.PHONE.ordinal() + "%%" + PROTO_MSG_TYPE_KEY + "=" + PROTO_MSGTYPE.PATH.ordinal() + "%%DATA=" + str;
            }
        }
        return msg;
    }

    private String dataSet(String str, boolean first) {
        String msg = "";
        if (first) {
            msg = PROTO_DVTYPE_KEY + "=" + "0" + "%%" + PROTO_MSG_TYPE_KEY + "=" + "0" + "%%DATA=" + str;
        }
        return msg;
    }
}