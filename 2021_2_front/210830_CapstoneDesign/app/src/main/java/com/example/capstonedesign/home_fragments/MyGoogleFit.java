package com.example.capstonedesign.home_fragments;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.capstonedesign.retrofit.DDTSRequest;
import com.example.capstonedesign.retrofit.DDTSResponse;
import com.example.capstonedesign.retrofit.RetrofitClient;
import com.example.capstonedesign.retrofit.initMyApi;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** 현 Class에서 가능한 작업 정리.
 *  1. 얻고자 하는 데이터 타입에 따른 client들 등록.
 *  2. Listener의 등록 & 해제.
 *  3. 원하는 데이터의 return 값.
 * **/

public class MyGoogleFit {
    private static MyGoogleFit myGoogleFit = null;
    private static Context appContext = null;
    private static FitnessOptions fitnessOptions = null;
    private static String TAG1 = "DataToServer";

    // TAGs
    private static final String _TAG_ = "MyGoogleFit";

    // DATA_TYPE
    public static final int TYPE_STEP       = 0b00001;
    public static final int TYPE_CALORIES   = 0b00010;
    public static final int TYPE_DISTANCE   = 0b00100;
    public static final int TYPE_MOVE_MIN   = 0b01000;

    // Masks
    private static final int[] TYPE_MASK = {
            TYPE_STEP,
            TYPE_CALORIES,
            TYPE_DISTANCE,
            TYPE_MOVE_MIN
    };

    // DataTypes
    private static final DataType[] DATA_TYPE = {
            DataType.TYPE_STEP_COUNT_DELTA,
            DataType.TYPE_CALORIES_EXPENDED,
            DataType.TYPE_DISTANCE_DELTA,
            DataType.TYPE_MOVE_MINUTES
    };

    /** ListnerManager cover area of each index
     * index[0] : TYPE_STEP_COUNT_DELTA     ( format: int    )
     * index[1] : TYPE_CALORIES_EXPENDED    ( format: float  )
     * index[2] : TYPE_DISTANCE_DELTA       ( format: float  )
     * index[3] : TYPE_MOVE_MINUTES         ( format: int    )
     * **/
    private static ArrayList<OnDataPointListener> ListenerManager = new ArrayList<OnDataPointListener>();

    public static MyGoogleFit getInstance(){
        Log.d("MyGoogleFit","Getting Instance" );
        if(myGoogleFit == null){
            myGoogleFit = new MyGoogleFit();
        }
        return myGoogleFit;
    }

    public MyGoogleFit setAppContext(Context appContext){
        MyGoogleFit.appContext = appContext;
        return myGoogleFit;
    }
    public MyGoogleFit setFitnessOptions(FitnessOptions fitnessOptions){
        MyGoogleFit.fitnessOptions = fitnessOptions;
        return myGoogleFit;
    }
    public Context getAppContext(){
        return MyGoogleFit.appContext;
    }
    public FitnessOptions getFitnessOptions(){
        return MyGoogleFit.fitnessOptions;
    }

    public MyGoogleFit subscription(int dataType, Context cur_context){
        int length = TYPE_MASK.length;
        for(int i =0 ; i<length ; i++){
            if((dataType & TYPE_MASK[i]) != 0){
                Fitness.getRecordingClient(cur_context, GoogleSignIn.getLastSignedInAccount(appContext))
                        .subscribe(DATA_TYPE[i])
                        .addOnCompleteListener(
                                new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(_TAG_, "TYPE_STEP_COUNT_DELTA | Successfully subscribed!");
                                        } else {
                                            Log.d(_TAG_, "There was a problem subscribing.", task.getException());
                                        }
                                    }
                                });
            }
        }
        return myGoogleFit;
    }
    public MyGoogleFit unsubscription(int dataType, Context cur_context){
        int length = TYPE_MASK.length;
        for(int i =0 ; i<length ; i++){
            if((dataType & TYPE_MASK[i]) != 0){
                Fitness.getRecordingClient(cur_context, GoogleSignIn.getLastSignedInAccount(appContext))
                        .unsubscribe(DATA_TYPE[i])
                        .addOnCompleteListener(
                                new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(_TAG_, "TYPE_STEP_COUNT_DELTA | Successfully subscribed!");
                                        } else {
                                            Log.d(_TAG_, "There was a problem subscribing.", task.getException());
                                        }
                                    }
                                });
            }
        }
        return myGoogleFit;
    }
    public void setLineChart(float[] result, LineChart lineChart,boolean tf_period,String name){
        int period = tf_period ? 30 : 7;

        ArrayList<Entry> entry_chart = new ArrayList<>();

        Log.d("Period Value",String.valueOf(period));

        for(int i = 0;i<period;i++){
            entry_chart.add(new Entry(i,result[i]));
        }

        LineDataSet lineDataSet = new LineDataSet(entry_chart, name);

        // LineData가 필요하여 선언.
        LineData chartData = new LineData();
        chartData.addDataSet(lineDataSet);

        lineChart.setData(chartData);

        lineChart.animateXY(3000,3000);
        lineChart.invalidate();
    }
    public void setLineChart(float[] myData,float[] friData, LineChart lineChart,boolean tf_period,String friName){
        float[] newFriData = new float[30];
        int period = tf_period ? 30 : 7;

        Arrays.fill(newFriData,0);

        for(int counter = period - 1, i = 0; counter >= 0; counter--, i++){
            newFriData[counter] = friData[i];
        }

        ArrayList<Entry> entry_chart1 = new ArrayList<>();
        ArrayList<Entry> entry_chart2 = new ArrayList<>();

        for(int i = 0;i<period;i++){
            entry_chart1.add(new Entry(i,myData[i]));
            entry_chart2.add(new Entry(i,newFriData[i]));
        }

        LineDataSet lineDataSet1 = new LineDataSet(entry_chart1, "내 정보");
        LineDataSet lineDataSet2 = new LineDataSet(entry_chart2, friName);
        lineDataSet2.setColor(Color.RED);

        // LineData가 필요하여 선언.
        LineData chartData = new LineData();
        chartData.addDataSet(lineDataSet1);
        chartData.addDataSet(lineDataSet2);

        lineChart.setData(chartData);

        lineChart.animateXY(3000,3000);
        lineChart.invalidate();
    }

    public void dataToServer(Context cur_context){
        ZonedDateTime endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime startTime = endTime.minusDays(1);

        Log.d(TAG1,"in");

         DataReadRequest dataReadRequests = new DataReadRequest.Builder()
                 .aggregate(DATA_TYPE[0])
                 .aggregate(DATA_TYPE[1])
                 .aggregate(DATA_TYPE[2])
                 .aggregate(DATA_TYPE[3])
                 .bucketByTime(1,TimeUnit.DAYS)
                 .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(),TimeUnit.SECONDS)
                 .build();

         Fitness.getHistoryClient(cur_context, GoogleSignIn.getLastSignedInAccount(appContext))
                 .readData(dataReadRequests)
                 .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                float[] result = new float[4];
                                Arrays.fill(result,0);

                                Log.d("AGGREGATE_data : ","process name getHistoryClient has been successfully done");
                                for(Bucket bucket : dataReadResponse.getBuckets()){
                                    Log.d("Bucket reading :", "Success");
                                    for(DataSet dataset : bucket.getDataSets()){
                                        Log.d("DataSet reading :", "Success");
                                        for(DataPoint dp : dataset.getDataPoints()){
                                            Log.i(TAG1+" Loaded Data","Data point:");
                                            Log.i(TAG1+" Loaded Data","\tType: "+dp.getDataType().getName());
                                            Log.i(TAG1+" Loaded Data","\tStart: "+dp.getStartTime(TimeUnit.DAYS));
                                            Log.i(TAG1+" Loaded Data","\tEnd: " + dp.getEndTime(TimeUnit.DAYS));
                                            for (Field field : dp.getDataType().getFields()) {
                                                Log.d(TAG1+" Field name", field.getName());
                                                switch(field.getName()){
                                                    case "steps":
                                                        result[0] = dp.getValue(field).asInt();
                                                        Log.i(TAG1+ " Loaded Data", "\tField: " + field.getName() +" Value: " + result[0]);
                                                        break;
                                                    case "calories":
                                                        result[1] = dp.getValue(field).asFloat();
                                                        Log.i(TAG1+" Loaded Data", "\tField: " + field.getName() +" Value: " + result[1]);
                                                        break;
                                                    case "distance":
                                                        result[2] = dp.getValue(field).asFloat();
                                                        Log.i(TAG1+" Loaded Data", "\tField: " + field.getName() +" Value: " + result[2]);
                                                        break;
                                                    case "duration":
                                                        result[3] = dp.getValue(field).asInt();
                                                        Log.i(TAG1+" Loaded Data", "\tField: " + field.getName() +" Value: " + result[3]);
                                                        break;
                                                    default:
                                                        Log.d("dataToServer","Switch문에서 문제가 있는것 같습니다.");
                                                }
                                            }
                                        }
                                    }
                                }
                                // 여기서 서버로 데이터 전송.
                                DDTSRequest ddtsRequest = new DDTSRequest(result[0],result[1],result[2],result[3]);

                                RetrofitClient retrofitClient = RetrofitClient.getNewInstance(appContext);
                                initMyApi initMyApi = RetrofitClient.getNewRetrofitInterface();
                                initMyApi.getDDTS(ddtsRequest).enqueue(new Callback<DDTSResponse>() {
                                    @Override
                                    public void onResponse(Call<DDTSResponse> call, Response<DDTSResponse> response) {
                                        DDTSResponse result = response.body();
                                        
                                        String status = result.getStatus();
                                        String message = result.getMessage();

                                        Log.d("DDTSRequest_status",status);
                                        Log.d("DDTSRequest_message",message);
                                    }

                                    @Override
                                    public void onFailure(Call<DDTSResponse> call, Throwable t) {
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("getHistoryClient","Here's some problem of getting history client");
                            }
                        });
    }


    public void getPeriodicData(int dataType, Context cur_context ,boolean tf_period,float[] result,LineChart lineChart){
        // 만약 SharedPreference data로 저장해놓고
        // last update 날짜가 오늘 날짜랑 같다면 거기서 데이터 꺼내오고 아니면
        // 새로 업데이트 해주는 방식으로 하면 좋을것 같네.
        // 우선은 호출 할때마다 갱신하는식으로 할 예정.
        int index_dataType = Math.getExponent(dataType);
        DataType CUR_DATATYPE = DATA_TYPE[index_dataType];

        ZonedDateTime endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime startTime = tf_period ? endTime.minusDays(30) : endTime.minusWeeks(1);

        DataReadRequest dataReadRequests = new DataReadRequest.Builder()
                .aggregate(CUR_DATATYPE)
                .bucketByTime(1,TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(),TimeUnit.SECONDS)
                .build();

        Fitness.getHistoryClient(cur_context, GoogleSignIn.getLastSignedInAccount(appContext))
                .readData(dataReadRequests)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        int count = 0;
                        Log.d("AGGREGATE_data : ","process name getHistoryClient has been successfully done");
                        for(Bucket bucket : dataReadResponse.getBuckets()){
                            Log.d("Bucket reading :", "Success");
                            for(DataSet dataset : bucket.getDataSets()){
                                Log.d("DataSet reading :", "Success");
                                for(DataPoint dp : dataset.getDataPoints()){
                                    Log.i("Loaded Data","Data point:");
                                    Log.i("Loaded Data","\tType: "+dp.getDataType().getName());
                                    Log.i("Loaded Data","\tStart: "+dp.getStartTime(TimeUnit.DAYS));
                                    Log.i("Loaded Data","\tEnd: " + dp.getEndTime(TimeUnit.DAYS));
                                    for (Field field : dp.getDataType().getFields()) {
                                        result[count] = (index_dataType == 0 || index_dataType == 3)? dp.getValue(field).asInt() : dp.getValue(field).asFloat();
                                        Log.i("Loaded Data", "\tField: " + field.getName() +" Value: " + result[count]);
                                    }
                                    count++;
                                }
                            }
                        }
                        setLineChart(result,lineChart, tf_period,"내 정보");
                     }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("getHistoryClient","Here's some problem of getting history client");
                    }
                });
    }
    public void getPeriodicData(int dataType, Context cur_context ,boolean tf_period,float[] result,float[] friData,String friname,LineChart lineChart){
        // 만약 SharedPreference data로 저장해놓고
        // last update 날짜가 오늘 날짜랑 같다면 거기서 데이터 꺼내오고 아니면
        // 새로 업데이트 해주는 방식으로 하면 좋을것 같네.
        // 우선은 호출 할때마다 갱신하는식으로 할 예정.
        int index_dataType = Math.getExponent(dataType);
        DataType CUR_DATATYPE = DATA_TYPE[index_dataType];

        ZonedDateTime endTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime startTime = tf_period ? endTime.minusDays(30) : endTime.minusWeeks(1);

        DataReadRequest dataReadRequests = new DataReadRequest.Builder()
                .aggregate(CUR_DATATYPE)
                .bucketByTime(1,TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(),TimeUnit.SECONDS)
                .build();

        Fitness.getHistoryClient(cur_context, GoogleSignIn.getLastSignedInAccount(appContext))
                .readData(dataReadRequests)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        int count = 0;
                        Log.d("AGGREGATE_data : ","process name getHistoryClient has been successfully done");
                        for(Bucket bucket : dataReadResponse.getBuckets()){
                            Log.d("Bucket reading :", "Success");
                            for(DataSet dataset : bucket.getDataSets()){
                                Log.d("DataSet reading :", "Success");
                                for(DataPoint dp : dataset.getDataPoints()){
                                    Log.i("Loaded Data","Data point:");
                                    Log.i("Loaded Data","\tType: "+dp.getDataType().getName());
                                    Log.i("Loaded Data","\tStart: "+dp.getStartTime(TimeUnit.DAYS));
                                    Log.i("Loaded Data","\tEnd: " + dp.getEndTime(TimeUnit.DAYS));
                                    for (Field field : dp.getDataType().getFields()) {
                                        result[count] = (index_dataType == 0 || index_dataType == 3)? dp.getValue(field).asInt() : dp.getValue(field).asFloat();
                                        Log.i("Loaded Data", "\tField: " + field.getName() +" Value: " + result[count]);
                                    }
                                    count++;
                                }
                            }
                        }
                        setLineChart(result,friData,lineChart, tf_period,friname);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("getHistoryClient","Here's some problem of getting history client");
                    }
                });
    }
    public MyGoogleFit updateDailyTotal(Context cur_context,int dataType,float[] result, TextView[] textViews,TextView cur_step, ProgressBar prgBar) {
        int length = TYPE_MASK.length;
        Log.d("DataType",String.valueOf(dataType));
        for(int i=0;i<length;i++){

            if((dataType & TYPE_MASK[i]) != 0){
                int finalI = i;
                Log.d("ActDatas",String.valueOf(finalI));
                Fitness.getHistoryClient(cur_context, GoogleSignIn.getLastSignedInAccount(appContext))
                        .readDailyTotal(DATA_TYPE[finalI])
                        .addOnSuccessListener(
                                new OnSuccessListener<DataSet>() {
                                    @Override
                                    public void onSuccess(DataSet dataSet) {
                                        for (DataPoint dp : dataSet.getDataPoints()) {
                                            for (Field field : dp.getDataType().getFields()) {
                                                Log.d("Stream Name : ", dp.getOriginalDataSource().getStreamName());
                                                if (!"user_input".equals(dp.getOriginalDataSource().getStreamName())) {

                                                    float act_data = (finalI == 0 || finalI == 3) ? (float)dp.getValue(field).asInt() : dp.getValue(field).asFloat();
                                                    result[finalI]= act_data;
                                                    if(finalI != 0){
                                                        Log.d("DailyTotal",String.valueOf(finalI));
                                                        textViews[finalI-1].setText(setTextView(finalI,result));
                                                    }else{
                                                        cur_step.setText(setTextView(finalI,result));
                                                        prgBar.setProgress((int)result[finalI]);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(_TAG_, "There was a problem getting the step count.", e);
                                    }
                                });
            }
        }
        return myGoogleFit;
    }
    public String setTextView(int index,float[] real_data){
        switch(index){
            case 0 :
                return Math.round(real_data[index]) + "걸음";
            case 1 :
                return "소모 칼로리 : " + Math.round(real_data[index]) + " cal";
            case 2 :
                return "이동 거리 : " + Math.round(real_data[index]) + " m";
            case 3 :
                return "이동 시간 : " + (int)real_data[index] + " 분";
            default :
                return "ERROR";
        }

    }

    public MyGoogleFit addListener(Context cur_context,int dataType, OnDataPointListener[] listenerManager,float[] real_data, TextView[] textView, int myMenu_length) {
        // Listener should be registered one at a time.
        for(int i=0;i<myMenu_length;i++){
            int mask = 1 <<(i+1);
            int trigger = dataType & mask;
            if(trigger != 0){
                int index = Math.getExponent(trigger);
                int finalI = i;

                OnDataPointListener listener = dataPoint -> {
                    Log.d("hellp","hello");
                    for (Field field : dataPoint.getDataType().getFields()) {
                        Log.d("Listener_others","Working");

                        if (!"user_input".equals(dataPoint.getOriginalDataSource().getStreamName())) {
                            float data = (index != 3) ? dataPoint.getValue(field).asFloat() : (float)dataPoint.getValue(field).asInt();
                            real_data[index] += data;
                        }
                    }
                    textView[finalI].setText(setTextView(index,real_data));
                };

                Fitness.getSensorsClient(cur_context, GoogleSignIn.getAccountForExtension(appContext, fitnessOptions))
                        .add(
                                new SensorRequest.Builder()
                                        .setDataType(DATA_TYPE[index]) // Can't be omitted.
                                        .setSamplingRate(1, TimeUnit.SECONDS)
                                        .build(),
                                listener
                        )
                        .addOnSuccessListener(unused ->
                                Log.i(_TAG_, DATA_TYPE[index].getName() + "Listener registered!"))
                        .addOnFailureListener(task ->
                                Log.e(_TAG_, "Listener not registered.", task.getCause()));

                listenerManager[index] =listener;
            }
        }

        return myGoogleFit;
    }
    public MyGoogleFit addListener(Context cur_context, int dataType, OnDataPointListener[] listenerManager, float[] real_data, TextView cur_step, ProgressBar prgBar) {
        // StepData는 ProgressBar의 갱신이 들어가고, MyMenu 편집에는 존재하지 않기 때문에 Method Overloading을 통해 다른 형태로 만듦.
        int index = Math.getExponent(dataType);

        OnDataPointListener listener = dataPoint -> {
            for (Field field : dataPoint.getDataType().getFields()) {
                Log.d("Listener_Step","Working");
                if (!"user_input".equals(dataPoint.getOriginalDataSource().getStreamName())) {
                    float data = (float)dataPoint.getValue(field).asInt();
                    real_data[index] += data;
                }
            }
            int temp = (int)real_data[index];
            cur_step.setText(String.valueOf(temp) + " 걸음");
            prgBar.setProgress(temp);
        };

        Fitness.getSensorsClient(cur_context, GoogleSignIn.getAccountForExtension(appContext, fitnessOptions))
                .add(
                        new SensorRequest.Builder()
                                .setDataType(DATA_TYPE[index]) // Can't be omitted.
                                .setSamplingRate(1, TimeUnit.SECONDS)
                                .build(),
                        listener
                )
                .addOnSuccessListener(unused ->
                        Log.i(_TAG_, "Step Listener registered!"))
                .addOnFailureListener(task ->
                        Log.e(_TAG_, "Step Listener not registered.", task.getCause()));

        listenerManager[index] =listener;
        return myGoogleFit;
    }
    public MyGoogleFit removeListeners(Context cur_context, OnDataPointListener[] ListenerManager,int index) {

        Fitness.getSensorsClient(cur_context, GoogleSignIn.getAccountForExtension(appContext, fitnessOptions))
                .remove(ListenerManager[index])
                .addOnSuccessListener(unused -> Log.i("ListenerManager", "Listener has been removed!"))
                .addOnFailureListener(e -> Log.i("ListenerManager", "Removing Listener has failed"));
        ListenerManager[index] = null;

        return myGoogleFit;
    }

}
