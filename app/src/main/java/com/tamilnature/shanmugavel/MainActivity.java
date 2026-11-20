
package com.tamilnature.shanmugavel;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
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
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PageRange;

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

    public class AndroidShareBridge {

        @JavascriptInterface
        public void sharePdf(String base64Pdf, String fileName) {
            try {
                String cleanBase64 = stripDataPrefix(base64Pdf);
                byte[] pdfBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                File pdfFile = writePdfFile(pdfBytes, fileName);
                runOnUiThread(() -> sharePdfFile(pdfFile));
            } catch (Exception e) {
                showShareError(e);
            }
        }

        @JavascriptInterface
        public void shareHtmlAsPdf(String base64Html, String fileName) {
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
                showShareError(e);
            }
        }
    }

    private String stripDataPrefix(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Empty content");
        }

        int comma = value.indexOf(',');
        return comma >= 0 ? value.substring(comma + 1) : value;
    }

    private File getReportFolder() throws IOException {
        File folder = new File(getCacheDir(), "shared_reports");

        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create report folder");
        }

        return folder;
    }

    private String safePdfName(String fileName) {
        String safeName = (
            fileName == null ? "TamilNature_Report.pdf" : fileName
        ).replaceAll("[^a-zA-Z0-9._-]", "_");

        if (!safeName.toLowerCase().endsWith(".pdf")) {
            safeName += ".pdf";
        }

        return safeName;
    }

    private File writePdfFile(byte[] bytes, String fileName)
            throws IOException {

        File pdfFile = new File(
            getReportFolder(),
            safePdfName(fileName)
        );

        try (FileOutputStream output = new FileOutputStream(pdfFile)) {
            output.write(bytes);
            output.flush();
        }

        return pdfFile;
    }

    private void renderHtmlToPdfAndShare(
            String html,
            String fileName) {

        final WebView pdfWebView = new WebView(this);

        WebSettings settings = pdfWebView.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDomStorageEnabled(false);

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
            WebView temporaryWebView) {

        try {
            PrintDocumentAdapter adapter =
                source.createPrintDocumentAdapter(
                    safePdfName(fileName)
                );

            PrintAttributes attributes =
                new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(
                        new PrintAttributes.Resolution(
                            "tn_pdf",
                            "TamilNature PDF",
                            300,
                            300
                        )
                    )
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build();

            adapter.onLayout(
                null,
                attributes,
                new CancellationSignal(),

                new PrintDocumentAdapter.LayoutResultCallback() {

                    @Override
                    public void onLayoutFinished(
                            PrintDocumentInfo info,
                            boolean changed) {

                        ParcelFileDescriptor descriptor = null;

                        try {
                            File outputFile = new File(
                                getReportFolder(),
                                safePdfName(fileName)
                            );

                            descriptor = ParcelFileDescriptor.open(
                                outputFile,
                                ParcelFileDescriptor.MODE_CREATE |
                                ParcelFileDescriptor.MODE_TRUNCATE |
                                ParcelFileDescriptor.MODE_WRITE_ONLY
                            );

                            ParcelFileDescriptor finalDescriptor =
                                descriptor;

                            adapter.onWrite(
                                new PageRange[]{PageRange.ALL_PAGES},
                                finalDescriptor,
                                new CancellationSignal(),

                                new PrintDocumentAdapter.WriteResultCallback() {

                                    @Override
                                    public void onWriteFinished(
                                            PageRange[] pages) {

                                        closeQuietly(finalDescriptor);

                                        temporaryWebView.post(() -> {
                                            sharePdfFile(outputFile);
                                            temporaryWebView.destroy();
                                        });
                                    }

                                    @Override
                                    public void onWriteFailed(
                                            CharSequence error) {

                                        closeQuietly(finalDescriptor);

                                        temporaryWebView.post(() -> {
                                            showToast(
                                                "PDF உருவாக்க முடியவில்லை: "
                                                + error
                                            );

                                            temporaryWebView.destroy();
                                        });
                                    }

                                    @Override
                                    public void onWriteCancelled() {
                                        closeQuietly(finalDescriptor);
                                        temporaryWebView.post(
                                            temporaryWebView::destroy
                                        );
                                    }
                                }
                            );

                        } catch (Exception e) {
                            closeQuietly(descriptor);

                            temporaryWebView.post(() -> {
                                showShareError(e);
                                temporaryWebView.destroy();
                            });
                        }
                    }

                    @Override
                    public void onLayoutFailed(CharSequence error) {
                        temporaryWebView.post(() -> {
                            showToast(
                                "PDF layout தோல்வி: " + error
                            );

                            temporaryWebView.destroy();
                        });
                    }
                },
                null
            );

        } catch (Exception e) {
            showShareError(e);
            temporaryWebView.destroy();
        }
    }

    private void closeQuietly(ParcelFileDescriptor descriptor) {
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void sharePdfFile(File pdfFile) {
        try {
            Uri contentUri = FileProvider.getUriForFile(
                MainActivity.this,
                getPackageName() + ".fileprovider",
                pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
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
