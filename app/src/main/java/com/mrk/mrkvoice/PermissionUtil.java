package com.mrk.mrkvoice;

import android.Manifest;

public class PermissionUtil {
    public static final String[] REQUEST_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    public static final int REQUEST_PERMISSIONS_CODE = 100;
}
