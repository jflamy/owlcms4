package org.ledocte.owlcms.data.competition;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

@Cacheable
@Entity
public class Competition {
	final static private Logger logger = (Logger) LoggerFactory.getLogger(Competition.class);

	private static Competition competition;
	
	public static Competition getCurrent() {
		if (competition == null) {
			competition = new Competition();
		}
		return competition;
	}
	

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;	
	
	private String competitionCity;
	private LocalDate competitionDate = null;
	private String competitionName;
	private String competitionOrganizer;
	private String competitionSite;
	private Locale defaultLocale = Locale.ENGLISH;
	private boolean enforce20kgRule = true;
	private String federation;
	private String federationAddress;
	private String federationEMail;
	private String federationWebSite;
	private Integer invitedIfBornBefore;
	private boolean masters = false;
	private String protocolFileName;
	private String resultTemplateFileName;
	private boolean useCategorySinclair = false;
	private boolean useOld20_15Rule = false;
	private boolean useOldBodyWeightTieBreak = false;
	private boolean useRegistrationCategory = true;

	private boolean useBirthYear;


	public String getCompetitionCity() {
		return competitionCity;
	}

	public LocalDate getCompetitionDate() {
		return competitionDate;
	}

	public String getCompetitionName() {
		return competitionName;
	}

	public String getCompetitionOrganizer() {
		return competitionOrganizer;
	}

	public String getCompetitionSite() {
		return competitionSite;
	}

	public Locale getDefaultLocale() {
		return defaultLocale ;
	}

	public String getFederation() {
		return federation;
	}

	public String getFederationAddress() {
		return federationAddress;
	}

	public String getFederationEMail() {
		return federationEMail;
	}

	public String getFederationWebSite() {
		return federationWebSite;
	}

	public Long getId() {
		return id;
	}

	public Integer getInvitedIfBornBefore() {
		if (invitedIfBornBefore == null)
			return 0;
		return invitedIfBornBefore;
	}

	public Boolean getMasters() {
		return masters;
	}

	public String getProtocolFileName() throws IOException {
		logger.debug("protocolFileName = {}", protocolFileName);
		String str = File.separator + "protocolSheet";
		int protocolPos = protocolFileName.indexOf(str);
		if (protocolPos != -1) {
			// make file relative
//	            String substring = protocolFileName.substring(protocolPos+1);
//	            logger.debug("relative protocolFileName = {}",substring);
//	            return Competition.getCurrent().getResourceFileName(substring);
			return "not implemented";
		} else {
			logger.debug("could not find {}", str);
		}
		return "not implemented";
	}

	public String getResultTemplateFileName() throws IOException {
		logger.debug("competitionBookFileName = {}", resultTemplateFileName);
		String str = File.separator + "competitionBook";
		int protocolPos = resultTemplateFileName.indexOf(str);
		if (protocolPos != -1) {
			// make file relative
			String substring = resultTemplateFileName.substring(protocolPos + 1);
			logger.debug("relative competitionBookFileName = {}", substring);
			return "not implemented";
		} else {
			logger.debug("could not find {}", str);
		}
		return "not implemented";
	}

	public boolean isEnforce20kgRule() {
		return enforce20kgRule;
	}

	public boolean isMasters() {
		return masters;
	}

	/**
	 * @return the useBirthYear
	 */
	public boolean isUseBirthYear() {
		return useBirthYear;
	}

	public boolean isUseCategorySinclair() {
		return useCategorySinclair;
	}

	public boolean isUseOld20_15rule() {
		return useOld20_15Rule;
	}

	public boolean isUseOldBodyWeightTieBreak() {
		return useOldBodyWeightTieBreak;
	}

	public boolean isUseRegistrationCategory() {
		return useRegistrationCategory;
	}

	public void setCompetitionCity(String competitionCity) {
		this.competitionCity = competitionCity;
	}

	public void setCompetitionDate(LocalDate localDate) {
		this.competitionDate = localDate;
	}

	public void setCompetitionName(String competitionName) {
		this.competitionName = competitionName;
	}

	public void setCompetitionOrganizer(String competitionOrganizer) {
		this.competitionOrganizer = competitionOrganizer;
	}

	public void setCompetitionSite(String competitionSite) {
		this.competitionSite = competitionSite;
	}

	public void setFederation(String federation) {
		this.federation = federation;
	}

	public void setFederationAddress(String federationAddress) {
		this.federationAddress = federationAddress;
	}

	public void setFederationEMail(String federationEMail) {
		this.federationEMail = federationEMail;
	}

	public void setFederationWebSite(String federationWebSite) {
		this.federationWebSite = federationWebSite;
	}

	public void setInvitedIfBornBefore(Integer invitedIfBornBefore) {
		this.invitedIfBornBefore = invitedIfBornBefore;
	}

	public void setMasters(Boolean masters) {
		this.masters = masters;
	}

	public void setProtocolFileName(String protocolFileName) {
		this.protocolFileName = protocolFileName;
	}

	public void setResultTemplateFileName(String resultTemplateFileName) {
		this.resultTemplateFileName = resultTemplateFileName;
	}

	public void setUseBirthYear(boolean b) {
		this.useBirthYear = true;
	}

	/**
	 * @param useRegistrationCategory the useRegistrationCategory to set
	 */
	public void setUseRegistrationCategory(boolean useRegistrationCategory) {
		this.useRegistrationCategory = useRegistrationCategory;
	}

	public Locale getLocale() {
		return getDefaultLocale();
	}

}
