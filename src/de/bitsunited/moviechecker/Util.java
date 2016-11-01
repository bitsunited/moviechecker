package de.bitsunited.moviechecker;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Collectors;

public class Util {

    private Util() {
        // Hide public constructor
    }

    public static String print(Duration duration) {
        Objects.requireNonNull(duration);

        StringBuilder sb = new StringBuilder();
        sb.append(duration.getSeconds());
        sb.append('.');
        long millis = duration.getNano() / 1000000;
        if (millis < 100) {
            sb.append('0');
        }
        if (millis < 10) {
            sb.append('0');
        }
        sb.append(millis);
        sb.append("sec");

        return sb.toString();
    }

    public static String printFileSize(long byteSize) {
        if (byteSize < 10000) {
            return byteSize + " B";
        }

        long kiloByteSize = byteSize / 1000;
        if (kiloByteSize < 10000) {
            return kiloByteSize + " kB";
        }

        long megaByteSize = kiloByteSize / 1000;
        if (megaByteSize < 10000) {
            return megaByteSize + " MB";
        }

        long gigaByteSize = megaByteSize / 1000;
        return gigaByteSize + " GB";
    }

    public static String read(InputStream inputStream) {
        Objects.requireNonNull(inputStream);

        BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
        return buffer.lines().collect(Collectors.joining("\n"));
    }

    public static boolean hasFileExtension(Path path, String extension) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(extension);

        return path.getFileName().toString().endsWith("." + extension);
    }

    public static Path replaceFileExtension(Path path, String newExtension) {
        String fileNameString = path.getFileName().toString();
        int i = fileNameString.lastIndexOf('.');
        if (i > 0) {
            String newFileNameString = fileNameString.substring(0, i + 1) + newExtension;
            return path.resolveSibling(newFileNameString);
        }
        return path.resolveSibling(fileNameString + "." + newExtension);
    }

    public static boolean hasParameter(String[] args, String... optionVariants) {
        Objects.requireNonNull(args);
        Objects.requireNonNull(optionVariants);

        for (String option : optionVariants) {
            for (int i = 0; i < args.length; i++) {
                if (Objects.equals(args[i], option)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String findParameter(String[] args, String... optionVariants) {
        Objects.requireNonNull(args);
        Objects.requireNonNull(optionVariants);

        for (String option : optionVariants) {
            for (int i = 0; i < args.length; i++) {
                if (Objects.equals(args[i], option)) {
                    i++;
                    if (i < args.length) {
                        return args[i];
                    }
                    return null;
                }
            }
        }
        return null;
    }
}
