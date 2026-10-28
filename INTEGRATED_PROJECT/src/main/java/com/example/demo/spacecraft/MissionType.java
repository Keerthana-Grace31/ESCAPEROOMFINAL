package com.example.demo.spacecraft;

import java.util.Locale;
import java.util.Optional;

/** The two spacecraft destinations. */
public enum MissionType {
    MARS("Mars", "Red Planet Expedition"),
    LUNAR("Lunar", "Moon Base Emergency");

    private final String label;
    private final String subtitle;

    MissionType(String label, String subtitle) {
        this.label = label;
        this.subtitle = subtitle;
    }

    public String label() { return label; }
    public String subtitle() { return subtitle; }
    public String slug() { return name().toLowerCase(Locale.ROOT); }

    public static Optional<MissionType> fromSlug(String slug) {
        if (slug == null) return Optional.empty();
        for (MissionType t : values()) {
            if (t.slug().equalsIgnoreCase(slug.trim())) return Optional.of(t);
        }
        return Optional.empty();
    }
}
