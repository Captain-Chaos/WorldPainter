package com.gmail.frogocomics;

/**
 *
 *
 * @author Jeff Chen
 */
public class BackgroundTask implements Task {

    private Thread task;
    private String name;

    public BackgroundTask(Thread task) {
        new BackgroundTask(task, "Background Process");
    }

    public BackgroundTask(Thread task, String name) {
        this.task = task;
        this.name = name;
    }

    @Override
    public Task addToQueue() {
        TaskManager.addTask(this);
        return this;
    }

    @Override
    public Task removeFromQueue() {
        TaskManager.removeTask(this);
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Thread getThread() {
        return task;
    }
}
