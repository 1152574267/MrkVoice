package com.mrk.mrkvoice;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (Build.VERSION.SDK_INT >= 23) {
            checkExternalStorage();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    /*********************************************************/
    public void checkExternalStorage() {
        // 检查APP是否已经有权限
        for (int i = 0; i < PermissionUtil.REQUEST_PERMISSIONS.length; i++) {
            int checkSelfPermission = ActivityCompat.checkSelfPermission(this, PermissionUtil.REQUEST_PERMISSIONS[i]);
            Log.d(TAG, "checkSelfPermission: " + checkSelfPermission + ", Permission: " + PermissionUtil.REQUEST_PERMISSIONS[i]);

            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
                requestExternalStoragePermissions();
                return;
            }
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void requestExternalStoragePermissions() {
        // 是否需要向用户解析需要申请的权限
        for (int i = 0; i < PermissionUtil.REQUEST_PERMISSIONS.length; i++) {
            boolean isShouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, PermissionUtil.REQUEST_PERMISSIONS[i]);
            Log.d(TAG, "isShouldShowRequestPermissionRationale: " + isShouldShowRequestPermissionRationale + ", Permission: " + PermissionUtil.REQUEST_PERMISSIONS[i]);

            if (isShouldShowRequestPermissionRationale) {
                ActivityCompat.requestPermissions(this, PermissionUtil.REQUEST_PERMISSIONS, PermissionUtil.REQUEST_PERMISSIONS_CODE);
                break;
            } else {
                ActivityCompat.requestPermissions(this, PermissionUtil.REQUEST_PERMISSIONS, PermissionUtil.REQUEST_PERMISSIONS_CODE);
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < grantResults.length; i++) {
            Log.d(TAG, "grantResults[" + i + "]: " + grantResults[i] + ", size: " + grantResults.length);
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth)
                        .setMessage("请开启应用权限！")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .create()
                        .show();
                return;
            }
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    /*********************************************************/
}
