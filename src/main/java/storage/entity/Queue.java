package storage.entity;

import java.util.HashMap;
import java.util.Map;

public class Queue {
    int source;
    int parent;
    boolean scanned;

    public Queue(int source, int parent) {
        this(source, parent, false);
    }

    public Queue(int source, int parent, boolean scanned) {
        this.source = source;
        this.parent = parent;
        this.scanned = scanned;
    }


    public Map<String, Object> toMap() {
        return toMap(source, parent, scanned);
    }

    public static Map<String, Object> toMap(int source, int parent, boolean scanned){
        HashMap<String, Object> res = new HashMap<>(4);
        res.put("source", source);
        res.put("parent", parent);
        res.put("target", scanned ? 1 : 0);
        return res;
    }

    public static Map<String, Object> toMap(int source, int parent){
        return toMap(source, parent, false);
    }
}
