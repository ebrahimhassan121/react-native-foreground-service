/*
 * Copyright (c) 2011-2019, Zingaya, Inc. All rights reserved.
 */

package com.softmills.foregroundservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.concurrent.ExecutionException;

import static com.softmills.foregroundservice.Constants.NOTIFICATION_CONFIG;

public class VIForegroundService extends Service {
    String CHANNEL_ID="ForegroundServiceChannel";
   public static  Boolean  inCall = false;
    int NOTIFICATION_ID=120;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action != null) {

            if (action.equals(Constants.ACTION_FOREGROUND_SERVICE_START)) {
                if (intent.getExtras() != null && intent.getExtras().containsKey(NOTIFICATION_CONFIG)) {
                    final Bundle notificationConfig = intent.getExtras().getBundle(NOTIFICATION_CONFIG);
                    this.inCall=true;
                    Log.v("notificationID--->",notificationConfig.get("id")+"");
                    final int NOTIFICATION_ID= new Double(notificationConfig.getDouble("id")).intValue();
                    final String avatarUrl= notificationConfig.getString("avatarUrl");
                    final  Intent finalIntnet=intent;
                    FutureTarget<Bitmap> futureTarget = Glide.with(this).asBitmap()
                            .load(avatarUrl)
                            .circleCrop().submit();
                    LoadImageTask task = new LoadImageTask(new LoadImageTask.OnSuccess() {
                        @Override
                        public void onSuccess(Bitmap icon) {
                            PowerManager pm = (PowerManager) VIForegroundServiceModule.reactContext.getSystemService(Context.POWER_SERVICE);
                            boolean isScreenOn = Build.VERSION.SDK_INT >= 20 ? pm.isInteractive() : pm.isScreenOn(); // check if screen is on
                            if (!isScreenOn) {
                                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "Telawa:notificationLock");
                                wl.acquire(3000); //set your time in milliseconds
                            }
                            VIForegroundService.this.startForeground(NOTIFICATION_ID, VIForegroundService.this.buildCustomNotification(finalIntnet, NOTIFICATION_ID, notificationConfig,icon));

                        }
                    },VIForegroundServiceModule.reactContext);
                    task.execute(futureTarget);


//                    startForeground(NOTIFICATION_ID,buildCustomNotification(intent,flags,NOTIFICATION_ID,notificationConfig));

                }
            } else if (action.equals(Constants.ACTION_FOREGROUND_SERVICE_STOP)) {
                this.inCall=false;
                stopSelf();
            }
        }
        return START_STICKY;

    }
    Notification buildCustomNotification(Intent intent, int notificationId,Bundle notificationConfig,Bitmap bitmap) {

        String Notification_CHANNEL_ID=CHANNEL_ID;
        String avatarUrl="";String DeclineText="";String AcceptText="";String CallerName="";String Description="";
        if ( notificationConfig.containsKey("channelId")) {
            Notification_CHANNEL_ID=notificationConfig.getString("channelId");
        }
        if(notificationConfig.containsKey("avatarUrl")){
         avatarUrl=  notificationConfig.getString("avatarUrl");
        }
        if(notificationConfig.containsKey("declineText")){
            DeclineText=  notificationConfig.getString("declineText");
        }
        if(notificationConfig.containsKey("acceptText")){
            AcceptText=  notificationConfig.getString("acceptText");
        }
        if(notificationConfig.containsKey("callerName")){
            CallerName=  notificationConfig.getString("callerName");
        }
        if(notificationConfig.containsKey("description")){
            Description=  notificationConfig.getString("description");
        }

        Intent DeclineIntent = new Intent(this, CallNotificationActionReceiver.class);
        DeclineIntent.setAction("decline");
        DeclineIntent.putExtra("action","decline");
        DeclineIntent.putExtra("action1","------");
        DeclineIntent.putExtra("notficationConfig",notificationConfig);

        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, 0, DeclineIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent FullScreenPendingIntent = PendingIntent.getBroadcast(this, 0,  new Intent(this, CallNotificationActionReceiver.class), 0);

        RemoteViews notificationView=new RemoteViews(this.getPackageName(),R.layout.collapesed);
        RemoteViews notificationViewExpanded=new RemoteViews(this.getPackageName(),R.layout.expanded);

        notificationView.setTextViewText(R.id.callerName,CallerName);
        notificationView.setTextViewText(R.id.description,Description);
        notificationView.setTextViewText(R.id.declineButton,DeclineText);
        notificationView.setTextViewText(R.id.answerButton,AcceptText);

        notificationViewExpanded.setTextViewText(R.id.callerName,CallerName);
        notificationViewExpanded.setTextViewText(R.id.description,Description);
        notificationViewExpanded.setTextViewText(R.id.declineButton,DeclineText);
        notificationViewExpanded.setTextViewText(R.id.answerButton,AcceptText);

        notificationView.setOnClickPendingIntent(R.id.declineButton,declinePendingIntent);
        notificationView.setOnClickPendingIntent(R.id.answerButton,AcceptPendingIntent(this,notificationId,notificationConfig));
        notificationViewExpanded.setOnClickPendingIntent(R.id.answerButton,AcceptPendingIntent(this,notificationId,notificationConfig));
        notificationViewExpanded.setOnClickPendingIntent(R.id.declineButton,declinePendingIntent);

        notificationView.setImageViewBitmap(R.id.callerAvatar,bitmap);
        notificationViewExpanded.setImageViewBitmap(R.id.callerAvatar,bitmap);

        NotificationCompat.Builder notificationBuilder=
                new NotificationCompat.Builder(this,Notification_CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(Notification.CATEGORY_CALL);
        notificationBuilder.setCustomContentView(notificationView);
        notificationBuilder.setCustomBigContentView(notificationViewExpanded);
        notificationBuilder.setCustomHeadsUpContentView(notificationViewExpanded);
        notificationBuilder.setSmallIcon(R.drawable.ic_call_black_24dp);
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setPriority(Notification.PRIORITY_MAX) ;
        notificationBuilder.setStyle(new NotificationCompat.InboxStyle());
        notificationBuilder.setFullScreenIntent(FullScreenPendingIntent,true);

        //Vibration
        notificationBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });

        //LED
        notificationBuilder.setLights(Color.RED, 3000, 3000);
        //Ton
        notificationBuilder.setSound(Uri.parse("android.resource://" + this.getPackageName() + "/"+ R.raw.nosound));
        return notificationBuilder.build();
    }
    private Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null || launchIntent.getComponent() == null) {
            Log.e("NotificationHelper", "Failed to get launch intent or component");
            return null;
        }
        try {
            return Class.forName(launchIntent.getComponent().getClassName());
        } catch (ClassNotFoundException e) {
            Log.e("NotificationHelper", "Failed to get main activity class");
            return null;
        }
    }

    public PendingIntent AcceptPendingIntent(Context context, int notificationID , Bundle notificationConfig){
        Class intentClass = getMainActivityClass(context);
        Intent intent = new Intent(this, intentClass);
        intent.setAction("acceptCall");
        intent.putExtra("action","acceptCall");
        intent.putExtra("notficationConfig",notificationConfig);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public Bitmap roundCornerImage(Bitmap raw, float round) {
        int width = raw.getWidth();
        int height = raw.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawARGB(0, 0, 0, 0);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#000000"));

        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);

        canvas.drawRoundRect(rectF, round, round, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(raw, rect, rect, paint);

        return result;
    }


    private static class LoadImageTask extends AsyncTask<FutureTarget<Bitmap>, Void, Bitmap> {
        private OnSuccess onSuccess;
        private Context context;
        interface OnSuccess {
            void onSuccess(Bitmap bitmap);
        }

        LoadImageTask(OnSuccess onSuccess,Context context) {
            this.onSuccess = onSuccess;
            this.context=context;
        }

        @SafeVarargs @Override
        protected final Bitmap doInBackground(FutureTarget<Bitmap>... futureTargets) {
            try {
                return futureTargets[0].get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null)
                onSuccess.onSuccess(bitmap);
            else {

                Bitmap defaultbitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.avatar);
                onSuccess.onSuccess(defaultbitmap);
                Log.v("@doInBackground","get failed");

            }
        }
    }
   }


