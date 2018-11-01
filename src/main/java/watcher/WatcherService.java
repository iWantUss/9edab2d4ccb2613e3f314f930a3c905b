package watcher;

import java.util.Set;

/**
 * Предназначен для получения информации из vk
 */
public interface WatcherService {
    /**
     * @param user контекстный пользователь
     * @return список идентификаторов пользователей
     */
    Set<Integer> getFriends(int user);

    /**
     * Получает список всех друзей и подписчиков
     *
     * @param parent
     * @return список идентификаторов пользователей
     */
    Set<Integer> getFriendsAndFollowers(int parent);

}
