package org.techishthoughts.payload.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User profile information with complex nested data.
 */
public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("avatarUrl")
    private String avatarUrl;

    @JsonProperty("bio")
    private String bio;

    @JsonProperty("dateOfBirth")
    private LocalDate dateOfBirth;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("nationality")
    private String nationality;

    @JsonProperty("occupation")
    private String occupation;

    @JsonProperty("company")
    private String company;

    @JsonProperty("interests")
    private List<String> interests;

    @JsonProperty("skills")
    private List<Skill> skills;

    @JsonProperty("education")
    private List<Education> education;

    @JsonProperty("languages")
    private List<Language> languages;

    public UserProfile() {}

    public UserProfile(String avatarUrl, String bio, LocalDate dateOfBirth, String gender,
                      String phoneNumber, String nationality, String occupation, String company,
                      List<String> interests, List<Skill> skills, List<Education> education,
                      List<Language> languages) {
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.nationality = nationality;
        this.occupation = occupation;
        this.company = company;
        this.interests = interests;
        this.skills = skills;
        this.education = education;
        this.languages = languages;
    }

    // Getters and Setters
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public List<Skill> getSkills() { return skills; }
    public void setSkills(List<Skill> skills) { this.skills = skills; }

    public List<Education> getEducation() { return education; }
    public void setEducation(List<Education> education) { this.education = education; }

    public List<Language> getLanguages() { return languages; }
    public void setLanguages(List<Language> languages) { this.languages = languages; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(bio, that.bio) &&
               Objects.equals(avatarUrl, that.avatarUrl) &&
               Objects.equals(dateOfBirth, that.dateOfBirth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bio, avatarUrl, dateOfBirth);
    }
}
