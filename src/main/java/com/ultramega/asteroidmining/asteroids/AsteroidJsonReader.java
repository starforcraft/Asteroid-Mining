package com.ultramega.asteroidmining.asteroids;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/** Reads a single asteroid, an array, or a chunk without retaining the chunk's JSON tree. */
public final class AsteroidJsonReader {
    private AsteroidJsonReader() {
    }

    public static void read(final ReaderSource source,
                            final Predicate<JsonElement> shouldLoad,
                            final Consumer<JsonElement> consumer) throws IOException {
        // Read envelope metadata first: conditions are allowed AFTER the asteroid array.
        // skipValue traverses the array without materializing its objects.
        try (JsonReader reader = new JsonReader(source.open())) {
            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                readArray(reader, shouldLoad, consumer);
                requireEnd(reader);
                return;
            }
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                requireEnd(reader);
                return;
            }
            final JsonObject envelope = new JsonObject();
            boolean chunk = false;
            reader.beginObject();
            while (reader.hasNext()) {
                final String name = reader.nextName();
                if (name.equals("asteroids") && reader.peek() == JsonToken.BEGIN_ARRAY) {
                    if (chunk) {
                        throw new IOException("Duplicate asteroid array");
                    }
                    chunk = true;
                    reader.skipValue();
                } else {
                    envelope.add(name, JsonParser.parseReader(reader));
                }
            }
            reader.endObject();
            requireEnd(reader);
            if (!shouldLoad.test(envelope)) {
                return;
            }
            if (!chunk) {
                consumer.accept(envelope);
                return;
            }
        }

        try (JsonReader reader = new JsonReader(source.open())) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("asteroids")) {
                    readArray(reader, shouldLoad, consumer);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            requireEnd(reader);
        }
    }

    private static void readArray(final JsonReader reader,
                                  final Predicate<JsonElement> shouldLoad,
                                  final Consumer<JsonElement> consumer) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            final JsonElement asteroid = JsonParser.parseReader(reader);
            if (shouldLoad.test(asteroid)) {
                consumer.accept(asteroid);
            }
        }
        reader.endArray();
    }

    private static void requireEnd(final JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.END_DOCUMENT) {
            throw new IOException("Trailing data after asteroid JSON");
        }
    }

    @FunctionalInterface
    public interface ReaderSource {
        Reader open() throws IOException;
    }
}
