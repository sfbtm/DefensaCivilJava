package com.defensacivil.dto;

public class MemberDTO {
    private int id;
    private String names;
    private String lastNames;
    private String relationship;
    private String phone;
    private int genderId;
    private int documentTypeId;
    private int nationalityId;
    private int bloodGroupId;
    private String eps;
    private String birthDate;
    private String documentNumber;

    public MemberDTO() {
    }

    public MemberDTO(int id, String names, String lastNames, String relationship, String phone, int genderId,
                     int documentTypeId, int nationalityId, int bloodGroupId, String eps, String birthDate,
                     String documentNumber) {
        this.id = id;
        this.names = names;
        this.lastNames = lastNames;
        this.relationship = relationship;
        this.phone = phone;
        this.genderId = genderId;
        this.documentTypeId = documentTypeId;
        this.nationalityId = nationalityId;
        this.bloodGroupId = bloodGroupId;
        this.eps = eps;
        this.birthDate = birthDate;
        this.documentNumber = documentNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getLastNames() {
        return lastNames;
    }

    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getGenderId() {
        return genderId;
    }

    public void setGenderId(int genderId) {
        this.genderId = genderId;
    }

    public int getDocumentTypeId() {
        return documentTypeId;
    }

    public void setDocumentTypeId(int documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    public int getNationalityId() {
        return nationalityId;
    }

    public void setNationalityId(int nationalityId) {
        this.nationalityId = nationalityId;
    }

    public int getBloodGroupId() {
        return bloodGroupId;
    }

    public void setBloodGroupId(int bloodGroupId) {
        this.bloodGroupId = bloodGroupId;
    }

    public String getEps() {
        return eps;
    }

    public void setEps(String eps) {
        this.eps = eps;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
}
