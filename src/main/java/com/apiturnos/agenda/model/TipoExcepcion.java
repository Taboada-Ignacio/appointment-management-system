package com.apiturnos.agenda.model;

public enum TipoExcepcion {
    DIA_NO_LABORABLE,
    VACACIONES,
    BLOQUEO_HORARIO,
    HABILITACION_EXTRAORDINARIA,
    MODIFICACION_HORARIO,

    // Valores legacy conservados para no romper datos ni clientes existentes.
    FERIADO,
    DIA_DADO_DE_BAJA,
    EXCEPCION_HORARIA,
    OTRO;

    public boolean esCierreDiaCompleto() {
        return this == DIA_NO_LABORABLE
                || this == VACACIONES
                || this == FERIADO
                || this == DIA_DADO_DE_BAJA
                || this == OTRO;
    }

    public boolean esBloqueoHorario() {
        return this == BLOQUEO_HORARIO || this == EXCEPCION_HORARIA;
    }

    public boolean requiereHorario() {
        return esBloqueoHorario()
                || this == HABILITACION_EXTRAORDINARIA
                || this == MODIFICACION_HORARIO;
    }
}
