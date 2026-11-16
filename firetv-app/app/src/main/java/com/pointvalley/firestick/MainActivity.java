package com.pointvalley.firestick;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.Base64;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Firestick Home: shows the TV page from GitHub Pages full screen and turns its
 * tiles into real actions (open installed apps, deep links, or Silk for websites).
 */
public class MainActivity extends Activity {

    static final String HOME_URL = "https://pointvalley.github.io/firestick/";
    static final String HOME_HOST = "pointvalley.github.io";
    static final String SILK = "com.amazon.cloud9";

    /**
     * Website domain -> Fire TV app package(s) to try, in order ("|" separated).
     * If none are installed the link opens in Silk instead.
     */
    static final String[][] APP_FOR_SITE = {
            {"netflix.com", "com.netflix.ninja"},
            {"youtube.com", "com.amazon.firetv.youtube|com.google.android.youtube.tv|com.google.android.youtube"},
            {"youtu.be", "com.amazon.firetv.youtube|com.google.android.youtube.tv|com.google.android.youtube"},
            {"primevideo.com", "com.amazon.avod|com.amazon.avod.thirdpartyclient"},
            {"disneyplus.com", "com.disney.disneyplus"},
            {"crave.ca", "ca.bellmedia.cravetv|ca.bellmedia.crave"},
            {"sportsnet.ca", "com.rogers.sportsnet.sportsnetnow|com.rogers.sportsnet"},
            {"spotify.com", "com.spotify.tv.android"},
            {"twitch.tv", "tv.twitch.android.viewer|tv.twitch.android.app"},
            {"plex.tv", "com.plexapp.android"},
            {"tubitv.com", "com.tubitv"},
            {"pluto.tv", "tv.pluto.android"},
            {"crunchyroll.com", "com.crunchyroll.crunchyroid"},
            {"max.com", "com.wbd.stream|com.hbo.hbonow"},
            {"paramountplus.com", "com.cbs.ott"},
            {"dazn.com", "com.dazn"},
            {"nhl.com", "com.nhl.gc1112.free"},
            {"teamsnap.com", "com.teamsnap.teamsnap"},
    };

    WebView web;
    final Handler main = new Handler(Looper.getMainLooper());
    boolean showingError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); // video wallpapers autoplay
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        // lets http:// video streams play on the https home page (M3U player)
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        web.addJavascriptInterface(new Bridge(), "FireTV");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showOffline();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && url.startsWith("http")) showingError = false;
            }
        });

        setContentView(web);
        hideSystemUi();
        web.loadUrl(HOME_URL);
        web.requestFocus();
    }

    /** Page links that leave the home page become app launches instead of browsing away. */
    boolean handleNavigation(String url) {
        if (url == null) return true;
        Uri u = Uri.parse(url);
        if (HOME_HOST.equalsIgnoreCase(u.getHost())) return false; // stay inside the home page
        openTarget(url, "", "app");
        return true;
    }

    /**
     * Opens what a tile points at.
     * kind "app": launch the package (given, or matched from the website), else Silk.
     * kind "deeplink": let Android pick the app that handles the link, else Silk.
     * kind "web": open in Silk.
     */
    void openTarget(String url, String pkg, String kind) {
        if (url != null && url.startsWith("app://")) {
            pkg = url.substring(6);
            kind = "app";
        }
        if (pkg != null && !pkg.isEmpty() && launchAny(pkg)) return;

        if ("app".equals(kind) || kind == null || kind.isEmpty()) {
            String mapped = packageForUrl(url);
            if (mapped != null && launchAny(mapped)) return;
        }
        if ("deeplink".equals(kind) && openWithAnyApp(url)) return;
        openInSilk(url);
    }

    String packageForUrl(String url) {
        if (url == null) return null;
        String host = Uri.parse(url).getHost();
        if (host == null) return null;
        host = host.toLowerCase(Locale.ROOT);
        for (String[] row : APP_FOR_SITE) {
            if (host.equals(row[0]) || host.endsWith("." + row[0])) return row[1];
        }
        return null;
    }

    /** Tries each "|" separated package until one launches. */
    boolean launchAny(String pkgs) {
        for (String p : pkgs.split("\\|")) {
            p = p.trim();
            if (!p.isEmpty() && launch(p)) return true;
        }
        return false;
    }

    boolean launch(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = null;
        try {
            i = pm.getLeanbackLaunchIntentForPackage(pkg);
        } catch (Exception ignored) {
        }
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean openWithAnyApp(String url) {
        if (url == null || !url.startsWith("http")) return false;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    void openInSilk(String url) {
        if (url == null || !url.startsWith("http")) return;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setPackage(SILK);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            // no Silk: try any browser, and as a last resort show it here (Back returns home)
            if (!openWithAnyApp(url)) web.loadUrl(url);
        }
    }

    void showOffline() {
        if (showingError) return;
        showingError = true;
        String html = "<html><body style='margin:0;background:#06080f;color:#fff;font-family:sans-serif;"
                + "display:flex;align-items:center;justify-content:center;height:100vh'>"
                + "<div style='text-align:center'><h1 style='font-weight:300'>Can't reach the home page</h1>"
                + "<p style='opacity:.6'>Check the Wi-Fi. Retrying automatically&hellip;</p></div></body></html>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        main.postDelayed(() -> web.loadUrl(HOME_URL), 10000);
    }

    void hideSystemUi() {
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (web != null) {
            web.onResume();
            web.requestFocus();
        }
    }

    @Override
    protected void onPause() {
        if (web != null) web.onPause(); // pauses video wallpapers while another app is open
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) { // ≡ button opens the Theme panel
            web.evaluateJavascript("window.TV&&TV.togglePanel&&TV.togglePanel()", null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        String cur = web.getUrl();
        if (cur != null && !cur.startsWith(HOME_URL) && web.canGoBack()) {
            web.goBack();
        } else if (cur == null || !cur.startsWith(HOME_URL)) {
            web.loadUrl(HOME_URL);
        }
        else {
            // on the home page Back only closes the Theme panel, like a real home screen
            web.evaluateJavascript("window.TV&&TV.closePanel&&TV.closePanel()", null);
        }
    }

    final HashMap<String, String> iconCache = new HashMap<>();

    /** First installed package from a "|" list, or null. */
    String installedOf(String pkgs) {
        if (pkgs == null) return null;
        for (String p : pkgs.split("\\|")) {
            p = p.trim();
            if (p.isEmpty()) continue;
            try {
                getPackageManager().getPackageInfo(p, 0);
                return p;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** The installed app's icon as a 128px PNG data URL, for tiles set to "Website / app icon". */
    String iconFor(String url, String pkg) {
        String p = null;
        if (pkg != null && !pkg.isEmpty()) p = installedOf(pkg);
        if (p == null && url != null && url.startsWith("app://")) p = installedOf(url.substring(6));
        if (p == null) p = installedOf(packageForUrl(url));
        if (p == null) return "";
        synchronized (iconCache) {
            if (iconCache.containsKey(p)) return iconCache.get(p);
        }
        String out = "";
        try {
            Drawable d = getPackageManager().getApplicationIcon(p);
            Bitmap b;
            int size = 128;
            if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
                b = Bitmap.createScaledBitmap(((BitmapDrawable) d).getBitmap(), size, size, true);
            } else {
                b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                d.setBounds(0, 0, size, size);
                d.draw(c);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.PNG, 100, bos);
            out = "data:image/png;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception ignored) {
        }
        synchronized (iconCache) {
            iconCache.put(p, out);
        }
        return out;
    }

    /** Fire TV screen mirroring (Miracast) screen; falls back to cast settings or AirScreen. */
    boolean openMirroring() {
        Intent[] tries = {
                new Intent().setComponent(new ComponentName("com.amazon.cast.sink", "com.amazon.cast.sink.DisplayMirroringSinkActivity")),
                new Intent(Settings.ACTION_CAST_SETTINGS),
        };
        for (Intent i : tries) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(i);
                return true;
            } catch (Exception ignored) {
            }
        }
        return launchAny("com.ionitech.airscreen");
    }

    /** Downloads text (an M3U playlist) for the page, bypassing website-only restrictions. */
    void fetchText(String url, int id) {
        new Thread(() -> {
            String body = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(20000);
                c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent", "FirestickHome/1.0");
                if (c.getResponseCode() < 400) {
                    InputStream in = c.getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buf = new byte[16384];
                    int n, total = 0;
                    while ((n = in.read(buf)) > 0 && total < 8 * 1024 * 1024) {
                        bos.write(buf, 0, n);
                        total += n;
                    }
                    in.close();
                    body = bos.toString("UTF-8");
                }
                c.disconnect();
            } catch (Exception ignored) {
            }
            final String js = "window.TV&&TV.fetched&&TV.fetched(" + id + "," + (body == null ? "null" : JSONObject.quote(body)) + ")";
            main.post(() -> web.evaluateJavascript(js, null));
        }).start();
    }

    /** Available to the page as window.FireTV */
    class Bridge {
        @JavascriptInterface
        public void open(String url, String pkg, String kind) {
            main.post(() -> openTarget(url, pkg, kind));
        }

        @JavascriptInterface
        public boolean isInstalled(String pkg) {
            try {
                getPackageManager().getPackageInfo(pkg, 0);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        /** Installed apps that can be opened, as JSON [{label, pkg}] sorted by name. */
        @JavascriptInterface
        public String apps() {
            PackageManager pm = getPackageManager();
            JSONArray out = new JSONArray();
            Set<String> seen = new HashSet<>();
            String[] cats = {"android.intent.category.LEANBACK_LAUNCHER", Intent.CATEGORY_LAUNCHER};
            java.util.ArrayList<String[]> list = new java.util.ArrayList<>();
            for (String cat : cats) {
                Intent q = new Intent(Intent.ACTION_MAIN);
                q.addCategory(cat);
                List<ResolveInfo> found = pm.queryIntentActivities(q, 0);
                for (ResolveInfo r : found) {
                    String p = r.activityInfo.packageName;
                    if (p.equals(getPackageName()) || !seen.add(p)) continue;
                    ApplicationInfo ai = r.activityInfo.applicationInfo;
                    list.add(new String[]{String.valueOf(pm.getApplicationLabel(ai)), p});
                }
            }
            java.util.Collections.sort(list, (a, b) -> a[0].compareToIgnoreCase(b[0]));
            try {
                for (String[] a : list) {
                    JSONObject o = new JSONObject();
                    o.put("label", a[0]);
                    o.put("pkg", a[1]);
                    out.put(o);
                }
            } catch (Exception ignored) {
            }
            return out.toString();
        }

        @JavascriptInterface
        public String appIcon(String url, String pkg) {
            return iconFor(url, pkg);
        }

        @JavascriptInterface
        public boolean mirror() {
            return openMirroring();
        }

        @JavascriptInterface
        public void fetchText(String url, int id) {
            MainActivity.this.fetchText(url, id);
        }

        @JavascriptInterface
        public String version() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                return "";
            }
        }
    }
}
