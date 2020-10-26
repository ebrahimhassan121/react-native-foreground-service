package com.softmills.foregroundservice;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class CallNotificationActionReceiver extends BroadcastReceiver {


    Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext=context;
        if (intent != null && intent.getAction() != null) {
            Log.d("--------",intent.getAction());
         handleIntentCalls(intent);
            String action ="";
            action=intent.getStringExtra("action");

            if (action != null&& !action.equalsIgnoreCase("decline")) {
                performClickAction(context, action);
            }
            // Close the notification after the click action is performed.
            Intent iclose = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(iclose);
            context.stopService(new Intent(context, VIForegroundService.class));

        }


    }
    private void performClickAction(Context context, String action) {
        if(action.equalsIgnoreCase("RECEIVE_CALL")) {

            if (checkAppPermissions()) {
                Intent intentCallReceive = new Intent(mContext, getMainActivityClass());
                intentCallReceive.putExtra("Call", "incoming");
                intentCallReceive.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mContext.startActivity(intentCallReceive);
            }
            else{
                Intent intent = new Intent(VIForegroundServiceModule.reactContext, getMainActivityClass());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("CallFrom","call from push");
                mContext.startActivity(intent);

            }
        }
        else if(action.equalsIgnoreCase("DIALOG_CALL")){

            // show ringing activity when phone is locked
            Intent intent = new Intent(VIForegroundServiceModule.reactContext, getMainActivityClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(intent);
        }

        else {
            context.stopService(new Intent(context, getMainActivityClass()));
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        }
    }

    private Boolean checkAppPermissions() {
        return hasReadPermissions() && hasWritePermissions() && hasCameraPermissions() && hasAudioPermissions();
    }

    private boolean hasAudioPermissions() {
        return (ContextCompat.checkSelfPermission(VIForegroundServiceModule.reactContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasReadPermissions() {
        return (ContextCompat.checkSelfPermission(VIForegroundServiceModule.reactContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean hasWritePermissions() {
        return (ContextCompat.checkSelfPermission(VIForegroundServiceModule.reactContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }
    private boolean hasCameraPermissions() {
        return (ContextCompat.checkSelfPermission(VIForegroundServiceModule.reactContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
    }
    public Class getMainActivityClass() {
        String packageName = mContext.getPackageName();
        Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void  handleIntentCalls(Intent intent){
        try {
            String action=intent.getAction();
            Log.d("action-->",action+"");
            if(action!=null&&action.equals("decline")){
                VIForegroundService.inCall=false;
                Bundle notificationConfig=intent.getBundleExtra("notficationConfig");
                WritableMap params = Arguments.createMap();
                Log.v("notification size",notificationConfig.keySet().size()+"");
                if(notificationConfig!=null&&notificationConfig.keySet().size()>0){
                    for (String key:notificationConfig.keySet()){
                        params.putString(key, notificationConfig.get(key+"")+"");
                        Log.v("key_"+key,notificationConfig.get(key)+"");
                    }
                }
                final ReactContext reactContext = VIForegroundServiceModule.reactContext;
                sendEvent(reactContext,"AndroidADeclineCall",params);

            }
        }catch (Exception error){
            Log.d("err--xxx-->",error.getMessage());
            error.printStackTrace();
        }
    }
    private  void sendEvent(ReactContext reactContext,
                            String eventName,
                            @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

}