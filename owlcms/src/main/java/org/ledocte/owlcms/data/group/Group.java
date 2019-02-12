package org.ledocte.owlcms.data.group;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.category.Category;
import org.ledocte.owlcms.data.platform.Platform;

public class Group {
	
    private static final SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private String announcer;
    // group is the property in Athlete that is the opposite of Group.athletes
    @OneToMany(mappedBy = "group")
    // ,fetch=FetchType.EAGER)
    Set<Athlete> athletes;
    @ManyToMany(fetch = FetchType.EAGER)
    Set<Category> categories;

    @Transient
    final transient String competitionShortDateTime = "";
    private LocalDateTime competitionTime;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String jury;
    private String marshall;
    private String name;
    @ManyToOne(optional = true)
    Platform platform;
    private String referee1;

    private String referee2;
    private String referee3;

    private String technicalController;

    private String timeKeeper;
    @Transient
    final transient String weighInShortDateTime = "";

    private LocalDateTime weighInTime;

    public Group() {
    }

    public Group(String groupName) {
        this.name = groupName;
        final LocalDateTime now = LocalDateTime.now();
        this.setWeighInTime(now);
        this.setCompetitionTime(now);
    }

    public Group(String groupName, LocalDateTime weighin, LocalDateTime competition) {
        this.name = groupName;
        this.setWeighInTime(weighin);
        this.setCompetitionTime(competition);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Group other = (Group) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public String getAnnouncer() {
        return announcer;
    }

    public Set<Athlete> getAthletes() {
        return athletes;
    }

    /**
     * @return the set of categories for the Group
     */
    public Set<Category> getCategories() {
        return categories;
    }

    /**
     * @return the competition time
     */
    public String getCompetitionShortDateTime() {
        String formatted = "";
        try {
            formatted = sFormat.format(competitionTime);
        } catch (Exception e) {
            //LoggerUtils.errorException(logger, e);
        }
        return formatted;
    }

    /**
     * @return the competition time
     */
    public LocalDateTime getCompetitionTime() {
        return competitionTime;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    public String getJury() {
        return jury;
    }

    public String getMarshall() {
        return marshall;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the platformName on which group will be lifting
     */
    public Platform getPlatform() {
        return platform;
    }

    public String getReferee1() {
        return referee1;
    }

    public String getReferee2() {
        return referee2;
    }

    public String getReferee3() {
        return referee3;
    }

    public String getTechnicalController() {
        return technicalController;
    }

    public String getTimeKeeper() {
        return timeKeeper;
    }

    /**
     * @return the weigh-in time (two hours before competition, normally)
     */
    public String getWeighInShortDateTime() {
        String formatted = "";
        try {
            formatted = sFormat.format(weighInTime);
        } catch (Exception e) {
            //LoggerUtils.errorException(logger, e);
        }
        return formatted;
    }

    /**
     * @return the weigh-in time (two hours before competition, normally)
     */
    public LocalDateTime getWeighInTime() {
        return weighInTime;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    public void setAnnouncer(String announcer) {
        this.announcer = announcer;
    }

    public void setAthletes(Set<Athlete> athletes) {
        this.athletes = athletes;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    /**
     * @param c
     *            the competition time to set
     */
    public void setCompetitionTime(LocalDateTime c) {
        this.competitionTime = c;
    }

    public void setJury(String jury) {
        this.jury = jury;
    }

    public void setMarshall(String announcer) {
        this.marshall = announcer;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param platformName
     *            the platformName to set
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public void setReferee1(String referee1) {
        this.referee1 = referee1;
    }

    public void setReferee2(String referee2) {
        this.referee2 = referee2;
    }

    public void setReferee3(String referee3) {
        this.referee3 = referee3;
    }

    public void setTechnicalController(String technicalController) {
        this.technicalController = technicalController;
    }

    public void setTimeKeeper(String timeKeeper) {
        this.timeKeeper = timeKeeper;
    }

    /**
     * @param w
     *            the weigh-in time to set
     */
    public void setWeighInTime(LocalDateTime w) {
        this.weighInTime = w;
    }


}
