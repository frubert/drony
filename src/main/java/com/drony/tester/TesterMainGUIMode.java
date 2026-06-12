/*
 * Copyright (c) 2017 Dukascopy (Suisse) SA. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 * 
 * Neither the name of Dukascopy (Suisse) SA or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. DUKASCOPY (SUISSE) SA ("DUKASCOPY")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL DUKASCOPY OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF DUKASCOPY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package com.drony.tester;

import com.drony.DronyV042;
import com.drony.config.DronyConfig;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.system.ISystemListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * This small program demonstrates how to initialize Dukascopy tester and start a strategy in GUI mode
 */
@SuppressWarnings("serial")
public class TesterMainGUIMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(TesterMainGUIMode.class);

    private static final DronyConfig CONFIG = DronyConfig.get();

    private static String jnlpUrl = CONFIG.jnlpUrl();
    private static String userName = CONFIG.userName();
    private static String password = CONFIG.password();
    private static String reportFileName = CONFIG.reportFileName();
    private static Instrument instrument = Instrument.valueOf(CONFIG.instrument());

    private static GUIModeChartControls roboWindow;
    private static RoboClientRunner testerClientRunner;

    public static void start() {
        testerClientRunner = new RoboClientRunner();
        roboWindow = new GUIModeChartControls(getTesterThread());
        roboWindow.showChartFrame();
    }

    public static Thread getTesterThread() {

        Runnable r = () -> {
            DronyV042 strategyWithJar = new DronyV042();
            strategyWithJar.fileParam = CONFIG.paramFile();
            strategyWithJar.fileResult = CONFIG.resultDir();
            strategyWithJar.outPutVerboso = CONFIG.verbose();
            try {
                testerClientRunner.start(
                        jnlpUrl,
                        userName,
                        password,
                        instrument,
                        roboWindow,
                        roboWindow,
                        getsystemListener(),
                        strategyWithJar);

            } catch (Exception e2) {
                LOGGER.error(e2.getMessage(), e2);
                e2.printStackTrace();
                roboWindow.resetButtons();
            }
        };
        Thread thread = new Thread(r);
        return thread;
    }


    private static ISystemListener getsystemListener() {
        //set the listener that will receive system events
        return new ISystemListener() {
            @Override
            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
                roboWindow.updateButtons();
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                roboWindow.resetButtons();
                createReport(processId, reportFileName);
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
            }

            @Override
            public void onDisconnect() {
                //tester doesn't disconnect
            }
        };
    }

    private static void createReport(long processId, String originName) {

        final String dotHtml = ".html";
        String fileName = originName;

        boolean fileNameNotFound = true;
        int prog = 1;

        while (fileNameNotFound) {
            if ((new File(fileName + dotHtml)).exists()) {
                fileName = originName + " (" + prog + ")";
                prog++;
            } else {
                fileNameNotFound = false;
            }
        }

        File reportFile = new File(fileName + dotHtml);
        System.out.println("Report in  " + reportFile.getAbsolutePath());

        try {
            testerClientRunner.client.createReport(processId, reportFile);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        if (testerClientRunner.client.getStartedStrategies().size() == 0) {
            //Do nothing
        }
    }

}
