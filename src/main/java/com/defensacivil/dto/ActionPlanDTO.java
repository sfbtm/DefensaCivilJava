package com.defensacivil.dto;

public class ActionPlanDTO {
    private int id;
    private Integer family_plan_id;
    private Integer coordinator_id;

    public ActionPlanDTO() {
    }

    public ActionPlanDTO(int id, Integer family_plan_id, Integer coordinator_id) {
        this.id = id;
        this.family_plan_id = family_plan_id;
        this.coordinator_id = coordinator_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getFamily_plan_id() {
        return family_plan_id;
    }

    public void setFamily_plan_id(Integer family_plan_id) {
        this.family_plan_id = family_plan_id;
    }

    public Integer getCoordinator_id() {
        return coordinator_id;
    }

    public void setCoordinator_id(Integer coordinator_id) {
        this.coordinator_id = coordinator_id;
    }
}
