package com.minimal.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Text-only, black-background launcher. No icons, no wallpaper, no services,
 * no widgets, no animations, no background work. Receiver is only registered
 * while the screen is visible.
 */
public class MainActivity extends Activity {

    static final class App {
        final String label, pkg;
        final ComponentName cn;
        App(String l, String p, ComponentName c) { label = l; pkg = p; cn = c; }
        @Override public String toString() { return label; }
    }

    private final ArrayList<App> all = new ArrayList<App>();
    private final ArrayList<App> shown = new ArrayList<App>();
    private ArrayAdapter<App> adapter;
    private EditText search;
    private ListView list;
    private boolean registered;

    private final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { reload(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad * 2, pad, 0);

        search = new EditText(this);
        search.setHint("search");
        search.setHintTextColor(0xFF555555);
        search.setTextColor(0xFFCCCCCC);
        search.setSingleLine(true);
        search.setBackgroundColor(Color.BLACK);
        search.setTextSize(18);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable e) { applyFilter(); }
        });
        root.addView(search, new LinearLayout.LayoutParams(-1, -2));

        list = new ListView(this);
        list.setDivider(null);
        list.setCacheColorHint(Color.BLACK);
        list.setFastScrollEnabled(true);
        adapter = new ArrayAdapter<App>(this, android.R.layout.simple_list_item_1, shown) {
            @Override public View getView(int pos, View v, android.view.ViewGroup p) {
                TextView t = (TextView) super.getView(pos, v, p);
                t.setTextColor(0xFFDDDDDD);
                t.setTextSize(20);
                return t;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
                launch(shown.get(pos));
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> a, View v, int pos, long id) {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", shown.get(pos).pkg, null));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
        reload();
    }

    @Override protected void onStart() {
        super.onStart();
        if (!registered) {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_PACKAGE_ADDED);
            f.addAction(Intent.ACTION_PACKAGE_REMOVED);
            f.addDataScheme("package");
            registerReceiver(pkgReceiver, f);
            registered = true;
        }
    }

    @Override protected void onStop() {
        super.onStop();
        if (registered) { unregisterReceiver(pkgReceiver); registered = false; }
    }

    @Override protected void onNewIntent(Intent i) {   // Home pressed while we're already open
        super.onNewIntent(i);
        search.setText("");
        list.setSelection(0);
    }

    @Override public void onBackPressed() { /* stay on home */ }

    private void reload() {
        Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentActivities(q, 0);
        all.clear();
        for (ResolveInfo ri : ris) {
            String pkg = ri.activityInfo.packageName;
            all.add(new App(String.valueOf(ri.loadLabel(pm)).toLowerCase(Locale.getDefault()),
                    pkg, new ComponentName(pkg, ri.activityInfo.name)));
        }
        Collections.sort(all, new Comparator<App>() {
            public int compare(App a, App b) { return a.label.compareTo(b.label); }
        });
        applyFilter();
    }

    private void applyFilter() {
        String q = search.getText().toString().trim().toLowerCase(Locale.getDefault());
        shown.clear();
        for (App a : all) if (q.length() == 0 || a.label.contains(q)) shown.add(a);
        adapter.notifyDataSetChanged();
        if (shown.size() == 1 && q.length() > 0) { /* single match: tap to open */ }
    }

    private void launch(App a) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        i.setComponent(a.cn);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(i); } catch (Exception ignored) {}
    }
}
