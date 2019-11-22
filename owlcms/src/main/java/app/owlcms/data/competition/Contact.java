package app.owlcms.data.competition;

import java.time.LocalDate;

public class Contact {
    String email;
    String phone;
    String firstName;
    String lastName;
    boolean doNotCall;
    LocalDate birthDate;
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public boolean isDoNotCall() {
        return doNotCall;
    }
    public void setDoNotCall(boolean doNotCall) {
        this.doNotCall = doNotCall;
    }
    public LocalDate getBirthDate() {
        return birthDate;
    }
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
}
