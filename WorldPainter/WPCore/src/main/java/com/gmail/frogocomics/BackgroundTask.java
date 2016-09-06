package com.gmail.frogocomics;

/**
 * Represents a task to be executed in order in the {@link TaskManager}. Tasks consists of a
 * {@link Thread} and a name (To display on the gui), but they do not function like threads as the
 * task manager does not allow them to run at the same time. Tasks can consist of anything, such as
 * (mostly) global operations, or perhaps brush operations. Background Tasks are run all at once.
 * <p><code>
 *     <i>//Create a task</i>
 *     BackgroundTask task = new BackgroundTask(new Thread(() -> {
 *         <i>//Do whatever</i>
 *     }).addToQueue();
 *     TaskManager.run();
 * </code></p>
 *
 * @author Jeff Chen
 */
public class BackgroundTask implements Task {

    private Thread task;
    private String name;

    /**
     * Create a new task.
     *
     * @param task The task to execute in the {@link TaskManager}.
     */
    public BackgroundTask(Thread task) {
        new BackgroundTask(task, "Background Process");
    }

    /**
     * Create a new task.
     *
     * @param task The task to execute in the {@link TaskManager}.
     * @param name The name of the task to display in the WorldPainter gui.
     */
    public BackgroundTask(Thread task, String name) {
        this.task = task;
        this.name = name;
    }

    /**
     * Add the task to the task manager.
     *
     * @return Returns this class, for chaining.
     */
    @Override
    public Task addToQueue() {
        TaskManager.addTask(this);
        return this;
    }

    /**
     * Remove the task to the task manager.
     *
     * @return Returns this class, for chaining.
     */
    @Override
    public Task removeFromQueue() {
        TaskManager.removeTask(this);
        return this;
    }

    /**
     * Get the name of this task, to display in the WorldPainter graphical user interface.
     *
     * @return Returns the name of this task.
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the thread to execute. Only {@link TaskManager} should be using this method.
     *
     * @return Returns the thread used to execute this task
     */
    @Override
    public Thread getThread() {
        return task;
    }
}
