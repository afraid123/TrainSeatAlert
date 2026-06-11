# Publishing Train Seat Alert to the Google Play Store

Everything in the **code/build** is now production-ready:
- ✅ Embedded API key (works out of the box, no setup needed by users)
- ✅ Cleaned permissions (removed flagged/unused ones)
- ✅ HTTPS only (cleartext disabled)
- ✅ Code shrinking + ProGuard enabled
- ✅ Signed **AAB** (Android App Bundle — the format Play requires)
- ✅ Persistent upload key (same key reused for every update)
- ✅ versionCode 2 / versionName 1.1.0

## Step 1 — Build the signed release
1. Go to the repo **Actions** tab → **"Build Signed Release (AAB + APK)"**
2. Click **"Run workflow"** → **Run**
3. First run generates a signing key and commits it to `keystore/` (one time only)
4. When green, download the **`TrainSeatAlert-AAB-PlayStore`** artifact → unzip → you get `app-release.aab`

> ⚠️ Keep the `keystore/release.jks.base64` file in the repo forever. It is the
> upload key. If you lose it you can still recover via Play App Signing key reset,
> but it's easiest to never delete it.

## Step 2 — Create a Play Console account
1. Go to https://play.google.com/console
2. Pay the **one-time $25** registration fee
3. Complete identity verification (can take 1–2 days)

## Step 3 — Create the app
1. Play Console → **Create app**
2. App name: **Train Seat Alert**
3. Default language: English; App or Game: **App**; Free
4. Accept declarations

## Step 4 — Required setup (Play Console will checklist these)
- **Privacy policy URL**: host `PRIVACY_POLICY.md` content somewhere public and
  paste the URL. Easiest: enable **GitHub Pages** on this repo, or paste it into a
  free https://telegra.ph page or a GitHub Gist "raw" URL.
- **Data safety form**: declare "Data shared: train search details sent to RapidAPI";
  "Data collected: none"; no, data is not sold.
- **App content**: Target audience (13+), no ads, content rating questionnaire
  (answer No to all sensitive categories → rated Everyone).
- **Store listing**:
  - Short description (≤80 chars): _Get a loud alarm the moment train seats open up on your route._
  - Full description: see `store_listing.txt` (below).
  - App icon: 512×512 PNG (I can generate one if you want).
  - Feature graphic: 1024×500 PNG.
  - Screenshots: at least 2 phone screenshots (take them on your phone).

## Step 5 — Upload the AAB
1. Play Console → **Production** → **Create new release**
2. Choose **Play App Signing** (recommended — Google manages the real key)
3. Upload `app-release.aab`
4. Add release notes (e.g. "Initial release")
5. **Review release** → **Start rollout to production**

## Step 6 — Wait for review
Google review typically takes a few hours to a few days for a new app. You'll get
an email when it's live.

---

## Honest caveats you should know
1. **API quota**: the embedded RapidAPI key has a shared free-tier limit. If many
   people install the app, the free quota will run out and seat checks will fail
   for everyone until the quota resets. For a public app you'd want a paid RapidAPI
   plan, or have users supply their own key (the app already supports this in
   Settings).
2. **Data source terms**: scraping/relaying Indian Railways availability via a
   third-party API may have its own terms — review RapidAPI's and the provider's
   terms before commercial distribution.
3. **The embedded key is visible** to anyone who decompiles the APK. Rotate it on
   RapidAPI if it gets abused.
