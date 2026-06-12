package com.esp.gestionnairenotes.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

// @Dao : cette interface regroupe toutes les requetes SQL liees aux notes.
// On evite ainsi d'ecrire du SQL un peu partout dans l'application.
@Dao
public interface NoteDao {

    // Ajoute une nouvelle note.
    @Insert
    void insert(Note note);

    // Modifie une note existante.
    @Update
    void update(Note note);

    // Supprime une note (utilise pour le bonus suppression).
    @Delete
    void delete(Note note);

    // Recupere toutes les notes, de la plus recente a la plus ancienne.
    @Query("SELECT * FROM notes ORDER BY dateCreation DESC")
    List<Note> getAllNotes();

    // Recupere uniquement les notes mises en favori (filtre Favoris).
    @Query("SELECT * FROM notes WHERE favori = 1 ORDER BY dateCreation DESC")
    List<Note> getFavoris();

    // Recherche les notes dont le titre contient le texte saisi.
    @Query("SELECT * FROM notes WHERE titre LIKE '%' || :recherche || '%' ORDER BY dateCreation DESC")
    List<Note> rechercherParTitre(String recherche);
}
