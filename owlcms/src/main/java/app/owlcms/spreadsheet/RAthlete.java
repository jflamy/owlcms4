package app.owlcms.spreadsheet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Used for registration.
 * Converts from String to data types as required to simplify Excel/CSV imports
 * 
 * @author Jean-François Lamy
 *
 */
public class RAthlete {
	
	final Logger logger = (Logger) LoggerFactory.getLogger(RAthlete.class);
	{logger.setLevel(Level.INFO);}
	
	Athlete a = new Athlete();
	

	public Athlete getAthlete() {
		
		return a;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.data.athlete.Athlete#setAgeDivision(app.owlcms.data.category.AgeDivision)
	 */
	public void setAgeDivision(String ageDivisionName) throws Exception {
		if (ageDivisionName == null) return;
		AgeDivision ageDivision = AgeDivision.getAgeDivisionFromCode(ageDivisionName);
		if (ageDivision == null) {
			throw new Exception("AgeDivision "+ageDivisionName+"is not defined.");
		}
		a.setAgeDivision(ageDivision);
	}
	
	/**
	 * @param category
	 * @throws Exception 
	 * @see app.owlcms.data.athlete.Athlete#setCategory(app.owlcms.data.category.Category)
	 */
	public void setCategory(String categoryName) throws Exception {
		if (categoryName == null) return;
		Category category = CategoryRepository.findByName(categoryName);
		if (category == null) {
			throw new Exception("Category "+categoryName+" is not defined.");
		}
		a.setCategory(category);
	}


	/**
	 * @param firstName
	 * @see app.owlcms.data.athlete.Athlete#setFirstName(java.lang.String)
	 */
	public void setFirstName(String firstName) {
		a.setFirstName(firstName);
	}

	
	/**
	 * @param category
	 * @throws Exception 
	 * @see app.owlcms.data.athlete.Athlete#setCategory(app.owlcms.data.category.Category)
	 */
	public void setFullBirthDate(String content) throws Exception {
		try {
			long l = Long.parseLong(content);
			if (l < 3000) {
				a.setYearOfBirth((int) l);
				logger.trace("short "+l);
			} else {
				LocalDate epoch = LocalDate.of(1900,1,1);
				LocalDate plusDays = epoch.plusDays(l-2); // Excel quirks: 1 is 1900-01-01 and 1900-02-29 did not exist.
				logger.trace("long "+plusDays);
				a.setFullBirthDate(plusDays);
			}
			return;
		} catch (NumberFormatException e) {
			try {
				// try as a ISO Date
				LocalDate parse = LocalDate.parse(content,DateTimeFormatter.ISO_LOCAL_DATE);
				logger.trace("dateString {}",parse);
				
			} catch (DateTimeParseException e1) {
				throw new Exception(content+" is not a 4-digit year or an international yyyy-MM-dd format date like "+
						DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.of(1961, 02, 28)));
			}
		}
	}

//	/**
//	 * @param category
//	 * @throws Exception 
//	 * @see app.owlcms.data.athlete.Athlete#setCategory(app.owlcms.data.category.Category)
//	 */
//	public void setFullBirthDate(Date fullBirthDate) throws Exception {
//		if (fullBirthDate == null) return;
//		LocalDate fbd = fullBirthDate.toInstant()
//			      .atZone(ZoneId.systemDefault())
//			      .toLocalDate();
//		a.setFullBirthDate(fbd);
//	}


	/**
	 * @param lastName
	 * @see app.owlcms.data.athlete.Athlete#setLastName(java.lang.String)
	 */
	public void setGender(String gender) {
		if (gender == null) return;
		a.setGender(Gender.valueOf(gender.toUpperCase()));
	}

	
	/**
	 * @param group
	 * @throws Exception 
	 * @see app.owlcms.data.athlete.Athlete#setGroup(app.owlcms.data.category.Group)
	 */
	public void setGroup(String groupName) throws Exception {
		if (groupName == null) return;
		Group group = GroupRepository.findByName(groupName);
		if (group == null) {
			throw new Exception("Group "+groupName+" is not defined.");
		}
		a.setGroup(group);
	}


	/**
	 * @param lastName
	 * @see app.owlcms.data.athlete.Athlete#setLastName(java.lang.String)
	 */
	public void setLastName(String lastName) {
		a.setLastName(lastName);
	}

	
	/**
	 * @param qualifyingTotal
	 * @see app.owlcms.data.athlete.Athlete#setQualifyingTotal(java.lang.Integer)
	 */
	public void setQualifyingTotal(Integer qualifyingTotal) {
		a.setQualifyingTotal(qualifyingTotal);
	}

	/**
	 * @param lotNumber
	 * @see app.owlcms.data.athlete.Athlete#setLotNumber(java.lang.Integer)
	 */
	public void setLotNumber(String lotNumber) {
		if (lotNumber == null) return;
		a.setLotNumber(Integer.parseInt(lotNumber));
	}

	
	/**
	 * @param membership
	 * @see app.owlcms.data.athlete.Athlete#setMembership(java.lang.String)
	 */
	public void setMembership(String membership) {
		a.setMembership(membership);
	}

	/**
	 * @param club
	 * @see app.owlcms.data.athlete.Athlete#setTeam(java.lang.String)
	 */
	public void setTeam(String club) {
		a.setTeam(club);
	}
	
	

}
