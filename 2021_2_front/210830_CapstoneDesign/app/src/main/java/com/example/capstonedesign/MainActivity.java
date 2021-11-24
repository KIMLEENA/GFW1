package com.example.capstonedesign;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.capstonedesign.home_fragments.FS;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("accessGoogle fit",String.valueOf(resultCode));

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                Log.d("REQUEST_OAUTH_REQUEST","Success");
                PreferenceManager.setBoolean(getApplicationContext(),"REQUEST_OAUTH_REQUEST",true);
            }
        }
        else{
            Log.d("REQUEST_OAUTH_REQUEST","Fail");
            PreferenceManager.setBoolean(getApplicationContext(),"REQUEST_OAUTH_REQUEST",false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setString(getApplicationContext(),"token",null);

        Context appContext = getApplicationContext();

        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(appContext,Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACTIVITY_RECOGNITION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_OAUTH_REQUEST_CODE);
        }

        FitnessOptions fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_CALORIES_EXPENDED,FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_DISTANCE_DELTA,FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA,FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.TYPE_SPEED,FitnessOptions.ACCESS_READ)
                        .build();

        /** Google fit를 사용하기 위한 google 로그인 **/
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(appContext,fitnessOptions);

        /** Permission이 주어져있지 않다면 Permission을 요청해줌. **/
        if(!GoogleSignIn.hasPermissions(account,fitnessOptions)){
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_OAUTH_REQUEST_CODE,
                    account,
                    fitnessOptions);
        } else{
            Log.d("REQUEST_OAUTH_REQUEST","MainActivity has passed true to Home act");
            PreferenceManager.setBoolean(appContext,"REQUEST_OAUTH_REQUEST",true);
        }

        //ActionBar actionBar = getSupportActionBar();
        //actionBar.hide();

        Button kakaobtn = (Button)findViewById(R.id.kakao);
        kakaobtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("'Running Mate'에서 '카카오톡'을(를) 열려고 합니다.");
                builder.setPositiveButton("열기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(getApplicationContext(), "카카오 연동하기", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), KakaoWebview.class);
                        startActivity(intent);
                        finish();
                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(getApplicationContext(), "카카오 연동 안하기", Toast.LENGTH_LONG).show();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        Button naverbtn = (Button)findViewById(R.id.naver);
        naverbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("'Running Mate'에서 '네이버'를(을) 열려고 합니다.");
                builder.setPositiveButton("열기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(getApplicationContext(), "네이버 연동하기", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), NaverWebview.class);
                        startActivity(intent);
                        finish();
                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(getApplicationContext(), "네이버 연동 안하기", Toast.LENGTH_LONG).show();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

    }

    public void loginBtn(View view) {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);

        finish();
    }

    public void register(View view) {
        Intent intent = new Intent(this, Register.class);
        startActivity(intent);

        finish();
    }
}