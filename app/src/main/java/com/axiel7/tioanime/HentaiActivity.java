package com.axiel7.tioanime;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HentaiActivity extends AppCompatActivity implements AnimeAdapter.ItemClickListener {
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private Button animeButton;
    private GenreAdapter adapter;
    private ArrayList<String> listGenre;
    private TinyDB tinyDB;
    private ArrayList<String> hentaiUrls;
    private ArrayList<String> hentaiTitles;
    public WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Toolbar toolbar;
    private SearchView searchView;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton favFab;
    private FrameLayout customViewContainer;
    public WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;
    public myWebChromeClient mWebChromeClient;
    public myWebViewClient mWebViewClient;
    public String currentUrl;
    private Pattern mPattern;
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeHentai);
        setContentView(R.layout.activity_hentai);

        //edge to edge support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryHen));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            View view = getWindow().getDecorView();
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        //setup recyclerView for genres
        RecyclerView recyclerView = findViewById(R.id.genres_list_hen);
        recyclerView.setHasFixedSize(true);
        listGenre = new ArrayList<>();
        Collections.addAll(listGenre, getResources().getStringArray(R.array.genres_hentai));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenreAdapter(this, listGenre);
        adapter.setClickListener(this::onItemClick);
        recyclerView.setAdapter(adapter);

        //setup toolbar and drawer
        toolbar = findViewById(R.id.hentai_toolbar);
        drawerLayout = findViewById(R.id.hentai_layout);
        animeButton = findViewById(R.id.change_button_hen);
        animeButton.setOnClickListener(v -> {
            Intent openAnime = new Intent(HentaiActivity.this, MainActivity.class);
            HentaiActivity.this.startActivity(openAnime);
            finish();
        });
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close) {
                public void onDrawerClosed(View view)
                {
                    supportInvalidateOptionsMenu();
                    //drawerOpened = false;
                }
                public void onDrawerOpened(View drawerView)
                {
                    supportInvalidateOptionsMenu();
                    //drawerOpened = true;
                }
            };
            actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
            actionBarDrawerToggle.syncState();
        }

        //setup bottomBar
        bottomNavMenu();

        //setup favorites database
        tinyDB = new TinyDB(this);
        hentaiUrls = tinyDB.getListString("hentaiUrls");
        hentaiTitles = tinyDB.getListString("hentaiTitles");

        //setup webViews complements
        customViewContainer = findViewById(R.id.customViewContainer_hen);
        webView = findViewById(R.id.webView_hen);

        swipeRefreshLayout = findViewById(R.id.swipe);
        swipeRefreshLayout.setOnRefreshListener(() -> webView.loadUrl(currentUrl));

        favFab = findViewById(R.id.fab_hen);
        favFab.bringToFront();

        mWebViewClient = new myWebViewClient();
        webView.setWebViewClient(mWebViewClient);

        mWebChromeClient = new myWebChromeClient();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(mWebChromeClient);

        //check if should load a favorite url
        String openFavUrl = tinyDB.getString("openFavUrlHen");
        if (openFavUrl.equals("")) {
            currentUrl = "https://tiohentai.com";
        }
        else {
            currentUrl = openFavUrl;
        }

        webView.loadUrl(currentUrl);
        tinyDB.putString("openFavUrlHen", "");
        mPattern = Pattern.compile("(http|https)://tiohentai.com/hentai/.*");
        if (currentUrl != null) {
            checkUrl(currentUrl);
        }
    }
    private void bottomNavMenu() {
        bottomNavigationView = findViewById(R.id.bottom_navigation_hen);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_home_hen:
                    webView.loadUrl("https://tiohentai.com");
                    break;
                case R.id.navigation_censor:
                    webView.loadUrl("https://tiohentai.com/directorio?censura=no");
                    break;
                case R.id.navigation_favorites_hen:
                    Intent openFav = new Intent(HentaiActivity.this, FavHenActivity.class);
                    HentaiActivity.this.startActivity(openFav);
                    break;
            }
            return true;
        });
    }
    public void saveFav(View view) {
        String title = webView.getTitle();
        title = title.replaceAll(" - TioHentai","");
        if (!hentaiUrls.contains(currentUrl)) {
            hentaiUrls.add(currentUrl);
            hentaiTitles.add(title);
            Toast.makeText(this,getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
        }
        else if (hentaiUrls.contains(currentUrl)) {
            hentaiUrls.remove(currentUrl);
            hentaiTitles.remove(title);
            Toast.makeText(this,getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show();
        }
        for (String s : hentaiUrls) {
            if (!hentaiUrls.contains(s)) {
                tinyDB.putString("hentaiUrl"+s,s);
            }
            break;
        }
        for (String s : hentaiTitles) {
            if (!hentaiTitles.contains(s)) {
                tinyDB.putString("hentaiTitle"+s,s);
            }
            break;
        }
        tinyDB.putListString("hentaiUrls",hentaiUrls);
        tinyDB.putListString("hentaiTitles",hentaiTitles);
        if (currentUrl != null) {
            checkUrl(currentUrl);
        }
    }
    @Override
    public void onItemClick(View view, int position) {
        String value = getResources().getStringArray(R.array.genres_hentai_values)[position];
        webView.loadUrl("https://tiohentai.com/directorio?genero=" + value);
        drawerLayout.closeDrawer(GravityCompat.START);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        //setup searchView
        MenuItem myActionMenuItem = menu.findItem( R.id.menu_search);
        searchView = (SearchView) myActionMenuItem.getActionView();
        searchView.setQueryHint(getString(R.string.title_search));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                webView.loadUrl("https://tiohentai.com/directorio?q=" + query);
                if( ! searchView.isIconified()) {
                    searchView.setIconified(true);
                }
                myActionMenuItem.collapseActionView();
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                // UserFeedback.show( "SearchOnQueryTextChanged: " + s);
                return false;
            }

        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            webView.reload();
            return true;
        }
        if(actionBarDrawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
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
        hentaiUrls = tinyDB.getListString("hentaiUrls");
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        if (inCustomView()) {
            hideCustomView();
        }
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
            drawerLayout.setFitsSystemWindows(false);
            bottomNavigationView.setVisibility(View.GONE);

        }

        @SuppressLint("InflateParams")
        @Override
        public View getVideoLoadingProgressView() {

            if (mVideoProgressView == null) {
                LayoutInflater inflater = LayoutInflater.from(HentaiActivity.this);
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
            bottomNavigationView.setVisibility(View.VISIBLE);
            drawerLayout.setFitsSystemWindows(true);
            toolbar.setFitsSystemWindows(true);

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
    }
    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    private class myWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            currentUrl=url;
            if (currentUrl != null) {
                checkUrl(currentUrl);
            }
            return super.shouldOverrideUrlLoading(view, url);    //To change body of overridden methods use File | Settings | File Templates.
        }
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            swipeRefreshLayout.setRefreshing(true);
            currentUrl=webView.getUrl();
            //searchView.clearFocus();
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
            }
        }
    }
    private void checkUrl(String currentUrl) {
        Matcher matcher = mPattern.matcher(currentUrl);
        if (matcher.find()) {
            favFab.setVisibility(View.VISIBLE);
            if (hentaiUrls.contains(currentUrl)) {
                favFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_white_24dp));
            }
            else {
                favFab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_favorite_border_white_24dp));
            }
        }
        else {
            favFab.setVisibility(View.INVISIBLE);
        }
    }
}