package se.oort.diplicity.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.oort.diplicity.App;
import se.oort.diplicity.R;
import se.oort.diplicity.RetrofitActivity;
import se.oort.diplicity.Sendable;

public class MapView extends FrameLayout {

    private List<Runnable> onFinished = new ArrayList<>();
    private Sendable<String> onClickedProvince;

    private void inflate() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(inflater.inflate(R.layout.map_view, null));
    }
    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate();
    }

    public void setOnClickedProvince(Sendable<String> l) {
        this.onClickedProvince = l;
    }

    private void addOnFinished(Runnable r) {
        synchronized (this) {
            if (onFinished == null) {
                r.run();
            } else {
                onFinished.add(r);
            }
        }
    }

    public void evaluateJS(final String js) {
        this.addOnFinished(new Runnable() {
            @Override
            public void run() {
                WebView mWebView = (WebView) findViewById(R.id.web_view);
                String toEvaluate = "window.map.addReadyAction(function() { " + js + " });";
                if (Build.VERSION.SDK_INT >= 19) {
                    try {
                        mWebView.evaluateJavascript(toEvaluate, null);
                    } catch (IllegalStateException e) {
                        // Since, evidently, the Build.VERSION.SDK_INT check isn't enough,
                        // according to Firebase crash reports....???!???!
                        if (e.getMessage().contains("This API not supported on Android 4.3 and earlier")) {
                            mWebView.loadUrl("javascript:" + toEvaluate);
                        }
                    }
                } else {
                    mWebView.loadUrl("javascript:" + toEvaluate);
                }
            }
        });
    }

    public void load(RetrofitActivity retrofitActivity, String url) {
        synchronized (this) {
            if (onFinished == null) {
                onFinished = new ArrayList<>();
            }
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(retrofitActivity);

        final WebView webView = (WebView) findViewById(R.id.web_view);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDisplayZoomControls(prefs.getBoolean(RetrofitActivity.ZOOM_BUTTONS_PREF_KEY, false));
        webView.setBackgroundColor(Color.parseColor("#212121"));

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                synchronized (MapView.this) {
                    if (onFinished != null) {
                        for (Runnable r : onFinished) {
                            r.run();
                        }
                    }
                    onFinished = null;
                }
            }
        });
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void provinceClicked(String province) {
                onClickedProvince.send(province);
            }
        }, "Android");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "bearer " + retrofitActivity.getAuthToken());

        Log.d("Diplicity", "Loading game view " + url);
        webView.loadUrl(url, headers);
    }
}
