package com.apiturnos.turno.dto;

import com.apiturnos.turno.service.ResultadoCancelacionTurno;
import com.apiturnos.turno.service.TipoResolucionCancelacion;

public class CancelarTurnoResponseDto {

    private String resultado;
    private Long turnoId;
    private String mensaje;
    private TurnoResponseDto turno;

    public CancelarTurnoResponseDto() {
    }

    public static CancelarTurnoResponseDto eliminadoAnticipadamente(Long turnoId) {
        CancelarTurnoResponseDto dto = new CancelarTurnoResponseDto();
        dto.resultado = "ELIMINADO_ANTICIPADAMENTE";
        dto.turnoId = turnoId;
        dto.mensaje = "El turno fue eliminado anticipadamente sin conservar registro histórico.";
        dto.turno = null;
        return dto;
    }

    public static CancelarTurnoResponseDto canceladoConHistorial(Long turnoId, TurnoResponseDto turno) {
        CancelarTurnoResponseDto dto = new CancelarTurnoResponseDto();
        dto.resultado = "CANCELADO";
        dto.turnoId = turnoId;
        dto.mensaje = "El turno fue cancelado dentro del umbral y conservado en el historial.";
        dto.turno = turno;
        return dto;
    }

    public static CancelarTurnoResponseDto fromResultado(ResultadoCancelacionTurno resultadoCancelacion,
                                                          TurnoResponseDto turnoDto) {
        if (resultadoCancelacion.resolucion() == TipoResolucionCancelacion.ELIMINACION_ANTICIPADA) {
            return eliminadoAnticipadamente(resultadoCancelacion.turnoId());
        } else {
            return canceladoConHistorial(resultadoCancelacion.turnoId(), turnoDto);
        }
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public Long getTurnoId() {
        return turnoId;
    }

    public void setTurnoId(Long turnoId) {
        this.turnoId = turnoId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public TurnoResponseDto getTurno() {
        return turno;
    }

    public void setTurno(TurnoResponseDto turno) {
        this.turno = turno;
    }
}

