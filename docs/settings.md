# Settings

Route `settings`, opened from the gear at the foot of the sidebar.

**UI scale**: how large everything is drawn, as presets or a slider; it applies as it moves and is kept in
`AMP/settings.json` with the sidebar choices of the sandbox and the Models page.

**FILES**: the AMP folder as a tree, everything the app writes (`modules/`, `pluginDir/`, `env/`, the json
files). Every folder opens and closes, every file shows its size, **Refresh** reads it again and **Reveal** opens
it in the file browser. A file or folder is deleted from its row (**Delete**, then the red button to confirm;
a folder goes with everything in it). A folder left empty deletes itself: after a delete the folders left
holding nothing go, and every read of the tree prunes the empty ones, since the app makes any folder it needs
again. The root stays.
