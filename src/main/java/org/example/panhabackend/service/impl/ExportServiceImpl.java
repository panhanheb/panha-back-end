package org.example.panhabackend.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.panhabackend.dto.report.ReportTable;
import org.example.panhabackend.service.ExportService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Excel (Apache POI) and PDF (OpenPDF) rendering of tabular reports.
 */
@Service
public class ExportServiceImpl implements ExportService {

    private static final int MAX_COLUMN_WIDTH = 60 * 256;

    @Override
    public byte[] toExcel(ReportTable table) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            applyBorders(headerStyle);

            CellStyle bodyStyle = workbook.createCellStyle();
            applyBorders(bodyStyle);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(table.title());
            titleCell.setCellStyle(titleStyle);

            for (String line : table.subtitleLines()) {
                sheet.createRow(rowIdx++).createCell(0).setCellValue(line);
            }
            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            List<String> headers = table.headers();
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (List<String> rowValues : table.rows()) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < rowValues.size(); i++) {
                    Cell cell = row.createCell(i);
                    setTypedValue(cell, rowValues.get(i));
                    cell.setCellStyle(bodyStyle);
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > MAX_COLUMN_WIDTH) {
                    sheet.setColumnWidth(i, MAX_COLUMN_WIDTH);
                }
            }
            sheet.createFreezePane(0, rowIdx - table.rows().size());

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate Excel report", e);
        }
    }

    @Override
    public byte[] toPdf(ReportTable table) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(table.headers().size() > 6 ? PageSize.A4.rotate() : PageSize.A4, 30, 30, 30, 30);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            Paragraph title = new Paragraph(table.title(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);

            for (String line : table.subtitleLines()) {
                document.add(new Paragraph(line, subtitleFont));
            }

            PdfPTable pdfTable = new PdfPTable(table.headers().size());
            pdfTable.setWidthPercentage(100);
            pdfTable.setSpacingBefore(12);
            pdfTable.setHeaderRows(1);

            Color headerColor = new Color(31, 56, 100);
            for (String header : table.headers()) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(headerColor);
                cell.setPadding(5);
                pdfTable.addCell(cell);
            }
            boolean shaded = false;
            Color stripe = new Color(240, 243, 248);
            for (List<String> row : table.rows()) {
                for (String value : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", bodyFont));
                    cell.setPadding(4);
                    if (shaded) {
                        cell.setBackgroundColor(stripe);
                    }
                    pdfTable.addCell(cell);
                }
                shaded = !shaded;
            }
            if (table.rows().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No data for the selected filters", bodyFont));
                empty.setColspan(table.headers().size());
                empty.setPadding(6);
                pdfTable.addCell(empty);
            }
            document.add(pdfTable);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF report", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private static void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /** Writes numbers as numeric cells so Excel can sum / chart them. */
    private static void setTypedValue(Cell cell, String value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value.matches("-?\\d{1,15}(\\.\\d+)?")) {
            cell.setCellValue(Double.parseDouble(value));
        } else {
            cell.setCellValue(value);
        }
    }
}
