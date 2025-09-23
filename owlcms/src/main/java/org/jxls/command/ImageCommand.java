package org.jxls.command;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        int poiPictureType = findPoiPictureTypeByImageType(imageType);
        int pictureIdx = workbook.addPicture(imageBytes, poiPictureType);
        addImage(workbook, areaRef, pictureIdx, scaleX, scaleY);
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

        // Anchor at the area start (top-left) so positioning is relative to the area's first cell
        anchor.setCol1(areaStartCol);
        anchor.setRow1(areaStartRow);
        anchor.setCol2(areaEndCol);
        anchor.setRow2(areaEndRow);
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);

        Picture picture = drawing.createPicture(anchor, imageIdx);

        if (scaleX == null && scaleY == null) {
            // we make the image as big as possible without distortion
            Dimension realDims = ImageDimensionReader.getImageDimensions(picture.getPictureData().getData());

            // Calculate target area dimensions
            Dimension areaDims = calculateAreaDimensions(areaRef, sheet);

            scaleX = computeScaleX(realDims.x(), realDims.y(), areaDims.x(), areaDims.y());
            scaleY = computeScaleY(realDims.x(), realDims.y(), areaDims.x(), areaDims.y());

            logger.warn("scale={},{} image={},{} area={},{}", scaleX, scaleY, realDims.x(), realDims.y(), areaDims.x(), areaDims.y());
        }

        picture.resize(scaleX.doubleValue(), scaleY.doubleValue());

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
        } catch (Exception e) {
            logger.warn("Could not log anchor/size details: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void applyCenteringOffset(ClientAnchor anchor, Dimension realDims, Dimension areaDims, double scaleX, double scaleY) {
        // Calculate what the scaled image size will be
        int scaledImageWidth = (int) Math.round(realDims.x() * scaleX);
        int scaledImageHeight = (int) Math.round(realDims.y() * scaleY);

        // Calculate centering offsets in pixels
        int offsetXPixels = Math.max(0, (areaDims.x() - scaledImageWidth) / 2);
        int offsetYPixels = Math.max(0, (areaDims.y() - scaledImageHeight) / 2);

        // Convert to EMU (1 pixel ≈ 9525 EMU at 96 DPI)
        int offsetXEMU = offsetXPixels * 9525;
        int offsetYEMU = offsetYPixels * 9525;

        try {
            anchor.setDx1(offsetXEMU);
            anchor.setDy1(offsetYEMU);

            logger.warn("Applied centering offset: {}px, {}px ({}EMU, {}EMU)",
                    offsetXPixels, offsetYPixels, offsetXEMU, offsetYEMU);

        } catch (Exception e) {
            logger.warn("Could not apply centering offsets: " + e.getMessage());
        }
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
            logger./**/warn("totalWidth {}", totalWidthPx);
        }

        // Calculate total height in pixels
        double totalHeightPx = 0;
        for (int row = startRow; row < endRow; row++) {
            totalHeightPx += getCellHeightInPixels(sheet, row);
            logger./**/warn("totalHeight {}", totalHeightPx);
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

    private double computeScaleX(int realX, int realY, int rectX, int rectY) {
        double ratioX = (double) rectX / realX;
        double ratioY = (double) rectY / realY;

        if (ratioX <= ratioY) {
            return 1.0; // Width is limiting factor
        } else {
            return ratioY / ratioX; // Height is limiting factor
        }
    }

    /**
     * Computes scaleY to maintain aspect ratio when fitting image in rectangle
     */
    private double computeScaleY(int realX, int realY, int rectX, int rectY) {
        double ratioX = (double) rectX / realX;
        double ratioY = (double) rectY / realY;

        if (rectY <= 0.01) {
            // assume the cell will be high enough, return the same ratio as horizontal
            logger.warn("computeScaleY: rectY is too small, using ratioX");
            return ratioX;
        }
        if (ratioX <= ratioY) {
            return ratioX / ratioY; // Width is limiting factor
        } else {
            return 1.0; // Height is limiting factor
        }
    }
}