package com.networkprobe.cli;

public abstract class ExecutionWorker implements Runnable {

    private final boolean updatable;
    private final boolean daemon;
    private final String name;

    private volatile boolean state = false;
    private volatile Thread worker;

    public ExecutionWorker(String name, boolean updatable, boolean daemon) {
        this.name = name;
        this.daemon = daemon;
        this.updatable = updatable;
    }

    public void start() {
        if (!state) {
            state = true;
            worker = new Thread(this, name);
            worker.setDaemon(daemon);
            worker.start();
        }
    }

    public void stop() {
        if (state) {
            state = false;
            interrupt(worker);
            worker = null;
        }
    }

    private void interrupt(Thread thread) {
        try {
            thread.interrupt();
        } catch (Exception e) {/* ignore */}
    }

    @Override
    public void run() {
        onBegin();
        while (state && updatable) {
            onUpdate();
        }
        onStop();
    }

    public boolean isAlive() {
        return state;
    }

    protected void onBegin() {}
    protected void onUpdate() {}
    protected void onStop() {}

}
