package watcher;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.friends.responses.GetResponse;
import com.vk.api.sdk.objects.users.responses.GetFollowersResponse;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WatcherServiceImpl implements WatcherService {

    private static final int APP_ID = 0;/*Изменить значние*/
    private static final String CLIENT_SECRET = "";/*Изменить значние*/
    private static final String ACCESS_TOKEN = "";/*Изменить значние*/
    private final int MAX_FOLLOWERS = 1000;
    private VkApiClient vk;
    private ServiceActor actor;
    private ExecutorService VKexecutor = Executors.newFixedThreadPool(1);

    public WatcherServiceImpl() {
        TransportClient transportClient = HttpTransportClient.getInstance();
        vk = new VkApiClient(transportClient);
        actor = new ServiceActor(APP_ID, CLIENT_SECRET, ACCESS_TOKEN);
    }

    @Override
    public Set<Integer> getFriends(int user) {
        return getFriendsStream(user).boxed().collect(Collectors.toSet());
    }

    private IntStream getFriendsStream(int user) {
        GetResponse response;
        try {
            response = VKexecutor.submit(() -> vk.friends().get(actor).userId(user).execute()).get();
        } catch (Exception e) {
            return IntStream.empty();
        }
        return response.getItems().stream().mapToInt(Integer::intValue);
    }

    @Override
    public Set<Integer> getFriendsAndFollowers(int parent) {
        Set<Integer> friends = getFriendsStream(parent).boxed().collect(Collectors.toSet());

        try {
            GetFollowersResponse execute = VKexecutor.submit(() -> vk.users()
                    .getFollowers(actor)
                    .userId(parent).count(MAX_FOLLOWERS).execute()).get();
            int count = execute.getCount();
            friends.addAll(getAllFollowers(parent, count).collect(Collectors.toSet()));
        } catch (Exception e) {
        }
        return friends;
    }

    private Stream<Integer> getAllFollowers(int parent, int count) {
        return IntStream.iterate(0, i -> i + MAX_FOLLOWERS)
                .limit((long) Math.ceil((double) count / (double) MAX_FOLLOWERS))
                .mapToObj(offset -> {
                    try {
                        return vk.users().getFollowers(actor)
                                .count(MAX_FOLLOWERS)
                                .offset(offset)
                                .userId(parent).execute().getItems();
                    } catch (ApiException | ClientException e) {
                        e.printStackTrace();
                    }
                    return Collections.emptyList();
                }).flatMap(i -> ((List<Integer>) i).stream());
    }
}
