package com.defensacivil.dto;

/**
 * Objeto de Transferencia de Datos (DTO) que representa una vacuna aplicada a una mascota.
 * Contiene información sobre el identificador de la vacuna, su nombre, la fecha de aplicación
 * y el identificador de la mascota asociada.
 */
public class VaccineDTO {
    private int id;
    private String name;
    private String date;
    private Integer pet_id;

    /**
     * Constructor por defecto de VaccineDTO.
     */
    public VaccineDTO() {
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
        return id;
    }

    /**
     * Establece el identificador único del registro de vacuna.
     *
     * @param id El identificador único a establecer.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre de la vacuna.
     *
     * @return El nombre de la vacuna.
     */
    public String getName() {
        return name;
    }

    /**
     * Establece el nombre de la vacuna.
     *
     * @param name El nombre de la vacuna a establecer.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtiene la fecha de aplicación de la vacuna.
     *
     * @return La fecha de aplicación.
     */
    public String getDate() {
        return date;
    }

    /**
     * Establece la fecha de aplicación de la vacuna.
     *
     * @param date La fecha de aplicación a establecer.
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Obtiene el identificador de la mascota asociada.
     *
     * @return El identificador de la mascota.
     */
    public Integer getPet_id() {
        return pet_id;
    }

    /**
     * Establece el identificador de la mascota asociada.
     *
     * @param pet_id El identificador de la mascota a establecer.
     */
    public void setPet_id(Integer pet_id) {
        this.pet_id = pet_id;
    }
}
