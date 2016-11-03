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
