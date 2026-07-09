-- Script de creación de la base de datos DefensaCivilDB
CREATE DATABASE IF NOT EXISTS DefensaCivilDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE DefensaCivilDB;

-- Desactivar temporalmente la verificación de claves foráneas para limpieza limpia
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS Accion;
DROP TABLE IF EXISTS PlanAccion;
DROP TABLE IF EXISTS Vulnerabilidad;
DROP TABLE IF EXISTS FactorRiesgo;
DROP TABLE IF EXISTS GraficoVivienda;
DROP TABLE IF EXISTS MascotaVacuna;
DROP TABLE IF EXISTS Mascotas;
DROP TABLE IF EXISTS IntegranteEnfermedad;
DROP TABLE IF EXISTS Integrante;
DROP TABLE IF EXISTS RespuestaPlan;
DROP TABLE IF EXISTS ValidacionPlan;
DROP TABLE IF EXISTS PlanFamiliar;
DROP TABLE IF EXISTS Familia;
DROP TABLE IF EXISTS Usuario;
DROP TABLE IF EXISTS Rol;
DROP TABLE IF EXISTS Organizacion;
DROP TABLE IF EXISTS Seccional;
DROP TABLE IF EXISTS Departamento;
DROP TABLE IF EXISTS DocumentoTipo;
DROP TABLE IF EXISTS Genero;
DROP TABLE IF EXISTS Nacionalidad;
DROP TABLE IF EXISTS Sector;
DROP TABLE IF EXISTS CalidadVivienda;
DROP TABLE IF EXISTS TipoAmenaza;
DROP TABLE IF EXISTS VulnerabilidadTipo;
DROP TABLE IF EXISTS Especie;
DROP TABLE IF EXISTS Vacuna;
DROP TABLE IF EXISTS Pregunta;
DROP TABLE IF EXISTS Servicio;
DROP TABLE IF EXISTS Recurso;
DROP TABLE IF EXISTS RecursoTipo;
DROP TABLE IF EXISTS RecursoDisponible;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Catálogos y Tablas Maestras Independientes

CREATE TABLE Rol (
    IdRol INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Departamento (
    IdDepartamento INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Seccional (
    IdSeccional INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Genero (
    IdGenero INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE DocumentoTipo (
    IdDocumentoTipo INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Nacionalidad (
    IdNacionalidad INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Sector (
    IdSector INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE CalidadVivienda (
    IdCalidad INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE TipoAmenaza (
    IdTipoAmenaza INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE VulnerabilidadTipo (
    IdTipoVulnerabilidad INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Especie (
    IdEspecie INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Vacuna (
    IdVacuna INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Pregunta (
    IdPregunta INT PRIMARY KEY AUTO_INCREMENT,
    Texto VARCHAR(255) NOT NULL,
    Activa TINYINT(1) DEFAULT 1,
    Precaucion TINYINT(1) DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE Servicio (
    IdServicio INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Recurso (
    IdRecurso INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL,
    Servicio VARCHAR(100) NOT NULL,
    Activo TINYINT(1) DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE RecursoTipo (
    IdRecursoTipo INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Enfermedad (
    IdEnfermedad INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(150) NOT NULL UNIQUE
) ENGINE=InnoDB;


-- 2. Tablas Dependientes y Relacionales

CREATE TABLE Organizacion (
    IdOrganizacion INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL,
    IdSeccional INT NOT NULL,
    FOREIGN KEY (IdSeccional) REFERENCES Seccional(IdSeccional) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE Usuario (
    IdUsuario INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(150) NOT NULL,
    IdRol INT NOT NULL,
    IdOrganizacion INT,
    IdGenero INT NOT NULL,
    Email VARCHAR(150) NOT NULL UNIQUE,
    Contrasena VARCHAR(255) NOT NULL,
    Activo INT NOT NULL DEFAULT 3, -- 1: Activo, 2/0: Inactivo, 3: Pendiente
    FOREIGN KEY (IdRol) REFERENCES Rol(IdRol) ON DELETE RESTRICT,
    FOREIGN KEY (IdOrganizacion) REFERENCES Organizacion(IdOrganizacion) ON DELETE SET NULL,
    FOREIGN KEY (IdGenero) REFERENCES Genero(IdGenero) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE Familia (
    IdFamilia INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(150) NOT NULL,
    IdSector INT NOT NULL,
    IdCalidad INT NOT NULL,
    Telefono VARCHAR(50),
    FOREIGN KEY (IdSector) REFERENCES Sector(IdSector) ON DELETE RESTRICT,
    FOREIGN KEY (IdCalidad) REFERENCES CalidadVivienda(IdCalidad) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE PlanFamiliar (
    IdPlanFamiliar INT PRIMARY KEY AUTO_INCREMENT,
    IdFamilia INT NOT NULL,
    IdUsuario INT NOT NULL,
    Fecha DATE NOT NULL,
    Estado VARCHAR(50) NOT NULL DEFAULT '1', -- '1': Creado, '3': En Proceso, '4': En Revisión, etc.
    FOREIGN KEY (IdFamilia) REFERENCES Familia(IdFamilia) ON DELETE CASCADE,
    FOREIGN KEY (IdUsuario) REFERENCES Usuario(IdUsuario) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE ValidacionPlan (
    IdValidacion INT PRIMARY KEY AUTO_INCREMENT,
    IdPlanFamiliar INT NOT NULL,
    IdSupervisor INT NOT NULL, -- Hace referencia al IdUsuario con Rol de Supervisor
    Fecha DATE NOT NULL,
    Estado VARCHAR(50) NOT NULL,
    Comentario TEXT,
    FOREIGN KEY (IdPlanFamiliar) REFERENCES PlanFamiliar(IdPlanFamiliar) ON DELETE CASCADE,
    FOREIGN KEY (IdSupervisor) REFERENCES Usuario(IdUsuario) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE RespuestaPlan (
    IdRespuestaPlan INT PRIMARY KEY AUTO_INCREMENT,
    IdPregunta INT NOT NULL,
    IdPlanFamiliar INT NOT NULL,
    Valor TINYINT(1) NOT NULL,
    FOREIGN KEY (IdPregunta) REFERENCES Pregunta(IdPregunta) ON DELETE CASCADE,
    FOREIGN KEY (IdPlanFamiliar) REFERENCES PlanFamiliar(IdPlanFamiliar) ON DELETE CASCADE,
    UNIQUE KEY unique_respuesta (IdPregunta, IdPlanFamiliar)
) ENGINE=InnoDB;

CREATE TABLE Integrante (
    IdIntegrante INT PRIMARY KEY AUTO_INCREMENT,
    IdPlanFamiliar INT NOT NULL,
    Nombre VARCHAR(100) NOT NULL,
    Apellido VARCHAR(100) NOT NULL,
    Parentesco VARCHAR(100) NOT NULL,
    Telefono VARCHAR(50),
    IdGenero INT NOT NULL,
    IdDocumentoTipo INT NOT NULL,
    IdNacionalidad INT NOT NULL,
    FOREIGN KEY (IdPlanFamiliar) REFERENCES PlanFamiliar(IdPlanFamiliar) ON DELETE CASCADE,
    FOREIGN KEY (IdGenero) REFERENCES Genero(IdGenero) ON DELETE RESTRICT,
    FOREIGN KEY (IdDocumentoTipo) REFERENCES DocumentoTipo(IdDocumentoTipo) ON DELETE RESTRICT,
    FOREIGN KEY (IdNacionalidad) REFERENCES Nacionalidad(IdNacionalidad) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IntegranteEnfermedad (
    IdIntegrante INT NOT NULL,
    IdEnfermedad INT NOT NULL,
    Medicina VARCHAR(150),
    Dosis VARCHAR(100),
    PRIMARY KEY (IdIntegrante, IdEnfermedad),
    FOREIGN KEY (IdIntegrante) REFERENCES Integrante(IdIntegrante) ON DELETE CASCADE,
    FOREIGN KEY (IdEnfermedad) REFERENCES Enfermedad(IdEnfermedad) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Mascotas (
    IdMascota INT PRIMARY KEY AUTO_INCREMENT,
    Nombre VARCHAR(100) NOT NULL,
    IdGenero INT NOT NULL,
    Raza VARCHAR(100),
    IdEspecie INT NOT NULL,
    FechaNacimiento DATE,
    IdPlanFamiliar INT NOT NULL,
    FOREIGN KEY (IdGenero) REFERENCES Genero(IdGenero) ON DELETE RESTRICT,
    FOREIGN KEY (IdEspecie) REFERENCES Especie(IdEspecie) ON DELETE RESTRICT,
    FOREIGN KEY (IdPlanFamiliar) REFERENCES PlanFamiliar(IdPlanFamiliar) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE MascotaVacuna (
    IdMascota INT NOT NULL,
    IdVacuna INT NOT NULL,
    FechaAplicacion DATE NOT NULL,
    PRIMARY KEY (IdMascota, IdVacuna),
    FOREIGN KEY (IdMascota) REFERENCES Mascotas(IdMascota) ON DELETE CASCADE,
    FOREIGN KEY (IdVacuna) REFERENCES Vacuna(IdVacuna) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE GraficoVivienda (
    IdGrafico INT PRIMARY KEY AUTO_INCREMENT,
    IdPlanFamiliar INT NOT NULL,
    RutaImagen VARCHAR(255) NOT NULL,
    Descripcion VARCHAR(255),
    EsEntorno INT NOT NULL DEFAULT 0, -- 1: Plano del Entorno, 0: Croquis Interno
    FOREIGN KEY (IdPlanFamiliar) REFERENCES PlanFamiliar(IdPlanFamiliar) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE FactorRiesgo (
    IdFactorRiesgo INT PRIMARY KEY AUTO_INCREMENT,
    IdPlanFamiliar INT NOT NULL,
    IdTipoAmenaza INT NOT NULL,
    Ubicacion VARCHAR(255) NOT NULL,
    AccionReduccion VARCHAR(255) DEFAULT '',
    FOREIGN KEY (IdPlanFamiliar) REFERENCES PlanFamiliar(IdPlanFamiliar) ON DELETE CASCADE,
    FOREIGN KEY (IdTipoAmenaza) REFERENCES TipoAmenaza(IdTipoAmenaza) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE Vulnerabilidad (
    IdVulnerabilidad INT PRIMARY KEY AUTO_INCREMENT,
    IdFactorRiesgo INT NOT NULL,
    IdTipoVulnerabilidad INT NOT NULL,
    Grado VARCHAR(50) NOT NULL, -- 'Bajo', 'Medio', 'Alto'
    FOREIGN KEY (IdFactorRiesgo) REFERENCES FactorRiesgo(IdFactorRiesgo) ON DELETE CASCADE,
    FOREIGN KEY (IdTipoVulnerabilidad) REFERENCES VulnerabilidadTipo(IdTipoVulnerabilidad) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE PlanAccion (
    IdPlanAccion INT PRIMARY KEY AUTO_INCREMENT,
    IdFactorRiesgo INT NOT NULL UNIQUE,
    IdCoordinador INT NOT NULL, -- Referencia al Integrante
    FOREIGN KEY (IdFactorRiesgo) REFERENCES FactorRiesgo(IdFactorRiesgo) ON DELETE CASCADE,
    FOREIGN KEY (IdCoordinador) REFERENCES Integrante(IdIntegrante) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE Accion (
    IdAccion INT PRIMARY KEY AUTO_INCREMENT,
    IdPlanAccion INT NOT NULL,
    IdResponsable INT NOT NULL, -- Referencia al Integrante
    Etapa VARCHAR(100) NOT NULL, -- 'Antes', 'Durante', 'Después'
    Descripcion VARCHAR(255) NOT NULL,
    FOREIGN KEY (IdPlanAccion) REFERENCES PlanAccion(IdPlanAccion) ON DELETE CASCADE,
    FOREIGN KEY (IdResponsable) REFERENCES Integrante(IdIntegrante) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE RecursoDisponible (
    IdRecurso INT PRIMARY KEY AUTO_INCREMENT,
    IdPlanFamiliar INT NOT NULL,
    IdRecursoTipo INT NOT NULL,
    IdServicio INT NOT NULL,
    Ubicacion VARCHAR(255) NOT NULL,
    Distancia FLOAT NOT NULL,
    Telefono VARCHAR(50),
    FOREIGN KEY (IdPlanFamiliar) REFERENCES PlanFamiliar(IdPlanFamiliar) ON DELETE CASCADE,
    FOREIGN KEY (IdRecursoTipo) REFERENCES RecursoTipo(IdRecursoTipo) ON DELETE RESTRICT,
    FOREIGN KEY (IdServicio) REFERENCES Servicio(IdServicio) ON DELETE RESTRICT
) ENGINE=InnoDB;


-- 3. Inserciones de Datos Iniciales (Semillas de Catálogos)

INSERT INTO Rol (IdRol, Nombre) VALUES 
(1, 'Administrador'),
(2, 'Supervisor'),
(3, 'Voluntario');

INSERT INTO Genero (IdGenero, Nombre) VALUES 
(1, 'Macho'),
(2, 'Hembra'),
(3, 'Masculino'),
(4, 'Femenino'),
(5, 'Otro');

INSERT INTO DocumentoTipo (IdDocumentoTipo, Nombre) VALUES 
(1, 'Cédula de Ciudadanía'),
(2, 'Tarjeta de Identidad'),
(3, 'Cédula de Extranjería'),
(4, 'Pasaporte');

INSERT INTO Nacionalidad (IdNacionalidad, Nombre) VALUES 
(1, 'Colombiana'),
(2, 'Venezolana'),
(3, 'Ecuatoriana'),
(4, 'Otra');

INSERT INTO Departamento (IdDepartamento, Nombre) VALUES 
(1, 'Cundinamarca'),
(2, 'Antioquia'),
(3, 'Valle del Cauca'),
(4, 'Atlántico');

INSERT INTO Seccional (IdSeccional, Nombre) VALUES 
(1, 'Bogotá'),
(2, 'Medellín'),
(3, 'Cali'),
(4, 'Barranquilla');

INSERT INTO Organizacion (IdOrganizacion, Nombre, IdSeccional) VALUES 
(1, 'Defensa Civil Bogotá Sur', 1),
(2, 'Defensa Civil Medellín Centro', 2);

INSERT INTO Sector (IdSector, Nombre) VALUES 
(1, 'Sector Norte'),
(2, 'Sector Sur'),
(3, 'Sector Oriente'),
(4, 'Sector Occidente');

INSERT INTO CalidadVivienda (IdCalidad, Nombre) VALUES 
(1, 'Buena / Sismoresistente'),
(2, 'Regular'),
(3, 'Deficiente / Riesgo de Colapso');

INSERT INTO Especie (IdEspecie, Nombre) VALUES 
(1, 'Perro'),
(2, 'Gato'),
(3, 'Ave'),
(4, 'Otro');

INSERT INTO TipoAmenaza (IdTipoAmenaza, Nombre) VALUES 
(1, 'Inundación'),
(2, 'Deslizamiento'),
(3, 'Incendio Forestal'),
(4, 'Sismo'),
(5, 'Erupción Volcánica');

INSERT INTO VulnerabilidadTipo (IdTipoVulnerabilidad, Nombre) VALUES 
(1, 'Estructural (Vivienda precaria)'),
(2, 'Física (Cercanía a ladera o río)'),
(3, 'Social (Adultos mayores / niños solos)'),
(4, 'Económica');

INSERT INTO Servicio (IdServicio, Nombre) VALUES 
(1, 'Salud / Primeros Auxilios'),
(2, 'Bomberos / Extinción'),
(3, 'Policía / Seguridad'),
(4, 'Refugio Temporal');

INSERT INTO RecursoTipo (IdRecursoTipo, Nombre) VALUES 
(1, 'Centro de Salud / Hospital'),
(2, 'Estación de Bomberos'),
(3, 'CAI / Estación de Policía'),
(4, 'Punto de Encuentro Comunitario');

INSERT INTO Recurso (IdRecurso, Nombre, Servicio, Activo) VALUES 
(1, 'Hospital Central', 'Salud / Primeros Auxilios', 1),
(2, 'Estación de Bomberos Principal', 'Bomberos / Extinción', 1),
(3, 'Estación de Policía local', 'Policía / Seguridad', 1);

INSERT INTO Vacuna (IdVacuna, Nombre) VALUES 
(1, 'Antirrábica'),
(2, 'Parvovirus'),
(3, 'Triple Felina'),
(4, 'Pentavalente');

INSERT INTO Pregunta (IdPregunta, Texto, Activa, Precaucion) VALUES 
(1, '¿La vivienda cuenta con un botiquín de primeros auxilios?', 1, 0),
(2, '¿Se conocen las rutas de evacuación del sector?', 1, 0),
(3, '¿Hay extintor vigente en la vivienda?', 1, 0),
(4, '¿Los integrantes conocen los números de emergencia?', 1, 0);

-- Inserción de un usuario de pruebas con contraseña 'admin123'
INSERT INTO Usuario (IdUsuario, Nombre, IdRol, IdOrganizacion, IdGenero, Email, Contrasena, Activo) VALUES 
(1, 'Usuario Voluntario Test', 3, 1, 3, 'voluntario@test.com', 'admin123', 1),
(2, 'Usuario Supervisor Test', 2, 1, 3, 'supervisor@test.com', 'admin123', 1);
