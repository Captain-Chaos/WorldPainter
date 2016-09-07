package com.gmail.frogocomics;

import java.util.UUID;

/**
 * Represents a task to be executed in order in the {@link TaskManager}. Tasks consists of a
 * {@link Thread} and a name (To display on the gui), but they do not function like threads as the
 * task manager does not allow them to run at the same time. Tasks can consist of anything, such as
 * (mostly) global operations, or perhaps brush operations. A {@link UserTask} executes one after
 * the other, as most, if not all user operations would require a specific order.
 * <p><code>
 *     <i>//Create a task</i>
 *     UserTask task = new UserTask(new Thread(() -> {
 *         <i>//Do whatever</i>
 *     }).addToQueue();
 *     TaskManager.run();
 * </code></p>
 *
 * @author Jeff Chen
 */
public class UserTask implements Task {

    private Thread task;
    private String name;
    private TaskTypes type;
    private UUID uuid;

    /**
     * Create a new task.
     *
     * @param task The task to execute in the {@link TaskManager}.
     * @param uuid The unique id of this task.
     */
    public UserTask(Thread task, UUID uuid) {
        new UserTask(task, "Unknown Task", TaskTypes.UNKNOWN, uuid);
    }

    /**
     * Create a new task.
     *
     * @param task The task to execute in the {@link TaskManager}.
     * @param name The name of the task to display in the WorldPainter gui.
     * @param uuid The unique id of this task.
     */
    public UserTask(Thread task, String name, UUID uuid) {
        new UserTask(task, name, TaskTypes.UNKNOWN, uuid);
    }

    /**
     * Create a new task.
     *
     * @param task The task to execute in the {@link TaskManager}.
     * @param name The name of the task to display in the WorldPainter gui.
     * @param type The type of task to be executed.
     * @param uuid The unique id of this task.
     */
    public UserTask(Thread task, String name, TaskTypes type, UUID uuid) {
        this.task = task;
        this.name = name;
        this.type = type;
        this.uuid = uuid;
    }

    /**
     * Add the task to the task manager.
     *
     * @return Returns this class, for chaining.
     */
    @Override
    public UserTask addToQueue() throws DuplicateTaskException {
        TaskManager.addTask(this);
        return this;
    }

    /**
     * Remove the task to the task manager.
     *
     * @return Returns this class, for chaining.
     */
    @Override
    public UserTask removeFromQueue() {
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
     * Get the {@link TaskTypes} for this task.
     *
     * @return Returns the task type for this task.
     */
    public TaskTypes getType() {
        return this.type;
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
