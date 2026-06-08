package com.defensacivil.dto;

public class VaccineDTO {
    private int id;
    private String name;
    private String date;
    private Integer pet_id;

    public VaccineDTO() {
    }

    public VaccineDTO(int id, String name, String date, Integer pet_id) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.pet_id = pet_id;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getPet_id() {
        return pet_id;
    }

    public void setPet_id(Integer pet_id) {
        this.pet_id = pet_id;
    }
}
