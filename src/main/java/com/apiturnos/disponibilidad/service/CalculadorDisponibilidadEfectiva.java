package com.apiturnos.disponibilidad.service;

import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.disponibilidad.model.IntervaloHorario;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Calcula la disponibilidad efectiva de un día sin alterar ni persistir su configuración base.
 *
 * <p>Precedencia determinística aplicada como pipeline de negocio (orden 1 → 2 → 3 → 4):
 * <ol>
 *   <li><b>Cierre completo (VACACIONES, DIA_NO_LABORABLE, etc.):</b> Cortocircuito total. El día queda sin disponibilidad.</li>
 *   <li><b>MODIFICACION_HORARIO:</b> Sustituye la disponibilidad base por los nuevos intervalos definidos.</li>
 *   <li><b>BLOQUEO_HORARIO:</b> Sustrae/resta los tramos horarios bloqueados de la disponibilidad actual.</li>
 *   <li><b>HABILITACION_EXTRAORDINARIA:</b> Añade/une tramos extraordinarios a la disponibilidad resultante.</li>
 * </ol>
 * </p>
 */
@Component
public class CalculadorDisponibilidadEfectiva {

    private static final Comparator<IntervaloHorario> ORDEN_INTERVALOS =
            Comparator.comparing(IntervaloHorario::inicio)
                    .thenComparing(IntervaloHorario::fin);

    public List<IntervaloHorario> calcular(
            Collection<IntervaloHorario> disponibilidadBase,
            Collection<ExcepcionAgenda> excepcionesAplicables) {

        Objects.requireNonNull(disponibilidadBase, "La disponibilidad base es obligatoria");
        Objects.requireNonNull(excepcionesAplicables, "Las excepciones aplicables son obligatorias");

        List<IntervaloHorario> baseNormalizada = normalizar(disponibilidadBase);
        List<ExcepcionAgenda> excepcionesActivas = excepcionesAplicables.stream()
                .map(excepcion -> Objects.requireNonNull(excepcion, "Una excepción aplicable no puede ser nula"))
                .filter(excepcion -> Boolean.TRUE.equals(excepcion.getActiva()))
                .toList();

        validarTipos(excepcionesActivas);

        // 1. Cierre completo: cortocircuito
        if (excepcionesActivas.stream().anyMatch(this::esCierreCompleto)) {
            return List.of();
        }

        // 2. Modificación horaria: reemplaza la disponibilidad base si existe
        List<IntervaloHorario> modificaciones = extraerIntervalos(
                excepcionesActivas, TipoExcepcion.MODIFICACION_HORARIO);

        List<IntervaloHorario> resultado = modificaciones.isEmpty()
                ? baseNormalizada
                : normalizar(modificaciones);

        // 3. Bloqueo horario: resta franjas bloqueadas
        List<IntervaloHorario> bloqueos = new ArrayList<>(extraerIntervalos(
                excepcionesActivas, TipoExcepcion.BLOQUEO_HORARIO));
        bloqueos.addAll(extraerIntervalos(excepcionesActivas, TipoExcepcion.EXCEPCION_HORARIA));
        resultado = restar(resultado, normalizar(bloqueos));

        // 4. Habilitación extraordinaria: une franjas habilitadas al final
        List<IntervaloHorario> habilitaciones = extraerIntervalos(
                excepcionesActivas, TipoExcepcion.HABILITACION_EXTRAORDINARIA);
        resultado = unir(resultado, habilitaciones);

        return resultado;
    }

    /**
     * Ordena los intervalos y une tanto solapamientos como extremos contiguos.
     */
    public List<IntervaloHorario> normalizar(Collection<IntervaloHorario> intervalos) {
        Objects.requireNonNull(intervalos, "Los intervalos son obligatorios");
        if (intervalos.isEmpty()) {
            return List.of();
        }

        List<IntervaloHorario> ordenados = intervalos.stream()
                .map(intervalo -> Objects.requireNonNull(intervalo, "Un intervalo no puede ser nulo"))
                .sorted(ORDEN_INTERVALOS)
                .toList();

        List<IntervaloHorario> normalizados = new ArrayList<>();
        IntervaloHorario actual = ordenados.getFirst();

        for (int indice = 1; indice < ordenados.size(); indice++) {
            IntervaloHorario siguiente = ordenados.get(indice);
            if (!siguiente.inicio().isAfter(actual.fin())) {
                LocalTime nuevoFin = siguiente.fin().isAfter(actual.fin())
                        ? siguiente.fin()
                        : actual.fin();
                actual = new IntervaloHorario(actual.inicio(), nuevoFin);
            } else {
                normalizados.add(actual);
                actual = siguiente;
            }
        }

        normalizados.add(actual);
        return List.copyOf(normalizados);
    }

    private void validarTipos(List<ExcepcionAgenda> excepciones) {
        for (ExcepcionAgenda excepcion : excepciones) {
            Objects.requireNonNull(excepcion.getTipo(), "El tipo de excepción es obligatorio");
        }
    }

    private boolean esCierreCompleto(ExcepcionAgenda excepcion) {
        return switch (excepcion.getTipo()) {
            case DIA_NO_LABORABLE, VACACIONES, FERIADO, DIA_DADO_DE_BAJA, OTRO -> true;
            case EXCEPCION_HORARIA ->
                    excepcion.getHoraInicio() == null && (excepcion.getBrechas() == null || excepcion.getBrechas().isEmpty());
            case BLOQUEO_HORARIO, HABILITACION_EXTRAORDINARIA, MODIFICACION_HORARIO -> false;
        };
    }

    private List<IntervaloHorario> extraerIntervalos(
            List<ExcepcionAgenda> excepciones,
            TipoExcepcion tipo) {

        return excepciones.stream()
                .filter(excepcion -> excepcion.getTipo() == tipo)
                .flatMap(excepcion -> {
                    List<IntervaloHorario> intervalos = excepcion.obtenerIntervalos();
                    if (intervalos.isEmpty()) {
                        throw new IllegalArgumentException(
                                "La excepción " + excepcion.getTipo() + " requiere hora de inicio y hora de fin");
                    }
                    return intervalos.stream();
                })
                .toList();
    }

    private List<IntervaloHorario> unir(
            Collection<IntervaloHorario> primeros,
            Collection<IntervaloHorario> segundos) {

        List<IntervaloHorario> union = new ArrayList<>(primeros.size() + segundos.size());
        union.addAll(primeros);
        union.addAll(segundos);
        return normalizar(union);
    }

    private List<IntervaloHorario> restar(
            List<IntervaloHorario> disponibles,
            List<IntervaloHorario> bloqueos) {

        if (disponibles.isEmpty() || bloqueos.isEmpty()) {
            return List.copyOf(disponibles);
        }

        List<IntervaloHorario> resultado = new ArrayList<>();
        for (IntervaloHorario disponible : disponibles) {
            LocalTime cursor = disponible.inicio();

            for (IntervaloHorario bloqueo : bloqueos) {
                if (!bloqueo.fin().isAfter(cursor)) {
                    continue;
                }
                if (!bloqueo.inicio().isBefore(disponible.fin())) {
                    break;
                }

                if (cursor.isBefore(bloqueo.inicio())) {
                    LocalTime finTramo = bloqueo.inicio().isBefore(disponible.fin())
                            ? bloqueo.inicio()
                            : disponible.fin();
                    if (cursor.isBefore(finTramo)) {
                        resultado.add(new IntervaloHorario(cursor, finTramo));
                    }
                }

                if (bloqueo.fin().isAfter(cursor)) {
                    cursor = bloqueo.fin();
                }
                if (!cursor.isBefore(disponible.fin())) {
                    break;
                }
            }

            if (cursor.isBefore(disponible.fin())) {
                resultado.add(new IntervaloHorario(cursor, disponible.fin()));
            }
        }

        return List.copyOf(resultado);
    }
}
