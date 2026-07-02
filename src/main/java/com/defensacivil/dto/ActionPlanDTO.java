package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa un plan de acción.
 * Relaciona un plan familiar con un coordinador específico.
 */
public class ActionPlanDTO {
    private int id;
    private Integer family_plan_id;
    private Integer coordinator_id;

    /**
     * Constructor por defecto de ActionPlanDTO.
     */
    public ActionPlanDTO() {
    }

    /**
     * Constructor parametrizado de ActionPlanDTO.
     *
     * @param id             Identificador único del plan de acción.
     * @param family_plan_id Identificador del plan familiar asociado.
     * @param coordinator_id Identificador del coordinador asignado.
     */
    public ActionPlanDTO(int id, Integer family_plan_id, Integer coordinator_id) {
        this.id = id;
        this.family_plan_id = family_plan_id;
        this.coordinator_id = coordinator_id;
    }

    /**
     * Obtiene el identificador único del plan de acción.
     *
     * @return El identificador único del plan de acción.
     */
    public int getId() {
        return id;
    }

    /**
     * Establece el identificador único del plan de acción.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el identificador del plan familiar asociado.
     *
     * @return El identificador del plan familiar.
     */
    public Integer getFamily_plan_id() {
        return family_plan_id;
    }

    /**
     * Establece el identificador del plan familiar asociado.
     *
     * @param family_plan_id El identificador del plan familiar a establecer.
     */
    public void setFamily_plan_id(Integer family_plan_id) {
        this.family_plan_id = family_plan_id;
    }

    /**
     * Obtiene el identificador del coordinador asignado.
     *
     * @return El identificador del coordinador.
     */
    public Integer getCoordinator_id() {
        return coordinator_id;
    }

    /**
     * Establece el identificador del coordinador asignado.
     *
     * @param coordinator_id El identificador del coordinador a establecer.
     */
    public void setCoordinator_id(Integer coordinator_id) {
        this.coordinator_id = coordinator_id;
    }
}
