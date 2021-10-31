package app.owlcms.data.xml;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;

public class CompetitionData {

    private List<AgeGroup> ageGroups;
    private List<Athlete> athletes;
    private List<Group> groups;
    private List<Platform> platforms;

    public CompetitionData() {
    }

    public CompetitionData fromDatabase() {
        ageGroups = AgeGroupRepository.findAll();
        athletes = AthleteRepository.findAll();
        groups = GroupRepository.findAll();
        platforms = PlatformRepository.findAll();
        return this;
    }

    @JsonProperty(index = 30)
    public List<AgeGroup> getAgeGroups() {
        return ageGroups;
    }

    @JsonProperty(index = 40)
    public List<Athlete> getAthletes() {
        return athletes;
    }

    @JsonProperty(index = 20)
    public List<Group> getGroups() {
        return groups;
    }

    @JsonProperty(index = 10)
    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setAgeGroups(List<AgeGroup> ageGroups) {
        this.ageGroups = ageGroups;
    }

    public void setAthletes(List<Athlete> athletes) {
        this.athletes = athletes;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }
}
