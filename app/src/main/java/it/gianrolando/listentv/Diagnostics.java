package it.gianrolando.listentv;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.media3.datasource.HttpDataSource;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
final class Diagnostics {
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("diagnostics",Context.MODE_PRIVATE);
    }

    static void install(Context context) {
        Context app=context.getApplicationContext();
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)-> {
            try {
                SharedPreferences p=prefs(app);
                p.edit().putString("java_crash","Channel: "+p.getString("channel","unknown")+"\n"+DiagnosticText.describe(error)).commit();
            } catch(Throwable ignored) { }
            finally {
                if(previous!=null) previous.uncaughtException(thread,error);
                else { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(10); }
            }
        });
    }

    static void channel(Context context,String id) {
        // Called on the resolver's worker thread; persist before playback can crash.
        prefs(context).edit().putString("channel",id).putString("last_event","Resolving channel").commit();
    }

    static void failure(Context context,String stage,Throwable error) {
        StringBuilder code=new StringBuilder(stage);
        Throwable cause=error;
        for(int i=0;i<6 && cause!=null;i++,cause=cause.getCause())
            if(cause instanceof HttpDataSource.InvalidResponseCodeException http) code.append(" HTTP ").append(http.responseCode);
            else if(cause instanceof StreamResolver.HttpStatusException http) code.append(" HTTP ").append(http.status);
        prefs(context).edit().putString("last_event",code+"\n"+DiagnosticText.describe(error)).apply();
    }

    static String report(Context context) {
        SharedPreferences p=prefs(context);
        String version="unknown";
        try { version=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName; }
        catch(Exception ignored) { }
        StringBuilder report=new StringBuilder("ListenTv ").append(version).append("\nAndroid API ")
            .append(Build.VERSION.SDK_INT).append(" · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append("\nChannel: ").append(p.getString("channel","none"))
            .append("\nLast event: ").append(p.getString("last_event","none"))
            .append("\nPrevious Java crash: ").append(p.getString("java_crash","none"));
        if(Build.VERSION.SDK_INT>=30) {
            try {
                ActivityManager manager=context.getSystemService(ActivityManager.class);
                if(manager!=null) for(ApplicationExitInfo exit:manager.getHistoricalProcessExitReasons(context.getPackageName(),0,8)) {
                    int reason=exit.getReason();
                    if(reason==ApplicationExitInfo.REASON_CRASH || reason==ApplicationExitInfo.REASON_CRASH_NATIVE || reason==ApplicationExitInfo.REASON_ANR) {
                        report.append("\nPrevious system exit: reason=").append(reason).append(" status=").append(exit.getStatus())
                            .append(" timestamp=").append(exit.getTimestamp());
                        break;
                    }
                }
            } catch(Exception ignored) { }
        }
        return report.toString();
    }
}
