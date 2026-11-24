# Mirage

Éditeur de zones isométriques pour la Mega Drive, dans l'esprit de
« Mirage Type III », l'outil PC‑98 de Landstalker. Java 17, Swing seul,
aucune bibliothèque.

## Démarrer

    make run                      # projet de démonstration
    make run FILE=demo/demo.mirage
    make test                     # tests du modèle
    make jar                      # build/mirage.jar (java -jar build/mirage.jar)

## La fenêtre (pensée pour 1440 × 900)

- **Palettes** (en haut à gauche) : 4 lignes de 16 couleurs, niveaux 0–7
  par composante comme la CRAM, réglés par trois curseurs R, G, B ; la couleur 0 de chaque ligne est
  transparente (barrée), la 0 de la ligne 0 est le fond.
- **Tile** (au milieu à gauche) : le tile sélectionné, 8 × 8 ; crayon,
  ligne, rectangle, remplissage, pipette, miroirs, quart de tour, copier /
  coller. Bouton droit (ou Ctrl) : couleur 0. Le tile 0 reste vide.
- **Tiles** (en haut à droite) : la planche, 32 par rangée. Clic ou glissé :
  le bloc à poser sur l'écran.
- **Écran** (en bas à droite), trois rangées :
  - l'outil : Poser, Gommer, Pipette | P on, P off (bit de priorité case
    par case), Hauteur (clic gauche +1, clic droit −1 sur le losange
    visé) | zoom x1 à x3 | Carte ;
  - l'entrée posée : plan A ou B, numéro du tile (lecture seule),
    palette 0 à 3, Priorité, H-flip, V-flip. La pipette reprend tout
    cela depuis la case cliquée ;
  - ce qu'on voit : plans, fil de fer de la carte des hauteurs (0–15 par
    losange), grille, priorités.
- **Carte** (bouton ou ⌘M) : toute la carte dans une grande fenêtre ;
  un clic y déplace l'écran ; touches 1, 2, 3 pour le zoom.

## Repère 3D (comme DDS520)

Case (i, j) : i vers le bas‑droite, j vers le bas‑gauche, hauteur h vers
le haut. Losange de 32 × 16, un niveau = 16 pixels :

    x = originX + 16 (i − j)
    y = originY + 8 (i + j) − 16 h     (sommet du losange)

## Fichiers

- `.mirage` : texte lisible (palette, tiles en hexadécimal, plans A et B,
  hauteurs et types par case).
- Export Mega Drive (⌘E) : `tiles.bin` (4 bpp), `plane_a.bin`,
  `plane_b.bin` (entrées 16 bits gros‑boutistes), `cram.bin` (128 octets),
  `heights.bin` (un octet par case : type << 4 | hauteur), `map.inc`
  (constantes pour asm68k).
