# MultiLaps documents

| Folder | What is in it |
|---|---|
| `play-store/` | What goes into Google Play: the upload walkthrough ([UPLOAD-GUIDE.md](play-store/UPLOAD-GUIDE.md)), the signing key notes ([SIGNING.md](play-store/SIGNING.md)), listing text, icon, feature graphic and six captioned screenshots |
| `marketing/` | The marketing kit PDF, the plain app screenshots it uses, and the sources both it and the store graphics are drawn from |
| `design-history/` | Screenshots from the design rounds, kept for reference: the first plain pass, the redesign, the Start all button, and the icon preview |
| `agents/` | Settings for the coding agents that work on this repository |

The privacy policy is `PRIVACY.md` at the top of the repository, because the store listing
links to it there. The glossary is `CONTEXT.md`, beside it.

## Redrawing the graphics

The store graphics and the kit are web pages turned into images by Chrome, with no build
step. From the top of the repository, with `CHROME` set to your Chrome binary:

```
src="file://$PWD/docs/marketing/source"
shot() { "$CHROME" --headless=new --hide-scrollbars --force-device-scale-factor=1 \
  --window-size=$1 --screenshot="$2" "$src/play-store/$3"; }

shot 512,512   docs/play-store/icon-512.png                  icon.html
shot 1024,500  docs/play-store/feature-graphic-1024x500.png  feature.html
shot 1080,1920 docs/play-store/screenshots/1-time-four-runners.png 1-time-four-runners.html

"$CHROME" --headless=new --no-pdf-header-footer \
  --print-to-pdf=docs/marketing/MultiLaps-marketing-kit.pdf "$src/kit.html"
```

Screenshots 2 to 5 are 1080 x 1920 like the first; number 6 is 1920 x 1080. Google Play
refuses screenshots with transparency, and Chrome writes none here.

A redrawn image looks the same as the committed one but is not the same bytes, so git
will show it as changed. Only commit a redraw when the picture itself has changed. The kit's
body text is set in Avenir Next, which ships with macOS; drawn elsewhere it falls back to
another typeface and the pages reflow.

The pages frame the plain screenshots in `marketing/screenshots/`. Those were taken from
an emulator (a Pixel 9 Pro image) with a race staged on it. If the app's look changes,
retake them under the same file names before redrawing.
