/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util.jobqueue;

import java.util.*;

/**
 * A combination of the List and Set interfaces. A List which can only contain
 * equal objects once (according to their <code>equals()</code> and
 * <code>hashCode()</code> methods). Or in other words an ordered Set.
 * 
 * <p>This collection's iterator is read-only and does not support the add(),
 * etc. methods. It is also <i>not</i> fail-fast! If you modify the collection
 * while iterating over it, the results are unspecified.
 *
 * @author pepijn
 */
public class HashList<E> extends AbstractList<E> implements Set<E> {
    public HashList() {
        anchor = new Element<>(null);
        anchor.previous = anchor;
        anchor.next = anchor;
        map = new HashMap<>();
    }
    
    public HashList(int initialCapacity) {
        anchor = new Element<>(null);
        anchor.previous = anchor;
        anchor.next = anchor;
        map = new HashMap<>(initialCapacity);
    }
    
    @Override
    @SuppressWarnings({"element-type-mismatch", "SuspiciousMethodCalls"})
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean addToEnd(E e) {
        Element<E> element = map.get(e);
        if (element != null) {
            if (anchor.previous == element) {
                // Object already at the end
                return false;
            } else {
                // Object on the list, but not at the end
                element.previous.next = element.next;
                element.next.previous = element.previous;
                element.next = anchor;
                element.previous = anchor.previous;
                element.previous.next = element;
                anchor.previous = element;
                return true;
            }
        } else {
            // Object not on the list
            element = new Element<>(e);
            map.put(e, element);
            element.next = anchor;
            element.previous = anchor.previous;
            element.previous.next = element;
            anchor.previous = element;
            return true;
        }
    }
    
    @Override
    public boolean add(E e) {
        if (map.containsKey(e)) {
            return false;
        } else {
            Element<E> element = new Element<>(e);
            map.put(e, element);
            element.next = anchor;
            element.previous = anchor.previous;
            element.previous.next = element;
            anchor.previous = element;
            return true;
        }
    }

    @Override
    @SuppressWarnings({"element-type-mismatch", "SuspiciousMethodCalls"})
    public boolean remove(Object o) {
        if (map.containsKey(o)) {
            Element element = map.remove(o);
            element.previous.next = element.next;
            element.next.previous = element.previous;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        map.clear();
        anchor.next = anchor;
        anchor.previous = anchor;
    }
    
    @Override
    public Iterator<E> iterator() {
        return listIterator(0);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public E get(int index) {
        if ((index < 0) || (index >= map.size())) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
        Element<E> element = anchor;
        for (int i = 0; i <= index; i++) {
            element = element.next;
        }
        return element.object;
    }

    @Override
    public E set(int index, E e) {
        if ((index < 0) || (index >= map.size())) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
        Element<E> element = anchor;
        for (int i = 0; i <= index; i++) {
            element = element.next;
        }
        E previous = element.object;
        element.object = e;
        map.put(e, element);
        return previous;
    }

    @Override
    public void add(int index, E e) {
        if ((index < 0) || (index > map.size())) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
        Element<E> element = anchor;
        for (int i = 0; i < index; i++) {
            element = element.next;
        }
        Element<E> newElement = new Element<>(e);
        newElement.previous = element;
        newElement.next = element.next;
        element.next.previous = newElement;
        element.next = newElement;
        if (map.containsKey(e)) {
            Element existingElement = map.get(e);
            existingElement.previous.next = existingElement.next;
            existingElement.next.previous = existingElement.previous;
        }
        map.put(e, newElement);
    }

    @Override
    public E remove(int index) {
        if ((index < 0) || (index >= map.size())) {
            throw new IndexOutOfBoundsException(Integer.toString(index));
        }
        Element<E> element = anchor;
        for (int i = 0; i <= index; i++) {
            element = element.next;
        }
        element.previous.next = element.next;
        element.next.previous = element.previous;
        map.remove(element.object);
        return element.object;
    }

    @Override
    @SuppressWarnings({"element-type-mismatch", "SuspiciousMethodCalls"})
    public int indexOf(Object o) {
        if (! map.containsKey(o)) {
            return -1;
        } else {
            Element element = map.get(o);
            int count = 0;
            while (element.previous != anchor) {
                element = element.previous;
                count++;
            }
            return count;
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    @Override
    public ListIterator<E> listIterator(final int initialIndex) {
        if ((initialIndex < 0) || (initialIndex >= map.size())) {
            throw new IndexOutOfBoundsException(Integer.toString(initialIndex));
        }
        Element<E> element = anchor;
        for (int i = 0; i <= initialIndex; i++) {
            element = element.next;
        }
        final Element<E> startElement = element;
        return new ListIterator<E>() {
            @Override
            public boolean hasNext() {
                return element.next != anchor;
            }

            @Override
            public E next() {
                if (element.next == anchor) {
                    throw new NoSuchElementException();
                }
                element = element.next;
                index++;
                return element.object;
            }

            @Override
            public boolean hasPrevious() {
                return element != anchor;
            }

            @Override
            public E previous() {
                if (element == anchor) {
                    throw new NoSuchElementException();
                }
                E obj = element.object;
                element = element.previous;
                index--;
                return obj;
            }

            @Override
            public int nextIndex() {
                return index + 1;
            }

            @Override
            public int previousIndex() {
                return index;
            }

            @Override
            public void remove() {
                HashList.this.remove(element.object);
            }

            @Override
            public void set(E e) {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public void add(E e) {
                throw new UnsupportedOperationException("Not supported");
            }
            
            private Element<E> element = startElement;
            private int index = initialIndex - 1;
        };
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.ORDERED | Spliterator.DISTINCT);
    }

    private final Map<E, Element<E>> map;
    private final Element<E> anchor;
    
    static class Element<T> {
        Element(T object) {
            this.object = object;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Element other = (Element) obj;
            if (this.object != other.object && (this.object == null || !this.object.equals(other.object))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + (this.object != null ? this.object.hashCode() : 0);
            return hash;
        }
        
        T object;
        Element<T> previous, next;
    }
}