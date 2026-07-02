package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa a una mascota.
 * Contiene información de identificación, raza, fecha de nacimiento, especie,
 * género, plan familiar asociado y edad de la mascota.
 */
public class PetDTO {
    private int id;
    private String name;
    private String breed;
    private String birthDate;
    private int speciesId;
    private int animalGenderId;
    private int familyPlanId;
    private int age;

    /**
     * Constructor por defecto de PetDTO.
     */
    public PetDTO() {
    }

    /**
     * Constructor parametrizado de PetDTO con todos los atributos de la mascota.
     *
     * @param id             Identificador único de la mascota.
     * @param name           Nombre de la mascota.
     * @param breed          Raza de la mascota.
     * @param birthDate      Fecha de nacimiento de la mascota.
     * @param speciesId      Identificador de la especie de la mascota.
     * @param animalGenderId Identificador del género de la mascota.
     * @param familyPlanId   Identificador del plan familiar al que pertenece.
     * @param age            Edad de la mascota.
     */
    public PetDTO(int id, String name, String breed, String birthDate, int speciesId, int animalGenderId,
                  int familyPlanId, int age) {
        this.id = id;
        this.name = name;
        this.breed = breed;
        this.birthDate = birthDate;
        this.speciesId = speciesId;
        this.animalGenderId = animalGenderId;
        this.familyPlanId = familyPlanId;
        this.age = age;
    }

    /**
     * Obtiene el identificador único de la mascota.
     *
     * @return El identificador único de la mascota.
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único de la mascota.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre de la mascota.
     *
     * @return El nombre de la mascota.
     */
    public String getName() {
        return name;
    }

    /**
     * Establece el nombre de la mascota.
     *
     * @param name El nombre de la mascota a establecer.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene la raza de la mascota.
     *
     * @return La raza de la mascota.
     */
    public String getBreed() {
        return breed;
    }

    /**
     * Establece la raza de la mascota.
     *
     * @param breed La raza a establecer.
     */
    public void setBreed(String breed) {
        this.breed = breed;
    }

    /**
     * Obtiene la fecha de nacimiento de la mascota.
     *
     * @return La fecha de nacimiento de la mascota.
     */
    public String getBirthDate() {
        return birthDate;
    }

    /**
     * Establece la fecha de nacimiento de la mascota.
     *
     * @param birthDate La fecha de nacimiento a establecer.
     */
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * Obtiene el identificador de la especie de la mascota.
     *
     * @return El identificador de la especie.
     */
    public int getSpeciesId() {
        return speciesId;
    }

    /**
     * Establece el identificador de la especie de la mascota.
     *
     * @param speciesId El identificador de la especie a establecer.
     */
    public void setSpeciesId(int speciesId) {
        this.speciesId = speciesId;
    }

    /**
     * Obtiene el identificador del género de la mascota.
     *
     * @return El identificador del género de la mascota.
     */
    public int getAnimalGenderId() {
        return animalGenderId;
    }

    /**
     * Establece el identificador del género de la mascota.
     *
     * @param animalGenderId El identificador del género de la mascota a establecer.
     */
    public void setAnimalGenderId(int animalGenderId) {
        this.animalGenderId = animalGenderId;
    }

    /**
     * Obtiene el identificador del plan familiar asociado.
     *
     * @return El identificador del plan familiar.
     */
    public int getFamilyPlanId() {
        return familyPlanId;
    }

    /**
     * Establece el identificador del plan familiar asociado.
     *
     * @param familyPlanId El identificador del plan familiar a establecer.
     */
    public void setFamilyPlanId(int familyPlanId) {
        this.familyPlanId = familyPlanId;
    }

    /**
     * Obtiene la edad de la mascota.
     *
     * @return La edad de la mascota.
     */
    public int getAge() {
        return age;
    }

    /**
     * Establece la edad de la mascota.
     *
     * @param age La edad a establecer.
     */
    public void setAge(int age) {
        this.age = age;
    }
}
