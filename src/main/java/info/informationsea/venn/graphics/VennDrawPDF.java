/*
    Venn Draw : Draw Venn Diagram
    Copyright (C) 2016 Yasunobu OKAMURA All Rights Reserved

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package info.informationsea.venn.graphics;

import info.informationsea.venn.VennFigure;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

@Slf4j
public class VennDrawPDF {

    private static final int FONT_SIZE = 12;
    private static final int MARGIN = 10;

    public static Rectangle2D stringBoundingBox(PDFont font, String str, int fontSize) {
        try {
            return new Rectangle2D.Double(0, 0, font.getStringWidth(str)/1000*fontSize, fontSize);
        } catch (IOException e) {
            throw new RuntimeException("Cannot calculate font width");
        }
    }

    public static <T> void draw(VennFigure<T> vennFigure, PDDocument doc) throws IOException {
        // based on https://svn.apache.org/viewvc/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/CreatePDFA.java?revision=1703059&view=markup
        PDFont font = PDType0Font.load(doc, VennDrawPDF.class.getResourceAsStream("../fx/mplus-1p-regular.ttf"));

        Rectangle2D drawRect = vennFigure.drawRect(str ->
                stringBoundingBox(font, str, FONT_SIZE)
        );
        PDRectangle pageSize = new PDRectangle((float)drawRect.getWidth() + MARGIN*2, (float)drawRect.getHeight() + MARGIN*2);
        PDPage page = new PDPage(pageSize);
        doc.addPage(page);

        VennDrawPDF.draw(vennFigure, doc, page, font, pageSize);

        // PDF/A1b support
        /*
        // add XMP metadata
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();

        try
        {
            DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
            dc.setTitle("Venn Diagram");
            dc.setSource("VennDraw "+ VersionResolver.getVersion());
            dc.setDescription("Venn Diagram");

            PDFAIdentificationSchema id = xmp.createAndAddPFAIdentificationSchema();
            id.setPart(1);
            id.setConformance("B");

            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(xmp, baos, true);

            PDMetadata metadata = new PDMetadata(doc);
            metadata.importXMPMetadata(baos.toByteArray());
            doc.getDocumentCatalog().setMetadata(metadata);
        } catch(BadFieldValueException|TransformerException  e) {
            throw new RuntimeException(e);
        }

        // sRGB output intent
        InputStream colorProfile = VennDrawPDF.class.getResourceAsStream(
                "sRGB Color Space Profile.icm");
        PDOutputIntent intent = new PDOutputIntent(doc, colorProfile);
        intent.setInfo("sRGB IEC61966-2.1");
        intent.setOutputCondition("sRGB IEC61966-2.1");
        intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
        intent.setRegistryName("http://www.color.org");
        doc.getDocumentCatalog().addOutputIntent(intent);
        */
    }

    public static <T> void draw(VennFigure<T> vennFigure, PDDocument doc, PDPage page, PDFont font, PDRectangle rect) throws IOException {
        PDPageContentStream contents = new PDPageContentStream(doc, page);

        Rectangle2D drawRect = vennFigure.drawRect(str ->
                stringBoundingBox(font, str, FONT_SIZE)
        );
        PointConverter.JoinedConverter pointConverter = new PointConverter.JoinedConverter();
        pointConverter.addLast(new PointConverter.Translate(-drawRect.getMinX() + MARGIN, -drawRect.getMinY() + MARGIN));
        pointConverter.addLast(new PointConverter.Scale(1, -1));
        pointConverter.addLast(new PointConverter.Translate(0, rect.getHeight()));

        // fill first
        for (VennFigure.Shape<T> shape : vennFigure.getShapes()) {
            if (shape instanceof VennFigure.Oval) {
                VennFigure.Oval<T> oval = (VennFigure.Oval<T>) shape;

                Color fillColor = VennDrawGraphics2D.decodeColor(oval.getColor());
                if (fillColor.getAlpha() == 0) continue;

                //COSName graphicsStateName = page.getResources().add(graphicsState);

                List<VennFigure.Point> polygon = oval.toPolygon();

                VennFigure.Point converted = pointConverter.convert(polygon.get(0));
                contents.moveTo((float) converted.getX(), (float) converted.getY());
                polygon.add(polygon.remove(0)); // move to last
                for (VennFigure.Point p : polygon) {
                    converted = pointConverter.convert(p);
                    contents.lineTo((float) converted.getX(), (float) converted.getY());
                }


                if (fillColor.getAlpha() != 255) {
                    PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                    graphicsState.setNonStrokingAlphaConstant(fillColor.getAlpha()/255.f);
                    contents.saveGraphicsState();
                    contents.setGraphicsStateParameters(graphicsState);
                }

                contents.setNonStrokingColor(fillColor);
                contents.fill();

                if (fillColor.getAlpha() != 255) {
                    contents.restoreGraphicsState();
                }

            }
        }

        for (VennFigure.Shape<T> shape : vennFigure.getShapes()) {
            if (shape instanceof VennFigure.Oval) {
                VennFigure.Oval<T> oval = (VennFigure.Oval<T>) shape;
                List<VennFigure.Point> polygon = oval.toPolygon();

                VennFigure.Point converted = pointConverter.convert(polygon.get(0));
                contents.moveTo((float)converted.getX(), (float)converted.getY());
                polygon.add(polygon.remove(0)); // move to last
                for (VennFigure.Point p : polygon) {
                    converted = pointConverter.convert(p);
                    contents.lineTo((float)converted.getX(), (float)converted.getY());
                }

                contents.setNonStrokingColor(Color.BLACK);
                contents.stroke();
            } else if (shape instanceof VennFigure.Text) {
                VennFigure.Text<T> text = (VennFigure.Text<T>) shape;

                Rectangle2D boundingBox = stringBoundingBox(font, text.getText(), FONT_SIZE);
                VennFigure.Point converted = pointConverter.convert(text.getCenter());

                float xOffset;
                switch (text.getJust()) {
                    case CENTER:
                        xOffset = (float)(-boundingBox.getWidth()/2);
                        break;
                    case RIGHT:
                        xOffset = (float)(-boundingBox.getWidth());
                        break;
                    default:
                        xOffset = 0;
                        break;
                }

                contents.beginText();
                contents.setNonStrokingColor(Color.BLACK);
                contents.setFont(font, FONT_SIZE);
                contents.newLineAtOffset((float)(converted.getX()+xOffset), (float)(converted.getY() - boundingBox.getHeight()/2));
                contents.showText(text.getText());
                contents.endText();
            }
        }

        contents.saveGraphicsState();
        contents.close();
    }
}
