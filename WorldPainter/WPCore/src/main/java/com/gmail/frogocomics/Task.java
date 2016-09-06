package com.gmail.frogocomics;

import org.pepsoft.util.jobqueue.Job;

public interface Task extends Job {

    Task addToQueue();

    Task removeFromQueue();

    String getName();

    Thread getThread();

}
