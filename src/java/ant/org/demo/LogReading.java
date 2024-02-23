package org.demo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogReading {

    public static void main(String[] args) {
        String logFilePath = "D:\\testlog.txt";

        // 1. 提取所有http(s)协议的url
        Set<String> urls = new HashSet<>();
        Pattern urlPattern = Pattern.compile("(https?://\\S+)");
        int totalUrls = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = urlPattern.matcher(line);
                while (matcher.find()) {
                    urls.add(matcher.group());
                    totalUrls++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 输出去重前的url总数
        System.out.println("输出去重前的url总数: " + totalUrls);

        // 输出去重后的url总数和列举所有url
        System.out.println("输出去重前的url总数: " + urls.size());
        System.out.println("去重前所有url:");
        for (String url : urls) {
            System.out.println(url);
        }

        // 2. 提取所有符合IPv4地址特征的信息
        Set<String> ipAddresses = new HashSet<>();
        Pattern ipPattern = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = ipPattern.matcher(line);
                while (matcher.find()) {
                    ipAddresses.add(matcher.group());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 输出去重后的IPv4地址总数和列举所有IPv4地址
        System.out.println("输出去重后的IPv4地址总数: " + ipAddresses.size());
        System.out.println("输出去重后的列举所有IPv4地址:");
        for (String ip : ipAddresses) {
            System.out.println(ip);
        }

        // 3. 找到所有进度信息
        Pattern progressPattern = Pattern.compile("\\((\\d+)/(\\d+)\\)|\\[(\\d+)/(\\d+)\\]");
        System.out.println("找到所有进度信息:");
        try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = progressPattern.matcher(line);
                while (matcher.find()) {
                    System.out.println(matcher.group());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
