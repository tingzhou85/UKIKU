package knf.kuma;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.evernote.android.job.JobManager;

import androidx.appcompat.app.AppCompatDelegate;
import es.munix.multidisplaycast.CastManager;
import io.branch.referral.Branch;
import io.fabric.sdk.android.Fabric;
import knf.kuma.commons.CastUtil;
import knf.kuma.commons.EAHelper;
import knf.kuma.commons.Network;
import knf.kuma.commons.PrefsUtil;
import knf.kuma.database.CacheDB;
import knf.kuma.database.EADB;
import knf.kuma.directory.DirectoryService;
import knf.kuma.download.DownloadManager;
import knf.kuma.download.DownloadService;
import knf.kuma.download.FileAccessHelper;
import knf.kuma.jobscheduler.JobsCreator;
import knf.kuma.jobscheduler.RecentsJob;
import knf.kuma.jobscheduler.UpdateJob;

public class App extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context CONTEXT;

    public static Context getContext() {
        return CONTEXT;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannels() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            NotificationChannel dir_channel = new NotificationChannel(DirectoryService.CHANNEL, getString(R.string.directory_channel_title), NotificationManager.IMPORTANCE_MIN);
            dir_channel.setSound(null, new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            manager.createNotificationChannel(dir_channel);
            manager.createNotificationChannel(new NotificationChannel(RecentsJob.CHANNEL_RECENTS, "Capitulos recientes", NotificationManager.IMPORTANCE_HIGH));
            manager.createNotificationChannel(new NotificationChannel(DownloadService.CHANNEL, "Descargas", NotificationManager.IMPORTANCE_HIGH));
            manager.createNotificationChannel(new NotificationChannel(DownloadService.CHANNEL_ONGOING, "Descargas en progreso", NotificationManager.IMPORTANCE_LOW));
            manager.createNotificationChannel(new NotificationChannel(DownloadManager.CHANNEL_FOREGROUND, "Administrador de descargas", NotificationManager.IMPORTANCE_MIN));
            manager.createNotificationChannel(new NotificationChannel(UpdateJob.CHANNEL, "Actualización de la app", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new JobsCreator());
        /*Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                *//*if (!(e instanceof InternalError)) {
                    if (!(e instanceof OutOfMemoryError)) {

                    }
                }*//*
                try {
                    File file=new File(getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath()),System.currentTimeMillis()+".txt");
                    file.createNewFile();
                    e.printStackTrace(new PrintStream(file));
                    System.exit(0);
                }catch (Exception ex){
                    //ex
                }
            }
        });*/
        CONTEXT = this;
        PrefsUtil.INSTANCE.init(this);
        Fabric.with(this, new Crashlytics(), new Answers());
        Branch.getAutoInstance(this);
        AppCompatDelegate.setDefaultNightMode(Integer.parseInt(PrefsUtil.INSTANCE.getThemeOption()));
        CastManager.register(this);
        Network.init(this);
        CacheDB.init(this);
        EADB.init(this);
        EAHelper.init(this);
        CastUtil.init(this);
        DownloadManager.init(this);
        FileAccessHelper.init(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannels();
    }
}
