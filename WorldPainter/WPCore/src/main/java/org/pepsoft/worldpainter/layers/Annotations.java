/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

import org.pepsoft.minecraft.Material;
import org.pepsoft.worldpainter.ColourScheme;

import static org.pepsoft.minecraft.Constants.COLOUR_NAMES;

/**
 * Annotation colours:
 *
 * <table>
 * <tr><th>Value</th><th>Colour</th>    <th>Data value</th></th><th>Value</th><th>Colour</th>    <th>Data value</th></tr>
 * <tr><td>0</td>    <td>-</td>         <td>-</td>         </td><td>8</td>    <td>Light Grey</td><td> 8</td>        </tr>
 * <tr><td>1</td>    <td>White</td>     <td>0</td>         </td><td>9</td>    <td>Cyan</td>      <td> 9</td>        </tr>
 * <tr><td>2</td>    <td>Orange</td>    <td>1</td>         </td><td>10</td>   <td>Purple</td>    <td>10</td>        </tr>
 * <tr><td>3</td>    <td>Magenta</td>   <td>2</td>         </td><td>11</td>   <td>Blue</td>      <td>11</td>        </tr>
 * <tr><td>4</td>    <td>Light Blue</td><td>3</td>         </td><td>12</td>   <td>Brown</td>     <td>12</td>        </tr>
 * <tr><td>5</td>    <td>Yellow</td>    <td>4</td>         </td><td>13</td>   <td>Green</td>     <td>13</td>        </tr>
 * <tr><td>6</td>    <td>Lime</td>      <td>5</td>         </td><td>14</td>   <td>Red</td>       <td>14</td>        </tr>
 * <tr><td>7</td>    <td>Pink</td>      <td>6</td>         </td><td>15</td>   <td>Black</td>     <td>15</td>        </tr>
 * </table>
 *
 * <strong>PLEASE NOTE:</strong> because 0 is needed as the default value to represent "layer not set", not all wool
 * colours/data values are represented! Specifically, data value 7 ("grey") is missing and cannot be placed as a colour
 * with the Annotations layer.
 *
 * <p>This also means that the layer values do not map 1:1 to indexes into the {@link Material#WOOLS} array.
 *
 * @author SchmitzP
 */
public class Annotations extends Layer {
    private Annotations() {
        super("org.pepsoft.Annotations", "Annotations", "Coloured annotations on the world, which can optionally be exported with it", DataSize.NIBBLE, true, 65);
    }

    /**
     * Convenience method to get the appropriate colour for a particular layer value, as a packed RGB {@code int}.
     */
    public static int getColour(int layerValue, ColourScheme colourScheme) {
        return colourScheme.getColour(Material.WOOLS[layerValue - ((layerValue < 8) ? 1 : 0)]);
    }

    /**
     * Convenience method to get the appropriate colour name for a particular layer value.
     */
    public static String getColourName(int layerValue) {
        return COLOUR_NAMES[layerValue - ((layerValue < 8) ? 1 : 0)];
    }
    
    public static final Annotations INSTANCE = new Annotations();
    
    private static final long serialVersionUID = 1L;
}