package br.ufpe.cin.if710.podcast.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;

import br.ufpe.cin.if710.podcast.db.PodcastProvider;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;

// Código da aula de services do professor Leopoldo

public class MusicPlayerService extends Service {
    private final String TAG = "MusicPlayerService";
    public static final String UPDATE_LIST = "br.ufpe.cin.if710.services.action.UPDATE_LIST";

    private MediaPlayer mPlayer;
    private int mStartID;
    ItemFeed item;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        item = (ItemFeed) intent.getSerializableExtra("podcastItem");
        mPlayer = MediaPlayer.create(this, Uri.parse(item.getUri()));
        mPlayer.seekTo(Integer.parseInt(item.getAudioState()));

        Log.d("MPService: ", "Started MP // Time: " + item.getAudioState());

        if (null != mPlayer) {
            //nao deixa entrar em loop
            mPlayer.setLooping(false);

            // encerrar o service quando terminar a musica
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    // Deletar item da memória quando concluir
                    File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File audioFile = new File(root, Uri.parse(item.getDownloadLink()).getLastPathSegment());
                    boolean deleted = audioFile.delete();

                    if(deleted) {
                        ContentValues contentValues = new ContentValues();
                        String downloadLink = item.getDownloadLink();
                        String selection = PodcastProviderContract.DOWNLOAD_LINK + " =?";
                        String[] selectionArgs = {downloadLink};

                        contentValues.put(PodcastProviderContract.TITLE, item.getTitle());
                        contentValues.put(PodcastProviderContract.DATE, item.getPubDate());
                        contentValues.put(PodcastProviderContract.DESCRIPTION, item.getDescription());
                        contentValues.put(PodcastProviderContract.EPISODE_LINK, item.getLink());
                        contentValues.put(PodcastProviderContract.DOWNLOAD_LINK, downloadLink);
                        contentValues.put(PodcastProviderContract.EPISODE_URI, "");
                        contentValues.put(PodcastProviderContract.AUDIO_STATE, String.valueOf(0));
                        contentValues.put(PodcastProviderContract.BUTTON_STATE, "0");

                        int updated_rows = getContentResolver().update(PodcastProviderContract.EPISODE_LIST_URI,
                                contentValues, selection, selectionArgs);
                    }

                    stopSelf(mStartID);
                }
            });
        }

        if (null != mPlayer) {
            mStartID = startId;
            mPlayer.start();
        }
        // nao reinicia service automaticamente se for eliminado
        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        if (null != mPlayer) {
            // Salvar posição da pausa para continuar reprodução na próxima solicitação
            int current_audio_state = mPlayer.getCurrentPosition();
            Log.d("MPService: ", "Stop MP // Time: " + String.valueOf(current_audio_state));

            ContentValues contentValues = new ContentValues();
            String downloadLink = item.getDownloadLink();
            String selection = PodcastProviderContract.DOWNLOAD_LINK + " =?";
            String[] selectionArgs = {downloadLink};

            contentValues.put(PodcastProviderContract.TITLE, item.getTitle());
            contentValues.put(PodcastProviderContract.DATE, item.getPubDate());
            contentValues.put(PodcastProviderContract.DESCRIPTION, item.getDescription());
            contentValues.put(PodcastProviderContract.EPISODE_LINK, item.getLink());
            contentValues.put(PodcastProviderContract.DOWNLOAD_LINK, downloadLink);
            contentValues.put(PodcastProviderContract.EPISODE_URI, item.getUri());
            contentValues.put(PodcastProviderContract.AUDIO_STATE, String.valueOf(current_audio_state));
            contentValues.put(PodcastProviderContract.BUTTON_STATE, "2");

            int updated_rows = getContentResolver().update(PodcastProviderContract.EPISODE_LIST_URI,
                    contentValues, selection, selectionArgs);

            Log.i("Atualizou (MPservice): ", String.valueOf(updated_rows) + " item(s)");

            // Broadcast sobre audio pausado para atualizar listView
            Intent finishBroadcast = new Intent(UPDATE_LIST);
            LocalBroadcastManager.getInstance(this).sendBroadcast(finishBroadcast);

            mPlayer.stop();
            mPlayer.release();
        }

        super.onDestroy();
    }

    //nao eh possivel fazer binding com este service
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
        //return null;
    }
}