package br.ufpe.cin.if710.podcast.ui;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;

/**
 * Created by Hoi on 13/10/2017.
 */

public class NotificationService extends Service {

    public static final String DOWNLOAD_COMPLETE = "br.ufpe.cin.if710.services.action.DOWNLOAD_COMPLETE";
    public NotificationCompat.Builder mBuilder;
    public Intent resultIntent;
    public PendingIntent resultPendingIntent;

    public List<ItemFeed> getListFromDB(){
        Cursor cursor = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                null, null, null);

        List<ItemFeed> listItem = new ArrayList<>();

        // https://stackoverflow.com/questions/2810615/how-to-retrieve-data-from-cursor-class
        // Extração de informação do cursor de acordo com o link acima

        if(cursor.moveToFirst()){
            do{
                ItemFeed item = new ItemFeed(cursor.getString(cursor.getColumnIndex(PodcastProviderContract.TITLE)),
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.EPISODE_LINK)),
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DATE)),
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DOWNLOAD_LINK)),
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.EPISODE_URI)),
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.AUDIO_STATE)),
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.BUTTON_STATE)));
                listItem.add(item);

                /*
                Log.v("Getting: ", cursor.getString(cursor.getColumnIndex(PodcastProviderContract.TITLE)) + " " +
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.EPISODE_LINK)) + " " +
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DATE)) + " " +
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DESCRIPTION)) + " " +
                        cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DOWNLOAD_LINK)));
                */
                cursor.moveToNext();
            } while(!cursor.isAfterLast());
        }

        cursor.close();

        Log.d("listDBsz (SERVICE): ", String.valueOf(listItem.size()));

        return listItem;
    }

    private BroadcastReceiver onDownloadCompleteEvent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(NotificationService.DOWNLOAD_COMPLETE)){

                // Exibição da notificação caso usuário não esteja com app em primeiro plano
                int mNotificationId = 001;
                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(mNotificationId, mBuilder.build());

                List<ItemFeed> listItem = getListFromDB();
                int listFromDBsize = listItem.size();

                Log.v("Downloaded (service): ", intent.getStringExtra("downloaded"));
                Log.v("listDBsz (service): ", String.valueOf(listFromDBsize));

                // Atualização do DB de modo similar ao feito na main activity
                File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File audioFile = new File(root, Uri.parse(intent.getStringExtra("downloaded")).getLastPathSegment());

                ContentValues contentValues = new ContentValues();
                String downloadLink = intent.getStringExtra("downloaded");

                String selection = PodcastProviderContract.DOWNLOAD_LINK + " =?";
                String[] selectionArgs = {downloadLink};

                Cursor cursor = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI,
                        null, selection, selectionArgs, null);

                if(cursor.moveToFirst()){
                    contentValues.put(PodcastProviderContract.TITLE,
                            cursor.getString(cursor.getColumnIndex(PodcastProviderContract.TITLE)));
                    contentValues.put(PodcastProviderContract.DATE,
                            cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DATE)));
                    contentValues.put(PodcastProviderContract.DESCRIPTION,
                            cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DESCRIPTION)));
                    contentValues.put(PodcastProviderContract.EPISODE_LINK,
                            cursor.getString(cursor.getColumnIndex(PodcastProviderContract.EPISODE_LINK)));
                    contentValues.put(PodcastProviderContract.DOWNLOAD_LINK, downloadLink);
                    contentValues.put(PodcastProviderContract.EPISODE_URI,
                            Uri.parse("file://" + audioFile.getAbsolutePath()).toString());
                    contentValues.put(PodcastProviderContract.AUDIO_STATE, String.valueOf(0));
                }

                int updated_rows = getContentResolver().update(PodcastProviderContract.EPISODE_LIST_URI, contentValues, selection, selectionArgs);
                Log.i("Atualizou (service): ", String.valueOf(updated_rows) + " item(s)");
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("Created: ", "NotificationService");

        mBuilder =
            new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Podcast")
                .setContentText("Download completed");

        resultIntent = new Intent(this, MainActivity.class);

        PendingIntent resultPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        mBuilder.setContentIntent(resultPendingIntent);

        IntentFilter f = new IntentFilter(DownloadService.DOWNLOAD_COMPLETE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onDownloadCompleteEvent, f);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onDownloadCompleteEvent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
        //return null;
    }
}
