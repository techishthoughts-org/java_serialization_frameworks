package org.techishthoughts.payload.model;

import java.io.Serializable;
import java.util.Objects;

import org.techishthoughts.payload.model.Skill.SkillLevel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Skill information with proficiency levels.
 */
public class Skill implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("level")
    private SkillLevel level;

    @JsonProperty("yearsOfExperience")
    private Integer yearsOfExperience;

    @JsonProperty("certifications")
    private String certifications;

    public Skill() {}

    public Skill(Long id, String name, SkillLevel level, Integer yearsOfExperience, String certifications) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.yearsOfExperience = yearsOfExperience;
        this.certifications = certifications;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SkillLevel getLevel() { return level; }
    public void setLevel(SkillLevel level) { this.level = level; }

    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getCertifications() { return certifications; }
    public void setCertifications(String certifications) { this.certifications = certifications; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return Objects.equals(id, skill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public enum SkillLevel implements Serializable {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, MASTER
    }
}
