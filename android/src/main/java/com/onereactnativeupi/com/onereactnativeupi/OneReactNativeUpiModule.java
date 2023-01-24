package com.onereactnativeupi;

import android.app.Activity;

import android.net.Uri;
import android.content.Intent;
import androidx.annotation.NonNull;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import java.util.List;
import java.util.Objects;

@ReactModule(name = OneReactNativeUpiModule.NAME)
public class OneReactNativeUpiModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    public static final String NAME = "OneReactNativeUpi";
    final private static String FAILED = "FAILED";
    final private static String SUCCESS = "SUCCESS";
    final private static int REQUEST_CODE = 321;

    public OneReactNativeUpiModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    Callback success;
    Callback failure;
    Uri uri = Uri.parse("upi://pay");

    @ReactMethod
    public void initiatePayment(ReadableMap config, Callback success, Callback failure) {

        this.success = success;
        this.failure = failure;
        Uri data = uri.buildUpon().appendQueryParameter("pa", config.getString("upiId"))
                .appendQueryParameter("pn", config.getString("name"))
                .appendQueryParameter("tn", config.getString("note"))
                .appendQueryParameter("am", config.getString("amount"))
                .appendQueryParameter("cu", "INR")
                .build();

        Intent upiPaymentIntent = new Intent(Intent.ACTION_VIEW);

        upiPaymentIntent.setData(data);

        if (config.getString("targetPackage").length() != 0) {
            try {
                upiPaymentIntent.setPackage(config.getString("targetPackage"));
                getCurrentActivity().startActivityForResult(upiPaymentIntent, REQUEST_CODE);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {

            Intent chooser = Intent.createChooser(upiPaymentIntent, config.getString("chooserText"));
            if (null != chooser.resolveActivity(Objects.requireNonNull(getCurrentActivity()).getPackageManager())) {

                getCurrentActivity().startActivityForResult(chooser, REQUEST_CODE);
            } else {

                WritableNativeMap message = new WritableNativeMap();
                message.putString("status", FAILED);
                message.putString("message", "No Apps Found for the upi payment");

                this.failure.invoke(message);

            }
        }

    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

        WritableNativeMap response = new WritableNativeMap();

        try {
            if (data == null) {
                response.putString("status", FAILED);
                response.putString("message", "No Action Taken");
                this.failure.invoke(response);
                return;

            }
            if (requestCode == REQUEST_CODE) {

                if (data.getStringExtra("Status").trim().equalsIgnoreCase(SUCCESS)) {
                    response.putString("status", SUCCESS);
                    response.putString("txnId", data.getStringExtra("txnId"));
                    response.putString("code", data.getStringExtra("responseCode"));
                    response.putString("approvalRefNo", data.getStringExtra("ApprovalRefNo"));
                    this.success.invoke(response);

                } else {
                    response.putString("status", FAILED);
                    response.putString("message", "Payment was not done!");
                    this.failure.invoke(response);

                }

            } else {
                response.putString("status", FAILED);
                response.putString("message", "Request code mismatched ");
                this.failure.invoke(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @ReactMethod
    public WritableNativeArray getInstalledUPIApps() {

        WritableNativeArray upiList = new WritableNativeArray();

        Uri uri = Uri.parse(String.format("%s://%s", "upi", "pay"));
        Intent upiUriIntent = new Intent();
        upiUriIntent.setData(uri);
        PackageManager packageManager = getReactApplicationContext().getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(upiUriIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfoList != null) {
            for (ResolveInfo resolveInfo : resolveInfoList) {
                upiList.pushString(resolveInfo.activityInfo.packageName);
            }

        }

        return upiList;

    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

}
