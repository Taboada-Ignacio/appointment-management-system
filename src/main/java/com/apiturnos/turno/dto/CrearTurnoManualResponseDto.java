package com.apiturnos.turno.dto;

import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.service.DatosConfirmacionTurnoManual;
import com.apiturnos.turno.service.ResultadoCrearTurnoManual;
import com.apiturnos.turno.service.ValidadorCrearTurnoManual;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class CrearTurnoManualResponseDto {

    private Long id;
    private Long turnoId;
    private boolean creado;
    private boolean puedeCrear;
    private boolean requiereConfirmacion;
    private String estado;
    private List<AdvertenciaTurnoManual> advertencias;
    private DatosConfirmacionResponseDto datosConfirmacion;
    private ClienteConfirmacionDto cliente;
    private TipoAtencionConfirmacionDto tipoAtencion;
    private LocalDate fecha;
    private Instant inicioEstimado;
    private Instant finEstimado;
    private String observaciones;

    public CrearTurnoManualResponseDto() {
    }

    public static CrearTurnoManualResponseDto fromResultado(
            ResultadoCrearTurnoManual resultado,
            Instant inicioEstimado,
            Instant finEstimado,
            String observaciones,
            Integer duracionMinutos,
            Integer capacidadSimultanea) {

        CrearTurnoManualResponseDto dto = new CrearTurnoManualResponseDto();
        dto.id = resultado.turnoId();
        dto.turnoId = resultado.turnoId();
        dto.creado = resultado.creado();
        dto.puedeCrear = resultado.puedeCrear();
        dto.requiereConfirmacion = resultado.requiereConfirmacion();
        dto.estado = resultado.creado() ? "ASIGNADO" : null;
        dto.advertencias = resultado.advertencias();
        dto.inicioEstimado = inicioEstimado;
        dto.finEstimado = finEstimado;
        dto.observaciones = observaciones;

        DatosConfirmacionTurnoManual datos = resultado.datosConfirmacion();
        if (datos != null) {
            dto.datosConfirmacion = new DatosConfirmacionResponseDto(datos);
            dto.fecha = datos.fecha();
            dto.cliente = new ClienteConfirmacionDto(
                    datos.clienteId(),
                    datos.nombreCliente(),
                    datos.apellidoCliente(),
                    datos.tipoDocumento(),
                    datos.numeroDocumento());
            dto.tipoAtencion = new TipoAtencionConfirmacionDto(
                    datos.tipoAtencionId(),
                    datos.tipoAtencion(),
                    duracionMinutos,
                    capacidadSimultanea);
        }
        return dto;
    }

    public static CrearTurnoManualResponseDto fromContextoValidado(
            ValidadorCrearTurnoManual.ContextoValidado contexto,
            Instant inicioEstimado,
            Instant finEstimado,
            String observaciones) {

        CrearTurnoManualResponseDto dto = new CrearTurnoManualResponseDto();
        dto.creado = false;
        dto.puedeCrear = true;
        dto.requiereConfirmacion = !contexto.advertencias().isEmpty();
        dto.advertencias = contexto.advertencias();
        dto.inicioEstimado = inicioEstimado;
        dto.finEstimado = finEstimado;
        dto.observaciones = observaciones;

        DatosConfirmacionTurnoManual datos = contexto.datosConfirmacion();
        if (datos != null) {
            dto.datosConfirmacion = new DatosConfirmacionResponseDto(datos);
            dto.fecha = datos.fecha();
            dto.cliente = new ClienteConfirmacionDto(
                    datos.clienteId(),
                    datos.nombreCliente(),
                    datos.apellidoCliente(),
                    datos.tipoDocumento(),
                    datos.numeroDocumento());
            dto.tipoAtencion = new TipoAtencionConfirmacionDto(
                    contexto.tipoAtencion().getId(),
                    contexto.tipoAtencion().getNombre(),
                    contexto.tipoAtencion().getDuracionMinutos(),
                    contexto.tipoAtencion().getCapacidadSimultanea());
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

    public boolean isCreado() {
        return creado;
    }

    public void setCreado(boolean creado) {
        this.creado = creado;
    }

    public boolean isPuedeCrear() {
        return puedeCrear;
    }

    public void setPuedeCrear(boolean puedeCrear) {
        this.puedeCrear = puedeCrear;
    }

    public boolean isRequiereConfirmacion() {
        return requiereConfirmacion;
    }

    public void setRequiereConfirmacion(boolean requiereConfirmacion) {
        this.requiereConfirmacion = requiereConfirmacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<AdvertenciaTurnoManual> getAdvertencias() {
        return advertencias;
    }

    public void setAdvertencias(List<AdvertenciaTurnoManual> advertencias) {
        this.advertencias = advertencias;
    }

    public DatosConfirmacionResponseDto getDatosConfirmacion() {
        return datosConfirmacion;
    }

    public void setDatosConfirmacion(DatosConfirmacionResponseDto datosConfirmacion) {
        this.datosConfirmacion = datosConfirmacion;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

