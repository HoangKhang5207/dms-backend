package com.genifast.dms.service.util;

import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfGState;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFHeader;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

@Service
public class WatermarkService {

    public InputStream addWatermark(InputStream pdfInputStream, String watermarkText) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(pdfInputStream);

        // Bổ sung cho phần thêm watermark riêng cho Visitor
        PdfStamper stamper = new PdfStamper(reader, outputStream, '\0', true);

        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        PdfGState gstate = new PdfGState();
        gstate.setFillOpacity(0.3f); // Hiển thị mờ (30%) thay vì ẩn hoàn toàn

        Random random = new Random();

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            Rectangle pageSize = reader.getPageSize(i);
            float x = random.nextFloat() * pageSize.getWidth();
            float y = random.nextFloat() * pageSize.getHeight();

            PdfContentByte over = stamper.getOverContent(i);
            over.saveState();
            over.setGState(gstate);
            over.setColorFill(Color.GRAY); // Màu xám
            over.beginText();
            over.setFontAndSize(bf, 20); // Tăng cỡ chữ lên 20 cho dễ nhìn
            over.showTextAligned(Element.ALIGN_LEFT, watermarkText, x, y, 45); // Xoay 45 độ cho giống watermark
            over.endText();
            over.restoreState();
        }

        stamper.close();
        reader.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public InputStream addWatermarkToImage(InputStream imageInputStream, String fileId, String fileExtension)
            throws IOException {
        BufferedImage image = ImageIO.read(imageInputStream);
        if (image == null) {
            throw new IOException("Could not read image from InputStream.");
        }

        Graphics2D g2d = (Graphics2D) image.getGraphics();

        // Set watermark properties
        g2d.setColor(new Color(0, 0, 0, 50)); // Black with 50% opacity
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(fileId);
        int textHeight = fontMetrics.getHeight();

        // Save the original transform
        java.awt.geom.AffineTransform originalTransform = g2d.getTransform();

        // Translate to the center of the image
        g2d.translate(image.getWidth() / 2, image.getHeight() / 2);

        // Rotate by 45 degrees
        g2d.rotate(Math.toRadians(45));

        // Draw watermark once in the center (relative to the translated and rotated
        // context)
        g2d.drawString(fileId, -textWidth / 2, textHeight / 2);

        // Restore the original transform
        g2d.setTransform(originalTransform);

        g2d.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Use fileExtension to determine the format for writing the image
        String formatName = fileExtension.toLowerCase().replace(".", "");
        if (!ImageIO.write(image, formatName, outputStream)) {
            throw new IOException("Could not write image with format: " + formatName);
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    public InputStream addWatermarkToDocx(InputStream docxInputStream, String fileId) throws IOException {
        try (XWPFDocument document = new XWPFDocument(docxInputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Create a new paragraph at the end of the document
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setAlignment(ParagraphAlignment.CENTER); // Căn giữa
            XWPFRun run = paragraph.createRun();
            run.setText(fileId);
            run.setColor("808080"); // Màu xám
            run.setFontSize(20); // Chữ to
            // run.getCTR().addNewRPr().addNewVanish(); // Bỏ ẩn

            // Add another visible watermark at the beginning
            XWPFParagraph firstParagraph = document.getParagraphs().get(0);
            if (firstParagraph != null) {
                XWPFRun firstRun = firstParagraph.insertNewRun(0);
                firstRun.setText(fileId + " - "); // Thêm dấu gạch nối để phân cách với nội dung chính
                firstRun.setColor("808080");
                firstRun.setFontSize(20);
                // firstRun.getCTR().addNewRPr().addNewVanish(); // Bỏ ẩn
            }

            document.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (Exception e) {
            throw new IOException("Error adding watermark to DOCX: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts all hidden strings (vanished runs) from a DOCX document.
     *
     * @param docxInputStream The InputStream of the DOCX file.
     * @return A list of hidden strings found in the document.
     * @throws IOException if there is an error reading the DOCX file.
     */
    public List<String> extractHiddenStringsFromDocx(InputStream docxInputStream) throws IOException {
        List<String> hiddenStrings = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(docxInputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    if (run.isVanish() && run.getText(0) != null) {
                        hiddenStrings.add(run.getText(0));
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Error extracting hidden strings from DOCX: " + e.getMessage(), e);
        }
        return hiddenStrings;
    }

    /**
     * Extracts potential watermark IDs (UUIDs) from a PDF document by searching its
     * raw content stream.
     * This method looks for patterns that resemble UUIDs in the page content.
     *
     * @param pdfInputStream The InputStream of the PDF file.
     * @return A list of potential UUID watermarks found in the document.
     * @throws IOException if there is an error reading the PDF.
     */
    public List<String> extractPossibleWatermarkIdsFromPdf(InputStream pdfInputStream) throws IOException {
        List<String> foundIds = new ArrayList<>();
        PdfReader reader = new PdfReader(pdfInputStream);
        try {
            // Regex to find UUIDs (e.g., xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx)
            Pattern uuidPattern = Pattern.compile(
                    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}");

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                byte[] pageContent = reader.getPageContent(i);
                String pageText = new String(pageContent, StandardCharsets.UTF_8);
                Matcher matcher = uuidPattern.matcher(pageText);
                while (matcher.find()) {
                    foundIds.add(matcher.group());
                }
            }
        } finally {
            reader.close();
        }
        return foundIds;
    }

    /**
     * Removes a text-based watermark from a PDF file.
     * This method operates by replacing occurrences of the watermark text in the
     * page content stream with an empty string.
     *
     * @param pdfInputStream The InputStream of the watermarked PDF file.
     * @param fileId         The file ID to remove.
     * @return An InputStream containing the PDF with the watermark removed.
     * @throws IOException if an error occurs during PDF processing.
     */
    public InputStream removePdfWatermark(InputStream pdfInputStream, String fileId) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfReader reader = new PdfReader(pdfInputStream);
        PdfStamper stamper = new PdfStamper(reader, outputStream);

        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            byte[] pageContent = reader.getPageContent(i);
            String contentString = new String(pageContent);
            String updatedContent = contentString.replaceAll(fileId, "");
            reader.setPageContent(i, updatedContent.getBytes());
        }

        stamper.close();
        reader.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    /**
     * Removes a hidden (vanished) watermark from a DOCX file.
     * This method iterates through the document, finds text runs that are marked as
     * "vanish"
     * and contain the watermark text, and removes them.
     *
     * @param docxInputStream The InputStream of the watermarked DOCX file.
     * @param fileId          The file ID to remove.
     * @return An InputStream containing the DOCX with the watermark removed.
     * @throws IOException if an error occurs during DOCX processing.
     */
    public InputStream removeDocxWatermark(InputStream docxInputStream, String fileId) throws IOException {
        try (XWPFDocument document = new XWPFDocument(docxInputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                // Iterate backwards to safely remove runs
                for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
                    XWPFRun run = paragraph.getRuns().get(i);
                    if (run.isVanish() && run.getText(0) != null && run.getText(0).contains(fileId)) {
                        paragraph.removeRun(i);
                    }
                }
            }

            document.write(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());

        } catch (Exception e) {
            throw new IOException("Error removing watermark from DOCX: " + e.getMessage(), e);
        }
    }
}
