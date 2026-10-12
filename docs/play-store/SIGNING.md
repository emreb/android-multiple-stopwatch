# The MultiLaps upload key

Every build uploaded to Google Play has to be signed with the same upload key. The key and
its password are kept off this repository, on the machine that builds releases. This page
says where, and how to look after them. It contains no secrets.

## What exists, and where

| Thing | Where | Notes |
|---|---|---|
| Keystore | `~/.multilaps-signing/multilaps-upload.jks` | PKCS12, one RSA 4096 key, alias `multilaps-upload`, valid to 2056 |
| Password | `~/.gradle/gradle.properties` | The store password and the key password are the same |

The build reads four Gradle properties, which that file sets:

```
MULTILAPS_UPLOAD_STORE_FILE=/full/path/to/multilaps-upload.jks
MULTILAPS_UPLOAD_STORE_PASSWORD=...
MULTILAPS_UPLOAD_KEY_ALIAS=multilaps-upload
MULTILAPS_UPLOAD_KEY_PASSWORD=...
```

Everything hangs on `MULTILAPS_UPLOAD_STORE_FILE`:

- All four set: `./gradlew bundleRelease` produces a signed bundle.
- Store file set, another one missing: every Gradle command stops, tests and debug builds
  included, and names the missing one.
- Store file missing or blank: the build succeeds and the bundle is **unsigned**, whatever
  the other three say. Asking for a task by a name containing "Release", such as
  `bundleRelease`, prints a warning; `./gradlew bundle`, `build` or a shortened name such
  as `bR` does not.

So check who signed a bundle, as below, before uploading it.

## Backing it up

Two things need backing up, and both matter: the `.jks` file and the password. Either one
alone is useless.

1. Put the password in a password manager.
2. Attach the `.jks` file to the same entry, or keep it in another private place you will
   still have in ten years.
3. Do not email it, put it in a shared drive, or commit it. `.gitignore` refuses `*.jks`
   and its relatives as a backstop, not as permission to keep one in the project folder.

## Setting up another machine

1. Copy the `.jks` file to it, anywhere outside the project.
2. Add the four properties above to `~/.gradle/gradle.properties` on that machine, with
   the path to where you put the file.
3. `chmod 600` both files.
4. Run `./gradlew bundleRelease` and check there is no warning about a missing key.

## Checking a key or a bundle

Fingerprint of the key, to compare with the upload certificate Play Console shows under
**Test and release** > **App integrity** (older consoles: **Setup** > **App signing**):

```
keytool -list -v -keystore ~/.multilaps-signing/multilaps-upload.jks -alias multilaps-upload
```

Who signed a bundle:

```
jarsigner -verify -verbose -certs docs/play-store/MultiLaps-0.1.0-release.aab | grep "CN="
```

It should name `CN=MultiLaps upload key`.

## If the key or password is lost, or leaks

MultiLaps uses Play App Signing, so Google holds the key that users' phones actually
trust, and the upload key only proves an upload came from you. That makes a lost or
leaked upload key recoverable:

1. Make a new keystore: `keytool -genkeypair -v -storetype PKCS12 -keystore new-upload.jks
   -alias multilaps-upload -keyalg RSA -keysize 4096 -validity 10950`.
2. Export its certificate: `keytool -export -rfc -keystore new-upload.jks -alias
   multilaps-upload -file upload_certificate.pem`.
3. In Play Console, on the app signing page, request an upload key reset and give Google
   the `.pem` file. The account owner has to make the request.
4. Google confirms after a day or two. Point `MULTILAPS_UPLOAD_STORE_FILE` and the
   passwords at the new keystore from then on.

Installed copies of the app are unaffected, and no data is lost.
