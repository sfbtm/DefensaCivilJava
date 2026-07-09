package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa a un miembro de la familia.
 * Contiene información personal detallada, de identificación, de contacto y médica del miembro.
 */
public class MemberDTO {
    // Identificador único del miembro de la familia
    private int id;
    // Nombres del integrante familiar
    private String names;
    // Apellidos del integrante familiar
    private String lastNames;
    // Parentesco o rol del integrante en la familia (ej. Padre, Hijo)
    private String relationship;
    // Número telefónico de contacto
    private String phone;
    // ID correspondiente al género del integrante (tabla Genero)
    private int genderId;
    // ID correspondiente al tipo de documento de identidad (tabla DocumentoTipo)
    private int documentTypeId;
    // ID correspondiente a la nacionalidad (tabla Nacionalidad)
    private int nationalityId;
    // ID correspondiente al grupo sanguíneo (almacenado virtualmente)
    private int bloodGroupId;
    // Nombre de la entidad promotora de salud (EPS) del miembro
    private String eps;
    // Fecha de nacimiento del integrante en formato AAAA-MM-DD
    private String birthDate;
    // Número del documento de identidad
    private String documentNumber;

    /**
     * Constructor por defecto de MemberDTO.
     */
    public MemberDTO() {
        // Bloque del constructor por defecto de MemberDTO
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
        // Bloque del constructor parametrizado de MemberDTO: inicializar todos los atributos del miembro familiar
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
        // Bloque getter para obtener el identificador del miembro
        return id;
    }

    /**
     * Establece el identificador único del miembro.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        // Bloque setter para establecer el identificador del miembro
        this.id = id;
    }

    /**
     * Obtiene los nombres del miembro.
     *
     * @return Los nombres del miembro.
     */
    public String getNames() {
        // Bloque getter para obtener los nombres del miembro
        return names;
    }

    /**
     * Establece los nombres del miembro.
     *
     * @param names Los nombres a establecer.
     */
    public void setNames(String names) {
        // Bloque setter para establecer los nombres del miembro
        this.names = names;
    }

    /**
     * Obtiene los apellidos del miembro.
     *
     * @return Los apellidos del miembro.
     */
    public String getLastNames() {
        // Bloque getter para obtener los apellidos del miembro
        return lastNames;
    }

    /**
     * Establece los apellidos del miembro.
     *
     * @param lastNames Los apellidos a establecer.
     */
    public void setLastNames(String lastNames) {
        // Bloque setter para establecer los apellidos del miembro
        this.lastNames = lastNames;
    }

    /**
     * Obtiene el parentesco o relación familiar del miembro.
     *
     * @return El parentesco del miembro.
     */
    public String getRelationship() {
        // Bloque getter para obtener el parentesco o relación familiar del miembro
        return relationship;
    }

    /**
     * Establece el parentesco o relación familiar del miembro.
     *
     * @param relationship El parentesco a establecer.
     */
    public void setRelationship(String relationship) {
        // Bloque setter para establecer el parentesco o relación familiar del miembro
        this.relationship = relationship;
    }

    /**
     * Obtiene el número de teléfono del miembro.
     *
     * @return El número de teléfono.
     */
    public String getPhone() {
        // Bloque getter para obtener el número de teléfono del miembro
        return phone;
    }

    /**
     * Establece el número de teléfono del miembro.
     *
     * @param phone El número de teléfono a establecer.
     */
    public void setPhone(String phone) {
        // Bloque setter para establecer el número de teléfono del miembro
        this.phone = phone;
    }

    /**
     * Obtiene el identificador del género del miembro.
     *
     * @return El identificador del género.
     */
    public int getGenderId() {
        // Bloque getter para obtener el identificador del género del miembro
        return genderId;
    }

    /**
     * Establece el identificador del género del miembro.
     *
     * @param genderId El identificador del género a establecer.
     */
    public void setGenderId(int genderId) {
        // Bloque setter para establecer el identificador del género del miembro
        this.genderId = genderId;
    }

    /**
     * Obtiene el identificador del tipo de documento de identidad.
     *
     * @return El identificador del tipo de documento.
     */
    public int getDocumentTypeId() {
        // Bloque getter para obtener el identificador del tipo de documento de identidad
        return documentTypeId;
    }

    /**
     * Establece el identificador del tipo de documento de identidad.
     *
     * @param documentTypeId El identificador del tipo de documento a establecer.
     */
    public void setDocumentTypeId(int documentTypeId) {
        // Bloque setter para establecer el identificador del tipo de documento de identidad
        this.documentTypeId = documentTypeId;
    }

    /**
     * Obtiene el identificador de la nacionalidad del miembro.
     *
     * @return El identificador de la nacionalidad.
     */
    public int getNationalityId() {
        // Bloque getter para obtener el identificador de la nacionalidad del miembro
        return nationalityId;
    }

    /**
     * Establece el identificador de la nacionalidad del miembro.
     *
     * @param nationalityId El identificador de la nacionalidad a establecer.
     */
    public void setNationalityId(int nationalityId) {
        // Bloque setter para establecer el identificador de la nacionalidad del miembro
        this.nationalityId = nationalityId;
    }

    /**
     * Obtiene el identificador del grupo sanguíneo del miembro.
     *
     * @return El identificador del grupo sanguíneo.
     */
    public int getBloodGroupId() {
        // Bloque getter para obtener el identificador del grupo sanguíneo del miembro
        return bloodGroupId;
    }

    /**
     * Establece el identificador del grupo sanguíneo del miembro.
     *
     * @param bloodGroupId El identificador del grupo sanguíneo a establecer.
     */
    public void setBloodGroupId(int bloodGroupId) {
        // Bloque setter para establecer el identificador del grupo sanguíneo del miembro
        this.bloodGroupId = bloodGroupId;
    }

    /**
     * Obtiene el nombre de la EPS (Entidad Promotora de Salud) del miembro.
     *
     * @return El nombre de la EPS.
     */
    public String getEps() {
        // Bloque getter para obtener el nombre de la EPS del miembro
        return eps;
    }

    /**
     * Establece la EPS (Entidad Promotora de Salud) del miembro.
     *
     * @param eps El nombre de la EPS a establecer.
     */
    public void setEps(String eps) {
        // Bloque setter para establecer la EPS del miembro
        this.eps = eps;
    }

    /**
     * Obtiene la fecha de nacimiento del miembro.
     *
     * @return La fecha de nacimiento.
     */
    public String getBirthDate() {
        // Bloque getter para obtener la fecha de nacimiento del miembro
        return birthDate;
    }

    /**
     * Establece la fecha de nacimiento del miembro.
     *
     * @param birthDate La fecha de nacimiento a establecer.
     */
    public void setBirthDate(String birthDate) {
        // Bloque setter para establecer la fecha de nacimiento del miembro
        this.birthDate = birthDate;
    }

    /**
     * Obtiene el número de documento de identidad del miembro.
     *
     * @return El número de documento.
     */
    public String getDocumentNumber() {
        // Bloque getter para obtener el número de documento de identidad del miembro
        return documentNumber;
    }

    /**
     * Establece el número de documento de identidad del miembro.
     *
     * @param documentNumber El número de documento a establecer.
     */
    public void setDocumentNumber(String documentNumber) {
        // Bloque setter para establecer el número de documento de identidad del miembro
        this.documentNumber = documentNumber;
    }
}
