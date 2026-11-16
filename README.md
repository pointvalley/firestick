# Firestick home screen

Custom home screen project for a Fire TV Stick 4K Max.

## Links

- **TV page (live):** https://pointvalley.github.io/firestick/ (open in Silk on the Firestick and bookmark it)
- **Sandbox:** https://pointvalley.github.io/firestick/sandbox.html (design and preview on PC)
- **Wallpaper loops:** [wallpapers/](wallpapers/)
- **Loop recorder:** [loop-recorder/](loop-recorder/)
- **Firestick Home app:** install with the Downloader app → type `pointvalley.github.io/firestick/get`

## Firestick Home app (`firetv-app/`)

A small Fire TV app that shows the TV page full screen and makes the tiles work for real:

- **App** tiles open the installed Fire TV app (Netflix, YouTube, Prime Video, Disney+, Crave, Sportsnet+ and more), falling back to Silk if it isn't installed.
- **Deep link** tiles open whichever app handles the link; **Web** tiles open in Silk.
- A **My apps** row lists every installed app automatically.
- **≡ (Menu)** on the remote opens the Theme panel; Back closes it.
- Tries to open on boot (Projectivy "Launch on Boot" is the backup).

GitHub builds the APK automatically whenever anything in `firetv-app/` changes (see the **Actions** tab) and publishes it under **Releases**.

## Theme panel

On the TV page, press ▲ from the top row to reach **⚙ Theme** (or ≡ in the app). Change wallpaper, background, highlight colour, widgets, row wallpapers and tile size with the remote. Choices are saved on that TV; **Reset to default** goes back to the layout from the sandbox.

> Use the GitHub sandbox link above. The older claude.ai sandbox doesn't support video loops or the ticker.

## Files

| Path | What it is |
|---|---|
| `index.html` | The TV page. Open `https://pointvalley.github.io/firestick/` in Silk on the Firestick and bookmark it. |
| `sandbox.html` | The design sandbox (Live vs Draft compare). Use **Get TV page** and paste the result into `index.html` to update the TV. |
| `wallpapers/` | Seamless 1080p MP4 loops and PNG stills for Projectivy wallpapers. |
| `firetv-app/` | Source for the Firestick Home app (built by GitHub Actions). |
| `get/` | Download page for the app (`pointvalley.github.io/firestick/get`). |
| `loop-recorder/` | Windows tool: record a Lively wallpaper or convert a video into a seamless loop. |

Launcher plan: Projectivy Launcher + Launch on Boot. Premium adds video and per-row wallpapers.
