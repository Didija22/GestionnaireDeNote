package com.esp.gestionnairenotes;
//Ajout des imports necessaires 
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esp.gestionnairenotes.adapter.NoteAdapter;
import com.esp.gestionnairenotes.data.AppDatabase;
import com.esp.gestionnairenotes.data.Note;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NOM = "prefs_notes";
    private static final String CLE_MODE_SOMBRE = "mode_sombre";

    private RecyclerView recyclerNotes;
    private TextView tvListeVide;
    private TextView tvCompteur;
    private TextView btnTrier;
    private TextView btnModeSombre;
    private EditText etRecherche;
    private TextView btnFavoris;
    private LinearLayout paletteCouleurs;
    private FloatingActionButton fabAjouter;

    private NoteAdapter noteAdapter;
    private List<Note> listeNotes;

    private AppDatabase database;
    private ExecutorService executorService;

    private boolean filtreFavorisActif = false;

    private int triActuel = 0;
    private static final String[] LIBELLES_TRI = {"Date ↓", "Date ↑", "Titre A→Z"};

    private ActivityResultLauncher<Intent> ajoutModifLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Appliquer le mode sombre AVANT setContentView
        appliquerModeSombreAuDemarrage();

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initialiserVues();

        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        configurerListe();
        configurerRecherche();
        configurerBoutons();
        initialiserLauncher();

        chargerNotes();
        mettreAJourIconeModeSombre();
    }

    private void appliquerModeSombreAuDemarrage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NOM, MODE_PRIVATE);
        boolean modeSombre = prefs.getBoolean(CLE_MODE_SOMBRE, false);
        AppCompatDelegate.setDefaultNightMode(
                modeSombre ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void initialiserVues() {
        recyclerNotes   = findViewById(R.id.recyclerNotes);
        tvListeVide     = findViewById(R.id.tvListeVide);
        tvCompteur      = findViewById(R.id.tvCompteur);
        btnTrier        = findViewById(R.id.btnTrier);
        btnModeSombre   = findViewById(R.id.btnModeSombre);
        etRecherche     = findViewById(R.id.etRecherche);
        btnFavoris      = findViewById(R.id.btnFavoris);
        paletteCouleurs = findViewById(R.id.paletteCouleurs);
        fabAjouter      = findViewById(R.id.fabAjouter);
    }

    private void configurerListe() {
        listeNotes = new ArrayList<>();
        noteAdapter = new NoteAdapter(listeNotes, new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                ouvrirModification(note);
            }
            @Override
            public void onNoteDoubleClick(Note note) {
                basculerFavori(note);
            }
            @Override
            public void onNoteLongClick(Note note) {
                afficherOptionsNote(note);
            }
        });
        recyclerNotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotes.setAdapter(noteAdapter);
    }

    private void configurerRecherche() {
        etRecherche.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                chargerNotes();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void configurerBoutons() {
        fabAjouter.setOnClickListener(v -> basculerPalette());

        configurerPastille(R.id.pastilleVert,   "#219653");
        configurerPastille(R.id.pastilleRouge,  "#EB5757");
        configurerPastille(R.id.pastilleBleu,   "#2F80ED");
        configurerPastille(R.id.pastilleJaune,  "#F2C94C");
        configurerPastille(R.id.pastilleOrange, "#F2994A");
        configurerPastille(R.id.pastilleGris,   "#828282");

        btnFavoris.setOnClickListener(v -> basculerFiltreFavoris());

        btnTrier.setText(LIBELLES_TRI[triActuel]);
        btnTrier.setOnClickListener(v -> {
            triActuel = (triActuel + 1) % LIBELLES_TRI.length;
            btnTrier.setText(LIBELLES_TRI[triActuel]);
            appliquerTri();
        });

        // Mode sombre : bascule et sauvegarde le choix
        btnModeSombre.setOnClickListener(v -> basculerModeSombre());
    }

    private void configurerPastille(int idPastille, String couleurHex) {
        findViewById(idPastille).setOnClickListener(v -> {
            masquerPalette();
            ouvrirAjout(couleurHex);
        });
    }

    private void initialiserLauncher() {
        ajoutModifLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Note note = (Note) data.getSerializableExtra(AddEditActivity.EXTRA_NOTE);
                        String mode = data.getStringExtra(AddEditActivity.EXTRA_MODE);
                        if (note == null) return;
                        if (AddEditActivity.MODE_EDIT.equals(mode)) {
                            modifierNote(note);
                        } else {
                            ajouterNote(note);
                        }
                    }
                });
    }

    // ----- Base de données -----

    private void chargerNotes() {
        String recherche = etRecherche.getText().toString().trim();
        executorService.execute(() -> {
            List<Note> notesDepuisBase;
            if (filtreFavorisActif) {
                notesDepuisBase = database.noteDao().getFavoris();
            } else if (!recherche.isEmpty()) {
                notesDepuisBase = database.noteDao().rechercherParTitre(recherche);
            } else {
                notesDepuisBase = database.noteDao().getAllNotes();
            }
            runOnUiThread(() -> {
                listeNotes.clear();
                listeNotes.addAll(notesDepuisBase);
                appliquerTri();
            });
        });
    }

    private void ajouterNote(Note note) {
        executorService.execute(() -> {
            database.noteDao().insert(note);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.note_creee, Toast.LENGTH_SHORT).show();
                chargerNotes();
            });
        });
    }

    private void modifierNote(Note note) {
        executorService.execute(() -> {
            database.noteDao().update(note);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.note_modifiee, Toast.LENGTH_SHORT).show();
                chargerNotes();
            });
        });
    }

    private void basculerFavori(Note note) {
        note.setFavori(!note.isFavori());
        executorService.execute(() -> {
            database.noteDao().update(note);
            runOnUiThread(() -> {
                int msg = note.isFavori() ? R.string.ajoute_favoris : R.string.retire_favoris;
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                chargerNotes();
            });
        });
    }

    private void supprimerNote(Note note) {
        executorService.execute(() -> {
            database.noteDao().delete(note);
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.note_supprimee, Toast.LENGTH_SHORT).show();
                chargerNotes();
            });
        });
    }

    // ----- Tri (bonus) -----

    private void appliquerTri() {
        switch (triActuel) {
            case 0:
                Collections.sort(listeNotes, (a, b) ->
                        Long.compare(b.getDateCreation(), a.getDateCreation()));
                break;
            case 1:
                Collections.sort(listeNotes, (a, b) ->
                        Long.compare(a.getDateCreation(), b.getDateCreation()));
                break;
            case 2:
                Collections.sort(listeNotes, (a, b) ->
                        a.getTitre().compareToIgnoreCase(b.getTitre()));
                break;
        }
        noteAdapter.notifyDataSetChanged();
        mettreAJourMessageVide();
    }

    // ----- Mode sombre (bonus) -----

    private void basculerModeSombre() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NOM, MODE_PRIVATE);
        boolean estSombre = prefs.getBoolean(CLE_MODE_SOMBRE, false);
        boolean nouveauMode = !estSombre;

        prefs.edit().putBoolean(CLE_MODE_SOMBRE, nouveauMode).apply();
        AppCompatDelegate.setDefaultNightMode(
                nouveauMode ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void mettreAJourIconeModeSombre() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NOM, MODE_PRIVATE);
        boolean estSombre = prefs.getBoolean(CLE_MODE_SOMBRE, false);
        btnModeSombre.setText(estSombre ? "☀️" : "🌙");
    }

    // ----- Options note -----

    private void afficherOptionsNote(Note note) {
        String[] options = {
                getString(R.string.partager),
                getString(R.string.supprimer)
        };
        new AlertDialog.Builder(this)
                .setTitle(note.getTitre())
                .setItems(options, (dialog, choix) -> {
                    if (choix == 0) partagerNote(note);
                    else confirmerSuppression(note);
                })
                .show();
    }

    private void confirmerSuppression(Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.supprimer)
                .setMessage(R.string.confirmer_suppression)
                .setPositiveButton(R.string.supprimer, (dialog, which) -> supprimerNote(note))
                .setNegativeButton(R.string.annuler, null)
                .show();
    }

    private void partagerNote(Note note) {
        String texte = note.getTitre() + "\n\n" + note.getContenu();
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, note.getTitre());
        i.putExtra(Intent.EXTRA_TEXT, texte);
        startActivity(Intent.createChooser(i, getString(R.string.partager)));
    }

    // ----- Navigation -----

    private void ouvrirAjout(String couleurHex) {
        Intent intent = new Intent(this, AddEditActivity.class);
        intent.putExtra(AddEditActivity.EXTRA_MODE, AddEditActivity.MODE_ADD);
        intent.putExtra(AddEditActivity.EXTRA_COULEUR, couleurHex);
        ajoutModifLauncher.launch(intent);
    }

    private void ouvrirModification(Note note) {
        Intent intent = new Intent(this, AddEditActivity.class);
        intent.putExtra(AddEditActivity.EXTRA_MODE, AddEditActivity.MODE_EDIT);
        intent.putExtra(AddEditActivity.EXTRA_NOTE, note);
        ajoutModifLauncher.launch(intent);
    }

    // ----- Affichage -----

    private void basculerPalette() {
        if (paletteCouleurs.getVisibility() == View.VISIBLE) {
            masquerPalette();
        } else {
            paletteCouleurs.setVisibility(View.VISIBLE);
        }
    }

    private void masquerPalette() {
        paletteCouleurs.setVisibility(View.GONE);
    }

    private void basculerFiltreFavoris() {
        filtreFavorisActif = !filtreFavorisActif;
        if (filtreFavorisActif) {
            btnFavoris.setBackgroundResource(R.drawable.bg_bouton_favoris_actif);
            btnFavoris.setTextColor(getColor(R.color.white));
        } else {
            btnFavoris.setBackgroundResource(R.drawable.bg_bouton_favoris);
            btnFavoris.setTextColor(getColor(R.color.black));
        }
        chargerNotes();
    }

    private void mettreAJourMessageVide() {
        if (listeNotes.isEmpty()) {
            tvListeVide.setVisibility(View.VISIBLE);
            recyclerNotes.setVisibility(View.GONE);
        } else {
            tvListeVide.setVisibility(View.GONE);
            recyclerNotes.setVisibility(View.VISIBLE);
        }
        int total = listeNotes.size();
        tvCompteur.setText(total + " note" + (total > 1 ? "s" : ""));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }
}