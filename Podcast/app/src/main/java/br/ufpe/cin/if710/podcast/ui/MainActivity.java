package br.ufpe.cin.if710.podcast.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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
import br.ufpe.cin.if710.podcast.db.PodcastDBHelper;
import br.ufpe.cin.if710.podcast.db.PodcastProvider;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.domain.XmlFeedParser;
import br.ufpe.cin.if710.podcast.ui.adapter.XmlFeedAdapter;

import static br.ufpe.cin.if710.podcast.db.PodcastDBHelper.columns;

public class MainActivity extends Activity {

    //ao fazer envio da resolucao, use este link no seu codigo!
    private final String RSS_FEED = "http://leopoldomt.com/if710/fronteirasdaciencia.xml";
    //TODO teste com outros links de podcast

    private ListView items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        items = (ListView) findViewById(R.id.items);
        updateListView();
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
        updateListView();
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
                                             cursor.getString(cursor.getColumnIndex(PodcastProviderContract.DOWNLOAD_LINK)));
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
                contentValues.put(PodcastProviderContract.EPISODE_URI, "-");

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
                    /*
                    Log.v("Insert item: ", String.valueOf(i));
                    Log.v("Details: ", contentValues.get(PodcastProviderContract.TITLE).toString() + " " +
                            contentValues.get(PodcastProviderContract.DATE).toString() + " " +
                            contentValues.get(PodcastProviderContract.DESCRIPTION).toString() + " " +
                            contentValues.get(PodcastProviderContract.DOWNLOAD_LINK).toString());
                    */
                    Uri uri = getContentResolver().insert(PodcastProviderContract.EPISODE_LIST_URI, contentValues);
                }

                cursor.close();
            }

            updateListView();
            Log.i("Aqui", "");

            /* Log para verificação da quantidade de itens inseridos
            Cursor cursor = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                                                       null, null, null);
            Log.v("Count items: ", String.valueOf(cursor.getCount()));
            */
        }
    }

    // Atualização da listView de acordo com lista de items presentes no BD
    public void updateListView(){
        //Adapter Personalizado
        List<ItemFeed> update_list = getListFromDB();
        XmlFeedAdapter adapter = new XmlFeedAdapter(getApplicationContext(), R.layout.itemlista, update_list);

        items.setAdapter(adapter);
        items.setTextFilterEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(DownloadService.DOWNLOAD_COMPLETE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onDownloadCompleteEvent, f);

        List<ItemFeed> update_list = getListFromDB();
        int listFromDBsize = update_list.size();
        String title, date, description, link, downloadLink;
        Log.d("OLA: ", String.valueOf(listFromDBsize));
        Log.d("HEY: ", String.valueOf(items.getCount()));

        for(int j = 0; j < listFromDBsize; ++j){
            View v = items.getChildAt(j-items.getFirstVisiblePosition());
            View view = items.getAdapter().getView(j, v, items);

            Log.v("foo >", String.valueOf(j));

            if(view == null)
                return;
            else{
                Log.v("AQUI >", " AQUI");
                ContentValues contentValues = new ContentValues();
                title = ""; date = ""; description = ""; link = ""; downloadLink = "";

                if(update_list.get(j).getTitle() != null) title = update_list.get(j).getTitle();
                if(update_list.get(j).getPubDate() != null) date = update_list.get(j).getPubDate();
                if(update_list.get(j).getDescription() != null) description = update_list.get(j).getDescription();
                if(update_list.get(j).getLink() != null) link = update_list.get(j).getLink();
                if(update_list.get(j).getDownloadLink() != null) downloadLink = update_list.get(j).getDownloadLink();

                contentValues.put(PodcastProviderContract.TITLE, title);
                contentValues.put(PodcastProviderContract.DATE, date);
                contentValues.put(PodcastProviderContract.DESCRIPTION, description);
                contentValues.put(PodcastProviderContract.EPISODE_LINK, link);
                contentValues.put(PodcastProviderContract.DOWNLOAD_LINK, downloadLink);

                String selection = PodcastProviderContract.TITLE + " =? AND " + PodcastProviderContract.DATE + " =? AND " +
                        PodcastProviderContract.DESCRIPTION + " =? AND " + PodcastProviderContract.EPISODE_LINK + " =? AND " +
                        PodcastProviderContract.DOWNLOAD_LINK + " =?";
                String[] selectionArgs = {title, date, description, link, downloadLink};
                String[] columns = {PodcastProviderContract.TITLE, PodcastProviderContract.DATE,
                        PodcastProviderContract.DESCRIPTION, PodcastProviderContract.EPISODE_URI};

                Cursor cursor = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI,
                        columns, selection, selectionArgs, null);

                Log.v("Column count: ", String.valueOf(cursor.getColumnCount()));
                Log.v("EPISODE URI: ", String.valueOf(cursor.getColumnIndex(PodcastProviderContract.EPISODE_URI)));

                if (cursor.moveToFirst()) {
                    do {
                        StringBuilder sb = new StringBuilder();
                        int columnsQty = cursor.getColumnCount();
                        for (int idx=0; idx<columnsQty; ++idx) {
                            if(idx == 3) {
                                Log.i("URI: ", cursor.getString(idx));
                                Log.i("IS empty: ", String.valueOf(cursor.getString(idx).equals("-")));
                            }
                            if (idx == 3 && cursor.getString(idx).equals("-") == false) {
                                Log.i("On Resume: ", "enable button to play " + String.valueOf(j));
                                Button downloadButton = (Button) view.findViewById(R.id.item_action);
                                downloadButton.setText("start");
                                downloadButton.setEnabled(true);
                            }
                        }
                    } while (cursor.moveToNext());
                }

                cursor.close();
                /*
                if(cursor.getColumnIndex(PodcastProviderContract.EPISODE_URI) != -1 &&
                        !cursor.getString(cursor.getColumnIndex(PodcastProviderContract.EPISODE_URI)).equals("")){
                    Button downloadButton = (Button) findViewById(R.id.item_action);
                    downloadButton.setText("start");
                    downloadButton.setEnabled(true);
                }
                */
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onDownloadCompleteEvent);
    }

    private BroadcastReceiver onDownloadCompleteEvent=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent i) {
            Toast.makeText(ctxt, "Download finalizado!", Toast.LENGTH_LONG).show();

            List<ItemFeed> update_list = getListFromDB();
            int listFromDBsize = update_list.size();

            Log.v("Downloaded: ", i.getStringExtra("downloaded"));
            Log.v("BD List size: ", String.valueOf(listFromDBsize));

            // Recepção do broadcast, verifica download de qual item foi concluído
            for(int j = 0; j < listFromDBsize; ++j){
                Log.d("Enter ", "loop to update button " + String.valueOf(j));
                if(update_list.get(j).getDownloadLink().equals(i.getStringExtra("downloaded"))){
                    View v = items.getChildAt(j-items.getFirstVisiblePosition());
                    View view = items.getAdapter().getView(j, v, items);

                    if(view == null) {
                        Log.i("Receiver: ", "NULL VIEW");
                        return;
                    }
                    else{
                        Log.i("Receiver: ", "enable button to play " + String.valueOf(j));
                        Button downloadButton = (Button) view.findViewById(R.id.item_action);
                        downloadButton.setText("start");
                        downloadButton.setEnabled(true);

                        // Atualização do BD com informações da Uri do arquivo baixado
                        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File audioFile = new File(root, Uri.parse(i.getStringExtra("downloaded")).getLastPathSegment());

                        ContentValues contentValues = new ContentValues();
                        String title, date, description, link, downloadLink;
                        title = ""; date = ""; description = ""; link = ""; downloadLink = "";

                        if(update_list.get(j).getTitle() != null) title = update_list.get(j).getTitle();
                        if(update_list.get(j).getPubDate() != null) date = update_list.get(j).getPubDate();
                        if(update_list.get(j).getDescription() != null) description = update_list.get(j).getDescription();
                        if(update_list.get(j).getLink() != null) link = update_list.get(j).getLink();
                        if(update_list.get(j).getDownloadLink() != null) downloadLink = update_list.get(j).getDownloadLink();

                        contentValues.put(PodcastProviderContract.TITLE, title);
                        contentValues.put(PodcastProviderContract.DATE, date);
                        contentValues.put(PodcastProviderContract.DESCRIPTION, description);
                        contentValues.put(PodcastProviderContract.EPISODE_LINK, link);
                        contentValues.put(PodcastProviderContract.DOWNLOAD_LINK, downloadLink);
                        contentValues.put(PodcastProviderContract.EPISODE_URI,
                                Uri.parse("file://" + audioFile.getAbsolutePath()).toString());

                        String selection = PodcastProviderContract.TITLE + " =? AND " + PodcastProviderContract.DATE + " =? AND " +
                                PodcastProviderContract.DESCRIPTION + " =? AND " + PodcastProviderContract.EPISODE_LINK + " =? AND " +
                                PodcastProviderContract.DOWNLOAD_LINK + " =?";
                        String[] selectionArgs = {title, date, description, link, downloadLink};

                        getContentResolver().update(PodcastProviderContract.EPISODE_LIST_URI, contentValues, selection, selectionArgs);
                    }

                    break;
                }
            }
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
