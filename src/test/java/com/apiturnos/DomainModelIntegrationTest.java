package com.apiturnos;

import com.apiturnos.agenda.model.AgendaAnual;
import com.apiturnos.agenda.model.BrechaHoraria;
import com.apiturnos.agenda.model.DiaAgenda;
import com.apiturnos.agenda.model.ExcepcionAgenda;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.agenda.model.TipoExcepcion;
import com.apiturnos.agenda.repository.AgendaAnualRepository;
import com.apiturnos.agenda.repository.BrechaHorariaRepository;
import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.repository.ExcepcionAgendaRepository;
import com.apiturnos.agenda.repository.MesAgendaRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.model.Estado;
import com.apiturnos.estado.repository.CambioEstadoRepository;
import com.apiturnos.estado.repository.EstadoRepository;
import com.apiturnos.profesional.model.Configuracion;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.turno.model.MotivoBajaTurno;
import com.apiturnos.turno.repository.MotivoBajaTurnoRepository;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class DomainModelIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private ProfesionalRepository profesionalRepository;

    @Autowired
    private ConfiguracionRepository configuracionRepository;

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
    private ExcepcionAgendaRepository excepcionAgendaRepository;

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private MotivoBajaTurnoRepository motivoBajaTurnoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private CambioEstadoRepository cambioEstadoRepository;

    private Profesional profesionalPrincipal;

    @BeforeEach
    void setUp() {
        Profesional prof = new Profesional();
        prof.setNombre("Mariana");
        prof.setApellido("Lopez");
        prof.setEmail("mariana.lopez." + System.nanoTime() + "@turnos.com");
        prof.setTelefono("+5491133221100");
        prof.setEspecialidad("Dermatología");
        profesionalPrincipal = profesionalRepository.save(prof);
    }

    @Test
    @DisplayName("1. Dos Profesionales distintos pueden tener Clientes con el mismo DNI")
    void test1_DosProfesionalesDistintosPuedenTenerClientesConMismoDni() {
        Profesional otroProfesional = new Profesional();
        otroProfesional.setNombre("Esteban");
        otroProfesional.setApellido("Quito");
        otroProfesional.setEmail("esteban." + System.nanoTime() + "@turnos.com");
        otroProfesional.setTelefono("+5491122334455");
        otroProfesional = profesionalRepository.save(otroProfesional);

        String dniCompartido = "33445566";

        Cliente clienteA = new Cliente();
        clienteA.setProfesional(profesionalPrincipal);
        clienteA.setNombre("Ana");
        clienteA.setApellido("García");
        clienteA.setTipoDocumento(TipoDocumento.DNI);
        clienteA.setNumeroDocumento(dniCompartido);
        clienteA.setEmail("ana.a." + System.nanoTime() + "@test.com");
        clienteA.setTelefono("+5491100000001");
        clienteA = clienteRepository.save(clienteA);

        Cliente clienteB = new Cliente();
        clienteB.setProfesional(otroProfesional);
        clienteB.setNombre("Ana");
        clienteB.setApellido("García");
        clienteB.setTipoDocumento(TipoDocumento.DNI);
        clienteB.setNumeroDocumento(dniCompartido);
        clienteB.setEmail("ana.b." + System.nanoTime() + "@test.com");
        clienteB.setTelefono("+5491100000002");
        clienteB = clienteRepository.save(clienteB);

        assertThat(clienteA.getId()).isNotNull();
        assertThat(clienteB.getId()).isNotNull();
        assertThat(clienteA.getId()).isNotEqualTo(clienteB.getId());
        assertThat(clienteA.getNumeroDocumento()).isEqualTo(clienteB.getNumeroDocumento());
    }

    @Test
    @DisplayName("2. Un mismo Profesional no puede tener dos Clientes con el mismo DNI")
    void test2_MismoProfesionalNoPuedeTenerDosClientesConMismoDni() {
        String dni = "44556677";

        Cliente cliente1 = new Cliente();
        cliente1.setProfesional(profesionalPrincipal);
        cliente1.setNombre("Carlos");
        cliente1.setApellido("Ruiz");
        cliente1.setTipoDocumento(TipoDocumento.DNI);
        cliente1.setNumeroDocumento(dni);
        cliente1.setEmail("carlos.1." + System.nanoTime() + "@test.com");
        cliente1.setTelefono("+5491100000003");
        clienteRepository.save(cliente1);

        Cliente cliente2 = new Cliente();
        cliente2.setProfesional(profesionalPrincipal);
        cliente2.setNombre("Carlos Duplicate");
        cliente2.setApellido("Ruiz");
        cliente2.setTipoDocumento(TipoDocumento.DNI);
        cliente2.setNumeroDocumento(dni);
        cliente2.setEmail("carlos.2." + System.nanoTime() + "@test.com");
        cliente2.setTelefono("+5491100000004");

        assertThatThrownBy(() -> clienteRepository.saveAndFlush(cliente2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("3. Un Profesional no puede tener dos AgendaAnual para el mismo año")
    void test3_ProfesionalNoPuedeTenerDosAgendaAnualParaMismoAnio() {
        AgendaAnual agenda1 = new AgendaAnual();
        agenda1.setProfesional(profesionalPrincipal);
        agenda1.setAnio(2027);
        agendaAnualRepository.save(agenda1);

        AgendaAnual agenda2 = new AgendaAnual();
        agenda2.setProfesional(profesionalPrincipal);
        agenda2.setAnio(2027);

        assertThatThrownBy(() -> agendaAnualRepository.saveAndFlush(agenda2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("4. Un MesAgenda pertenece a una AgendaAnual")
    void test4_MesAgendaPerteneceAAgendaAnual() {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesionalPrincipal);
        agenda.setAnio(2028);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mesAgenda = new MesAgenda();
        mesAgenda.setAgendaAnual(agenda);
        mesAgenda.setNroMes(5);
        mesAgenda.setRepetirConfiguracion(true);
        mesAgenda = mesAgendaRepository.save(mesAgenda);

        assertThat(mesAgenda.getId()).isNotNull();
        assertThat(mesAgenda.getAgendaAnual().getId()).isEqualTo(agenda.getId());
        assertThat(mesAgenda.getNroMes()).isEqualTo(5);
    }

    @Test
    @DisplayName("5. Un DiaAgenda puede tener 0 BrechaHoraria (día inactivo)")
    void test5_DiaAgendaPuedeTenerCeroBrechaHoraria() {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesionalPrincipal);
        agenda.setAnio(2029);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(1);
        mes = mesAgendaRepository.save(mes);

        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(LocalDate.of(2029, 1, 1)); // Feriado / inactivo
        dia = diaAgendaRepository.save(dia);

        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(dia.getId());
        assertThat(brechas).isEmpty();
    }

    @Test
    @DisplayName("6. Un DiaAgenda puede tener múltiples BrechaHoraria")
    void test6_DiaAgendaPuedeTenerMultiplesBrechasHorarias() {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesionalPrincipal);
        agenda.setAnio(2030);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(3);
        mes = mesAgendaRepository.save(mes);

        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(LocalDate.of(2030, 3, 10));
        dia = diaAgendaRepository.save(dia);

        BrechaHoraria manana = new BrechaHoraria();
        manana.setDiaAgenda(dia);
        manana.setHoraInicioAtencion(LocalTime.of(8, 0));
        manana.setHoraFinAtencion(LocalTime.of(12, 0));
        brechaHorariaRepository.save(manana);

        BrechaHoraria tarde = new BrechaHoraria();
        tarde.setDiaAgenda(dia);
        tarde.setHoraInicioAtencion(LocalTime.of(16, 0));
        tarde.setHoraFinAtencion(LocalTime.of(20, 0));
        brechaHorariaRepository.save(tarde);

        List<BrechaHoraria> brechas = brechaHorariaRepository.findByDiaAgendaId(dia.getId());
        assertThat(brechas).hasSize(2);
    }

    @Test
    @DisplayName("7. Un DiaAgenda puede tener múltiples Turnos")
    void test7_DiaAgendaPuedeTenerMultiplesTurnos() {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesionalPrincipal);
        agenda.setAnio(2031);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(4);
        mes = mesAgendaRepository.save(mes);

        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(LocalDate.of(2031, 4, 15));
        dia = diaAgendaRepository.save(dia);

        Cliente c1 = createCliente("Lucia", "55667788");
        Cliente c2 = createCliente("Pedro", "66778899");

        Instant base = Instant.now().plus(10, ChronoUnit.DAYS);

        Turno t1 = new Turno();
        t1.setDiaAgenda(dia);
        t1.setCliente(c1);
        t1.setInicioEstimado(base);
        t1.setFinEstimado(base.plus(30, ChronoUnit.MINUTES));
        t1.setOrigen(OrigenTurno.PROFESIONAL);
        turnoRepository.save(t1);

        Turno t2 = new Turno();
        t2.setDiaAgenda(dia);
        t2.setCliente(c2);
        t2.setInicioEstimado(base.plus(30, ChronoUnit.MINUTES));
        t2.setFinEstimado(base.plus(60, ChronoUnit.MINUTES));
        t2.setOrigen(OrigenTurno.CLIENTE_AUTOGESTION);
        turnoRepository.save(t2);

        List<Turno> turnos = turnoRepository.findByDiaAgendaId(dia.getId());
        assertThat(turnos).hasSize(2);
    }

    @Test
    @DisplayName("8. Un Turno pertenece obligatoriamente a un Cliente")
    void test8_TurnoPerteneceObligatoriamenteAUnCliente() {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesionalPrincipal);
        agenda.setAnio(2032);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(6);
        mes = mesAgendaRepository.save(mes);

        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(LocalDate.of(2032, 6, 20));
        dia = diaAgendaRepository.save(dia);

        Turno turnoSinCliente = new Turno();
        turnoSinCliente.setDiaAgenda(dia);
        turnoSinCliente.setCliente(null); // Sin cliente
        turnoSinCliente.setInicioEstimado(Instant.now().plus(5, ChronoUnit.DAYS));
        turnoSinCliente.setFinEstimado(Instant.now().plus(5, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES));
        turnoSinCliente.setOrigen(OrigenTurno.PROFESIONAL);

        assertThatThrownBy(() -> turnoRepository.saveAndFlush(turnoSinCliente))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("9. Varios Turnos pueden superponerse (capacidad simultánea)")
    void test9_VariosTurnosPuedenSuperponerse() {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesionalPrincipal);
        agenda.setAnio(2033);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(7);
        mes = mesAgendaRepository.save(mes);

        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(LocalDate.of(2033, 7, 5));
        dia = diaAgendaRepository.save(dia);

        Cliente c1 = createCliente("Tomas", "77889900");
        Cliente c2 = createCliente("Valeria", "88990011");

        Instant mismoHorarioInicio = Instant.now().plus(15, ChronoUnit.DAYS);
        Instant mismoHorarioFin = mismoHorarioInicio.plus(30, ChronoUnit.MINUTES);

        Turno t1 = new Turno();
        t1.setDiaAgenda(dia);
        t1.setCliente(c1);
        t1.setInicioEstimado(mismoHorarioInicio);
        t1.setFinEstimado(mismoHorarioFin);
        t1.setOrigen(OrigenTurno.PROFESIONAL);
        turnoRepository.save(t1);

        Turno t2 = new Turno();
        t2.setDiaAgenda(dia);
        t2.setCliente(c2);
        t2.setInicioEstimado(mismoHorarioInicio); // Mismo horario exacto
        t2.setFinEstimado(mismoHorarioFin);
        t2.setOrigen(OrigenTurno.PROFESIONAL);
        turnoRepository.save(t2);

        List<Turno> turnos = turnoRepository.findByDiaAgendaId(dia.getId());
        assertThat(turnos).hasSize(2);
        assertThat(turnos.get(0).getInicioEstimado()).isEqualTo(turnos.get(1).getInicioEstimado());
    }

    @Test
    @DisplayName("10. Un Turno puede existir aunque su horario no coincida con ninguna BrechaHoraria")
    void test10_TurnoPuedeExistirFueraDeBrechaHoraria() {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesionalPrincipal);
        agenda.setAnio(2034);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(8);
        mes = mesAgendaRepository.save(mes);

        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(LocalDate.of(2034, 8, 12));
        dia = diaAgendaRepository.save(dia);

        // Brecha configurada de 08:00 a 12:00
        BrechaHoraria brecha = new BrechaHoraria();
        brecha.setDiaAgenda(dia);
        brecha.setHoraInicioAtencion(LocalTime.of(8, 0));
        brecha.setHoraFinAtencion(LocalTime.of(12, 0));
        brechaHorariaRepository.save(brecha);

        // Turno creado manualmente a las 14:30 (fuera de la brecha)
        Cliente cliente = createCliente("Sofia", "99001122");
        Instant turnoInicio = Instant.parse("2034-08-12T14:30:00Z");
        Instant turnoFin = Instant.parse("2034-08-12T15:00:00Z");

        Turno turno = new Turno();
        turno.setDiaAgenda(dia);
        turno.setCliente(cliente);
        turno.setInicioEstimado(turnoInicio);
        turno.setFinEstimado(turnoFin);
        turno.setOrigen(OrigenTurno.PROFESIONAL);
        turno = turnoRepository.save(turno);

        assertThat(turno.getId()).isNotNull();
        assertThat(turno.getInicioEstimado()).isEqualTo(turnoInicio);
    }

    @Test
    @DisplayName("11. CambioEstado permite reconstruir el estado actual tomando el último cambio")
    void test11_CambioEstadoPermiteReconstruirEstadoActual() {
        Cliente cliente = createCliente("Martin", "11223344");

        Estado habilitado = estadoRepository.findByNombreAndAmbito("HABILITADO", AmbitoEstado.CLIENTE).orElseThrow();
        Estado inhabilitado = estadoRepository.findByNombreAndAmbito("INHABILITADO", AmbitoEstado.CLIENTE).orElseThrow();

        Instant t0 = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant t1 = Instant.now().minus(1, ChronoUnit.HOURS);

        CambioEstado c1 = new CambioEstado();
        c1.setEstado(habilitado);
        c1.setAmbito(AmbitoEstado.CLIENTE);
        c1.setEntidadId(cliente.getId());
        c1.setFechaHoraInicio(t0);
        c1.setFechaHoraFin(t1);
        cambioEstadoRepository.save(c1);

        CambioEstado c2 = new CambioEstado();
        c2.setEstado(inhabilitado);
        c2.setAmbito(AmbitoEstado.CLIENTE);
        c2.setEntidadId(cliente.getId());
        c2.setFechaHoraInicio(t1);
        cambioEstadoRepository.save(c2);

        Optional<CambioEstado> ultimoCambio = cambioEstadoRepository
                .findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDesc(AmbitoEstado.CLIENTE, cliente.getId());

        assertThat(ultimoCambio).isPresent();
        assertThat(ultimoCambio.get().getEstado().getNombre()).isEqualTo("INHABILITADO");
    }

    @Test
    @DisplayName("12. CambioEstado conserva historial completo (ej. reprogramación ASIGNADO -> REPROGRAMADO -> ASIGNADO)")
    void test12_CambioEstadoConservaHistorialReprogramacion() {
        AgendaAnual agenda = new AgendaAnual();
        agenda.setProfesional(profesionalPrincipal);
        agenda.setAnio(2035);
        agenda = agendaAnualRepository.save(agenda);

        MesAgenda mes = new MesAgenda();
        mes.setAgendaAnual(agenda);
        mes.setNroMes(9);
        mes = mesAgendaRepository.save(mes);

        DiaAgenda dia = new DiaAgenda();
        dia.setMesAgenda(mes);
        dia.setFecha(LocalDate.of(2035, 9, 20));
        dia = diaAgendaRepository.save(dia);

        Cliente cliente = createCliente("Gonzalo", "22334455");

        Turno turno = new Turno();
        turno.setDiaAgenda(dia);
        turno.setCliente(cliente);
        turno.setInicioEstimado(Instant.now().plus(20, ChronoUnit.DAYS));
        turno.setFinEstimado(Instant.now().plus(20, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES));
        turno.setOrigen(OrigenTurno.PROFESIONAL);
        turno = turnoRepository.save(turno);

        Estado asignado = estadoRepository.findByNombreAndAmbito("ASIGNADO", AmbitoEstado.TURNO).orElseThrow();
        Estado reprogramado = estadoRepository.findByNombreAndAmbito("REPROGRAMADO", AmbitoEstado.TURNO).orElseThrow();

        Instant t0 = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant t1 = Instant.now().minus(15, ChronoUnit.MINUTES);
        Instant t2 = Instant.now();

        // 1. ASIGNADO
        CambioEstado ce1 = new CambioEstado();
        ce1.setEstado(asignado);
        ce1.setAmbito(AmbitoEstado.TURNO);
        ce1.setEntidadId(turno.getId());
        ce1.setFechaHoraInicio(t0);
        ce1.setFechaHoraFin(t1);
        cambioEstadoRepository.save(ce1);

        // 2. REPROGRAMADO (evento)
        CambioEstado ce2 = new CambioEstado();
        ce2.setEstado(reprogramado);
        ce2.setAmbito(AmbitoEstado.TURNO);
        ce2.setEntidadId(turno.getId());
        ce2.setFechaHoraInicio(t1);
        ce2.setFechaHoraFin(t2);
        cambioEstadoRepository.save(ce2);

        // 3. ASIGNADO (operativo nuevamente)
        CambioEstado ce3 = new CambioEstado();
        ce3.setEstado(asignado);
        ce3.setAmbito(AmbitoEstado.TURNO);
        ce3.setEntidadId(turno.getId());
        ce3.setFechaHoraInicio(t2);
        cambioEstadoRepository.save(ce3);

        List<CambioEstado> historial = cambioEstadoRepository
                .findByAmbitoAndEntidadIdOrderByFechaHoraInicioAsc(AmbitoEstado.TURNO, turno.getId());

        assertThat(historial).hasSize(3);
        assertThat(historial.get(0).getEstado().getNombre()).isEqualTo("ASIGNADO");
        assertThat(historial.get(1).getEstado().getNombre()).isEqualTo("REPROGRAMADO");
        assertThat(historial.get(2).getEstado().getNombre()).isEqualTo("ASIGNADO");

        // El estado actual es el último (ASIGNADO)
        Optional<CambioEstado> actual = cambioEstadoRepository
                .findFirstByAmbitoAndEntidadIdOrderByFechaHoraInicioDesc(AmbitoEstado.TURNO, turno.getId());
        assertThat(actual).isPresent();
        assertThat(actual.get().getEstado().getNombre()).isEqualTo("ASIGNADO");
    }

    @Test
    @DisplayName("13. MotivoBajaTurno es opcional en cambios que no requieren motivo")
    void test13_MotivoBajaTurnoEsOpcional() {
        Cliente cliente = createCliente("Camila", "33445577");

        Estado habilitado = estadoRepository.findByNombreAndAmbito("HABILITADO", AmbitoEstado.CLIENTE).orElseThrow();
        Estado inhabilitado = estadoRepository.findByNombreAndAmbito("INHABILITADO", AmbitoEstado.CLIENTE).orElseThrow();

        // Cambio SIN motivo
        CambioEstado sinMotivo = new CambioEstado();
        sinMotivo.setEstado(habilitado);
        sinMotivo.setAmbito(AmbitoEstado.CLIENTE);
        sinMotivo.setEntidadId(cliente.getId());
        sinMotivo.setFechaHoraInicio(Instant.now().minus(1, ChronoUnit.HOURS));
        sinMotivo.setMotivoBajaTurno(null);
        sinMotivo = cambioEstadoRepository.save(sinMotivo);
        assertThat(sinMotivo.getMotivoBajaTurno()).isNull();

        // Cambio CON motivo
        MotivoBajaTurno motivo = new MotivoBajaTurno();
        motivo.setMotivo("Inhabilitación por reiteradas inasistencias");
        motivo = motivoBajaTurnoRepository.save(motivo);

        CambioEstado conMotivo = new CambioEstado();
        conMotivo.setEstado(inhabilitado);
        conMotivo.setAmbito(AmbitoEstado.CLIENTE);
        conMotivo.setEntidadId(cliente.getId());
        conMotivo.setFechaHoraInicio(Instant.now());
        conMotivo.setMotivoBajaTurno(motivo);
        conMotivo = cambioEstadoRepository.save(conMotivo);

        assertThat(conMotivo.getMotivoBajaTurno()).isNotNull();
        assertThat(conMotivo.getMotivoBajaTurno().getMotivo()).contains("inasistencias");
    }

    @Test
    @DisplayName("14. Una ExcepcionAgenda puede cubrir uno o varios días")
    void test14_ExcepcionAgendaPuedeCubrirUnoO_VariosDias() {
        // Excepción de 1 día (fechaInicio == fechaFin)
        ExcepcionAgenda unDia = new ExcepcionAgenda();
        unDia.setProfesional(profesionalPrincipal);
        unDia.setTipo(TipoExcepcion.FERIADO);
        unDia.setFechaInicio(LocalDate.of(2026, 12, 25));
        unDia.setFechaFin(LocalDate.of(2026, 12, 25));
        unDia.setMotivo("Navidad");
        unDia = excepcionAgendaRepository.save(unDia);
        assertThat(unDia.getId()).isNotNull();

        // Excepción de varios días (fechaInicio < fechaFin)
        ExcepcionAgenda variosDias = new ExcepcionAgenda();
        variosDias.setProfesional(profesionalPrincipal);
        variosDias.setTipo(TipoExcepcion.VACACIONES);
        variosDias.setFechaInicio(LocalDate.of(2027, 1, 15));
        variosDias.setFechaFin(LocalDate.of(2027, 1, 31));
        variosDias.setMotivo("Vacaciones de verano");
        variosDias = excepcionAgendaRepository.save(variosDias);
        assertThat(variosDias.getId()).isNotNull();

        List<ExcepcionAgenda> excepciones = excepcionAgendaRepository.findByProfesionalId(profesionalPrincipal.getId());
        assertThat(excepciones).hasSize(2);
    }

    @Test
    @DisplayName("15. Las restricciones Flyway y las entidades JPA son consistentes")
    void test15_RestriccionesFlywayYEntidadesJpaSonConsistentes() {
        // Verifica que la configuración 1:1 con profesional no permita duplicados
        Configuracion config1 = new Configuracion();
        config1.setProfesional(profesionalPrincipal);
        config1.setCantidadMaxTurnosALaVez(1);
        config1.setDuracionAproximadaPorTurno(45);
        configuracionRepository.save(config1);

        Configuracion configDuplicada = new Configuracion();
        configDuplicada.setProfesional(profesionalPrincipal); // Mismo profesional
        configDuplicada.setCantidadMaxTurnosALaVez(2);
        configDuplicada.setDuracionAproximadaPorTurno(30);

        assertThatThrownBy(() -> configuracionRepository.saveAndFlush(configDuplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Cliente createCliente(String nombre, String dni) {
        Cliente c = new Cliente();
        c.setProfesional(profesionalPrincipal);
        c.setNombre(nombre);
        c.setApellido("Test");
        c.setTipoDocumento(TipoDocumento.DNI);
        c.setNumeroDocumento(dni);
        c.setEmail(nombre.toLowerCase() + "." + System.nanoTime() + "@test.com");
        c.setTelefono("+54911" + dni);
        return clienteRepository.save(c);
    }
}
