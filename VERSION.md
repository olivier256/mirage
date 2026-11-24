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

## 0.4 — tilemap par plan, sélection multiple, export .s

Ce que fait la version :

- Planche des tiles : les 2 048 numéros sont tous utilisables ; un tile
  jamais dessiné est vide et existe dès qu'on dessine dedans. Le bouton
  « Nouveau » disparaît ; « Dupliquer » copie vers la première position
  libre.
- Tilemap : un onglet « Plan A » et un onglet « Plan B », chacun vu et
  modifié seul ; la carte (Affichage > Carte) montre toujours les deux.
- Outils Sélection (rectangle, ⌘-clic, ⌘-glissé), Poser, Gommer. P on,
  P off, Hauteur, fil de fer et cases plan A / plan B sont retirés de la
  tilemap (la 3D reviendra plus tard).
- Champs de l'entrée dans l'ordre des bits `pccvhnnn nnnnnnnn` :
  Priorité, Palette, V-flip, H-flip, tile en décimal et en hexadécimal
  (éditables). Ils montrent la case cliquée et s'appliquent à toute la
  sélection ; c'est le seul endroit où l'on modifie la tilemap.
- Corrigé : une palette changée sur une case est bien gardée.
- Export au choix en `.bin` (⌘E) ou en source asm68k `.s` (⇧⌘E).
- Aide > À propos (et menu Mirage > À propos sur Mac) : « Mirage 0.4 —
  patch 0004 » ; le numéro est aussi dans le titre de la fenêtre.

Mesures :

- `make test` : 60 vérifications, 0 échec (23 nouvelles : tiles libres,
  champs de l'entrée, export `.s`, sélection, version = dernière section
  de VERSION.md).
- Essai à la souris : rectangle de 18 cases + ⌘-clic = 19 cases, tile
  $02A tapé une fois appliqué aux 19 ; palette 2 posée, autre case, retour :
  la palette 2 est restée.

## 0.5 — sélection reliée à la planche, copier-coller des tiles

Ce que fait la version :

- Tilemap : un clic avec l'outil Sélection choisit le tile de la case
  dans la planche (qui défile jusqu'à lui) et l'ouvre dans l'éditeur de
  tile, dans la palette de la case.
- Palettes : les numéros 1 à 3 sont en noir comme le 0 (ils prenaient
  la couleur F de la ligne précédente) ; la palette choisie est entourée
  d'un cadre rouge épais, en plus du cadre de la couleur.
- Planche : « Dupliquer » est remplacé par Copier / Coller (boutons,
  ⌘C / ⌘V, menu Édition). Le texte passe par le presse-papiers du
  système, au format de l'export .s (« ; Mirage : bloc L x H », puis
  8 lignes dc.l par tile) : on copie un tile ou un bloc, on choisit la
  destination, on colle. On peut aussi coller des dc.l venus de
  n'importe quel source. Le tile 0 reste vide.
- Éditeur de tile : ses boutons Copier / Coller, qui faisaient doublon,
  sont retirés.
- Après une saisie du numéro de tile (Entrée), le clavier revient à la
  tilemap, pour que ⌘C / ⌘V portent sur les tiles.

Mesures :

- `make test` : 67 vérifications, 0 échec (7 nouvelles pour le
  copier-coller : texte .s, forme du bloc, dc.l d'un autre source, tile 0,
  texte illisible refusé).
- Essai à la souris : clic sur le dessus bleu de l'estrade → planche sur
  le tile 5, éditeur sur le tile 5 en palette 1 ; ⌘C, clic sur le
  tile 190 vide, ⌘V → le tile 190 est la copie du 5.

## 0.6 — plan B d'abord, défilement des tiles

Ce que fait la version :

- Tilemap : l'onglet « Plan B » vient en premier (et s'ouvre au
  démarrage), puis « Plan A » : l'ordre du VDP, où B est sous A.
- Éditeur de tile : les indications « Tile N » et « palette N » sont
  retirées (seule reste la mention du tile 0, toujours vide).
- Éditeur de tile : quatre boutons ← → ↑ ↓ « Défiler » décalent le
  dessin du tile affiché d'un pixel, avec enroulement (ce qui sort d'un
  côté rentre de l'autre). Un clic = un pas d'annulation.

Mesures :

- `make test` : 70 vérifications, 0 échec (3 nouvelles : enroulement
  horizontal, vertical, 8 décalages = identité).

## 0.7 — calques, copier-coller de cases, taille libre, tiles déplaçables

Ce que fait la version :

- Tilemap : les onglets sont remplacés par un panneau de calques à droite,
  comme dans Photoshop : « Plan A » au-dessus de « Plan B », une case de
  visibilité pour chacun, un clic sur le nom choisit le calque modifié.
  Les deux calques visibles suivent la composition du VDP (une case du
  plan B prioritaire passe devant une case du plan A non prioritaire).
- Copier-coller de cases dans la tilemap (boutons, ⌘C / ⌘V) : les
  entrées complètes (tile et attributs) du calque modifié. On colle sur
  la case sélectionnée la plus en haut à gauche ; les cases collées
  deviennent la sélection. Texte du presse-papiers : « ; Mirage : cases
  L x H », un masque en commentaire si la sélection n'est pas un
  rectangle (les cases hors masque ne sont pas touchées), puis des dc.w.
- Panneau actif : ⌘C / ⌘V portent sur le dernier panneau cliqué
  (bibliothèque ou tilemap), entouré de bleu ; le menu Édition dit ce
  qui sera copié (« Copier les cases de la tilemap » ou « Copier les
  tiles de la bibliothèque »).
- Taille de carte libre : Nouveau demande la taille (64 x 64 par
  défaut, pas de maximum) ; Édition > Taille de la carte… agrandit ou
  réduit (le contenu reste en haut à gauche ; annulable).
- Bibliothèque : glisser le tile sélectionné (règle du Finder) le
  déplace ; les tiles entre l'ancienne et la nouvelle place glissent
  d'une case et les entrées des deux plans sont recalculées. Le tile 0
  ne bouge pas. Un cadre rouge montre où le tile va se poser.

Mesures :

- `make test` : 86 vérifications, 0 échec (16 nouvelles : copie et
  collage de cases avec masque, bords de carte, déplacement de tile et
  recalcul des plans, tailles de carte).
- Essai à la souris : tile 1 glissé en position 10 → la carte affichée
  est identique au pixel près (0 pixel différent sur 740 x 470) ;
  rectangle de 54 cases copié sur l'estrade et collé sur le sol.
