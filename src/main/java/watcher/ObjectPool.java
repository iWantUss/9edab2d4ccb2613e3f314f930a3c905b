package watcher;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class ObjectPool<T> {
    private static final int maxHandlers = Runtime.getRuntime().availableProcessors() * 2;
    private Queue<T> locked, unlocked;

    public ObjectPool() {
        locked = new ConcurrentLinkedQueue<>();
        unlocked = new ConcurrentLinkedQueue<>();
    }

    protected abstract T create();

    public synchronized T checkOut() {
        T t;
        if (unlocked.size() > 0) {
            t = unlocked.poll();
            if (t != null) {
                locked.add(t);
//                System.out.println(String.format("locked [%d], unlocked [%d] - %s", locked.size(), unlocked.size(), t.getClass().getSimpleName()));
                return (t);
            }
        }
        t = create();
        locked.add(t);
//        System.out.println(String.format("locked [%d], unlocked [%d] - %s", locked.size(), unlocked.size(), t.getClass().getSimpleName()));
        return (t);
    }

    public synchronized void checkIn(T t) {
        locked.remove(t);
        if (unlocked.size() < maxHandlers) {
            unlocked.add(t);
        }
//        System.out.println(String.format("locked [%d], unlocked [%d] - %s", locked.size(), unlocked.size(), t.getClass().getSimpleName()));
    }
}