package com.portfolio.financetracker.service;

import com.portfolio.financetracker.model.Transaction;
import com.portfolio.financetracker.model.TransactionType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] MONTH_NAMES = {
            "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
            "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
    };

    public byte[] generateExcel(List<Transaction> transactions) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // raggruppa per anno
            Map<Integer, List<Transaction>> byYear = new TreeMap<>();
            for (Transaction t : transactions) {
                int year = t.getDate().getYear();
                byYear.computeIfAbsent(year, k -> new ArrayList<>()).add(t);
            }

            // una scheda per ogni anno
            for (Map.Entry<Integer, List<Transaction>> entry : byYear.entrySet()) {
                int year = entry.getKey();
                List<Transaction> yearTxns = entry.getValue();
                Sheet sheet = workbook.createSheet("Anno " + year);
                createYearSheet(workbook, sheet, yearTxns, year);
            }

            // scheda riepilogo totale
            Sheet riepilogoSheet = workbook.createSheet("Riepilogo Totale");
            createRiepilogoSheet(workbook, riepilogoSheet, byYear, transactions.size());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Errore durante la generazione del file Excel", e);
        }
    }

    private void createYearSheet(XSSFWorkbook workbook, Sheet sheet, List<Transaction> transactions, int year) {
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);

        // raggruppa per mese
        Map<YearMonth, List<Transaction>> byMonth = new TreeMap<>();
        for (Transaction t : transactions) {
            YearMonth ym = YearMonth.from(t.getDate());
            byMonth.computeIfAbsent(ym, k -> new ArrayList<>()).add(t);
        }

        BigDecimal yearIncome = BigDecimal.ZERO;
        BigDecimal yearExpense = BigDecimal.ZERO;
        int rowNum = 0;

        for (Map.Entry<YearMonth, List<Transaction>> entry : byMonth.entrySet()) {
            YearMonth ym = entry.getKey();
            List<Transaction> monthTxns = entry.getValue();

            // header mese
            Row monthHeader = sheet.createRow(rowNum++);
            Cell monthCell = monthHeader.createCell(0);
            monthCell.setCellValue(MONTH_NAMES[ym.getMonthValue() - 1]);
            monthCell.setCellStyle(headerStyle);

            // intestazioni tabella
            String[] headers = {"Data", "Descrizione", "Categoria", "Metodo Pagamento", "Tipo", "Importo"};
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }

            BigDecimal monthIncome = BigDecimal.ZERO;
            BigDecimal monthExpense = BigDecimal.ZERO;

            for (Transaction t : monthTxns) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.getDate().format(DATE_FORMATTER));
                row.createCell(1).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                row.createCell(2).setCellValue(t.getCategory() != null ? t.getCategory().getName() : "");
                row.createCell(3).setCellValue(t.getPaymentMethod() != null ? t.getPaymentMethod().getName() : "");
                row.createCell(4).setCellValue(t.getType() == TransactionType.INCOME ? "Entrata" : "Uscita");
                row.createCell(5).setCellValue(t.getAmount().doubleValue());

                if (t.getType() == TransactionType.INCOME) {
                    monthIncome = monthIncome.add(t.getAmount());
                } else {
                    monthExpense = monthExpense.add(t.getAmount());
                }
            }

            // subtotale mese
            Row subRow = sheet.createRow(rowNum++);
            Cell subLabel = subRow.createCell(4);
            subLabel.setCellValue("Subtotale " + MONTH_NAMES[ym.getMonthValue() - 1]);
            subLabel.setCellStyle(boldStyle);

            Row subIncRow = sheet.createRow(rowNum++);
            subIncRow.createCell(4).setCellValue("  Entrate");
            subIncRow.createCell(5).setCellValue(monthIncome.doubleValue());

            Row subExpRow = sheet.createRow(rowNum++);
            subExpRow.createCell(4).setCellValue("  Uscite");
            subExpRow.createCell(5).setCellValue(monthExpense.doubleValue());

            Row subBalRow = sheet.createRow(rowNum++);
            subBalRow.createCell(4).setCellValue("  Saldo Mese");
            subBalRow.createCell(5).setCellValue(monthIncome.subtract(monthExpense).doubleValue());

            rowNum++; // riga vuota

            yearIncome = yearIncome.add(monthIncome);
            yearExpense = yearExpense.add(monthExpense);
        }

        // riepilogo anno
        rowNum++;
        Row yearHeader = sheet.createRow(rowNum++);
        Cell yearCell = yearHeader.createCell(0);
        yearCell.setCellValue("RIEPILOGO ANNO " + year);
        yearCell.setCellStyle(headerStyle);

        Row r1 = sheet.createRow(rowNum++);
        r1.createCell(0).setCellValue("Totale Entrate");
        r1.createCell(1).setCellValue(yearIncome.doubleValue());

        Row r2 = sheet.createRow(rowNum++);
        r2.createCell(0).setCellValue("Totale Uscite");
        r2.createCell(1).setCellValue(yearExpense.doubleValue());

        Row r3 = sheet.createRow(rowNum++);
        r3.createCell(0).setCellValue("Saldo Netto");
        r3.createCell(1).setCellValue(yearIncome.subtract(yearExpense).doubleValue());

        Row r4 = sheet.createRow(rowNum);
        r4.createCell(0).setCellValue("Numero Transazioni");
        r4.createCell(1).setCellValue(transactions.size());

        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRiepilogoSheet(XSSFWorkbook workbook, Sheet sheet,
                                       Map<Integer, List<Transaction>> byYear, int totalCount) {
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);

        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RIEPILOGO TOTALE");
        titleCell.setCellStyle(headerStyle);

        rowNum++;

        String[] yearHeaders = {"Anno", "Entrate", "Uscite", "Saldo", "Transazioni"};
        Row headerRow = sheet.createRow(rowNum++);
        for (int i = 0; i < yearHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(yearHeaders[i]);
            cell.setCellStyle(boldStyle);
        }

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Map.Entry<Integer, List<Transaction>> entry : byYear.entrySet()) {
            int year = entry.getKey();
            List<Transaction> txns = entry.getValue();

            BigDecimal yearIncome = BigDecimal.ZERO;
            BigDecimal yearExpense = BigDecimal.ZERO;

            for (Transaction t : txns) {
                if (t.getType() == TransactionType.INCOME) {
                    yearIncome = yearIncome.add(t.getAmount());
                } else {
                    yearExpense = yearExpense.add(t.getAmount());
                }
            }

            totalIncome = totalIncome.add(yearIncome);
            totalExpense = totalExpense.add(yearExpense);

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(year);
            row.createCell(1).setCellValue(yearIncome.doubleValue());
            row.createCell(2).setCellValue(yearExpense.doubleValue());
            row.createCell(3).setCellValue(yearIncome.subtract(yearExpense).doubleValue());
            row.createCell(4).setCellValue(txns.size());
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum++);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTALE COMPLESSIVO");
        totalLabel.setCellStyle(boldStyle);

        totalRow.createCell(1).setCellValue(totalIncome.doubleValue());
        totalRow.createCell(2).setCellValue(totalExpense.doubleValue());
        BigDecimal overallBalance = totalIncome.subtract(totalExpense);
        totalRow.createCell(3).setCellValue(overallBalance.doubleValue());
        totalRow.createCell(4).setCellValue(totalCount);

        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
