
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
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.rgb(36, 75, 56));
        window.setNavigationBarColor(Color.rgb(24, 60, 44));

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

    public class AndroidShareBridge {

        @JavascriptInterface
        public void sharePdf(String base64Pdf, String fileName) {
            try {
                byte[] bytes = Base64.decode(
                    stripDataPrefix(base64Pdf),
                    Base64.DEFAULT
                );

                File file = writePdfFile(bytes, fileName);

                runOnUiThread(() -> sharePdfFile(file));

            } catch (Exception e) {
                showError(e);
            }
        }

        @JavascriptInterface
        public void shareHtmlAsPdf(String base64Html, String fileName) {

            runOnUiThread(() ->
                Toast.makeText(
                    MainActivity.this,
                    "PDF Share Request Received",
                    Toast.LENGTH_LONG
                ).show()
            );

            try {
                byte[] htmlBytes = Base64.decode(
                    stripDataPrefix(base64Html),
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
                showError(e);
            }
        }
    }

    private String stripDataPrefix(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Empty content");
        }

        int comma = value.indexOf(",");
        return comma >= 0 ? value.substring(comma + 1) : value;
    }

    private File getReportFolder() throws IOException {
        File folder = new File(getCacheDir(), "shared_reports");

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Cannot create folder");
        }

        return folder;
    }

    private String safePdfName(String name) {
        String safe = (name == null ? "TamilNature_Report.pdf" : name)
            .replaceAll("[^a-zA-Z0-9._-]", "_");

        if (!safe.toLowerCase().endsWith(".pdf")) {
            safe += ".pdf";
        }

        return safe;
    }

    private File writePdfFile(byte[] bytes, String name)
            throws IOException {
        File file = new File(getReportFolder(), safePdfName(name));

        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.flush();
        }

        return file;
    }

    private void renderHtmlToPdfAndShare(String html, String fileName) {

        WebView pdfWebView = new WebView(this);

        WebSettings settings = pdfWebView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);

        pdfWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.post(() ->
                    writeWebViewPdf(view, fileName, pdfWebView)
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

    private void writeWebViewPdf(
        WebView source,
        String fileName,
        WebView temporaryWebView
    ) {
        PdfDocument document = new PdfDocument();

        try {
            int width = getResources().getDisplayMetrics().widthPixels;
            int pageHeight = Math.round(width * 842f / 595f);

            source.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(pageHeight, View.MeasureSpec.EXACTLY)
            );

            source.layout(0, 0, width, pageHeight);

            int contentHeight = (int) (
                source.getContentHeight()
                * getResources().getDisplayMetrics().density
            );

            int pages = Math.max(
                1,
                (int) Math.ceil((double) contentHeight / pageHeight)
            );

            for (int i = 0; i < pages; i++) {

                PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(
                        595, 842, i + 1
                    ).create();

                PdfDocument.Page page = document.startPage(info);

                source.scrollTo(0, i * pageHeight);

                Canvas canvas = page.getCanvas();
                canvas.save();
                canvas.scale(595f / width, 595f / width);
                source.draw(canvas);
                canvas.restore();

                document.finishPage(page);
            }

            File outputFile = new File(
                getReportFolder(),
                safePdfName(fileName)
            );

            try (FileOutputStream output =
                new FileOutputStream(outputFile)) {
                document.writeTo(output);
                output.flush();
            }

            document.close();

            temporaryWebView.post(() -> {
                Toast.makeText(
                    MainActivity.this,
                    "PDF Created Successfully",
                    Toast.LENGTH_SHORT
                ).show();

                sharePdfFile(outputFile);
                temporaryWebView.destroy();
            });

        } catch (Exception e) {
            document.close();

            temporaryWebView.post(() -> {
                showError(e);
                temporaryWebView.destroy();
            });
        }
    }

    private void sharePdfFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                MainActivity.this,
                getPackageName() + ".fileprovider",
                file
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setClipData(
                ClipData.newUri(getContentResolver(), "PDF Report", uri)
            );
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(
                Intent.createChooser(intent, "Share PDF Report")
            );

        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError(Exception e) {
        runOnUiThread(() ->
            Toast.makeText(
                MainActivity.this,
                "PDF Error: " + e.getMessage(),
                Toast.LENGTH_LONG
            ).show()
        );
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
