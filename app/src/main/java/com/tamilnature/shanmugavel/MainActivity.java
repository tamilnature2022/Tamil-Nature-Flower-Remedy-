
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
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.util.Base64;
import android.content.Intent;
import android.net.Uri;
import android.content.ClipData;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

        webView = new WebView(this);
        setContentView(webView);

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

        webView.loadUrl("file:///android_asset/index.html");
    }

    // ANDROID SHARE BRIDGE
    public class AndroidShareBridge {

        @JavascriptInterface
        public void sharePdf(String base64Pdf, String fileName) {

            try {
                String cleanBase64 = stripDataPrefix(base64Pdf);

                byte[] pdfBytes = Base64.decode(
                    cleanBase64,
                    Base64.DEFAULT
                );

                File pdfFile = writePdfFile(pdfBytes, fileName);

                runOnUiThread(() -> sharePdfFile(pdfFile));

            } catch (Exception e) {
                showShareError(e);
            }
        }

        @JavascriptInterface
        public void shareHtmlAsPdf(
            String base64Html,
            String fileName
        ) {

            try {
                String cleanBase64 = stripDataPrefix(base64Html);

                byte[] htmlBytes = Base64.decode(
                    cleanBase64,
                    Base64.DEFAULT
                );

                String html = new String(
                    htmlBytes,
                    java.nio.charset.StandardCharsets.UTF_8
                );

                runOnUiThread(() ->
                    renderHtmlToPdfAndShare(html, fileName)
                );

            } catch (Exception e) {
                showShareError(e);
            }
        }
    }

    private String stripDataPrefix(String value) {

        if (value == null) {
            throw new IllegalArgumentException("Empty content");
        }

        int comma = value.indexOf(',');

        return comma >= 0
            ? value.substring(comma + 1)
            : value;
    }

    private File getReportFolder() throws IOException {

        File folder = new File(
            getCacheDir(),
            "shared_reports"
        );

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create report folder");
        }

        return folder;
    }

    private String safePdfName(String fileName) {

        String safeName = (
            fileName == null
                ? "TamilNature_Report.pdf"
                : fileName
        ).replaceAll("[^a-zA-Z0-9._-]", "_");

        if (!safeName.toLowerCase().endsWith(".pdf")) {
            safeName += ".pdf";
        }

        return safeName;
    }

    private File writePdfFile(
        byte[] bytes,
        String fileName
    ) throws IOException {

        File pdfFile = new File(
            getReportFolder(),
            safePdfName(fileName)
        );

        try (FileOutputStream output =
            new FileOutputStream(pdfFile)) {

            output.write(bytes);
            output.flush();
        }

        return pdfFile;
    }

    // HTML TO PDF
    private void renderHtmlToPdfAndShare(
        String html,
        String fileName
    ) {

        WebView pdfWebView = new WebView(this);

        WebSettings settings = pdfWebView.getSettings();

        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);

        pdfWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(
                WebView view,
                String url
            ) {

                view.post(() ->
                    writeWebViewPdf(
                        view,
                        fileName,
                        pdfWebView
                    )
                );
            }
        });

        pdfWebView.loadDataWithBaseURL(
            "file:///android_asset/",
            html,
            "text/html",
            "UTF-8",
            null
        );
    }

    // PDF CREATION
    private void writeWebViewPdf(
        WebView source,
        String fileName,
        WebView temporaryWebView
    ) {

        PdfDocument pdfDocument = new PdfDocument();

        try {

            int width = getResources()
                .getDisplayMetrics().widthPixels;

            int pageHeight = Math.round(
                width * 842f / 595f
            );

            source.measure(
                View.MeasureSpec.makeMeasureSpec(
                    width,
                    View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(
                    pageHeight,
                    View.MeasureSpec.EXACTLY
                )
            );

            source.layout(
                0,
                0,
                width,
                pageHeight
            );

            int contentHeight = (int) (
                source.getContentHeight()
                * getResources()
                    .getDisplayMetrics().density
            );

            int totalPages = Math.max(
                1,
                (int) Math.ceil(
                    (double) contentHeight / pageHeight
                )
            );

            for (int i = 0; i < totalPages; i++) {

                PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(
                        595,
                        842,
                        i + 1
                    ).create();

                PdfDocument.Page page =
                    pdfDocument.startPage(pageInfo);

                source.scrollTo(
                    0,
                    i * pageHeight
                );

                Canvas canvas = page.getCanvas();

                canvas.save();

                canvas.scale(
                    595f / width,
                    595f / width
                );

                source.draw(canvas);

                canvas.restore();

                pdfDocument.finishPage(page);
            }

            File outputFile = new File(
                getReportFolder(),
                safePdfName(fileName)
            );

            try (FileOutputStream output =
                new FileOutputStream(outputFile)) {

                pdfDocument.writeTo(output);
                output.flush();
            }

            pdfDocument.close();

            temporaryWebView.post(() -> {

                sharePdfFile(outputFile);

                temporaryWebView.destroy();
            });

        } catch (Exception e) {

            pdfDocument.close();

            temporaryWebView.post(() -> {

                showShareError(e);

                temporaryWebView.destroy();
            });
        }
    }

    // SHARE PDF
    private void sharePdfFile(File pdfFile) {

        try {

            Uri contentUri = FileProvider.getUriForFile(
                MainActivity.this,
                getPackageName() + ".fileprovider",
                pdfFile
            );

            Intent shareIntent = new Intent(
                Intent.ACTION_SEND
            );

            shareIntent.setType("application/pdf");

            shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                contentUri
            );

            shareIntent.setClipData(
                ClipData.newUri(
                    getContentResolver(),
                    "PDF Report",
                    contentUri
                )
            );

            shareIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share PDF Report"
                )
            );

        } catch (Exception e) {
            showShareError(e);
        }
    }

    private void showShareError(Exception error) {

        runOnUiThread(() ->
            showToast(
                "PDF Share Failed: " + error.getMessage()
            )
        );
    }

    private void showToast(String message) {

        Toast.makeText(
            MainActivity.this,
            message,
            Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
