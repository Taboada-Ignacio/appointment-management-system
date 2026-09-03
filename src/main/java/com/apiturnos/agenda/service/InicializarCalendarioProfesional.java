package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.DiaSemanaConfiguracionDto;
import com.apiturnos.agenda.dto.InicializarCalendarioResponseDto;
import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class InicializarCalendarioProfesional {

    private final AgendaAnualRepository agendaAnualRepository;
    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final CrearAgendaAnual crearAgendaAnual;
    private final ConfigurarMesModoSemana configurarMesModoSemana;
    private final ActivarInactivarMesAgenda activarInactivarMesAgenda;
    private final GestorCambioEstado gestorCambioEstado;
    private final Clock clock;

    public InicializarCalendarioProfesional(
            AgendaAnualRepository agendaAnualRepository,
            MesAgendaRepository mesAgendaRepository,
            DiaAgendaRepository diaAgendaRepository,
            CrearAgendaAnual crearAgendaAnual,
            ConfigurarMesModoSemana configurarMesModoSemana,
            ActivarInactivarMesAgenda activarInactivarMesAgenda,
            GestorCambioEstado gestorCambioEstado,
            Clock clock) {
        this.agendaAnualRepository = agendaAnualRepository;
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.crearAgendaAnual = crearAgendaAnual;
        this.configurarMesModoSemana = configurarMesModoSemana;
        this.activarInactivarMesAgenda = activarInactivarMesAgenda;
        this.gestorCambioEstado = gestorCambioEstado;
        this.clock = clock;
    }

    @Transactional
    public InicializarCalendarioResponseDto ejecutar(
            Long profesionalId,
            List<DiaSemanaConfiguracionDto> diasSemana,
            boolean repetirAlMesSiguiente,
            String usuario) {
        List<ConfigurarMesModoSemana.DiaSemanaTemplate> templates = diasSemana.stream()
                .map(dia -> new ConfigurarMesModoSemana.DiaSemanaTemplate(
                        dia.getDiaSemana(),
                        dia.getBrechas().stream()
                                .map(b -> new ConfigurarDiaAgenda.BrechaInput(b.getHoraInicio(), b.getHoraFin()))
                                .toList()))
                .toList();
        configurarMesModoSemana.validarPlantillas(templates);

        YearMonth actual = YearMonth.now(clock);
        YearMonth siguiente = actual.plusMonths(1);
        Set<Integer> anios = new LinkedHashSet<>(List.of(actual.getYear(), siguiente.getYear()));

        for (Integer anio : anios) {
            if (agendaAnualRepository.findByProfesionalIdAndAnio(profesionalId, anio).isEmpty()) {
                crearAgendaAnual.ejecutar(profesionalId, anio, usuario);
            }
        }

        List<YearMonth> objetivos = repetirAlMesSiguiente
                ? List.of(actual, siguiente)
                : List.of(actual);
        List<InicializarCalendarioResponseDto.MesConfiguradoDto> resumenMeses = new ArrayList<>();
        for (YearMonth objetivo : objetivos) {
            MesAgenda mes = obtenerMes(profesionalId, objetivo);
            configurarMesModoSemana.ejecutar(profesionalId, mes.getId(), templates, usuario);
            if (!"ACTIVO".equals(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, mes.getId()))) {
                activarInactivarMesAgenda.activar(profesionalId, mes.getId(), usuario);
            }
            resumenMeses.add(resumir(mes));
        }

        return new InicializarCalendarioResponseDto(
                true,
                List.copyOf(anios),
                resumenMeses,
                diasSemana.size(),
                repetirAlMesSiguiente);
    }

    private MesAgenda obtenerMes(Long profesionalId, YearMonth objetivo) {
        AgendaAnual agenda = agendaAnualRepository
                .findByProfesionalIdAndAnio(profesionalId, objetivo.getYear())
                .orElseThrow();
        return mesAgendaRepository.findByAgendaAnualIdAndNroMes(agenda.getId(), objetivo.getMonthValue())
                .orElseThrow();
    }

    private InicializarCalendarioResponseDto.MesConfiguradoDto resumir(MesAgenda mes) {
        List<DiaAgenda> dias = diaAgendaRepository.findByMesAgendaId(mes.getId());
        var estados = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.DIA_AGENDA, dias.stream().map(DiaAgenda::getId).toList());
        int activos = (int) dias.stream().filter(dia -> "ACTIVO".equals(estados.get(dia.getId()))).count();
        return new InicializarCalendarioResponseDto.MesConfiguradoDto(
                mes.getId(), mes.getAgendaAnual().getAnio(), mes.getNroMes(),
                gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, mes.getId()),
                activos, dias.size() - activos);
    }
}
