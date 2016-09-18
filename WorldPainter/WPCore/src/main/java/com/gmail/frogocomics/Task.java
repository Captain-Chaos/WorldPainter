package com.gmail.frogocomics;

import org.pepsoft.util.jobqueue.Job;

import java.util.UUID;

public interface Task extends Job {

    Task addToQueue() throws DuplicateTaskException;

    Task removeFromQueue();

    String getName();

    Thread getThread();

    UUID getUniqueId();

}
