//   Copyright 2016 @bitsunited
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//
package de.bitsunited.moviechecker.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import de.bitsunited.moviechecker.Database;
import de.bitsunited.moviechecker.scan.Result.ResultState;

public class FolderWalker extends RecursiveTask<Result> {

    private static final long serialVersionUID = 1L;

    private final Path directory;

    private final Database database;

    public FolderWalker(Path directory, Database database) {
        this.directory = Objects.requireNonNull(directory);
        this.database = Objects.requireNonNull(database);
    }

    @Override
    protected Result compute() {
        Set<RecursiveTask<Result>> subTaskSet = new HashSet<>();

        FileWalker fileWalker = new FileWalker(directory, database);
        fileWalker.fork();
        subTaskSet.add(fileWalker);

        try {
            List<Path> list = Files.list(directory).collect(Collectors.toList());

            for (Path file : list) {
                if (Files.isDirectory(file)) {
                    FolderWalker walker = new FolderWalker(file, database);
                    walker.fork();
                    subTaskSet.add(walker);
                }
            }

            return Result.ofTasks(subTaskSet);
        } catch (IOException e) {
            e.printStackTrace();
            return ResultState.EXCEPTION.getResult();
        }
    }

}
