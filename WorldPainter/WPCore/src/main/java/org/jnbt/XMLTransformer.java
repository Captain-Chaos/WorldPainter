package org.jnbt;

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
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static Tag fromElement(Element element) {
        switch (element.getTagName()) {
            case "compound":
                Map<String, Tag> map = new HashMap<>();
                forEachElement(element.getChildNodes(), child -> {
                    Tag childTag = fromElement(child);
                    map.put(childTag.getName(), childTag);
                });
                return new CompoundTag(element.getAttribute("name"), map);
            case "string":
                return new StringTag(element.getAttribute("name"), element.getTextContent());
            case "byte":
                return new ByteTag(element.getAttribute("name"), Byte.parseByte(element.getTextContent()));
            case "list":
                List<Tag> list = new ArrayList<>(element.getChildNodes().getLength());
                forEachElement(element.getChildNodes(), child -> list.add(fromElement(child)));
                return new ListTag(element.getAttribute("name"), list.get(0).getClass(), list);
            default:
                throw new IllegalArgumentException("Don't know how to convert element " + element + " to NBT tag");
        }
    }

    private static Element toElement(Document document, Tag tag) {
        Element element;
        if (tag instanceof CompoundTag) {
            element = document.createElement("compound");
            for (Tag childTag: ((CompoundTag) tag).getValue().values()) {
                element.appendChild(toElement(document, childTag));
            }
        } else if (tag instanceof StringTag) {
            element = document.createElement("string");
            element.setTextContent(((StringTag) tag).getValue());
        } else if (tag instanceof ByteTag) {
            element = document.createElement("byte");
            element.setTextContent(((ByteTag) tag).getValue().toString());
        } else if (tag instanceof ListTag) {
            element = document.createElement("list");
            for (Tag childTag: ((ListTag) tag).getValue()) {
                element.appendChild(toElement(document, childTag));
            }
        } else {
            throw new IllegalArgumentException("Don't know how to convert tag " + tag + " to XML element");
        }
        if (! isNullOrEmpty(tag.getName())) {
            element.setAttribute("name", tag.getName());
        }
        return element;
    }
}