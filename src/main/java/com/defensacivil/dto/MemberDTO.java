package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa a un miembro de la familia.
 * Contiene información personal detallada, de identificación, de contacto y médica del miembro.
 */
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

    /**
     * Constructor por defecto de MemberDTO.
     */
    public MemberDTO() {
    }

    /**
     * Constructor parametrizado de MemberDTO con todos los atributos de un miembro familiar.
     *
     * @param id             Identificador único del miembro familiar.
     * @param names          Nombres del miembro.
     * @param lastNames      Apellidos del miembro.
     * @param relationship   Relación o parentesco familiar.
     * @param phone          Número de teléfono de contacto.
     * @param genderId       Identificador del género.
     * @param documentTypeId Identificador del tipo de documento de identidad.
     * @param nationalityId  Identificador de la nacionalidad.
     * @param bloodGroupId   Identificador del grupo sanguíneo.
     * @param eps            Nombre de la EPS (Entidad Promotora de Salud).
     * @param birthDate      Fecha de nacimiento.
     * @param documentNumber Número de documento de identidad.
     */
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

    /**
     * Obtiene el identificador único del miembro.
     *
     * @return El identificador único.
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único del miembro.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene los nombres del miembro.
     *
     * @return Los nombres del miembro.
     */
    public String getNames() {
        return names;
    }

    /**
     * Establece los nombres del miembro.
     *
     * @param names Los nombres a establecer.
     */
    public void setNames(String names) {
        this.names = names;
    }

    /**
     * Obtiene los apellidos del miembro.
     *
     * @return Los apellidos del miembro.
     */
    public String getLastNames() {
        return lastNames;
    }

    /**
     * Establece los apellidos del miembro.
     *
     * @param lastNames Los apellidos a establecer.
     */
    public void setLastNames(String lastNames) {
        this.lastNames = lastNames;
    }

    /**
     * Obtiene el parentesco o relación familiar del miembro.
     *
     * @return El parentesco del miembro.
     */
    public String getRelationship() {
        return relationship;
    }

    /**
     * Establece el parentesco o relación familiar del miembro.
     *
     * @param relationship El parentesco a establecer.
     */
    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    /**
     * Obtiene el número de teléfono del miembro.
     *
     * @return El número de teléfono.
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Establece el número de teléfono del miembro.
     *
     * @param phone El número de teléfono a establecer.
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Obtiene el identificador del género del miembro.
     *
     * @return El identificador del género.
     */
    public int getGenderId() {
        return genderId;
    }

    /**
     * Establece el identificador del género del miembro.
     *
     * @param genderId El identificador del género a establecer.
     */
    public void setGenderId(int genderId) {
        this.genderId = genderId;
    }

    /**
     * Obtiene el identificador del tipo de documento de identidad.
     *
     * @return El identificador del tipo de documento.
     */
    public int getDocumentTypeId() {
        return documentTypeId;
    }

    /**
     * Establece el identificador del tipo de documento de identidad.
     *
     * @param documentTypeId El identificador del tipo de documento a establecer.
     */
    public void setDocumentTypeId(int documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    /**
     * Obtiene el identificador de la nacionalidad del miembro.
     *
     * @return El identificador de la nacionalidad.
     */
    public int getNationalityId() {
        return nationalityId;
    }

    /**
     * Establece el identificador de la nacionalidad del miembro.
     *
     * @param nationalityId El identificador de la nacionalidad a establecer.
     */
    public void setNationalityId(int nationalityId) {
        this.nationalityId = nationalityId;
    }

    /**
     * Obtiene el identificador del grupo sanguíneo del miembro.
     *
     * @return El identificador del grupo sanguíneo.
     */
    public int getBloodGroupId() {
        return bloodGroupId;
    }

    /**
     * Establece el identificador del grupo sanguíneo del miembro.
     *
     * @param bloodGroupId El identificador del grupo sanguíneo a establecer.
     */
    public void setBloodGroupId(int bloodGroupId) {
        this.bloodGroupId = bloodGroupId;
    }

    /**
     * Obtiene el nombre de la EPS (Entidad Promotora de Salud) del miembro.
     *
     * @return El nombre de la EPS.
     */
    public String getEps() {
        return eps;
    }

    /**
     * Establece la EPS (Entidad Promotora de Salud) del miembro.
     *
     * @param eps El nombre de la EPS a establecer.
     */
    public void setEps(String eps) {
        this.eps = eps;
    }

    /**
     * Obtiene la fecha de nacimiento del miembro.
     *
     * @return La fecha de nacimiento.
     */
    public String getBirthDate() {
        return birthDate;
    }

    /**
     * Establece la fecha de nacimiento del miembro.
     *
     * @param birthDate La fecha de nacimiento a establecer.
     */
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * Obtiene el número de documento de identidad del miembro.
     *
     * @return El número de documento.
     */
    public String getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Establece el número de documento de identidad del miembro.
     *
     * @param documentNumber El número de documento a establecer.
     */
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
}
