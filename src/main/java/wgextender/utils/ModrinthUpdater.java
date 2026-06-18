package wgextender.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
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
    private static final Comparator<VersionFile> BY_VERSION = Comparator.comparing(VersionFile::version);

    private final String urlBaseDefault;
    private final String projectId;
    private final PluginVersion currentVersion;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private CheckResult lastResult;

    public ModrinthUpdater(@NotNull String projectId, @NotNull String currentVersion) {
        this(MODRINTH_BASE, projectId, currentVersion);
    }

    public ModrinthUpdater(@NotNull String urlBase, @NotNull String projectId, @NotNull String currentVersion) {
        this.urlBaseDefault = normalizeUrlBase(urlBase);
        this.projectId = projectId;
        this.currentVersion = PluginVersion.parse(currentVersion);
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private static @NotNull String normalizeUrlBase(@NotNull String urlBase) {
        return urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    public @NotNull Optional<CheckResult> lastResult() {
        return Optional.ofNullable(lastResult);
    }

    public @NotNull ModrinthUpdater.CheckResult checkForUpdate(boolean allowStaging) {
        return _checkForUpdate(urlBaseDefault, allowStaging);
    }

    public @NotNull ModrinthUpdater.CheckResult checkForUpdate(@NotNull String urlBase, boolean allowStaging) {
        return _checkForUpdate(normalizeUrlBase(urlBase), allowStaging);
    }

    private @NotNull ModrinthUpdater.CheckResult _checkForUpdate(@NotNull String normalizedUrlBase, boolean allowStaging) {
        List<VersionFile> versions;
        try {
            versions = _fetchVersions(normalizedUrlBase);
        } catch (IOException | InterruptedException e) {
            return (lastResult = new CheckResult.Failure(currentVersion, e));
        }

        Optional<VersionFile> latest = latestMatchingVersion(versions, allowStaging);
        if (latest.isEmpty()) {
            return (lastResult = new CheckResult.Unknown(currentVersion));
        }

        int cmp = latest.get().version().compareTo(currentVersion);
        if (cmp > 0) {
            return (lastResult = new CheckResult.Available(currentVersion, latest.get()));
        }
        if (cmp < 0) {
            return (lastResult = new CheckResult.Ahead(currentVersion, latest.get()));
        }
        return (lastResult = new CheckResult.UpToDate(currentVersion, latest.get()));
    }

    public @NotNull Optional<VersionFile> getLatestVersion(boolean allowStaging) throws IOException, InterruptedException {
        return _getLatestVersion(urlBaseDefault, allowStaging);
    }

    public @NotNull Optional<VersionFile> getLatestVersion(@NotNull String urlBase, boolean allowStaging) throws IOException, InterruptedException {
        return _getLatestVersion(normalizeUrlBase(urlBase), allowStaging);
    }

    private @NotNull Optional<VersionFile> _getLatestVersion(@NotNull String normalizedUrlBase, boolean allowStaging) throws IOException, InterruptedException {
        return latestMatchingVersion(_fetchVersions(normalizedUrlBase), allowStaging);
    }

    private @NotNull Optional<VersionFile> latestMatchingVersion(@NotNull List<VersionFile> versions, boolean allowStaging) {
        return versions.stream()
                .filter(v -> currentVersion.snapshot() || !v.version().snapshot())
                .filter(v -> allowStaging || v.versionType() == VersionType.RELEASE)
                .max(BY_VERSION);
    }

    public @NotNull List<VersionFile> fetchVersions() throws IOException, InterruptedException {
        return _fetchVersions(urlBaseDefault);
    }

    public @NotNull List<VersionFile> fetchVersions(@NotNull String urlBase) throws IOException, InterruptedException {
        return _fetchVersions(normalizeUrlBase(urlBase));
    }

    private @NotNull List<VersionFile> _fetchVersions(@NotNull String normalizedUrlBase) throws IOException, InterruptedException {
        String url = normalizedUrlBase + "/v2/project/" + projectId + "/version?include_changelog=false";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API returned status " + response.statusCode());
        }

        try {
            JsonArray array = JsonParser.parseString(response.body()).getAsJsonArray();
            List<VersionFile> versions = new ArrayList<>();

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                PluginVersion parsed = PluginVersion.parse(obj.get("version_number").getAsString());
                VersionType versionType = VersionType.parse(obj.get("version_type").getAsString());
                JsonObject primaryFile = extractPrimaryFile(obj.getAsJsonArray("files"));
                if (primaryFile == null) {
                    throw new IOException("Missing primary file for version " + parsed.raw());
                }
                versions.add(new VersionFile(
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

    public record PluginVersion(
            @NotNull LongList core,
            boolean snapshot,
            @NotNull LongList tail,
            @NotNull String raw
    ) implements Comparable<PluginVersion> {
        public static @NotNull PluginVersion parse(@NotNull String version) {
            String trimmed = version.trim();
            int snapshotIndex = trimmed.toUpperCase(Locale.ROOT).indexOf(SNAPSHOT_TOKEN);
            boolean isSnapshot = snapshotIndex != -1;
            String corePart = isSnapshot ? trimmed.substring(0, snapshotIndex) : trimmed;
            String tailPart = isSnapshot ? trimmed.substring(snapshotIndex + SNAPSHOT_TOKEN.length()) : "";
            return new PluginVersion(extractNumbers(corePart), isSnapshot, extractNumbers(tailPart), trimmed);
        }

        private static @NotNull LongList extractNumbers(@NotNull String text) {
            LongList numbers = new LongArrayList();
            Matcher matcher = NUMBER_PATTERN.matcher(text);
            while (matcher.find()) {
                numbers.add(Long.parseLong(matcher.group()));
            }
            return numbers;
        }

        @Override
        public int compareTo(@NotNull PluginVersion other) {
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
    }

    public enum VersionType {
        RELEASE, BETA, ALPHA, UNKNOWN;

        public static @NotNull VersionType parse(@Nullable String raw) {
            if (raw == null) return UNKNOWN;
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }

    public record VersionFile(
            @NotNull PluginVersion version,
            @NotNull VersionType versionType,
            @NotNull String fileUrl,
            @NotNull String fileName
    ) {
        public @NotNull String versionRaw() {
            return version.raw();
        }
    }

    public sealed interface CheckResult {
        @NotNull PluginVersion current();

        default @NotNull String currentRaw() {
            return current().raw;
        }

        record Failure(@NotNull PluginVersion current, @NotNull Exception cause) implements CheckResult { }

        record Unknown(@NotNull PluginVersion current) implements CheckResult { }

        sealed interface Known extends CheckResult {
            @NotNull ModrinthUpdater.VersionFile latestFile();

            default @NotNull PluginVersion latest() {
                return latestFile().version;
            }

            default @NotNull String latestRaw() {
                return latestFile().version.raw;
            }
        }

        record Ahead(@NotNull PluginVersion current, @NotNull ModrinthUpdater.VersionFile latestFile) implements Known { }

        record UpToDate(@NotNull PluginVersion current, @NotNull ModrinthUpdater.VersionFile latestFile) implements Known { }

        record Available(@NotNull PluginVersion current, @NotNull ModrinthUpdater.VersionFile latestFile) implements Known { }
    }
}