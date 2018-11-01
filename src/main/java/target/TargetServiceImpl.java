package target;

import tools.mysql.Config;
import tools.mysql.MysqlService;
import storage.StorageService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TargetServiceImpl implements TargetService {

    MysqlService ms = MysqlService.getInstance(new Config() {
        @Override
        public String getDataBase() {
            return "vk";
        }
    });

    @Override
    public void setTargets(Set<Integer> sources, StorageService storageService) {
        for (Integer source : sources) {
            storageService.addTarget(source, sources);
        }
    }

    @Override
    public void hasHandlers(int updatedTarget) throws SQLException, NullPointerException {
        ResultSet rs = ms.read("target", String.format("handler%1$d", updatedTarget), "`fetched`=0");
        List<Integer> list = new ArrayList<>();
        while (rs.next()) {
            list.add(rs.getInt(1));
        }

        ResultSet read = ms.read("targetId", "targets", "`hasHandlers`=0 AND `sourceid` = " + updatedTarget);
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
        list.clear();
        ms.update(String.format("handler%d", updatedTarget), Collections.singletonMap("fetched", 1), "`fetched`=0");
    }
}
