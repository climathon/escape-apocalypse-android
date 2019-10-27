package de.hackerstolz.climathon;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewController extends WebViewClient {
    private MainActivity mainActivity;

    public WebViewController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        Log.w(MainActivity.TAG, "URL change: " + url);
        if (url.endsWith("/plasticinfo")) {
            mainActivity.spawnScreenSpaceEarth();
            mainActivity.plasticScene();
        }
    }
}