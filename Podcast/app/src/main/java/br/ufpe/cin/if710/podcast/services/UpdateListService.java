package br.ufpe.cin.if710.podcast.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.domain.XmlFeedParser;

/**
 * Created by Hoi on 15/10/2017.
 */

public class UpdateListService extends JobService {

    // Utiliza mesmo método da MainActivity para obter feed
    private String getRssFeed(String feed) throws IOException {
        InputStream in = null;
        String rssFeed = "";
        try {
            URL url = new URL(feed);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int count; (count = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, count);
            }
            byte[] response = out.toByteArray();
            rssFeed = new String(response, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return rssFeed;
    }

    // Link: https://stackoverflow.com/questions/6343166/how-do-i-fix-android-os-networkonmainthreadexception
    // Necessário usar AsyncTask para estabelecer conexão para obtenção do feed
    private class ULSDownloadXmlTask extends AsyncTask<String, Void, List<ItemFeed>> {
        @Override
        protected void onPreExecute() {}

        @Override
        protected List<ItemFeed> doInBackground(String... params) {
            List<ItemFeed> itemList = new ArrayList<>();
            try {
                itemList = XmlFeedParser.parse(getRssFeed(params[0]));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            return itemList;
        }

        @Override
        protected void onPostExecute(List<ItemFeed> feed) {
            String title, date, description, link, downloadLink;
            int feedSz = feed.size();
            boolean updated = false;

            for(int i = 0; i < feedSz; ++i){
                ContentValues contentValues = new ContentValues();
                title = ""; date = ""; description = ""; link = ""; downloadLink = "";

                // Caso contrário, inserir a própria string
                if(feed.get(i).getTitle() != null) title = feed.get(i).getTitle();
                if(feed.get(i).getPubDate() != null) date = feed.get(i).getPubDate();
                if(feed.get(i).getDescription() != null) description = feed.get(i).getDescription();
                if(feed.get(i).getLink() != null) link = feed.get(i).getLink();
                if(feed.get(i).getDownloadLink() != null) downloadLink = feed.get(i).getDownloadLink();

                contentValues.put(PodcastProviderContract.TITLE, title);
                contentValues.put(PodcastProviderContract.DATE, date);
                contentValues.put(PodcastProviderContract.DESCRIPTION, description);
                contentValues.put(PodcastProviderContract.EPISODE_LINK, link);
                contentValues.put(PodcastProviderContract.DOWNLOAD_LINK, downloadLink);
                contentValues.put(PodcastProviderContract.EPISODE_URI, "");
                contentValues.put(PodcastProviderContract.AUDIO_STATE, String.valueOf(0));
                contentValues.put(PodcastProviderContract.BUTTON_STATE, String.valueOf(0));

                String[] columns = {PodcastProviderContract.TITLE, PodcastProviderContract.DATE,
                        PodcastProviderContract.DESCRIPTION};
                String selection = PodcastProviderContract.TITLE + " =? AND " + PodcastProviderContract.DATE + " =? AND " +
                        PodcastProviderContract.DESCRIPTION + " =? AND " + PodcastProviderContract.EPISODE_LINK + " =? AND " +
                        PodcastProviderContract.DOWNLOAD_LINK + " =?";
                String[] selectionArgs = {title, date, description, link, downloadLink};

                Cursor cursor = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI,
                        columns, selection, selectionArgs, null);

                if(cursor.getCount() == 0) {
                    // Houve modificação
                    updated = true;
                    // Inserção no banco caso não seja encontrado neste
                    getContentResolver().insert(PodcastProviderContract.EPISODE_LIST_URI, contentValues);
                }

                cursor.close();
            }

            // Envio de broadcast avisando sbore atualização da lista, para possível
            // notificação quando app está no background apenas no caso de haver algo novo
            if(updated) {
                Intent finishBroadcast = new Intent(MusicPlayerService.UPDATE_LIST);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(finishBroadcast);
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // Usa AyncTask para obter feed
        new ULSDownloadXmlTask().execute(params.getExtras().getString("downloadURL"));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
