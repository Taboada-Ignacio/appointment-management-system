package com.apiturnos.atencion;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.atencion.service.ActivarTipoAtencion;
import com.apiturnos.atencion.service.EditarTipoAtencion;
import com.apiturnos.atencion.service.InactivarTipoAtencion;
import com.apiturnos.atencion.service.ListarTiposAtencion;
import com.apiturnos.atencion.service.ObtenerTipoAtencion;
import com.apiturnos.atencion.service.RegistrarTipoAtencion;
import com.apiturnos.atencion.service.VerificarCapacidadTipoAtencion;
import com.apiturnos.auditoria.repository.AuditoriaEventoRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.disponibilidad.dto.SlotDisponibleDto;
import com.apiturnos.disponibilidad.service.CalcularSlotsDisponiblesAutogestion;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.CapacidadAgotadaException;
import com.apiturnos.shared.exception.NegocioException;
import com.apiturnos.shared.exception.TipoAtencionNoPerteneceProfesionalException;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import com.apiturnos.turno.service.CrearTurno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "turnos.zona-horaria=UTC")
@Testcontainers
class TipoAtencionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AgendaAnualRepository agendaAnualRepository;

    @Autowired
    private MesAgendaRepository mesAgendaRepository;

    @Autowired
    private DiaAgendaRepository diaAgendaRepository;

    @Autowired
    private BrechaHorariaRepository brechaHorariaRepository;

    @Autowired
    private TipoAtencionRepository tipoAtencionRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Autowired
    private com.apiturnos.notificacion.repository.NotificacionRepository notificacionRepository;

    @Autowired
    private com.apiturnos.estado.repository.CambioEstadoRepository cambioEstadoRepository;

    @Autowired
    private com.apiturnos.turno.repository.TurnoHistorialRepository turnoHistorialRepository;

    @Autowired
    private com.apiturnos.turno.repository.MotivoBajaTurnoRepository motivoBajaTurnoRepository;

    @Autowired
    private GestorCambioEstado gestorCambioEstado;

    @Autowired
    private RegistrarTipoAtencion registrarTipoAtencion;

    @Autowired
    private EditarTipoAtencion editarTipoAtencion;

    @Autowired
    private ActivarTipoAtencion activarTipoAtencion;

    @Autowired
    private InactivarTipoAtencion inactivarTipoAtencion;

    @Autowired
    private ListarTiposAtencion listarTiposAtencion;

    @Autowired
    private ObtenerTipoAtencion obtenerTipoAtencion;

    @Autowired
    private VerificarCapacidadTipoAtencion verificarCapacidadTipoAtencion;

    @Autowired
    private CalcularSlotsDisponiblesAutogestion calcularSlotsDisponiblesAutogestion;

    @Autowired
    private CrearTurno crearTurno;

    private Profesional profesional1;
    private Profesional profesional2;
    private Cliente cliente1;
    private DiaAgenda diaAgenda1;

    @BeforeEach
    void setUp() {
        notificacionRepository.deleteAll();
        cambioEstadoRepository.deleteAll();
        turnoHistorialRepository.deleteAll();
        turnoRepository.deleteAll();
        motivoBajaTurnoRepository.deleteAll();
        brechaHorariaRepository.deleteAll();
        tipoAtencionRepository.deleteAll();
        diaAgendaRepository.deleteAll();
        mesAgendaRepository.deleteAll();
        agendaAnualRepository.deleteAll();
        clienteRepository.deleteAll();
        profesionalRepository.deleteAll();

        profesional1 = new Profesional();
        profesional1.setNombre("Dr. Martín");
        profesional1.setApellido("Pérez");
        profesional1.setEmail("martin@test.com");
        profesional1.setTelefono("123456");
        profesional1.setEspecialidad("Odontología");
        profesional1 = profesionalRepository.save(profesional1);

        profesional2 = new Profesional();
        profesional2.setNombre("Dra. Laura");
        profesional2.setApellido("Gómez");
        profesional2.setEmail("laura@test.com");
        profesional2.setTelefono("654321");
        profesional2.setEspecialidad("Dermatología");
        profesional2 = profesionalRepository.save(profesional2);

        cliente1 = new Cliente();
        cliente1.setProfesional(profesional1);
        cliente1.setNombre("Carlos");
        cliente1.setApellido("López");
        cliente1.setEmail("carlos@test.com");
        cliente1.setTelefono("11223344");
        cliente1.setTipoDocumento(TipoDocumento.DNI);
        cliente1.setNumeroDocumento("30111222");
        cliente1 = clienteRepository.save(cliente1);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.CLIENTE, cliente1.getId(), "HABILITADO", "admin", "Cliente habilitado");

        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesional1);
        agenda.setAnio(2026);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(9);
        mes = mesAgendaRepository.save(mes);

        diaAgenda1 = new DiaAgenda();
        diaAgenda1.setMesAgenda(mes);
        diaAgenda1.setFecha(LocalDate.of(2026, 9, 1));
        diaAgenda1 = diaAgendaRepository.save(diaAgenda1);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.DIA_AGENDA, diaAgenda1.getId(), "ACTIVO", "admin", "Día activo");

        BrechaHoraria brecha = new BrechaHoraria();
        brecha.setDiaAgenda(diaAgenda1);
        brecha.setHoraInicioAtencion(LocalTime.of(8, 0));
        brecha.setHoraFinAtencion(LocalTime.of(12, 0));
        brechaHorariaRepository.save(brecha);
    }

    @Test
    @DisplayName("1. Un Profesional puede tener múltiples Tipos de Atención")
    void test01_profesionalPuedeTenerMultiplesTiposAtencion() {
        TipoAtencion t1 = registrarTipoAtencion.ejecutar(profesional1.getId(), "Consulta General", "Chequeo", 30, 1, "admin");
        TipoAtencion t2 = registrarTipoAtencion.ejecutar(profesional1.getId(), "Extracción", "Cirugía menor", 45, 1, "admin");
        TipoAtencion t3 = registrarTipoAtencion.ejecutar(profesional1.getId(), "Limpieza Dental", "Profilaxis", 20, 2, "admin");

        List<TipoAtencion> lista = listarTiposAtencion.ejecutar(profesional1.getId(), false);
        assertThat(lista).hasSize(3);
        assertThat(lista).extracting(TipoAtencion::getNombre)
                .containsExactly("Consulta General", "Extracción", "Limpieza Dental");
    }

    @Test
    @DisplayName("2. TipoAtencion pertenece a un único Profesional y rechaza acceso de otro Profesional")
    void test02_tipoAtencionPerteneceAUnicoProfesional() {
        TipoAtencion t1 = registrarTipoAtencion.ejecutar(profesional1.getId(), "Consulta General", "Chequeo", 30, 1, "admin");

        // Intentar obtener o editar desde profesional2
        assertThatThrownBy(() -> obtenerTipoAtencion.ejecutar(profesional2.getId(), t1.getId()))
                .isInstanceOf(TipoAtencionNoPerteneceProfesionalException.class);

        assertThatThrownBy(() -> editarTipoAtencion.ejecutar(profesional2.getId(), t1.getId(), "Nuevo", "Desc", 30, 1, "admin"))
                .isInstanceOf(TipoAtencionNoPerteneceProfesionalException.class);

        assertThatThrownBy(() -> inactivarTipoAtencion.ejecutar(profesional2.getId(), t1.getId(), "admin"))
                .isInstanceOf(TipoAtencionNoPerteneceProfesionalException.class);
    }

    @Test
    @DisplayName("3. Validaciones de duración > 0 y capacidad >= 1")
    void test03_validacionesDuracionYCapacidad() {
        assertThatThrownBy(() -> registrarTipoAtencion.ejecutar(profesional1.getId(), "Inválido", null, 0, 1, "admin"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("duración");

        assertThatThrownBy(() -> registrarTipoAtencion.ejecutar(profesional1.getId(), "Inválido", null, 30, 0, "admin"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("capacidad");
    }

    @Test
    @DisplayName("4. TipoAtencion INACTIVO no se ofrece para nuevos Turnos")
    void test04_tipoAtencionInactivo_noSeOfreceParaNuevosTurnos() {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(profesional1.getId(), "Consulta Inactiva", null, 30, 1, "admin");
        inactivarTipoAtencion.ejecutar(profesional1.getId(), tipo.getId(), "admin");

        // Autogestión cálculo slots devuelve vacío
        List<SlotDisponibleDto> slots = calcularSlotsDisponiblesAutogestion.ejecutar(
                profesional1.getId(), tipo.getId(), LocalDate.of(2026, 9, 1));
        assertThat(slots).isEmpty();

        // Intento de crear turno con tipo inactivo lanza NegocioException
        Instant inicio = Instant.parse("2026-09-01T08:00:00Z");
        Instant fin = Instant.parse("2026-09-01T08:30:00Z");
        assertThatThrownBy(() -> crearTurno.ejecutar(
                diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                inicio, fin, OrigenTurno.PROFESIONAL, true, "Obs", "admin"))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("5. Capacidad 1: permite 1 turno concurrente; segundo turno detecta sobrecapacidad")
    void test05_capacidad1_permiteUnTurnoConcurrente() {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(profesional1.getId(), "Individual", null, 30, 1, "admin");

        Instant inicio = Instant.parse("2026-09-01T08:00:00Z");
        Instant fin = Instant.parse("2026-09-01T08:30:00Z");

        // Primer turno manual -> OK
        CrearTurno.Resultado r1 = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                inicio, fin, OrigenTurno.PROFESIONAL, false, "Primer turno", "admin");
        assertThat(r1.getTurno()).isNotNull();
        assertThat(r1.isCapacidadExcedida()).isFalse();
        assertThat(r1.isRequiereConfirmacion()).isFalse();

        // Segundo turno manual sin confirmación -> Requiere confirmación
        CrearTurno.Resultado r2 = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                inicio, fin, OrigenTurno.PROFESIONAL, false, "Segundo turno", "admin");
        assertThat(r2.getTurno()).isNull();
        assertThat(r2.isRequiereConfirmacion()).isTrue();
    }

    @Test
    @DisplayName("6. Capacidad 3: permite 3 turnos concurrentes; un cuarto turno detecta sobrecapacidad")
    void test06_capacidad3_permiteTresTurnosConcurrentes() {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(profesional1.getId(), "Grupal", null, 30, 3, "admin");

        Instant inicio = Instant.parse("2026-09-01T08:00:00Z");
        Instant fin = Instant.parse("2026-09-01T08:30:00Z");

        // 3 turnos creados exitosamente sin sobrecapacidad
        CrearTurno.Resultado r1 = crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T1", "admin");
        CrearTurno.Resultado r2 = crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T2", "admin");
        CrearTurno.Resultado r3 = crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T3", "admin");

        assertThat(r1.getTurno()).isNotNull();
        assertThat(r2.getTurno()).isNotNull();
        assertThat(r3.getTurno()).isNotNull();
        assertThat(r3.isCapacidadExcedida()).isFalse();

        // El 4to turno detecta sobrecapacidad
        CrearTurno.Resultado r4 = crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T4", "admin");
        assertThat(r4.getTurno()).isNull();
        assertThat(r4.isRequiereConfirmacion()).isTrue();
    }

    @Test
    @DisplayName("7. Solapamiento parcial también cuenta como concurrencia")
    void test07_solapamientoParcialCuentaComoConcurrencia() {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(profesional1.getId(), "Control", null, 30, 1, "admin");

        // Turno existente 08:00 a 08:30
        crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                Instant.parse("2026-09-01T08:00:00Z"), Instant.parse("2026-09-01T08:30:00Z"),
                OrigenTurno.PROFESIONAL, false, "T1", "admin");

        // Intento de turno solapado parcialmente: 08:15 a 08:45
        CrearTurno.Resultado rSolapado = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                Instant.parse("2026-09-01T08:15:00Z"), Instant.parse("2026-09-01T08:45:00Z"),
                OrigenTurno.PROFESIONAL, false, "T2", "admin");

        assertThat(rSolapado.getTurno()).isNull();
        assertThat(rSolapado.isRequiereConfirmacion()).isTrue();

        // Intervalo contiguo 08:30 a 09:00 -> NO se solapa, entra limpio
        CrearTurno.Resultado rContiguo = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                Instant.parse("2026-09-01T08:30:00Z"), Instant.parse("2026-09-01T09:00:00Z"),
                OrigenTurno.PROFESIONAL, false, "T3", "admin");

        assertThat(rContiguo.getTurno()).isNotNull();
        assertThat(rContiguo.isCapacidadExcedida()).isFalse();
    }

    @Test
    @DisplayName("8. Alta manual con confirmación crea Turno con sobrecapacidad")
    void test08_altaManualConConfirmacion_creaTurno() {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(profesional1.getId(), "Examen", null, 30, 1, "admin");

        Instant inicio = Instant.parse("2026-09-01T08:00:00Z");
        Instant fin = Instant.parse("2026-09-01T08:30:00Z");

        // Turno 1
        crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T1", "admin");

        // Turno 2 con confirmarSobrecapacidad = true
        CrearTurno.Resultado r2 = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                inicio, fin, OrigenTurno.PROFESIONAL, true, "T2 confirmado", "admin");

        assertThat(r2.getTurno()).isNotNull();
        assertThat(r2.isCapacidadExcedida()).isTrue();
    }

    @Test
    @DisplayName("9. Autogestión lanza CapacidadAgotadaException si la capacidad está llena")
    void test09_autogestionLanzaCapacidadAgotadaException() {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(profesional1.getId(), "AutogestionTest", null, 30, 1, "admin");

        Instant inicio = Instant.parse("2026-09-01T08:00:00Z");
        Instant fin = Instant.parse("2026-09-01T08:30:00Z");

        // Llenar capacidad
        crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T1", "admin");

        // Autogestión intenta reservar en ese intervalo -> Error estricto
        assertThatThrownBy(() -> crearTurno.ejecutar(
                diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                inicio, fin, OrigenTurno.CLIENTE_AUTOGESTION, false, "Intento cliente", "cliente"))
                .isInstanceOf(CapacidadAgotadaException.class);
    }

    @Test
    @DisplayName("10. Reducir capacidad no elimina Turnos existentes")
    void test10_reducirCapacidadNoEliminaTurnosExistentes() {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(profesional1.getId(), "CapacidadVariable", null, 30, 3, "admin");

        Instant inicio = Instant.parse("2026-09-01T08:00:00Z");
        Instant fin = Instant.parse("2026-09-01T08:30:00Z");

        crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T1", "admin");
        crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T2", "admin");
        crearTurno.ejecutar(diaAgenda1.getId(), cliente1.getId(), tipo.getId(), inicio, fin, OrigenTurno.PROFESIONAL, false, "T3", "admin");

        assertThat(turnoRepository.count()).isEqualTo(3);

        // Reducir capacidad de 3 a 2
        editarTipoAtencion.ejecutar(profesional1.getId(), tipo.getId(), "CapacidadVariable", null, 30, 2, "admin");

        // Los 3 turnos deben seguir existiendo intactos
        assertThat(turnoRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("11. Inactivación conserva integridad referencial de turnos históricos")
    void test11_inactivacionConservaIntegridadReferencial() {
        TipoAtencion tipo = registrarTipoAtencion.ejecutar(profesional1.getId(), "Histórico", null, 30, 1, "admin");

        Instant inicio = Instant.parse("2026-09-01T08:00:00Z");
        Instant fin = Instant.parse("2026-09-01T08:30:00Z");

        CrearTurno.Resultado r = crearTurno.ejecutar(
                diaAgenda1.getId(), cliente1.getId(), tipo.getId(),
                inicio, fin, OrigenTurno.PROFESIONAL, false, "Turno Histórico", "admin");

        assertThat(r.getTurno().getTipoAtencion().getId()).isEqualTo(tipo.getId());

        // Inactivar tipo
        inactivarTipoAtencion.ejecutar(profesional1.getId(), tipo.getId(), "admin");

        Turno turnoRecuperado = turnoRepository.findById(r.getTurno().getId()).orElseThrow();
        assertThat(turnoRecuperado.getTipoAtencion()).isNotNull();
        assertThat(turnoRecuperado.getTipoAtencion().getId()).isEqualTo(tipo.getId());

        TipoAtencion tipoRecuperado = tipoAtencionRepository.findById(tipo.getId()).orElseThrow();
        assertThat(tipoRecuperado.isActivo()).isFalse();
    }
}
