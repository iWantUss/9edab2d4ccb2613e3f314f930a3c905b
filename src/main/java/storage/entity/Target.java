package storage.entity;

import java.util.HashMap;
import java.util.Map;

public class Target {
    int sourceid;
    int targetId;
    boolean hasHandlers = false;

    public Target(int sourceid, int targetId, boolean hasHandlers) {
        this.sourceid = sourceid;
        this.targetId = targetId;
        this.hasHandlers = hasHandlers;
    }

    public Target(int sourceid, int targetId) {
        this(sourceid, targetId, false);
    }

    public Map<String, Object> toMap(){
        HashMap<String, Object> res = new HashMap<>(3);
        res.put("sourceid", sourceid);
        res.put("targetId", targetId);
        res.put("hasHandlers", hasHandlers ? 1 : 0);
        return res;
    }
}
