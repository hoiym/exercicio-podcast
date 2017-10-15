package br.ufpe.cin.if710.podcast.ui.adapter;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.db.PodcastProviderContract;
import br.ufpe.cin.if710.podcast.domain.ItemFeed;
import br.ufpe.cin.if710.podcast.services.DownloadService;
import br.ufpe.cin.if710.podcast.ui.EpisodeDetailActivity;
import br.ufpe.cin.if710.podcast.services.MusicPlayerService;

public class XmlFeedAdapter extends ArrayAdapter<ItemFeed> {

    int linkResource;
    Context adapterContext;
    MediaPlayer mPlayer;
    List<ItemFeed> items;

    public XmlFeedAdapter(Context context, int resource, List<ItemFeed> objects) {
        super(context, resource, objects);
        linkResource = resource;
        adapterContext = context;
        objects = items;
    }

    /**
     * public abstract View getView (int position, View convertView, ViewGroup parent)
     * <p>
     * Added in API level 1
     * Get a View that displays the data at the specified position in the data set. You can either create a View manually or inflate it from an XML layout file. When the View is inflated, the parent View (GridView, ListView...) will apply default layout parameters unless you use inflate(int, android.view.ViewGroup, boolean) to specify a root view and to prevent attachment to the root.
     * <p>
     * Parameters
     * position	The position of the item within the adapter's data set of the item whose view we want.
     * convertView	The old view to reuse, if possible. Note: You should check that this view is non-null and of an appropriate type before using. If it is not possible to convert this view to display the correct data, this method can create a new view. Heterogeneous lists can specify their number of view types, so that this View is always of the right type (see getViewTypeCount() and getItemViewType(int)).
     * parent	The parent that this view will eventually be attached to
     * Returns
     * A View corresponding to the data at the specified position.
     */


	/*
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.itemlista, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.item_title);
		textView.setText(items.get(position).getTitle());
	    return rowView;
	}
	/**/

    //http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder
    static class ViewHolder {
        TextView item_title;
        TextView item_date;
        Button item_action;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final Intent musicPlayerService = new Intent(adapterContext, MusicPlayerService.class);;

        if (convertView == null) {
            convertView = View.inflate(getContext(), linkResource, null);
            holder = new ViewHolder();
            holder.item_title = (TextView) convertView.findViewById(R.id.item_title);
            holder.item_date = (TextView) convertView.findViewById(R.id.item_date);
            holder.item_action = (Button) convertView.findViewById(R.id.item_action);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                // Logs de debug
                Log.v("Adapter Pos: ", String.valueOf(position));
                Log.i("Atapter Title: ", getItem(position).getTitle().toString());
                Intent intent = new Intent(adapterContext, EpisodeDetailActivity.class);
                // Inserção do extra para ser obtido na activity de EpisodeDetailActivity
                intent.putExtra("podcastItem", getItem(position));
                // Adição de flag para chamar nova activity fora de uma activity
                intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
                adapterContext.startActivity(intent);
            }
        });

        final ItemFeed current_item = getItem(position);

        // FSM do botão do item
        if(current_item.getButtonState().toString().equals("0")) {
            holder.item_action.setText("baixar");
            holder.item_action.setEnabled(true);
        } else if(current_item.getButtonState().toString().equals("1")) {
            holder.item_action.setText("baixar");
            holder.item_action.setEnabled(false);
        } else if(current_item.getButtonState().toString().equals("2")) {
            holder.item_action.setText("play");
            holder.item_action.setEnabled(true);
        } else{
            holder.item_action.setText("pause");
            holder.item_action.setEnabled(true);
        }

        // Detecção de click no botão de download
        holder.item_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("Adapter Item clicked: ", String.valueOf(position));

                // Configuração do botão para executar / pausar / continuar áudio
                if(holder.item_action.isEnabled()) {
                    ContentValues contentValues = new ContentValues();
                    String downloadLink = current_item.getDownloadLink();
                    String selection = PodcastProviderContract.DOWNLOAD_LINK + " =?";
                    String[] selectionArgs = {downloadLink};

                    contentValues.put(PodcastProviderContract.TITLE, current_item.getTitle());
                    contentValues.put(PodcastProviderContract.DATE, current_item.getPubDate());
                    contentValues.put(PodcastProviderContract.DESCRIPTION, current_item.getDescription());
                    contentValues.put(PodcastProviderContract.EPISODE_LINK, current_item.getLink());
                    contentValues.put(PodcastProviderContract.DOWNLOAD_LINK, downloadLink);
                    contentValues.put(PodcastProviderContract.EPISODE_URI, current_item.getUri());
                    contentValues.put(PodcastProviderContract.AUDIO_STATE, current_item.getAudioState());

                    if (holder.item_action.getText().toString().equals("baixar")) {
                        // Baixar arquivo caso ainda esteja baixado
                        Intent downloadService = new Intent(adapterContext, DownloadService.class);
                        downloadService.setData(Uri.parse(current_item.getDownloadLink()));
                        contentValues.put(PodcastProviderContract.BUTTON_STATE, "1");

                        Log.v("CALLING: ", current_item.getDownloadLink());
                        downloadService.addFlags(downloadService.FLAG_ACTIVITY_NEW_TASK);
                        adapterContext.startService(downloadService);
                    } else if (holder.item_action.getText().toString().equals("play")) {
                        // Executar áudio caso já tenha sido baixado
                        Log.d("Adapter (audio): ", getItem(position).getAudioState());
                        contentValues.put(PodcastProviderContract.BUTTON_STATE, "3");
                        musicPlayerService.putExtra("podcastItem", getItem(position));
                        adapterContext.startService(musicPlayerService);
                    } else {
                        // Encerrar service caso pausado
                        contentValues.put(PodcastProviderContract.BUTTON_STATE, "2");
                        adapterContext.stopService(musicPlayerService);
                    }

                    // Atualização do estado do botão
                    adapterContext.getContentResolver().update(PodcastProviderContract.EPISODE_LIST_URI,
                            contentValues, selection, selectionArgs);

                    // Envio de broadcast para modificação do adapter com estado do botão modificado
                    Intent finishBroadcast = new Intent(MusicPlayerService.UPDATE_LIST);
                    LocalBroadcastManager.getInstance(adapterContext).sendBroadcast(finishBroadcast);
                }
            }
        });

        holder.item_title.setText(current_item.getTitle());
        holder.item_date.setText(current_item.getPubDate());
        return convertView;
    }

    // Override dos 2 métodos abaixo para evitar troca de conteúdo da ListView ao navegar neste
    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}