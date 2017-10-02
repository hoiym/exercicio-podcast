package br.ufpe.cin.if710.podcast.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
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

                Uri uri = getContentResolver().insert(PodcastProviderContract.EPISODE_LIST_URI, contentValues);
            }

            // Código para verificação da quantidade de itens inseridos
            /*
            Cursor cursor = getContentResolver().query(PodcastProviderContract.EPISODE_LIST_URI, null,
                                                       null, null, null);
            int numItems = 0;

            if(cursor != null){
                while(cursor.moveToNext()){
                    numItems++;
                }
            }

            Toast.makeText(getApplicationContext(), String.valueOf(numItems), Toast.LENGTH_LONG);
            */

            //Adapter Personalizado
            XmlFeedAdapter adapter = new XmlFeedAdapter(getApplicationContext(), R.layout.itemlista, feed);

            //atualizar o list view
            items.setAdapter(adapter);
            items.setTextFilterEnabled(true);
            /*
            items.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    XmlFeedAdapter adapter = (XmlFeedAdapter) parent.getAdapter();
                    ItemFeed item = adapter.getItem(position);
                    String msg = item.getTitle() + " " + item.getLink();
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
            */
        }
    }

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
