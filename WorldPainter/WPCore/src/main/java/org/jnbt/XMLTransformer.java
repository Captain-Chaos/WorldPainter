package org.jnbt;

import org.pepsoft.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static org.pepsoft.util.XMLUtils.forEachElement;

public class XMLTransformer {
    public static Tag fromXML(Reader in) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(in));
            return fromElement(document.getDocumentElement());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    public static void toXML(Tag tag, Writer out) throws IOException {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document = builder.newDocument();
            document.appendChild(toElement(document, tag));
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(out));
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unchecked") // Responsibility of caller
    private static Tag fromElement(Element element) throws IOException {
        switch (element.getTagName()) {
            case "bytes":
                return new ByteArrayTag(element.getAttribute("name"), decodeBytes(element.getTextContent()));
            case "byte":
                return new ByteTag(element.getAttribute("name"), Byte.valueOf(element.getTextContent()));
            case "compound":
                Map<String, Tag> map = new HashMap<>();
                forEachElement(element.getChildNodes(), child -> {
                    Tag childTag = fromElement(child);
                    map.put(childTag.getName(), childTag);
                });
                return new CompoundTag(element.getAttribute("name"), map);
            case "double":
                return new DoubleTag(element.getAttribute("name"), Double.valueOf(element.getTextContent()));
            case "float":
                return new FloatTag(element.getAttribute("name"), Float.valueOf(element.getTextContent()));
            case "ints":
                return new IntArrayTag(element.getAttribute("name"), decodeInts(element.getTextContent()));
            case "int":
                return new IntTag(element.getAttribute("name"), Integer.valueOf(element.getTextContent()));
            case "list":
                List<Tag> list = new ArrayList<>(element.getChildNodes().getLength());
                forEachElement(element.getChildNodes(), child -> list.add(fromElement(child)));
                return new ListTag<>(element.getAttribute("name"), (Class<Tag>) list.get(0).getClass(), list);
            case "longs":
                return new LongArrayTag(element.getAttribute("name"), decodeLongs(element.getTextContent()));
            case "long":
                return new LongTag(element.getAttribute("name"), Long.valueOf(element.getTextContent()));
            case "short":
                return new ShortTag(element.getAttribute("name"), Short.valueOf(element.getTextContent()));
            case "string":
                return new StringTag(element.getAttribute("name"), element.getTextContent());
            default:
                throw new IllegalArgumentException("Don't know how to convert element " + element + " to NBT tag");
        }
    }

    private static Element toElement(Document document, Tag tag) throws IOException {
        Element element;
        if (tag instanceof ByteArrayTag) {
            element = textElement(document, "bytes", encodeBytes(((ByteArrayTag) tag).getValue()));
        } else if (tag instanceof ByteTag) {
            element = textElement(document, "byte", Byte.toString(((ByteTag) tag).getValue()));
        } else if (tag instanceof CompoundTag) {
            element = document.createElement("compound");
            for (Tag childTag: ((CompoundTag) tag).getValue().values()) {
                element.appendChild(toElement(document, childTag));
            }
        } else if (tag instanceof DoubleTag) {
            element = textElement(document, "double", Double.toString(((DoubleTag) tag).getValue()));
        } else if (tag instanceof FloatTag) {
            element = textElement(document, "float", Float.toString(((FloatTag) tag).getValue()));
        } else if (tag instanceof IntArrayTag) {
            element = textElement(document, "ints", encodeInts(((IntArrayTag) tag).getValue()));
        } else if (tag instanceof IntTag) {
            element = textElement(document, "int", Integer.toString(((IntTag) tag).getValue()));
        } else if (tag instanceof ListTag) {
            element = document.createElement("list");
            for (Tag childTag: ((ListTag<?>) tag).getValue()) {
                element.appendChild(toElement(document, childTag));
            }
        } else if (tag instanceof LongArrayTag) {
            element = textElement(document, "longs", encodeLongs(((LongArrayTag) tag).getValue()));
        } else if (tag instanceof LongTag) {
            element = textElement(document, "long", Long.toString(((LongTag) tag).getValue()));
        } else if (tag instanceof ShortTag) {
            element = textElement(document, "short", Short.toString(((ShortTag) tag).getValue()));
        } else if (tag instanceof StringTag) {
            element =  textElement(document, "string", ((StringTag) tag).getValue());
        } else {
            throw new IllegalArgumentException("Don't know how to convert tag " + tag + " to XML element");
        }
        if (! isNullOrEmpty(tag.getName())) {
            element.setAttribute("name", tag.getName());
        }
        return element;
    }

    private static Element textElement(Document document, String name, String text) {
        Element element = document.createElement(name);
        element.setTextContent(text);
        return element;
    }

    private static String encodeBytes(byte[] bytes) throws IOException {
        ByteArrayOutputStream outputStore = new ByteArrayOutputStream();
        try (GZIPOutputStream compressor = new GZIPOutputStream(outputStore)) {
            compressor.write(bytes);
        }
        byte[] compressedBytes = outputStore.toByteArray();
        if (compressedBytes.length < bytes.length) {
            return "g" + Base64.getEncoder().encodeToString(compressedBytes);
        } else {
            return "u" + Base64.getEncoder().encodeToString(bytes);
        }
    }

    private static String encodeInts(int[] ints) throws IOException {
        ByteArrayOutputStream outputStore = new ByteArrayOutputStream();
        try (GZIPOutputStream compressor = new GZIPOutputStream(outputStore)) {
            for (int i: ints) {
                compressor.write( i & 0x000000ff);
                compressor.write((i & 0x0000ff00) >> 8);
                compressor.write((i & 0x00ff0000) >> 16);
                compressor.write((i & 0xff000000) >>> 24);
            }
        }
        byte[] compressedBytes = outputStore.toByteArray();
        return "g" + Base64.getEncoder().encodeToString(compressedBytes);
    }

    private static String encodeLongs(long[] longs) throws IOException {
        ByteArrayOutputStream outputStore = new ByteArrayOutputStream();
        try (GZIPOutputStream compressor = new GZIPOutputStream(outputStore)) {
            for (long l: longs) {
                compressor.write((int)  (l & 0x00000000000000ffL));
                compressor.write((int) ((l & 0x000000000000ff00L) >> 8));
                compressor.write((int) ((l & 0x0000000000ff0000L) >> 16));
                compressor.write((int) ((l & 0x00000000ff000000L) >> 24));
                compressor.write((int) ((l & 0x000000ff00000000L) >> 32));
                compressor.write((int) ((l & 0x0000ff0000000000L) >> 40));
                compressor.write((int) ((l & 0x00ff000000000000L) >> 48));
                compressor.write((int) ((l & 0xff00000000000000L) >> 56));
            }
        }
        byte[] compressedBytes = outputStore.toByteArray();
        return "g" + Base64.getEncoder().encodeToString(compressedBytes);
    }

    private static byte[] decodeBytes(String str) throws IOException {
        if (str.startsWith("g")) {
            byte[] compressedBytes = Base64.getDecoder().decode(str.substring(1));
            ByteArrayOutputStream outputStore = new ByteArrayOutputStream();
            GZIPInputStream decompressor = new GZIPInputStream(new ByteArrayInputStream(compressedBytes));
            StreamUtils.copy(decompressor, outputStore);
            return outputStore.toByteArray();
        } else if (str.startsWith("u")) {
            return Base64.getDecoder().decode(str.substring(1));
        } else {
            throw new IllegalArgumentException("Compression method flag missing");
        }
    }

    private static int[] decodeInts(String str) throws IOException {
        if (str.startsWith("g")) {
            byte[] bytes = decodeBytes(str);
            if (bytes.length % 4 != 0) {
                throw new IllegalArgumentException("Byte count not a multiple of four (size of int)");
            }
            int[] ints = new int[bytes.length / 4];
            for (int i = 0; i < ints.length; i++) {
                ints[i] =  (bytes[ i << 2]      & 0xff)
                        | ((bytes[(i << 2) | 1] & 0xff) << 8)
                        | ((bytes[(i << 2) | 2] & 0xff) << 16)
                        | ((bytes[(i << 2) | 3] & 0xff) << 24);
            }
            return ints;
        } else {
            throw new IllegalArgumentException("Compression method flag missing");
        }
    }

    private static long[] decodeLongs(String str) throws IOException {
        if (str.startsWith("g")) {
            byte[] bytes = decodeBytes(str);
            if (bytes.length % 8 != 0) {
                throw new IllegalArgumentException("Byte count not a multiple of eight (size of long)");
            }
            long[] longs = new long[bytes.length / 8];
            for (int i = 0; i < longs.length; i++) {
                longs[i] = (bytes[ i << 3]      & 0xffL)
                        | ((bytes[(i << 3) | 1] & 0xffL) << 8)
                        | ((bytes[(i << 3) | 2] & 0xffL) << 16)
                        | ((bytes[(i << 3) | 3] & 0xffL) << 24)
                        | ((bytes[(i << 3) | 4] & 0xffL) << 32)
                        | ((bytes[(i << 3) | 5] & 0xffL) << 40)
                        | ((bytes[(i << 3) | 6] & 0xffL) << 48)
                        | ((bytes[(i << 3) | 7] & 0xffL) << 56);
            }
            return longs;
        } else {
            throw new IllegalArgumentException("Compression method flag missing");
        }
    }
}