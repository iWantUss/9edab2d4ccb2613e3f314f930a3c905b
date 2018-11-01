import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import storage.StorageService;
import storage.StorageServiceImpl;
import target.TargetService;
import target.TargetServiceImpl;
import tools.mysql.Config;
import watcher.WatcherService;
import watcher.WatcherServicePool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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

    public static void main(String[] args) {
        if (args.length < 3) {
            throw new Error("Неверный формат команды [host:port где находится БД] [название базы данных] [режим] ...");
        }
        Config.createInstance(args[0], args[1]);//.createInstance("localhost:3306", "vk");
        MODE mode = MODE.valueOf(args[2]);//MODE.EXPORT;
        Main app = new Main();

        switch (mode) {
            case INIT:
                app.storage.init();
                app.init().forEach(par -> {
                    //Добавление целевых пользователей в очередь для обхода
                    app.storage.addInQueue(Collections.singleton(par), par);
                    //Создание таблиц
                    app.storage.createTargetHandlers(par);
                });
                break;
            case EXPORT:
                if (args.length != 5) {
                    throw new Error("Неверный формат команды EXPORT [путь к файлу] [название листа]");
                }
                String path = args[3];
                File file;
                if (path == null) {
                    throw new Error("Не указан путь к файлу");
                } else {
                    file = new File(path);
                    if (file.exists()) {
                        if (!file.canRead() || !file.canWrite()) {
                            throw new Error("Нет прав на чтение и запись файла");
                        }
                    } else {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            throw new Error("Не верно указан путь к файлу.");
                        }
                    }
                }
                String sheetName = args[4];
                if (sheetName == null) {
                    throw new Error(String.format("Не указано название листа в файле '%s'", path));
                }
                Set<Integer> init = app.init();
                Workbook book = null;
                Sheet sheet = null;
                try {
                    FileInputStream fis = new FileInputStream(file);
                    book = new XSSFWorkbook(fis);
                    sheet = book.getSheet(sheetName);
                    if (sheet == null) {
                        sheet = book.createSheet(sheetName);
                        Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());
                        row.createCell(0).setCellValue("source");
                        row.createCell(1).setCellValue("target");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (Integer i : app.init()) {
                    app.exportFriendship(i, sheet);
                }
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                    book.write(fileOutputStream);
                    book.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case PARSING:
                if (args.length != 5) {
                    throw new Error("Неверный формат команды [host:port где находится БД] [Название базы данных] PARSING [количество машин] [id машины]");
                }
                int count;
                try {
                    count = Integer.valueOf(args[3]);
                } catch (NumberFormatException e) {
                    throw new Error("Неверно указано количество машин");
                }
                int segment;//id машины
                try {
                    segment = Integer.valueOf(args[4]);
                    if (segment >= count) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    throw new Error(String.format("Неверно указан id машины возможные значения [0-%d]", count - 1));
                }
                int shift = (int) Math.ceil((double) app.init().size() / count);
                app.init().stream().skip(shift * segment).limit(segment).parallel().forEach(par -> {
                    while (!app.allTargetsFinded(par)) ;
                });
                break;
            default:
                throw new Error(String.format("Unknown mode! Available modes: %s", Arrays.toString(MODE.values())));
        }
        System.out.println("[КОНЕЦ ПРОГАРММЫ]");
    }

    public void exportFriendship(int parent, Sheet sheet) {

        this.storage.getDirectFriendship(parent, init()).forEach((key, value) -> {
            Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());
            putEdge(key, value, row);
            System.out.printf("Найдена прямая связь %d - %d%n", key, value);
        });
        Set<Integer> init = this.storage.getFindedFriendship();
        this.storage.getIndirectFriendship(parent, init).forEach(pair -> {
            Row row = sheet.createRow(sheet.getPhysicalNumberOfRows());
            putEdge(pair.getKey(), pair.getValue(), row);
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
    public boolean allTargetsFinded(int parent) {
        int nextUser = storage.getNextUser(parent);
        Set<Integer> friends = watcher.getFriendsAndFollowers(nextUser);
        storage.addHandlers(parent, nextUser, friends);
        storage.addInQueue(friends, parent);
        return targetService.hasFriendship(parent);
    }

    public Set<Integer> init() {
        if (targets == null) {
            targets = Stream.of(
                    0/*СПИСОК ID пользователей, между которыми нужно найти связь*/
            ).collect(Collectors.toSet());
        }
        return targets;
    }

    enum MODE {
        INIT, //Создает БД на мастере
        PARSING,//Запускает программу в режиме парсинга
        EXPORT //Экспортирует результат в xlsx
    }
}
