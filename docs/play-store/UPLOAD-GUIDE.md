# Uploading MultiLaps to Google Play

Everything named here is in this folder, except the app bundle, which is built rather than
kept in git (see below). Menu names were right when this was written; Google renames parts
of Play Console from time to time, so a label may differ slightly.

## What you have

| File | What it is for |
|---|---|
| `MultiLaps-0.3.2-release.aab` | The signed app bundle to upload (version code 5). Not in git: see "Building the bundle" |
| `icon-512.png` | App icon, 512 x 512 |
| `feature-graphic-1024x500.png` | Feature graphic |
| `screenshots/` | Seven phone screenshots, in upload order |
| `listing-text.txt`, `full-description.txt` | Text to paste into the store listing |

The upload key that signs the bundle is not in this repository and must never be. Where
it lives, how to back it up and what to do if it is lost are in [SIGNING.md](SIGNING.md).
Back it up before you upload anything.

## Building the bundle

On a machine set up as SIGNING.md describes:

```
./gradlew bundleRelease
cp app/build/outputs/bundle/release/app-release.aab docs/play-store/MultiLaps-0.3.2-release.aab
```

Git ignores `*.aab`, so the copy stays out of the repository. If the build warns that no
upload key is set, the bundle is unsigned and Play will refuse it.

## 1. Upload to internal testing first

Internal testing puts the real, Play-signed app on your own phone within minutes and has
no review wait. It is where each new release build gets its first run.

1. Play Console > your app > **Test and release** > **Testing** > **Internal testing**.
2. **Testers** tab: create an email list with your own Google account on it, and save.
3. **Releases** tab > **Create new release**.
4. If asked about app signing, accept **Play App Signing** (the default). Google then
   holds the real signing key, and the key on your Mac is only the upload key.
5. Upload `MultiLaps-0.3.2-release.aab`.
6. Release name: leave as offered. Release notes: paste the notes for this version from `listing-text.txt`.
7. **Next** > **Save and publish** (or **Start rollout to internal testing**).

Then, on your Pixel 10:

8. Uninstall the MultiLaps that is on it now. That one is a test build signed with a
   different key, and Android will not install the Play version over it. Its stopwatch
   names and times go with it.
9. Open the opt-in link from the **Testers** tab on the phone, accept, and install from
   Play.
10. Time a few laps. Check the vibration, turning the phone on its side, and killing and
    reopening the app.

## 2. Fill in the store listing

Play Console > **Grow users** > **Store presence** > **Main store listing**.

- App name, short description, full description: from `listing-text.txt` and
  `full-description.txt`.
- App icon: `icon-512.png`.
- Feature graphic: `feature-graphic-1024x500.png`.
- Phone screenshots: the seven in `screenshots/`, in number order. Number 7 is landscape;
  if Play shows it oddly beside the portrait ones, leave it out. Two is the minimum.
- Tablet screenshots are optional. Skip them.

Under **Store settings**: category **Sports**, and your contact email.

Privacy policy link, if you have not set one already:
`https://github.com/emreb/android-multiple-stopwatch/blob/main/PRIVACY.md`

## 3. Check the declarations against the app

You have done these. For reference, what is true of the app:

- **Data safety:** collects no data, shares no data. Nothing leaves the device unless the person shares a result themselves.
- **Ads:** none.
- **App access:** everything is available without sign-in.
- **Target audience:** adults (parents and coaches), 18 and over. Declaring a children's
  audience brings Google's Families rules with it.
- **Permissions:** vibrate only. Play Console may also list
  `com.swetw.multilaps.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. A library adds it, it is
  private to the app, and users are never asked for it.

## 4. Go to production

Which path you get depends on your account.

- **Personal account created after November 2023:** Google requires a closed test first,
  with at least 12 testers opted in for 14 days running. Set that up under **Testing** >
  **Closed testing**, then apply for production access from the dashboard when the 14
  days are up.
- **Organisation account, or an older personal account:** go straight to production.

To release: **Test and release** > **Production** > **Create new release** > add the
bundle from your library (or promote the internal release) > choose countries under
**Countries / regions** > **Next** > **Send for review**.

Review usually takes a few days for a first release. Nothing is public until it passes.

## 5. Later versions

1. In `app/build.gradle.kts`, raise `versionCode` by one (Play refuses a number it has
   seen) and set `versionName` as you like.
2. Run `./gradlew bundleRelease` in the project.
3. Upload `app/build/outputs/bundle/release/app-release.aab` as a new release.
4. If the app's look has changed, retake the screenshots: see `../README.md`.

## Decide before production

- **Free or paid.** A free app can never be made paid later.
- **Version name.** The bundle says 0.3.2. That is what users see. To launch as 1.0.0
  instead, change `versionName` and rebuild before promoting to production.
- **Package name.** `com.swetw.multilaps` is fixed for good from the first upload.
