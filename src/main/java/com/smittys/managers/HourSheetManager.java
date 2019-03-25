package com.smittys.managers;

import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

@RequiredArgsConstructor
public class HourSheetManager {

    private final String day;

    public static void main(String[] args) {
        new HourSheetManager("").run();
    }

    public void run() {

//        Ocr.setUp();
//        Ocr ocr = new Ocr();
//        ocr.startEngine("eng", Ocr.SPEED_FASTEST);
//        String result = ocr.recognize(new File[]{new File("C:/Users/Cody/Desktop/hours2.png")}, Ocr.RECOGNIZE_TYPE_ALL, Ocr.OUTPUT_FORMAT_PLAINTEXT);
//        System.out.println("Result: " + result);
//        ocr.stopEngine();

        Tesseract tess = new Tesseract();
        try {
            tess.setDatapath("D:/Tesseract/tessdata/");
            tess.setLanguage("eng");
            String result = tess.doOCR(new File("C:/Users/Cody/Desktop/hours2.png"));
            System.out.println("Result: " + result);
        } catch (TesseractException e) {
            e.printStackTrace();
        }

    }
}
