# Mirage — versions

## 0.1 — l'éditeur de base

Ce que fait la version :

- Fenêtre unique 1440 × 900 : palettes et tile éditable à gauche,
  planche des tiles et écran à droite ; fenêtre « Carte » pour la vue
  d'ensemble.
- Modèle Mega Drive exact : 4 × 16 couleurs en niveaux 0–7, tiles 8 × 8
  4 bpp, entrées de plan 16 bits (priorité, ligne, miroirs, tile),
  composition du VDP (A haut, B haut, A bas, B bas, fond).
- Carte des hauteurs 0–15 par losange (repère DDS520), fil de fer 3D
  superposé, édition au clic.
- Annuler / rétablir (64 pas), format texte `.mirage`, export binaire
  pour la ROM (`tiles.bin`, `plane_a.bin`, `plane_b.bin`, `cram.bin`,
  `heights.bin`, `map.inc`).
- Projet de démonstration généré (pièce 16 × 16, estrade, escalier,
  piliers) découpé automatiquement en tiles avec dédoublonnage par
  retournements ; aussi enregistré dans `demo/demo.mirage`.

Mesures :

- Démo : 53 tiles uniques pour une carte 64 × 64, générée en 3 à 22 ms.
- `make test` : 37 vérifications (CRAM, retournements, bits d'entrée,
  composition, projection et sélection 3D, format texte, export,
  annulation, démo).
- Aucune dépendance hors du JDK ; compile avec `javac --release 17`.

## 0.2 — sélecteur de palette et curseurs de couleur

Ce que fait la version :

- Barre de l'écran : sélecteur « Palette 0 1 2 3 » ; la palette choisie
  est celle des entrées posées. Il reste synchronisé avec les pastilles
  du panneau Palettes et avec la pipette de l'écran.
- Panneau Palettes : les boutons − / + sont remplacés par trois curseurs
  R, G, B de 0 à 7 (crans, valeur affichée). Un glissé complet compte
  pour un seul pas d'annulation.
- Panneau Tile : « palette N » au lieu de « ligne de palette N ».

Mesures :

- `make test` : 37 vérifications, 0 échec.
- Glissé de R de 1 à 7 en trois mouvements : 1 pas d'annulation, la
  couleur revient à 1 après Annuler.

## 0.3 — barre de l'écran regroupée

Ce que fait la version :

- La barre de l'écran passe de deux rangées trop larges à trois rangées
  compactes, groupées par rôle :
  1. l'outil : Poser, Gommer, Pipette | P on, P off, Hauteur, puis le
     zoom et le bouton Carte ;
  2. l'entrée de plan posée : Plan A/B, numéro du tile en lecture seule
     (décimal et hexadécimal, taille du bloc s'il y en a un), Palette en
     liste déroulante, cases Priorité, H-flip, V-flip ;
  3. l'affichage : plans, fil de fer, grille, priorités.
- Tous ces contrôles suivent l'état : la pipette de l'écran reprend
  désormais aussi la priorité et les miroirs de la case cliquée.

Mesures :

- Largeur de la barre : environ 510 pixels au lieu de 890 (elle
  débordait de l'écran à 1440 × 900).
- `make test` : 37 vérifications, 0 échec.
