package com.esp.gestionnairenotes.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

// fichier  Membre 1 
// @Entity indique a Room que cette classe represente une table SQLite.
// On garde Serializable pour pouvoir passer une Note d'une activite a l'autre via Intent.
@Entity(tableName = "notes")
public class Note implements Serializable {

    // autoGenerate = true : c'est Room qui s'occupe de generer l'id
    // automatiquement.
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String titre;
    private String contenu;

    // On stocke la couleur sous forme hexadecimale (ex : "#219653").
    private String couleur;

    // Etat favori de la note.
    private boolean favori;

    // Date de creation en millisecondes. On la formate au moment de l'affichage.
    private long dateCreation;

    public Note(String titre, String contenu, String couleur, boolean favori, long dateCreation) {
        this.titre = titre;
        this.contenu = contenu;
        this.couleur = couleur;
        this.favori = favori;
        this.dateCreation = dateCreation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public boolean isFavori() {
        return favori;
    }

    public void setFavori(boolean favori) {
        this.favori = favori;
    }

    public long getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(long dateCreation) {
        this.dateCreation = dateCreation;
    }
}
