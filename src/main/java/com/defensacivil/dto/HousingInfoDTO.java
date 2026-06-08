package com.defensacivil.dto;

public class HousingInfoDTO {
    private int id;
    private String path;
    private String description;
    private Integer family_plan_id;

    public HousingInfoDTO() {
    }

    public HousingInfoDTO(int id, String path, String description, Integer family_plan_id) {
        this.id = id;
        this.path = path;
        this.description = description;
        this.family_plan_id = family_plan_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getFamily_plan_id() {
        return family_plan_id;
    }

    public void setFamily_plan_id(Integer family_plan_id) {
        this.family_plan_id = family_plan_id;
    }
}
