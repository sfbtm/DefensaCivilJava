package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa una acción dentro de un plan.
 * Contiene información de la acción, su estado y el miembro asignado.
 */
public class ActionDTO {
    // Identificador único de la acción
    private int id;
    // Descripción de la acción a realizar
    private String description;
    // Etapa en la que se ejecuta la acción (ej. Antes, Durante, Después)
    private String stage;
    // ID del integrante de la familia encargado de la acción
    private Integer member_id;
    // Datos básicos del integrante estructurados como objeto
    private MemberInfo member;

    /**
     * Clase interna que contiene información básica del miembro asignado a la acción.
     */
    public static class MemberInfo {
        // Nombres del integrante asignado
        private String names;
        // Apellidos del integrante asignado
        private String last_names;

        /**
         * Constructor por defecto de MemberInfo.
         */
        public MemberInfo() {
            // Bloque del constructor por defecto de MemberInfo
        }

        /**
         * Constructor parametrizado de MemberInfo.
         *
         * @param names      Nombres del miembro.
         * @param last_names Apellidos del miembro.
         */
        public MemberInfo(String names, String last_names) {
            // Bloque del constructor parametrizado de MemberInfo: inicializar atributos del miembro
            this.names = names;
            this.last_names = last_names;
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
        public String getLast_names() {
            // Bloque getter para obtener los apellidos del miembro
            return last_names;
        }

        /**
         * Establece los apellidos del miembro.
         *
         * @param last_names Los apellidos a establecer.
         */
        public void setLast_names(String last_names) {
            // Bloque setter para establecer los apellidos del miembro
            this.last_names = last_names;
        }
    }

    /**
     * Constructor por defecto de ActionDTO.
     */
    public ActionDTO() {
        // Bloque del constructor por defecto de ActionDTO
    }

    /**
     * Constructor parametrizado de ActionDTO.
     *
     * @param id          Identificador único de la acción.
     * @param description Descripción detallada de la acción.
     * @param stage       Etapa del plan de acción a la que pertenece.
     * @param member_id   Identificador del miembro de la familia asignado.
     * @param member      Objeto con información básica del miembro.
     */
    public ActionDTO(int id, String description, String stage, Integer member_id, MemberInfo member) {
        // Bloque del constructor parametrizado de ActionDTO: inicializar atributos de la acción
        this.id = id;
        this.description = description;
        this.stage = stage;
        this.member_id = member_id;
        this.member = member;
    }

    /**
     * Obtiene el identificador único de la acción.
     *
     * @return El identificador único de la acción.
     */
    public int getId() {
        // Bloque getter para obtener el identificador de la acción
        return id;
    }

    /**
     * Establece el identificador único de la acción.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        // Bloque setter para establecer el identificador de la acción
        this.id = id;
    }

    /**
     * Obtiene la descripción detallada de la acción.
     *
     * @return La descripción de la acción.
     */
    public String getDescription() {
        // Bloque getter para obtener la descripción de la acción
        return description;
    }

    /**
     * Establece la descripción detallada de la acción.
     *
     * @param description La descripción a establecer.
     */
    public void setDescription(String description) {
        // Bloque setter para establecer la descripción de la acción
        this.description = description;
    }

    /**
     * Obtiene la etapa del plan de acción.
     *
     * @return La etapa de la acción.
     */
    public String getStage() {
        // Bloque getter para obtener la etapa del plan de acción
        return stage;
    }

    /**
     * Establece la etapa del plan de acción.
     *
     * @param stage La etapa a establecer.
     */
    public void setStage(String stage) {
        // Bloque setter para establecer la etapa del plan de acción
        this.stage = stage;
    }

    /**
     * Obtiene el identificador del miembro asignado.
     *
     * @return El identificador del miembro.
     */
    public Integer getMember_id() {
        // Bloque getter para obtener el identificador del miembro asignado
        return member_id;
    }

    /**
     * Establece el identificador del miembro asignado.
     *
     * @param member_id El identificador del miembro a establecer.
     */
    public void setMember_id(Integer member_id) {
        // Bloque setter para establecer el identificador del miembro asignado
        this.member_id = member_id;
    }

    /**
     * Obtiene el objeto con la información del miembro asignado.
     *
     * @return El objeto {@link MemberInfo} del miembro.
     */
    public MemberInfo getMember() {
        // Bloque getter para obtener el objeto de información del miembro asignado
        return member;
    }

    /**
     * Establece el objeto con la información del miembro asignado.
     *
     * @param member El objeto {@link MemberInfo} a establecer.
     */
    public void setMember(MemberInfo member) {
        // Bloque setter para establecer el objeto de información del miembro asignado
        this.member = member;
    }
}
