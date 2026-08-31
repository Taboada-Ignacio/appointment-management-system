package com.apiturnos.turno.service;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.auditoria.repository.AuditoriaEventoRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.notificacion.model.CanalNotificacion;
import com.apiturnos.notificacion.model.EstadoNotificacion;
import com.apiturnos.notificacion.repository.NotificacionRepository;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.turno.model.AdvertenciaTurnoManual;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.repository.TurnoHistorialRepository;
import com.apiturnos.turno.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "turnos.zona-horaria=UTC")
@Testcontainers
class CrearTurnoManualIntegrationTest {

    private static final LocalDate FECHA = LocalDate.of(2030, 9, 10);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private CrearTurnoManual crearTurnoManual;
    @Autowired private GestorCambioEstado gestorCambioEstado;
    @Autowired private ProfesionalRepository profesionalRepository;
    @Autowired private ConfiguracionRepository configuracionRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private AgendaAnualRepository agendaAnualRepository;
    @Autowired private MesAgendaRepository mesAgendaRepository;
    @Autowired private DiaAgendaRepository diaAgendaRepository;
    @Autowired private BrechaHorariaRepository brechaHorariaRepository;
    @Autowired private ExcepcionAgendaRepository excepcionAgendaRepository;
    @Autowired private TipoAtencionRepository tipoAtencionRepository;
    @Autowired private TurnoRepository turnoRepository;
    @Autowired private TurnoHistorialRepository turnoHistorialRepository;
    @Autowired private MotivoBajaTurnoRepository motivoBajaTurnoRepository;
    @Autowired private CambioEstadoRepository cambioEstadoRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private AuditoriaEventoRepository auditoriaEventoRepository;

    private Profesional profesional;
    private Cliente cliente;
    private DiaAgenda dia;
    private TipoAtencion tipo;

    @BeforeEach
    void setUp() {
        notificacionRepository.deleteAll();
        auditoriaEventoRepository.deleteAll();
        cambioEstadoRepository.deleteAll();
        turnoHistorialRepository.deleteAll();
        turnoRepository.deleteAll();
        motivoBajaTurnoRepository.deleteAll();
        excepcionAgendaRepository.deleteAll();
        brechaHorariaRepository.deleteAll();
        tipoAtencionRepository.deleteAll();
        diaAgendaRepository.deleteAll();
        mesAgendaRepository.deleteAll();
        agendaAnualRepository.deleteAll();
        configuracionRepository.deleteAll();
        clienteRepository.deleteAll();
        profesionalRepository.deleteAll();

        profesional = new Profesional();
        profesional.setNombre("Laura");
        profesional.setApellido("Méndez");
        profesional.setEmail("laura.manual@test.com");
        profesional.setTelefono("+5491100000001");
        profesional.setEspecialidad("Clínica");
        profesional = profesionalRepository.save(profesional);

        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesional);
        agenda.setAnio(FECHA.getYear());
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(FECHA.getMonthValue());
        mes = mesAgendaRepository.save(mes);

        dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(FECHA);
        dia = diaAgendaRepository.save(dia);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.DIA_AGENDA, dia.getId(), "ACTIVO", "test", "Día activo");

        BrechaHoraria brecha = new BrechaHoraria();
        brecha.setDiaAgenda(dia);
        brecha.setHoraInicioAtencion(java.time.LocalTime.of(8, 0));
        brecha.setHoraFinAtencion(java.time.LocalTime.of(12, 0));
        brechaHorariaRepository.save(brecha);

        cliente = new Cliente();
        cliente.setProfesional(profesional);
        cliente.setNombre("Ana");
        cliente.setApellido("Pérez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("30111222");
        cliente.setEmail("ana.manual@test.com");
        cliente.setTelefono("+5491112345678");
        cliente.setNotificacionesHabilitadas(true);
        cliente = clienteRepository.save(cliente);
        gestorCambioEstado.registrarCambioInicial(
                AmbitoEstado.CLIENTE, cliente.getId(), "HABILITADO", "test", "Cliente habilitado");

        tipo = new TipoAtencion();
        tipo.setProfesional(profesional);
        tipo.setNombre("Consulta manual");
        tipo.setDescripcion("Consulta de prueba");
        tipo.setDuracionMinutos(30);
        tipo.setCapacidadSimultanea(1);
        tipo.setActivo(true);
        tipo = tipoAtencionRepository.save(tipo);
    }

    @Test
    void creaTurnoYTodosLosEfectosDeDominioEnPostgresql() {
        ResultadoCrearTurnoManual resultado = crearTurnoManual.ejecutar(solicitud(9, 0, 9, 30, false));

        assertThat(resultado.creado()).isTrue();
        assertThat(turnoRepository.count()).isEqualTo(1);
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.TURNO, resultado.turnoId())).isEqualTo("ASIGNADO");
        assertThat(auditoriaEventoRepository.findByProfesionalIdOrderByFechaHoraDesc(profesional.getId()))
                .singleElement()
                .satisfies(evento -> assertThat(evento.getDetalles()).contains("TURNO_CREADO_MANUALMENTE"));
        assertThat(notificacionRepository.findByTurnoId(resultado.turnoId()))
                .singleElement()
                .satisfies(notificacion -> {
                    assertThat(notificacion.getCanal()).isEqualTo(CanalNotificacion.WHATSAPP);
                    assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE);
                });
    }

    @Test
    void fueraDeBrechaNoPersisteHastaQueSeConfirma() {
        ResultadoCrearTurnoManual previo = crearTurnoManual.ejecutar(solicitud(14, 0, 14, 30, false));

        assertThat(previo.requiereConfirmacion()).isTrue();
        assertThat(previo.advertencias())
                .containsExactly(AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA);
        assertThat(turnoRepository.count()).isZero();

        ResultadoCrearTurnoManual confirmado = crearTurnoManual.ejecutar(solicitud(14, 0, 14, 30, true));
        assertThat(confirmado.creado()).isTrue();
        assertThat(turnoRepository.count()).isEqualTo(1);
    }

    @Test
    void permiteMultiplesTurnosSimultaneosTrasConfirmarSobrecapacidad() {
        crearTurnoManual.ejecutar(solicitud(9, 0, 9, 30, false));

        ResultadoCrearTurnoManual previo = crearTurnoManual.ejecutar(solicitud(9, 0, 9, 30, false));
        assertThat(previo.advertencias()).containsExactly(AdvertenciaTurnoManual.CAPACIDAD_SUPERADA);
        assertThat(turnoRepository.count()).isEqualTo(1);

        ResultadoCrearTurnoManual confirmado = crearTurnoManual.ejecutar(solicitud(9, 0, 9, 30, true));
        assertThat(confirmado.creado()).isTrue();
        assertThat(turnoRepository.count()).isEqualTo(2);
    }

    @Test
    void devuelveFueraDeBrechaYSobrecapacidadSimultaneamente() {
        crearTurnoManual.ejecutar(solicitud(14, 0, 14, 30, true));

        ResultadoCrearTurnoManual previo = crearTurnoManual.ejecutar(solicitud(14, 0, 14, 30, false));

        assertThat(previo.advertencias()).containsExactly(
                AdvertenciaTurnoManual.HORARIO_FUERA_DE_BRECHA,
                AdvertenciaTurnoManual.CAPACIDAD_SUPERADA);
        assertThat(turnoRepository.count()).isEqualTo(1);
    }

    @Test
    void clienteRequiereAprobacionQuedaAsignadoPorqueCreaElProfesional() {
        gestorCambioEstado.registrarCambio(
                AmbitoEstado.CLIENTE,
                cliente.getId(),
                "REQUIERE_APROBACION",
                "test",
                "Revisión profesional",
                null);

        ResultadoCrearTurnoManual resultado = crearTurnoManual.ejecutar(solicitud(10, 0, 10, 30, false));

        assertThat(resultado.creado()).isTrue();
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(
                AmbitoEstado.TURNO, resultado.turnoId())).isEqualTo("ASIGNADO");
    }

    @Test
    void telefonoNoHabilitadoNoImpideCrearYNoGeneraNotificacion() {
        cliente.setNotificacionesHabilitadas(false);
        clienteRepository.save(cliente);

        ResultadoCrearTurnoManual resultado = crearTurnoManual.ejecutar(solicitud(11, 0, 11, 30, false));

        assertThat(resultado.creado()).isTrue();
        assertThat(notificacionRepository.findByTurnoId(resultado.turnoId())).isEmpty();
    }

    private SolicitudCrearTurnoManual solicitud(
            int horaInicio, int minutoInicio, int horaFin, int minutoFin, boolean confirmar) {
        return new SolicitudCrearTurnoManual(
                profesional.getId(),
                dia.getId(),
                cliente.getId(),
                tipo.getId(),
                FECHA.atTime(horaInicio, minutoInicio).toInstant(ZoneOffset.UTC),
                FECHA.atTime(horaFin, minutoFin).toInstant(ZoneOffset.UTC),
                confirmar,
                "Creación manual",
                "profesional@test");
    }
}
