package org.utility;

import java.text.MessageFormat;

public class TypedValue {
    private final TypedValue.Type type;
    private final TypedValue.Resolution resolution;
    private final String usedValue;
    private final String rawValue;
    private final Class<? extends Enum<?>> enumClass;

    private boolean used;

    public TypedValue(TypedValue.Type type, TypedValue.Resolution resolution, String usedValue, String rawValue, Class<? extends Enum<?>> enumClass) {
        this.type = type;
        this.resolution = resolution;
        this.usedValue = usedValue;
        this.rawValue = rawValue;
        this.enumClass = enumClass;

        this.used = false;
    }

    @Override
    public String toString() {
        return this.rawValue;
    }

    public String getStateMessage(String modifierName, boolean includeDefaultValueUsage) {
        String message = "";
        switch (this.resolution) {
            case MODIFIER_MISSING -> message = MessageFormat.format("Modifier \"{0}\" was not found", modifierName);
            case ARGUMENT_MISSING -> message = MessageFormat.format("Argument for modifier \"{0}\" was not found", modifierName);
            case ARGUMENT_INVALID -> message = MessageFormat.format("Argument \"{0}\" for modifier \"{1}\" is not valid", this.rawValue, modifierName);
            case ARGUMENT_VALID -> throw new IllegalStateException(MessageFormat.format("Cannot generate state message when resolution is set to\"{0}\"", this.resolution));
        }

        if (includeDefaultValueUsage && this.type != Type.NULL) {
            message += MessageFormat.format(", default value \"{0}\" was used instead", this.usedValue);
        }

        return message;
    }

    public String getTrimmedUsedValue(ProcessingContext processingContext, String valueName) {
        boolean hasLeadingWhitespace = this.usedValue.startsWith(" ");
        boolean hasTrailingWhitespace = this.usedValue.endsWith(" ");
        if (hasLeadingWhitespace && hasTrailingWhitespace) {
            processingContext.addMessages("Trailing and leading whitespace(s) were removed in the provided " + valueName, ProcessingContext.MessageType.WARNING);
        } else if (hasLeadingWhitespace) {
            processingContext.addMessages("Leading whitespace(s) were removed in the provided " + valueName, ProcessingContext.MessageType.WARNING);
        } else if (hasTrailingWhitespace) {
            processingContext.addMessages("Trailing whitespace(s)were removed in the provided" + valueName, ProcessingContext.MessageType.WARNING);
        }

        return this.usedValue.trim();
    }

    public String getTrimmedNormalizedUsedValue(ProcessingContext processingContext, String valueName) {
        String trimmedUsedValue = this.getTrimmedUsedValue(processingContext, valueName);

        String noNewLinesValue = trimmedUsedValue.replaceAll("[\\t\\n\\r]+"," ");
        if (!trimmedUsedValue.equals(noNewLinesValue)) {
            processingContext.addMessages("Line breaks and tabs were replaced with a single space in the provided " + valueName, ProcessingContext.MessageType.WARNING);
        }

        String normalizedUsedValue = noNewLinesValue.replaceAll(" +", " ");
        if (!noNewLinesValue.equals(normalizedUsedValue)) {
            processingContext.addMessages("Multiple consecutive spaces were replaced with a single space in the provided " + valueName, ProcessingContext.MessageType.WARNING);
        }

        return normalizedUsedValue;
    }

    public String getTrimmedNormalizedLowercaseUsedValue(ProcessingContext processingContext, String valueName) {
        String normalizedUsedValue = this.getTrimmedNormalizedUsedValue(processingContext, valueName);
        String lowercaseUsedValue = normalizedUsedValue.toLowerCase();

        if (!normalizedUsedValue.equals(lowercaseUsedValue)) {
            processingContext.addMessages("Uppercase letters were converted to lowercase in the provided " + valueName, ProcessingContext.MessageType.WARNING);
        }

        return lowercaseUsedValue;
    }

    public TypedValue.Type getType() {
        return this.type;
    }

    public TypedValue.Resolution getResolution() {
        return this.resolution;
    }

    public String getUsedValue() {
        return this.usedValue;
    }

    public Class<? extends Enum<?>> getEnumClass() {
        return this.enumClass;
    }

    public boolean getUsed() {
        return this.used;
    }

    public void setUsed() {
        this.used = true;
    }

    public enum Type { STRING, WHOLE_NUMBER, DECIMAL_NUMBER, ENUMERATOR, SWITCH, NULL }
    public enum Resolution { MODIFIER_MISSING, ARGUMENT_MISSING, ARGUMENT_INVALID, ARGUMENT_VALID }
}
