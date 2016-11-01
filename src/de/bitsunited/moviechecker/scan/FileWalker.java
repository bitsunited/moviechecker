package de.bitsunited.moviechecker.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import de.bitsunited.moviechecker.Database;
import de.bitsunited.moviechecker.scan.Result.ResultState;

public class FileWalker extends RecursiveTask<Result> {

    private static final long serialVersionUID = 1L;

    private final Path directory;

    private final Database database;

    public FileWalker(Path directory, Database database) {
        this.directory = Objects.requireNonNull(directory);
        this.database = Objects.requireNonNull(database);
    }

    @Override
    protected Result compute() {
        try {
            return Result.ofTasks(Files.list(directory).filter(f -> Files.isRegularFile(f)).map(f -> handle(f))
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
            return ResultState.EXCEPTION.getResult();
        }
    }

    private RecursiveTask<Result> handle(Path file) {
        FileCheck check = new FileCheck(file, database);
        check.fork();
        return check;
    }

}
