package ru.pelengator.service;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.ParamsController;
import java.io.File;
import java.util.concurrent.*;

import static ru.pelengator.API.utils.Utils.*;
import static ru.pelengator.API.utils.Utils.saveOrder;

/**
 * Сервис сбора данных
 */
public class SaveFilesService extends Service<Void> {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SaveFilesService.class);
    /**
     * Ссылка на контроллер
     */
    private ParamsController controller;

    private ExecutorService service;
    static private int MP;

    {
        MP = Runtime.getRuntime().availableProcessors();
    }


    private File pdfFile;
    static  volatile double  k = 0.4;
    boolean f1=true;
    boolean f2=true;
    boolean f3=true;

    /**
     * Конструктор
     *
     * @param controller
     */
    public SaveFilesService(ParamsController controller, File pdfFile) {
        this.controller = controller;
        this.pdfFile = pdfFile;

    }

    @Override
    protected Task<Void> createTask() {

        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> controller.getpIndicator().setVisible(true));

                service = Executors.newFixedThreadPool(MP);

                updateProgress(0.1, 1);
                ExelSaver exelSaver = new ExelSaver();
                Future<String> future1 = service.submit(exelSaver);
                updateProgress(0.2, 1);
                TxtSaver txtSaver = new TxtSaver();
                Future<String> future2 = service.submit(txtSaver);
                updateProgress(0.3, 1);
                PdfSaver pdfSaver = new PdfSaver();
                Future<String> future3 = service.submit(pdfSaver);
                updateProgress(0.4, 1);
                do {
                    try {
                        String s1 = future1.get(250, TimeUnit.MILLISECONDS);
                       if(f1){
                           k = k+0.1;
                           f1=false;
                       }
                        String s2 = future2.get(250, TimeUnit.MILLISECONDS);
                        if(f2){
                            k = k+0.1;
                            f2=false;
                        }
                        String s3 = future3.get(250, TimeUnit.MILLISECONDS);
                        if(f3){
                            k = k+0.1;
                            f3=false;
                        }
                    } catch (TimeoutException e) {
                        updateProgress(k, 1);
                    }
                    if(future1.isDone() && future2.isDone() && future3.isDone()){
                        break;
                    }
                } while (true);

                updateProgress(0.8, 1);
                try {
                    updateMessage("Завершение");
                    service.shutdown();
                    service.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOG.error("Экстренное завершение");
                } finally {
                    if (!service.isTerminated()) {
                        LOG.error("Отмена незаконченных задач");
                        service.shutdownNow();
                    }
                }
                updateMessage("");
                updateProgress(1, 1);
                return null;
            }
        };
    }

    /**
     * Создание узла с текстром ошибки
     *
     * @param exception
     * @return
     */
    private Node setErrorMSG(Throwable exception) {
        Pane pane = new Pane();
        pane.getChildren().add(new TextArea(exception.getMessage()));
        return pane;
    }


    /**
     * Вывод окна предупреждения
     *
     * @param s Текст предупреждения
     */
    private void showAlert(String s) {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("WARNING");
                alert.setHeaderText(null);
                alert.setContentText(s);
                Rectangle2D bounds = Screen.getPrimary().getBounds();
                alert.setX(bounds.getMaxX() / 2 - alert.getDialogPane().getWidth() / 2);
                alert.setY(bounds.getMaxY() / 2 + alert.getDialogPane().getHeight() * 2);
                alert.show();
            }
        });
    }


    @Override
    protected void succeeded() {
        super.succeeded();
        Button btnGetData = controller.getBtnSave();
        Platform.runLater(() -> {
            btnGetData.setStyle("-fx-background-color: green");
            controller.getpIndicator().setVisible(false);
        });
    }

    @Override
    protected void cancelled() {
        super.cancelled();
    }

    @Override
    protected void failed() {
        super.failed();
        LOG.error("Failed!");
        Button btnGetData = controller.getBtnSave();
        Platform.runLater(() -> {
            btnGetData.setStyle("-fx-background-color: red");
            controller.getLab_status().textProperty().unbind();
            controller.getLab_status().textProperty().setValue("Ошибка при записи файла");
            controller.getpIndicator().setVisible(false);
        });
    }

    @Override
    public boolean cancel() {
        LOG.error("Canceled!");
        Button btnGetData = controller.getBtnSave();
        Platform.runLater(() -> {
            btnGetData.setStyle("-fx-background-color: red");
            controller.getLab_status().textProperty().unbind();
            controller.getLab_status().textProperty().setValue("Отмена записи файла");
            controller.getpIndicator().setVisible(false);
        });
        return super.cancel();
    }

    class ExelSaver implements Callable<String> {
        @Override
        public String call() throws Exception {

            if (controller.getController().getParams().isTbExel()) {
                saveExel(controller.getService(), pdfFile);
            } else {
                return "Exel файл Не записан";
            }
            return "Exel файл записан";
        }
    }

    class TxtSaver implements Callable<String> {
        @Override
        public String call() throws Exception {
            if (controller.getController().getSelExp().isPrintBpList()
                    && controller.getController().getParams().isTbTxt()) {
                saveTxt(controller.getController().getSelExp(), pdfFile);
            } else {
                return "Txt файл Не записан";
            }
            return "Txt файл записан";
        }
    }

    class PdfSaver implements Callable<String> {
        @Override
        public String call() throws Exception {
            boolean b = saveOrder(controller.getController().getSelExp(), controller.getScrlPane(), pdfFile);
            if (!b) {
                failed();
                return "PDF файл записан";
            }
            return "PDF файл записан";
        }
    }


}
