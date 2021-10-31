package app.owlcms.data.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import ch.qos.logback.classic.Logger;

public class CompetitionData {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionData.class);

    private List<AgeGroup> ageGroups;
    private List<Athlete> athletes;
    private List<Group> groups;
    private List<Platform> platforms;

    public CompetitionData() {
    }

    public CompetitionData fromDatabase() {
        setAgeGroups(AgeGroupRepository.findAll());
        setAthletes(AthleteRepository.findAll());
        setGroups(GroupRepository.findAll());
        setPlatforms(PlatformRepository.findAll());
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

    /**
     * For debugging
     * 
     * @return
     */
    public String exportDataAsString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();
        String serialized;
        try {
            serialized = writerWithDefaultPrettyPrinter.writeValueAsString(this.fromDatabase());
            // System.out.println(serialized);
            return serialized;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream exportData() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();

            PipedOutputStream out = new PipedOutputStream();
            PipedInputStream in = new PipedInputStream(out);
            new Thread(() -> {
                try {
                    writerWithDefaultPrettyPrinter.writeValue(out, this.fromDatabase());
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            return in;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompetitionData importDataFromString(String serialized)
            throws JsonMappingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        CompetitionData newData = mapper.readValue(serialized, CompetitionData.class);
        logger.warn("after unmarshall {}", newData.getPlatforms());
        return newData;
    }

    public CompetitionData importData(InputStream serialized) throws StreamReadException, DatabindException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        CompetitionData newData = mapper.readValue(serialized, CompetitionData.class);
        logger.warn("after unmarshall {}", newData.getPlatforms());
        return newData;
    }
}
