package paru.com.loktra.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parvendan on 11/05/17.
 */

public class DbHelper extends SQLiteOpenHelper {

    private static final String TABLE_NAME = " loktra";
    private static final String LATITUDE = "lati";
    private static final String LONGTITUDE = "long";
    public static final String ID_COL = "id";
    public static final String DATABASE_NAME = "loktra.db";
    private static final String TAG = DbHelper.class.getSimpleName();
    static final int DATABASE_VERSION = 1;
    private static DbHelper mDbInstance;
    private Context mContext;

    public static final String SQL_CREATE_MAP_DATA = "CREATE TABLE " + TABLE_NAME + " (" +
            ID_COL + " INTEGER PRIMARY KEY, " +
            LATITUDE + " TEXT, " +
            LONGTITUDE + " TEXT);)";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    public synchronized static DbHelper getInstance(Context context) {
        if (mDbInstance == null) {
            mDbInstance = new DbHelper(context);
        }
        return mDbInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MAP_DATA);
        Log.i(TAG, "All tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        Log.i(TAG, "On upgrade called, Current DB version: " + DATABASE_VERSION);
        Log.i(TAG, "All tables dropped");
        onCreate(db);
    }

    public synchronized List<Loktra> getAllLatAndLong() {
        SQLiteDatabase dataBase = null;
        List<Loktra> latAndLog = new ArrayList<>();
        Cursor lCursor = null;
        try {
            dataBase = this.getWritableDatabase();
            String latAndLongList = "SELECT * FROM " + TABLE_NAME;
            lCursor = dataBase.rawQuery(latAndLongList, null);
            lCursor.moveToFirst();
            if (lCursor != null && lCursor.moveToFirst()) {
                do {
                    Loktra ll = new Loktra();
                    ll.setLati(lCursor.getDouble(lCursor.getColumnIndex(LATITUDE)));
                    ll.setLongtitude(lCursor.getDouble(lCursor.getColumnIndex(LONGTITUDE)));
                    latAndLog.add(ll);
                } while (lCursor.moveToNext());

            }
            Log.e(TAG, "get allLat and long: " + latAndLog);
            return latAndLog;

        } catch (SQLiteException e) {
            Log.e(TAG, "get allLat and long: : " + e.getMessage());
        } finally {
            if (lCursor != null) {
                lCursor.close();
                lCursor = null;
            }
        }
        return null;
    }

    /**
     * @param latitude
     */
    public synchronized void addDataToDatabase(Double latitude, Double logtitude) {
        SQLiteDatabase dataBase = null;
        try {
            dataBase = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(LATITUDE, latitude);
            values.put(LONGTITUDE, logtitude);
            dataBase.insert(TABLE_NAME, null, values);
            Log.e(TAG, "Added to DataBase" + values);
        } catch (SQLiteException e) {
            Log.e(TAG, "addDataToDatabase: database cannot be opened: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "addDataToDatabase: toString exception");
        }
    }

    public synchronized void deleteAllLocationData() {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            String sql = " DELETE FROM " + TABLE_NAME;
            db.execSQL(sql);
        } catch (Exception e) {
            Log.e(TAG, "deleteAllData: " + e.getMessage());
        }
    }
}
