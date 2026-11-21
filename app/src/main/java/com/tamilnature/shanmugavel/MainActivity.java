
private void createPdf(
        WebView source,
        String fileName
) throws Exception {

    PdfDocument document = new PdfDocument();

    try {
        final int pageWidth = 595;
        final int pageHeight = 842;
        final int margin = 28;

        final int printableWidth = pageWidth - (margin * 2);
        final int printableHeight = pageHeight - (margin * 2);

        int webViewWidth = source.getWidth();

        if (webViewWidth <= 0) {
            throw new IOException("Report width is invalid");
        }

        float webViewScale = source.getScale();

        int fullContentHeight = (int) Math.ceil(
                source.getContentHeight() * webViewScale
        );

        if (fullContentHeight <= 0) {
            throw new IOException("Report content is empty");
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(
                webViewWidth,
                View.MeasureSpec.EXACTLY
        );

        int heightSpec = View.MeasureSpec.makeMeasureSpec(
                fullContentHeight,
                View.MeasureSpec.EXACTLY
        );

        source.measure(widthSpec, heightSpec);

        source.layout(
                0,
                0,
                webViewWidth,
                fullContentHeight
        );

        float pdfScale = (float) printableWidth / webViewWidth;

        float pageContentHeight = printableHeight / pdfScale;

        int totalPages = (int) Math.ceil(
                fullContentHeight / pageContentHeight
        );

        totalPages = Math.max(1, totalPages);

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

            canvas.translate(margin, margin);

            canvas.clipRect(
                    0,
                    0,
                    printableWidth,
                    printableHeight
            );

            canvas.scale(pdfScale, pdfScale);

            canvas.translate(
                    0,
                    -i * pageContentHeight
            );

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

        sharePdfFile(outputFile);

    } finally {
        document.close();
    }
}
