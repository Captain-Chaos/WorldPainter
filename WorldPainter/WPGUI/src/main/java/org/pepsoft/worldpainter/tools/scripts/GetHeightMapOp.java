/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.BitmapHeightMap;

/**
 *
 * @author SchmitzP
 */
public class GetHeightMapOp extends AbstractOperation<HeightMap> {
    public GetHeightMapOp fromFile(String fileName) throws ScriptException {
        if (fileName == null) {
            throw new ScriptException("fileName may not be null");
        }
        this.fileName = fileName;
        return this;
    }

    public GetHeightMapOp selectRedChannel() {
        channel = 0;
        return this;
    }

    public GetHeightMapOp selectGreenChannel() {
        channel = 1;
        return this;
    }

    public GetHeightMapOp selectBlueChannel() {
        channel = 2;
        return this;
    }

    @Override
    public HeightMap go() throws ScriptException {
        File file = sanityCheckFileName(fileName);
        try {
            BufferedImage image = ImageIO.read(file);
            boolean greyscale = (image.getType() == BufferedImage.TYPE_BYTE_BINARY) || (image.getType() == BufferedImage.TYPE_BYTE_GRAY);
            if (channel == -1) {
                return new BitmapHeightMap(file.getName(), image);
            } else {
                if (greyscale) {
                    throw new ScriptException("Colour channel selected for grey scale image");
                }
                return new BitmapHeightMap(file.getName(), image, channel);
            }
        } catch (IOException e) {
            throw new ScriptException("I/O error while loading image " + fileName, e);
        }
    }
    
    private String fileName;
    private int channel = -1;
}