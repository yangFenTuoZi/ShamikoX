package yangFenTuoZi.shamikox.server.magisk;

import android.database.sqlite.SQLiteDatabase;

public class MagiskDB {
    public static final String DB_FILE = "/data/adb/magisk.db";
    public static final String TABLE_NAME = "policies";
    public static final String COLUMN_UID = "uid";
    public static final String COLUMN_POLICY = "policy";
    public static final String COLUMN_UNTIL = "until";
    public static final String COLUMN_LOGGING = "logging";
    public static final String COLUMN_NOTIFICATION = "notification";

    private SQLiteDatabase database;

    public MagiskDB() {
        open();
    }

    public void delete(SuPolicy suPolicy) {
        database.delete(TABLE_NAME, COLUMN_UID + " = ?", new String[]{String.valueOf(suPolicy.uid)});
    }

    public void update(SuPolicy suPolicy) {
        database.update(TABLE_NAME, suPolicy.toContentValues(), COLUMN_UID + " = ?", new String[]{String.valueOf(suPolicy.uid)});
    }

    public void insert(SuPolicy suPolicy) {
        database.insert(TABLE_NAME, null, suPolicy.toContentValues());
    }

    public SuPolicy query(int uid) {
        var cursor = database.query(TABLE_NAME, null, COLUMN_UID + " = ?", new String[]{String.valueOf(uid)}, null, null, null);
        SuPolicy suPolicy;
        if (cursor.moveToFirst()) {
            suPolicy = new SuPolicy(cursor);
        } else {
            suPolicy = new SuPolicy();
        }
        return suPolicy;
    }

    public SuPolicy[] query() {
        var cursor = database.query(TABLE_NAME, null, null, null, null, null, null);
        var result = new SuPolicy[cursor.getCount()];
        for (int i = 0; i < result.length; i++) {
            cursor.moveToNext();
            result[i] = new SuPolicy(cursor);
        }
        return result;
    }

    public boolean exist(int uid) {
        var cursor = database.query(TABLE_NAME, null, COLUMN_UID + " = ?", new String[]{String.valueOf(uid)}, null, null, null);
        return cursor.getCount() > 0;
    }

    public void insertOrUpdate(SuPolicy suPolicy) {
        if (exist(suPolicy.uid)) {
            update(suPolicy);
        } else {
            insert(suPolicy);
        }
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public void close() {
        database.close();
        database = null;
    }

    public void open() {
        database = SQLiteDatabase.openDatabase(DB_FILE,null, SQLiteDatabase.OPEN_READWRITE);
    }
}
