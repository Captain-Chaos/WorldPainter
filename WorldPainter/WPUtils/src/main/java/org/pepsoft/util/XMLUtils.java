package org.pepsoft.util;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utility methods for working with XML APIs.
 */
public final class XMLUtils {
    private XMLUtils() {
        // Prevent instantiation
    }

    /**
     * Visit each element in a node list. This method assumes that every node is
     * an {@link Element}, otherwise it will throw a {@code ClassCastException}.
     *
     * @param nodeList The node list over which to iterate.
     * @param visitor The visitor to invoke for each element on the node list.
     * @throws ClassCastException If not all nodes on the list are an
     * {@code Element}.
     * @throws E If the visitor throws a checked exception of the specified or
     * inferred type.
     */
    public static <E extends Exception> void forEachElement(NodeList nodeList, ElementVisitor<E> visitor) throws E {
        for (int i = 0; i < nodeList.getLength(); i++) {
            visitor.visit((Element) nodeList.item(i));
        }
    }

    @FunctionalInterface
    public interface ElementVisitor<E extends Exception> {
        void visit(Element element) throws E;
    }
}