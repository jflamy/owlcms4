/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.data.group;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.platform.Platform;

/**
 * The Class Group.
 */
@Entity(name="CompetitionGroup")
@Cacheable
public class Group implements Comparable<Group> {
	
    private static final SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private String announcer;
    
    /** The athletes. */
    // group is the property in Athlete that is the opposite of Group.athletes
    @OneToMany(cascade={CascadeType.MERGE}, mappedBy = "group", fetch = FetchType.LAZY)
    // ,fetch=FetchType.EAGER)
    Set<Athlete> athletes;
    
    /** The categories. */
    @ManyToMany(cascade={CascadeType.MERGE}, fetch = FetchType.LAZY)
    Set<Category> categories;

    /** The competition short date time. */
    @Transient
    final transient String competitionShortDateTime = "";
    private LocalDateTime competitionTime;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String jury;
    private String marshall;
    private String name;
    
    /** The platform. */
    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.MERGE}, optional = true, fetch = FetchType.LAZY)
    Platform platform;
    
    private String referee1;
    private String referee2;
    private String referee3;

    private String technicalController;

    private String timeKeeper;
    
    /** The weigh in short date time. */
    @Transient
    final transient String weighInShortDateTime = "";

    private LocalDateTime weighInTime;

    /**
     * Instantiates a new group.
     */
    public Group() {
    }

    /**
     * Instantiates a new group.
     *
     * @param groupName the group name
     */
    public Group(String groupName) {
        this.name = groupName;
        final LocalDateTime now = LocalDateTime.now();
        this.setWeighInTime(now);
        this.setCompetitionTime(now);
    }

    /**
     * Instantiates a new group.
     *
     * @param groupName the group name
     * @param weighin the weighin
     * @param competition the competition
     */
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

    /* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Group obj) {
        if (this == obj)
            return 0;
        if (obj == null)
            return -1;
        Group other = obj;
        if (name == null) {
            if (other.name != null)
                return 1;
            else
            	return 0;
        } else {
            if (other.name != null)
                return name.compareTo(other.name);
            else // this != null other.name == null
            	return -1;
        }
	}

	/**
     * Gets the announcer.
     *
     * @return the announcer
     */
    public String getAnnouncer() {
        return announcer;
    }

    /**
     * Gets the athletes.
     *
     * @return the athletes
     */
    public Set<Athlete> getAthletes() {
        return athletes;
    }

    /**
     * Gets the categories.
     *
     * @return the set of categories for the Group
     */
    public Set<Category> getCategories() {
        return categories;
    }

    /**
     * Gets the competition short date time.
     *
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
     * Gets the competition time.
     *
     * @return the competition time
     */
    public LocalDateTime getCompetitionTime() {
        return competitionTime;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the jury.
     *
     * @return the jury
     */
    public String getJury() {
        return jury;
    }

    /**
     * Gets the marshall.
     *
     * @return the marshall
     */
    public String getMarshall() {
        return marshall;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the platform.
     *
     * @return the platformName on which group will be lifting
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * Gets the referee 1.
     *
     * @return the referee 1
     */
    public String getReferee1() {
        return referee1;
    }

    /**
     * Gets the referee 2.
     *
     * @return the referee 2
     */
    public String getReferee2() {
        return referee2;
    }

    /**
     * Gets the referee 3.
     *
     * @return the referee 3
     */
    public String getReferee3() {
        return referee3;
    }

    /**
     * Gets the technical controller.
     *
     * @return the technical controller
     */
    public String getTechnicalController() {
        return technicalController;
    }

    /**
     * Gets the time keeper.
     *
     * @return the time keeper
     */
    public String getTimeKeeper() {
        return timeKeeper;
    }

    /**
     * Gets the weigh in short date time.
     *
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
     * Gets the weigh in time.
     *
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

    /**
     * Sets the announcer.
     *
     * @param announcer the new announcer
     */
    public void setAnnouncer(String announcer) {
        this.announcer = announcer;
    }

    /**
     * Sets the athletes.
     *
     * @param athletes the new athletes
     */
    public void setAthletes(Set<Athlete> athletes) {
        this.athletes = athletes;
    }

    /**
     * Sets the categories.
     *
     * @param categories the new categories
     */
    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    /**
     * Sets the competition time.
     *
     * @param c            the competition time to set
     */
    public void setCompetitionTime(LocalDateTime c) {
        this.competitionTime = c;
    }

    /**
     * Sets the jury.
     *
     * @param jury the new jury
     */
    public void setJury(String jury) {
        this.jury = jury;
    }

    /**
     * Sets the marshall.
     *
     * @param announcer the new marshall
     */
    public void setMarshall(String announcer) {
        this.marshall = announcer;
    }

    /**
     * Sets the name.
     *
     * @param name            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the platform.
     *
     * @param platform the new platform
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    /**
     * Sets the referee 1.
     *
     * @param referee1 the new referee 1
     */
    public void setReferee1(String referee1) {
        this.referee1 = referee1;
    }

    /**
     * Sets the referee 2.
     *
     * @param referee2 the new referee 2
     */
    public void setReferee2(String referee2) {
        this.referee2 = referee2;
    }

    /**
     * Sets the referee 3.
     *
     * @param referee3 the new referee 3
     */
    public void setReferee3(String referee3) {
        this.referee3 = referee3;
    }

    /**
     * Sets the technical controller.
     *
     * @param technicalController the new technical controller
     */
    public void setTechnicalController(String technicalController) {
        this.technicalController = technicalController;
    }

    /**
     * Sets the time keeper.
     *
     * @param timeKeeper the new time keeper
     */
    public void setTimeKeeper(String timeKeeper) {
        this.timeKeeper = timeKeeper;
    }

    /**
     * Sets the weigh in time.
     *
     * @param w            the weigh-in time to set
     */
    public void setWeighInTime(LocalDateTime w) {
        this.weighInTime = w;
    }

    @Override
	public String toString() {
    	return getName();
    }



}
