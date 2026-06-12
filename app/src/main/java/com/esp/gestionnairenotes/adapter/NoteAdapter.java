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

// Adapter qui affiche la liste des notes dans le RecyclerView.
// L'Adapter se contente d'afficher : c'est MainActivity qui decide quoi faire au clic.
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    // Delai (en ms) en dessous duquel deux clics sont consideres comme un double clic.
    private static final long DELAI_DOUBLE_CLIC = 300;

    // L'Adapter previent MainActivity des actions de l'utilisateur :
    // clic simple -> modifier, double clic -> favori, clic long -> options (partager / supprimer).
    public interface OnNoteClickListener {
        void onNoteClick(Note note);

        void onNoteDoubleClick(Note note);

        void onNoteLongClick(Note note);
    }

    private final List<Note> listeNotes;
    private final OnNoteClickListener listener;

    // Mois en francais pour afficher la date comme sur la maquette ("01 Juin 2026").
    private static final String[] MOIS = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    public NoteAdapter(List<Note> listeNotes, OnNoteClickListener listener) {
        this.listeNotes = listeNotes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vue = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(vue);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = listeNotes.get(position);

        holder.tvTitre.setText(note.getTitre());
        holder.tvDate.setText(formaterDate(note.getDateCreation()));

        // Couleur de fond de la carte = couleur choisie pour la note.
        holder.carte.setCardBackgroundColor(Color.parseColor(note.getCouleur()));

        // On affiche l'etoile uniquement si la note est en favori.
        holder.ivFavori.setVisibility(note.isFavori() ? View.VISIBLE : View.GONE);

        // Gestion du clic simple et du double clic sur la meme carte.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long maintenant = System.currentTimeMillis();

                if (maintenant - holder.dernierClic < DELAI_DOUBLE_CLIC) {
                    // Deuxieme clic rapproche : c'est un double clic -> on annule le clic simple.
                    holder.handler.removeCallbacksAndMessages(null);
                    holder.dernierClic = 0;
                    if (listener != null) {
                        listener.onNoteDoubleClick(note);
                    }
                } else {
                    // Premier clic : on attend un peu pour voir si un second clic arrive.
                    holder.dernierClic = maintenant;
                    holder.handler.postDelayed(() -> {
                        if (listener != null) {
                            listener.onNoteClick(note);
                        }
                    }, DELAI_DOUBLE_CLIC);
                }
            }
        });

        // Clic long : ouvrir le menu d'options (partager ou supprimer la note).
        holder.itemView.setOnLongClickListener(v -> {
            // On annule un eventuel clic simple en attente pour eviter d'ouvrir la modification.
            holder.handler.removeCallbacksAndMessages(null);
            holder.dernierClic = 0;
            if (listener != null) {
                listener.onNoteLongClick(note);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listeNotes.size();
    }

    // Transforme une date en millisecondes en texte "01 Juin 2026".
    private String formaterDate(long dateEnMillis) {
        Calendar calendrier = Calendar.getInstance();
        calendrier.setTimeInMillis(dateEnMillis);

        int jour = calendrier.get(Calendar.DAY_OF_MONTH);
        String mois = MOIS[calendrier.get(Calendar.MONTH)];
        int annee = calendrier.get(Calendar.YEAR);

        return String.format(Locale.FRENCH, "%02d %s %d", jour, mois, annee);
    }

    // Le ViewHolder garde une reference vers les vues d'une carte pour eviter de
    // refaire findViewById a chaque defilement.
    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        CardView carte;
        TextView tvTitre;
        TextView tvDate;
        ImageView ivFavori;

        // Outils pour distinguer clic simple et double clic.
        final Handler handler = new Handler(Looper.getMainLooper());
        long dernierClic = 0;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            carte = itemView.findViewById(R.id.carteNote);
            tvTitre = itemView.findViewById(R.id.tvTitreNote);
            tvDate = itemView.findViewById(R.id.tvDateNote);
            ivFavori = itemView.findViewById(R.id.ivFavori);
        }
    }
}
