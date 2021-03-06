package com.axiel7.tioanime;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.enums.AppUpdaterError;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.github.javiersantos.appupdater.objects.Update;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    public final static String tioAnimeUrl = "https://tioanime.com";
    private CoordinatorLayout rootLayout, snackbarLocation;
    private TinyDB tinyDB;
    private ArrayList<String> animeUrls, animeTitles;
    public WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton favFab, commentsFab, filterFab;
    private FrameLayout customViewContainer;
    public WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;
    public myWebChromeClient mWebChromeClient;
    public myWebViewClient mWebViewClient;
    public String currentUrl, externalUrl;
    private Pattern mPattern, episodePattern;
    private AppUpdaterUtils appUpdater;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup layouts
        rootLayout = findViewById(R.id.main_layout);
        snackbarLocation = findViewById(R.id.snackbar_location);

        //setup bottomBar
        bottomNavMenu();

        //setup webViews complements
        customViewContainer = findViewById(R.id.customViewContainer);
        webView = findViewById(R.id.webView);
        webView.setBackgroundColor(Color.TRANSPARENT);

        swipeRefreshLayout = findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(() -> webView.loadUrl(currentUrl));

        favFab = findViewById(R.id.floatingActionButton);
        favFab.bringToFront();
        commentsFab = findViewById(R.id.commentsFab);
        commentsFab.bringToFront();
        filterFab = findViewById(R.id.filtersFab);
        filterFab.bringToFront();

        mWebViewClient = new myWebViewClient();
        webView.setWebViewClient(mWebViewClient);

        mWebChromeClient = new myWebChromeClient();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        //allow cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(mWebChromeClient);

        //handle external links
        handleIntent(getIntent());

        //setup favorites database
        tinyDB = new TinyDB(this);
        animeUrls = tinyDB.getListString("animeUrls");
        animeTitles = tinyDB.getListString("animeTitles");

        //check if should load a favorite or genre url
        String openFavGenreUrl = tinyDB.getString("openFavGenreUrl");
        if (openFavGenreUrl.equals("")) {
            currentUrl = tioAnimeUrl;
        } else {
            currentUrl = openFavGenreUrl;
        }
        //check if should load an external link
        if (externalUrl != null) {
            webView.loadUrl(externalUrl);
        }
        else {
            webView.loadUrl(currentUrl);
        }
        tinyDB.putString("openFavGenreUrl", "");

        //setup app updater
        appUpdater = new AppUpdaterUtils(this)
                .setUpdateFrom(UpdateFrom.GITHUB)
                .setGitHubUserAndRepo("axiel7", "TioAnime")
                .withListener(new AppUpdaterUtils.UpdateListener() {
                    @Override
                    public void onSuccess(Update update, Boolean isUpdateAvailable) {
                        Snackbar snackbar = Snackbar.make(snackbarLocation,"Actualización disponible",Snackbar.LENGTH_INDEFINITE)
                                .setAction("Descargar", v -> {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(update.getUrlToDownload().toString()));
                                    startActivity(intent);
                                });
                        snackbar.setBackgroundTint(getResources().getColor(R.color.defaultDark));
                        snackbar.setTextColor(getResources().getColor(R.color.colorText));
                        snackbar.setActionTextColor(getResources().getColor(R.color.colorAccent));
                        if (isUpdateAvailable) {
                            snackbar.show();
                        }
                    }

                    @Override
                    public void onFailed(AppUpdaterError error) {
                        Toast.makeText(MainActivity.this, "AppUpdater: Algo salió mal :s", Toast.LENGTH_SHORT).show();
                    }
                });
        if (tinyDB.getBoolean("searchUpdates")) {
            appUpdater.start();
        }
        else {
            appUpdater.stop();
        }

        //other
        mPattern = Pattern.compile("(http|https)://(tioanime.com/anime/|tiohentai.com/hentai/).*");
        episodePattern = Pattern.compile("(http|https)://(tioanime.com/ver/|tiohentai.com/ver/).*");
        if (currentUrl != null) {
            checkUrl(currentUrl);
        }
    }
    private void bottomNavMenu() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    swipeRefreshLayout.setRefreshing(true);
                    webView.loadUrl(tioAnimeUrl);
                    break;
                case R.id.navigation_schedule:
                    swipeRefreshLayout.setRefreshing(true);
                    webView.loadUrl(tioAnimeUrl + "/programacion");
                    break;
                case R.id.navigation_genres:
                    Intent openGenres = new Intent(MainActivity.this, GenresActivity.class);
                    MainActivity.this.startActivity(openGenres);
                    break;
                case R.id.navigation_favorites:
                    Intent openFav = new Intent(MainActivity.this, FavActivity.class);
                    MainActivity.this.startActivity(openFav);
                    break;
                case R.id.navigation_settings:
                    Intent openSettings = new Intent(MainActivity.this, SettingsActivity.class);
                    MainActivity.this.startActivity(openSettings);
                    break;
            }
            return true;
        });
    }
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    private void handleIntent(Intent intent) {
        String appLinkAction = intent.getAction();
        Uri appLinkData = intent.getData();
        if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null){
            String recipeId = appLinkData.getPath();
            externalUrl = tioAnimeUrl + recipeId;
        }
    }
    public void saveFav(View view) {
        String title = webView.getTitle();
        title = title.replaceAll(" - Tio(Anime|Hentai)","");
        if (!animeUrls.contains(currentUrl)) {
            animeUrls.add(currentUrl);
            animeTitles.add(title);
            Toast.makeText(this,getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
        }
        else if (animeUrls.contains(currentUrl)) {
            animeUrls.remove(currentUrl);
            animeTitles.remove(title);
            Toast.makeText(this,getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show();
        }
        tinyDB.putListString("animeUrls",animeUrls);
        tinyDB.putListString("animeTitles", animeTitles);
        if (currentUrl != null) {
            checkUrl(currentUrl);
        }
    }
    public void viewComments(View view) {
        webView.loadUrl("javascript:document.getElementById('disqus_thread').scrollIntoView();");
    }
    public void hideFilters(View view) {
        webView.loadUrl("javascript:(function() { " +
                "var head = document.getElementsByClassName('filter-bx')[0].style.display='none'; " +
                "})()");
    }
    public void showFilters(View view) {
        webView.loadUrl("javascript:(function() { " +
                "var head = document.getElementsByClassName('filter-bx')[0].style.display='block'; " +
                "})()");
    }
    public boolean inCustomView() {
        return (mCustomView != null);
    }

    public void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        webView.onResume();
        animeUrls = tinyDB.getListString("animeUrls");
        animeTitles = tinyDB.getListString("animeTitles");
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        if (inCustomView()) {
            hideCustomView();
        }
        appUpdater.stop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.clearHistory();
        webView.removeAllViews();
        webView.destroy();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (inCustomView()) {
                hideCustomView();
                return true;
            }

            if ((mCustomView == null) && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    class myWebChromeClient extends WebChromeClient {
        private View mVideoProgressView;

        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg)
        {
            WebView.HitTestResult result = view.getHitTestResult();
            String data = result.getExtra();
            Context context = view.getContext();
            if (data != null) {
                if (data.startsWith("https://disqus.com/embed")) {
                    mDialog("Después de iniciar sesión volverás a la página principal y ya podrás comentar.",
                            "Inicia sesión en disqus por primera vez",
                            getString(R.string.ok),
                            "");
                    webView.loadUrl("https://disqus.com/profile/login/?next=https%3A%2F%2Fdisqus.com%2Fhome%2Finbox%2F&forum=https-tioanime-com");
                    return true;
                }
                else if (data.contains("disquscdn.com")) {
                    Toast.makeText(MainActivity.this, "No soportado", Toast.LENGTH_SHORT).show();
                    return true;
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
                context.startActivity(browserIntent);
            }
            return false;
        }

        @SuppressLint("SourceLockedOrientationActivity")
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {

            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;
            webView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            hideSystemUI();
        }

        @SuppressLint("InflateParams")
        @Override
        public View getVideoLoadingProgressView() {

            if (mVideoProgressView == null) {
                LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                mVideoProgressView = inflater.inflate(R.layout.video_progress, null);
            }
            return mVideoProgressView;
        }

        @SuppressLint("SourceLockedOrientationActivity")
        @Override
        public void onHideCustomView() {
            super.onHideCustomView();    //To change body of overridden methods use File | Settings | File Templates.
            if (mCustomView == null)
                return;

            webView.setVisibility(View.VISIBLE);
            customViewContainer.setVisibility(View.GONE);

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(mCustomView);
            mCustomView = null;
            customViewCallback.onCustomViewHidden();

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            showSystemUI();
        }
    }
    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        rootLayout.setFitsSystemWindows(false);
        bottomNavigationView.setVisibility(View.GONE);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        rootLayout.setFitsSystemWindows(true);
        bottomNavigationView.setVisibility(View.VISIBLE);
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
    }
    private class myWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            return super.shouldOverrideUrlLoading(view, url);
        }
        @Override
        public void onLoadResource (WebView view, String url) {
            Uri uri = Uri.parse(url);
            boolean shouldPlayVlc = tinyDB.getBoolean("playWithVlc");
            boolean isPlayable = url.endsWith(".mp4") && url.contains("storage.googleapis.com");
            if (shouldPlayVlc && isPlayable) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setPackage("org.videolan.vlc");
                intent.setComponent(new ComponentName("org.videolan.vlc", "org.videolan.vlc.gui.video.VideoPlayerActivity"));
                intent.setDataAndTypeAndNormalize(uri,"video/*");
                intent.putExtra("url", uri);
                startActivity(intent);
            }
            else if (shouldPlayVlc && url.endsWith(".mp4")) {
                Toast.makeText(MainActivity.this, "Opción no soportada, se reproducirá en la app", Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            swipeRefreshLayout.setRefreshing(true);
            currentUrl=webView.getUrl();
            if (currentUrl != null) {
                checkUrl(currentUrl);
            }
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            swipeRefreshLayout.setRefreshing(false);
            currentUrl=webView.getUrl();
            if (currentUrl != null) {
                checkUrl(currentUrl);
                if (currentUrl.equals(tioAnimeUrl) || currentUrl.equals(tioAnimeUrl + "/")) {
                    webView.loadUrl("javascript:(function() { " +
                            "var head = document.getElementsByClassName('row latest flex-nowrap')[0].style.display='none'; " +
                            "})()");
                }
                else if (currentUrl.startsWith(tioAnimeUrl + "/directorio")) {
                    hideFilters(webView);
                }
            }
        }
    }
    private void checkUrl(String currentUrl) {
        if (currentUrl.equals("https://disqus.com/home/inbox/")) {
            webView.loadUrl(tioAnimeUrl);
        }
        Matcher matcher = mPattern.matcher(currentUrl);
        Matcher commentsMatcher = episodePattern.matcher(currentUrl);
        if (matcher.find()) {
            favFab.setVisibility(View.VISIBLE);
            if (animeUrls.contains(currentUrl)) {
                favFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_white_24dp));
            }
            else {
                favFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_border_white_24dp));
            }
        }
        else {
            favFab.setVisibility(View.GONE);
        }
        if (commentsMatcher.find()) {
            commentsFab.setVisibility(View.VISIBLE);
        }
        else {
            commentsFab.setVisibility(View.GONE);
        }
        if (currentUrl.startsWith(tioAnimeUrl + "/directorio")) {
            filterFab.setVisibility(View.VISIBLE);
        }
        else {
            filterFab.setVisibility(View.GONE);
        }
    }
    public void mDialog(String message, String title, String positive, String negative) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton(positive, (dialog, which) -> {
                })
                .setNegativeButton(negative, (dialog, which) -> {
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
