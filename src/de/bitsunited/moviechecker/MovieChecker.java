package de.bitsunited.moviechecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import de.bitsunited.moviechecker.scan.FileCheck;
import de.bitsunited.moviechecker.scan.FolderWalker;
import de.bitsunited.moviechecker.scan.Result;
import de.bitsunited.moviechecker.scan.Result.ResultState;

public class MovieChecker {

    public static void main(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            printHelp();
            return;
        }
        Instant startTime = Instant.now();

        String mode = args[0];
        if ("scan".equalsIgnoreCase(mode)) {
            scanMode(args);
        } else if ("convert".equalsIgnoreCase(mode)) {
            convertMode(args);
        } else if ("find".equalsIgnoreCase(mode)) {
            findMode(args);
        } else if ("list".equalsIgnoreCase(mode)) {
            listMode(args);
        } else {
            printHelp();
            return;
        }

        System.out.println("Runtime: " + Util.print(Duration.between(startTime, Instant.now())));
    }

    private static void convertMode(String[] args) throws IOException {
        Database database = loadDatabase(args, true);

        String encoding = Util.findParameter(args, "-e", "--encoding");
        if (encoding == null) {
            System.err.println("Missing video encoding argument.");
            return;
        }

        Path logPath = getLogPath(args);

        String maxValue = Util.findParameter(args, "-m", "--max");
        Integer count = null;
        if (maxValue != null) {
            count = Integer.parseInt(maxValue);
        }

        List<Path> pathList = database.getPathList();
        List<Path> filteredList = new LinkedList<>();
        for (Path path : pathList) {
            String errOut = database.getEntryErrOut(path);
            if (errOut != null && errOut.contains("Video: " + encoding)) {
                filteredList.add(path);
            }
        }

        if (count != null && count < filteredList.size()) {
            filteredList = filteredList.subList(0, count);
        }

        for (Path path : filteredList) {
            convert(path, database, logPath);
        }
    }

    private static Path getLogPath(String[] args) throws IOException {
        String logfileName = Util.findParameter(args, "-l", "--logfile");
        if (logfileName == null) {
            logfileName = System.getProperty("user.home") + "/.moviechecker/convert.log";
        }
        Path logPath = Paths.get(logfileName);
        logPath = logPath.toAbsolutePath();

        if (!Files.isDirectory(logPath.getParent())) {
            Files.createDirectories(logPath.getParent());
        }
        return logPath;
    }

    private static void findMode(String[] args) throws IOException {
        String encoding = Util.findParameter(args, "-e", "--encoding");
        if (encoding == null) {
            System.err.println("Missing video encoding argument.");
            return;
        }

        Database database = loadDatabase(args, true);

        List<Path> pathList = database.getPathList();
        int counter = 0;
        for (Path path : pathList) {
            String errOut = database.getEntryErrOut(path);
            if (errOut != null && errOut.contains("Video: " + encoding)) {
                long size = Files.size(path);
                System.out.println(path + " [" + Util.printFileSize(size) + "]");
                counter++;
            }
        }
        System.out.println("Found: " + counter);
    }

    private static void listMode(String[] args) throws IOException {
        Database database = loadDatabase(args, true);

        Set<String> encodingSet = new HashSet<>();

        String pattern = "Video: ";

        List<Path> pathList = database.getPathList();
        for (Path path : pathList) {
            String errOut = database.getEntryErrOut(path);
            if (errOut != null && errOut.contains(pattern)) {
                int i = errOut.indexOf(pattern);
                i += pattern.length();
                StringBuilder sb = new StringBuilder();
                char c = errOut.charAt(i);
                while (!Character.isWhitespace(c) && c != ',') {
                    sb.append(c);
                    i++;
                    c = errOut.charAt(i);
                }
                encodingSet.add(sb.toString());
            }
        }

        List<String> encodingList = new ArrayList<String>(encodingSet);
        Collections.sort(encodingList);
        for (String encoding : encodingList) {
            System.out.println("  " + encoding);
        }
        System.out.println("Found: " + encodingList.size());
    }

    private static void convert(Path inputPath, Database database, Path logPath) throws IOException {
        System.out.println(inputPath + ": Start encoding...");

        Instant startTime = Instant.now();

        Path newPath = inputPath.resolveSibling(".temp." + inputPath.getFileName());
        if (!Util.hasFileExtension(newPath, "mkv")) {
            newPath = Util.replaceFileExtension(newPath, "mkv");
        }

        Path oldPath = inputPath.resolveSibling(inputPath.getFileName() + ".old");
        Path finalPath = inputPath;
        if (!Util.hasFileExtension(finalPath, "mkv")) {
            finalPath = Util.replaceFileExtension(finalPath, "mkv");
        }
        
        if (Files.isRegularFile(newPath)) {
            Files.delete(newPath);
        }

        String[] ffmpegCommand = new String[] { "ffmpeg", "-hide_banner", "-loglevel", "quiet", "-i", inputPath.toString(), "-crf", "20", "-map", "0",
                "-acodec", "copy", "-scodec", "copy", "-c:v", "libx264", "-threads", "0", "-preset", "veryslow", newPath.toString() };
        Process process = Runtime.getRuntime().exec(ffmpegCommand);

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long newSize = Files.size(newPath);
        long oldSize = Files.size(inputPath);
        long percent100 = newSize * 10000 / oldSize;
        String percent = (percent100 / 100.0) + "%";

        String line = inputPath + ": " + Util.printFileSize(oldSize) + " => " + Util.printFileSize(newSize) + ". " + percent + " in "
                + Util.print(Duration.between(startTime, Instant.now())) + "\n";
        System.out.println(line);

        Files.write(logPath, line.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        Files.move(inputPath, oldPath);
        Files.move(newPath, finalPath);

        database.load();
        if (!inputPath.equals(finalPath)) {
            database.removeEntry(inputPath);
        }
        FileCheck check = new FileCheck(finalPath, database);
        check.compute();
    }

    private static void scanMode(String[] args) throws IOException {
        String scanPath = Util.findParameter(args, "-s", "--scan");
        if (scanPath == null) {
            System.err.println("Missing scan path argument.");
            return;
        }
        Path directory = Paths.get(scanPath);

        Database database = loadDatabase(args, false);

        FolderWalker baseTask = new FolderWalker(directory, database);

        Result result = ForkJoinPool.commonPool().invoke(baseTask);

        for (ResultState state : ResultState.values()) {
            System.out.println(state.name() + ": " + result.getCount(state));
        }
    }

    private static Database loadDatabase(String[] args, boolean mustExist) throws IOException {
        String databaseName = Util.findParameter(args, "-d", "--database");
        if (databaseName == null) {
            databaseName = System.getProperty("user.home") + "/.moviechecker/database.xml";
        }
        Path databasePath = Paths.get(databaseName);
        databasePath = databasePath.toAbsolutePath();

        if (mustExist && !Files.isRegularFile(databasePath)) {
            throw new IOException("Database file is missing.");
        }
        if (!Files.isDirectory(databasePath.getParent())) {
            Files.createDirectories(databasePath.getParent());
        }

        Database database = new Database(databasePath, null);
        if (Files.isRegularFile(databasePath)) {
            database.load();
        }

        database.setAutoSave(true);
        
        List<Path> pathList = database.getPathList();
        for (Path path : pathList) {
            if (!Files.isRegularFile(path)) {
                database.removeEntry(path);
            }
        }
        
        return database;
    }

    private static void printHelp() {
        System.out.println("Command: scan [-d | --database <databasefile>] (-s | --scan <directory>)");
        System.out.println("Command: list [-d | --database <databasefile>]");
        System.out.println("Command: find [-d | --database <databasefile>] (-e | --encoding <videoencoding>)");
        System.out
                .println("Command: convert [-d | --database <databasefile>] [-l | --logfile <logfile>] [-m | --max <max-count>] (-e | --encoding <videoencoding>)");
    }
}
