package br.ufpe.cin.if710.podcast.db;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class PodcastProvider extends ContentProvider {
    private PodcastDBHelper pdbh;

    public PodcastProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.

        if (uri.getLastPathSegment().equals(PodcastProviderContract.EPISODE_TABLE)) {
            int numRowsDeleted = pdbh.getWritableDatabase().delete(PodcastProviderContract.EPISODE_TABLE, selection, selectionArgs);
            return numRowsDeleted;
        } else {
            return 0;
        }
    }

    @Override
    public String getType(Uri uri) {
        if(uri.getLastPathSegment().equals(PodcastProviderContract.EPISODE_TABLE)) {
            // Acesso ao modelo do conteúdo
            ContentResolver contentResolver = getContext().getContentResolver();
            // Acesso ao mapa que mapeia extensões para mime e vice-versa
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            // Obtenção do mimeType da uri provida
            String mimeType = mime.getExtensionFromMimeType(contentResolver.getType(uri));

            return mimeType;
        } else {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if(uri.getLastPathSegment().equals(PodcastProviderContract.EPISODE_TABLE)) {
            // Inserção da nova linha de values, obtendo chave primária da nova linha
            long newRowId = pdbh.getWritableDatabase().insert(PodcastDBHelper.DATABASE_TABLE, null, values);
            // Uri novo com adição da nova linha
            Uri idUri = Uri.withAppendedPath(PodcastProviderContract.EPISODE_LIST_URI, String.valueOf(newRowId));

            return idUri;
        } else {
            return null;
        }
    }

    @Override
    public boolean onCreate() {
        // Obtenção da instancia to helper
        pdbh = PodcastDBHelper.getInstance(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if(uri.getLastPathSegment().equals(PodcastProviderContract.EPISODE_TABLE)){
            // Obtenção do cursor dos elementos que satisfazem a query
            Cursor cursor = pdbh.getReadableDatabase().query(PodcastDBHelper.DATABASE_TABLE,
                                                             projection, selection, selectionArgs,
                                                             null, null, sortOrder);

            return cursor;
        } else {
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if(uri.getLastPathSegment().equals(PodcastProviderContract.EPISODE_TABLE)){
            int numRowsUpdated = pdbh.getWritableDatabase().update(PodcastDBHelper.DATABASE_TABLE,
                                                                   values, selection, selectionArgs);

            return numRowsUpdated;
        } else {
            return 0;
        }
    }
}
