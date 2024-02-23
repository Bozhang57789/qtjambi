package org.demo;

import io.qt.widgets.QApplication;
import io.qt.widgets.QMessageBox;

public class Main {
    public static void main(String[] args) {
        System.setProperty("java.library.path","C:\\ceshi\\Qt\\6.6.2\\msvc2019_64\\bin");
        System.out.println(System.getProperty("java.library.path"));
        QApplication.initialize(args);
        QMessageBox.warning(null, "QtJambi_Demo", "I love Ky!\n");
        QApplication.shutdown();
    }
}