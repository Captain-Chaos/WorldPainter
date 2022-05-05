package org.pepsoft.util;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * A {@link LinkedBlockingDeque} of which the non ordered element addition methods add elements at the start of the
 * queue rather than the end. When used as a {@link Queue} this will result in a LIFO (last in first out) queue.
 *
 * <p><strong>Note:</strong> that this means it breaks the contract of {@link Deque}.
 *
 * @param <E> The element type of the queue.
 */
public class LifoBlockingDeque<E> extends LinkedBlockingDeque<E> {
    /**
     * Add an element to the start of the queue.
     *
     * @param e The element to add.
     * @return {@code true}
     * @throws IllegalStateException If there is no space on the queue.
     */
    @Override
    public boolean add(E e) {
        super.addFirst(e);
        return true;
    }

    /**
     * Add an element to the start of the queue, if there is space, without blocking.
     *
     * @param e The element to add.
     * @return {@code true} if the element was added or {@code false} if there was no space.
     */
    @Override
    public boolean offer(E e) {
        return super.offerFirst(e);
    }

    /**
     * Add an element to the start of the queue, if there is space, waiting for the specified amount of time if
     * necessary for space to become available.
     *
     * @param e The element to add.
     * @param timeout The maximum amount of time to wait for space to become available.
     * @param unit The unit of {@code timeout}.
     * @return {@code true} if the element was added or {@code false} if there was no space after waiting the specified
     * amount of time.
     * @throws InterruptedException If the thread was interrupted while waiting for space to become available.
     */
    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return super.offerFirst(e, timeout, unit);
    }

    /**
     * Add an element to the start of the queue, waiting indefinitely for space to become available if necessary.
     *
     * @param e The element to add.
     * @throws InterruptedException If the thread was interrupted while waiting for space to become available.
     */
    @Override
    public void put(E e) throws InterruptedException {
        super.putFirst(e);
    }
}