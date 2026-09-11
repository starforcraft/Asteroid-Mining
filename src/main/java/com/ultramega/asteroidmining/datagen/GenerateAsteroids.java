package com.ultramega.asteroidmining.datagen;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.jspecify.annotations.Nullable;

import static com.ultramega.asteroidmining.AsteroidMining.MOD_ID;

public final class GenerateAsteroids {
    private static final Path PROFILE_FILE = Path.of("src/main/resources/data/asteroidmining/asteroid_generation/profiles.json");
    private static final Path OUTPUT_DIR = Path.of("src/generated/resources/data/asteroidmining/asteroids");
    private static final Path DEFAULT_SAMPLE_CACHE = Path.of("src/generated/resources/.cache/sbdb_asteroid_sample_cache.json");
    private static final String DEFAULT_SAMPLE_SEED = "asteroidmining-sbdb-v1";
    private static final String JPL_SBDB_QUERY_URL = "https://ssd-api.jpl.nasa.gov/sbdb_query.api";
    private static final int API_RETRIES = 4;

    private static final double AU_TO_SCREEN_UNITS = 100.0;
    private static final int DEFAULT_AMOUNT = 10_000;
    private static final int DEFAULT_CHUNK_SIZE = 5_000;
    private static final int DEFAULT_API_PAGE_SIZE = 2_500;
    private static final int SAMPLE_ALGORITHM_VERSION = 3;
    private static final int FLUID_BUCKET = 1_000;

    private static final List<OrbitBand> ORBIT_BANDS = List.of(
        new OrbitBand("inner", 0.0, 2.0, 20),
        new OrbitBand("main-belt", 2.0, 3.2, 30),
        new OrbitBand("outer-belt-jupiter", 3.2, 5.5, 20),
        new OrbitBand("outer-giants-inner", 5.5, 15.0, 10),
        new OrbitBand("outer-giants-outer", 15.0, 30.1, 10),
        new OrbitBand("trans-neptunian", 30.1, Double.POSITIVE_INFINITY, 10)
    );
    private static final boolean ORBIT_VISIBLE = false;
    private static final List<String> ASTEROID_TEXTURES = List.of(
        MOD_ID + ":space/asteroid_1",
        MOD_ID + ":space/asteroid_2"
    );
    private static final List<String> SBDB_FIELDS = List.of(
        "spkid", "full_name", "pdes", "name", "diameter", "H", "a", "e", "i",
        "per", "class", "neo", "pha", "rot_per", "albedo", "spec_T", "spec_B"
    );

    private static final Map<String, String> TAXONOMY_PREFIX_TO_PROFILE = Map.ofEntries(
        Map.entry("A", "S"), Map.entry("B", "C"), Map.entry("C", "C"),
        Map.entry("D", "D"), Map.entry("E", "M"), Map.entry("F", "C"),
        Map.entry("G", "C"), Map.entry("K", "S"), Map.entry("L", "S"),
        Map.entry("M", "M"), Map.entry("P", "D"), Map.entry("Q", "S"),
        Map.entry("S", "S"), Map.entry("T", "D"), Map.entry("V", "V"),
        Map.entry("X", "M")
    );

    private static final Map<String, Integer> FALLBACK_PROFILE_WEIGHTS = new LinkedHashMap<>();
    private static final Set<String> EXCLUDED_MAJOR_BODY_SLUGS = Set.of(
        "mercury", "venus", "earth", "moon", "luna", "mars", "phobos", "deimos",
        "jupiter", "io", "europa", "ganymede", "callisto", "saturn", "titan",
        "enceladus", "mimas", "iapetus", "rhea", "uranus", "titania", "oberon",
        "neptune", "triton"
    );

    private static final Set<String> RETRYABLE_HTTP = Set.of("429", "500", "502", "503", "504");
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT_GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(120))
        .build();

    static {
        FALLBACK_PROFILE_WEIGHTS.put("C", 38);
        FALLBACK_PROFILE_WEIGHTS.put("S", 34);
        FALLBACK_PROFILE_WEIGHTS.put("M", 10);
        FALLBACK_PROFILE_WEIGHTS.put("V", 5);
        FALLBACK_PROFILE_WEIGHTS.put("D", 8);
        FALLBACK_PROFILE_WEIGHTS.put("ICY", 5);
    }

    private GenerateAsteroids() {
    }

    static void main(final String[] args) {
        try {
            run(Arguments.parse(args));
        } catch (IllegalArgumentException | IOException | InterruptedException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void run(final Arguments args) throws IOException, InterruptedException {
        final Integer amountLimit = parseAmount(args.amount);
        final JsonObject profiles = loadProfiles(args.profileFile);

        if (args.apiPageSize <= 0 || args.chunkSize <= 0) {
            throw new IllegalArgumentException("--api-page-size and --chunk-size must be larger than 0");
        }

        if (amountLimit == null) {
            final Set<String> usedPaths = new HashSet<>();
            try (ChunkWriter output = new ChunkWriter(args.outputDir, args.chunkSize)) {
                forEachSbdbRow(args.apiPageSize, row -> {
                    final JsonObject config = toConfig(row, usedPaths, profiles, ASTEROID_TEXTURES);
                    if (config != null) {
                        output.write(config);
                    }
                });
                output.finish(args.clearOutputDir);
            }
            return;
        }

        final List<JsonObject> rows;
        if (amountLimit == 0) {
            rows = List.of();
        } else {
            List<JsonObject> cachedRows = null;
            if (!args.noSampleCache && !args.refreshSample) {
                cachedRows = loadSampleCache(args.sampleCache, amountLimit, args.sampleSeed);
            }

            if (cachedRows != null) {
                System.out.printf("Loaded the same %,d asteroid rows from sample cache %s%n", cachedRows.size(), args.sampleCache);
                rows = cachedRows;
            } else {
                final SampleResult result = fetchStratifiedSbdbRows(amountLimit, args.apiPageSize, args.sampleSeed);
                rows = result.rows;
                if (!args.noSampleCache) {
                    System.out.println("Saving deterministic sample cache to " + args.sampleCache + "...");
                    writeSampleCache(args.sampleCache, rows, amountLimit, args.sampleSeed, result.catalogCount);
                }
            }
        }

        final Set<String> usedPaths = new HashSet<>();
        try (ChunkWriter output = new ChunkWriter(args.outputDir, args.chunkSize)) {
            for (final JsonObject row : selectAsteroidRows(rows, amountLimit, args.sampleSeed)) {
                final JsonObject config = toConfig(row, usedPaths, profiles, ASTEROID_TEXTURES);
                if (config != null) {
                    output.write(config);
                }
            }
            output.finish(args.clearOutputDir);
        }
    }

    @Nullable
    private static Integer parseAmount(final String amount) {
        if ("all".equalsIgnoreCase(amount)) {
            return null;
        }
        try {
            final int count = Integer.parseInt(amount);
            if (count < 0) {
                throw new IllegalArgumentException("--amount must be non-negative");
            }
            return count;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--amount must be an integer or 'all'");
        }
    }

    private static JsonObject requestSbdb(final Map<String, String> params, final String description)
        throws IOException, InterruptedException {
        final String query = params.entrySet().stream()
            .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
            .collect(Collectors.joining("&"));
        final URI uri = URI.create(JPL_SBDB_QUERY_URL + "?" + query);
        Exception lastError = null;

        for (int attempt = 0; attempt < API_RETRIES; attempt++) {
            try {
                final HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(120))
                    .GET()
                    .build();
                final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    final JsonElement parsed = JsonParser.parseString(response.body());
                    if (!parsed.isJsonObject()) {
                        throw new IOException("JPL SBDB response was not a JSON object");
                    }
                    return parsed.getAsJsonObject();
                }

                lastError = new IOException("HTTP " + response.statusCode());
                if (!RETRYABLE_HTTP.contains(Integer.toString(response.statusCode()))) {
                    break;
                }
            } catch (IOException e) {
                lastError = e;
            }

            if (attempt + 1 < API_RETRIES) {
                final long delay = 1L << attempt;
                System.out.printf("JPL request failed (%s); retrying in %ds: %s%n", description, delay, lastError);
                Thread.sleep(delay * 1000L);
            }
        }

        throw new IOException("Failed JPL SBDB request (" + description + "): " + lastError);
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static Map<String, String> baseSbdbParams(@Nullable final OrbitBand band) {
        final Map<String, String> params = new LinkedHashMap<>();
        params.put("sb-kind", "a");
        final JsonArray constraints = new JsonArray();
        constraints.add("a|GT|0");
        constraints.add("e|GE|0");
        constraints.add("e|LT|1");
        if (band != null) {
            constraints.add("a|GE|" + band.minAu);
            if (Double.isFinite(band.maxAu)) {
                constraints.add("a|LT|" + band.maxAu);
            }
        }
        final JsonObject filter = new JsonObject();
        filter.add("AND", constraints);
        params.put("sb-cdata", COMPACT_GSON.toJson(filter));
        params.put("full-prec", "true");
        return params;
    }

    private static int fetchSbdbCount() throws IOException, InterruptedException {
        return fetchSbdbCount(null);
    }

    private static int fetchSbdbCount(@Nullable final OrbitBand band) throws IOException, InterruptedException {
        final JsonObject payload = requestSbdb(baseSbdbParams(band), band == null ? "catalog count" : band.name + " count");
        if (!payload.has("count")) {
            throw new IOException("JPL SBDB count response was invalid: " + payload);
        }
        return Integer.parseInt(payload.get("count").getAsString());
    }

    private static JsonObject fetchSbdbPage(final int offset, final int pageSize, final String sort)
        throws IOException, InterruptedException {
        return fetchSbdbPage(offset, pageSize, sort, null);
    }

    private static JsonObject fetchSbdbPage(final int offset, final int pageSize, final String sort, @Nullable final OrbitBand band)
        throws IOException, InterruptedException {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("--api-page-size must be larger than 0");
        }
        final Map<String, String> params = baseSbdbParams(band);
        params.put("fields", String.join(",", SBDB_FIELDS));
        params.put("sort", sort);
        params.put("limit", Integer.toString(pageSize));
        params.put("limit-from", Integer.toString(offset));
        return requestSbdb(params, "offset " + offset + ", limit " + pageSize);
    }

    private static List<JsonObject> tableRows(final JsonObject sbdb) {
        final List<String> fields = new ArrayList<>();
        final JsonArray fieldsJson = sbdb.getAsJsonArray("fields");
        if (fieldsJson != null) {
            for (final JsonElement field : fieldsJson) {
                fields.add(field.getAsString());
            }
        } else {
            fields.addAll(SBDB_FIELDS);
        }

        final List<JsonObject> rows = new ArrayList<>();
        final JsonArray data = sbdb.getAsJsonArray("data");
        if (data == null) {
            return rows;
        }

        for (final JsonElement rowElement : data) {
            final JsonArray row = rowElement.getAsJsonArray();
            final JsonObject object = new JsonObject();
            for (int i = 0; i < Math.min(fields.size(), row.size()); i++) {
                object.add(fields.get(i), row.get(i).deepCopy());
            }
            rows.add(object);
        }
        return rows;
    }

    private static void forEachSbdbRow(final int pageSize, final RowConsumer consumer) throws IOException, InterruptedException {
        int offset = 0;
        final int totalCount = fetchSbdbCount();

        while (offset < totalCount) {
            final int requestSize = Math.min(pageSize, totalCount - offset);
            System.out.printf("Fetching complete JPL catalog: %,d/%,d rows...%n", offset, totalCount);
            final List<JsonObject> pageRows = tableRows(fetchSbdbPage(offset, requestSize, "a,spkid"));
            if (pageRows.isEmpty()) {
                throw new IOException("JPL returned an empty page before the end of the catalog");
            }
            for (final JsonObject row : pageRows) {
                if (isGeneratableAsteroidRow(row)) {
                    consumer.accept(row);
                }
            }
            offset += pageRows.size();
        }
    }

    private static List<RequestPlan> stratifiedRequestPlan(final int totalCount, final int amount, final int maxPageSize, final String sampleSeed) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        final int actualAmount = Math.min(amount, totalCount);
        if (actualAmount == 0) {
            return List.of();
        }
        if (maxPageSize <= 0) {
            throw new IllegalArgumentException("--api-page-size must be larger than 0");
        }

        final int stratumCount = Math.max(1, (int) (((long) actualAmount + maxPageSize - 1) / maxPageSize));
        final List<RequestPlan> requests = new ArrayList<>();

        for (int index = 0; index < stratumCount; index++) {
            final int stratumStart = (int) (((long) index * totalCount) / stratumCount);
            final int stratumEnd = (int) (((long) (index + 1) * totalCount) / stratumCount);
            final int stratumSize = stratumEnd - stratumStart;
            final int takeStart = (int) (((long) index * actualAmount) / stratumCount);
            final int takeEnd = (int) (((long) (index + 1) * actualAmount) / stratumCount);
            final int take = Math.min(takeEnd - takeStart, stratumSize);
            if (take <= 0) {
                continue;
            }

            final int localStart = stableSeedBigInteger(sampleSeed, "stratum", index)
                .mod(BigInteger.valueOf(stratumSize)).intValue();
            final int firstLimit = Math.min(take, stratumSize - localStart);
            requests.add(new RequestPlan(index, stratumCount, stratumStart + localStart, firstLimit));

            final int wrappedLimit = take - firstLimit;
            if (wrappedLimit > 0) {
                requests.add(new RequestPlan(index, stratumCount, stratumStart, wrappedLimit));
            }
        }
        return requests;
    }

    private static SampleResult fetchStratifiedSbdbRows(final int amount, final int pageSize, final String sampleSeed)
        throws IOException, InterruptedException {
        final int[] capacities = new int[ORBIT_BANDS.size()];
        int totalCount = 0;
        for (int i = 0; i < capacities.length; i++) {
            capacities[i] = fetchSbdbCount(ORBIT_BANDS.get(i));
            totalCount = Math.addExact(totalCount, capacities[i]);
        }
        final int target = Math.min(amount, totalCount);
        final int[] quotas = allocateBandQuotas(capacities, target);
        final List<JsonObject> rows = new ArrayList<>(target);
        final Set<String> identities = new HashSet<>();
        for (int i = 0; i < capacities.length; i++) {
            final OrbitBand band = ORBIT_BANDS.get(i);
            System.out.printf("Sampling %s: %,d of %,d available objects%n", band.name, quotas[i], capacities[i]);
            final List<RequestPlan> plan = stratifiedRequestPlan(capacities[i], quotas[i], pageSize, sampleSeed + "|" + band.name);
            for (final RequestPlan request : plan) {
                // SPK-ID ordering avoids fetching a narrow contiguous slice of orbital distances.
                final List<JsonObject> page = tableRows(fetchSbdbPage(request.offset, request.limit, "spkid", band));
                if (page.size() != request.limit) {
                    throw new IOException("JPL catalog changed while sampling " + band.name + "; retry generation.");
                }
                for (final JsonObject row : page) {
                    final Double axis = parseDouble(row.get("a"), null);
                    if (!isGeneratableAsteroidRow(row) || axis == null || axis < band.minAu || axis >= band.maxAu
                        || !identities.add(asteroidIdentity(row))) {
                        throw new IOException("Invalid or duplicate JPL row in " + band.name + "; retry generation.");
                    }
                    rows.add(row);
                }
            }
        }
        return new SampleResult(rows, totalCount);
    }

    private static int[] allocateBandQuotas(final int[] capacities, final int target) {
        final int[] quotas = new int[capacities.length];
        int remaining = target;
        while (remaining > 0) {
            int activeWeight = 0;
            for (int i = 0; i < capacities.length; i++) {
                if (quotas[i] < capacities[i]) {
                    activeWeight += ORBIT_BANDS.get(i).weight;
                }
            }
            if (activeWeight == 0) {
                break;
            }
            final int roundAmount = remaining;
            int cumulativeWeight = 0;
            for (int i = 0; i < capacities.length; i++) {
                if (quotas[i] >= capacities[i]) {
                    continue;
                }
                final int weight = ORBIT_BANDS.get(i).weight;
                final int share = (int) (((long) roundAmount * (cumulativeWeight + weight)) / activeWeight
                    - ((long) roundAmount * cumulativeWeight) / activeWeight);
                cumulativeWeight += weight;
                final int take = Math.min(share, capacities[i] - quotas[i]);
                quotas[i] += take;
                remaining -= take;
            }
        }
        return quotas;
    }

    @Nullable
    private static List<JsonObject> loadSampleCache(final Path cacheFile, final int amount, final String sampleSeed) {
        if (!Files.exists(cacheFile)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
            final JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                return null;
            }
            final JsonObject payload = parsed.getAsJsonObject();

            if (getInt(payload, "algorithmVersion", -1) != SAMPLE_ALGORITHM_VERSION) {
                return null;
            }
            if (getInt(payload, "amount", -1) != amount) {
                return null;
            }
            if (!sampleSeed.equals(getString(payload, "sampleSeed"))) {
                return null;
            }

            final JsonArray fields = payload.getAsJsonArray("fields");
            if (fields == null || fields.size() != SBDB_FIELDS.size()) {
                return null;
            }
            for (int i = 0; i < fields.size(); i++) {
                if (!SBDB_FIELDS.get(i).equals(fields.get(i).getAsString())) {
                    return null;
                }
            }

            final JsonArray rowsJson = payload.getAsJsonArray("rows");
            if (rowsJson == null || rowsJson.size() != Math.min(amount, getInt(payload, "catalogCountAtCreation", -1))) {
                return null;
            }
            final List<JsonObject> rows = new ArrayList<>(rowsJson.size());
            final Set<String> identities = new HashSet<>();
            for (final JsonElement element : rowsJson) {
                if (!element.isJsonObject() || !isGeneratableAsteroidRow(element.getAsJsonObject())
                    || !identities.add(asteroidIdentity(element.getAsJsonObject()))) {
                    return null;
                }
                rows.add(element.getAsJsonObject());
            }
            return rows;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writeSampleCache(
        final Path cacheFile, final List<JsonObject> rows, final int amount, final String sampleSeed, final int catalogCount)
        throws IOException {
        final Path parent = cacheFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        final Path temporary = Files.createTempFile(cacheFile.toAbsolutePath().getParent(), "asteroid-sample-", ".tmp");
        try {
            try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(temporary, StandardCharsets.UTF_8))) {
                writer.beginObject();
                writer.name("algorithmVersion").value(SAMPLE_ALGORITHM_VERSION);
                writer.name("amount").value(amount);
                writer.name("sampleSeed").value(sampleSeed);
                writer.name("catalogCountAtCreation").value(catalogCount);
                writer.name("sort").value("spkid");
                writer.name("sampling").value("weighted-semimajor-axis-bands");
                writer.name("fields");
                COMPACT_GSON.toJson(COMPACT_GSON.toJsonTree(SBDB_FIELDS), writer);
                writer.name("rows").beginArray();
                for (final JsonObject row : rows) {
                    COMPACT_GSON.toJson(row, writer);
                }
                writer.endArray().endObject();
            }
            replaceFile(temporary, cacheFile);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String normalizedSlug(@Nullable final String value, final String fallback) {
        final String source = (value == null || value.isEmpty()) ? fallback : value;
        final String normalized = Normalizer.normalize(source, Normalizer.Form.NFKD);
        final StringBuilder ascii = new StringBuilder(normalized.length());
        for (final char c : normalized.toCharArray()) {
            if (c <= 0x7f) {
                ascii.append(c);
            }
        }
        final String slug = ascii.toString().toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? fallback : slug;
    }

    @Nullable
    private static Double parseDouble(@Nullable final JsonElement value, @Nullable final Double defaultValue) {
        if (value == null || value.isJsonNull()) {
            return defaultValue;
        }
        final String string = value.isJsonPrimitive() ? value.getAsString() : "";
        if (string.isEmpty() || "null".equals(string)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static byte[] sha256(final String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static BigInteger stableSeedBigInteger(final Object... parts) {
        final String joined = Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining("|"));
        final byte[] digest = sha256(joined);
        return new BigInteger(1, Arrays.copyOf(digest, 8));
    }

    private static long stableSeedLong(final Object... parts) {
        final String joined = Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining("|"));
        return ByteBuffer.wrap(sha256(joined), 0, 8).getLong();
    }

    private static BigInteger deterministicSampleRank(final String identity, final String sampleSeed) {
        return new BigInteger(1, sha256(sampleSeed + "|" + identity + "|sample"));
    }

    private static double deterministicAngle(final String seed) {
        final byte[] digest = sha256(seed + "|angle");
        final long integer = Integer.toUnsignedLong(ByteBuffer.wrap(digest, 0, 4).getInt());
        return round((integer / 4294967295.0) * 360.0, 3);
    }

    private static int scaledDiameter(@Nullable final Double diameterKm, @Nullable final Double absoluteMagnitude) {
        if (diameterKm != null && diameterKm > 0) {
            return Math.clamp((int) Math.round(Math.log10(diameterKm + 1.0) * 1.35), 1, 8);
        }
        if (absoluteMagnitude != null) {
            return absoluteMagnitude >= 14 ? 1 : 2;
        }
        return 1;
    }

    private static double orbitalSpeed(@Nullable final Double aAu) {
        if (aAu == null || aAu <= 0) {
            return 0.0;
        }
        return round((29.78 / Math.sqrt(aAu)) / 100.0, 5);
    }

    private static String cleanName(final String rawName, final String designation, final String spkid) {
        final String value = firstNonBlank(rawName, designation, spkid, "asteroid").trim();
        return value.replaceAll("\\s+", " ");
    }

    private static String asteroidIdentity(final JsonObject row) {
        final String spkid = getString(row, "spkid").trim();
        final String designation = getString(row, "pdes").trim();
        final String name = cleanName(firstNonBlank(getString(row, "full_name"), getString(row, "name")), designation, spkid);
        return firstNonBlank(spkid, designation, name);
    }

    private static String pickTexture(final String seed, final JsonObject profile, final List<String> fallbackTextures) {
        final Random rng = new Random(stableSeedLong(seed, "texture"));
        final JsonElement configured = profile.get("textures");
        if (configured != null && !configured.isJsonNull() && !configured.getAsJsonArray().isEmpty()) {
            final JsonArray textures = configured.getAsJsonArray();
            return textures.get(rng.nextInt(textures.size())).getAsString();
        }
        return fallbackTextures.get(rng.nextInt(fallbackTextures.size()));
    }

    private static void validateProfileTextures(final String profileKey, final JsonObject profile) throws IOException {
        final JsonElement textures = profile.get("textures");
        if (textures == null || textures.isJsonNull()) {
            return;
        }
        if (!textures.isJsonArray()) {
            throw new IOException("Profile " + profileKey + ": textures must be an array of sprite identifiers");
        }
        for (final JsonElement texture : textures.getAsJsonArray()) {
            if (!texture.isJsonPrimitive() || !texture.getAsJsonPrimitive().isString()
                || !texture.getAsString().matches("(?:[a-z0-9_.-]+:)?[a-z0-9/._-]+")) {
                throw new IOException("Profile " + profileKey + ": invalid texture identifier " + texture);
            }
        }
    }

    private static JsonObject loadProfiles(final Path profileFile) throws IOException {
        if (!Files.exists(profileFile)) {
            throw new IOException("Profile file not found: " + profileFile);
        }
        try (Reader reader = Files.newBufferedReader(profileFile, StandardCharsets.UTF_8)) {
            final JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject() || parsed.getAsJsonObject().isEmpty()) {
                throw new IOException("Profile file must contain a non-empty JSON object: " + profileFile);
            }
            final JsonObject profiles = parsed.getAsJsonObject();
            for (final Map.Entry<String, JsonElement> entry : profiles.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    throw new IOException("Profile " + entry.getKey() + " must be a JSON object");
                }
                validateProfileTextures(entry.getKey(), entry.getValue().getAsJsonObject());
            }
            return profiles;
        }
    }

    private static String pickProfileKey(final JsonObject row, final Random rng, final JsonObject profiles) {
        final String spec = firstNonBlank(getString(row, "spec_T"), getString(row, "spec_B"))
            .toUpperCase(Locale.ROOT).trim();
        if (!spec.isEmpty()) {
            final String mapped = TAXONOMY_PREFIX_TO_PROFILE.get(spec.substring(0, 1));
            if (mapped != null && profiles.has(mapped)) {
                return mapped;
            }
        }

        final String orbitClass = getString(row, "class").toUpperCase(Locale.ROOT).trim();
        final Double albedo = parseDouble(row.get("albedo"), null);
        final Double aAu = parseDouble(row.get("a"), null);

        if ((Set.of("CEN", "TNO").contains(orbitClass) || (aAu != null && aAu >= 5.5)) && profiles.has("ICY")) {
            return "ICY";
        }
        if (albedo != null) {
            if (albedo < 0.08 && profiles.has("C")) {
                return "C";
            }
            if (albedo > 0.28 && profiles.has("S")) {
                return "S";
            }
        }

        final int totalWeight = FALLBACK_PROFILE_WEIGHTS.entrySet().stream()
            .filter(e -> profiles.has(e.getKey()))
            .mapToInt(Map.Entry::getValue)
            .sum();
        if (totalWeight > 0) {
            final double draw = rng.nextDouble() * totalWeight;
            double cumulative = 0;
            for (final Map.Entry<String, Integer> entry : FALLBACK_PROFILE_WEIGHTS.entrySet()) {
                if (!profiles.has(entry.getKey())) {
                    continue;
                }
                cumulative += entry.getValue();
                if (draw < cumulative) {
                    return entry.getKey();
                }
            }
        }

        return profiles.keySet().stream().sorted().findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No profiles available"));
    }

    @Nullable
    private static Double diameterFromMagnitude(@Nullable final Double absoluteMagnitude, @Nullable final Double albedo) {
        if (absoluteMagnitude == null) {
            return null;
        }
        final double effectiveAlbedo = Math.clamp(albedo == null ? 0.14 : albedo, 0.02, 0.6);
        return (1329.0 / Math.sqrt(effectiveAlbedo)) * Math.pow(10.0, -absoluteMagnitude / 5.0);
    }

    private static long baseMass(@Nullable final Double diameterKm, final Double absoluteMagnitude, final Double albedo) {
        final Double estimated = diameterFromMagnitude(absoluteMagnitude, albedo);
        final double effectiveDiameter = diameterKm != null ? diameterKm : estimated != null ? estimated : 0.2;
        final double clamped = Math.clamp(effectiveDiameter, 0.05, 120.0);
        return (long) (12_000 * Math.pow(clamped, 1.35));
    }

    private static long amountFromWeight(final Random rng, final long mass, @Nullable final JsonElement bounds, final long minimum) {
        final double low;
        final double high;

        if (bounds != null && bounds.isJsonPrimitive() && bounds.getAsJsonPrimitive().isNumber()) {
            low = bounds.getAsDouble();
            high = bounds.getAsDouble();
        } else if (bounds != null && bounds.isJsonArray() && bounds.getAsJsonArray().size() >= 2) {
            low = bounds.getAsJsonArray().get(0).getAsDouble();
            high = bounds.getAsJsonArray().get(1).getAsDouble();
        } else {
            throw new IllegalArgumentException("Invalid profile bounds: " + bounds);
        }

        final double weight = uniform(rng, Math.min(low, high), Math.max(low, high));
        final double noise = uniform(rng, 0.82, 1.18);
        return Math.max(minimum, (long) (mass * weight * noise));
    }

    private static long maybeAddTrace(final Random rng, final long mass, final JsonElement bounds) {
        if (rng.nextDouble() > 0.35) {
            return 0;
        }
        return amountFromWeight(rng, mass, bounds, 1);
    }

    private static boolean looksLikeChemicalResource(final String resourceId) {
        if (!resourceId.startsWith("mekanism:")) {
            return false;
        }
        return Stream.of("slurry", "hydrogen", "oxygen", "dioxide", "ethene", "water", "chlorine", "sulfur", "uranium_oxide")
            .anyMatch(resourceId::contains);
    }

    private static JsonObject compositionEntry(final String resourceId, final long amount, final String resourceType) {
        final JsonObject entry = new JsonObject();
        final JsonObject resource = new JsonObject();
        resource.addProperty("id", resourceId);
        entry.add("resource", resource);
        entry.addProperty("amount", amount);
        entry.addProperty("type", resourceType);

        final String namespace = resourceId.contains(":") ? resourceId.substring(0, resourceId.indexOf(':')) : resourceId;
        if (!"minecraft".equals(namespace) && !MOD_ID.equals(namespace)) {
            final JsonObject condition = new JsonObject();
            condition.addProperty("type", "neoforge:mod_loaded");
            condition.addProperty("modid", namespace);
            final JsonArray conditions = new JsonArray();
            conditions.add(condition);
            entry.add("neoforge:conditions", conditions);
        }
        return entry;
    }

    private static JsonArray generateComposition(final JsonObject profile, final long mass, final Random rng) {
        final Map<String, Long> items = new TreeMap<>();
        final Map<String, Long> fluids = new TreeMap<>();
        final Map<String, Long> chemicals = new TreeMap<>();

        readResourceAmounts(profile, "items",
            (id, bounds) -> items.put(id, amountFromWeight(rng, mass, bounds, 1)));
        readResourceAmounts(profile, "fluids",
            (id, bounds) -> fluids.put(id, amountFromWeight(rng, mass * FLUID_BUCKET, bounds, FLUID_BUCKET)));
        readResourceAmounts(profile, "chemicals",
            (id, bounds) -> chemicals.put(id, amountFromWeight(rng, mass, bounds, 1)));
        readResourceAmounts(profile, "traces", (id, bounds) -> {
            final long value = maybeAddTrace(rng, mass, bounds);
            if (value <= 0) {
                return;
            }
            if (looksLikeChemicalResource(id)) {
                chemicals.put(id, value);
            } else {
                items.put(id, value);
            }
        });

        final JsonArray composition = new JsonArray();
        items.forEach((id, amount) -> composition.add(compositionEntry(id, amount, "item")));
        fluids.forEach((id, amount) -> composition.add(compositionEntry(id, amount, "fluid")));
        chemicals.forEach((id, amount) -> composition.add(compositionEntry(id, amount, "chemical")));
        return composition;
    }

    private static void readResourceAmounts(final JsonObject profile, final String key, final ResourceAmountConsumer consumer) {
        final JsonObject object = profile.has(key) && profile.get(key).isJsonObject() ? profile.getAsJsonObject(key) : new JsonObject();
        object.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> consumer.accept(e.getKey(), e.getValue()));
    }

    private static boolean isSupportedAsteroidRow(final JsonObject row, final String slug) {
        if (EXCLUDED_MAJOR_BODY_SLUGS.contains(slug)) {
            return false;
        }
        return !getString(row, "pdes").trim().startsWith("S/");
    }

    private static boolean isGeneratableAsteroidRow(final JsonObject row) {
        final String spkid = getString(row, "spkid").trim();
        final String designation = getString(row, "pdes").trim();
        final String name = cleanName(firstNonBlank(getString(row, "full_name"), getString(row, "name")),
            designation, spkid);
        final String seed = firstNonBlank(spkid, designation, name);
        final String slug = normalizedSlug(name, "asteroid_" + seed);
        final Double aAu = parseDouble(row.get("a"), null);
        final Double eccentricity = parseDouble(row.get("e"), null);
        return isSupportedAsteroidRow(row, slug) && aAu != null && Double.isFinite(aAu) && aAu > 0
            && eccentricity != null && Double.isFinite(eccentricity) && eccentricity >= 0 && eccentricity < 1;
    }

    private static List<JsonObject> selectAsteroidRows(final Collection<JsonObject> rows, @Nullable final Integer amountLimit, final String sampleSeed) {
        if (amountLimit != null && amountLimit == 0) {
            return List.of();
        }

        final Comparator<JsonObject> finalOrder = Comparator
            .comparing((JsonObject row) -> deterministicSampleRank(asteroidIdentity(row), sampleSeed))
            .thenComparing(GenerateAsteroids::asteroidIdentity);

        if (amountLimit == null) {
            return rows.stream()
                .filter(GenerateAsteroids::isGeneratableAsteroidRow)
                .sorted(finalOrder)
                .collect(Collectors.toCollection(ArrayList::new));
        }

        final Comparator<RankedRow> worstFirst = Comparator
            .comparing(RankedRow::rank)
            .thenComparingInt(RankedRow::sequence)
            .reversed();
        final PriorityQueue<RankedRow> heap = new PriorityQueue<>(worstFirst);
        int sequence = 0;

        for (final JsonObject row : rows) {
            if (!isGeneratableAsteroidRow(row)) {
                continue;
            }
            final RankedRow ranked = new RankedRow(
                deterministicSampleRank(asteroidIdentity(row), sampleSeed),
                sequence++,
                row
            );
            if (heap.size() < amountLimit) {
                heap.add(ranked);
            } else if (ranked.rank.compareTo(heap.peek().rank) < 0) {
                heap.poll();
                heap.add(ranked);
            }
        }

        return heap.stream().map(RankedRow::row).sorted(finalOrder)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String uniquePath(final String basePath, final String seed, final Set<String> usedPaths) {
        String path = basePath;
        int collisionIndex = 1;
        while (usedPaths.contains(path)) {
            collisionIndex++;
            path = normalizedSlug(basePath + "_" + seed + "_" + collisionIndex, "asteroid_" + usedPaths.size());
        }
        usedPaths.add(path);
        return path;
    }

    @Nullable
    private static JsonObject toConfig(final JsonObject row, final Set<String> usedPaths, final JsonObject profiles, final List<String> textures) {
        final String spkid = getString(row, "spkid").trim();
        final String designation = getString(row, "pdes").trim();
        final String name = cleanName(firstNonBlank(getString(row, "full_name"), getString(row, "name")),
            designation, spkid);
        final String seed = firstNonBlank(spkid, designation, name);
        final String basePath = normalizedSlug(name, "asteroid_" + firstNonBlank(seed, Integer.toString(usedPaths.size())));
        if (!isGeneratableAsteroidRow(row)) {
            return null;
        }
        final String path = uniquePath(basePath, seed, usedPaths);

        final Double aAu = parseDouble(row.get("a"), null);
        final double eccentricity = parseDouble(row.get("e"), 0.0);
        final Double diameterKm = parseDouble(row.get("diameter"), null);
        final Double absoluteMagnitude = parseDouble(row.get("H"), null);
        final Double albedo = parseDouble(row.get("albedo"), null);
        if (aAu == null || aAu <= 0) {
            return null;
        }

        final Random rng = new Random(stableSeedLong(seed, "composition"));
        final String profileKey = pickProfileKey(row, rng, profiles);
        final JsonObject profile = profiles.getAsJsonObject(profileKey);
        final long mass = baseMass(diameterKm, absoluteMagnitude, albedo);
        final double semiMajorAxis = round(aAu * AU_TO_SCREEN_UNITS, 5);
        final double semiMinorAxis = round(semiMajorAxis * Math.sqrt(1.0 - eccentricity * eccentricity), 5);

        final JsonObject config = new JsonObject();
        config.addProperty("id", MOD_ID + ":" + path);
        config.addProperty("name", name);
        config.addProperty("texture", pickTexture(seed, profile, textures));
        config.addProperty("diameter", scaledDiameter(diameterKm, absoluteMagnitude));
        config.add("composition", generateComposition(profile, mass, rng));

        final JsonObject orbit = new JsonObject();
        orbit.addProperty("centralBodyName", "Sun");
        orbit.addProperty("semiMajorAxis", semiMajorAxis);
        orbit.addProperty("semiMinorAxis", semiMinorAxis);
        orbit.addProperty("orbitalSpeed", orbitalSpeed(aAu));
        orbit.addProperty("isClockwise", true);
        orbit.addProperty("startingAngleDegrees", deterministicAngle(seed));
        orbit.addProperty("isOrbitVisible", ORBIT_VISIBLE);
        orbit.addProperty("rotateAroundItself", true);
        config.add("orbit", orbit);

        return config;
    }

    private static void replaceFile(final Path source, final Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String getString(final JsonObject object, final String key) {
        final JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static int getInt(final JsonObject object, final String key, final int fallback) {
        try {
            final JsonElement value = object.get(key);
            return value == null || value.isJsonNull() ? fallback : value.getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String firstNonBlank(final String... values) {
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static double uniform(final Random rng, final double low, final double high) {
        return low + (high - low) * rng.nextDouble();
    }

    private static double round(final double value, final int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_EVEN).doubleValue();
    }

    private static final class ChunkWriter implements AutoCloseable {
        private final Path outputDir;
        private final Path stagingDir;
        private final int chunkSize;
        @Nullable
        private JsonWriter writer;
        private int count;
        private int files;

        private ChunkWriter(final Path outputDir, final int chunkSize) throws IOException {
            this.outputDir = outputDir;
            this.chunkSize = chunkSize;
            Files.createDirectories(outputDir);
            this.stagingDir = Files.createTempDirectory(outputDir.toAbsolutePath().getParent(), ".asteroid-generation-");
        }

        private void write(final JsonObject config) throws IOException {
            if (this.writer == null) {
                this.writer = new JsonWriter(Files.newBufferedWriter(this.stagingDir.resolve(fileName(this.files++)), StandardCharsets.UTF_8));
                this.writer.setIndent("  ");
                this.writer.beginObject().name("asteroids").beginArray();
            }
            PRETTY_GSON.toJson(config, this.writer);
            this.count++;
            if (this.count % this.chunkSize == 0) {
                this.closeChunk();
            }
        }

        private void closeChunk() throws IOException {
            if (this.writer != null) {
                this.writer.endArray().endObject();
                this.writer.close();
                this.writer = null;
            }
        }

        private void finish(final boolean clearObsolete) throws IOException {
            this.closeChunk();
            for (int i = 0; i < this.files; i++) {
                final String name = fileName(i);
                replaceFile(this.stagingDir.resolve(name), this.outputDir.resolve(name));
            }
            if (clearObsolete) {
                // Never delete hand-authored data, profiles, or subdirectories.
                try (DirectoryStream<Path> paths = Files.newDirectoryStream(this.outputDir, "asteroids_*.json")) {
                    for (final Path path : paths) {
                        final String name = path.getFileName().toString();
                        if (name.matches("asteroids_[0-9]{5,10}\\.json") && Long.parseLong(name.substring(10, name.length() - 5)) >= this.files) {
                            Files.deleteIfExists(path);
                        }
                    }
                }
            }
            System.out.printf("Generated %,d asteroids into %,d JSON files at %s%n", this.count, this.files, this.outputDir);
        }

        @Override
        public void close() throws IOException {
            try {
                this.closeChunk();
            } finally {
                try (DirectoryStream<Path> paths = Files.newDirectoryStream(this.stagingDir)) {
                    for (final Path path : paths) {
                        Files.deleteIfExists(path);
                    }
                }
                Files.deleteIfExists(this.stagingDir);
            }
        }

        private static String fileName(final int index) {
            return String.format(Locale.ROOT, "asteroids_%05d.json", index);
        }
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(JsonObject row) throws IOException;
    }

    @FunctionalInterface
    private interface ResourceAmountConsumer {
        void accept(String id, JsonElement bounds);
    }

    private record OrbitBand(String name, double minAu, double maxAu, int weight) {
    }

    private record RequestPlan(int stratumIndex, int stratumCount, int offset, int limit) {
    }

    private record SampleResult(List<JsonObject> rows, int catalogCount) {
    }

    private record RankedRow(BigInteger rank, int sequence, JsonObject row) {
    }

    private static final class Arguments {
        String amount = Integer.toString(DEFAULT_AMOUNT);
        Path outputDir = OUTPUT_DIR;
        Path profileFile = PROFILE_FILE;
        int chunkSize = DEFAULT_CHUNK_SIZE;
        int apiPageSize = DEFAULT_API_PAGE_SIZE;
        Path sampleCache = DEFAULT_SAMPLE_CACHE;
        boolean refreshSample;
        boolean noSampleCache;
        String sampleSeed = DEFAULT_SAMPLE_SEED;
        boolean clearOutputDir;

        static Arguments parse(final String[] args) {
            final Arguments parsed = new Arguments();
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                switch (arg) {
                    case "--amount" -> parsed.amount = requireValue(args, ++i, arg);
                    case "--output-dir" -> parsed.outputDir = Path.of(requireValue(args, ++i, arg));
                    case "--profile-file" -> parsed.profileFile = Path.of(requireValue(args, ++i, arg));
                    case "--chunk-size" -> parsed.chunkSize = Integer.parseInt(requireValue(args, ++i, arg));
                    case "--api-page-size" -> parsed.apiPageSize = Integer.parseInt(requireValue(args, ++i, arg));
                    case "--sample-cache" -> parsed.sampleCache = Path.of(requireValue(args, ++i, arg));
                    case "--refresh-sample" -> parsed.refreshSample = true;
                    case "--no-sample-cache" -> parsed.noSampleCache = true;
                    case "--sample-seed" -> parsed.sampleSeed = requireValue(args, ++i, arg);
                    case "--clear-output-dir" -> parsed.clearOutputDir = true;
                    case "--help", "-h" -> {
                        printHelp();
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return parsed;
        }

        private static String requireValue(final String[] args, final int index, final String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " requires a value");
            }
            return args[index];
        }

        private static void printHelp() {
            System.out.println("""
                Generate chunked asteroid datapack JSON.
                Finite samples balance six semimajor-axis bands (20/30/20/10/10/10%).
                Sparse bands contribute all available objects; unused shares are redistributed.
                Orbital distances/eccentricities come from JPL; phases remain schematic.
                --amount all streams every supported bound orbit without reweighting.
                Each profile may define a textures array of GUI sprite identifiers.
                Missing, null or empty textures use the default asteroid textures.
                Texture selection is deterministic and does not affect composition.

                Options:
                  --amount <n|all>         Number of asteroids (default: 10000)
                  --output-dir <path>      Generated JSON directory
                  --profile-file <path>    Asteroid resource profile JSON
                  --chunk-size <n>         Asteroids per JSON chunk (default: 5000)
                  --api-page-size <n>      Maximum rows per API request (default: 2500)
                  --sample-cache <path>    Raw JPL sample cache path
                  --refresh-sample         Ignore and replace the sample cache
                  --no-sample-cache        Do not read or write the sample cache
                  --sample-seed <seed>     Stable deterministic sample seed
                  --clear-output-dir       Remove obsolete generated chunks after successful generation
                """);
        }
    }
}
