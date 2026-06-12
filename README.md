# Gestionnaire de Notes

Application Android (Java) de gestion de notes personnelles, réalisée dans le cadre
de l'examen de TP Mobile 2025/2026.

L'utilisateur peut créer, modifier, rechercher et organiser ses notes. Chaque note a
une couleur et peut être mise en favori. Les données sont sauvegardées localement avec
Room, donc elles sont conservées après la fermeture de l'application.

## Fonctionnalités

- Création d'une note (titre, contenu, couleur)
- Consultation de toutes les notes
- Modification d'une note (formulaire pré-rempli)
- Mise en favori / retrait des favoris par **double clic** sur la note
- Recherche d'une note par son titre
- Filtre pour n'afficher que les favoris
- Choix parmi 6 couleurs
- Persistance locale des données (Room)

### Fonctionnalités bonus

- Suppression d'une note (appui long → menu)
- Partage d'une note vers une autre application (appui long → menu)
- Message dédié quand la liste est vide
- Confirmation avant suppression

## Technologies

- Java
- Android (minSdk 24)
- Room (persistance locale)
- RecyclerView / CardView

## Organisation du projet

```
com.esp.gestionnairenotes
├── MainActivity.java        // liste des notes, recherche, filtre, palette
├── AddEditActivity.java     // formulaire de création / modification
├── adapter
│   └── NoteAdapter.java     // affichage des cartes + clic / double clic
└── data
    ├── Note.java            // entité Room
    ├── NoteDao.java         // requêtes SQL
    └── AppDatabase.java     // base de données locale (singleton)
```

## Lancement

Ouvrir le projet dans Android Studio, laisser la synchronisation Gradle se terminer,
puis lancer l'application sur un émulateur ou un téléphone (API 24 minimum).
