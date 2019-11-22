package app.owlcms.data.competition;

import java.time.LocalDate;

public class Contact {
    String email;
    String phone;
    String firstName;
    String lastName;
    boolean doNotCall;
    LocalDate birthDate;

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isDoNotCall() {
        return doNotCall;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setDoNotCall(boolean doNotCall) {
        this.doNotCall = doNotCall;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

}
