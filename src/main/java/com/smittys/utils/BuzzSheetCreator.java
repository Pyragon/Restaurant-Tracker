package com.smittys.utils;

import com.smittys.entities.ScheduleTime;
import lombok.Cleanup;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

public class BuzzSheetCreator {

    public static void main(String[] args) {
        createBuzzSheet(null, null);
    }

    public static int[] COLUMN_WIDTHS = { 74, 39, 45, 40, 40, 58, 61, 43, 58, 61, 43 };

    public static int[] MERGED_CELLS = {0, 0, 0, 4,
            0, 0, 5, 10,
            1, 1, 0, 4,
            1, 1, 5, 10,
            2, 8, 5, 10,
            2, 2, 1, 2,
            2, 2, 3, 4,
            3, 3, 1, 2,
            3, 3, 3, 4,
            4, 4, 1, 2,
            4, 4, 3, 4,
            5, 5, 1, 2,
            5, 5, 3, 4,
            6, 6, 1, 2,
            6, 6, 3, 4,
            7, 7, 1, 2,
            7, 7, 3, 4,
            8, 8, 1, 2,
            8, 8, 3, 4,
            9, 9, 1, 2,
            9, 9, 3, 4,
            9, 9, 5, 10,
            10, 10, 0, 4,
            10, 10, 5, 9,
            11, 11, 5, 9,
            12, 12, 5, 9,
            13, 13, 5, 9,
            14, 14, 5, 9,
            15, 15, 5, 9,
            11, 12, 0, 0,
            11, 12, 1, 4,
            13, 13, 1, 4,
            14, 14, 1, 4,
            15, 15, 0, 2,
            16, 16, 0, 1,
            17, 17, 0, 1,
            18, 18, 0, 1,
            19, 19, 0, 1,
            20, 20, 0, 1,
            21, 21, 0, 1,
            22, 22, 0, 1,
            23, 23, 0, 1,
            24, 24, 0, 1,
            25, 25, 0, 1,
            26, 26, 0, 1,
            27, 27, 0, 1,
            28, 28, 0, 1,
            29, 29, 0, 1,
            30, 30, 0, 1,
            31, 31, 0, 4,
            32, 32, 0, 4,
            33, 33, 0, 1,
            33, 33, 2, 3,
            34, 34, 0, 1,
            34, 34, 2, 3,
            35, 35, 0, 1,
            35, 35, 2, 3,
            36, 36, 0, 1,
            36, 36, 2, 3,
            37, 37, 0, 1,
            37, 37, 2, 3,
            38, 38, 0, 1,
            38, 38, 2, 3,
            39, 39, 0, 1,
            39, 39, 2, 3,
            40, 40, 0, 1,
            40, 40, 2, 3,
            16, 16, 5, 7,
            16, 16, 8, 10,
            17, 17, 5, 7,
            17, 17, 8, 10,
            18, 18, 5, 6,
            18, 18, 8, 9,
            19, 19, 5, 6,
            19, 19, 8, 9,
            20, 20, 5, 6,
            20, 20, 8, 9,
            21, 21, 5, 6,
            21, 21, 8, 9,
            22, 22, 5, 6,
            22, 22, 8, 9,
            23, 23, 5, 6,
            23, 23, 8, 9,
            24, 24, 5, 6,
            24, 24, 8, 9,
            25, 25, 5, 6,
            25, 25, 8, 9,
            26, 26, 5, 6,
            26, 26, 8, 9,
            27, 27, 5, 7,
            27, 27, 8, 10,
            28, 28, 5, 6,
            28, 28, 8, 9,
            29, 29, 5, 6,
            29, 29, 8, 9,
            30, 30, 5, 6,
            30, 30, 8, 9,
            31, 31, 5, 6,
            31, 31, 8, 9,
            32, 32, 5, 6,
            32, 32, 8, 9,
            33, 33, 5, 6,
            33, 33, 8, 9,
            34, 34, 5, 6,
            34, 34, 8, 9,
            35, 35, 5, 6,
            35, 35, 8, 9,
            36, 36, 5, 6,
            36, 36, 8, 9,
            37, 37, 5, 6,
            37, 37, 8, 9,
            38, 38, 5, 6,
            38, 38, 8, 9,
            39, 39, 5, 6,
            39, 39, 8, 9,
            40, 40, 5, 6,
            40, 40, 8, 9,
    };

    public static int[] BOLD_CELLS = { 0, 0,
            1, 0,
            1, 5,
            10, 0,
            9, 5,
            15, 0,
            15, 3,
            15, 4,
            16, 5,
            17, 5,
            16, 8,
            17, 8,
            31, 0,
            33, 0,
            33, 2,
            33, 4,
            27, 5,
            27, 8
    };

    public static void createBuzzSheet(Date date, ArrayList<ScheduleTime> times) {
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Buzz Sheet");

        //set border
        setFullBorder(new CellRangeAddress(0, 40, 0, 10), sheet);

        //set column widths
        for(int i = 0; i < COLUMN_WIDTHS.length; i++)
            sheet.setColumnWidth(i, PixelUtils.pixel2WidthUnits(COLUMN_WIDTHS[i]));

        //Merge cells
        int index = 0;
        while(index < MERGED_CELLS.length) {
            int fromRow = MERGED_CELLS[index++];
            int toRow = MERGED_CELLS[index++];
            int fromCol = MERGED_CELLS[index++];
            int toCol = MERGED_CELLS[index++];
            sheet.addMergedRegion(new CellRangeAddress(fromRow, toRow, fromCol, toCol));
        }

        Font boldFont = workbook.createFont();
        boldFont.setFontHeightInPoints((short) 10);
        boldFont.setFontName("Arial");
        boldFont.setColor(IndexedColors.BLACK.getIndex());
        boldFont.setBold(true);
        boldFont.setItalic(false);

        //Set bold cells
        index = 0;
        while(index < BOLD_CELLS.length) {
            int rowNum = BOLD_CELLS[index++];
            int colNum = BOLD_CELLS[index++];
            Row row = sheet.getRow(rowNum);
            if(row.getCell(colNum) == null)
                row.createCell(colNum);
            CellStyle style = sheet.getRow(rowNum).getCell(colNum).getCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setFont(boldFont);
        }

        sheet.getRow(0).getCell(0).setCellValue("Date:");
        sheet.getRow(1).getCell(0).setCellValue("On Shift");
        sheet.getRow(1).getCell(5).setCellValue("Comments/Notes/Test Strip");
        sheet.getRow(9).getCell(5).setCellValue("Daily Cleaning Checklist");
        sheet.getRow(10).getCell(0).setCellValue("Food of the Day");
        sheet.getRow(11).getCell(0).setCellValue("Sandwich:");
        sheet.getRow(13).getCell(0).setCellValue("Soup:");
        sheet.getRow(14).getCell(0).setCellValue("Veggies:");
        sheet.getRow(15).getCell(0).setCellValue("Temps - in F°");
        sheet.getRow(15).getCell(3).setCellValue("AM");
        sheet.getRow(15).getCell(4).setCellValue("PM");
        sheet.getRow(31).getCell(0).setCellValue("Waste Items");
        sheet.getRow(32).getCell(0).setCellValue("All items must be weighed or by case count!");
        sheet.getRow(33).getCell(0).setCellValue("Item");
        sheet.getRow(33).getCell(2).setCellValue("Quantity");
        sheet.getRow(33).getCell(4).setCellValue("Sign");
//        sheet.getRow(10).getCell(5).setCellValue("Item");
        sheet.getRow(10).createCell(5).setCellValue("Item");
        sheet.getRow(10).getCell(10).setCellValue("Sign");
        sheet.getRow(16).getCell(5).setCellValue("AM Checklist");
        sheet.getRow(16).getCell(8).setCellValue("PM Checklist");
        sheet.getRow(17).getCell(5).setCellValue("Start of Shift");
        sheet.getRow(17).getCell(8).setCellValue("Start of Shift");
        sheet.getRow(27).getCell(5).setCellValue("End of Shift");
        sheet.getRow(27).getCell(8).setCellValue("End of Shift");


        sheet.getRow(9).getCell(5).getCellStyle().setAlignment(HorizontalAlignment.CENTER);

        try {
            File file = new File("./data/buzz_sheets/temp.xlsx");
            if(file.exists()) file.delete();
            file.createNewFile();

            @Cleanup FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void setFullBorder(CellRangeAddress region, Sheet sheet) {
        BorderStyle style = BorderStyle.MEDIUM;
        RegionUtil.setBorderBottom(style, region, sheet);
        RegionUtil.setBorderTop(style, region, sheet);
        RegionUtil.setBorderLeft(style, region, sheet);
        RegionUtil.setBorderRight(style, region, sheet);
    }
}
