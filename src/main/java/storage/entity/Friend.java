package storage.entity;

import java.util.HashMap;
import java.util.Map;

public class Friend {
    int source;
    int target;

    public Friend(int source, int target) {
        this.source = source;
        this.target = target;
    }

    public Map<String, Object> toMap(){
        return toMap(source, target);
    }

    public static Map<String, Object> toMap(int source, int target) {
        HashMap<String, Object> res = new HashMap<>(2);
        res.put("source", source);
        res.put("target", target);
        return res;
    }
}
