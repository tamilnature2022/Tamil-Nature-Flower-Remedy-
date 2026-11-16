
package com.tamilnature.shanmugavel;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.graphics.Color;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Status Bar and Navigation Bar
        Window window = getWindow();

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        );

        window.setStatusBarColor(Color.rgb(36, 75, 56));
        window.setNavigationBarColor(Color.rgb(24, 60, 44));

        // White Status Bar icons
        View decorView = window.getDecorView();

        int flags = decorView.getSystemUiVisibility();

        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        flags &= ~View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        decorView.setSystemUiVisibility(flags);

        // WebView
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
