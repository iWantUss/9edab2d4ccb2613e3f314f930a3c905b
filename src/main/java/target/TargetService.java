package target;

import storage.StorageService;

import java.sql.SQLException;
import java.util.Set;

public interface TargetService {

    /**
     * Устанавливает цели между которыми необходимо найти связь
     */
    void setTargets(Set<Integer> targets, StorageService storageService);

    /**
     * @return найдены ли все короткие связи
     */
    void hasHandlers(int updatedTarget) throws SQLException, NullPointerException;


}
