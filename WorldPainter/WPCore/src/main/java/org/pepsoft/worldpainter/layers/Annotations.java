/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.layers;

/**
 * Annotation colours:
 *
 * <table>
 * <tr><th>Value</th><th>Colour</th><th>Value</th><th>Colour</th></tr>
 * <tr><td>0</td><td>-</td><td>8</td><td>Light Grey</td></tr>
 * <tr><td>1</td><td>White</td><td>9</td><td>Cyan</td></tr>
 * <tr><td>2</td><td>Orange</td><td>10</td><td>Purple</td></tr>
 * <tr><td>3</td><td>Magenta</td><td>11</td><td>Blue</td></tr>
 * <tr><td>4</td><td>Light Blue</td><td>12</td><td>Brown</td></tr>
 * <tr><td>5</td><td>Yellow</td><td>13</td><td>Green</td></tr>
 * <tr><td>6</td><td>Lime</td><td>14</td><td>Red</td></tr>
 * <tr><td>7</td><td>Pink</td><td>15</td><td>Black</td></tr>
 * </html>
 *
 * @author SchmitzP
 */
public class Annotations extends Layer {
    private Annotations() {
        super("org.pepsoft.Annotations", "Annotations", "Coloured annotations on the world, which can optionally be exported with it", DataSize.NIBBLE, true, 65);
    }
    
    public static final Annotations INSTANCE = new Annotations();
    
    private static final long serialVersionUID = 1L;
}