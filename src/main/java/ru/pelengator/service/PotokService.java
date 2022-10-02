package ru.pelengator.service;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pelengator.Controller;
import ru.pelengator.PotokController;

import java.util.*;

import static ru.pelengator.API.utils.Utils.*;

/**
 * Сервис расчета потока.
 */
public class PotokService extends Service<Void> {
    /**
     * Логгер.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PotokService.class);

    private PotokController controller;
    private Controller mainController;

    private double plank0;
    private double plank1;
    private double T0;
    private double T1;
    private double epsilin0;
    private double epsilin1;
    private double areaACHT0;
    private double areaACHT1;
    private double L0;
    private double L1;
    private double betta0;
    private double betta1;
    private double areaFPU0;
    private double areaFPU1;
    private double potok0;
    private double potok1;
    private double potok;
    private double exposure;


    public PotokService(PotokController controller) {
        this.controller = controller;
        this.mainController = controller.getMainController();
    }

    @Override
    protected Task<Void> createTask() {

        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                LOG.trace("Start task");
                updateMessage("Старт Сервиса");
                updateProgress(0.0, 1);
                updateMessage("Инициализация данных");
                initParams();
                updateProgress(0.4, 1);
                updateMessage("Расчет");
                math();
                updateProgress(0.7, 1);
                updateMessage("Сохранение");
                saveData();
                updateProgress(0.8, 1);
                updateMessage("Стоп сервис");
                updateMessage("");
                updateProgress(1, 1);
                return null;
            }

        };
    }

    /**
     * Инициализация параметров.
     */
    private void initParams() {
        LOG.trace("Init");
        plank0 = mainController.getParams().getPlank0();
        plank1 = mainController.getParams().getPlank1();
        T0 = mainController.getParams().getTemp0();
        T1 = mainController.getParams().getTemp1();
        epsilin0 = mainController.getParams().getEpsilin0();
        epsilin1 = mainController.getParams().getEpsilin1();
        areaACHT0 = mainController.getParams().getAreaACHT0();
        areaACHT1 = mainController.getParams().getAreaACHT1();
        L0 = mainController.getParams().getRasstACHTfpu0();
        L1 = mainController.getParams().getRasstACHTfpu1();
        betta0 = mainController.getParams().getBetta0();
        betta1 = mainController.getParams().getBetta1();
        areaFPU0 = mainController.getParams().getAreaFPU0();
        areaFPU1 = mainController.getParams().getAreaFPU1();
        exposure = mainController.getParams().getExposure();
    }

    /**
     * Расчетная функция.
     */
    private void math() {
        potok0 = potok(T0, plank0, epsilin0, areaACHT0, L0, betta0, areaFPU0);
        potok1 = potok(T1, plank1, epsilin1, areaACHT1, L1, betta1, areaFPU1);
        potok = potok1 - potok0;
        exposure = exposure(potok,areaFPU0);
        printPotok(controller, potok0, potok1, potok, exposure);

    }

    /**
     * Печать потока.
     *
     * @param controller
     * @param potok0     поток при меньшей засветке.
     * @param potok1     поток при большей засветке.
     * @param potok      итоговый поток.
     * @param exposure   итоговая одлученность.
     */
    private void printPotok(PotokController controller, double potok0, double potok1, double potok, double exposure) {
        String fpotok0 = String.format(Locale.CANADA, "%.3e", potok0).toUpperCase();
        String fpotok1 = String.format(Locale.CANADA, "%.3e", potok1).toUpperCase();
        String fpotok = String.format(Locale.CANADA, "%.3e", potok).toUpperCase();
        String fexposure = String.format(Locale.CANADA, "%.3e", exposure).toUpperCase();
        Platform.runLater(() -> {
            controller.getLab_potok0().setText(fpotok0);
            controller.getLab_potok1().setText(fpotok1);
            controller.getLab_potok().setText(fpotok);
            controller.getLab_exposure().setText(fexposure);
        });

    }

    /**
     * Сохраняем полученные данные
     */
    private void saveData() {
        LOG.trace("Saving");
        mainController.getParams().setPotok(potok);
        mainController.getParams().setPotok0(potok0);
        mainController.getParams().setPotok1(potok1);
        mainController.getParams().setExposure(exposure);
        mainController.getSelExp().setParams(mainController.getParams());
        mainController.save();
    }


    @Override
    protected void succeeded() {
        super.succeeded();
        controller.getMainController().save();
        Platform.runLater(() -> {
            controller.setButtonsDisable(false, false);
            controller.getMainController().getBtnGetData().setDisable(false);
            controller.getMainController().getBtnPotok().setStyle("-fx-background-color: green");
        });
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        Platform.runLater(() -> {
        controller.getMainController().getBtnPotok().setStyle("-fx-background-color: red");
        });
    }
}
