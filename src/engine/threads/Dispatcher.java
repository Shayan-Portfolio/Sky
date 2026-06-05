package engine.threads;

import engine.logging.SkyRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Dispatcher {
    private List<Future> futures = new ArrayList<>();
    private ExecutorService executorService;

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Future submit(Runnable runnable) {
        Future future = executorService.submit(runnable);
        futures.add(future);
        return future;
    }

    public void tick() {
        futures.removeIf(Future::isDone);
    }
    public boolean isBusy() {
        for(Future future : futures) {
            if(!future.isDone()) return true;
        }
        return false;
    }

    public void join() {
        do tick(); while (isBusy());
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
