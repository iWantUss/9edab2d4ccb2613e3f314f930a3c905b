package target;

import storage.StorageService;
import tools.mysql.Config;
import tools.mysql.MysqlService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TargetServiceImpl implements TargetService {

    MysqlService ms = MysqlService.getInstance(Config.getInstance());

    @Override
    public void setTargets(Set<Integer> sources, StorageService storageService) {
        for (Integer source : sources) {
            storageService.addTarget(source, sources);
        }
    }

    public boolean hasFriendship(int updatedTarget) {
        ResultSet rs = ms.read("target", String.format("handler%1$d", updatedTarget), "`fetched`=0");
        List<Integer> list = new ArrayList<>();
        try {
            while (rs.next()) {
                list.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        ResultSet read = ms.read("targetId", "targets", "`hasHandlers`=0 AND `sourceid` = " + updatedTarget);
        try {
            while (read.next()) {
                int targetId = read.getInt(1);
                ResultSet rs2 = ms.read("target", String.format("handler%1$d", targetId), "");
                while (rs2.next()) {
                    if (list.contains(rs2.getInt(1))) {
                        ms.update("targets",
                                Collections.singletonMap("hasHandlers", 1),
                                String.format("(`targetId`=%1$d AND `sourceid`=%2$d) OR (`targetId`=%2$d AND `sourceid`=%1$d)", targetId, updatedTarget));
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        list.clear();
        ms.update(String.format("handler%d", updatedTarget), Collections.singletonMap("fetched", 1), "`fetched`=0");
        int count = ms.countAll("targets", "`hasHandlers`=0 AND `sourceid` = " + updatedTarget);
        return count == 0;
    }
}
