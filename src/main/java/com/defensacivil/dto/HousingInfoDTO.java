package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa la información de vivienda.
 * Almacena la ruta del archivo o croquis de la vivienda, su descripción y el plan familiar asociado.
 */
public class HousingInfoDTO {
    // Identificador único del registro de información de vivienda
    private int id;
    // Ruta de almacenamiento física o lógica del archivo de imagen del croquis
    private String path;
    // Descripción de la vivienda o detalles explicativos del croquis cargado
    private String description;
    // Identificador del plan familiar al que pertenecen estos detalles de vivienda
    private Integer family_plan_id;

    /**
     * Constructor por defecto de HousingInfoDTO.
     */
    public HousingInfoDTO() {
        // Bloque del constructor por defecto de HousingInfoDTO
    }

    /**
     * Constructor parametrizado de HousingInfoDTO.
     *
     * @param id             Identificador único de la información de vivienda.
     * @param path           Ruta o ubicación del archivo/croquis de la vivienda.
     * @param description    Descripción detallada de la vivienda o el croquis.
     * @param family_plan_id Identificador del plan familiar asociado.
     */
    public HousingInfoDTO(int id, String path, String description, Integer family_plan_id) {
        // Bloque del constructor parametrizado de HousingInfoDTO: inicializar atributos de la vivienda
        this.id = id;
        this.path = path;
        this.description = description;
        this.family_plan_id = family_plan_id;
    }

    /**
     * Obtiene el identificador único de la información de vivienda.
     *
     * @return El identificador único.
     */
    public int getId() {
        // Bloque getter para obtener el identificador de la información de vivienda
        return id;
    }

    /**
     * Establece el identificador único de la información de vivienda.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        // Bloque setter para establecer el identificador de la información de vivienda
        this.id = id;
    }

    /**
     * Obtiene la ruta o ubicación del archivo/croquis de la vivienda.
     *
     * @return La ruta del archivo.
     */
    public String getPath() {
        // Bloque getter para obtener la ruta del croquis de la vivienda
        return path;
    }

    /**
     * Establece la ruta o ubicación del archivo/croquis de la vivienda.
     *
     * @param path La ruta a establecer.
     */
    public void setPath(String path) {
        // Bloque setter para establecer la ruta del croquis de la vivienda
        this.path = path;
    }

    /**
     * Obtiene la descripción detallada de la vivienda.
     *
     * @return La descripción de la vivienda.
     */
    public String getDescription() {
        // Bloque getter para obtener la descripción detallada de la vivienda
        return description;
    }

    /**
     * Establece la descripción detallada de la vivienda.
     *
     * @param description La descripción a establecer.
     */
    public void setDescription(String description) {
        // Bloque setter para establecer la descripción detallada de la vivienda
        this.description = description;
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
}
