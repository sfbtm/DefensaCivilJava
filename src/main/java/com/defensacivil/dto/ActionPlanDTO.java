package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa un plan de acción.
 * Relaciona un plan familiar con un coordinador específico.
 */
public class ActionPlanDTO {
    // Identificador único del plan de acción
    private int id;
    // Identificador del plan familiar con el que se vincula este plan de acción
    private Integer family_plan_id;
    // Identificador del integrante que actúa como coordinador del plan
    private Integer coordinator_id;

    /**
     * Constructor por defecto de ActionPlanDTO.
     */
    public ActionPlanDTO() {
        // Bloque del constructor por defecto de ActionPlanDTO
    }

    /**
     * Constructor parametrizado de ActionPlanDTO.
     *
     * @param id             Identificador único del plan de acción.
     * @param family_plan_id Identificador del plan familiar asociado.
     * @param coordinator_id Identificador del coordinador asignado.
     */
    public ActionPlanDTO(int id, Integer family_plan_id, Integer coordinator_id) {
        // Bloque del constructor parametrizado de ActionPlanDTO: inicializar atributos del plan de acción
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
        // Bloque getter para obtener el identificador del plan de acción
        return id;
    }

    /**
     * Establece el identificador único del plan de acción.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        // Bloque setter para establecer el identificador del plan de acción
        this.id = id;
    }

    /**
     * Obtiene el identificador del plan familiar asociado.
     *
     * @return El identificador del plan familiar.
     */
    public Integer getFamily_plan_id() {
        // Bloque getter para obtener el identificador del plan familiar asociado
        return family_plan_id;
    }

    /**
     * Establece el identificador del plan familiar asociado.
     *
     * @param family_plan_id El identificador del plan familiar a establecer.
     */
    public void setFamily_plan_id(Integer family_plan_id) {
        // Bloque setter para establecer el identificador del plan familiar asociado
        this.family_plan_id = family_plan_id;
    }

    /**
     * Obtiene el identificador del coordinador asignado.
     *
     * @return El identificador del coordinador.
     */
    public Integer getCoordinator_id() {
        // Bloque getter para obtener el identificador del coordinador asignado
        return coordinator_id;
    }

    /**
     * Establece el identificador del coordinador asignado.
     *
     * @param coordinator_id El identificador del coordinador a establecer.
     */
    public void setCoordinator_id(Integer coordinator_id) {
        // Bloque setter para establecer el identificador del coordinador asignado
        this.coordinator_id = coordinator_id;
    }
}
