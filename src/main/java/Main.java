import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import storage.StorageService;
import storage.StorageServiceImpl;
import target.TargetService;
import target.TargetServiceImpl;
import watcher.WatcherService;
import watcher.WatcherServicePool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private StorageService storage;
    private TargetService targetService;
    private WatcherService watcher;
    private Set<Integer> targets = null;

    public Main() {
        targetService = new TargetServiceImpl();
        storage = new StorageServiceImpl();
        watcher = WatcherServicePool.getInstance();
    }


    public static void main(String[] args) throws NullPointerException, IOException, InvalidFormatException, SQLException {
        Main app = new Main();
        int parent = 5;


        while (app.storage.containsAllHandler()) {//Программа будет работать до тех пор, пока не будут найдены все связи
            int size = 0;
            switch (parent) {
                case 0://id машины
                    size = app.init().size()/4;//4 - количество машин на которых будет запускаться программа
                    app.init().stream().limit(size).parallel().forEach(par -> {
                        while (!app.searchAllTargets(par)) ;
                    });
                    break;

                case 1://id машины
                    size = app.init().size()/4;//4 - количество машин на которых будет запускаться программа
                    app.init().stream().skip(size).limit(size).parallel().forEach(par -> {
                        while (!app.searchAllTargets(par)) ;
                    });
                    break;
                case 2://id машины
                    size = app.init().size()/4;//4 - количество машин на которых будет запускаться программа
                    app.init().stream().skip(size*2).limit(size).parallel().forEach(par -> {
                        while (!app.searchAllTargets(par)) ;
                    });
                    break;
                case 3://id машины
                    size = app.init().size()/4;//4 - количество машин на которых будет запускаться программа
                    app.init().stream().skip(size*3).parallel().forEach(par -> {
                        while (!app.searchAllTargets(par)) ;
                    });
                    break;
                case 4://Выполняется на мастере кластера, где находится БД
                    app.storage.init();
                    app.init().forEach(par -> {
                        //Добавление целевых пользователей в очередь для обхода
                        app.storage.addInQueue(Collections.singleton(par), par);
                        //Создание таблиц
                        app.storage.createTargetHandlers(par);
                    });
                    break;
                case 5://Выполняется на мастере кластера, где находится БД
                    Set<Integer> init = app.init();
                    File file = new File("result.xlsx");
                    Workbook book = null;
                    Sheet sheet = null;
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        book = new XSSFWorkbook(fis);
                        sheet = book.getSheet("RESULT1");
                        if (sheet == null) {
                            sheet = book.createSheet("RESULT1");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    for (Integer i : app.init()) {
                        try {
                            app.exportFriendship(i, sheet);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                    }
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                        book.write(fileOutputStream);
                        book.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default://добавляет id в список `targets` между которым нужно найти связь
                    app.storage.addInQueue(Collections.singleton(parent), parent);
                    app.storage.createTargetHandlers(parent);
                    break;
            }
        }
        System.out.println("[КОНЕЦ ПРОГАРММЫ]");
    }

    public void exportFriendship(int parent, Sheet sheet) throws SQLException {
        Map<Integer, Integer> friendship = this.storage.getDirectFriendship(parent, init());
        friendship.putAll(this.storage.getIndirectFriendship(parent, init()));
        friendship.forEach((key, value) -> {
            Row row = sheet.createRow(sheet.getPhysicalNumberOfRows() + 1);
            putEdge(key, value, row);
            System.out.printf("%d - %d%n", key, value);
        });
    }

    private void putEdge(int source, int target, Row row) {
        row.createCell(0).setCellValue(source);
        row.createCell(1).setCellValue(target);
    }


    /**
     * Метод подходит для кластерного распараллеливания
     *
     * @param parent
     * @return
     */
    public boolean searchAllTargets(int parent) {
        int nextUser = storage.getNextUser(parent);
        Set<Integer> friends = watcher.getFriendsAndFollowers(nextUser);

        storage.addHandlers(parent, nextUser, friends);

        try {
            targetService.hasHandlers(parent);
        } catch (Exception e) {
            return true;
        }
        storage.addInQueue(friends, parent);
        return false;
    }

    public Set<Integer> init() {
        if (targets == null) {
            targets = Stream.of(
                    0/*СПИСОК ID пользователей, между которыми нужно найти связь*/
            ).collect(Collectors.toSet());
        }
        return targets;
    }
}
