package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa una vacuna aplicada a una mascota.
 * Contiene información sobre el identificador de la vacuna, su nombre, la fecha de aplicación
 * y el identificador de la mascota asociada.
 */
public class VaccineDTO {
    // Identificador único del registro de la vacuna en la base de datos
    private int id;
    // Nombre de la vacuna (ej. Antirrábica, Triple Felina)
    private String name;
    // Fecha de aplicación de la vacuna en formato AAAA-MM-DD
    private String date;
    // Identificador de la mascota a la que se le administró la vacuna
    private Integer pet_id;

    /**
     * Constructor por defecto de VaccineDTO.
     */
    public VaccineDTO() {
        // Bloque del constructor por defecto de VaccineDTO
    }

    /**
     * Constructor parametrizado de VaccineDTO con todos los atributos.
     *
     * @param id     Identificador único del registro de vacuna.
     * @param name   Nombre de la vacuna.
     * @param date   Fecha de aplicación de la vacuna.
     * @param pet_id Identificador de la mascota que recibió la vacuna.
     */
    public VaccineDTO(int id, String name, String date, Integer pet_id) {
        // Bloque del constructor parametrizado de VaccineDTO: inicializar los atributos del registro de vacuna
        this.id = id;
        this.name = name;
        this.date = date;
        this.pet_id = pet_id;
    }

    /**
     * Obtiene el identificador único del registro de vacuna.
     *
     * @return El identificador de la vacuna.
     */
    public int getId() {
        // Bloque getter para obtener el identificador de la vacuna
        return id;
    }

    /**
     * Establece el identificador único del registro de vacuna.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        // Bloque setter para establecer el identificador de la vacuna
        this.id = id;
    }

    /**
     * Obtiene el nombre de la vacuna.
     *
     * @return El nombre de la vacuna.
     */
    public String getName() {
        // Bloque getter para obtener el nombre de la vacuna
        return name;
    }

    /**
     * Establece el nombre de la vacuna.
     *
     * @param name El nombre de la vacuna a establecer.
     */
    public void setName(String name) {
        // Bloque setter para establecer el nombre de la vacuna
        this.name = name;
    }

    /**
     * Obtiene la fecha de aplicación de la vacuna.
     *
     * @return La fecha de aplicación.
     */
    public String getDate() {
        // Bloque getter para obtener la fecha de aplicación de la vacuna
        return date;
    }

    /**
     * Establece la fecha de aplicación de la vacuna.
     *
     * @param date La fecha de aplicación a establecer.
     */
    public void setDate(String date) {
        // Bloque setter para establecer la fecha de aplicación de la vacuna
        this.date = date;
    }

    /**
     * Obtiene el identificador de la mascota asociada.
     *
     * @return El identificador de la mascota.
     */
    public Integer getPet_id() {
        // Bloque getter para obtener el identificador de la mascota asociada a la vacuna
        return pet_id;
    }

    /**
     * Establece el identificador de la mascota asociada.
     *
     * @param pet_id El identificador de la mascota a establecer.
     */
    public void setPet_id(Integer pet_id) {
        // Bloque setter para establecer el identificador de la mascota asociada a la vacuna
        this.pet_id = pet_id;
    }
}
