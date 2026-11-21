
package com.tamilnature.shanmugavel;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.util.Base64;
import android.content.Intent;
import android.net.Uri;
import android.content.ClipData;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    private WebView webView;
    private FrameLayout rootLayout;

    private Handler handler =
        new Handler(Looper.getMainLooper());

    private void msg(String message) {
        handler.post(() ->
            Toast.makeText(
                MainActivity.this,
                message,
                Toast.LENGTH_LONG
            ).show()
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        );

        window.setStatusBarColor(Color.rgb(36, 75, 56));
        window.setNavigationBarColor(Color.rgb(24, 60, 44));

        rootLayout = new FrameLayout(this);
        setContentView(rootLayout);

        webView = new WebView(this);

        rootLayout.addView(
            webView,
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        );

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);

        webView.addJavascriptInterface(
            new AndroidShareBridge(),
            "AndroidShare"
        );

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl(
            "file:///android_asset/index.html"
        );
    }

    // ANDROID JAVASCRIPT BRIDGE

    public class AndroidShareBridge {

        @JavascriptInterface
        public void sharePdf(
            String base64Pdf,
            String fileName
        ) {
            try {
                byte[] bytes = Base64.decode(
                    stripDataPrefix(base64Pdf),
                    Base64.DEFAULT
                );

                File file = writePdfFile(
                    bytes,
                    fileName
                );

                handler.post(() ->
                    sharePdfFile(file)
                );

            } catch (Exception e) {
                showError(e);
            }
        }

        @JavascriptInterface
        public void shareHtmlAsPdf(
            String base64Html,
            String fileName
        ) {
            msg("1. PDF Share Request Received");

            try {
                byte[] htmlBytes = Base64.decode(
                    stripDataPrefix(base64Html),
                    Base64.DEFAULT
                );

                String html = new String(
                    htmlBytes,
                    java.nio.charset.StandardCharsets.UTF_8
                );

                handler.post(() ->
                    renderHtmlToPdfAndShare(
                        html,
                        fileName
                    )
                );

            } catch (Exception e) {
                showError(e);
            }
        }
    }

    // BASE64

    private String stripDataPrefix(String value) {

        if (value == null) {
            throw new IllegalArgumentException(
                "Empty content"
            );
        }

        int comma = value.indexOf(",");

        return comma >= 0
            ? value.substring(comma + 1)
            : value;
    }

    // REPORT FOLDER

    private File getReportFolder() throws IOException {

        File folder = new File(
            getCacheDir(),
            "shared_reports"
        );

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException(
                "Cannot create report folder"
            );
        }

        return folder;
    }

    private String safePdfName(String name) {

        String safe = (
            name == null
                ? "TamilNature_Report.pdf"
                : name
        ).replaceAll(
            "[^a-zA-Z0-9._-]",
            "_"
        );

        if (!safe.toLowerCase().endsWith(".pdf")) {
            safe += ".pdf";
        }

        return safe;
    }

    private File writePdfFile(
        byte[] bytes,
        String name
    ) throws IOException {

        File file = new File(
            getReportFolder(),
            safePdfName(name)
        );

        try (
            FileOutputStream output =
                new FileOutputStream(file)
        ) {
            output.write(bytes);
            output.flush();
        }

        return file;
    }

    // HTML TO PDF

    private void renderHtmlToPdfAndShare(
        String html,
        String fileName
    ) {

        msg("2. PDF HTML Loading...");

        WebView pdfWebView = new WebView(
            MainActivity.this
        );

        WebSettings settings =
            pdfWebView.getSettings();

        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);

        // Attach temporary WebView to the Activity
        FrameLayout.LayoutParams params =
            new FrameLayout.LayoutParams(
                1,
                1
            );

        rootLayout.addView(
            pdfWebView,
            params
        );

        pdfWebView.setAlpha(0.01f);

        pdfWebView.setWebViewClient(
            new WebViewClient() {

                @Override
                public void onPageFinished(
                    WebView view,
                    String url
                ) {

                    msg("3. PDF HTML Loaded");

                    // Use Main Thread Handler
                    handler.postDelayed(() -> {

                        msg("4. PDF Creating...");

                        createPdf(
                            pdfWebView,
                            fileName
                        );

                    }, 1500);
                }

                @Override
                public void onReceivedError(
                    WebView view,
                    int errorCode,
                    String description,
                    String failingUrl
                ) {
                    msg(
                        "HTML Error: " + description
                    );

                    removeTemporaryWebView(
                        pdfWebView
                    );
                }
            }
        );

        pdfWebView.loadDataWithBaseURL(
            "file:///android_asset/",
            html,
            "text/html",
            "UTF-8",
            null
        );
    }

    // CREATE PDF

    private void createPdf(
        WebView source,
        String fileName
    ) {

        PdfDocument document =
            new PdfDocument();

        try {

            int width = 595;
            int pageHeight = 842;

            int densityWidth =
                getResources()
                    .getDisplayMetrics()
                    .widthPixels;

            int contentHeight = (int) (
                source.getContentHeight()
                * getResources()
                    .getDisplayMetrics()
                    .density
            );

            if (contentHeight <= 0) {
                contentHeight = pageHeight;
            }

            int pages = Math.max(
                1,
                (int) Math.ceil(
                    (double) contentHeight /
                    pageHeight
                )
            );

            for (int i = 0; i < pages; i++) {

                PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(
                        width,
                        pageHeight,
                        i + 1
                    ).create();

                PdfDocument.Page page =
                    document.startPage(info);

                Canvas canvas =
                    page.getCanvas();

                canvas.save();

                float scale =
                    (float) width / densityWidth;

                canvas.scale(scale, scale);

                source.scrollTo(
                    0,
                    (int) (
                        i * pageHeight / scale
                    )
                );

                source.draw(canvas);

                canvas.restore();

                document.finishPage(page);
            }

            File outputFile = new File(
                getReportFolder(),
                safePdfName(fileName)
            );

            try (
                FileOutputStream output =
                    new FileOutputStream(outputFile)
            ) {
                document.writeTo(output);
                output.flush();
            }

            document.close();

            msg("5. PDF Created Successfully");

            removeTemporaryWebView(source);

            sharePdfFile(outputFile);

        } catch (Exception e) {

            document.close();

            showError(e);

            removeTemporaryWebView(source);
        }
    }

    // REMOVE TEMPORARY WEBVIEW

    private void removeTemporaryWebView(
        WebView temporaryWebView
    ) {
        handler.post(() -> {

            if (temporaryWebView != null) {

                if (temporaryWebView.getParent()
                    instanceof FrameLayout) {

                    ((FrameLayout)
                        temporaryWebView.getParent()
                    ).removeView(temporaryWebView);
                }

                temporaryWebView.destroy();
            }
        });
    }

    // SHARE PDF

    private void sharePdfFile(File file) {

        try {

            msg("6. Opening PDF Share...");

            Uri uri = FileProvider.getUriForFile(
                MainActivity.this,
                getPackageName() + ".fileprovider",
                file
            );

            Intent intent =
                new Intent(Intent.ACTION_SEND);

            intent.setType("application/pdf");

            intent.putExtra(
                Intent.EXTRA_STREAM,
                uri
            );

            intent.setClipData(
                ClipData.newUri(
                    getContentResolver(),
                    "PDF Report",
                    uri
                )
            );

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                Intent.createChooser(
                    intent,
                    "Share PDF Report"
                )
            );

        } catch (Exception e) {
            showError(e);
        }
    }

    // ERROR

    private void showError(Exception e) {

        msg(
            "PDF Error: " + e.getMessage()
        );
    }

    // BACK BUTTON

    @Override
    public void onBackPressed() {

        if (webView != null &&
            webView.canGoBack()) {

            webView.goBack();

        } else {
            super.onBackPressed();
        }
    }
}
