
package com.tamilnature.shanmugavel;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private WebView webView;
    private FrameLayout rootLayout;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

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
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.addJavascriptInterface(
                new AndroidShareBridge(),
                "AndroidShare"
        );

        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl("file:///android_asset/index.html");
    }

    // ------------------------------------
    // ANDROID JAVASCRIPT BRIDGE
    // ------------------------------------

    private class AndroidShareBridge {

        @JavascriptInterface
        public void sharePdf(String base64Pdf, String fileName) {

            try {
                String cleanBase64 =
                        stripDataPrefix(base64Pdf);

                byte[] pdfBytes =
                        Base64.decode(
                                cleanBase64,
                                Base64.DEFAULT
                        );

                File outputFile =
                        writePdfFile(pdfBytes, fileName);

                handler.post(() ->
                        sharePdfFile(outputFile)
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

            try {
                String cleanBase64 =
                        stripDataPrefix(base64Html);

                byte[] htmlBytes =
                        Base64.decode(
                                cleanBase64,
                                Base64.DEFAULT
                        );

                String html = new String(
                        htmlBytes,
                        StandardCharsets.UTF_8
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

    // ------------------------------------
    // REMOVE BASE64 PREFIX
    // ------------------------------------

    private String stripDataPrefix(String value) {

        if (value == null) {
            return "";
        }

        int commaIndex = value.indexOf(",");

        if (value.startsWith("data:")
                && commaIndex >= 0) {

            return value.substring(commaIndex + 1);
        }

        return value;
    }

    // ------------------------------------
    // PDF FILE LOCATION
    // ------------------------------------

    private File getReportFolder() throws IOException {

        File folder = new File(
                getCacheDir(),
                "shared_reports"
        );

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException(
                    "Cannot create PDF folder"
            );
        }

        return folder;
    }

    private String safePdfName(String fileName) {

        if (fileName == null
                || fileName.trim().isEmpty()) {

            fileName = "TamilNature_Report.pdf";
        }

        fileName = fileName.replaceAll(
                "[^a-zA-Z0-9._-]",
                "_"
        );

        if (!fileName.toLowerCase().endsWith(".pdf")) {
            fileName += ".pdf";
        }

        return fileName;
    }

    private File writePdfFile(
            byte[] bytes,
            String fileName
    ) throws IOException {

        File folder = getReportFolder();

        File outputFile = new File(
                folder,
                safePdfName(fileName)
        );

        try (FileOutputStream outputStream =
                     new FileOutputStream(outputFile)) {

            outputStream.write(bytes);
            outputStream.flush();
        }

        return outputFile;
    }

    // ------------------------------------
    // HTML TO PDF
    // ------------------------------------

    private void renderHtmlToPdfAndShare(
            String html,
            String fileName
    ) {

        WebView pdfWebView = new WebView(this);

        WebSettings settings =
                pdfWebView.getSettings();

        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setDefaultTextEncodingName("UTF-8");

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );

        rootLayout.addView(pdfWebView, params);

        pdfWebView.setAlpha(0.01f);

        pdfWebView.setWebViewClient(
                new WebViewClient() {

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {

                handler.postDelayed(() -> {

                    try {
                        createPdf(
                                pdfWebView,
                                fileName
                        );

                    } catch (Exception e) {

                        showError(e);

                        removeTemporaryWebView(
                                pdfWebView
                        );
                    }

                }, 1500);
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    int errorCode,
                    String description,
                    String failingUrl
            ) {

                showError(
                        new Exception(description)
                );

                removeTemporaryWebView(
                        pdfWebView
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

    // ------------------------------------
    // CREATE PDF DOCUMENT - A4 PAGINATION
    // ------------------------------------

    private void createPdf(
            WebView source,
            String fileName
    ) throws Exception {

        PdfDocument document = new PdfDocument();

        try {

            final int pageWidth = 595;
            final int pageHeight = 842;

            final int margin = 28;

            final int printableWidth =
                    pageWidth - (margin * 2);

            final int printableHeight =
                    pageHeight - (margin * 2);

            int webViewWidth = source.getWidth();

            if (webViewWidth <= 0) {
                throw new IOException(
                        "Report width is invalid"
                );
            }

            float webViewScale = source.getScale();

            float contentHeight =
                    source.getContentHeight()
                            * webViewScale;

            if (contentHeight <= 0) {
                throw new IOException(
                        "Report content is empty"
                );
            }

            // Fit content width to A4 printable width
            float pdfScale =
                    (float) printableWidth
                            / webViewWidth;

            // Content height represented by one PDF page
            float pageContentHeight =
                    printableHeight / pdfScale;

            int totalPages = (int) Math.ceil(
                    contentHeight / pageContentHeight
            );

            if (totalPages < 1) {
                totalPages = 1;
            }

            for (int i = 0; i < totalPages; i++) {

                PdfDocument.PageInfo pageInfo =
                        new PdfDocument.PageInfo.Builder(
                                pageWidth,
                                pageHeight,
                                i + 1
                        ).create();

                PdfDocument.Page page =
                        document.startPage(pageInfo);

                Canvas canvas = page.getCanvas();

                canvas.drawColor(Color.WHITE);

                canvas.save();

                // A4 page margin
                canvas.translate(margin, margin);

                // Scale WebView to PDF width
                canvas.scale(
                        pdfScale,
                        pdfScale
                );

                // Scroll to current page content
                int scrollY = Math.round(
                        i * pageContentHeight
                );

                source.scrollTo(0, scrollY);

                // Clip current page content
                canvas.clipRect(
                        0,
                        0,
                        webViewWidth,
                        pageContentHeight
                );

                // Draw WebView content
                source.draw(canvas);

                canvas.restore();

                document.finishPage(page);
            }

            File folder = getReportFolder();

            File outputFile = new File(
                    folder,
                    safePdfName(fileName)
            );

            try (FileOutputStream outputStream =
                         new FileOutputStream(outputFile)) {

                document.writeTo(outputStream);
                outputStream.flush();
            }

            removeTemporaryWebView(source);

            // Open Android PDF share directly
            sharePdfFile(outputFile);

        } finally {

            document.close();
        }
    }

    // ------------------------------------
    // REMOVE TEMPORARY WEBVIEW
    // ------------------------------------

    private void removeTemporaryWebView(
            WebView view
    ) {

        if (view == null) {
            return;
        }

        handler.post(() -> {

            ViewParentCleanup.remove(view);

        });
    }

    // ------------------------------------
    // OPEN ANDROID PDF SHARE
    // ------------------------------------

    private void sharePdfFile(File file) {

        try {

            if (file == null || !file.exists()) {

                throw new IOException(
                        "PDF file not found"
                );
            }

            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            Intent shareIntent = new Intent(
                    Intent.ACTION_SEND
            );

            shareIntent.setType("application/pdf");

            shareIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    pdfUri
            );

            shareIntent.setClipData(
                    ClipData.newUri(
                            getContentResolver(),
                            "TamilNature PDF",
                            pdfUri
                    )
            );

            shareIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share PDF"
                    )
            );

        } catch (Exception e) {

            showError(e);
        }
    }

    // ------------------------------------
    // ERROR ONLY
    // ------------------------------------

    private void showError(Exception e) {

        String message = "PDF Error";

        if (e != null && e.getMessage() != null) {
            message += ": " + e.getMessage();
        }

        final String finalMessage = message;

        handler.post(() ->
                Toast.makeText(
                        MainActivity.this,
                        finalMessage,
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    // ------------------------------------
    // ANDROID BACK BUTTON
    // ------------------------------------

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    // ------------------------------------
    // TEMPORARY WEBVIEW CLEANUP
    // ------------------------------------

    private static class ViewParentCleanup {

        static void remove(WebView view) {

            if (view == null) {
                return;
            }

            android.view.ViewParent parent =
                    view.getParent();

            if (parent instanceof ViewGroup) {

                ((ViewGroup) parent).removeView(view);
            }

            view.stopLoading();
            view.destroy();
        }
    }
}
