package ru.pelengator.service;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.ParamsController;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

import static ru.pelengator.API.utils.Utils.*;
import static ru.pelengator.API.utils.Utils.saveOrder;

/**
 * Сервис записи файлов.
 */
public class SaveFilesService extends Service<Void> {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SaveFilesService.class);
    /**
     * Ссылка на контроллер.
     */
    private ParamsController controller;

    private ExecutorService service;
    /**
     * Количество ядер ПК
     */
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
     * Конструктор.
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
                ExelTask exelSaver = new ExelTask();
                Future<String> ExelFuture = service.submit(exelSaver);
                updateProgress(0.2, 1);
                TxtTask txtSaver = new TxtTask();
                Future<String> txtFuture = service.submit(txtSaver);
                updateProgress(0.3, 1);
                PdfTask pdfSaver = new PdfTask();
                Future<String> pdfFuture = service.submit(pdfSaver);
                updateProgress(0.4, 1);
                do {
                    try {
                        ExelFuture.get(250, TimeUnit.MILLISECONDS);
                       if(f1){
                           k = k+0.1;
                           f1=false;
                       }
                        txtFuture.get(250, TimeUnit.MILLISECONDS);
                        if(f2){
                            k = k+0.1;
                            f2=false;
                        }
                        pdfFuture.get(250, TimeUnit.MILLISECONDS);
                        if(f3){
                            k = k+0.1;
                            f3=false;
                        }
                    } catch (TimeoutException e) {
                        updateProgress(k, 1);
                    }
                    if(ExelFuture.isDone() && txtFuture.isDone() && pdfFuture.isDone()){
                        break;
                    }
                } while (true);

                updateProgress(0.8, 1);
                try {
                    updateMessage("Завершение");
                    service.shutdown();
                    service.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOG.error("Error while saving Files");
                } finally {
                    if (!service.isTerminated()) {
                        LOG.error("Tasks not shatdown. Forsing");
                        service.shutdownNow();
                    }
                }
                updateMessage("");
                updateProgress(1, 1);
                return null;
            }
        };
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

    class ExelTask implements Callable<String> {
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

    class TxtTask implements Callable<String> {
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

    class PdfTask implements Callable<String> {
        @Override
        public String call() throws Exception {
            boolean b = saveOrder(controller.getController().getSelExp(), controller.getScrlPane(), pdfFile);
            if (!b) {
                throw new IOException("PDF файл записан");
            //    return "PDF файл записан";
            }
            return "PDF файл записан";
        }
    }

}
