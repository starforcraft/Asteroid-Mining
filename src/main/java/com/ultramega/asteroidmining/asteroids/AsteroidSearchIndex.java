package com.ultramega.asteroidmining.asteroids;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lazy ore postings: one bit per matching asteroid, not a resource-name string per deposit. */
public final class AsteroidSearchIndex {
    private final List<AsteroidConfig> asteroids;
    private final String[] names;
    private Map<String, BitSet> ores;

    public AsteroidSearchIndex(final List<AsteroidConfig> asteroids) {
        this.asteroids = asteroids;
        this.names = new String[asteroids.size()];
        for (int i = 0; i < asteroids.size(); i++) {
            this.names[i] = normalize(asteroids.get(i).getName());
        }
    }

    /** Names use substring matching. #iron or ore:iron restricts by resource ID; terms are ANDed. */
    public synchronized List<AsteroidConfig> find(final String query, final int limit) {
        if (query.isBlank() || limit <= 0) {
            return List.of();
        }
        final List<String> nameTerms = new ArrayList<>();
        BitSet matches = null;
        for (final String token : normalize(query).split("\\s+")) {
            final String ore = token.startsWith("#") ? token.substring(1)
                : token.startsWith("ore:") ? token.substring(4) : null;
            if (ore == null) {
                nameTerms.add(token);
                continue;
            }
            if (ore.isBlank()) {
                return List.of();
            }
            final BitSet termMatches = new BitSet();
            for (final Map.Entry<String, BitSet> entry : this.ores().entrySet()) {
                if (entry.getKey().contains(ore)) {
                    termMatches.or(entry.getValue());
                }
            }
            if (matches == null) {
                matches = termMatches;
            } else {
                matches.and(termMatches);
            }
            if (matches.isEmpty()) {
                return List.of();
            }
        }

        final List<AsteroidConfig> results = new ArrayList<>(Math.min(limit, 50));
        for (int i = matches == null ? 0 : matches.nextSetBit(0);
             i >= 0 && i < this.asteroids.size();
             i = matches == null ? i + 1 : matches.nextSetBit(i + 1)) {
            boolean accepted = true;
            for (final String term : nameTerms) {
                if (!this.names[i].contains(term)) {
                    accepted = false;
                    break;
                }
            }
            if (accepted) {
                results.add(this.asteroids.get(i));
                if (results.size() == limit) {
                    break;
                }
            }
        }
        return results;
    }

    private Map<String, BitSet> ores() {
        if (this.ores == null) {
            this.ores = new HashMap<>();
            final Map<Object, String> resourceIds = new HashMap<>();
            for (int i = 0; i < this.asteroids.size(); i++) {
                for (final AsteroidResource resource : this.asteroids.get(i).getComposition()) {
                    if (!resource.isEmpty()) {
                        final Object template = resource instanceof AsteroidResource.ItemEntry item ? item.resource()
                            : ((AsteroidResource.FluidEntry) resource).resource();
                        final String id = resourceIds.computeIfAbsent(template, ignored -> resource.resourceId().toString());
                        this.ores.computeIfAbsent(id, ignored -> new BitSet()).set(i);
                    }
                }
            }
        }
        return this.ores;
    }

    private static String normalize(final String text) {
        return text.strip().toLowerCase(Locale.ROOT);
    }
}
