package com.axiel7.tioanime;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class FavHenActivity extends AppCompatActivity implements AnimeAdapter.ItemClickListener {
    private AnimeAdapter adapter;
    private TinyDB tinyDB;
    private Map<String, String> hentaiMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeHentai);
        setContentView(R.layout.activity_fav_hen);
        //edge to edge support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDarkHen));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            View view = getWindow().getDecorView();
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        //setup toolbar
        Toolbar toolbar = findViewById(R.id.fav_toolbar_hen);
        setSupportActionBar(toolbar);

        //setup favorites database
        tinyDB = new TinyDB(this);
        ArrayList<String> hentaiUrls = tinyDB.getListString("hentaiUrls");
        ArrayList<String> hentaiTitles = tinyDB.getListString("hentaiTitles");
        if (hentaiMap==null) {
            hentaiMap = new LinkedHashMap<>();
        }
        for (int i=0; i<hentaiUrls.size(); i++) {
            hentaiMap.put(hentaiUrls.get(i), hentaiTitles.get(i));
        }

        //setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.favListHen);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnimeAdapter(this, hentaiMap);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

    }
    @Override
    public void onItemClick(View view, int position) {
        String valueFav = adapter.getItem(position);
        Intent intent = new Intent(getBaseContext(), HentaiActivity.class);
        tinyDB.putString("openFavUrlHen", valueFav);
        startActivity(intent);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fav_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.menu_delete) {
            tinyDB.remove("hentaiUrls");
            tinyDB.remove("animeTitles");
            hentaiMap.clear();
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
    }
    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
    }
}