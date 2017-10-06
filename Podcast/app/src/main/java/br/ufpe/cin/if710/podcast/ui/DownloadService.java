package br.ufpe.cin.if710.podcast.ui;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// Código da aula de services do professor Leopoldo

public class DownloadService extends IntentService {

    public static final String DOWNLOAD_COMPLETE = "br.ufpe.cin.if710.services.action.DOWNLOAD_COMPLETE";


    public DownloadService() {
        super("DownloadService");
    }

    @Override
    public void onHandleIntent(Intent i) {
        try {
            //checar se tem permissao... Android 6.0+
            Log.v("Begin:", "DOWNLOAD SERVICE");
            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            root.mkdirs();
            File output = new File(root, i.getData().getLastPathSegment());
            if (output.exists()) {
                output.delete();
            }
            URL url = new URL(i.getData().toString());

            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            FileOutputStream fos = new FileOutputStream(output.getPath());
            BufferedOutputStream out = new BufferedOutputStream(fos);
            try {
                InputStream in = c.getInputStream();
                byte[] buffer = new byte[8192];
                int len = 0;
                while ((len = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
            finally {
                fos.getFD().sync();
                out.close();
                c.disconnect();
            }

            Intent finishBroadcast = new Intent(DOWNLOAD_COMPLETE);

            // Adição do link de download para identificar qual botão deverá ser modificado
            finishBroadcast.putExtra("downloaded", i.getData().toString());
            // Envio de broadcast para notificar conclusão de download
            LocalBroadcastManager.getInstance(this).sendBroadcast(finishBroadcast);
            Log.d("Finished ", "download");

        } catch (IOException e2) {
            Log.e(getClass().getName(), "Exception durante download", e2);
        }
    }
}