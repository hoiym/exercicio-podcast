package br.ufpe.cin.if710.podcast.ui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;

public class EpisodeDetailActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode_detail);

        ItemFeed item = (ItemFeed) getIntent().getSerializableExtra("podcastItem");

        // Log de debug para verificação de item entregue a esta activity
        // Log.i("Details Title: ", item.getTitle().toString());

        // Tratamento das possíveis strings com valor null
        String title_text = (item.getTitle() == null ? "-" : item.getTitle());
        String date_text = (item.getPubDate() == null ? "-" : item.getPubDate());
        String description_text = (item.getDescription() == null ? "-" : item.getDescription());
        String link_text = (item.getLink() == null ? "-" : item.getLink());
        String downloadLink_text = (item.getDownloadLink() == null ? "-" : item.getDownloadLink());

        // Ajuste nos text views que irão disponibilizar as informações do podcast
        TextView title_tv = (TextView) findViewById(R.id.episode_detail_title);
        title_tv.setText(">>> Title:\n" + title_text);
        TextView date_tv = (TextView) findViewById(R.id.episode_detail_date);
        date_tv.setText(">>> Date:\n" + date_text);
        TextView description_tv = (TextView) findViewById(R.id.episode_detail_description);
        description_tv.setText(">>> Description:\n" + description_text);
        TextView link_tv = (TextView) findViewById(R.id.episode_detail_link);
        link_tv.setText(">>> Link:\n" + link_text);
        TextView downloadLink_tv = (TextView) findViewById(R.id.episode_detail_downloadLink);
        downloadLink_tv.setText(">>> Download Link:\n" + downloadLink_text);
    }
}
