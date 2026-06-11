package com.defensacivil.dto;

public class PetDTO {
    private int id;
    private String name;
    private String breed;
    private String birthDate;
    private int speciesId;
    private int animalGenderId;
    private int familyPlanId;
    private int age;

    public PetDTO() {
    }

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public int getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(int speciesId) {
        this.speciesId = speciesId;
    }

    public int getAnimalGenderId() {
        return animalGenderId;
    }

    public void setAnimalGenderId(int animalGenderId) {
        this.animalGenderId = animalGenderId;
    }

    public int getFamilyPlanId() {
        return familyPlanId;
    }

    public void setFamilyPlanId(int familyPlanId) {
        this.familyPlanId = familyPlanId;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
