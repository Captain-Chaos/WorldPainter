package org.jnbt;

/*
 * JNBT License
 * 
 * Copyright (c) 2010 Graham Edgecombe
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *       
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *       
 *     * Neither the name of the JNBT team nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
/**
 * The
 * {@code TAG_Byte_Array} tag.
 *
 * @author Graham Edgecombe
 *
 */
public final class ByteArrayTag extends Tag {
    /**
     * The value.
     */
    private byte[] value;

    /**
     * Creates the tag.
     *
     * @param name The name.
     * @param value The value.
     */
    public ByteArrayTag(String name, byte[] value) {
        super(name);
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder hex = new StringBuilder();
        if (value.length <= 32) {
            for (byte b: value) {
                String hexDigits = Integer.toHexString(b & 0xff).toUpperCase();
                if (hexDigits.length() == 1) {
                    hex.append("0");
                }
                hex.append(hexDigits).append(" ");
            }
        } else {
            for (int i = 0; i < 32; i++) {
                if (i != 30) {
                    String hexDigits = Integer.toHexString((value[(i <= 30) ? i : (value.length - 1)]) & 0xff).toUpperCase();
                    if (hexDigits.length() == 1) {
                        hex.append("0");
                    }
                    hex.append(hexDigits).append(" ");
                } else {
                    hex.append("(");
                    hex.append(value.length - 31);
                    hex.append(" more) ");
                }
            }
        }
        String name = getName();
        String append = "";
        if (name != null && (! name.equals(""))) {
            append = "(\"" + this.getName() + "\")";
        }
        return "TAG_Byte_Array" + append + ": " + ((hex.length() > 0) ? hex.substring(0, hex.length() - 1) : "empty");
    }

    @Override
    public ByteArrayTag clone() {
        ByteArrayTag clone = (ByteArrayTag) super.clone();
        clone.value = value.clone();
        return clone;
    }
    
    private static final long serialVersionUID = 1L;
}