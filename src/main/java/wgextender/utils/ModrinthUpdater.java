package wgextender.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO This should go as a separate generic API
public final class ModrinthUpdater implements AutoCloseable {
    public static final String MODRINTH_BASE = "https://api.modrinth.com/";

    private static final String USER_AGENT = "dani-version-checker/1.0";
    private static final String SNAPSHOT_TOKEN = "SNAPSHOT";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Comparator<Artifact> BY_VERSION = Comparator.comparing(Artifact::version);

    private final String urlBaseDefault;
    private final String projectId;
    private final Version currentVersion;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private Result lastResult;

    public ModrinthUpdater(@NotNull String projectId, @NotNull String currentVersion) {
        this(MODRINTH_BASE, projectId, currentVersion);
    }

    public ModrinthUpdater(@NotNull String urlBase, @NotNull String projectId, @NotNull String currentVersion) {
        this.urlBaseDefault = normalizeUrlBase(urlBase);
        this.projectId = projectId;
        this.currentVersion = Version.parse(currentVersion);
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private static @NotNull String normalizeUrlBase(@NotNull String urlBase) {
        return urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    public @NotNull Optional<Result> lastResult() {
        return Optional.ofNullable(lastResult);
    }

    public @NotNull Result checkForUpdate(boolean allowStaging) {
        return _checkForUpdate(urlBaseDefault, allowStaging);
    }

    public @NotNull Result checkForUpdate(@NotNull String urlBase, boolean allowStaging) {
        return _checkForUpdate(normalizeUrlBase(urlBase), allowStaging);
    }

    private @NotNull Result _checkForUpdate(@NotNull String normalizedUrlBase, boolean allowStaging) {
        List<Artifact> versions;
        try {
            versions = _fetchVersions(normalizedUrlBase);
        } catch (IOException | InterruptedException e) {
            return (lastResult = new Result.Failure(currentVersion, e));
        }

        return lastResult = latestMatchingVersion(versions, allowStaging).<Result>map(artifact ->
                new Result.Success(
                        currentVersion,
                        artifact,
                        switch (Comparison.of(artifact.version, currentVersion)) {
                            case ABOVE -> Status.AVAILABLE;
                            case EQUAL -> Status.UP_TO_DATE;
                            case BELOW -> Status.AHEAD;
                        }
                )
        ).orElseGet(() ->
                new Result.Failure(
                        currentVersion,
                        new NoSuchElementException("No matching versions found")
                )
        );
    }

    public @NotNull Optional<Artifact> getLatestVersion(boolean allowStaging) throws IOException, InterruptedException {
        return _getLatestVersion(urlBaseDefault, allowStaging);
    }

    public @NotNull Optional<Artifact> getLatestVersion(@NotNull String urlBase, boolean allowStaging) throws IOException, InterruptedException {
        return _getLatestVersion(normalizeUrlBase(urlBase), allowStaging);
    }

    private @NotNull Optional<Artifact> _getLatestVersion(@NotNull String normalizedUrlBase, boolean allowStaging) throws IOException, InterruptedException {
        return latestMatchingVersion(_fetchVersions(normalizedUrlBase), allowStaging);
    }

    private @NotNull Optional<Artifact> latestMatchingVersion(@NotNull List<Artifact> versions, boolean allowStaging) {
        return versions.stream()
                .filter(v -> currentVersion.snapshot() || !v.version().snapshot())
                .filter(v -> allowStaging || v.versionType() == Version.Type.RELEASE)
                .max(BY_VERSION);
    }

    public @NotNull List<Artifact> fetchVersions() throws IOException, InterruptedException {
        return _fetchVersions(urlBaseDefault);
    }

    public @NotNull List<Artifact> fetchVersions(@NotNull String urlBase) throws IOException, InterruptedException {
        return _fetchVersions(normalizeUrlBase(urlBase));
    }

    private @NotNull List<Artifact> _fetchVersions(@NotNull String normalizedUrlBase) throws IOException, InterruptedException {
        String url = normalizedUrlBase + "/v2/project/" + projectId + "/version?include_changelog=false";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API returned status " + response.statusCode());
        }

        try {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            List<Artifact> versions = new ArrayList<>();

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                Version parsed = Version.parse(obj.get("version_number").getAsString());
                Version.Type versionType = Version.Type.of(obj.get("version_type").getAsString());
                JsonObject primaryFile = extractPrimaryFile(obj.getAsJsonArray("files"));
                if (primaryFile == null) {
                    throw new IOException("Missing primary file for version " + parsed.raw());
                }
                versions.add(new Artifact(
                        parsed,
                        versionType,
                        primaryFile.get("url").getAsString(),
                        primaryFile.get("filename").getAsString()
                ));
            }
            return versions;
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse Modrinth response", e);
        }
    }

    private static @Nullable JsonObject extractPrimaryFile(@Nullable JsonArray files) {
        if (files == null) return null;
        JsonObject chosen = null;
        for (JsonElement element : files) {
            JsonObject candidate = element.getAsJsonObject();
            if (chosen == null) chosen = candidate;
            if (candidate.has("primary") && candidate.get("primary").getAsBoolean()) {
                chosen = candidate;
                break;
            }
        }
        return chosen;
    }

    public record Version(
            @NotNull LongList core,
            boolean snapshot,
            @NotNull LongList tail,
            @NotNull String raw
    ) implements Comparable<Version> {
        public static @NotNull Version parse(@NotNull String version) {
            String trimmed = version.trim();
            int snapshotIndex = trimmed.toUpperCase(Locale.ROOT).indexOf(SNAPSHOT_TOKEN);
            boolean isSnapshot = snapshotIndex != -1;
            String corePart = isSnapshot ? trimmed.substring(0, snapshotIndex) : trimmed;
            String tailPart = isSnapshot ? trimmed.substring(snapshotIndex + SNAPSHOT_TOKEN.length()) : "";
            return new Version(extractNumbers(corePart), isSnapshot, extractNumbers(tailPart), trimmed);
        }

        private static @NotNull LongList extractNumbers(@NotNull String text) {
            LongList numbers = new LongArrayList();
            Matcher matcher = NUMBER_PATTERN.matcher(text);
            while (matcher.find()) {
                numbers.add(Long.parseLong(matcher.group()));
            }
            return LongLists.unmodifiable(numbers);
        }

        @Override
        public int compareTo(@NotNull Version other) {
            int coreCmp = compareNumbers(core, other.core);
            if (coreCmp != 0) return coreCmp;
            if (snapshot != other.snapshot) return Boolean.compare(other.snapshot, snapshot);
            return snapshot ? compareNumbers(tail, other.tail) : 0;
        }

        private static int compareNumbers(@NotNull LongList a, @NotNull LongList b) {
            int shared = Math.min(a.size(), b.size());
            for (int i = 0; i < shared; i++) {
                int cmp = Long.compare(a.getLong(i), b.getLong(i));
                if (cmp != 0) return cmp;
            }
            return Integer.compare(a.size(), b.size());
        }

        public enum Type {
            UNKNOWN, ALPHA, BETA, RELEASE;

            public static @NotNull Type of(@Nullable String raw) {
                if (raw == null) return UNKNOWN;
                return switch (raw.toLowerCase(Locale.ROOT)) {
                    case "release" -> RELEASE;
                    case "beta" -> BETA;
                    case "alpha" -> ALPHA;
                    default -> UNKNOWN;
                };
            }
        }
    }

    public record Artifact(
            @NotNull Version version,
            @NotNull Version.Type versionType,
            @NotNull String fileUrl,
            @NotNull String fileName
    ) {
        public @NotNull String versionRaw() {
            return version.raw();
        }
    }

    public sealed interface Result {
        @NotNull Version current();

        default @NotNull String currentRaw() {
            return current().raw;
        }

        @NotNull Status status();

        record Failure(@NotNull Version current, @NotNull Exception cause) implements Result {
            @Override
            public @NotNull Status status() {
                return Status.FAILURE;
            }
        }

        record Success(@NotNull Version current, @NotNull ModrinthUpdater.Artifact latestFile, @NotNull Status status) implements Result {
            public @NotNull Version latest() {
                return latestFile().version;
            }

            public @NotNull String latestRaw() {
                return latestFile().version.raw;
            }
        }
    }

    public enum Status {
        FAILURE,
        AVAILABLE,
        UP_TO_DATE,
        AHEAD
    }
}