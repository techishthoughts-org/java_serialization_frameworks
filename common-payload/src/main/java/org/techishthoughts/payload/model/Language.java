package org.techishthoughts.payload.model;

import java.io.Serializable;
import java.util.Objects;

import org.techishthoughts.payload.model.Language.LanguageProficiency;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Language proficiency information.
 */
public class Language implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("code")
    private String code;

    @JsonProperty("proficiency")
    private LanguageProficiency proficiency;

    @JsonProperty("isNative")
    private Boolean isNative;

    public Language() {}

    public Language(Long id, String name, String code, LanguageProficiency proficiency, Boolean isNative) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.proficiency = proficiency;
        this.isNative = isNative;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LanguageProficiency getProficiency() { return proficiency; }
    public void setProficiency(LanguageProficiency proficiency) { this.proficiency = proficiency; }

    public Boolean getIsNative() { return isNative; }
    public void setIsNative(Boolean isNative) { this.isNative = isNative; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Language language = (Language) o;
        return Objects.equals(id, language.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum LanguageProficiency implements Serializable {
        BEGINNER, INTERMEDIATE, ADVANCED, NATIVE, FLUENT
    }
}
