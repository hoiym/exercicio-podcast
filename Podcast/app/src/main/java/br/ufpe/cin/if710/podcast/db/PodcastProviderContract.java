package br.ufpe.cin.if710.podcast.db;

import android.content.ContentResolver;
import android.net.Uri;

/**
 * Created by leopoldomt on 9/19/17.
 */

public class PodcastProviderContract {

    // Modificação nos valores da string para acessar os nomes das colunas
    // sem necessidade de chamar diretamente o helper
    public static final String _ID = "_id";
    public static final String TITLE = PodcastDBHelper.EPISODE_TITLE;
    public static final String DATE = PodcastDBHelper.EPISODE_DATE;
    public static final String DESCRIPTION = PodcastDBHelper.EPISODE_DESC;
    public static final String EPISODE_LINK = PodcastDBHelper.EPISODE_LINK;
    public static final String DOWNLOAD_LINK = PodcastDBHelper.EPISODE_DOWNLOAD_LINK;
    public static final String EPISODE_URI = PodcastDBHelper.EPISODE_FILE_URI;
    public static final String AUDIO_STATE = PodcastDBHelper.EPISODE_AUDIO_STATE;
    public static final String BUTTON_STATE = PodcastDBHelper.EPISODE_BUTTON_STATE;
    public static final String EPISODE_TABLE = "episodes";


    public final static String[] ALL_COLUMNS = {
            _ID, TITLE, DATE, EPISODE_LINK, DESCRIPTION, DOWNLOAD_LINK, EPISODE_URI,
            AUDIO_STATE, BUTTON_STATE};

    private static final Uri BASE_LIST_URI = Uri.parse("content://br.ufpe.cin.if710.podcast.feed/");
    //URI para tabela
    public static final Uri EPISODE_LIST_URI = Uri.withAppendedPath(BASE_LIST_URI, EPISODE_TABLE);

    // Mime type para colecao de itens
    public static final String CONTENT_DIR_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/PodcastProvider.data.text";

    // Mime type para um item especifico
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/PodcastProvider.data.text";

}