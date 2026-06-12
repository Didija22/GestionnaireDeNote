package com.esp.gestionnairenotes.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// @Database : on indique a Room les entites qui composent la base et la version du schema.
@Database(entities = {Note.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Instance unique de la base (pattern singleton).
    private static volatile AppDatabase INSTANCE;

    private static final String NOM_BASE = "notes_database";

    // Donne acces au DAO des notes.
    public abstract NoteDao noteDao();

    // Singleton thread-safe avec double verrouillage.
    // Evite de creer plusieurs instances de la base en meme temps.
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            NOM_BASE
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
