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
- **Tiles** (en haut à droite) : les 2 048 numéros de la Mega Drive, 32
  par rangée. Tous sont utilisables : un tile jamais dessiné est vide (cadre
  discret), il existe dès qu'on dessine dedans. Clic ou glissé : le tile
  (ou le bloc) à éditer et à poser. « Dupliquer » copie le tile dans la
  première position libre qui suit.
- **Tilemap** (en bas à droite) : un onglet par plan, « Plan A » et
  « Plan B », chacun vu et modifié seul.
  - Outils : **Sélection** (rectangle à la souris ; ⌘-clic ajoute ou
    retire une case, ⌘-glissé ajoute un rectangle), **Poser** (le bloc de
    la planche, avec les champs de l'entrée en cours), **Gommer**.
  - L'entrée, dans l'ordre des bits `pccvhnnn nnnnnnnn` : Priorité,
    Palette, V-flip, H-flip, numéro de tile en décimal et en hexadécimal
    (Entrée pour valider). Les champs montrent la case cliquée et
    s'appliquent à toutes les cases sélectionnées. C'est le seul endroit
    où l'on modifie la tilemap : la palette choisie en haut à gauche ne
    sert qu'à dessiner et voir les tiles.
  - Zoom x1 à x3, grille, priorités (teinte rouge des cases prioritaires).
- **Carte** (bouton ou ⌘M) : les deux plans ensemble, et le fil de fer de
  la 3D ; un clic y déplace la vue ; touches 1, 2, 3 pour le zoom.
- **Aide > À propos** : la version et le patch.

## Repère 3D (comme DDS520)

Case (i, j) : i vers le bas‑droite, j vers le bas‑gauche, hauteur h vers
le haut. Losange de 32 × 16, un niveau = 16 pixels :

    x = originX + 16 (i − j)
    y = originY + 8 (i + j) − 16 h     (sommet du losange)

## Fichiers

- `.mirage` : texte lisible (palette, tiles en hexadécimal, plans A et B,
  hauteurs et types par case).
- Export Mega Drive en binaire (⌘E) : `tiles.bin` (4 bpp), `plane_a.bin`,
  `plane_b.bin` (entrées 16 bits gros‑boutistes), `cram.bin` (128 octets),
  `heights.bin` (un octet par case : type << 4 | hauteur), `map.inc`
  (constantes pour asm68k).
- Ou en source asm68k (⇧⌘E), pour lire le résultat : `tiles.s` (un tile
  = 8 lignes `dc.l`, un chiffre hexadécimal par pixel), `plane_a.s`,
  `plane_b.s` (une rangée = 4 lignes de 16 `dc.w`), `cram.s`,
  `heights.s`, et le même `map.inc`.
- Les tiles exportés vont jusqu'au dernier dessiné ou référencé par un
  plan.
