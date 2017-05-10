package paru.com.loktra.data;

import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import paru.com.loktra.LoadData;

/**
 * Created by parvendan on 11/05/17.
 */

public class LoadDataIntoMap extends AsyncTask<Void, Void, List<Loktra>> {
    private DbHelper mHelper;

    private LoadData loadData;

    public LoadDataIntoMap(Context context, LoadData loadData) {
        mHelper = DbHelper.getInstance(context);
        this.loadData = loadData;
    }

    @Override
    protected List<Loktra> doInBackground(Void... params) {
        List<Loktra> loktra = mHelper.getAllLatAndLong();
        mHelper.deleteAllLocationData();
        return loktra;
    }

    @Override
    protected void onPostExecute(List<Loktra> loktras) {
        loadData.onSuccess(loktras);
        super.onPostExecute(loktras);
    }
}
