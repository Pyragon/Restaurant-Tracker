package com.smittys.utils;

import com.smittys.Tracker;
import com.smittys.db.impl.LabourConnection;
import com.smittys.entities.Employee;
import com.smittys.entities.ScheduleTime;
import lombok.Cleanup;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;

public class BuzzSheetCreator {

    public static void main(String[] args) {
        Tracker tracker = new Tracker();
        tracker.startConnections();

        Properties prop = new Properties();
        prop.put("date", new java.util.Date());
        prop.put("sandwich", "Roast beef sandwich");
        prop.put("soup", "Minestrone");
        prop.put("veg", "Carrot, Broccoli, Cauliflower");

        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

        try {
            ArrayList<ScheduleTime> times = Utilities.getTimes(new Date(format.parse("06/30/2019").getTime()), 0, true);
            prop.put("times", times);
        } catch (ParseException e) {
            e.printStackTrace();
            prop.put("times", new ArrayList<ScheduleTime>());
        }
        createBuzzSheet(prop);
    }

    private static int[] COLUMN_WIDTHS = { 74, 39, 45, 43, 43, 58, 81, 43, 58, 81, 43 };

    private static int[] MERGED_CELLS = {0, 0, 0, 4,
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

    private static int[] BOLD_CELLS = { 0, 0,
            1, 0,
            1, 5,
            9, 5,
            10, 0,
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

    private static int[] CENTERED_CELLS = {
            2, 0,
            2, 3,
            3, 0,
            3, 3,
            4, 0,
            4, 3,
            5, 0,
            5, 3,
            6, 0,
            6, 3,
            7, 0,
            7, 3,
            8, 0,
            8, 3,
            9, 0,
            9, 3,
            0, 5,
            10, 5,
            10, 10,
            11, 0,
            13, 0,
            14, 0
    };

    private static int[] SMALL_TEXT_CELLS = {
            2, 1,
            3, 1,
            4, 1,
            5, 1,
            6, 1,
            7, 1,
            8, 1,
            9, 1,
            11, 1,
            13, 1,
            14, 1,
            32, 0,
            16, 0,
            16, 2,
            17, 0,
            17, 2,
            18, 0,
            18, 2,
            19, 0,
            19, 2,
            20, 0,
            20, 2,
            21, 0,
            21, 2,
            22, 0,
            22, 2,
            23, 0,
            23, 2,
            24, 0,
            24, 2,
            25, 0,
            25, 2,
            26, 0,
            26, 2,
            27, 0,
            27, 2,
            28, 0,
            28, 2,
            29, 0,
            29, 2,
            30, 0,
            30, 2,
    };

    private static String[] TEMPERATURES = {
            "Sour Cream Fridge", "34-40",
            "Salad Fridge", "34-40",
            "Milk Fridge", "34-40",
            "Soup", "160",
            "Ice Cream Freezer", "0-10",
            "Line Freezer", "0-10",
            "Hollandaise", "160",
            "Gravy/Au Jus", "160",
            "Pancake Grill", "375",
            "Fryer", "350",
            "Walk-in Cooler", "34-40",
            "Walk-in Freezer", "0-10",
            "Sandwich Fridge", "34-40",
            "Dishwasher - Wash", "100-120",
            "Dishwasher - Rinse", "100-120"
    };

    private static String[] AM_START_CHECKLIST = {
            "Fill out buzz sheet",
            "Turn on equipment",
            "Heat gravies/au jus",
            "Heat soup",
            "Cook bacon/sausage",
            "Cook shredded",
            "Freezer pull",
            "Temp list"
    };

    private static String[] AM_END_CHECKLIST = {
            "Stock all product",
            "Wipe down stainless",
            "Replace poaching pot",
            "Clean 2nd grill (Sat/Sun)",
            "Take out full garbages",
            "Empty grease traps if full"
    };

    private static String[] PM_START_CHECKLIST = {
            "Temp list",
            "Bake potatoes",
            "Flip sandwich cooler",
            "Prep items needed"
    };

    private static String[] PM_END_CHECKLIST = {
            "Stock all product",
            "Wipe down stainless",
            "All food put away",
            "Replace waffle tray",
            "Clean grill",
            "Wrap/lid all open food",
            "Turn off grill/hood vents",
            "Take out garbages",
            "Lock back door",
            "Check dish area",
            "Wipe down prep area",
            "Turn off oven/fryer",
            "Turn off soup well"
    };

    private static int[] BOTTOM_BORDER_RANGE = {
            0, 0, 0, 10,
            1, 1, 0, 10,
            8, 8, 5, 10,
            9, 9, 0, 10,
            10, 10, 0, 4,
            14, 14, 0, 4,
            15, 15, 0, 10,
            16, 16, 5, 10,
            17, 17, 5, 10,
            26, 26, 5, 10,
            27, 27, 5, 10,
            30, 30, 0, 4,
            33, 33, 0, 4
    };

    private static CellStyle small;

    private static void createBuzzSheet(Properties props) {
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Buzz Sheet");

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
        boldFont.setFontHeightInPoints((short) 11);
        boldFont.setFontName("Calibri");
        boldFont.setColor(IndexedColors.BLACK.getIndex());
        boldFont.setBold(true);
        boldFont.setItalic(false);

        Font smallFont = workbook.createFont();
        smallFont.setFontHeightInPoints((short) 9);
        smallFont.setFontName("Calibri");
        smallFont.setColor(IndexedColors.BLACK.getIndex());
        smallFont.setBold(false);
        smallFont.setItalic(false);

        CellStyle bold = workbook.createCellStyle();
        bold.setAlignment(HorizontalAlignment.CENTER);
        bold.setVerticalAlignment(VerticalAlignment.CENTER);
        bold.setFont(boldFont);

        CellStyle centered = workbook.createCellStyle();
        centered.setAlignment(HorizontalAlignment.CENTER);
        centered.setVerticalAlignment(VerticalAlignment.CENTER);

        small = workbook.createCellStyle();
        small.setFont(smallFont);
        small.setAlignment(HorizontalAlignment.CENTER);
        small.setVerticalAlignment(VerticalAlignment.CENTER);

//        Set bold cells
        index = 0;
        while(index < BOLD_CELLS.length) {
            int rowNum = BOLD_CELLS[index++];
            int colNum = BOLD_CELLS[index++];
            getCell(rowNum, colNum, sheet).setCellStyle(bold);
        }

        index = 0;
        while(index < CENTERED_CELLS.length) {
            int rowNum = CENTERED_CELLS[index++];
            int colNum = CENTERED_CELLS[index++];
            getCell(rowNum, colNum, sheet).setCellStyle(centered);
        }

        index = 0;
        while(index < SMALL_TEXT_CELLS.length) {
            int rowNum = SMALL_TEXT_CELLS[index++];
            int colNum = SMALL_TEXT_CELLS[index++];
            getCell(rowNum, colNum, sheet).setCellStyle(small);
        }

        getCell(0,0, sheet).setCellValue("Date:");
        getCell(1, 0, sheet).setCellValue("On Shift");
        getCell(1, 5, sheet).setCellValue("Comments/Notes/Test Strip");
        getCell(9, 5, sheet).setCellValue("Daily Cleaning Checklist");
        getCell(10, 0, sheet).setCellValue("Food of the Day");
        getCell(11, 0, sheet).setCellValue("Sandwich:");
        getCell(13, 0, sheet).setCellValue("Soup:");
        getCell(14, 0, sheet).setCellValue("Veggies:");
        getCell(15, 0, sheet).setCellValue("Temps - in F°");
        getCell(15, 3, sheet).setCellValue("AM");
        getCell(15, 4, sheet).setCellValue("PM");
        getCell(31, 0, sheet).setCellValue("Waste Items");
        getCell(32, 0, sheet).setCellValue("All items must be weighed or by case count!");
        getCell(33, 0, sheet).setCellValue("Item");
        getCell(33, 2, sheet).setCellValue("Quantity");
        getCell(33, 4, sheet).setCellValue("Sign");
        getCell(10, 5, sheet).setCellValue("Item");
        getCell(10, 10, sheet).setCellValue("Sign");
        getCell(16, 5, sheet).setCellValue("AM Checklist");
        getCell(16, 8, sheet).setCellValue("PM Checklist");
        getCell(17, 5, sheet).setCellValue("Start of Shift");
        getCell(17, 8, sheet).setCellValue("Start of Shift");
        getCell(27, 5, sheet).setCellValue("End of Shift");
        getCell(27, 8, sheet).setCellValue("End of Shift");

        SimpleDateFormat format = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
        getCell(0, 5, sheet).setCellValue(format.format((java.util.Date) props.get("date")));

        getCell(11, 1, sheet).setCellValue(props.getProperty("sandwich"));
        getCell(13, 1, sheet).setCellValue(props.getProperty("soup"));
        getCell(14, 1, sheet).setCellValue(props.getProperty("veg"));

        ArrayList<ScheduleTime> times = (ArrayList<ScheduleTime>) props.get("times");
        for(int i = 0; i < times.size(); i++) {
            ScheduleTime time = times.get(i);
            Object[] data = LabourConnection.connection().handleRequest("get-employee", time.getEmployeeId());
            if(data == null) continue;
            Employee employee = (Employee) data
                    [0];
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm");
            String formatted = timeFormat.format(time.getStartTime());
            formatted += "-";
            if(time.isClose())
                formatted += "CL";
            else
                formatted += timeFormat.format(time.getEndTime());
            getCell(2+i, 0, sheet).setCellValue(employee.getFirstName());
            getCell(2+i, 1, sheet).setCellValue(formatted);
            getCell(2+i, 3, sheet).setCellValue(RoleNames.getName(employee.getDefaultRole()));
        }

        index = 0;
        int row = 16;
        while(index < TEMPERATURES.length) {
            String location = TEMPERATURES[index++];
            String temp = TEMPERATURES[index++];
            getCell(row, 0, sheet).setCellValue(location);
            getCell(row++, 2, sheet).setCellValue(temp);
        }

        writeInfo(18, 5, AM_START_CHECKLIST, sheet);
        writeInfo(18, 8, PM_START_CHECKLIST, sheet);
        writeInfo(28, 5, AM_END_CHECKLIST, sheet);
        writeInfo(28, 8, PM_END_CHECKLIST, sheet);

        //set border
        setFullBorder(new CellRangeAddress(0, 40, 0, 10), workbook, sheet);

        index = 0;
        while(index < BOTTOM_BORDER_RANGE.length) {
            int startRow = BOTTOM_BORDER_RANGE[index++];
            int endRow = BOTTOM_BORDER_RANGE[index++];
            int startCol = BOTTOM_BORDER_RANGE[index++];
            int endCol = BOTTOM_BORDER_RANGE[index++];
            RegionUtil.setBorderBottom(BorderStyle.MEDIUM, new CellRangeAddress(startRow, endRow, startCol, endCol), sheet);
        }

        RegionUtil.setBorderRight(BorderStyle.MEDIUM, new CellRangeAddress(0, 40, 0, 4), sheet);

        try {
            java.util.Date date = (java.util.Date) props.get("date");
            File file = new File("./data/buzz_sheets/"+format.format(date)+".xlsx");
            if(file.exists()) file.delete();
            file.createNewFile();

            @Cleanup FileOutputStream outputStream = new FileOutputStream(file);

            workbook.write(outputStream);
            workbook.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    private static void writeInfo(int startRow, int column, String[] info, Sheet sheet) {
        for(int i = 0; i < info.length; i++) {
            getCell(startRow + i, column, sheet).setCellStyle(small);
            getCell(startRow + i, column, sheet).setCellValue(info[i]);
        }
    }

    private static Cell getCell(int r, int c, Sheet sheet) {
        Row row = sheet.getRow(r);
        if(row == null) row = sheet.createRow(r);
        Cell cell = row.getCell(c);
        if(cell == null) return row.createCell(c);
        return cell;
    }

    private static void setFullBorder(CellRangeAddress region, Workbook workbook, Sheet sheet) {
        CellStyle thinStyle = workbook.createCellStyle();
        thinStyle.setBorderBottom(BorderStyle.THIN);
        thinStyle.setBorderTop(BorderStyle.THIN);
        thinStyle.setBorderLeft(BorderStyle.THIN);
        thinStyle.setBorderRight(BorderStyle.THIN);

        for(int r = 0; r < 41; r++) {
            for(int c = 0; c < 11; c++) {
                CellStyle style = getCell(r, c, sheet).getCellStyle();
                if(style == null) {
                    getCell(r, c, sheet).setCellStyle(thinStyle);
                    continue;
                }
                CellUtil.setCellStyleProperty(getCell(r, c, sheet), CellUtil.BORDER_BOTTOM, BorderStyle.THIN);
                CellUtil.setCellStyleProperty(getCell(r, c, sheet), CellUtil.BORDER_TOP, BorderStyle.THIN);
                CellUtil.setCellStyleProperty(getCell(r, c, sheet), CellUtil.BORDER_LEFT, BorderStyle.THIN);
                CellUtil.setCellStyleProperty(getCell(r, c, sheet), CellUtil.BORDER_RIGHT, BorderStyle.THIN);
            }
        }

        BorderStyle medium = BorderStyle.MEDIUM;
        RegionUtil.setBorderBottom(medium, region, sheet);
        RegionUtil.setBorderTop(medium, region, sheet);
        RegionUtil.setBorderLeft(medium, region, sheet);
        RegionUtil.setBorderRight(medium, region, sheet);
    }
}
