package paru.com.loktra.data;

import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by parvendan on 11/05/17.
 */

public class SaveLatAndLong extends AsyncTask<Void, Void, Void> {
    private Double latitude;
    private Double longitude;
    private DbHelper mHelper;

    public SaveLatAndLong(Context context, Double latitude, Double logtitude) {
        this.latitude = latitude;
        this.longitude = logtitude;
        mHelper = DbHelper.getInstance(context);
    }

    @Override
    protected Void doInBackground(Void... params) {
        mHelper.addDataToDatabase(latitude, longitude);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mHelper != null) {
            mHelper.close();
            mHelper = null;
        }
        super.onPostExecute(aVoid);
    }
}