package org.parsing;

import org.javatuples.Quartet;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class IndexedMessage {
    private final List<TypedCharacter> typedCharacters;

    public IndexedMessage() {
        this.typedCharacters = new ArrayList<>();
    }

    public void addCharacter(char c, TypedCharacter.CharacterType characterType) {
        this.typedCharacters.add(new TypedCharacter(c, characterType));
    }

    public Quartet<Integer, Integer, String, List<TypedCharacter>> getTokenStartingFromIndex(int index) {
        if (index >= this.typedCharacters.size()
                || (index == this.typedCharacters.size() - 1 &&  this.typedCharacters.get(index).characterType == TypedCharacter.CharacterType.SENTENCE_END)) {
            return null;
        }

        int startIndex = index;
        TypedCharacter typedCharacter = this.typedCharacters.get(startIndex);
        while (typedCharacter.characterType == TypedCharacter.CharacterType.SENTENCE_END
                || typedCharacter.character == ' ') {
            ++startIndex;
            typedCharacter = this.typedCharacters.get(startIndex);
        }


        int endIndex;
        if (typedCharacter.characterType == TypedCharacter.CharacterType.SENTENCE_START) {
            endIndex = IntStream.range(startIndex, this.typedCharacters.size())
                    .filter(i -> this.typedCharacters.get(i).characterType == TypedCharacter.CharacterType.SENTENCE_END)
                    .findFirst()
                    .getAsInt();
            ++startIndex;
        } else {
            OptionalInt potentialEndIndex = IntStream.range(startIndex, this.typedCharacters.size())
                    .filter(i -> this.typedCharacters.get(i).character == ' ')
                    .findFirst();
            endIndex = potentialEndIndex.isEmpty() ? this.typedCharacters.size() : potentialEndIndex.getAsInt();
        }

        StringBuilder stringBuilder = new StringBuilder();
        List<TypedCharacter> indexedString = new ArrayList<>();
        this.typedCharacters.subList(startIndex, endIndex).forEach(element -> {
            stringBuilder.append(element.character); indexedString.add(element);
        });

        if (indexedString.isEmpty()) {
            indexedString.add(new TypedCharacter(null, TypedCharacter.CharacterType.NULL));
        }

        return new Quartet<>(startIndex, endIndex, stringBuilder.toString(), indexedString);
    }

    public List<TypedCharacter> getTypedCharacters() {
        return this.typedCharacters;
    }

    public record TypedCharacter(Character character, CharacterType characterType) {
        public enum CharacterType{ NULL, LITERAL, MODIFIER, SENTENCE_START, SENTENCE_END }
    }
}
