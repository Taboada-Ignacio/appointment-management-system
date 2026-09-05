package com.apiturnos.agenda.service;

import com.apiturnos.agenda.dto.DiaAgendaResumenResponseDto;
import com.apiturnos.agenda.dto.MesAgendaDetalleResponseDto;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.EntidadNoEncontradaException;
import com.apiturnos.turno.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ObtenerDetalleMesAgenda {

    private final MesAgendaRepository mesAgendaRepository;
    private final DiaAgendaRepository diaAgendaRepository;
    private final BrechaHorariaRepository brechaHorariaRepository;
    private final GestorCambioEstado gestorCambioEstado;
    private final TurnoRepository turnoRepository;
    private final ExcepcionAgendaRepository excepcionAgendaRepository;

    public ObtenerDetalleMesAgenda(MesAgendaRepository mesAgendaRepository,
                                  DiaAgendaRepository diaAgendaRepository,
                                  BrechaHorariaRepository brechaHorariaRepository,
                                  GestorCambioEstado gestorCambioEstado,
                                  TurnoRepository turnoRepository,
                                  ExcepcionAgendaRepository excepcionAgendaRepository) {
        this.mesAgendaRepository = mesAgendaRepository;
        this.diaAgendaRepository = diaAgendaRepository;
        this.brechaHorariaRepository = brechaHorariaRepository;
        this.gestorCambioEstado = gestorCambioEstado;
        this.turnoRepository = turnoRepository;
        this.excepcionAgendaRepository = excepcionAgendaRepository;
    }

    @Transactional(readOnly = true)
    public MesAgendaDetalleResponseDto ejecutar(Long profesionalId, Long mesAgendaId) {
        MesAgenda mes = mesAgendaRepository.findByIdWithAgendaAndProfesional(mesAgendaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("MesAgenda", mesAgendaId));

        if (!mes.getAgendaAnual().getProfesional().getId().equals(profesionalId)) {
            throw new ClienteNoPerteneceProfesionalException(mesAgendaId, profesionalId);
        }

        String estadoMes = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA, mes.getId());
        List<DiaAgenda> dias = diaAgendaRepository.findByMesAgendaId(mes.getId());
        List<Long> diaIds = dias.stream().map(DiaAgenda::getId).toList();

        Map<Long, String> estadosDiasMap = gestorCambioEstado.obtenerEstadosActualesPorEntidades(
                AmbitoEstado.DIA_AGENDA, diaIds);
        Map<Long, Long> turnosPorDia = (diaIds.isEmpty() ? List.<com.apiturnos.turno.model.Turno>of()
                : turnoRepository.findByDiaAgendaIdIn(diaIds)).stream()
                .collect(Collectors.groupingBy(turno -> turno.getDiaAgenda().getId(), Collectors.counting()));
        Long profesionalIdReal = mes.getAgendaAnual().getProfesional().getId();
        var excepciones = dias.isEmpty() ? List.<com.apiturnos.agenda.model.ExcepcionAgenda>of()
                : excepcionAgendaRepository.findActivasIntersectandoRango(
                        profesionalIdReal,
                        dias.stream().map(DiaAgenda::getFecha).min(Comparator.naturalOrder()).orElseThrow(),
                        dias.stream().map(DiaAgenda::getFecha).max(Comparator.naturalOrder()).orElseThrow());

        List<DiaAgendaResumenResponseDto> diasDto = dias.stream()
                .sorted(Comparator.comparing(DiaAgenda::getFecha))
                .map(dia -> {
                    var modificacionesDia = excepciones.stream()
                            .filter(com.apiturnos.agenda.model.ExcepcionAgenda::isActiva)
                            .filter(excepcion -> excepcion.aplicaEn(dia.getFecha()))
                            .filter(excepcion -> excepcion.getTipo() == com.apiturnos.agenda.model.TipoExcepcion.MODIFICACION_HORARIO)
                            .toList();
                    int cantBrechas = modificacionesDia.isEmpty()
                            ? brechaHorariaRepository.findByDiaAgendaId(dia.getId()).size()
                            : modificacionesDia.stream().mapToInt(e -> e.obtenerIntervalos().size()).sum();
                    int cantBrechasHabilitadas = excepciones.stream()
                            .filter(com.apiturnos.agenda.model.ExcepcionAgenda::isActiva)
                            .filter(excepcion -> excepcion.aplicaEn(dia.getFecha()))
                            .filter(excepcion -> excepcion.getTipo() == com.apiturnos.agenda.model.TipoExcepcion.HABILITACION_EXTRAORDINARIA)
                            .mapToInt(excepcion -> excepcion.obtenerIntervalos().size())
                            .sum();
                    var tiposExcepcion = excepciones.stream()
                            .filter(com.apiturnos.agenda.model.ExcepcionAgenda::isActiva)
                            .filter(excepcion -> excepcion.aplicaEn(dia.getFecha()))
                            .map(com.apiturnos.agenda.model.ExcepcionAgenda::getTipo)
                            .distinct()
                            .toList();
                    return new DiaAgendaResumenResponseDto(dia, cantBrechas + cantBrechasHabilitadas,
                            turnosPorDia.getOrDefault(dia.getId(), 0L).intValue(), tiposExcepcion,
                            estadosDiasMap.get(dia.getId()));
                })
                .toList();

        return new MesAgendaDetalleResponseDto(mes, estadoMes, diasDto);
    }
}

