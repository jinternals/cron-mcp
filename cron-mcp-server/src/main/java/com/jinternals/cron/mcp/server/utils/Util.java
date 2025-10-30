package com.jinternals.cron.mcp.server.utils;

import com.jinternals.cron.mcp.server.constants.OS;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class Util {

    public static OS os() {
        String n = System.getProperty("os.name", "").toLowerCase();
        if (n.contains("win")) return OS.WINDOWS;
        if (n.contains("mac")) return OS.MAC;
        if (n.contains("nix") || n.contains("nux") || n.contains("aix") || n.contains("bsd")) return OS.LINUX;
        return OS.OTHER;
    }

    public static String tailFile(Path p, int maxLines) throws IOException {
        var lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        int from = Math.max(0, lines.size() - maxLines);
        return String.join("\n", lines.subList(from, lines.size()));
    }

    public static String run(List<String> cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0 && out.isBlank()) {
            return "(command failed: " + String.join(" ", cmd) + " -> " + code + ")";
        }
        return out;
    }


    public String run(List<String> cmd, String stdin) throws Exception {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        if (stdin != null) try (var os = p.getOutputStream()) {
            os.write(stdin.getBytes(StandardCharsets.UTF_8));
        }
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        if (code != 0) throw new IOException(String.join(" ", cmd) + " -> " + code + ": " + out);
        return out;
    }


    public static String readAllFilesUnder(Path dir, String headerPrefix) throws IOException {
        if (!Files.isDirectory(dir)) return "";
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*")) {
            StringBuilder sb = new StringBuilder();
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    sb.append(headerPrefix).append(p.getFileName()).append("\n")
                            .append(Files.readString(p)).append("\n\n");
                }
            }
            return sb.toString();
        }
    }

    public static String tailString(String src, int maxLines) {
        if (src == null || src.isBlank()) return "";
        var lines = src.lines().collect(Collectors.toList());
        int from = Math.max(0, lines.size() - maxLines);
        return String.join("\n", lines.subList(from, lines.size()));
    }


}
