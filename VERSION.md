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
