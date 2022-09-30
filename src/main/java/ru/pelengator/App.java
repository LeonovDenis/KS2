package ru.pelengator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.*;

import static ru.pelengator.API.utils.Utils.*;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Parent root;
    private static Scene scene;
    private static FXMLLoader loader;//окно рабочее
    private static String ftd3XX;

    @Override
    public void start(Stage stage) throws IOException {
        ftd3XX = loadJarDll("FTD3XX.dll");
        //загрузчик первого окна
        loader = new FXMLLoader(getClass().getResource("primaryPage.fxml"));
        root = loader.load();
        scene = new Scene(root);

        String crc32 = calkCRC32();
        stage.setTitle("Стенд ИС2. CRC-32: " + crc32);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();

        /**
         * Обработка закрытия окна
         */
        stage.setOnCloseRequest(t -> {
            Platform.exit();
            Controller controller = loader.getController();
            controller.save();
            System.exit(0);
        });
    }

    /**
     * Обработчик загрузки конфигурации окна из ресурсов
     *
     * @param fxml Имя файла
     * @return Загрузчик окна
     * @throws IOException в случае отсутствия файла описания
     */
    public static FXMLLoader loadFXML(String fxml) throws IOException {

        return new FXMLLoader(App.class.getResource(fxml + ".fxml"));
    }

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            //Обработка ошибок при запуске программы.
            //Вывод в файл и диалоговое окно
            String name = "./Error.txt";//имя файла ошибки
            File file = new File(name);
            try (FileOutputStream fl = new FileOutputStream(file);
                 PrintWriter pw = new PrintWriter(fl);) {
                JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + "Смотри file: " + file.getAbsolutePath());
                e.printStackTrace();//вывод в консоль
                e.printStackTrace(pw);//вывод в файл
            } catch (IOException e1) {
                e1.printStackTrace();//отработка отсутствия файла ошибок
            }
        }
    }

    public static String loadFilePath(String name) {
        return loadJarDll(name);
    }

    public static Scene getScene() {
        return scene;
    }

    public static void setScene(Scene scene) {
        App.scene = scene;
    }

    public static String getFtd3XX() {
        return ftd3XX;
    }

    public static FXMLLoader getLoader() {
        return loader;
    }

    public static Parent getRoot() {
        return root;
    }

}