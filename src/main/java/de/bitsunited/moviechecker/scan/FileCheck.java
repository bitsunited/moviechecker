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
        if (file.getFileName().toString().startsWith(".temp.")) {
            return null;
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

            return recordedTime == null ? ResultState.NEW.getResult() : ResultState.UPDATED.getResult();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return ResultState.EXCEPTION.getResult();
        }
    }

}
