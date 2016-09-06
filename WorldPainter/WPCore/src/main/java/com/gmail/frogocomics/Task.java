package com.gmail.frogocomics;

/**
 *
 * @author Jeff Chen
 */
public class Task {

    private Thread task;

    public Task(Thread task) {
        this.task = task;
    }

    public Task addToQueue() {
        TaskManager.addTask(this);
        return this;
    }

    public Task removeFromQueue() {
        TaskManager.removeTask(this);
        return this;
    }

    protected Thread getThread() {
        return task;
    }
}
