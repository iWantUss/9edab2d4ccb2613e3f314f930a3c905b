package watcher;

import storage.StorageService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WatcherServicePool extends ObjectPool<WatcherService> implements WatcherService{

    private static volatile WatcherServicePool mInstance;

    private WatcherServicePool() {
    }

    public static WatcherServicePool getInstance() {
        if (mInstance == null) {
            synchronized (WatcherServicePool.class) {
                if (mInstance == null) {
                    mInstance = new WatcherServicePool();
                }
            }
        }
        return mInstance;
    }

    @Override
    protected WatcherService create() {
        return new WatcherServiceImpl();
    }

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public Set<Integer> getFriends(int user) {
        WatcherService watcherService = this.checkOut();
        try {
            return executor.submit(() -> watcherService.getFriends(user)).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            this.checkIn(watcherService);
        }
        return Collections.emptySet();
    }

    @Override
    public Set<Integer> getFriendsAndFollowers(int parent) {
        WatcherService watcherService = this.checkOut();
        try {
            return executor.submit(() -> watcherService.getFriendsAndFollowers(parent)).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            this.checkIn(watcherService);
        }
        return Collections.emptySet();
    }

}
