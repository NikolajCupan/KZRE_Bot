package org.utility;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
    public static final Path RESOURCES_PATH = Paths.get("src", "main", "resources");
    public static final String DEVELOPMENT_ARGUMENT = "development";
    public static final String DEVELOPMENT_SERVER_ID = "949756585616474152";

    public static final int TAG_MAX_LENGTH = 100;
    public static final double LEVENSHTEIN_DISTANCE_WARNING_THRESHOLD = 0.3;

    public static final int CONFIRMATION_ATTEMPTS = 5;
}
