package com.taca.boombuy.ui.sign;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.otto.Subscribe;
import com.taca.boombuy.NetRetrofit.NetSSL;
import com.taca.boombuy.R;
import com.taca.boombuy.Resmodel.ResBasic;
import com.taca.boombuy.database.StorageHelper;
import com.taca.boombuy.evt.OttoBus;
import com.taca.boombuy.networkmodel.LoginDTO;
import com.taca.boombuy.ui.mainview.activity.GiftManageActivity;
import com.taca.boombuy.ui.mainview.activity.MainActivity;
import com.taca.boombuy.ui.mainview.activity.SelectedSendOrderActivity;
import com.taca.boombuy.util.ImageProc;
import com.taca.boombuy.util.U;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    // 버튼
    ImageButton btn_auto_signin;
    TextView btn_signup;

    // 텍스트
    EditText et_signin_id;
    EditText et_signin_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ImageProc.getInstance().getImageLoader(this);
        // READ_PHONE_STATE 퍼미션
        request_READ_PHONE_STATE_permission();

        // 이벤트 받을 녀석(회원가입 완료 메시지)
        OttoBus.getInstance().getLogin_Bus().register(this);

        // [1] 노티 관련 ===============================================================================
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.cancel(getIntent().getIntExtra("NOTI_ID", 0));  // 노티 삭제

        et_signin_id = (EditText) findViewById(R.id.et_signin_id);
        et_signin_password = (EditText) findViewById(R.id.et_signin_password);

        // 자동로그인 후(sharedpreference에 저장)
        if (StorageHelper.getInstance().getBoolean(LoginActivity.this, "auto_login")) {
            et_signin_id.setText(StorageHelper.getInstance().getString(LoginActivity.this, "my_phone_number"));
            et_signin_password.setText(StorageHelper.getInstance().getString(LoginActivity.this, "auto_login_password"));

            // 서버에 로그인 시도
            LoginDTO loginDTO = new LoginDTO(
                    StorageHelper.getInstance().getString(LoginActivity.this, "my_phone_number"),
                    StorageHelper.getInstance().getString(LoginActivity.this, "auto_login_password"),
                    FirebaseInstanceId.getInstance().getToken()
            );

            Call<ResBasic> NetLogin = NetSSL.getInstance().getMemberImpFactory().NetLogin(loginDTO);
            NetLogin.enqueue(new Callback<ResBasic>() {
                @Override
                public void onResponse(Call<ResBasic> call, Response<ResBasic> response) {

                    Log.i("LOG RESPONSE", response.message().toString());

                    if (response.isSuccessful()) {
                        if (response.body() != null && response.body().getMessage() != null) {
                            Log.i("RES SUC", response.body().getMessage());
                            OttoBus.getInstance().getLogin_Bus().post(response.body());
                        } else {

                            Toast.makeText(LoginActivity.this, "서버 점검 중입니다", Toast.LENGTH_SHORT).show();
                            Log.i("RF", "로그인실패:" + response.message());
                        }
                    } else {
                        Log.i("RF", "로그인실패11:" + response.message());

                        Toast.makeText(LoginActivity.this, "서버 점검 중입니다", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResBasic> call, Throwable t) {

                    t.printStackTrace();
                    Log.i("ERROR : ", t.getMessage());
                }
            });
        }

        // 버튼 매칭 및 클릭리스너
        btn_auto_signin = (ImageButton) findViewById(R.id.btn_auto_signin);
        btn_auto_signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 서버 로그인 시도
                LoginDTO loginDTO = new LoginDTO(
                        et_signin_id.getText().toString(),
                        et_signin_password.getText().toString(),
                        FirebaseInstanceId.getInstance().getToken()
                );
                Call<ResBasic> NetLogin = NetSSL.getInstance().getMemberImpFactory().NetLogin(loginDTO);

                NetLogin.enqueue(new Callback<ResBasic>() {
                    @Override
                    public void onResponse(Call<ResBasic> call, Response<ResBasic> response) {

                        Log.i("LOG RESPONSE", response.message().toString());

                        if (response.isSuccessful()) {
                            if (response.body() != null && response.body().getMessage() != null) {
                                Log.i("RES SUC", response.body().getMessage());
                                OttoBus.getInstance().getLogin_Bus().post(response.body());
                            } else {
                                Log.i("RF", "로그인실패:" + response.message());
                                Toast.makeText(LoginActivity.this, "서버 점검 중입니다", Toast.LENGTH_SHORT).show();
                            }
                        } else {

                            Log.i("RF", "로그인실패11:" + response.message());
                            Toast.makeText(LoginActivity.this, "서버 점검 중입니다", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResBasic> call, Throwable t) {

                        t.printStackTrace();
                        Log.i("ERROR : ", t.getMessage());
                    }
                });


            }
        });

        btn_signup = (TextView) findViewById(R.id.btn_signup);
        btn_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // my_phone_number 쉐어드프리퍼런스에 내 전화번호 저장
                StorageHelper.getInstance().setString(LoginActivity.this, "my_phone_number", U.getInstance().getMyPhoneNum(LoginActivity.this));
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivityForResult(intent, 77);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 77 && resultCode == 7) {
            et_signin_id.setText(StorageHelper.getInstance().getString(LoginActivity.this, "my_phone_number"));
            et_signin_password.setText(StorageHelper.getInstance().getString(LoginActivity.this, "auto_login_password"));
            // 서버 로그인 시도
            LoginDTO loginDTO = new LoginDTO(
                    et_signin_id.getText().toString(),
                    et_signin_password.getText().toString(),
                    FirebaseInstanceId.getInstance().getToken()
            );
            Call<ResBasic> NetLogin = NetSSL.getInstance().getMemberImpFactory().NetLogin(loginDTO);

            NetLogin.enqueue(new Callback<ResBasic>() {
                @Override
                public void onResponse(Call<ResBasic> call, Response<ResBasic> response) {

                    Log.i("LOG RESPONSE", response.message().toString());

                    if (response.isSuccessful()) {
                        if (response.body() != null && response.body().getMessage() != null) {
                            Log.i("RES SUC", response.body().getMessage());
                            OttoBus.getInstance().getLogin_Bus().post(response.body());
                        } else {
                            Log.i("RF", "로그인실패:" + response.message());
                            Toast.makeText(LoginActivity.this, "서버 점검 중입니다", Toast.LENGTH_SHORT).show();
                        }
                    } else {

                        Log.i("RF", "로그인실패11:" + response.message());
                        Toast.makeText(LoginActivity.this, "서버 점검 중입니다", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResBasic> call, Throwable t) {

                    t.printStackTrace();
                    Log.i("ERROR : ", t.getMessage());
                }
            });
        }
    }

    // 오토버스 이벤트 도착
    ResBasic resBasic;

    @Subscribe
    public void FinishLoad(ResBasic data) {

        resBasic = data;
        if (resBasic.getMessage() != null) {
            StorageHelper.getInstance().setBoolean(LoginActivity.this, "auto_login", true);
            StorageHelper.getInstance().setString(LoginActivity.this, "my_phone_number", et_signin_id.getText().toString());
            StorageHelper.getInstance().setString(LoginActivity.this, "auto_login_password", et_signin_password.getText().toString());

            if (getIntent().getStringExtra("FCM") != null) {
                if(getIntent().getStringExtra("FCM").contains("/")) {
                    // 받은 선물함으로
                    Intent intent = new Intent(LoginActivity.this, GiftManageActivity.class);
                    intent.putExtra("receivedFCM", "받은선물바로");
                    startActivity(intent);
                } else {
                    // 해당 주문으로
                    Intent intent = new Intent(LoginActivity.this, SelectedSendOrderActivity.class);
                    intent.putExtra("oid", Integer.parseInt(getIntent().getStringExtra("FCM")));
                    startActivity(intent);
                }

            } else {
                //그냥 왔다
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            // 오토버스 썼으면 등록해제
            OttoBus.getInstance().getLogin_Bus().unregister(this);
            finish();

        } else {
            Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // 사용자가 권한 동의를 안하므로 종료
                    finish();
                }
            }
        }
    }

    public void request_READ_PHONE_STATE_permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            //권한이 없을 경우

            //최초 권한 요청인지 혹은 사용자에 의한 재요청인지 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                //사용자가 임의로 권한을 취소시킨 경우
                //권한 재요청
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            } else {
                //최초로 권한을 요청하는 경우(첫실행)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            }
        } else {
            // my_phone_number 쉐어드프리퍼런스에 내 전화번호 저장
            //StorageHelper.getInstance().setString(LoginActivity.this, "my_phone_number", U.getInstance().getMyPhoneNum(LoginActivity.this));
        }
    }
}