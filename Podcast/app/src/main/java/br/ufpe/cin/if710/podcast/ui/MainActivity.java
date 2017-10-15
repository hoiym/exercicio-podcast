package br.ufpe.cin.if710.podcast.ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.domain.XmlFeedParser;
import br.ufpe.cin.if710.podcast.services.DownloadService;
import br.ufpe.cin.if710.podcast.services.MusicPlayerService;
import br.ufpe.cin.if710.podcast.services.NotificationService;
import br.ufpe.cin.if710.podcast.ui.adapter.XmlFeedAdapter;

public class MainActivity extends Activity {

    //ao fazer envio da resolucao, use este link no seu codigo!
    private final String RSS_FEED = "http://leopoldomt.com/if710/fronteirasdaciencia.xml";
    //TODO teste com outros links de podcast

    private ListView items;
    Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        serviceIntent = new Intent(this, NotificationService.class);

        items = (ListView) findViewById(R.id.items);

        // Solicitação de permissão
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new DownloadXmlTask().execute(RSS_FEED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        XmlFeedAdapter adapter = (XmlFeedAdapter) items.getAdapter();
        adapter.clear();
    }

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
                cursor.moveToNext();
            } while(!cursor.isAfterLast());
        }

        cursor.close();

        Log.d("Size of list from db: ", String.valueOf(listItem.size()));

        return listItem;
    }

    private class DownloadXmlTask extends AsyncTask<String, Void, List<ItemFeed>> {
        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "iniciando...", Toast.LENGTH_SHORT).show();
        }

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
            Toast.makeText(getApplicationContext(), "terminando...", Toast.LENGTH_SHORT).show();
            String title, date, description, link, downloadLink;

            int feedSz = feed.size();

            Log.v("Feed size: ", String.valueOf(feedSz));

            for(int i = 0; i < feedSz; ++i){
                ContentValues contentValues = new ContentValues();

                // Caso seja lido algum atributo nulo, inserir no banco como string vazia
                title = ""; date = ""; description = ""; link = ""; downloadLink = "";

                // Caso contrário, inserir a própria string
                if(feed.get(i).getTitle() != null) title = feed.get(i).getTitle();
                if(feed.get(i).getPubDate() != null) date = feed.get(i).getPubDate();
                if(feed.get(i).getDescription() != null) description = feed.get(i).getDescription();
                if(feed.get(i).getLink() != null) link = feed.get(i).getLink();
                if(feed.get(i).getDownloadLink() != null) downloadLink = feed.get(i).getDownloadLink();

                /* Log de verificação do item sendo inserido
                Log.i("Main: ", String.valueOf(i) + "\n" + title + "\n" + date + "\n" +
                        description + "\n" + link + "\n" + downloadLink);
                */

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
                    // Inserção no banco caso não seja encontrado neste

                    Log.v("Insert item: ", String.valueOf(i));
                    Log.v("Details: ", contentValues.get(PodcastProviderContract.TITLE).toString() + " @ " +
                            contentValues.get(PodcastProviderContract.DATE).toString() + " @ " +
                            contentValues.get(PodcastProviderContract.DESCRIPTION).toString() + " @ " +
                            contentValues.get(PodcastProviderContract.DOWNLOAD_LINK).toString() + " @ " +
                            contentValues.get(PodcastProviderContract.EPISODE_URI));

                    getContentResolver().insert(PodcastProviderContract.EPISODE_LIST_URI, contentValues);
                }

                cursor.close();
            }

            // Envio de broadcast avisando sbore atualização da lista, para possível
            // notificação quando app está no background
            Intent finishBroadcast = new Intent(MusicPlayerService.UPDATE_LIST);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(finishBroadcast);
        }
    }

    // Atualização da listView de acordo com lista de items presentes no BD
    public void updateListView(){
        //Adapter Personalizado
        List<ItemFeed> update_list = getListFromDB();

        if(update_list.size() > 1) {
            XmlFeedAdapter adapter = new XmlFeedAdapter(getApplicationContext(), R.layout.itemlista, update_list);

            items.setAdapter(adapter);
            items.setTextFilterEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(DownloadService.DOWNLOAD_COMPLETE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onDownloadCompleteEvent, f);
        IntentFilter g = new IntentFilter(MusicPlayerService.UPDATE_LIST);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onUpdateListEvent, g);
        stopService(serviceIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onDownloadCompleteEvent);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onUpdateListEvent);
        startService(serviceIntent);
    }

    private BroadcastReceiver onDownloadCompleteEvent=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {
            Toast.makeText(ctxt, "Download finalizado!", Toast.LENGTH_LONG).show();

            List<ItemFeed> update_list = getListFromDB();
            int listFromDBsize = update_list.size();

            Log.v("Downloaded: ", i.getStringExtra("downloaded"));
            Log.v("BD List size: ", String.valueOf(listFromDBsize));

            // Recepção do broadcast, verifica download de qual item foi concluído e atualiza BD com Uri do podcast
            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File audioFile = new File(root, Uri.parse(i.getStringExtra("downloaded")).getLastPathSegment());

            ContentValues contentValues = new ContentValues();
            String downloadLink = i.getStringExtra("downloaded");

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
                contentValues.put(PodcastProviderContract.BUTTON_STATE, String.valueOf(2));
            }

            int updated_rows = getContentResolver().update(PodcastProviderContract.EPISODE_LIST_URI,
                    contentValues, selection, selectionArgs);

            Log.i("Atualizou : ", String.valueOf(updated_rows) + " item(s)");
            updateListView();
        }
    };

    private BroadcastReceiver onUpdateListEvent=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {
            updateListView();
        }
    };

    //TODO Opcional - pesquise outros meios de obter arquivos da internet
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
}
