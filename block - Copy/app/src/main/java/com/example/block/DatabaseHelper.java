package com.example.block;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "property.db";
    private static final int DATABASE_VERSION = 3;

    // Table Info
    public static final String TABLE_NAME = "data";

    // Column Names
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_HOUSE_NUMBER = "house_number";
    public static final String COLUMN_ENERGY_COUNT = "energy_count";
    public static final String COLUMN_VAT = "vat";
    public static final String COLUMN_ADDITIONAL_PAYMENT = "additional_payment";
    public static final String COLUMN_FINAL_PAYMENT = "final_payment";
    public static final String COLUMN_TARIF = "tarif";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TIME = "time";

    // Table Creation SQL
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_HOUSE_NUMBER + " TEXT, " +
                    COLUMN_ENERGY_COUNT + " REAL, " +
                    COLUMN_VAT + " REAL, " +
                    COLUMN_ADDITIONAL_PAYMENT + " REAL, " +
                    COLUMN_FINAL_PAYMENT + " REAL, " +
                    COLUMN_TARIF + " REAL, " +
                    COLUMN_DATE + " TEXT, " +
                    COLUMN_TIME + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert a new row
    public void insertData(String houseNumber,
                           float energyCount,
                           float vat,
                           float additionalPayment,
                           float finalPayment,
                           float tarif,
                           String date,
                           String time) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_HOUSE_NUMBER, houseNumber);
        values.put(COLUMN_ENERGY_COUNT, energyCount);
        values.put(COLUMN_VAT, vat);
        values.put(COLUMN_ADDITIONAL_PAYMENT, additionalPayment);
        values.put(COLUMN_FINAL_PAYMENT, finalPayment);
        values.put(COLUMN_TARIF, tarif);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_TIME, time);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // Get most recent count for a block
    public float getRecentCount(String houseNumber) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_ENERGY_COUNT +
                        " FROM " + TABLE_NAME +
                        " WHERE " + COLUMN_HOUSE_NUMBER + "=? " +
                        " ORDER BY " + COLUMN_DATE + " DESC, " +
                        COLUMN_TIME + " DESC LIMIT 1",
                new String[]{houseNumber});

        float count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getFloat(0);
        }

        cursor.close();
        return count;
    }

    // Get all records
    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME +
                        " ORDER BY " + COLUMN_DATE + " DESC, " +
                        COLUMN_TIME + " DESC",
                null
        );
    }

    // Get records filtered by block/house number
    public Cursor getRecordsByBlock(String houseNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME +
                        " WHERE " + COLUMN_HOUSE_NUMBER + "=? " +
                        " ORDER BY " + COLUMN_DATE + " DESC, " +
                        COLUMN_TIME + " DESC",
                new String[]{houseNumber});
    }

    // Delete a row by ID
    public boolean deleteRow(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deleted = db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        return deleted > 0;
    }

    // Get records filtered by date range (last X hours)
    public Cursor getRecordsByDateFilter(String filter) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Determine hours based on filter
        long hours = 0;
        switch (filter) {
            case "day": hours = 24; break;
            case "week": hours = 168; break;
            case "month": hours = 730; break;
            case "year": hours = 8760; break;
        }

        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - hours * 60L * 60L * 1000L;

        Cursor allCursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME +
                        " ORDER BY " + COLUMN_DATE + " DESC, " + COLUMN_TIME + " DESC",
                null);

        // Create a MatrixCursor to hold filtered rows
        android.database.MatrixCursor filteredCursor = new android.database.MatrixCursor(
                new String[]{COLUMN_ID, COLUMN_HOUSE_NUMBER, COLUMN_ENERGY_COUNT, COLUMN_VAT,
                        COLUMN_ADDITIONAL_PAYMENT, COLUMN_FINAL_PAYMENT, COLUMN_TARIF, COLUMN_DATE, COLUMN_TIME}
        );

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        while (allCursor.moveToNext()) {
            String date = allCursor.getString(allCursor.getColumnIndex(COLUMN_DATE));
            String time = allCursor.getString(allCursor.getColumnIndex(COLUMN_TIME));
            String dateTime = date + " " + time;

            try {
                long recordTime = sdf.parse(dateTime).getTime();
                if (recordTime >= cutoffTime) {
                    Object[] row = new Object[]{
                            allCursor.getInt(allCursor.getColumnIndex(COLUMN_ID)),
                            allCursor.getString(allCursor.getColumnIndex(COLUMN_HOUSE_NUMBER)),
                            allCursor.getDouble(allCursor.getColumnIndex(COLUMN_ENERGY_COUNT)),
                            allCursor.getDouble(allCursor.getColumnIndex(COLUMN_VAT)),
                            allCursor.getDouble(allCursor.getColumnIndex(COLUMN_ADDITIONAL_PAYMENT)),
                            allCursor.getDouble(allCursor.getColumnIndex(COLUMN_FINAL_PAYMENT)),
                            allCursor.getDouble(allCursor.getColumnIndex(COLUMN_TARIF)),
                            date,
                            time
                    };
                    filteredCursor.addRow(row);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        allCursor.close();
        return filteredCursor;
    }

    // --- NEW METHOD: Get previous energy based on date & time ---
    public double getPreviousEnergy(String houseNumber, String currentDate, String currentTime) {
        double previous = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + COLUMN_ENERGY_COUNT +
                        " FROM " + TABLE_NAME +
                        " WHERE " + COLUMN_HOUSE_NUMBER + "=? AND " +
                        "( " + COLUMN_DATE + " < ? OR (" + COLUMN_DATE + " = ? AND " + COLUMN_TIME + " < ?) ) " +
                        " ORDER BY " + COLUMN_DATE + " DESC, " + COLUMN_TIME + " DESC LIMIT 1",
                new String[]{houseNumber, currentDate, currentDate, currentTime}
        );

        if (c.moveToFirst()) {
            previous = c.getDouble(0);
        }
        c.close();
        return previous;
    }
}
