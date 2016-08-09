/*
 * WorldPainter, a graphical and interactive map generator for Minecraft.
 * Copyright © 2011-2015  pepsoft.org, The Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.tools.scripts;

import org.pepsoft.worldpainter.HeightMap;
import org.pepsoft.worldpainter.heightMaps.BitmapHeightMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author SchmitzP
 */
public class GetHeightMapOp extends AbstractOperation<HeightMap> {
    protected GetHeightMapOp(ScriptingContext context) {
        super(context);
    }

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
        goCalled();

        File file = sanityCheckFileName(fileName);
        try {
            BufferedImage image = ImageIO.read(file);
            boolean greyscale = (image.getType() == BufferedImage.TYPE_BYTE_BINARY) || (image.getType() == BufferedImage.TYPE_BYTE_GRAY);
            if (channel == -1) {
                return BitmapHeightMap.build().withName(file.getName()).withImage(image).withFile(file).now();
            } else {
                if (greyscale) {
                    throw new ScriptException("Colour channel selected for grey scale image");
                }
                return BitmapHeightMap.build().withName(file.getName()).withImage(image).withChannel(channel).withFile(file).now();
            }
        } catch (IOException e) {
            throw new ScriptException("I/O error while loading image " + fileName, e);
        }
    }
    
    private String fileName;
    private int channel = -1;
}