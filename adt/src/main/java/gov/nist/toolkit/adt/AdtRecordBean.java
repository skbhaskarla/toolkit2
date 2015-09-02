package gov.nist.toolkit.adt;

import java.sql.SQLException;
import java.util.Collection;

public class AdtRecordBean {

    private AdtRecord record = null;
    private Hl7Race currentRace = null;
    private int uuidCharsToUse = 15;

    public static final String PATIENT_ID_DOMAIN_SEPARATOR = "^^^";
    public static final String FAKE_PATIENT_ID_PREFIX = "";

    /** Creates a new instance of AdtRecordBean */
    public AdtRecordBean() throws ClassNotFoundException, SQLException {
        record = new AdtRecord();
    }

    public String getPatientId() {
        return record.getPatientId();
    }
    public void setPatientId(String patientId) {
        record.setPatientId(patientId);
    }

//    public void setPatientIdAutoGenerated(String domain) {
//        record.setPatientId(this.FAKE_PATIENT_ID_PREFIX + AdtRecordBean.firstNChars(AdtRecordBean.stripUuid(this.getId()),uuidCharsToUse) + AdtRecordBean.PATIENT_ID_DOMAIN_SEPARATOR + domain);
//    }

//    public String getTimestamp() {
//        return record.getTimestamp();
//    }
//    public void setTimestamp(String timestamp) {
//        record.setTimestamp(timestamp);
//    }

    public void setUuidChars(int n) {
        uuidCharsToUse = n;
    }

    public String getPatientAdminSex() {
        return record.getPatientAdminSex();
    }
    public void setPatientAdminSex(String patientAdminSex) {
        record.setPatientAdminSex(patientAdminSex);
    }
    public String getPatientAccountNumber() {
        return record.getPatientAccountNumber();
    }
    public void setPatientAccountNumber(String patientAccountNumber) {
        record.setPatientAccountNumber(patientAccountNumber);
    }
    public String getPatientBedId() {
        return record.getPatientBedId();
    }
    public void setPatientBedId(String patientBedId) {
        record.setPatientBedId(patientBedId);
    }
    public String getPatientBirthDateTime() {
        return record.getPatientBirthDateTime();
    }
    public void setPatientBirthDateTime(String patientBirthDateTime) {
        record.setPatientBirthDateTime(patientBirthDateTime);
    }

    public Collection getPatientRace() {
        return record.getPatientRace();
    }

    public void setPatientRace(Collection patientRace) {
        record.setPatientRace(patientRace);
    }

//    public void setAddRace(String race) {
//        this.setAddRace(new Hl7Race(this.getId(),race));
//    }
//    public void setAddRace(Hl7Race race) {
//        record.getPatientRace().add(race);
//    }

    public Collection getPatientNames() {
        return record.getPatientNames();
    }

    public void setPatientNames(Collection patientNames) {
        record.setPatientNames(patientNames);
    }

    public void setAddName(Hl7Name name) {
        record.getPatientNames().add(name);
    }

//    public void setAddName(String familyName, String givenName, String secondAndFurtherName,
//                           String suffix, String prefix, String degree) {
//
//        Hl7Name name = new Hl7Name(this.getId(),familyName, givenName, secondAndFurtherName,
//                suffix, prefix, degree);
//        this.setAddName(name);
//    }


    public Collection getPatientAddresses() {
        return record.getPatientAddresses();
    }

    public void setPatientAddresses(Collection patientAddress) {
        record.setPatientAddresses(patientAddress);
    }

    public void setAddAddress(Hl7Address address) {
        record.getPatientAddresses().add(address);
    }

//    public void setAddAddress(String streetAddress, String otherDesignation, String city, String stateOrProvince,
//                              String zipCode, String country, String countyOrParish) {
//        Hl7Address address = new Hl7Address(this.getId(), streetAddress, otherDesignation, city, stateOrProvince,
//                zipCode, country, countyOrParish);
//        this.setAddAddress(address);
//    }

//    public void setSaveToDatabase(String ignore) throws SQLException, ClassNotFoundException {
//        record.saveToDatabase();
//    }
//
//    public String getId() {
//        return record.getUuid();
//    }

    public AdtRecord getRecord() {
        return record;
    }

    public void setRecord(AdtRecord record) {
        this.record = record;
    }
    static public String stripUuid(String uuid) {
        String withoutColons = uuid.substring(uuid.lastIndexOf(':') + 1);
        StringBuffer withoutHyphens = new StringBuffer();
        for(int i = 0; i < withoutColons.length(); i++) {
            if(withoutColons.charAt(i) != '-')
                withoutHyphens.append(withoutColons.charAt(i));
        }
        return withoutHyphens.toString();
    }

    static public String firstNChars(String str, int n) {
        return str.substring(0, n);
    }
}
