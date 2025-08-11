package org.jxls.command;

public class ImageDimensionReader {
    
    // Record to hold image dimensions
    public record Dimension(int x, int y) {}
    
    /**
     * Extracts image dimensions from a byte array containing PNG or JPG data
     * @param imageBytes byte array containing the image data
     * @return Dimension record with width (x) and height (y), or null if unable to read
     */
    public static Dimension getImageDimensions(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length < 8) {
            return null;
        }
        
        try {
            // Check if it's a PNG
            if (isPng(imageBytes)) {
                return getPngDimensions(imageBytes);
            }
            // Check if it's a JPEG
            else if (isJpeg(imageBytes)) {
                return getJpegDimensions(imageBytes);
            }
        } catch (Exception e) {
            System.err.println("Error reading image dimensions: " + e.getMessage());
        }
        
        return null;
    }
    
    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 8 &&
               bytes[0] == (byte) 0x89 &&
               bytes[1] == (byte) 0x50 &&
               bytes[2] == (byte) 0x4E &&
               bytes[3] == (byte) 0x47 &&
               bytes[4] == (byte) 0x0D &&
               bytes[5] == (byte) 0x0A &&
               bytes[6] == (byte) 0x1A &&
               bytes[7] == (byte) 0x0A;
    }
    
    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 2 &&
               bytes[0] == (byte) 0xFF &&
               bytes[1] == (byte) 0xD8;
    }
    
    private static Dimension getPngDimensions(byte[] bytes) {
        // PNG IHDR chunk starts at byte 16
        // Width is at bytes 16-19, height at bytes 20-23
        if (bytes.length < 24) return null;
        
        int width = readInt32BE(bytes, 16);
        int height = readInt32BE(bytes, 20);
        
        return new Dimension(width, height);
    }
    
    private static Dimension getJpegDimensions(byte[] bytes) {
        int pos = 2; // Skip initial 0xFFD8
        
        while (pos < bytes.length - 1) {
            // Find next marker
            if (bytes[pos] != (byte) 0xFF) {
                pos++;
                continue;
            }
            
            byte marker = bytes[pos + 1];
            pos += 2;
            
            // SOF (Start of Frame) markers contain image dimensions
            if ((marker >= (byte) 0xC0 && marker <= (byte) 0xC3) ||
                (marker >= (byte) 0xC5 && marker <= (byte) 0xC7) ||
                (marker >= (byte) 0xC9 && marker <= (byte) 0xCB) ||
                (marker >= (byte) 0xCD && marker <= (byte) 0xCF)) {
                
                if (pos + 5 >= bytes.length) return null;
                
                // Skip segment length (2 bytes) and precision (1 byte)
                pos += 3;
                
                int height = readInt16BE(bytes, pos);
                int width = readInt16BE(bytes, pos + 2);
                
                return new Dimension(width, height);
            }
            
            // Skip other segments
            if (pos + 1 >= bytes.length) break;
            int segmentLength = readInt16BE(bytes, pos);
            pos += segmentLength;
        }
        
        return null;
    }
    
    private static int readInt32BE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
               ((bytes[offset + 1] & 0xFF) << 16) |
               ((bytes[offset + 2] & 0xFF) << 8) |
               (bytes[offset + 3] & 0xFF);
    }
    
    private static int readInt16BE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) |
               (bytes[offset + 1] & 0xFF);
    }
    
    // Example usage
    public static void main(String[] args) {
        // Example with a hypothetical byte array
        // byte[] imageData = ...; // your image byte array
        // Dimension dim = getImageDimensions(imageData);
        // 
        // if (dim != null) {
        //     System.out.println("Image dimensions: " + dim.x() + " x " + dim.y());
        // } else {
        //     System.out.println("Could not determine image dimensions");
        // }
    }
}