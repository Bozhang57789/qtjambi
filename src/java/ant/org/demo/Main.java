package org.demo;

import io.qt.widgets.QApplication;
import io.qt.widgets.QMessageBox;

public class Main {
    public static void main(String[] args) {
        /**
         * 由于我安装过程中，自己下的安装包和您给的安装包都没有6.6.0 和文档中说明相近的版本只有6.6.2
         * 所以就用的6.6.2的jar，我电脑是可以运行成功的 不知道6.6.0会不会有影响
         * 本地环境变量有点问题，我就指定了一个Qt6Core.dll 环境
         */
        System.setProperty("java.library.path","C:\\ceshi\\Qt\\6.6.2\\msvc2019_64\\bin");
        System.out.println(System.getProperty("java.library.path"));
        QApplication.initialize(args);
        QMessageBox.warning(null, "QtJambi_Demo", "I love Test Ky!\n");
        QApplication.shutdown();
    }
}