package com.gmail.frogocomics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Queue<UserTask> userTasks = new PriorityQueue<>();
    private static Queue<BackgroundTask> backgroundTasks = new PriorityQueue<>();

    private static int initialValue1 = 0;
    private static int initialValue2 = 0;

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
        if(t instanceof UserTask) {
            t.getThread().join();
        }
    }

    /**
     * Runs all tasks in order and clears the queue.
     *
     * @throws InterruptedException When a task thread is interrupted
     */
   public static void runAll() throws InterruptedException {
       //If the thread is a background task, it will one it all at once, but it will run user tasks
       //one after the other.
       for(Task t : tasks) {
           if(t instanceof UserTask) {
               userTasks.add((UserTask) t);
           } else if(t instanceof BackgroundTask) {
               backgroundTasks.add((BackgroundTask) t);
           }
       }
       initialValue1 = userTasks.size();
       initialValue2 = backgroundTasks.size();
       Thread userTaskRunner = new Thread(() -> {
           try {
               for (UserTask ut : userTasks) {
                   ut.getThread().run();
                   ut.getThread().join();
                   userTasks.poll();
               }
           } catch(InterruptedException e) {
               //TODO: Open the error dialog. I can't seem to make it work as I do not want to introduce circular dependencies.
           }
       });
       userTaskRunner.run();
       Thread backgroundTaskRunner = new Thread(() -> {
           for (BackgroundTask bt : backgroundTasks) {
               bt.getThread().run();
               backgroundTasks.poll();
           }
       });
       backgroundTaskRunner.run();
       tasks.clear();
       userTasks.clear();
       backgroundTasks.clear();
       initialValue1 = 0;
       initialValue2 = 0;
   }

    /**
     * Get the initial amount of tasks.
     *
     * @return Returns the amount of tasks.
     */
    public static int getTaskInitialAmount(Class<? extends Task> type) {
       if(type == UserTask.class) {
           return initialValue1;
       } else {
           return initialValue2;
       }
    }

    /**
     * Get the current amount of tasks.
     *
     * @return Returns the amount of tasks.
     */
    public static int getTaskCurrentAmount(Class<? extends Task> type) {
        if(type == UserTask.class) {
            return userTasks.size();
        } else {
            return backgroundTasks.size();
        }
    }

    private static Logger logger = LoggerFactory.getLogger(TaskManager.class);
}
