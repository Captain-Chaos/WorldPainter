package com.gmail.frogocomics;

import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * The task manager stores loaded tasks and has options to execute them in order and add/remove
 * any, if needed.
 *
 * @author Jeff Chen
 */
public class TaskManager {

    private static Queue<Task> tasks = new PriorityQueue<>();

    /**
     * Prevent initialization.
     */
    private TaskManager() {
    }

    /**
     * Add a task to the end of the queue.
     *
     * @param task The task to add
     */
    protected static void addTask(Task task) {
        tasks.add(task);
    }

    /**
     * Remove a specific task from the queue, provided that it exists.
     *
     * @param task The task to remove, if it exists
     */
    protected static void removeTask(Task task) {
        tasks.remove(task);
    }

    /**
     * Get the first task from the queue, without removing it.
     *
     * @return Returns the first task
     */
    public static Optional<Task> peek() {
        return Optional.of(tasks.peek());
    }

    /**
     * Gets and removes the first task from the queue.
     *
     * @return Returns the first task
     */
    public static Optional<Task> poll() {
        return Optional.of(tasks.poll());
    }

    /**
     * Gets all the current tasks ready to be executed.
     *
     * @return Returns the tasks if there is any. If there isn't, {@link Optional#empty()} will
     *         be returned.
     */
    public static Optional<Queue<Task>> getTasks() {
        if (tasks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tasks);
    }

    /**
     * Run one task.
     *
     * @throws InterruptedException When a task thread is interrupted
     */
    public static void runOne() throws InterruptedException {
        Task t = tasks.poll();
        t.getThread().start();
        t.getThread().join();
    }

    /**
     * Runs all tasks in order and clears the queue.
     *
     * @throws InterruptedException When a task thread is interrupted
     */
   public static void runAll() throws InterruptedException {
       for(Task t : tasks) {
           t.getThread().run();
           t.getThread().join();
           tasks.poll();
       }
       tasks.clear();
   }

    /**
     * Get the amount of tasks.
     *
     * @return Returns the amount of tasks.
     */
    public static int getTaskAmount() {
        return tasks.size();
    }
}
