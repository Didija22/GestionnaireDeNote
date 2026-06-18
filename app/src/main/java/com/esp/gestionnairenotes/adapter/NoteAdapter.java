package com.esp.gestionnairenotes.adapter;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.esp.gestionnairenotes.R;
import com.esp.gestionnairenotes.data.Note;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// =========================================================================
// NoteAdapter
// =========================================================================
// Role general : c'est le "pont" entre la liste de Note (les donnees) et le
// RecyclerView (l'affichage a l'ecran). Pour chaque note de la liste, il cree
// une carte visuelle (CardView) et y affiche le titre, la date, la couleur,
// et l'icone favori.
//
// Important pour la presentation : l'Adapter NE DECIDE PAS quoi faire quand
// on clique sur une note (modifier, supprimer, mettre en favori...). Il se
// contente de DETECTER le type de clic (simple / double / long) et de
// PREVENIR MainActivity via l'interface OnNoteClickListener. C'est
// MainActivity qui contient la vraie logique metier.
// =========================================================================
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    // Delai (en millisecondes) en dessous duquel on considere que deux clics
    // rapproches forment un "double clic". Au-dessus de ce delai, le premier
    // clic est traite comme un clic simple normal.
    private static final long DELAI_DOUBLE_CLIC = 300;

    // -------------------------------------------------------------------
    // Interface de callback : OnNoteClickListener
    // -------------------------------------------------------------------
    // Permet a l'Adapter de "remonter" les actions de l'utilisateur vers
    // MainActivity, sans que l'Adapter ait besoin de connaitre les details
    // de ce qu'il faut faire. C'est le principe d'inversion de dependance :
    // l'Adapter dit "il s'est passe ceci", et MainActivity decide la suite.
    //
    //   - onNoteClick       : clic simple  -> generalement : ouvrir la note pour modification
    //   - onNoteDoubleClick : double clic  -> generalement : ajouter/retirer des favoris
    //   - onNoteLongClick   : clic long    -> generalement : ouvrir un menu (partager / supprimer)
    public interface OnNoteClickListener {
        void onNoteClick(Note note);

        void onNoteDoubleClick(Note note);

        void onNoteLongClick(Note note);
    }

    // Liste des notes a afficher. C'est la "source de verite" des donnees ;
    // l'Adapter ne fait que la lire pour generer l'affichage.
    private final List<Note> listeNotes;

    // Reference vers l'objet (en general MainActivity) qui ecoute les clics.
    private final OnNoteClickListener listener;

    // Tableau des noms de mois en francais, utilise pour formater la date
    // affichee sur chaque carte (index 0 = Janvier, ..., index 11 = Decembre).
    private static final String[] MOIS = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    // Constructeur : on recoit la liste des notes a afficher et le "listener"
    // qui sera notifie des clics de l'utilisateur.
    public NoteAdapter(List<Note> listeNotes, OnNoteClickListener listener) {
        this.listeNotes = listeNotes;
        this.listener = listener;
    }

    // -------------------------------------------------------------------
    // onCreateViewHolder
    // -------------------------------------------------------------------
    // Appele par le RecyclerView quand il a besoin de CREER une nouvelle
    // carte visuelle (par exemple au tout debut, ou quand on scrolle et qu'il
    // n'y a pas assez de cartes "recyclees" disponibles).
    //
    // On "inflate" (transforme en objets View) le layout XML item_note.xml,
    // puis on l'enveloppe dans un NoteViewHolder.
    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vue = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(vue);
    }

    // -------------------------------------------------------------------
    // onBindViewHolder
    // -------------------------------------------------------------------
    // Appele chaque fois que le RecyclerView doit AFFICHER une note a une
    // position donnee dans une carte (holder) existante. C'est ici qu'on
    // "remplit" la carte avec les vraies donnees de la note, et qu'on
    // branche les ecouteurs de clic.
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        // On recupere la note correspondant a la position actuelle dans la liste.
        Note note = listeNotes.get(position);

        // Affichage du titre et de la date (formatee en francais) de la note.
        holder.tvTitre.setText(note.getTitre());
        holder.tvDate.setText(formaterDate(note.getDateCreation()));

        // La couleur de fond de la carte correspond a la couleur choisie par
        // l'utilisateur pour cette note (stockee sous forme de chaine, ex: "#FFEEAA").
        holder.carte.setCardBackgroundColor(Color.parseColor(note.getCouleur()));

        // L'icone "etoile" (favori) n'est visible que si la note est marquee favorite,
        // sinon on la cache completement (GONE = pas affichee et ne prend pas de place).
        holder.ivFavori.setVisibility(note.isFavori() ? View.VISIBLE : View.GONE);

        // -----------------------------------------------------------------
        // Gestion du clic simple ET du double clic sur la meme carte
        // -----------------------------------------------------------------
        // Android ne propose pas nativement de "double clic" sur une View,
        // donc on le simule nous-memes avec un Handler (qui permet d'executer
        // une action apres un delai, ou de l'annuler).
        //
        // Logique :
        //   1) Au premier clic, on enregistre l'heure (dernierClic) et on
        //      programme l'action "clic simple" pour DANS 300ms.
        //   2) Si un second clic arrive AVANT ces 300ms, on annule l'action
        //      "clic simple" programmee, et on declenche directement le
        //      "double clic".
        //   3) Si aucun second clic n'arrive, le clic simple s'execute
        //      normalement apres les 300ms.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long maintenant = System.currentTimeMillis();

                if (maintenant - holder.dernierClic < DELAI_DOUBLE_CLIC) {
                    // Cas "double clic" : le 2e clic arrive trop vite apres le 1er.
                    // On annule le clic simple qui etait en attente (postDelayed)
                    // pour ne pas declencher les deux actions a la fois.
                    holder.handler.removeCallbacksAndMessages(null);
                    holder.dernierClic = 0; // reinitialisation pour le prochain cycle de clics
                    if (listener != null) {
                        listener.onNoteDoubleClick(note);
                    }
                } else {
                    // Cas "1er clic" : on memorise l'instant du clic, puis on
                    // attend DELAI_DOUBLE_CLIC ms avant de declencher le clic
                    // simple, au cas ou un 2e clic viendrait juste apres.
                    holder.dernierClic = maintenant;
                    holder.handler.postDelayed(() -> {
                        if (listener != null) {
                            listener.onNoteClick(note);
                        }
                    }, DELAI_DOUBLE_CLIC);
                }
            }
        });

        // -----------------------------------------------------------------
        // Gestion du clic long (appui prolonge)
        // -----------------------------------------------------------------
        // Utilise pour ouvrir un menu d'options (par ex. partager ou
        // supprimer la note). On annule d'abord tout clic simple en attente,
        // pour eviter qu'il ne se declenche "par erreur" juste apres le clic
        // long (ce qui ouvrirait la modification en plus du menu d'options).
        holder.itemView.setOnLongClickListener(v -> {
            holder.handler.removeCallbacksAndMessages(null);
            holder.dernierClic = 0;
            if (listener != null) {
                listener.onNoteLongClick(note);
            }
            // "return true" indique a Android que l'evenement est pris en charge
            // (sinon le systeme pourrait aussi declencher un clic simple juste apres).
            return true;
        });
    }

    // -------------------------------------------------------------------
    // getItemCount
    // -------------------------------------------------------------------
    // Indique au RecyclerView combien d'elements il doit afficher au total.
    // C'est ce nombre qui determine combien de fois onBindViewHolder sera
    // appele (une fois par position, de 0 a getItemCount() - 1).
    @Override
    public int getItemCount() {
        return listeNotes.size();
    }

    // -------------------------------------------------------------------
    // formaterDate
    // -------------------------------------------------------------------
    // Convertit une date stockee en millisecondes (format technique, ex:
    // System.currentTimeMillis()) en un texte lisible en francais, du type
    // "01 Juin 2026", pour correspondre a la maquette du projet.
    private String formaterDate(long dateEnMillis) {
        Calendar calendrier = Calendar.getInstance();
        calendrier.setTimeInMillis(dateEnMillis);

        int jour = calendrier.get(Calendar.DAY_OF_MONTH);
        String mois = MOIS[calendrier.get(Calendar.MONTH)]; // MONTH commence a 0 (0 = Janvier)
        int annee = calendrier.get(Calendar.YEAR);

        // "%02d" garantit que le jour s'affiche toujours sur 2 chiffres (ex: "01" et non "1").
        return String.format(Locale.FRENCH, "%02d %s %d", jour, mois, annee);
    }

    // =====================================================================
    // NoteViewHolder
    // =====================================================================
    // Le ViewHolder represente UNE carte de note a l'ecran. Son role est de
    // garder en memoire les references vers les vues (CardView, TextView,
    // ImageView...) une seule fois, pour eviter de refaire des
    // findViewById() couteux a chaque fois que le RecyclerView affiche ou
    // reaffiche une carte pendant le defilement (scroll). C'est le coeur de
    // l'optimisation de performance des RecyclerView.
    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        CardView carte;       // La carte elle-meme (pour changer sa couleur de fond)
        TextView tvTitre;     // Le texte du titre de la note
        TextView tvDate;      // Le texte de la date formatee
        ImageView ivFavori;   // L'icone etoile, visible seulement si la note est favorite

        // Outils utilises pour distinguer un clic simple d'un double clic
        // (voir l'explication detaillee dans onBindViewHolder).
        final Handler handler = new Handler(Looper.getMainLooper());
        long dernierClic = 0; // Horodatage (en ms) du dernier clic enregistre sur cette carte

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            // On recupere une seule fois les references vers les vues du layout item_note.xml.
            carte = itemView.findViewById(R.id.carteNote);
            tvTitre = itemView.findViewById(R.id.tvTitreNote);
            tvDate = itemView.findViewById(R.id.tvDateNote);
            ivFavori = itemView.findViewById(R.id.ivFavori);
        }
    }
}
