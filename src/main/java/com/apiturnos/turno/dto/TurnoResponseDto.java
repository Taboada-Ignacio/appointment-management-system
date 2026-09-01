package com.apiturnos.turno.dto;

import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;

import java.time.Instant;
import java.time.LocalDate;

public class TurnoResponseDto {

    private Long id;
    private Long turnoId;
    private Long profesionalId;
    private Long clienteId;
    private Long diaAgendaId;
    private Long tipoAtencionId;
    private LocalDate fecha;
    private Instant inicioEstimado;
    private Instant finEstimado;
    private Instant inicioReal;
    private Instant finReal;
    private OrigenTurno origen;
    private String estado;
    private String observaciones;
    private ClienteConfirmacionDto cliente;
    private TipoAtencionConfirmacionDto tipoAtencion;

    public TurnoResponseDto() {
    }

    public static TurnoResponseDto from(Turno turno, String estado) {
        if (turno == null) {
            return null;
        }
        TurnoResponseDto dto = new TurnoResponseDto();
        dto.id = turno.getId();
        dto.turnoId = turno.getId();
        dto.inicioEstimado = turno.getInicioEstimado();
        dto.finEstimado = turno.getFinEstimado();
        dto.inicioReal = turno.getInicioReal();
        dto.finReal = turno.getFinReal();
        dto.origen = turno.getOrigen();
        dto.observaciones = turno.getObservaciones();
        dto.estado = estado;

        if (turno.getDiaAgenda() != null && org.hibernate.Hibernate.isInitialized(turno.getDiaAgenda())) {
            dto.diaAgendaId = turno.getDiaAgenda().getId();
            dto.fecha = turno.getDiaAgenda().getFecha();
            if (turno.getDiaAgenda().getMesAgenda() != null
                    && org.hibernate.Hibernate.isInitialized(turno.getDiaAgenda().getMesAgenda())
                    && turno.getDiaAgenda().getMesAgenda().getAgendaAnual() != null
                    && org.hibernate.Hibernate.isInitialized(turno.getDiaAgenda().getMesAgenda().getAgendaAnual())
                    && turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional() != null
                    && org.hibernate.Hibernate.isInitialized(turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional())) {
                dto.profesionalId = turno.getDiaAgenda().getMesAgenda().getAgendaAnual().getProfesional().getId();
            }
        }

        if (turno.getCliente() != null && org.hibernate.Hibernate.isInitialized(turno.getCliente())) {
            Cliente c = turno.getCliente();
            dto.clienteId = c.getId();
            dto.cliente = new ClienteConfirmacionDto(
                    c.getId(),
                    c.getNombre(),
                    c.getApellido(),
                    c.getTipoDocumento(),
                    c.getNumeroDocumento());
        }

        if (turno.getTipoAtencion() != null && org.hibernate.Hibernate.isInitialized(turno.getTipoAtencion())) {
            TipoAtencion ta = turno.getTipoAtencion();
            dto.tipoAtencionId = ta.getId();
            dto.tipoAtencion = new TipoAtencionConfirmacionDto(
                    ta.getId(),
                    ta.getNombre(),
                    ta.getDuracionMinutos(),
                    ta.getCapacidadSimultanea());
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTurnoId() {
        return turnoId;
    }

    public void setTurnoId(Long turnoId) {
        this.turnoId = turnoId;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public void setProfesionalId(Long profesionalId) {
        this.profesionalId = profesionalId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getDiaAgendaId() {
        return diaAgendaId;
    }

    public void setDiaAgendaId(Long diaAgendaId) {
        this.diaAgendaId = diaAgendaId;
    }

    public Long getTipoAtencionId() {
        return tipoAtencionId;
    }

    public void setTipoAtencionId(Long tipoAtencionId) {
        this.tipoAtencionId = tipoAtencionId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Instant getInicioEstimado() {
        return inicioEstimado;
    }

    public void setInicioEstimado(Instant inicioEstimado) {
        this.inicioEstimado = inicioEstimado;
    }

    public Instant getFinEstimado() {
        return finEstimado;
    }

    public void setFinEstimado(Instant finEstimado) {
        this.finEstimado = finEstimado;
    }

    public Instant getInicioReal() {
        return inicioReal;
    }

    public void setInicioReal(Instant inicioReal) {
        this.inicioReal = inicioReal;
    }

    public Instant getFinReal() {
        return finReal;
    }

    public void setFinReal(Instant finReal) {
        this.finReal = finReal;
    }

    public OrigenTurno getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenTurno origen) {
        this.origen = origen;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public ClienteConfirmacionDto getCliente() {
        return cliente;
    }

    public void setCliente(ClienteConfirmacionDto cliente) {
        this.cliente = cliente;
    }

    public TipoAtencionConfirmacionDto getTipoAtencion() {
        return tipoAtencion;
    }

    public void setTipoAtencion(TipoAtencionConfirmacionDto tipoAtencion) {
        this.tipoAtencion = tipoAtencion;
    }
}
