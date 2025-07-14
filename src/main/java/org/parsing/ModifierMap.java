package org.parsing;

import org.utility.TypedValue;

import java.util.*;

public class ModifierMap {
    private final Map<Enum<?>, List<TypedValue>> modifiers;
    private final Set<Enum<?>> accessedModifiers;
    private final Set<Enum<?>> addedAfterParsingModifiers;

    public ModifierMap() {
        this.modifiers = new HashMap<>();
        this.accessedModifiers = new HashSet<>();
        this.addedAfterParsingModifiers = new HashSet<>();
    }

    public boolean containsKey(Enum<?> key) {
        return this.modifiers.containsKey(key);
    }

    public void putIfAbsent(Enum<?> key, List<TypedValue> value, boolean addedAfterParsing) {
        if (addedAfterParsing) {
            this.addedAfterParsingModifiers.add(key);
        }

        this.modifiers.putIfAbsent(key, value);
    }

    public List<TypedValue> get(Enum<?> key) {
        if (this.modifiers.containsKey(key)) {
            this.accessedModifiers.add(key);
        }

        return this.modifiers.get(key);
    }

    public Map<Enum<?>, List<TypedValue>> getModifiers() {
        return this.modifiers;
    }

    public Set<Enum<?>> getAccessedModifiers() {
        return this.accessedModifiers;
    }

    public Set<Enum<?>> getAddedAfterParsingModifiers() {
        return this.addedAfterParsingModifiers;
    }
}
