package com.gmail.frogocomics;

import java.util.UUID;

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
    private UUID uuid;

    /**
     * Create a new task.
     *
     * @param task The task to execute in the {@link TaskManager}.
     * @param uuid The unique id of this task.
     */
    public BackgroundTask(Thread task, UUID uuid) {
        new BackgroundTask(task, "Background Process", uuid);
    }

    /**
     * Create a new task.
     *
     * @param task The task to execute in the {@link TaskManager}.
     * @param name The name of the task to display in the WorldPainter gui.
     * @param uuid The unique id of this task.
     */
    public BackgroundTask(Thread task, String name, UUID uuid) {
        this.task = task;
        this.name = name;
        this.uuid = uuid;
    }

    /**
     * Add the task to the task manager.
     *
     * @return Returns this class, for chaining.
     */
    @Override
    public Task addToQueue() throws DuplicateTaskException {
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

    /**
     * Get the unique id of this task, so that same tasks cannot be run twice.
     *
     * @return Returns the unique id (UUID) of this task.
     */
    @Override
    public UUID getUniqueId() {
        return this.uuid;
    }
}
