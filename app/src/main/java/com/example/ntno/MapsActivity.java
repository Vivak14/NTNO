package com.example.ntno;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.animation.TypeEvaluator;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;


import android.os.Handler;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.MapsInitializer.Renderer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.ntno.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnMapsSdkInitializedCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private Handler handler;
    private ActivityMapsBinding binding;
    private SupportMapFragment mapFragment;
    private Polyline polyline;
    private WebSocket ws;
    private OkHttpClient client;
    Map<String, Marker> markers = new HashMap<String, Marker>();
    private String channelId = "my_channel_id";
    CharSequence channelName = "Відслідковування";
    NotificationManager notificationManager;
    int importance = NotificationManager.IMPORTANCE_DEFAULT;
    private Notification notif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        MapsInitializer.initialize(getApplicationContext(), Renderer.LATEST, this);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        handler = new Handler();
        client = new OkHttpClient();

        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 1000);
            }

        };
        if(!isNetworkConnected()) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Відсутній доступ до інтернету", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
        notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(channelId,             channelName, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationChannel.setVibrationPattern(new long[]{1000, 2000});
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notif = new Notification.Builder(this)
                .setContentTitle("Запущено")
                .setContentText("Очікуємо підключення!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setChannelId(channelId)
                .build();

        notificationManager.notify(1, notif);

        runnableCode.run();
    }
    @Override
    protected void onStop() {
        super.onStop();
        //ws.cancel();
        ws.close(1000, null);
        Log.d("pull", "onstop");

    }

    @Override
    protected void onStart() {
        super.onStart();
        reconnect();
        Log.d("pull", "onstart");

    }
    private void reconnect() {
        try {
            if (ws != null) {
                ws.close(1000, "reconnect");
                Log.d("pull", "closed");
            }

            Request request = new Request.Builder().url("wss://wss.ntno.de").build();
            EchoWebSocketListener listener = new EchoWebSocketListener(mMap, this);
            ws = client.newWebSocket(request, listener);

        } catch (Exception e) {
            Log.d("pull", e.getMessage());

        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        /*mMap = googleMap;
        googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.style_json));
        LatLng sydney = new LatLng(40.7534621,25.3085896);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50.749516, 25.335173), 12.0f));*/
        mMap = googleMap;
        googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.style_json));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50.749516, 25.335173), 12.0f));
        mMap.setOnMarkerClickListener(this);
       // mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        /*for (int i = 0; i < 500; i += 50) {
            PolylineOptions circle = new PolylineOptions().width(5).color(Color.HSVToColor(new float[]{(18.0f), 1, 1})).geodesic(true);
            double lat = 50.725796;
            double lon = 25.308276;
            for (int x = 0; x <= 360; x++) {
                double radian = x * 0.0174532925;
                circle.add(new LatLng(lat + (Math.sin(radian) * i * 0.000001), lon + (Math.cos(radian) * i * 0.000001)));

                if (x == 0) {
                    int w = 70, h = 70;

                    Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                    Bitmap bmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap

                    Canvas canvas = new Canvas(bmp);
                    Paint p = new Paint();
                    p.setColor(Color.parseColor("#43C074"));

                    RectF oval = new RectF(5, 5, 65, 65);
                    Path path = new Path();
                    float sweep = 50;
                    p.setAntiAlias(true);
                    p.setColor(Color.BLUE);
                    p.setStrokeWidth(8);
                    p.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, p);
                    p.setStyle(Paint.Style.FILL);

                    p.setColor(Color.BLACK);
                    p.setTextSize(25);
                    p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    String title = String.valueOf(i);
                    Rect textBounds = new Rect();
                    p.getTextBounds(title, 0, title.length(), textBounds);
                    canvas.drawText(title, w / 2.0f - textBounds.exactCenterX(), h / 2.0f - textBounds.exactCenterY(), p);

                    Marker m =  mMap.addMarker(new MarkerOptions().position(new LatLng(lat + (Math.sin(radian) * i * 0.000001), lon + (Math.cos(radian) * i * 0.000001))).anchor(0.5f, 0.5f));
                    m.setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
                }
            }
            mMap.addPolyline(circle);
        }*/
    }
    @Override
    public void onMapsSdkInitialized(MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("MapsDemo", "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d("MapsDemo", "The legacy version of the renderer is used.");
                break;
        }
    }
    @Override
    public boolean onMarkerClick(final Marker marker) {
        ws.send("{\"event\":\"historyRequest\",\"data\":{\"bortId\":" + getKeyByValue(markers, marker) + ",\"lapse\":10000}}");
        //Log.d("pull", getKeyByValue(markers, marker));
        return false;
    }
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
    private double[] nextDoor(double[] latLon, int dir, int speed) {
        double r = 6371000.0;
        double distance = speed / 3.4; //3.6

        double sigma = distance / r;
        double theta = toRad(dir);

        double phi1 = toRad(latLon[0]);
        double lambda1 = toRad(latLon[1]);

        double sinphi1 = Math.sin(phi1), cosphi1 = Math.cos(phi1);
        double sinsigma = Math.sin(sigma), cossigma = Math.cos(sigma);
        double sintheta = Math.sin(theta), costheta = Math.cos(theta);

        double sinphi2 = sinphi1 * cossigma + cosphi1 * sinsigma * costheta;
        double phi2 = Math.asin(sinphi2);
        double y = sintheta * sinsigma * cosphi1;
        double x = cossigma * sinphi1 * sinphi2;
        double lambda2 = lambda1 + Math.atan2(y, x);

        //Log.d("MapsDemo", "From "+ dir +" to " + latLon[2]);
        if (Math.abs((int)(dir-latLon[2]))>180){
            if(dir<180) dir = dir + 360; else dir = dir - 360;
        }
        return new double[]{toDeg(phi2), (toDeg(lambda2) + 540.0) % 360.0 - 180.0, dir};
    }
    private double toRad(double deg) {
        return deg * Math.PI / 180.0;
    }
    private double toDeg(double rad) {
        return rad * 180.0 / Math.PI;
    }
    private final class EchoWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;
        private int old_dir=0;
        private FragmentActivity toi;
        private GoogleMap maps;
        public EchoWebSocketListener(GoogleMap mapa, FragmentActivity f){
            this.maps = mapa;
            this.toi = f;
        }
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            output("OnOpen : ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //info.setText("Сокет є.");
                    Log.d("MapsDemo", "Socket start.");
                    notif = new Notification.Builder(toi)
                            .setContentTitle("Запущено")
                            .setContentText("Сокет підключено!")
                            .setSmallIcon(R.drawable.ic_stat_name)
                            .setChannelId(channelId)
                            .setColor(Color.GREEN)
                            .build();

                    notificationManager.notify(1, notif);
                }
            });
            webSocket.send("{\"event\":\"webClientInit\",\"data\":\"hello\"}");
        }
        private Bitmap arror(){
           // rotatee = (int)(System.currentTimeMillis() / 100)%360;

            Bitmap bitmapOrg =  Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.arror),
                    (int)(BitmapFactory.decodeResource(getResources(),R.drawable.arror).getWidth()*0.15),
                    (int)(BitmapFactory.decodeResource(getResources(),R.drawable.arror).getHeight()*0.15), false);
            /*Matrix matrix = new Matrix();
            matrix.postRotate(rotatee);
            Bitmap bmp = Bitmap.createBitmap(bitmapOrg, 0, 0, bitmapOrg.getWidth(), bitmapOrg.getHeight(), matrix, true);*/
            return bitmapOrg;
        }

        @Override
        public void onMessage(WebSocket webSocket, final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Log.d("MapsDemo", "Socket message.");
                    try {
                        JSONObject jObject = new JSONObject(text);
                        String event = jObject.getString("event");

                        switch (event) {
                            case "gps": {
                                JSONArray data = jObject.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject mes = data.getJSONObject(i);
                                    LatLng pos = new LatLng(mes.getDouble("lat"), mes.getDouble("lon"));
                                    Double angl = mes.getDouble("dir");
                                    Bitmap smallMarker;
                                    float vrot = 0;
                                    if(mes.getInt("speed")>0) {
                                        smallMarker = arror(); // rotate(angl.intValue());
                                        vrot = (float) mes.getDouble("dir");

                                    }else{
                                        smallMarker = ((BitmapDrawable)getResources().getDrawable(R.drawable.point)).getBitmap();
                                    }
                                    if (!markers.containsKey(mes.getString("bortID"))) {
                                        markers.put(mes.getString("bortID"), mMap.addMarker(new MarkerOptions().rotation(vrot).title(mes.getString("bortID")).position(pos).icon(BitmapDescriptorFactory.fromBitmap(smallMarker)).anchor(0.5f, 0.5f)));
                                    }
                                    final Marker m = markers.get(mes.getString("bortID"));
                                    //m.setRotation(angl.floatValue());
                                    m.setIcon(BitmapDescriptorFactory.fromBitmap(smallMarker));
                                    m.setTitle(mes.getString("speed"));
                                    if (m.getTag() != null) {
                                        ValueAnimator animator = (ValueAnimator) m.getTag();
                                        animator.cancel();
                                    }

                                    double[] thisDoor = {mes.getDouble("lat"), mes.getDouble("lon"), old_dir};
                                    final ValueAnimator latLngAnimator = ValueAnimator.ofObject(new DoubleArrayEvaluator(), thisDoor, nextDoor(thisDoor, mes.getInt("dir"), mes.getInt("speed")));
                                    old_dir = mes.getInt("dir");
                                    latLngAnimator.setDuration(800);
                                    latLngAnimator.setInterpolator(new LinearInterpolator());
                                    latLngAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            double[] animatedValue = (double[]) animation.getAnimatedValue();
                                            m.setPosition(new LatLng(animatedValue[0], animatedValue[1]));
                                            int dirs = (int)animatedValue[2];
                                            m.setRotation(dirs);
                                        }
                                    });
                                    latLngAnimator.start();
                                    m.setTag(latLngAnimator);
                                    m.setTitle(mes.getString("bortID") + " Швидкість: " + mes.getString("speed") + "км/год");

                                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                                    cal.setTimeInMillis(mes.getLong("time") * 1000);
                                    String date = DateFormat.format("HH:mm:ss", cal).toString();
                                    m.setSnippet(date);

                                    //------Тут нада добавити швидкість на значок--------------
                                    int w = 70, h = 70;

                                    Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                                    Bitmap bmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap

                                    Canvas canvas = new Canvas(bmp);
                                    Paint p = new Paint();
                                    Path path = new Path();

                                    p.setAntiAlias(true);
                                    p.setColor(Color.BLUE);
                                    p.setStrokeWidth(8);
                                    p.setStyle(Paint.Style.STROKE);
                                    canvas.drawPath(path, p);
                                    p.setStyle(Paint.Style.FILL);
                                    p.setColor(Color.WHITE);
                                    p.setTextSize(50);
                                    p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                                    String title = mes.getString("speed");
                                    Rect textBounds = new Rect();
                                    p.getTextBounds(title, 0, title.length(), textBounds);
                                    canvas.drawText(title, w / 2.0f - textBounds.exactCenterX(), h / 2.0f - textBounds.exactCenterY(), p);
                                    m.setIcon(BitmapDescriptorFactory.fromBitmap(overlay(smallMarker,bmp)));
                                    //Log.d("MapsDemo", m.getTitle());
                                    //m.setIcon(BitmapDescriptorFactory.fromBitmap(bmp));

                                    //m.hideInfoWindow();
                                    if (m.isInfoWindowShown()) {
                                        m.hideInfoWindow();
                                        m.showInfoWindow();
                                    }

                                    String name = "P";//data.getJSONObject(i).getString("name");
                                    //output("Receiving : " + name);
                                }
                                break;
                            }
                            case "history": {
                                JSONArray data = jObject.getJSONArray("data");
                                String debug = data.toString();
                                //output("history" + debug.substring(debug.length() - 500, debug.length() - 1));
                                Random rand = new Random();
                                int temp = 1;
                                for (int i = 1; i < data.length(); i++) {
                                    if ((data.getJSONObject(i).getInt("unixtime") - data.getJSONObject(i - 1).getInt("unixtime")) > 600) {
                                        temp++;
                                    }
                                }

                                float color = 0;
                                PolylineOptions options = new PolylineOptions().width(5).color(Color.HSVToColor(new float[]{(360.f / temp) * color, 1, 1})).geodesic(true);

                                for (int i = 1; i < data.length(); i++) {
                                    if ((data.getJSONObject(i).getInt("unixtime") - data.getJSONObject(i - 1).getInt("unixtime")) < 600) {
                                        options.add(new LatLng(data.getJSONObject(i).getDouble("lat"), data.getJSONObject(i).getDouble("lon")));
                                    } else {
                                        color++;
                                        output("catch" + color);

                                        polyline = mMap.addPolyline(options);
                                        options = null;
                                        options = new PolylineOptions().width(5).color(Color.HSVToColor(new float[]{(360.f / temp) * color, 1, 1})).geodesic(true);
                                    }
                                }
                                polyline = mMap.addPolyline(options);

                                /*if (polyline != null) {
                                    polyline.remove();
                                }*/
                                break;
                            }
                        }

                    } catch (Exception e) {
                        output("error : " + e.getMessage());
                    }
                }
            });
        }
        private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
            Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
            Canvas canvas = new Canvas(bmOverlay);
            canvas.drawBitmap(bmp1, new Matrix(), null);
            canvas.drawBitmap(bmp2, bmp1.getWidth()/2-bmp2.getWidth()/2,bmp1.getHeight()/2-bmp2.getHeight()/2, null);
            return bmOverlay;
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Receiving bytes : " + bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closing : " + code + " / " + reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, final Throwable t, Response response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //info.setText("Підкючення перервано " + t.toString());
                    notif = new Notification.Builder(toi)
                            .setContentTitle("Запущено")
                            .setContentText("Втрачено підключення до сокету!")
                            .setSmallIcon(R.drawable.ic_stat_name)
                            .setChannelId(channelId)
                            .build();

                    notificationManager.notify(1, notif);
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            reconnect();
                        }
                    }, 1000);
                }
            });
            output("Error : " + t.toString());
        }

        private void output(String s) {
            Log.d("pull", s);
        }
    }
    public class DoubleArrayEvaluator implements TypeEvaluator<double[]> {

        private double[] mArray;

        public DoubleArrayEvaluator() {
        }

        public DoubleArrayEvaluator(double[] reuseArray) {
            mArray = reuseArray;
        }

        @Override
        public double[] evaluate(float fraction, double[] startValue, double[] endValue) {
            double[] array = mArray;
            if (array == null) {
                array = new double[startValue.length];
            }

            for (int i = 0; i < array.length; i++) {
                double start = startValue[i];
                double end = endValue[i];
                array[i] = start + (fraction * (end - start));
            }
            return array;
        }
    }
}