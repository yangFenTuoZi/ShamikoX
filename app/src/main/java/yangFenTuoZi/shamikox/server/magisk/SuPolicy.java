package yangFenTuoZi.shamikox.server.magisk;

import static yangFenTuoZi.shamikox.server.magisk.MagiskDB.COLUMN_UID;
import static yangFenTuoZi.shamikox.server.magisk.MagiskDB.COLUMN_POLICY;
import static yangFenTuoZi.shamikox.server.magisk.MagiskDB.COLUMN_UNTIL;
import static yangFenTuoZi.shamikox.server.magisk.MagiskDB.COLUMN_LOGGING;
import static yangFenTuoZi.shamikox.server.magisk.MagiskDB.COLUMN_NOTIFICATION;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class SuPolicy implements Parcelable {

    public static final int POLICY_INTERACTIVE = 0;
    public static final int POLICY_DENY = 1;
    public static final int POLICY_ALLOW = 2;

    public int uid = -1;
    public int policy = POLICY_INTERACTIVE;
    public int until = 0;
    public boolean logging = true;
    public boolean notification = true;

    public SuPolicy() {
    }

    public SuPolicy(ContentValues contentValues) {
        uid = contentValues.getAsInteger(COLUMN_UID);
        policy = contentValues.getAsInteger(COLUMN_POLICY);
        until = contentValues.getAsInteger(COLUMN_UNTIL);
        logging = contentValues.getAsInteger(COLUMN_LOGGING) == 1;
        notification = contentValues.getAsInteger(COLUMN_NOTIFICATION) == 1;
    }

    public SuPolicy(Cursor cursor) {
        int uid_index = cursor.getColumnIndex(COLUMN_UID);
        if (uid_index != -1) uid = cursor.getInt(uid_index);

        int policy_index = cursor.getColumnIndex(COLUMN_POLICY);
        if (policy_index != -1) policy = cursor.getInt(policy_index);

        int until_index = cursor.getColumnIndex(COLUMN_UNTIL);
        if (until_index != -1) until = cursor.getInt(until_index);

        int logging_index = cursor.getColumnIndex(COLUMN_LOGGING);
        if (logging_index != -1) logging = cursor.getInt(logging_index) == 1;

        int notification_index = cursor.getColumnIndex(COLUMN_NOTIFICATION);
        if (notification_index != -1) notification = cursor.getInt(notification_index) == 1;
    }

    public SuPolicy(Parcel source) {
        super();
        uid = source.readInt();
        policy = source.readInt();
        until = source.readInt();
        logging = source.readInt() == 1;
        notification = source.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(uid);
        dest.writeInt(policy);
        dest.writeInt(until);
        dest.writeInt(logging ? 1 : 0);
        dest.writeInt(notification ? 1 : 0);
    }

    public static final Parcelable.Creator<SuPolicy> CREATOR = new Parcelable.Creator<>() {
        @Override
        public SuPolicy createFromParcel(Parcel source) {
            return new SuPolicy(source);
        }

        @Override
        public SuPolicy[] newArray(int size) {
            return new SuPolicy[size];
        }
    };

    public ContentValues toContentValues() {
        var result = new ContentValues();
        result.put(COLUMN_UID, uid);
        result.put(COLUMN_POLICY, policy);
        result.put(COLUMN_UNTIL, until);
        result.put(COLUMN_LOGGING, logging ? 1 : 0);
        result.put(COLUMN_NOTIFICATION, notification ? 1 : 0);
        return result;
    }
}
