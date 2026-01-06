package com.ausganslage.ausgangslageBackend.exception;

public class DuplicateResourceException extends RuntimeException {

    private final String resourceType;
    private final String duplicateField;
    private final Object duplicateValue;

    public DuplicateResourceException(String resourceType, String duplicateField, Object duplicateValue) {
        super(String.format("%s with %s '%s' already exists", resourceType, duplicateField, duplicateValue));
        this.resourceType = resourceType;
        this.duplicateField = duplicateField;
        this.duplicateValue = duplicateValue;
    }

    public DuplicateResourceException(String message) {
        super(message);
        this.resourceType = null;
        this.duplicateField = null;
        this.duplicateValue = null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getDuplicateField() {
        return duplicateField;
    }

    public Object getDuplicateValue() {
        return duplicateValue;
    }
}

