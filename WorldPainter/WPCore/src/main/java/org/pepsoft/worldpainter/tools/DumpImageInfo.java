/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.tools;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author SchmitzP
 */
public class DumpImageInfo {
    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        File[] files = dir.listFiles();
        for (File file: files) {
            if (file.isFile() && file.canRead()) {
                dumpInfo(file);
            }
        }
    }
    
    private static void dumpInfo(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image != null) {
            System.out.println(file.getPath());
            dumpInfo(image);
            
//            if (image.getType() != BufferedImage.TYPE_CUSTOM) {
//                BufferedImage scaledImage = new BufferedImage(image.getWidth() * 2, image.getHeight() * 2, image.getType());
//                Graphics2D g2 = scaledImage.createGraphics();
//                try {
//                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//                    g2.drawImage(image, 0, 0, image.getWidth() * 2, image.getHeight() * 2, null);
//                } finally {
//                    g2.dispose();
//                }
//                System.out.println(file.getPath() + " (scaled)");
//                dumpInfo(scaledImage);
//            }
            
//            boolean sixteenBit = image.getSampleModel().getSampleSize(0) == 16;
//            int targetImageType = sixteenBit ? BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_BYTE_GRAY;
//            BufferedImage transformedImage = new BufferedImage(image.getWidth() * 2, image.getHeight() * 2, targetImageType);
//            Graphics2D g2 = transformedImage.createGraphics();
//            try {
//                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//                g2.drawImage(image, 0, 0, image.getWidth() * 2, image.getHeight() * 2, null);
//            } finally {
//                g2.dispose();
//            }
//            System.out.println(file.getPath() + " (transformed)");
//            dumpInfo(transformedImage);
            
            System.out.println();
        }
    }

    private static void dumpInfo(BufferedImage image) throws IOException {
        System.out.print("  Image type: ");
        switch (image.getType()) {
            case BufferedImage.TYPE_3BYTE_BGR:
                System.out.println("3BYTE_BGR");
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
                System.out.println("4BYTE_ABGR");
                break;
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                System.out.println("4BYTE_ABGR_PRE");
                break;
            case BufferedImage.TYPE_BYTE_BINARY:
                System.out.println("BYTE_BINARY");
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
                System.out.println("BYTE_GRAY");
                break;
            case BufferedImage.TYPE_BYTE_INDEXED:
                System.out.println("BYTE_INDEXED");
                break;
            case BufferedImage.TYPE_CUSTOM:
                System.out.println("CUSTOM");
                break;
            case BufferedImage.TYPE_INT_ARGB:
                System.out.println("INT_ARGB");
                break;
            case BufferedImage.TYPE_INT_ARGB_PRE:
                System.out.println("INT_ARGB_PRE");
                break;
            case BufferedImage.TYPE_INT_BGR:
                System.out.println("INT_BGR");
                break;
            case BufferedImage.TYPE_INT_RGB:
                System.out.println("INT_RGB");
                break;
            case BufferedImage.TYPE_USHORT_555_RGB:
                System.out.println("USHORT_555_RGB");
                break;
            case BufferedImage.TYPE_USHORT_565_RGB:
                System.out.println("USHORT_565_RGB");
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                System.out.println("USHORT_GRAY");
                break;
            default:
                System.out.println("unknown (" + image.getType() + ")");
        }
        System.out.println("  Width: " + image.getWidth());
        System.out.println("  Height: " + image.getWidth());
        SampleModel sampleModel = image.getSampleModel();
        System.out.print("  Sample model data type: ");
        switch (sampleModel.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                System.out.println("byte");
                break;
            case DataBuffer.TYPE_DOUBLE:
                System.out.println("double");
                break;
            case DataBuffer.TYPE_FLOAT:
                System.out.println("float");
                break;
            case DataBuffer.TYPE_INT:
                System.out.println("int");
                break;
            case DataBuffer.TYPE_SHORT:
                System.out.println("short");
                break;
            case DataBuffer.TYPE_UNDEFINED:
                System.out.println("undefined");
                break;
            case DataBuffer.TYPE_USHORT:
                System.out.println("unsigned short");
                break;
            default:
                System.out.println("unknown (" + sampleModel.getDataType() + ")");
                break;
        }
        System.out.println("  Sample model number of bands: " + sampleModel.getNumBands());
        for (int i = 0; i < sampleModel.getNumBands(); i++) {
            System.out.println("    Band " + i + ": " + sampleModel.getSampleSize(i) + " bits");
        }
        ColorModel colorModel = image.getColorModel();
        if (colorModel instanceof IndexColorModel) {
            System.out.println("  Color model is indexed");
            IndexColorModel indexColorModel = (IndexColorModel) colorModel;
            System.out.println("    Palette size: " + indexColorModel.getMapSize());
            System.out.println("    Palette:");
            for (int i = 0; i < indexColorModel.getMapSize(); i++) {
                System.out.printf("      Index %2d: 0x%8x%n", i, indexColorModel.getRGB(i));
            }
        } else {
            System.out.println("  Color model is not indexed");
        }
        ColorSpace colorSpace = colorModel.getColorSpace();
        System.out.print("  Color space type: ");
        switch (colorSpace.getType()) {
            case ColorSpace.TYPE_2CLR:
            case ColorSpace.TYPE_3CLR:
            case ColorSpace.TYPE_4CLR:
            case ColorSpace.TYPE_5CLR:
            case ColorSpace.TYPE_6CLR:
            case ColorSpace.TYPE_7CLR:
            case ColorSpace.TYPE_8CLR:
            case ColorSpace.TYPE_9CLR:
            case ColorSpace.TYPE_ACLR:
            case ColorSpace.TYPE_BCLR:
            case ColorSpace.TYPE_CCLR:
            case ColorSpace.TYPE_DCLR:
            case ColorSpace.TYPE_ECLR:
            case ColorSpace.TYPE_FCLR:
                System.out.println("generic");
                break;
            case ColorSpace.TYPE_CMY:
                System.out.println("CMY");
                break;
            case ColorSpace.TYPE_CMYK:
                System.out.println("CMYK");
                break;
            case ColorSpace.TYPE_GRAY:
                System.out.println("grayscale");
                break;
            case ColorSpace.TYPE_HLS:
                System.out.println("HLS");
                break;
            case ColorSpace.TYPE_HSV:
                System.out.println("HSV");
                break;
            case ColorSpace.TYPE_Lab:
                System.out.println("Lab");
                break;
            case ColorSpace.TYPE_Luv:
                System.out.println("Luv");
                break;
            case ColorSpace.TYPE_RGB:
                System.out.println("RGB");
                break;
            case ColorSpace.TYPE_XYZ:
                System.out.println("XYZ");
                break;
            case ColorSpace.TYPE_YCbCr:
                System.out.println("YCbCr");
                break;
            case ColorSpace.TYPE_Yxy:
                System.out.println("Yxy");
                break;
            default:
                System.out.println("unknown (" + colorSpace.getType() + ")");
                break;
        }
        System.out.println("  Color space components: " + colorSpace.getNumComponents());
        for (int i = 0; i < colorSpace.getNumComponents(); i++) {
            System.out.println("    Component " + i + ": " + colorSpace.getName(i));
            System.out.println("      Minimum value: " + colorSpace.getMinValue(i));
            System.out.println("      Maximum value: " + colorSpace.getMaxValue(i));
        }
        System.out.println("  Color model components: " + colorModel.getNumComponents());
        for (int i = 0; i < colorModel.getNumComponents(); i++) {
            System.out.println("    Component " + i + ": " + colorModel.getComponentSize(i) + " bits");
        }
        System.out.println("  Color model pixel size: " + colorModel.getPixelSize() + " bits");
        System.out.print("  Color model transparency: ");
        switch (colorModel.getTransparency()) {
            case Transparency.BITMASK:
                System.out.println("bitmask");
                break;
            case Transparency.OPAQUE:
                System.out.println("opaque");
                break;
            case Transparency.TRANSLUCENT:
                System.out.println("translucent");
                break;
            default:
                System.out.println("unknown (" + colorModel.getTransparency() + ")");
                break;
        }
        System.out.println("  Color model has alpha: " + colorModel.hasAlpha());
        if (colorModel.hasAlpha()) {
            System.out.println("    Premultiplied: " + colorModel.isAlphaPremultiplied());
        }

        System.out.println("[ R][ G][ B][ A][smpl]");
        for (int x = 0; x < 16; x++) {
            int rgba = image.getRGB(x, 0);
            System.out.printf("[%2x][%2x][%2x][%2x][%4x]%n", rgba & 0x000000ff, (rgba & 0x0000ff00) >> 8, (rgba & 0x00ff0000) >> 16, (rgba & 0xff000000) >>> 24, image.getRaster().getSample(x, 0, 0));
        }
    }
}