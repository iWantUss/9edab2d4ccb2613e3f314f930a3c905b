package storage;

import javafx.util.Pair;
import storage.entity.Friend;
import storage.entity.Queue;
import storage.entity.Target;
import tools.mysql.Config;
import tools.mysql.MysqlService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class StorageServiceImpl implements StorageService {

    private static final String TARGETS_TABLE_NAME = "targets";
    MysqlService ms = MysqlService.getInstance(Config.getInstance());

    @Override
    public void addHandlers(int target, int source, Collection<Integer> users) {
        for (Integer id : users) {
            ms.insert(String.format("handler%d", target), Friend.toMap(source, id));
        }
    }

    @Override
    public void addTarget(int source, Set<Integer> targets) {
        targets.stream().filter(target -> target != source)
                .map(target -> new Target(source, target).toMap())
                .forEach(handler -> ms.insert(TARGETS_TABLE_NAME, handler));
    }

    //TODO: jpa hibernate
    @Override
    public void init() {
        ms.execute("CREATE TABLE IF NOT EXISTS `targets`" +
                " (" +
                " id int PRIMARY KEY NOT NULL AUTO_INCREMENT," +
                " sourceid int NOT NULL," +
                " targetId int NOT NULL," +
                " hasHandlers tinyint(1) DEFAULT '0' NOT NULL" +
                ");");
        ms.execute("CREATE TABLE IF NOT EXISTS `queue`" +
                " (" +
                " id int PRIMARY KEY NOT NULL AUTO_INCREMENT," +
                " source int NOT NULL," +
                " target tinyint(1) DEFAULT '0' NOT NULL," +
                " parent int NOT NULL," +
                " constraint queue_source_uindex UNIQUE (source)" +
                ");");
        ms.execute("CREATE INDEX queue_parent_index ON `queue` (parent)");
    }

    @Override
    public void createTargetHandlers(int target) {
        ms.execute(String.format("CREATE TABLE IF NOT EXISTS handler%d " +
                "(" +
                " id int PRIMARY KEY NOT NULL AUTO_INCREMENT," +
                " source int NOT NULL," +
                " target int NOT NULL," +
                " fetched tinyint(1) DEFAULT '0' NOT NULL" +
                ")", target));
        ms.execute(String.format("CREATE INDEX handler%1$d_source_index ON handler%1$d (source);", target));
        ms.execute(String.format("CREATE UNIQUE INDEX handler%1$d_uindex ON handler%1$d (target);", target));
        HashMap<String, Object> res = new HashMap<>(2);
        res.put("source", target);
        res.put("target", target);
        ms.insert(String.format("handler%d", target), res);
    }


    @Override
    public void addInQueue(Set<Integer> users, int target) {
        users.stream().map(user -> Queue.toMap(user, target))
                .forEach(user -> ms.insert("queue", user));
    }

    @Override
    public synchronized int getNextUser(int parent) {
        ResultSet read = ms.read("source", "queue", String.format("`target`=0 AND `parent`=%d LIMIT 0,1", parent));
        int anInt = -1;
        try {
            read.next();
            anInt = read.getInt(1);
            return anInt;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            userScaned(anInt, parent);
        }
        throw new RuntimeException("НЕТ НОВОГО ПОЛЬЗОВАТЕЛЯ");
    }


    @Override
    public boolean hasNextUser(int parent) {
        ResultSet read = ms.read("source", "queue", String.format("`target`=0  AND `parent`=%d LIMIT 0,1", parent));
        try {
            return read.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Set<Integer> getNextUsers(int offset, int count) {
        ResultSet read = ms.read("source", "queue", String.format("`target`=0 LIMIT %d,%d", offset, count));
        try {
            Set<Integer> res = new HashSet<>(count);
            while (read.next()) {
                res.add(read.getInt(1));
            }
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    @Override
    public ResultSet getDistinctFriends(int parent, int offset, int count) {
        return ms.read("DISTINCT target", String.format("handler%d", parent), "");
    }

    @Override
    public ResultSet getFriends(int parent, int offset, int count) {
        return ms.read("source, target", String.format("handler%d", parent), "");
    }

    @Override
    public Set<Integer> getFindedFriendship() {
        ResultSet read = ms.read("DISTINCT sourceid", TARGETS_TABLE_NAME, "`hasHandlers`=1");
        Set<Integer> res = new HashSet<>();
        try {
            while (read.next()) {
                res.add(read.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
        return res;
    }

    @Override
    public Map<Integer, Integer> getDirectFriendship(int parent, Set<Integer> target) {
        ResultSet read = ms.read("target", String.format("handler%d", parent), String.format("`source`=%d", parent));
        Map<Integer, Integer> res = new HashMap<>();
        try {
            while (read.next()) {
                try {
                    if (target.contains(read.getInt(1)))
                        res.put(parent, read.getInt(1));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
        return res;
    }

    @Override
    public List<Pair<Integer, Integer>> getIndirectFriendship(int parent, Set<Integer> target) {
        Map<Integer, Boolean> finded = target.stream().filter(i -> i != parent).collect(Collectors.toMap(Integer::intValue, i -> false));
        List<Pair<Integer, Integer>> res = new ArrayList<>();

        int offset = 0, offset1 = 0, count = 40000;
        Set<Integer> list = new HashSet<>(40000);
        while (ms.countAll(String.format("handler%1$d", parent), "") > offset1) {
            offset1 += count;

            ResultSet rs = ms.read("target", String.format("handler%1$d LIMIT %2$d,%3$d", parent, offset1, count), "");
            try {
                while (rs.next()) {
                    list.add(rs.getInt(1));
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            for (Map.Entry<Integer, Boolean> targetEntry : finded.entrySet()) {
                if (targetEntry.getValue()) continue;
                int targetId = targetEntry.getKey();
                while (ms.countAll(String.format("handler%1$d", targetId), "") > offset) {
                    ResultSet rs2 = ms.read("source, target", String.format("handler%1$d LIMIT %2$d,%3$d", targetId, offset, count), "");
                    offset += count;
                    try {
                        while (rs2.next()) {
                            if (list.contains(rs2.getInt(2))) {
                                putFriendship(parent, targetId, res, rs2.getInt(2), rs2.getInt(1));
                                putFriendship(targetId, parent, res, rs2.getInt(1), rs2.getInt(2));
                                targetEntry.setValue(true);
                                break;
                            }
                        }
                        rs2.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    if (targetEntry.getValue()) break;
                }
                offset = 0;
            }
            list.clear();
        }

        return res;
    }

    private void putFriendship(int parent, int target, List<Pair<Integer, Integer>> res, int src, int targ) {
        try {
            res.add(new Pair<>(targ, src));
            System.out.printf("Найдена косвенная связь [%d - %d] %d - %d%n", parent, target, targ, src);
            if (targ == parent) {
                return;
            }
            ResultSet source = ms.read("source, target", String.format("handler%d", parent), String.format("`target`=%d LIMIT 0,1", src));
            if (source.next()) {
                 putFriendship(parent, target, res, source.getInt(1), source.getInt(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void userScaned(int userId, int parent) {
        ms.update("queue", Queue.toMap(userId, parent, true), String.format("`source`=%d", userId));
    }

    @Override
    public boolean containsAllHandler() {
        return ms.countAll(TARGETS_TABLE_NAME, "`hasHandlers`=0") > 0;
    }
}
