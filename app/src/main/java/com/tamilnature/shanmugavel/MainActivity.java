
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
import android.webkit.JavascriptInterface;
import android.graphics.Color;
import android.util.Base64;
import android.content.Intent;
import android.net.Uri;
import android.content.ClipData;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        );

        window.setStatusBarColor(Color.rgb(36, 75, 56));
        window.setNavigationBarColor(Color.rgb(24, 60, 44));

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

        // Android Native Share Bridge
        webView.addJavascriptInterface(
            new AndroidShareBridge(),
            "AndroidShare"
        );

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl("file:///android_asset/index.html");
    }

    // Native PDF Share
    public class AndroidShareBridge {

        @JavascriptInterface
        public void sharePdf(String base64Pdf, String fileName) {

            try {
                String cleanBase64 = base64Pdf;

                if (cleanBase64.contains(",")) {
                    cleanBase64 = cleanBase64.substring(
                        cleanBase64.indexOf(",") + 1
                    );
                }

                byte[] pdfBytes = Base64.decode(
                    cleanBase64,
                    Base64.DEFAULT
                );

                File folder = new File(getCacheDir(), "shared_reports");

                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String safeName = fileName.replaceAll(
                    "[^a-zA-Z0-9._-]", "_"
                );

                if (!safeName.toLowerCase().endsWith(".pdf")) {
                    safeName += ".pdf";
                }

                File pdfFile = new File(folder, safeName);

                FileOutputStream output = new FileOutputStream(pdfFile);
                output.write(pdfBytes);
                output.close();

                Uri contentUri = FileProvider.getUriForFile(
                    MainActivity.this,
                    getPackageName() + ".fileprovider",
                    pdfFile
                );

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("application/pdf");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setClipData(
                    ClipData.newUri(getContentResolver(), "PDF Report", contentUri)
                );
                shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                startActivity(
                    Intent.createChooser(shareIntent, "Share PDF Report")
                );

            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(
                        MainActivity.this,
                        "PDF Share Failed: " + e.getMessage(),
                        Toast.LENGTH_LONG
                    ).show()
                );
            }
        }
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
