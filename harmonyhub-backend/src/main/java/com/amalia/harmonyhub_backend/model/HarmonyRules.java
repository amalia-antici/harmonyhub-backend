package com.amalia.harmonyhub_backend.model;

import java.util.Map;

public class HarmonyRules {
    private static final Map<MusicalNote, MusicalNote> PAIRS = Map.of(
            MusicalNote.DO,  MusicalNote.SOL,
            MusicalNote.RE,  MusicalNote.LA,
            MusicalNote.MI,  MusicalNote.SI,
            MusicalNote.FA,  MusicalNote.DO,
            MusicalNote.SOL, MusicalNote.RE,
            MusicalNote.LA,  MusicalNote.MI,
            MusicalNote.SI,  MusicalNote.FA
    );

    public static MusicalNote getHarmony(MusicalNote note) {
        return PAIRS.get(note);
    }

    public static boolean isHarmony(MusicalNote sent, MusicalNote received) {
        return PAIRS.getOrDefault(sent, null) == received;
    }
}
