package com.example.tmapwklim;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPOIItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

// 새로운 리스트에서 사용할 위도, 경도 리스트 설정에 사용할 클래스
class searchLoc{
    public double lati;
    public double longi;

    searchLoc(double lati, double longi){
        this.lati = lati;
        this.longi = longi;
    }
}

public class MainActivity extends AppCompatActivity {
    LinearLayout linMapView;
    Button btnZoomIn, btnZoomOut,btnSearch,btnMyLocation;
    EditText edtSearch;
    TMapView tMapView;
    TMapData tMapData;

    // 티맵에서 POI 검색 결과를 저장할 리스트
    ArrayList<TMapPOIItem> poiResult;
    LocationManager locationManager;

    ArrayAdapter<String> adapter;

    Bitmap rightButton;
    BitmapFactory.Options options;

    // 현재위치, 목표위치
    TMapPoint tMapPointStart;
    TMapPoint tMapPointEnd;
    // 현재 위도, 경도
    double lati;
    double longi;

    float latit;
    float longit;

    ArrayList<searchLoc> searchLocArrayList;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initInstance();
        setEventListener();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView list = (ListView) findViewById(R.id.listview1);
        list.setAdapter(adapter);

        // 시작했을 때 화면을 내 위치에 갖다 놓기
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1,locationListener);
        }catch (SecurityException e){ }
    }

    // 위젯 변수 및 필요 객체 등의 멤버 변수 초기화 작업
    public void initView(){
        linMapView = findViewById(R.id.linMapView);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        btnMyLocation = findViewById(R.id.btnMyLocation);
        btnSearch = findViewById(R.id.btnSearch);
        edtSearch = findViewById(R.id.edtSearch);

        options = new BitmapFactory.Options();
        options.inSampleSize = 16;
        rightButton = BitmapFactory.decodeResource(getResources(), R.drawable.right_arrow, options);
        listView = findViewById(R.id.listview1);
    }

    // 필요 객체 변수 인스턴스화 작업
    public void initInstance(){
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("app_key");
        linMapView.addView(tMapView); //->linMapView에 tMapView를 추가한다.

        tMapData = new TMapData();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        poiResult = new ArrayList<>();
        searchLocArrayList=new ArrayList<>();
    }

    // 각 버튼 및 객체의 이벤트 리스너 설정 작업
    public void setEventListener(){
        btnZoomIn.setOnClickListener(listener);
        btnZoomOut.setOnClickListener(listener);
        btnSearch.setOnClickListener(listener);
        btnMyLocation.setOnClickListener(listener);
    }

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.btnSearch: // 검색을 눌렀을 경우
                    String strData = edtSearch.getText().toString(); // edtSearch로부터 문자를 받아옴
                    if(!strData.equals("")){
                        searchPOI(strData);
                        adapter.add(edtSearch.getText().toString());
                        adapter.notifyDataSetChanged();
                        searchLocArrayList.add(new searchLoc(tMapView.getLatitude(),tMapView.getLongitude()));
                    } else {
                        Toast.makeText(getApplicationContext(),"검색어를 정확히 입력하세요!", Toast.LENGTH_SHORT);
                    }
                    break;
                case R.id.btnZoomIn:
                    tMapView.MapZoomIn();
                    break;
                case R.id.btnZoomOut:
                    tMapView.MapZoomOut();
                    break;
                case R.id.btnMyLocation:
                    try {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1,locationListener);
                    }catch (SecurityException e){
                        Toast.makeText(getApplicationContext(),"위치가 파악 불가!", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };


    // 통합 검색 메서드
    public void searchPOI(String strData){
        tMapData.findAllPOI(strData, new TMapData.FindAllPOIListenerCallback() {
            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> arrayList) {
                //외부에서 사용하기 위한 검색 결과 복사
                poiResult.addAll(arrayList);

                tMapView.setCenterPoint(arrayList.get(0).getPOIPoint().getLongitude(),
                        arrayList.get(0).getPOIPoint().getLatitude(),true);
                for(int i = 0; i < arrayList.size(); i++) {
                    TMapPOIItem item = arrayList.get(1);
                    Log.d("POI Name: ", item.getPOIName().toString() + ", " +
                            "Address: " + item.getPOIAddress().replace("null","") + ", " +
                            "Point: " + item.getPOIPoint().toString() + "Contents: " + item.getPOIContent());
                    TMapMarkerItem markerItem = new TMapMarkerItem();
                    markerItem.setTMapPoint(item.getPOIPoint());
                    markerItem.setCalloutTitle(item.getPOIName());
                    markerItem.setCalloutSubTitle(item.getPOIAddress());
                    markerItem.setCanShowCallout(true);
                    markerItem.setCalloutRightButtonImage(rightButton);
                    tMapView.addMarkerItem(item.getPOIName(), markerItem);
                    // 마크를 선택하고 바로 안내시작을 누른 경우를 위한 위도, 경도
                    try {
                        latit = (float) searchLocArrayList.get(1).lati;
                        longit = (float) searchLocArrayList.get(1).longi;
                    } catch (Exception e) {
                        latit = (float) arrayList.get(0).getPOIPoint().getLatitude();
                        longit = (float) arrayList.get(0).getPOIPoint().getLongitude();
                    }
                }

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        try {
                            tMapView.setCenterPoint(searchLocArrayList.get(arg2+1).longi, searchLocArrayList.get(arg2+1).lati);

                            // 리스트를 선택하고 바로 안내시작을 누른 경우를 위한 위도, 경도
                            latit = (float) searchLocArrayList.get(arg2+1).lati;
                            longit = (float) searchLocArrayList.get(arg2+1).longi;
                        } catch (Exception e) {
                            tMapView.setCenterPoint(arrayList.get(0).getPOIPoint().getLongitude(),
                                    arrayList.get(0).getPOIPoint().getLatitude());

                            // 리스트를 선택하고 바로 안내시작을 누른 경우를 위한 위도, 경도
                            latit = (float) arrayList.get(0).getPOIPoint().getLatitude();
                            longit = (float) arrayList.get(0).getPOIPoint().getLongitude();
                        }
                    }
                });
            }
        });

        tMapView.setOnCalloutRightButtonClickListener(new TMapView.OnCalloutRightButtonClickCallback() {
            @Override
            public void onCalloutRightButton(TMapMarkerItem tMapMarkerItem) {
                TMapPolyLine polyLine = new TMapPolyLine();
                PathAsync pathAsync = new PathAsync();
                pathAsync.execute(polyLine);
                tMapPointStart = new TMapPoint(lati, longi);
                tMapPointEnd = new TMapPoint(tMapView.getLatitude(), tMapView.getLongitude());

                FindElapsedTimeTask findElapsedTimeTask = new FindElapsedTimeTask(getApplicationContext());
                try {
                    findElapsedTimeTask.execute("1", "app_key",
                            String.valueOf(longi),
                            String.valueOf(lati),
                            String.valueOf(tMapView.getLongitude()),
                            String.valueOf(tMapView.getLatitude()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 위치 수신자 객체 설정
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double lat = location.getLatitude(); // 위도 읽어오기
            double lon = location.getLongitude(); // 경도 읽어오기
            tMapView.setCenterPoint(lon, lat);
            tMapView.setLocationPoint(lon, lat);
            tMapView.setIconVisibility(true);

            lati = location.getLatitude();
            longi =location.getLongitude();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private void setMarker(String name, double lat, double lon){
        TMapMarkerItem markerItem = new TMapMarkerItem();

        TMapPoint tMapPoint = new TMapPoint(lat, lon);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pin_r_b_x);
        markerItem.setIcon(bitmap); //마커 아이콘 지정
        markerItem.setPosition(0.5f,1.0f); //마커의 중심점을 중앙, 하단으로 지정
        markerItem.setTMapPoint(tMapPoint);
        markerItem.setName(name); //마커 타이틀 지정
        markerItem.setCanShowCallout(true);
        markerItem.setCalloutTitle(name);
        tMapView.addMarkerItem("name", markerItem); //지도에 마커 추가
    }

    // PolyLine을 그려주는 클래스 생성 (최적경로)
    class PathAsync extends AsyncTask<TMapPolyLine, Void, TMapPolyLine> {
        @Override
        protected TMapPolyLine doInBackground(TMapPolyLine... tMapPolyLines) {
            TMapPolyLine tMapPolyLine = tMapPolyLines[0];
            try {
                tMapPolyLine = new TMapData().findPathDataWithType(TMapData.TMapPathType.CAR_PATH, tMapPointStart, tMapPointEnd);
                tMapPolyLine.setLineColor(Color.BLUE);
                tMapPolyLine.setLineWidth(1);


            } catch (Exception e) {
                e.printStackTrace();
                Log.e("error", e.getMessage());
            }
            return tMapPolyLine;
        }

        @Override
        protected void onPostExecute(TMapPolyLine tMapPolyLine) {
            super.onPostExecute(tMapPolyLine);
            tMapView.addTMapPolyLine("Line1", tMapPolyLine);
        }
    }

    // 예상 소요 시간을 그리는 클래스 생성
    private class FindElapsedTimeTask extends AsyncTask<String, Void, String> {
        Context context;
        String[] arrParametersName = new String[6];
        String[] arrJsonKeys = new String[3];

        public FindElapsedTimeTask(Context context)
        {
            super();
            this.context = context;
        }

        private String MinuteToSecond(int nSecond)
        {
            String strText = null;
            try
            {
                if( nSecond >= 3600 )
                {

                    int minute = ( nSecond / 3600 );
                    int second = ( nSecond % 3600 / 60 );

                    strText = String.format(("%d시간 %d분"), minute, second);
                }
                else
                {
                    int second = ( nSecond / 60 );
                    strText = String.format(("%d분"), second);
                }
                return strText;
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String time)
        {
            super.onPreExecute();
            String strTime = MinuteToSecond(Integer.parseInt(time));
            Toast.makeText(context, "예상 소요시간 : " + strTime + "", Toast.LENGTH_LONG).show();
        }

        @Override
        protected String doInBackground(String[] args)
        {
            setArrays();
            TMapWebService tMapWebService = new TMapWebService("https://api2.sktelecom.com/tmap/routes");
            tMapWebService.setParameters(arrParametersName, args, 6);
            String totalTime = tMapWebService.connectWebService(arrJsonKeys);
            return totalTime;
        }

        private void setArrays()
        {
            arrParametersName[0] = "version";
            arrParametersName[1] = "appKey";
            arrParametersName[2] = "startX";
            arrParametersName[3] = "startY";
            arrParametersName[4] = "endX";
            arrParametersName[5] = "endY";

            arrJsonKeys[0] = "features";
            arrJsonKeys[1] = "properties";
            arrJsonKeys[2] = "totalTime";
        }
    }

    private class TMapWebService {
        private final String REVERSE_GEOCODING = "https://api2.sktelecom.com/tmap/geo/" + "reversegeocoding";
        private final String ROUTES = "https://api2.sktelecom.com/tmap/routes";

        private String mStrFullURI = "";
        private String mStrURI = "";

        public TMapWebService(String uri)
        {
            this.mStrURI = uri;
        }

        public void setParameters(String[] parametersName, String[] parametersData, int size)
        {
            for( int i = 0; i < size; i++ )
            {
                if( i == 0 )
                {
                    mStrFullURI += mStrURI + "?" + parametersName[i] + "=" + parametersData[i];
                    continue;
                }
                mStrFullURI += "&" + parametersName[i] + "=" + parametersData[i];
            }
        }

        public String connectWebService(String[] jsonKeys)
        {
            try
            {
                URL url = new URL(mStrFullURI);
                HttpURLConnection urlConnection = ( HttpURLConnection ) url.openConnection();
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                JSONObject json = new JSONObject(getStringFromInputStream(in));

                String totalAddress = parseJSON(json, jsonKeys);

                return totalAddress;
            }
            catch( MalformedURLException e )
            {
                e.printStackTrace();
            }
            catch( JSONException e )
            {
                e.printStackTrace();
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
            return null;
        }

        private String getStringFromInputStream(InputStream inputStream)
        {
            BufferedReader bufferedReader = null;
            StringBuilder stringBuilder = new StringBuilder();
            String line = "";

            try
            {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                while( (line = bufferedReader.readLine()) != null )
                {
                    stringBuilder.append(line);
                }
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
            finally
            {
                if( bufferedReader != null )
                {
                    try
                    {
                        bufferedReader.close();
                    }
                    catch( IOException e )
                    {
                        e.printStackTrace();
                    }
                }
            }
            return stringBuilder.toString();
        }

        private String parseJSON(JSONObject jsonObject, String[] arrKey) throws JSONException
        {
            if( REVERSE_GEOCODING.equals(mStrURI) )
            {
                JSONObject jsonAddress = jsonObject.getJSONObject(arrKey[0]);

                String arrAddress[] = new String[arrKey.length - 1];
                String strAddress = "";

                for(int i = 1; i < arrKey.length; i++)
                {
                    arrAddress[i - 1] = jsonAddress.getString(arrKey[i]);
                    strAddress += arrAddress[i - 1] + " ";
                }
                return strAddress;
            }
            else if( ROUTES.equals(mStrURI) )
            {
                JSONArray jsonArray = jsonObject.getJSONArray(arrKey[0]);
                JSONObject jsonFeatures = jsonArray.getJSONObject(0);
                JSONObject jsonProperties = jsonFeatures.getJSONObject(arrKey[1]);

                String strTime = jsonProperties.getString(arrKey[2]);

                return strTime;
            }
            return null;
        }
    }
}