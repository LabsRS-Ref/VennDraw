package info.informationsea.venn.graphics;

import info.informationsea.venn.VennFigure;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * venndraw
 * Copyright (C) 2016 OKAMURA Yasunobu
 * Created on 2016/02/27.
 */
public class VennDrawPDFTest {

    public static final File DIST_DIR = new File(new File("build"), "test-pdf");

    @BeforeClass
    public static void setup() throws Exception {
        //noinspection ResultOfMethodCallIgnored
        DIST_DIR.mkdirs();
    }

    @Test
    public void testDraw() throws Exception {
        VennFigure<String> vennFigure = new VennFigure<>();
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(0, 0), 0, 100, 100));
        vennFigure.addShape(new VennFigure.Text<>(new VennFigure.Point(0, 0), "Normal"));
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(50, 50), 0, 50, 100));
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(200, 200), Math.PI / 4, 50, 100));
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(150, 0), 0, 50, 20, "#00ff00ff"));
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(150, 10), 0, 50, 20, "#ff0000ff"));
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(150, 20), 0, 50, 20, "#0000ffff"));
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(150, 30), 0, 50, 20, "#00ff0050"));
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(150, 40), 0, 50, 20, "#00ff0020"));
        vennFigure.addShape(new VennFigure.Oval<>(new VennFigure.Point(150, 100), 0, 50, 20, "#00ff00"));
        vennFigure.addShape(new VennFigure.Text<>(new VennFigure.Point(200, 200), "Rotated"));
        vennFigure.addShape(new VennFigure.Text<>(new VennFigure.Point(100, 100), "Center"));
        vennFigure.addShape(new VennFigure.Text<>(new VennFigure.Point(100, 120), "Left", VennFigure.TextJust.LEFT));
        vennFigure.addShape(new VennFigure.Text<>(new VennFigure.Point(100, 140), "Right", VennFigure.TextJust.RIGHT));

        try (PDDocument doc = new PDDocument()) {
            VennDrawPDF.draw(vennFigure, doc);
            doc.save(new File(DIST_DIR, "test.pdf"));
        }
    }
}