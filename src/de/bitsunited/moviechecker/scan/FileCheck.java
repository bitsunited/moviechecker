package de.bitsunited.moviechecker.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.RecursiveTask;

import de.bitsunited.moviechecker.Database;
import de.bitsunited.moviechecker.Util;
import de.bitsunited.moviechecker.scan.Result.ResultState;

public class FileCheck extends RecursiveTask<Result> {

    private static final long serialVersionUID = 1L;

    private final Path file;

    private final Database database;

    public FileCheck(Path file, Database database) {
        this.file = Objects.requireNonNull(file);
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public Result compute() {
        if (Util.hasFileExtension(file, "old")) {
            return ResultState.OLD_FILE.getResult();
        }
        try {
            Path absolutFile = file.toAbsolutePath();
            Instant fileTime = Files.getLastModifiedTime(absolutFile).toInstant();

            Instant recordedTime = database.getEntryTime(absolutFile);

            if (Objects.equals(fileTime, recordedTime)) {
                return ResultState.UNCHANGED.getResult();
            }

            String[] ffprobeCommand = new String[] { "ffprobe", "-hide_banner", "-i", absolutFile.toString() };
            Process process = Runtime.getRuntime().exec(ffprobeCommand);

            process.waitFor();

            String stdOut = Util.read(process.getInputStream());
            String errOut = Util.read(process.getErrorStream());

            database.addEntry(absolutFile, fileTime, stdOut, errOut);

            return ResultState.UPDATED.getResult();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ResultState.EXCEPTION.getResult();
        }
    }

}
