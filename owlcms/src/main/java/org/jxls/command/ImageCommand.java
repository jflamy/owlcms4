package org.jxls.command;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jxls.area.Area;
import org.jxls.command.ImageDimensionReader.Dimension;
import org.jxls.common.AreaRef;
import org.jxls.common.CellRef;
import org.jxls.common.Context;
import org.jxls.common.ImageType;
import org.jxls.common.Size;
import org.jxls.transform.poi.PoiTransformer;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * <p>
 * Implements image rendering
 * </p>
 * <p>
 * Image is specified by providing image bytes and type.
 * </p>
 * 
 * @author Leonid Vysochyn
 */
public class ImageCommand extends AbstractCommand {
    Logger logger = (Logger) LoggerFactory.getLogger(ImageCommand.class);
    public static final String COMMAND_NAME = "image";
    private byte[] imageBytes;
    private ImageType imageType = ImageType.PNG;
    private Area area;
    /** Expression that can be evaluated to image byte array byte[] */
    private String src;
    /**
     * org.apache.poi.ss.usermodel.Picture#resize(double scaleX, double scaleY)
     * <p>
     * Resize the image.
     * <p>
     * Please note, that this method works correctly only for workbooks with the default font size (Arial 10pt for .xls and Calibri 11pt for .xlsx). If the
     * default font is changed the resized image can be streched vertically or horizontally.
     * </p>
     * <p>
     * <code>resize(1.0,1.0)</code> keeps the original size,<br>
     * <code>resize(0.5,0.5)</code> resize to 50% of the original,<br>
     * <code>resize(2.0,2.0)</code> resizes to 200% of the original.<br>
     * <code>resize({@link Double#MAX_VALUE},{@link Double#MAX_VALUE})</code> resizes to the dimension of the embedded image.
     * </p>
     */
    private Double scaleX;
    private Double scaleY;
    private String ptHeight;
    private String border;

    public ImageCommand() {
    }

    /**
     * Creates the command from an image in the context
     * 
     * @param image     name of the context attribute with the image bytes
     * @param imageType type of the image
     */
    public ImageCommand(String image, ImageType imageType) {
        this.src = image;
        this.imageType = imageType;
    }

    /**
     * Creates the command from the image bytes
     * 
     * @param imageBytes the image byte array
     * @param imageType  the type of the image to render (e.g. PNG, JPEG etc)
     */
    public ImageCommand(byte[] imageBytes, ImageType imageType) {
        this.imageBytes = imageBytes;
        this.imageType = imageType;
    }

    /**
     * @return src expression producing image byte array
     */
    public String getSrc() {
        return src;
    }

    /**
     * @param src expression resulting in image byte array
     */
    public void setSrc(String src) {
        this.src = src;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    /**
     * @param strType "PNG", "JPEG" (not "JPG"), ...
     */
    public void setImageType(String strType) {
        imageType = ImageType.valueOf(strType);
    }

    public Double getScaleX() {
        return scaleX;
    }

    public void setScaleX(String scaleX) {
        this.scaleX = Double.valueOf(scaleX);
    }

    public Double getScaleY() {
        return scaleY;
    }

    public void setScaleY(String scaleY) {
        this.scaleY = Double.valueOf(scaleY);
    }

    /**
     * Height in points as a string (e.g. "12.0").
     */
    public String getPtHeight() {
        return ptHeight;
    }

    /**
     * Set the cell/area height in points as a string value.
     * The value is stored as-is; parsing should be handled by callers if needed.
     */
    public void setPtHeight(String ptHeight) {
        this.ptHeight = ptHeight;
    }

    /**
     * If set to a value that parses to true (e.g. "true"), a border will
     * be drawn around the image before adding it to the workbook.
     */
    public String getBorder() {
        return border;
    }

    public void setBorder(String border) {
        this.border = border;
    }

    private boolean needResizePicture() {
        return this.scaleX != null && this.scaleY != null;
    }

    @Override
    public Boolean getLockRange() {
        return needResizePicture() ? Boolean.FALSE : super.getLockRange();
    }

    @Override
    public Command addArea(Area area) {
        if (areaList.size() >= 1) {
            throw new IllegalArgumentException("You can only add 1 area to 'image' command!");
        }
        this.area = area;
        return super.addArea(area);
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public Size applyAt(CellRef cellRef, Context context) {
        if (area == null) {
            throw new IllegalArgumentException("No area is defined for image command");
        }
        Size imageAnchorAreaSize = new Size(area.getSize().getWidth() + 1, area.getSize().getHeight() + 1);
        AreaRef imageAnchorArea = new AreaRef(cellRef, imageAnchorAreaSize);
        byte[] imgBytes = imageBytes;
        if (src != null) {
            Object imgObj = context.evaluate(src);
            if (imgObj == null) {
                return area.getSize();
            }
            if (!(imgObj instanceof byte[])) {
                throw new IllegalArgumentException("src value must contain image bytes (byte[])");
            }
            imgBytes = (byte[]) imgObj;
        }
        addImage(((PoiTransformer) getTransformer()).getWorkbook(), imageAnchorArea, imgBytes, imageType, scaleX, scaleY);
        return area.getSize();
    }

    private void addImage(Workbook workbook, AreaRef areaRef, byte[] imageBytes, ImageType imageType, Double scaleX, Double scaleY) {
        if (imageBytes == null || imageBytes.length == 0) {
            logger.warn("No image bytes to add for area " + areaRef);
            return;
        }
        // If border is requested and parses to true, draw a border into the image bytes
        if (border != null) {
            try {
                if (Boolean.parseBoolean(border)) {
                    // default border: 2px black, keep same format as imageType
                    String fmt = imageType == ImageType.PNG ? "png" : (imageType == ImageType.JPEG ? "jpg" : "png");
                    imageBytes = addBorderToImage(imageBytes, 2, Color.BLACK, fmt);
                }
            } catch (Exception e) {
                logger.warn("Could not apply border to image: {}", e.getMessage());
            }
        }
        int poiPictureType = findPoiPictureTypeByImageType(imageType);
        int pictureIdx = workbook.addPicture(imageBytes, poiPictureType);
        addImage(workbook, areaRef, pictureIdx, scaleX, scaleY);
    }

    private byte[] addBorderToImage(byte[] imageBytes, int borderPx, Color borderColor, String outputFormat) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage src = ImageIO.read(bais);
        if (src == null) {
            throw new IOException("Cannot read image bytes");
        }
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.drawImage(src, 0, 0, null);
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(borderPx));
            int half = Math.max(1, borderPx / 2);
            g.drawRect(half, half, Math.max(0, w - borderPx), Math.max(0, h - borderPx));
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(dst, outputFormat, baos);
        return baos.toByteArray();
    }

    private int findPoiPictureTypeByImageType(ImageType imageType) {
        if (imageType == null) {
            throw new IllegalArgumentException("imageType must not be null");
        }
        switch (imageType) {
            case PNG:
                return Workbook.PICTURE_TYPE_PNG;
            case JPEG:
                return Workbook.PICTURE_TYPE_JPEG;
            case EMF:
                return Workbook.PICTURE_TYPE_EMF;
            case WMF:
                return Workbook.PICTURE_TYPE_WMF;
            case DIB:
                return Workbook.PICTURE_TYPE_DIB;
            case PICT:
                return Workbook.PICTURE_TYPE_PICT;
            default:
                return -1;
        }
    }

    private void addImage(Workbook workbook, AreaRef areaRef, int imageIdx, Double scaleX, Double scaleY) {

        CreationHelper helper = workbook.getCreationHelper();
        Sheet sheet = workbook.getSheet(areaRef.getSheetName());
        if (sheet == null) {
            sheet = workbook.createSheet(areaRef.getSheetName());
        }
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = helper.createClientAnchor();

        int areaStartCol = areaRef.getFirstCellRef().getCol();
        int areaStartRow = areaRef.getFirstCellRef().getRow();
        int areaEndCol = areaRef.getLastCellRef().getCol();
        int areaEndRow = areaRef.getLastCellRef().getRow();

        // DIAGNOSTIC: Log initial area bounds
        logger.warn("DIAGNOSTIC: Initial area bounds - startCol={} startRow={} endCol={} endRow={}", 
                    areaStartCol, areaStartRow, areaEndCol, areaEndRow);
        logger.warn("DIAGNOSTIC: Area range = {}:{}", 
                    toExcelCell(areaStartCol, areaStartRow), 
                    toExcelCell(Math.max(0, areaEndCol - 1), Math.max(0, areaEndRow - 1)));

        // Anchor at the area start (top-left) so positioning is relative to the area's first cell
        anchor.setCol1(areaStartCol);
        anchor.setRow1(areaStartRow);
        anchor.setCol2(areaEndCol);
        anchor.setRow2(areaEndRow);
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);

        // DIAGNOSTIC: Log anchor BEFORE creating picture
        logger.warn("DIAGNOSTIC: Anchor BEFORE picture creation - Col1={} Row1={} Col2={} Row2={} dx1={} dy1={} dx2={} dy2={}",
                    anchor.getCol1(), anchor.getRow1(), anchor.getCol2(), anchor.getRow2(),
                    anchor.getDx1(), anchor.getDy1(), anchor.getDx2(), anchor.getDy2());

        Picture picture = drawing.createPicture(anchor, imageIdx);

        // DIAGNOSTIC: Log anchor AFTER creating picture (but before resize)
        logger.warn("DIAGNOSTIC: Anchor AFTER picture creation - Col1={} Row1={} Col2={} Row2={} dx1={} dy1={} dx2={} dy2={}",
                    anchor.getCol1(), anchor.getRow1(), anchor.getCol2(), anchor.getRow2(),
                    anchor.getDx1(), anchor.getDy1(), anchor.getDx2(), anchor.getDy2());

        if (scaleX == null && scaleY == null) {
            // compute a uniform scale based on the target area's height so the
            // image height becomes equal to the area's height and the width is
            // scaled to preserve aspect ratio (width = aspectRatio * areaHeight).
            Dimension realDims = ImageDimensionReader.getImageDimensions(picture.getPictureData().getData());
            Dimension areaDims = calculateAreaDimensions(areaRef, sheet);
            double uniformScale = computeUniformScale(realDims.x(), realDims.y(), areaDims.x(), areaDims.y());
            scaleX = uniformScale;
            scaleY = uniformScale;
            logger.warn("scale={},{} image={},{} area={},{}", scaleX, scaleY, realDims.x(), realDims.y(), areaDims.x(), areaDims.y());
        }

        // DIAGNOSTIC: Log scale values
        logger.warn("DIAGNOSTIC: About to resize with scaleX={} scaleY={}", scaleX, scaleY);

        // Resize the picture first. Some POI implementations rewrite anchor offsets during resize,
        // so apply centering offsets after resize to ensure `dx1`/`dy1` reflect the offset from the
        // top-left anchor cell (so the image is shifted right/down as expected).
        picture.resize(scaleX.doubleValue(), scaleY.doubleValue());

        // DIAGNOSTIC: Log anchor AFTER resize - THIS IS THE KEY DIAGNOSTIC
        logger.warn("DIAGNOSTIC: Anchor AFTER resize - Col1={} Row1={} Col2={} Row2={} dx1={} dy1={} dx2={} dy2={}",
                    anchor.getCol1(), anchor.getRow1(), anchor.getCol2(), anchor.getRow2(),
                    anchor.getDx1(), anchor.getDy1(), anchor.getDx2(), anchor.getDy2());

        // After resize, POI sets the anchor to define the image bounds.
        // Instead of resetting Col2/Row2 to area end, keep the image anchor as-is
        // and adjust Col1/Row1 + dx1/dy1 to center the image within the area

        // DIAGNOSTIC: Check if the anchor width is now zero/invalid
        if (anchor.getCol2() <= anchor.getCol1()) {
            logger.error("DIAGNOSTIC: PROBLEM! Col2 ({}) <= Col1 ({}) after resize! This will cause zero width!",
                         anchor.getCol2(), anchor.getCol1());
            // Don't reset Col2 - let POI handle it
        }
        if (anchor.getRow2() <= anchor.getRow1()) {
            logger.error("DIAGNOSTIC: PROBLEM! Row2 ({}) <= Row1 ({}) after resize! This will cause zero height!",
                         anchor.getRow2(), anchor.getRow1());
            // Don't reset Row2 - let POI handle it
        }

        // Compute dimensions and center the image anchor within the area
        try {
            logger.warn("DIAGNOSTIC: About to call applyCenteringOffset");
            Dimension realDimsForCenter = ImageDimensionReader.getImageDimensions(picture.getPictureData().getData());
            Dimension areaDimsForCenter = calculateAreaDimensions(areaRef, sheet);
            applyCenteringOffset(anchor, realDimsForCenter, areaDimsForCenter, scaleX.doubleValue(), scaleY.doubleValue(), areaStartCol, areaEndCol,
                    areaStartRow, areaEndRow, sheet);
            logger.warn("DIAGNOSTIC: applyCenteringOffset completed successfully");
        } catch (Exception e) {
            logger.error("DIAGNOSTIC: Exception in applyCenteringOffset: {}", e.getMessage(), e);
            // Ensure we have valid bounds even if centering fails
            if (anchor.getCol2() <= anchor.getCol1()) {
                anchor.setCol2(areaEndCol);
            }
            if (anchor.getRow2() <= anchor.getRow1()) {
                anchor.setRow2(areaEndRow);
            }
        }

        // DIAGNOSTIC: Log final anchor state
        logger.warn("DIAGNOSTIC: FINAL anchor - Col1={} Row1={} Col2={} Row2={} dx1={} dy1={} dx2={} dy2={}",
                    anchor.getCol1(), anchor.getRow1(), anchor.getCol2(), anchor.getRow2(),
                    anchor.getDx1(), anchor.getDy1(), anchor.getDx2(), anchor.getDy2());

        // Log resulting image size after resizing and anchor placement details
        try {
            Dimension realDimsLog = ImageDimensionReader.getImageDimensions(picture.getPictureData().getData());
            int realW = realDimsLog.x();
            int realH = realDimsLog.y();
            int scaledW = (int) Math.round(realW * scaleX.doubleValue());
            int scaledH = (int) Math.round(realH * scaleY.doubleValue());
            // Anchor cell indices
            int aCol1 = anchor.getCol1();
            int aRow1 = anchor.getRow1();
            int aCol2 = anchor.getCol2();
            int aRow2 = anchor.getRow2();
            // Offsets (EMU) and converted to pixels (approx 1 px = 9525 EMU)
            int dx1emu = anchor.getDx1();
            int dy1emu = anchor.getDy1();
            int dx2emu = anchor.getDx2();
            int dy2emu = anchor.getDy2();
            double dx1px = dx1emu / 9525.0;
            double dy1px = dy1emu / 9525.0;
            double dx2px = dx2emu / 9525.0;
            double dy2px = dy2emu / 9525.0;

            // Note: anchor offsets are relative to the top-left corner of the anchor cell.
            String topLeftCell = toExcelCell(aCol1, aRow1);
            // For bottom-right, convert exclusive indices to inclusive Excel cell
            String bottomRightCell = toExcelCell(Math.max(0, aCol2 - 1), Math.max(0, aRow2 - 1));

            logger.warn(
                    "image scaled to {}x{} px (from {}x{}), anchor top-left={} dx1={}px ({}EMU) dy1={}px ({}EMU), anchor bottom-right={} dx2={}px ({}EMU) dy2={}px ({}EMU)",
                    scaledW, scaledH, realW, realH,
                    topLeftCell, String.format("%.2f", dx1px), dx1emu, String.format("%.2f", dy1px), dy1emu,
                    bottomRightCell, String.format("%.2f", dx2px), dx2emu, String.format("%.2f", dy2px), dy2emu);

            // Diagnostic: compute the pixel extent of the anchor (left/right/top/bottom)
            try {
                double anchorWidthPx = computeAnchorWidthPixels(sheet, aCol1, aCol2, dx1px, dx2px);
                double anchorHeightPx = computeAnchorHeightPixels(sheet, aRow1, aRow2, dy1px, dy2px);
                logger.warn("anchor pixel extent width={}px height={}px (scaled image {}x{})", String.format("%.2f", anchorWidthPx),
                        String.format("%.2f", anchorHeightPx), scaledW, scaledH);
            } catch (Exception e) {
                logger.warn("Could not compute anchor pixel extents: {}", e.getMessage());
            }
        } catch (Exception e) {
            logger.warn("Could not log anchor/size details: {}", e.getMessage());
        }
    }

    private double computeAnchorWidthPixels(Sheet sheet, int col1, int col2Exclusive, double dx1px, double dx2px) {
        if (col1 == col2Exclusive - 1) {
            // Anchor spans within a single column
            return dx2px - dx1px;
        } else {
            // Anchor spans multiple columns
            double width = -dx1px; // Start from negative of left offset
            for (int c = col1; c < col2Exclusive; c++) {
                width += getCellWidthInPixels(sheet, c);
            }
            width += dx2px; // Add right offset
            return width;
        }
    }

    private double computeAnchorHeightPixels(Sheet sheet, int row1, int row2Exclusive, double dy1px, double dy2px) {
        if (row1 == row2Exclusive - 1) {
            // Anchor spans within a single row
            return dy2px - dy1px;
        } else {
            // Anchor spans multiple rows
            double height = -dy1px; // Start from negative of top offset
            // Handle ptHeight case like calculateAreaDimensions does
            if (ptHeight != null) {
                try {
                    float pt = Float.parseFloat(ptHeight);
                    double totalHeightPx = pt * 96D / 72D;
                    int numRows = row2Exclusive - row1;
                    if (numRows > 0) {
                        double uniformRowHeight = totalHeightPx / numRows;
                        for (int r = row1; r < row2Exclusive; r++) {
                            height += uniformRowHeight;
                        }
                    }
                } catch (NumberFormatException nfe) {
                    // Fall back to actual row heights
                    for (int r = row1; r < row2Exclusive; r++) {
                        height += getCellHeightInPixels(sheet, r);
                    }
                }
            } else {
                // Use actual row heights
                for (int r = row1; r < row2Exclusive; r++) {
                    height += getCellHeightInPixels(sheet, r);
                }
            }
            height += dy2px; // Add bottom offset
            return height;
        }
    }

    private void applyCenteringOffset(ClientAnchor anchor, Dimension realDims, Dimension areaDims, double scaleX, double scaleY,
            int areaStartCol, int areaEndCol, int areaStartRow, int areaEndRow, Sheet sheet) {

        logger.warn("DEBUG: applyCenteringOffset START - positioning image in area");

        // Calculate what the scaled image size will be
        int scaledImageWidth = (int) Math.round(realDims.x() * scaleX);
        int scaledImageHeight = (int) Math.round(realDims.y() * scaleY);

        // Calculate centering offsets in pixels (areaDims are already pixels)
        int offsetXPixels = Math.max(0, (areaDims.x() - scaledImageWidth) / 2);
        int offsetYPixels = Math.max(0, (areaDims.y() - scaledImageHeight) / 2);

        logger.warn("DEBUG: scaledImage={}x{} area={}x{} offsetX={}px offsetY={}px",
                scaledImageWidth, scaledImageHeight, areaDims.x(), areaDims.y(), offsetXPixels, offsetYPixels);

        // Set anchor to span the full area
        anchor.setCol1(areaStartCol);
        anchor.setRow1(areaStartRow);
        anchor.setCol2(areaEndCol);
        anchor.setRow2(areaEndRow);

        // Position the image within the area by setting offsets
        // Top-left of image relative to area start
        int imageStartXPixels = offsetXPixels;
        int imageStartYPixels = offsetYPixels;

        // Bottom-right of image relative to area start
        int imageEndXPixels = imageStartXPixels + scaledImageWidth;
        int imageEndYPixels = imageStartYPixels + scaledImageHeight;

        // Convert to anchor coordinates (Col1/Row1/dx1/dy1 to Col2/Row2/dx2/dy2)
        // Find which column/row the image start/end positions fall into

        // X position: find column and offset for image start
        int currentCol = areaStartCol;
        int remainingXPixels = imageStartXPixels;
        while (currentCol < areaEndCol && remainingXPixels > 0) {
            float colWidthPx = getCellWidthInPixels(sheet, currentCol);
            if (remainingXPixels >= colWidthPx) {
                remainingXPixels -= colWidthPx;
                currentCol++;
            } else {
                break;
            }
        }
        anchor.setCol1(currentCol);
        anchor.setDx1((int) Math.round(remainingXPixels * 9525.0)); // Convert to EMU

        // X position: find column and offset for image end
        currentCol = areaStartCol;
        remainingXPixels = imageEndXPixels;
        while (currentCol < areaEndCol && remainingXPixels > 0) {
            float colWidthPx = getCellWidthInPixels(sheet, currentCol);
            if (remainingXPixels >= colWidthPx) {
                remainingXPixels -= colWidthPx;
                currentCol++;
            } else {
                break;
            }
        }
        anchor.setCol2(currentCol);
        anchor.setDx2((int) Math.round(remainingXPixels * 9525.0)); // Convert to EMU

        // Y position: find row and offset for image start
        int currentRow = areaStartRow;
        int remainingYPixels = imageStartYPixels;
        int numRows = areaEndRow - areaStartRow;

        if (ptHeight != null) {
            // Use uniform row heights
            try {
                float pt = Float.parseFloat(ptHeight);
                double totalHeightPx = pt * 96D / 72D;
                double uniformRowHeight = totalHeightPx / numRows;

                while (currentRow < areaEndRow && remainingYPixels > 0) {
                    if (remainingYPixels >= uniformRowHeight) {
                        remainingYPixels -= uniformRowHeight;
                        currentRow++;
                    } else {
                        break;
                    }
                }
                anchor.setRow1(currentRow);
                anchor.setDy1((int) Math.round(remainingYPixels * 9525.0));
            } catch (NumberFormatException nfe) {
                // Fall back to actual row heights
                while (currentRow < areaEndRow && remainingYPixels > 0) {
                    float rowHeightPx = getCellHeightInPixels(sheet, currentRow);
                    if (remainingYPixels >= rowHeightPx) {
                        remainingYPixels -= rowHeightPx;
                        currentRow++;
                    } else {
                        break;
                    }
                }
                anchor.setRow1(currentRow);
                anchor.setDy1((int) Math.round(remainingYPixels * 9525.0));
            }
        } else {
            // Use actual row heights
            while (currentRow < areaEndRow && remainingYPixels > 0) {
                float rowHeightPx = getCellHeightInPixels(sheet, currentRow);
                if (remainingYPixels >= rowHeightPx) {
                    remainingYPixels -= rowHeightPx;
                    currentRow++;
                } else {
                    break;
                }
            }
            anchor.setRow1(currentRow);
            anchor.setDy1((int) Math.round(remainingYPixels * 9525.0));
        }

        // Y position: find row and offset for image end
        currentRow = areaStartRow;
        remainingYPixels = imageEndYPixels;

        if (ptHeight != null) {
            // Use uniform row heights
            try {
                float pt = Float.parseFloat(ptHeight);
                double totalHeightPx = pt * 96D / 72D;
                double uniformRowHeight = totalHeightPx / numRows;

                while (currentRow < areaEndRow && remainingYPixels > 0) {
                    if (remainingYPixels >= uniformRowHeight) {
                        remainingYPixels -= uniformRowHeight;
                        currentRow++;
                    } else {
                        break;
                    }
                }
                anchor.setRow2(currentRow);
                anchor.setDy2((int) Math.round(remainingYPixels * 9525.0));
            } catch (NumberFormatException nfe) {
                // Fall back to actual row heights
                while (currentRow < areaEndRow && remainingYPixels > 0) {
                    float rowHeightPx = getCellHeightInPixels(sheet, currentRow);
                    if (remainingYPixels >= rowHeightPx) {
                        remainingYPixels -= rowHeightPx;
                        currentRow++;
                    } else {
                        break;
                    }
                }
                anchor.setRow2(currentRow);
                anchor.setDy2((int) Math.round(remainingYPixels * 9525.0));
            }
        } else {
            // Use actual row heights
            while (currentRow < areaEndRow && remainingYPixels > 0) {
                float rowHeightPx = getCellHeightInPixels(sheet, currentRow);
                if (remainingYPixels >= rowHeightPx) {
                    remainingYPixels -= rowHeightPx;
                    currentRow++;
                } else {
                    break;
                }
            }
            anchor.setRow2(currentRow);
            anchor.setDy2((int) Math.round(remainingYPixels * 9525.0));
        }

        logger.warn("Applied centering: image positioned at ({}, {}) to ({}, {}) in area",
                anchor.getCol1(), anchor.getDx1(), anchor.getCol2(), anchor.getDx2());

        logger.warn("DEBUG: applyCenteringOffset END");
    }

    /**
     * Calculate the pixel dimensions of the target area (AreaRef)
     */
    private Dimension calculateAreaDimensions(AreaRef areaRef, Sheet sheet) {
        int startCol = areaRef.getFirstCellRef().getCol();
        int endCol = areaRef.getLastCellRef().getCol();
        int startRow = areaRef.getFirstCellRef().getRow();
        int endRow = areaRef.getLastCellRef().getRow();

        // Note: AreaRef uses an exclusive end index (one past the inclusive cell).
        // Log the range in Excel style (e.g. E10:G12) and include internal indices for debugging.
        int inclusiveEndRow = Math.max(0, endRow - 1);
        int inclusiveEndCol = Math.max(0, endCol - 1);
        String excelRange = toExcelCell(startCol, startRow) + ":" + toExcelCell(inclusiveEndCol, inclusiveEndRow);
        logger./**/warn("area {} (internal start={},{} end(exclusive)={},{})", excelRange, startRow, startCol, endRow, endCol);
        // Calculate total width in pixels
        double totalWidthPx = 0;
        for (int col = startCol; col < endCol; col++) {
            totalWidthPx += getCellWidthInPixels(sheet, col);
            logger.warn("totalWidth {} px", totalWidthPx);
        }

        // Calculate total height in pixels
        double totalHeightPx = 0;
        boolean computeRows = true;
        if (ptHeight != null) {
            // we are forcing the height by adding manually the row heights reported by Excel
            try {
                float pt = Float.parseFloat(ptHeight);
                // Convert points to pixels (px = points * 96 / 72)
                totalHeightPx = pt * 96D / 72D;
                logger.warn("ptHeight override: {} pt -> {} px", pt, String.format("%.2f", totalHeightPx));
                computeRows = false;
            } catch (NumberFormatException nfe) {
               // ignore and log
               logger.warn("Could not parse ptHeight value '{}', will compute height from rows", ptHeight);
            }
        } 
        if (computeRows) {
            for (int row = startRow; row < endRow; row++) {
                totalHeightPx += getCellHeightInPixels(sheet, row);
                logger.warn("{} totalHeight {}", row, totalHeightPx);
            }
        }
        return new Dimension((int) Math.round(totalWidthPx), (int) Math.round(totalHeightPx));
    }

    /**
     * Gets the height of a cell in points by sheet and row number
     * 
     * @param sheet     The sheet containing the row
     * @param rowNumber The row number (0-based)
     * @return Height in points
     */
    public float getCellHeightInPoints(Sheet sheet, int rowNumber) {
        Row row = sheet.getRow(rowNumber);

        if (row == null || row.getZeroHeight()) {
            return 0f;
        } else if (row.getHeight() == -1) {
            // Use default row height when not explicitly set
            return sheet.getDefaultRowHeightInPoints();
        } else {
            return row.getHeightInPoints();
        }
    }

    /**
     * Gets the width of a cell in points by sheet and column number
     * 
     * @param sheet        The sheet containing the column
     * @param columnNumber The column number (0-based)
     * @return Width in points
     */
    public float getCellWidthInPoints(Sheet sheet, int columnNumber) {
        // Convert pixels to points (assuming 96 DPI)
        float widthInPixels = sheet.getColumnWidthInPixels(columnNumber);
        return widthInPixels * 72f / 96f;
    }

    /**
     * Gets the width of a cell in pixels by sheet and column number
     */
    public float getCellWidthInPixels(Sheet sheet, int columnNumber) {
        return sheet.getColumnWidthInPixels(columnNumber);
    }

    /**
     * Gets the height of a cell in pixels by sheet and row number
     */
    public float getCellHeightInPixels(Sheet sheet, int rowNumber) {
        float heightPoints = getCellHeightInPoints(sheet, rowNumber);
        // Convert points to pixels (assuming 96 DPI): px = points * 96 / 72
        return heightPoints * 96f / 72f;
    }

    // Convert 0-based column and row to Excel cell name, e.g., (4,9) -> E10
    private String toExcelCell(int col0, int row0) {
        return colToName(col0) + Integer.toString(row0 + 1);
    }

    // Convert 0-based column to Excel column letters (0 -> A, 25 -> Z, 26 -> AA)
    private String colToName(int col0) {
        int col = col0;
        StringBuilder sb = new StringBuilder();
        while (col >= 0) {
            int rem = col % 26;
            sb.append((char) ('A' + rem));
            col = (col / 26) - 1;
        }
        return sb.reverse().toString();
    }

    /**
     * Reads all the data from the input stream and returns the bytes read.
     * 
     * @param inputStream -
     * @return byte array
     * @throws IOException -
     */
    public byte[] toByteArray(InputStream inputStream) throws IOException { // used by templates and SimpleExporter
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(inputStream, baos);
        return baos.toByteArray();
    }

    public void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        for (int count; (count = in.read(buffer)) != -1;) {
            out.write(buffer, 0, count);
        }
    }

    /**
     * Computes a uniform scale factor that ensures the image fits within the target area while preserving aspect ratio. The scale factor is determined by the
     * most restrictive dimension (width or height) to prevent the image from exceeding area boundaries.
     * 
     * For portrait images: scales based on height constraint, unless width is more restrictive For landscape images: scales based on width constraint, unless
     * height is more restrictive
     * 
     * @param realX Original image width in pixels
     * @param realY Original image height in pixels
     * @param rectX Target area width in pixels
     * @param rectY Target area height in pixels
     * @return Scale factor (1.0 = original size, 0.5 = half size, etc.)
     */
    private double computeUniformScale(int realX, int realY, int rectX, int rectY) {
        if (realY <= 0 || realX <= 0) {
            return 1.0;
        }
        double ratioX = (double) rectX / realX;
        double ratioY = (double) rectY / realY;
        // Use the smaller ratio to ensure the image fits within the area bounds
        return Math.min(ratioX, ratioY);
    }
}