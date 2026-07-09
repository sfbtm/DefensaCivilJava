package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa a una mascota.
 * Contiene información de identificación, raza, fecha de nacimiento, especie,
 * género, plan familiar asociado y edad de la mascota.
 */
public class PetDTO {
    // Identificador único de la mascota
    private int id;
    // Nombre de la mascota
    private String name;
    // Raza de la mascota (ej. Criollo, Labrador, Siamés)
    private String breed;
    // Fecha de nacimiento de la mascota (formato AAAA-MM-DD)
    private String birthDate;
    // ID correspondiente a la especie del animal (ej. Perro, Gato)
    private int speciesId;
    // ID correspondiente al género o sexo del animal
    private int animalGenderId;
    // ID del plan familiar al que pertenece la mascota
    private int familyPlanId;
    // Edad calculada de la mascota en años
    private int age;

    /**
     * Constructor por defecto de PetDTO.
     */
    public PetDTO() {
        // Bloque del constructor por defecto de PetDTO
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
        // Bloque del constructor parametrizado de PetDTO: inicializar todos los atributos de la mascota
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
        // Bloque getter para obtener el identificador de la mascota
        return id;
    }

    /**
     * Establece el identificador único de la mascota.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        // Bloque setter para establecer el identificador de la mascota
        this.id = id;
    }

    /**
     * Obtiene el nombre de la mascota.
     *
     * @return El nombre de la mascota.
     */
    public String getName() {
        // Bloque getter para obtener el nombre de la mascota
        return name;
    }

    /**
     * Establece el nombre de la mascota.
     *
     * @param name El nombre de la mascota a establecer.
     */
    public void setName(String name) {
        // Bloque setter para establecer el nombre de la mascota
        this.name = name;
    }

    /**
     * Obtiene la raza de la mascota.
     *
     * @return La raza de la mascota.
     */
    public String getBreed() {
        // Bloque getter para obtener la raza de la mascota
        return breed;
    }

    /**
     * Establece la raza de la mascota.
     *
     * @param breed La raza a establecer.
     */
    public void setBreed(String breed) {
        // Bloque setter para establecer la raza de la mascota
        this.breed = breed;
    }

    /**
     * Obtiene la fecha de nacimiento de la mascota.
     *
     * @return La fecha de nacimiento de la mascota.
     */
    public String getBirthDate() {
        // Bloque getter para obtener la fecha de nacimiento de la mascota
        return birthDate;
    }

    /**
     * Establece la fecha de nacimiento de la mascota.
     *
     * @param birthDate La fecha de nacimiento a establecer.
     */
    public void setBirthDate(String birthDate) {
        // Bloque setter para establecer la fecha de nacimiento de la mascota
        this.birthDate = birthDate;
    }

    /**
     * Obtiene el identificador de la especie de la mascota.
     *
     * @return El identificador de la especie.
     */
    public int getSpeciesId() {
        // Bloque getter para obtener el identificador de la especie de la mascota
        return speciesId;
    }

    /**
     * Establece el identificador de la especie de la mascota.
     *
     * @param speciesId El identificador de la especie a establecer.
     */
    public void setSpeciesId(int speciesId) {
        // Bloque setter para establecer el identificador de la especie de la mascota
        this.speciesId = speciesId;
    }

    /**
     * Obtiene el identificador del género de la mascota.
     *
     * @return El identificador del género de la mascota.
     */
    public int getAnimalGenderId() {
        // Bloque getter para obtener el identificador del género de la mascota
        return animalGenderId;
    }

    /**
     * Establece el identificador del género de la mascota.
     *
     * @param animalGenderId El identificador del género de la mascota a establecer.
     */
    public void setAnimalGenderId(int animalGenderId) {
        // Bloque setter para establecer el identificador del género de la mascota
        this.animalGenderId = animalGenderId;
    }

    /**
     * Obtiene el identificador del plan familiar asociado.
     *
     * @return El identificador del plan familiar.
     */
    public int getFamilyPlanId() {
        // Bloque getter para obtener el identificador del plan familiar asociado a la mascota
        return familyPlanId;
    }

    /**
     * Establece el identificador del plan familiar asociado.
     *
     * @param familyPlanId El identificador del plan familiar a establecer.
     */
    public void setFamilyPlanId(int familyPlanId) {
        // Bloque setter para establecer el identificador del plan familiar asociado a la mascota
        this.familyPlanId = familyPlanId;
    }

    /**
     * Obtiene la edad de la mascota.
     *
     * @return La edad de la mascota.
     */
    public int getAge() {
        // Bloque getter para obtener la edad de la mascota
        return age;
    }

    /**
     * Establece la edad de la mascota.
     *
     * @param age La edad a establecer.
     */
    public void setAge(int age) {
        // Bloque setter para establecer la edad de la mascota
        this.age = age;
    }
}
