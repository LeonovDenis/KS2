package ru.pelengator.API;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DetectorDriverUtils {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DetectorDriverUtils.class);

    private DetectorDriverUtils() {
    }

    /**
     * Найти драйвер детектора.
     * Сканирует пакеты для поиска драйверов, указанных в аргументе.
     *
     * @param names Массив имен драйверов для поиска
     * @return драйвер, если он найден или выдает исключение
     * @throw DetectorException
     */
    protected static DetectorDriver findDriver(List<String> names, List<Class<?>> classes) {

        for (String name : names) {

            LOG.info("Searching driver {}", name);

            Class<?> clazz = null;

            for (Class<?> c : classes) {
                if (c.getCanonicalName().equals(name)) {
                    clazz = c;
                    break;
                }
            }

            if (clazz == null) {
                try {
                    clazz = Class.forName(name);
                } catch (ClassNotFoundException e) {
                    LOG.trace("Class not found {}, fall thru", name);
                }
            }

            if (clazz == null) {
                LOG.debug("Driver {} not found", name);
                continue;
            }

            LOG.info("Detector driver {} has been found", name);

            try {
                return (DetectorDriver) clazz.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    /** Сканирует все классы, доступные из контекстного загрузчика классов, которые принадлежат
     * к данному пакету и подпакетам.
     *
     * @param pkgname Базовый пакет
     * @param flat сканирует только один уровень пакета, не погружаясь в подкаталоги
     * @return Классы
     * @throws ClassNotFoundException
     * @throws IOException
     */

    protected static Class<?>[] getClasses(String pkgname, boolean flat) {

        List<File> dirs = new ArrayList<File>();
        List<Class<?>> classes = new ArrayList<Class<?>>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = pkgname.replace('.', '/');

        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(path);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read path " + path, e);
        }

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }

        for (File directory : dirs) {
            try {
                classes.addAll(findClasses(directory, pkgname, flat));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found", e);
            }
        }

        return classes.toArray(new Class<?>[classes.size()]);
    }

    /**
     * Рекурсивный метод, используемый для поиска всех классов в данном каталоге и
     * подкаталогах.
     *
     * @param dir     базовый каталог
     * @param pkgname Имя пакетадля классов, найденных внутри базового каталога
     * @param flat    сканирует только один уровень пакета, не погружаясь в подкаталоги
     * @return Список классов
     * @throws ClassNotFoundException
     */

    private static List<Class<?>> findClasses(File dir, String pkgname, boolean flat) throws ClassNotFoundException {

        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!dir.exists()) {
            return classes;
        }

        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory() && !flat) {
                classes.addAll(findClasses(file, pkgname + "." + file.getName(), flat));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(pkgname + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }

        return classes;
    }


}
