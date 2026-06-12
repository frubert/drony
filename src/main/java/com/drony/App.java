package com.drony;

import com.drony.tester.TesterMainGUIMode;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {

        try {
            TesterMainGUIMode.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
