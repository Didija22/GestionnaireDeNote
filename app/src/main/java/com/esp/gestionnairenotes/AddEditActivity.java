package com.esp.gestionnairenotes;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.esp.gestionnairenotes.data.Note;

public class AddEditActivity extends AppCompatActivity {

    // Cles utilisees pour echanger des donnees avec MainActivity.
    public static final String EXTRA_MODE = "MODE";
    public static final String EXTRA_NOTE = "NOTE";
    public static final String EXTRA_COULEUR = "COULEUR";

    public static final String MODE_ADD = "ADD";
    public static final String MODE_EDIT = "EDIT";

    // Couleur utilisee par defaut si aucune n'est transmise.
    private static final String COULEUR_PAR_DEFAUT = "#219653";

    private CardView carteFormulaire;
    private EditText etTitre;
    private EditText etContenu;
    private TextView btnEnregistrer;

    private String mode;
    private Note noteAModifier;

    // Couleur actuellement selectionnee pour la note.
    private String couleurActuelle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit);
        appliquerInsetsSysteme();

        initialiserVues();
        configurerSelecteurCouleur();
        preparerFormulaireSelonMode();

        btnEnregistrer.setOnClickListener(v -> enregistrerNote());
    }

    // Reserve l'espace des barres systeme (statut + navigation) pour que le bouton
    // "Creer/Modifier" ne se retrouve pas cache sous la barre de navigation du telephone.
    private void appliquerInsetsSysteme() {
        View racine = findViewById(R.id.addEditRoot);
        int marge = (int) getResources().getDimension(R.dimen.marge_ecran);

        ViewCompat.setOnApplyWindowInsetsListener(racine, (v, insets) -> {
            Insets barres = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    marge + barres.left,
                    marge + barres.top,
                    marge + barres.right,
                    marge + barres.bottom);
            return insets;
        });
    }

    private void initialiserVues() {
        carteFormulaire = findViewById(R.id.carteFormulaire);
        etTitre = findViewById(R.id.etTitre);
        etContenu = findViewById(R.id.etContenu);
        btnEnregistrer = findViewById(R.id.btnEnregistrer);
    }

    private void preparerFormulaireSelonMode() {
        Intent intent = getIntent();
        mode = intent.getStringExtra(EXTRA_MODE);

        if (MODE_EDIT.equals(mode)) {
            // Mode modification : on pre-remplit le formulaire avec la note existante.
            noteAModifier = (Note) intent.getSerializableExtra(EXTRA_NOTE);

            if (noteAModifier != null) {
                //préremplissage du formulaire avec les données de la note à modifier
                etTitre.setText(noteAModifier.getTitre());
                etContenu.setText(noteAModifier.getContenu());
                appliquerCouleur(noteAModifier.getCouleur());
            }
            btnEnregistrer.setText(R.string.modifier);

        } else {
            // Mode ajout : on part d'un formulaire vide avec la couleur choisie dans la palette.
            mode = MODE_ADD;
            String couleur = intent.getStringExtra(EXTRA_COULEUR);
            appliquerCouleur(couleur != null ? couleur : COULEUR_PAR_DEFAUT);
            btnEnregistrer.setText(R.string.creer);
        }
    }

    private void configurerSelecteurCouleur() {
        // Permet de choisir ou de changer la couleur de la note dans le formulaire.
        configurerPastille(R.id.pfVert, "#219653");
        configurerPastille(R.id.pfRouge, "#EB5757");
        configurerPastille(R.id.pfBleu, "#2F80ED");
        configurerPastille(R.id.pfJaune, "#F2C94C");
        configurerPastille(R.id.pfOrange, "#F2994A");
        configurerPastille(R.id.pfGris, "#828282");
    }

    private void configurerPastille(int idPastille, String couleurHex) {
        findViewById(idPastille).setOnClickListener(v -> appliquerCouleur(couleurHex));
    }

    // Applique la couleur a la carte et la garde en memoire.
    private void appliquerCouleur(String couleurHex) {
        couleurActuelle = couleurHex;
        carteFormulaire.setCardBackgroundColor(Color.parseColor(couleurHex));
    }

    private void enregistrerNote() {
        String titre = etTitre.getText().toString().trim();
        String contenu = etContenu.getText().toString().trim();

        // Contrainte de l'enonce : une note vide ne doit pas pouvoir etre enregistree.
        if (TextUtils.isEmpty(titre) && TextUtils.isEmpty(contenu)) {
            Toast.makeText(this, R.string.note_vide, Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(titre)) {
            etTitre.setError(getString(R.string.note_vide));
            return;
        }

        Note note;

        if (MODE_EDIT.equals(mode) && noteAModifier != null) {
            // On conserve l'id, l'etat favori et la date d'origine de la note.
            note = noteAModifier;
            note.setTitre(titre);
            note.setContenu(contenu);
            note.setCouleur(couleurActuelle);
        } else {
            note = new Note(titre, contenu, couleurActuelle, false, System.currentTimeMillis());
        }

        Intent resultat = new Intent();
        resultat.putExtra(EXTRA_MODE, mode);
        resultat.putExtra(EXTRA_NOTE, note);
        setResult(RESULT_OK, resultat);
        finish();
    }
}
