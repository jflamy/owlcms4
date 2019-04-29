/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.competition;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

import javax.persistence.Cacheable;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.slf4j.LoggerFactory;

import app.owlcms.data.jpa.LocaleAttributeConverter;
import ch.qos.logback.classic.Logger;

/**
 * Class Competition.
 */
@Cacheable
@Entity
public class Competition {
	final static private Logger logger = (Logger) LoggerFactory.getLogger(Competition.class);

	private static Competition competition;
	
	/**
	 * Gets the current.
	 *
	 * @return the current
	 */
	public static Competition getCurrent() {
		if (competition == null) {
//			competition = new Competition();
			competition = CompetitionRepository.findAll().get(0);
		}
		return competition;
	}
	
	public static void setCurrent(Competition c) {
		competition = c;
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;	

	private String competitionName;
	private LocalDate competitionDate = null;
	private String competitionOrganizer;
	private String competitionSite;
	private String competitionCity;

	private String federation;
	private String federationAddress;
	private String federationEMail;
	private String federationWebSite;
	
	@Convert(converter = LocaleAttributeConverter.class)
	private Locale defaultLocale = Locale.ENGLISH;
	private String protocolFileName;
	private String resultTemplateFileName;
	
	private boolean enforce20kgRule = true;
	private boolean masters = false;
	private boolean useBirthYear = false;
	
	private boolean useCategorySinclair = false;
	private boolean useOld20_15Rule = false;
	private boolean useOldBodyWeightTieBreak = false;
	private boolean useRegistrationCategory = true;
	

	/**
	 * Gets the competition city.
	 *
	 * @return the competition city
	 */
	public String getCompetitionCity() {
		return competitionCity;
	}

	/**
	 * Gets the competition date.
	 *
	 * @return the competition date
	 */
	public LocalDate getCompetitionDate() {
		return competitionDate;
	}

	/**
	 * Gets the competition name.
	 *
	 * @return the competition name
	 */
	public String getCompetitionName() {
		return competitionName;
	}

	/**
	 * Gets the competition organizer.
	 *
	 * @return the competition organizer
	 */
	public String getCompetitionOrganizer() {
		return competitionOrganizer;
	}

	/**
	 * Gets the competition site.
	 *
	 * @return the competition site
	 */
	public String getCompetitionSite() {
		return competitionSite;
	}

	/**
	 * Gets the default locale.
	 *
	 * @return the default locale
	 */
	public Locale getDefaultLocale() {
		return defaultLocale ;
	}

	/**
	 * Gets the federation.
	 *
	 * @return the federation
	 */
	public String getFederation() {
		return federation;
	}

	/**
	 * Gets the federation address.
	 *
	 * @return the federation address
	 */
	public String getFederationAddress() {
		return federationAddress;
	}

	/**
	 * Gets the federation E mail.
	 *
	 * @return the federation E mail
	 */
	public String getFederationEMail() {
		return federationEMail;
	}

	/**
	 * Gets the federation web site.
	 *
	 * @return the federation web site
	 */
	public String getFederationWebSite() {
		return federationWebSite;
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
	 * Gets the invited if born before.
	 *
	 * @return the invited if born before
	 */
	public Integer getInvitedIfBornBefore() {
		return 0;
	}

	/**
	 * Gets the masters.
	 *
	 * @return the masters
	 */
	public Boolean getMasters() {
		return masters;
	}

	/**
	 * Gets the protocol file name.
	 *
	 * @return the protocol file name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getProtocolFileName() throws IOException {
		logger.debug("protocolFileName = {}", protocolFileName);
		if (protocolFileName == null) return null; 
		
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

	/**
	 * Gets the result template file name.
	 *
	 * @return the result template file name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getFinalPackageTemplateFileName() throws IOException {
		logger.debug("competitionBookFileName = {}", resultTemplateFileName);
		String str = File.separator + "competitionBook";
		if (resultTemplateFileName == null) return "defaultResults.xls";
		
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

	/**
	 * Checks if is enforce 20 kg rule.
	 *
	 * @return true, if is enforce 20 kg rule
	 */
	public boolean isEnforce20kgRule() {
		return enforce20kgRule;
	}

	/**
	 * Checks if is masters.
	 *
	 * @return true, if is masters
	 */
	public boolean isMasters() {
		return masters;
	}

	/**
	 * Checks if is use birth year.
	 *
	 * @return the useBirthYear
	 */
	public boolean isUseBirthYear() {
		return useBirthYear;
	}

	/**
	 * Checks if is use category sinclair.
	 *
	 * @return true, if is use category sinclair
	 */
	public boolean isUseCategorySinclair() {
		return useCategorySinclair;
	}

	/**
	 * Checks if is use old 20 15 rule.
	 *
	 * @return true, if is use old 20 15 rule
	 */
	public boolean isUseOld20_15rule() {
		return useOld20_15Rule;
	}

	/**
	 * Checks if is use old body weight tie break.
	 *
	 * @return true, if is use old body weight tie break
	 */
	public boolean isUseOldBodyWeightTieBreak() {
		return useOldBodyWeightTieBreak;
	}

	/**
	 * Checks if is use registration category.
	 *
	 * @return true, if is use registration category
	 */
	public boolean isUseRegistrationCategory() {
		return useRegistrationCategory;
	}

	/**
	 * Sets the competition city.
	 *
	 * @param competitionCity the new competition city
	 */
	public void setCompetitionCity(String competitionCity) {
		this.competitionCity = competitionCity;
	}

	/**
	 * Sets the competition date.
	 *
	 * @param localDate the new competition date
	 */
	public void setCompetitionDate(LocalDate localDate) {
		this.competitionDate = localDate;
	}

	/**
	 * Sets the competition name.
	 *
	 * @param competitionName the new competition name
	 */
	public void setCompetitionName(String competitionName) {
		this.competitionName = competitionName;
	}

	/**
	 * Sets the competition organizer.
	 *
	 * @param competitionOrganizer the new competition organizer
	 */
	public void setCompetitionOrganizer(String competitionOrganizer) {
		this.competitionOrganizer = competitionOrganizer;
	}

	/**
	 * Sets the competition site.
	 *
	 * @param competitionSite the new competition site
	 */
	public void setCompetitionSite(String competitionSite) {
		this.competitionSite = competitionSite;
	}

	/**
	 * Sets the federation.
	 *
	 * @param federation the new federation
	 */
	public void setFederation(String federation) {
		this.federation = federation;
	}

	/**
	 * Sets the federation address.
	 *
	 * @param federationAddress the new federation address
	 */
	public void setFederationAddress(String federationAddress) {
		this.federationAddress = federationAddress;
	}

	/**
	 * Sets the federation E mail.
	 *
	 * @param federationEMail the new federation E mail
	 */
	public void setFederationEMail(String federationEMail) {
		this.federationEMail = federationEMail;
	}

	/**
	 * Sets the federation web site.
	 *
	 * @param federationWebSite the new federation web site
	 */
	public void setFederationWebSite(String federationWebSite) {
		this.federationWebSite = federationWebSite;
	}

	/**
	 * Sets the invited if born before.
	 *
	 * @param invitedIfBornBefore the new invited if born before
	 */
	public void setInvitedIfBornBefore(Integer invitedIfBornBefore) {
	}

	/**
	 * Sets the masters.
	 *
	 * @param masters the new masters
	 */
	public void setMasters(Boolean masters) {
		this.masters = masters;
	}

	/**
	 * Sets the protocol file name.
	 *
	 * @param protocolFileName the new protocol file name
	 */
	public void setProtocolFileName(String protocolFileName) {
		this.protocolFileName = protocolFileName;
	}

	/**
	 * Sets the result template file name.
	 *
	 * @param resultTemplateFileName the new result template file name
	 */
	public void setResultTemplateFileName(String resultTemplateFileName) {
		this.resultTemplateFileName = resultTemplateFileName;
	}

	/**
	 * Sets the use birth year.
	 *
	 * @param b the new use birth year
	 */
	public void setUseBirthYear(boolean b) {
		this.useBirthYear = true;
	}

	/**
	 * Sets the use registration category.
	 *
	 * @param useRegistrationCategory the useRegistrationCategory to set
	 */
	public void setUseRegistrationCategory(boolean useRegistrationCategory) {
		this.useRegistrationCategory = useRegistrationCategory;
	}

	/**
	 * Gets the locale.
	 *
	 * @return the locale
	 */
	public Locale getLocale() {
		return getDefaultLocale();
	}

	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}
	

}
