package storage;

import javafx.util.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StorageService {
    /**
     * добавляет цепочки связей в базу данных
     */
    void addHandlers(int target, int source, Collection<Integer> users);

    /**
     * добавляет цели в базу данных
     */
    void addTarget(int source, Set<Integer> targets);

    //TODO: jpa hibernate
    void init();

    /**
     * добавляет цели в базу данных
     */
    void createTargetHandlers(int target);

    /**
     * добавляет пользователй в очередь на обработку
     */
    void addInQueue(Set<Integer> users, int target);

    /**
     * @return пользователя, которого необходимо обработать
     */
    int getNextUser(int parent);

    /**
     * @return есть ли пользователь
     */
    boolean hasNextUser(int parent);

    /**
     * @return Список непроверенных пользователей
     */
    Set<Integer> getNextUsers(int offset, int count);

    ResultSet getDistinctFriends(int parent, int offset, int count);
    ResultSet getFriends(int parent, int offset, int count);
    Set<Integer> getFindedFriendship();
    Map<Integer, Integer> getDirectFriendship(int parent, Set<Integer> target);

    List<Pair<Integer, Integer>> getIndirectFriendship(int parent, Set<Integer> target);

    /**
     * пользователь просканирован
     */
    void userScaned(int userId, int parent);

    /**
     * Найдены ли все связи между пользователями
     * @return
     */
    boolean containsAllHandler();

}
