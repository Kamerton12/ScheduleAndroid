package com.project.androidtestwidgetonly;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implementation of App Widget functionality.
 */
public class Schedule extends AppWidgetProvider
{
    static final String site ="http://www.mrk-bsuir.by/ru";
    static String pdfUrl = "";
    static final String UPDATE_ACTION = "update_action";
    static final String FORCE_UPDATE_ACTION = "force_update_action";
    final static String PREFIX = "ppreffix";

    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, final int appWidgetId, final boolean force)
    {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.schedule);

        Intent intent1 = new Intent(context, Schedule.class);
        intent1.setAction(FORCE_UPDATE_ACTION);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context, 0, intent1, 0);
        views.setOnClickPendingIntent(R.id.imageView, pendingIntent1);
        views.setOnClickPendingIntent(R.id.relative, pendingIntent1);

        if(force)
            Log.d(PREFIX, "Force update");
        Handler handler = new Handler(Looper.getMainLooper());
        if(force)
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(context,"Updating...", Toast.LENGTH_SHORT).show();
                }
            });
        class Asynk extends AsyncTask<Void, Void, Void>
        {
            @Override
            protected Void doInBackground(Void... voids)
            {
                Log.d(PREFIX, "doInBackground");
                try
                {
                    String pdfUrlNew = "";
                    URL siteUrl = new URL(site);
                    try(BufferedReader bf1 = new BufferedReader(new InputStreamReader(siteUrl.openStream())))
                    {
                        StringBuilder sb1 = new StringBuilder();
                        String line1 = null;
                        while((line1 = bf1.readLine()) != null)
                        {
                            sb1.append(line1);
                        }
                        int pos1 = sb1.indexOf("Объявления");
                        int end1 = sb1.indexOf(".pdf", pos1);
                        pdfUrlNew = sb1.substring(pos1+76, end1+4);
                    }
                    if(pdfUrl == null || pdfUrlNew != pdfUrl)
                    {
                        pdfUrl = pdfUrlNew;
                        URL file = new URL(pdfUrl);

                        String fileNamePath = Environment.getExternalStorageDirectory().toString() + "/load/" + "rasp.pdf";
                        try(InputStream is = file.openStream())
                        {
                            int len;
                            byte[] buf = new byte[2048 * 2];
                            try(FileOutputStream fos = new FileOutputStream(fileNamePath))
                            {
                                while((len = is.read(buf)) != -1)
                                {
                                    fos.write(buf, 0 , len);
                                }
                            }
                        }

                        File f = new File(fileNamePath);
                        try(PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)))
                        {
                            PdfRenderer.Page page = renderer.openPage(4);
                            Bitmap bitmap = Bitmap.createBitmap(page.getWidth()*2, page.getHeight()*2, Bitmap.Config.ARGB_8888);
                            Log.d(PREFIX, Integer.toString(page.getHeight()));
                            Log.d(PREFIX, Integer.toString(page.getWidth()));
                            page.render(bitmap, new Rect(0, 0, page.getWidth() * 2, page.getHeight() * 2), null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            Log.d(PREFIX, Integer.toString(bitmap.getHeight()));
                            Log.d(PREFIX, Integer.toString(bitmap.getWidth()));
                            int x = 80;
                            int y = 30;
                            Bitmap bmp = Bitmap.createBitmap(bitmap, x ,  y, bitmap.getWidth() - x - 50, bitmap.getHeight()/4 - y + 15);
                            views.setBitmap(R.id.imageView, "setImageBitmap", bmp);
                            page.close();
                        }
                    }
                    Log.d(PREFIX, "Update");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    Handler handler = new Handler(Looper.getMainLooper());
                    if(force)
                        handler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(context,"DONE", Toast.LENGTH_LONG).show();
                            }
                        });
                }
                catch (Exception e)
                {
                    Log.d(PREFIX, "error", e);
                    Handler handler = new Handler(Looper.getMainLooper());
                    if(force)
                        handler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(context,"Something went wrong...", Toast.LENGTH_LONG).show();
                            }
                        });
                    e.printStackTrace();
                }
                return null;
            }
        }

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(mWifi.isConnected() || force)
        {
            new Asynk().execute();
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        }
    }

    @Override
    public void onEnabled(Context context)
    {
        Log.d(PREFIX, "onEnabled");
        Intent intent = new Intent(context, Schedule.class);
        intent.setAction(UPDATE_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 60000, pendingIntent);
    }

    @Override
    public void onDisabled(Context context)
    {
        Log.d(PREFIX, "onDisabled");
        Intent intent = new Intent(context, Schedule.class);
        intent.setAction(UPDATE_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);
        Log.d(PREFIX, "onReceive");
        if(intent.getAction().equalsIgnoreCase(UPDATE_ACTION))
        {
            ComponentName name = new ComponentName(context.getPackageName(), getClass().getName());
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(name);
            for(int i: ids)
            {
                updateAppWidget(context, manager, i, false);
            }
        }
        else if(intent.getAction().equalsIgnoreCase(FORCE_UPDATE_ACTION))
        {

            Log.d(PREFIX, "onReceiveForce");
            ComponentName name = new ComponentName(context.getPackageName(), getClass().getName());
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(name);
            for(int i: ids)
            {
                updateAppWidget(context, manager, i, true);
            }
        }
    }
}

