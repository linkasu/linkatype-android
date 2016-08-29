package ru.ibakaidov.distypepro;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.yandex.metrica.YandexMetrica;

import java.util.ArrayList;

/**
 * Created by aacidov on 28.05.16.
 */

public class DB {
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_STATEMENTS = "statements";
    static String withoutCategory;
    DBHelper dbHelper;

    public DB(Context cxt, String withoutCategory) {
        DB.withoutCategory = withoutCategory;
        dbHelper = new DBHelper(cxt);
    }

    public static void createCategory(SQLiteDatabase db, String label) {
        ContentValues cv = new ContentValues();
        cv.put("label", label);
        db.insert(TABLE_CATEGORIES, null, cv);
        YandexMetrica.reportEvent("create category", "{\"text\":\"" + label + "\"}");
    }

    public void createCategory(String label) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        DB.createCategory(db, label);
        db.close();
    }

    public void createStatement(String statement, int category) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query(
                TABLE_STATEMENTS, new String[]{"text"}, "`text`=?", new String[]{statement},
                null, null, null
        );
        if (c.getCount() == 1) {
            updateRating(statement);
            return;
        }

        c.close();
        ContentValues cv = new ContentValues();
        cv.put("text", statement);
        cv.put("category", category);
        db.insert(TABLE_STATEMENTS, null, cv);
        db.close();
        YandexMetrica.reportEvent("create statement", "{\"text\":\"" + statement + "\"}");

    }

    public void updateRating(String statement) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE statements SET rating = rating + 1 WHERE `text`=\"" + statement + "\"");
        db.close();
    }

    public ArrayList<String> getCategories() {
        ArrayList<String> categories = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(TABLE_CATEGORIES, null, null, null, null, null, null);

        if (c.moveToFirst()) {

            int labelColIndex = c.getColumnIndex("label");

            do {
                categories.add(c.getString(labelColIndex));
            } while (c.moveToNext());
        }
        db.close();
        c.close();
        return categories;
    }

    public ArrayList<String> getStatements(int position) {
        ArrayList<String> statements = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(
                TABLE_STATEMENTS, new String[]{"text"}, "`category`=?",
                new String[]{Integer.toString(position)}, null, null, "rating DESC"
        );
        if (c.moveToFirst()) {

            int textColIndex = c.getColumnIndex("text");

            do {
                statements.add(c.getString(textColIndex));
            } while (c.moveToNext());
        }

        c.close();
        db.close();
        return statements;
    }

    public ArrayList<String> getStatements() {
        ArrayList<String> statements = new ArrayList<String>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(
                TABLE_STATEMENTS, new String[]{"text"}, null, null, null, null,"rating DESC"
        );
        if (c.moveToFirst()) {

            int textColIndex = c.getColumnIndex("text");

            do {
                statements.add(c.getString(textColIndex));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return statements;
    }

    public void setCategory(int idCategory, String statement) {
        ContentValues cv = new ContentValues();
        cv.put("text", statement);
        cv.put("category", idCategory);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.update(TABLE_STATEMENTS, cv, "text='" + statement + "'", null);
        db.close();
        YandexMetrica.reportEvent("change category", "{\"text\":\"" + statement + "\"}");

    }

    public void deleteStatement(String selected) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_STATEMENTS, "text='" + selected + "'", new String[]{});
        db.close();
    }

    public void editStatement(String old, String newText) {
        ContentValues cv = new ContentValues();
        cv.put("text", newText);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.update(TABLE_STATEMENTS, cv, "text='" + old + "'", null);
        db.close();
    }

    public void editCategory(String selected, String s) {
        ContentValues cv = new ContentValues();
        cv.put("label", s);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.update(TABLE_CATEGORIES, cv, "label='" + selected + "'", null);
        db.close();
    }

    public void deleteCategory(String selected) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_CATEGORIES, "label='" + selected + "'", new String[]{});
        db.close();
    }


    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "DisCannibal", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_STATEMENTS + " (`id` INTEGER primary key " +
                    "autoincrement, `text` VARCHAR(500) NOT NULL, `category` INT DEFAULT 1, " +
                    "`rating` INT DEFAULT 0);");
            db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (`id` INTEGER primary key " +
                    "autoincrement, `label` VARCHAR(200) NOT NULL );");
            DB.createCategory(db, withoutCategory);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}

