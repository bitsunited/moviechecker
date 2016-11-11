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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.RecursiveTask;

public class Result {
    public enum ResultState {
        NEW, UPDATED, UNCHANGED, EXCEPTION, OLD_FILE;

        public Result getResult() {
            return Result.of(this);
        }
    }

    public static Result of(ResultState resultState) {
        Objects.requireNonNull(resultState);

        long[] newArray = new long[ResultState.values().length];
        newArray[resultState.ordinal()] = 1;
        return new Result(newArray);
    }

    public static Result of(Collection<Result> resultList) {
        Objects.requireNonNull(resultList);

        long[] newArray = new long[ResultState.values().length];
        for (Result result : resultList) {
            Objects.requireNonNull(result);
            for (int i = 0; i < newArray.length; i++) {
                newArray[i] += result.stateCounterArray[i];
            }
        }
        return new Result(newArray);
    }

    public static Result ofTasks(Collection<? extends RecursiveTask<Result>> taskCollection) {
        List<Result> list = new LinkedList<>();
        if (taskCollection != null) {
            for (RecursiveTask<Result> t : taskCollection) {
                if (t != null) {
                    Result result = t.join();
                    if (result != null) {
                        list.add(result);
                    }
                }
            }
        }
        return of(list);
    }

    private final long[] stateCounterArray;

    private Result(long[] stateCounterArray) {
        this.stateCounterArray = stateCounterArray;
    }

    public long getCount(ResultState resultState) {
        Objects.requireNonNull(resultState);
        return stateCounterArray[resultState.ordinal()];
    }
}
