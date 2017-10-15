package br.ufpe.cin.if710.podcast.ui;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import br.ufpe.cin.if710.podcast.R;
import br.ufpe.cin.if710.podcast.services.UpdateListService;

public class SettingsActivity extends Activity {
    public static final String FEED_LINK = "feedlink";
    public static final String JOB_TIME = "jobTime";

    // Visto que tempo é em ms, usar conversão adequada
    private static long INTERVAL = 60000;
    private static final int JOB_ID = 710;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    public static class FeedPreferenceFragment extends PreferenceFragment {

        protected static final String TAG = "FeedPreferenceFragment";
        private SharedPreferences.OnSharedPreferenceChangeListener mListener;
        private Preference feedLinkPref;
        private Preference jobTimePref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Log.d("(SettingsActivity):", "Fragment Criado");

            // carrega preferences de um recurso XML em /res/xml
            addPreferencesFromResource(R.xml.preferences);

            // pega o valor atual de FeedLink
            feedLinkPref = (Preference) getPreferenceManager().findPreference(FEED_LINK);
            // pega o valor do intervalo desejado para atualização
            jobTimePref = (Preference) getPreferenceManager().findPreference(JOB_TIME);

            // cria listener para atualizar summary ao modificar link do feed
            // mesmo listener é aplicado ao tempo de atualização caso modificado
            mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    // Modificação da preference de acordo com a key
                    if(key == FEED_LINK) {
                        feedLinkPref.setSummary(sharedPreferences.getString(FEED_LINK,
                                getActivity().getResources().getString(R.string.feed_link)));
                    } else if(key == JOB_TIME) {
                        jobTimePref.setSummary(sharedPreferences.getString(JOB_TIME,
                                getActivity().getResources().getString(R.string.job_time)));
                    }
                }
            };

            // pega objeto SharedPreferences gerenciado pelo PreferenceManager deste fragmento
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

            // registra o listener no objeto SharedPreferences
            prefs.registerOnSharedPreferenceChangeListener(mListener);

            // força chamada ao metodo de callback para exibir link atual
            mListener.onSharedPreferenceChanged(prefs, FEED_LINK);
            // Equivalente ao feito acima para o tempo de atualização
            mListener.onSharedPreferenceChanged(prefs, JOB_TIME);
        }

        @Override
        public void onStop(){
            // Realiza agendamento quando sai deste fragment
            Log.d("(SettingsActivity):", "Prepara agendamento onStop");

            agendarJob();
            super.onStop();
        }

        public void agendarJob() {
            SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
            // Obtem link do feed setado pelo usuário
            String feedLink = sharedPreferences.getString(FEED_LINK,
                    getActivity().getResources().getString(R.string.feed_link));
            // Obtem tempo de atualização, realizando conversão
            long interval_ms = Long.parseLong(sharedPreferences.getString(JOB_TIME,
                    getActivity().getResources().getString(R.string.job_time))) * INTERVAL;
            JobInfo.Builder b = new JobInfo.Builder(JOB_ID, new ComponentName(getActivity(),
                    UpdateListService.class));
            PersistableBundle pb=new PersistableBundle();
            pb.putString("downloadURL", feedLink);
            // Link para fazer download da nova lista no UpdateListService
            b.setExtras(pb);
            //criterio de rede
            b.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            //define intervalo de periodicidade, obtido a partir de info provida pelo usuário
            b.setPeriodic(interval_ms);
            //exige (ou nao) que esteja conectado ao carregador
            b.setRequiresCharging(false);
            //persiste (ou nao) job entre reboots
            //se colocar true, tem que solicitar permissao action_boot_completed
            b.setPersisted(false);
            //exige (ou nao) que dispositivo esteja idle
            b.setRequiresDeviceIdle(false);

            Log.d("(SettingsActivity):", "Job agendado para cada (ms): " +
                    String.valueOf(interval_ms));

            JobScheduler jobScheduler = (JobScheduler) getActivity().getSystemService(JOB_SCHEDULER_SERVICE);
            // Agendamento do job que atualiza lista do feed
            jobScheduler.schedule(b.build());
        }
    }
}