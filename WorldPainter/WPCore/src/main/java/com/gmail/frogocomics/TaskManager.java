package com.gmail.frogocomics;

import java.util.Optional;
import java.util.Queue;

/**
 *
 * @author Jeff Chen
 */
public class TaskManager {

    private static Queue<Task> tasks;

    private TaskManager() {
    }

    protected static void addTask(Task task) {
        tasks.add(task);
    }

    protected static void removeTask(Task task) {
        tasks.remove(task);
    }

    public static Optional<Task> peek() {
        return Optional.of(tasks.peek());
    }

    public static Optional<Task> poll() {
        return Optional.of(tasks.poll());
    }

   public static void run() throws InterruptedException {
       for(Task t : tasks) {
           t.getThread().run();
           t.getThread().join();
       }
       tasks.clear();
   }

}
