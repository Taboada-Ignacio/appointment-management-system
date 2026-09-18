package com.apiturnos.autogestion;

import com.apiturnos.agenda.model.*;
import com.apiturnos.agenda.repository.*;
import com.apiturnos.atencion.model.TipoAtencion;
import com.apiturnos.atencion.repository.TipoAtencionRepository;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.cliente.service.RegistrarCliente;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.*;
import com.apiturnos.profesional.repository.*;
import com.apiturnos.turno.repository.TurnoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.apiturnos.autogestion.AutogestionDtos.*;

@SpringBootTest(properties="turnos.autogestion.clave-enlaces=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Testcontainers
class AutogestionIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
    @Autowired AutogestionService service;
    @Autowired ProfesionalRepository profesionales;
    @Autowired ClienteRepository clientes;
    @Autowired RegistrarCliente registrar;
    @Autowired GestorCambioEstado estados;
    @Autowired AgendaAnualRepository agendas;
    @Autowired MesAgendaRepository meses;
    @Autowired DiaAgendaRepository dias;
    @Autowired BrechaHorariaRepository brechas;
    @Autowired TipoAtencionRepository tipos;
    @Autowired TurnoRepository turnos;
    @Autowired com.apiturnos.turno.repository.MotivoBajaTurnoRepository motivos;
    @Autowired com.apiturnos.turno.service.AprobarTurno aprobar;
    @Autowired ConfiguracionRepository configs;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;
    @Autowired com.apiturnos.turno.service.CrearTurnoManual crearManual;
    @Autowired com.apiturnos.turno.service.CrearTurno crearDirecto;
    @Autowired LimitarAutogestion limiter;
    @Autowired AutogestionRateFilter rateFilter;
    @Autowired WebApplicationContext context;
    Profesional p;
    Cliente c;
    TipoAtencion tipo;
    DiaAgenda dia;
    LocalDate fecha;
    String link;
    MockMvc mvc;

    @BeforeEach void setup() {
        p=professional();
        c=registrar.ejecutar(p.getId(),"Ana","Pérez",TipoDocumento.DNI,"30111222","ana@test.com","1122334455",false,"test");
        fecha=LocalDate.now(clock).plusDays(1);
        AgendaAnual a=new AgendaAnual(); a.setProfesional(p); a.setAnio(fecha.getYear()); a=agendas.save(a);
        MesAgenda m=new MesAgenda(); m.setAgendaAnual(a); m.setNroMes(fecha.getMonthValue()); m=meses.save(m);
        estados.registrarCambioInicial(AmbitoEstado.MES_AGENDA,m.getId(),"ACTIVO","test","");
        dia=new DiaAgenda(); dia.setMesAgenda(m); dia.setFecha(fecha); dia=dias.save(dia);
        estados.registrarCambioInicial(AmbitoEstado.DIA_AGENDA,dia.getId(),"ACTIVO","test","");
        BrechaHoraria b=new BrechaHoraria(); b.setDiaAgenda(dia); b.setHoraInicioAtencion(LocalTime.of(8,0));
        b.setHoraFinAtencion(LocalTime.of(10,0)); brechas.save(b);
        tipo=type(p);
        Configuracion config=new Configuracion(); config.setProfesional(p); config.setPermitirMultiplesTurnosPorClienteEnDia(true); config=configs.save(config);
        link=service.regenerar(p.getId()).token();
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(rateFilter).build();
    }
    Profesional professional() {
        Profesional p=new Profesional(); p.setNombre("Doctor"); p.setApellido("Test");
        p.setEmail(UUID.randomUUID()+"@test.com"); p.setTelefono("1122334455"); return profesionales.save(p);
    }
    TipoAtencion type(Profesional owner) {
        TipoAtencion t=new TipoAtencion(); t.setProfesional(owner); t.setNombre("Consulta");
        t.setDuracionMinutos(30); t.setCapacidadSimultanea(1); return tipos.save(t);
    }
    String session() {
        String s=service.abrir(link).token(); service.identificar(s,new Identificar("30111222")); return s;
    }
    Credencial reference(String s) { return service.elegir(s,new Elegir(fecha,LocalTime.of(8,0))); }
    void expectStatus(int expected,Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(AutogestionException.class,e -> assertThat(e.getStatus()).isEqualTo(expected));
    }

    @Test void diasDelMesSoloIncluyenActivosDelProfesionalInclusoSinIntervalos() throws Exception {
        String s=session(); YearMonth mes=YearMonth.from(fecha);
        assertThat(service.diasMes(s,mes)).extracting(DiaPublico::fecha).containsExactly(fecha);
        estados.registrarCambio(AmbitoEstado.DIA_AGENDA,dia.getId(),"INACTIVO","test","",null);
        assertThat(service.diasMes(s,mes)).isEmpty();
        estados.registrarCambio(AmbitoEstado.DIA_AGENDA,dia.getId(),"ACTIVO","test","",null);
        brechas.deleteAll(brechas.findByDiaAgendaId(dia.getId()));
        assertThat(service.diasMes(s,mes)).extracting(DiaPublico::fecha).containsExactly(fecha);
        assertThat(service.brechas(s,fecha)).isEmpty();
        expectStatus(409,()->service.diasMes(s,mes.plusMonths(1)));
        expectStatus(409,()->service.diasMes(s,YearMonth.now(clock).minusMonths(1)));
        mvc.perform(get("/api/autogestion/dias").header("Authorization","Bearer "+s).param("mes",mes.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].estadoActual").value("ACTIVO"))
            .andExpect(jsonPath("$[0].fecha").value(fecha.toString()));
        mvc.perform(get("/api/autogestion/dias").header("Authorization","Bearer "+s).param("mes","invalid"))
            .andExpect(status().isBadRequest());
    }

    @Test void mesesInactivosNoSePuedenConsultarNiConfirmar() {
        String s=session(); var ref=reference(s);
        assertThat(service.mesesActivos(s)).containsExactly(YearMonth.from(fecha));
        estados.registrarCambio(AmbitoEstado.MES_AGENDA,dia.getMesAgenda().getId(),"INACTIVO","test","",null);
        assertThat(service.mesesActivos(s)).isEmpty();
        assertThat(service.diasMes(s,null)).isEmpty();
        expectStatus(409,()->service.diasMes(s,YearMonth.from(fecha)));
        expectStatus(409,()->reference(s));
        expectStatus(409,()->service.confirmar(s,"closed-month",new Confirmar(ref.token(),null)));
    }

    @Test void unTurnoPorClienteBloqueaAmbosOrigenesYPermiteReReservarTrasCancelar() throws Exception {
        var config=configs.findByProfesionalId(p.getId()).orElseThrow();
        config.setPermitirMultiplesTurnosPorClienteEnDia(false); config=configs.save(config);
        mvc.perform(get("/api/profesionales/"+p.getId()+"/configuracion"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.permitirMultiplesTurnosPorClienteEnDia").value(false));
        String s=session();
        var primero=service.confirmar(s,"first",new Confirmar(reference(s).token(),null));
        var ref=service.elegir(s,new Elegir(fecha,LocalTime.of(8,30)));
        Instant inicio=fecha.atTime(8,30).atZone(clock.getZone()).toInstant();
        var manual=new com.apiturnos.turno.service.SolicitudCrearTurnoManual(p.getId(),dia.getId(),c.getId(),null,inicio,inicio.plusSeconds(1800),true,null,"test");
        assertThatThrownBy(() -> service.confirmar(s,"second",new Confirmar(ref.token(),null)))
            .isInstanceOf(com.apiturnos.shared.exception.NegocioException.class).hasMessageContaining("ya tiene un turno");
        assertThatThrownBy(() -> crearManual.ejecutar(manual))
            .isInstanceOf(com.apiturnos.shared.exception.NegocioException.class).hasMessageContaining("ya tiene un turno");
        assertThatThrownBy(() -> crearDirecto.ejecutar(dia.getId(),c.getId(),tipo.getId(),inicio,inicio.plusSeconds(1800),com.apiturnos.turno.model.OrigenTurno.PROFESIONAL,false,null,"test"))
            .isInstanceOf(com.apiturnos.shared.exception.NegocioException.class).hasMessageContaining("ya tiene un turno");
        assertThat(turnos.findByDiaAgendaId(dia.getId())).hasSize(1);
        var motivo=new com.apiturnos.turno.model.MotivoBajaTurno(); motivo.setMotivo("Cancelación de prueba"); motivo=motivos.save(motivo);
        estados.registrarCambio(AmbitoEstado.TURNO,primero.turnoId(),"CANCELADO","test","",motivo);
        service.confirmar(s,"second",new Confirmar(ref.token(),null));
        assertThat(turnos.findByDiaAgendaId(dia.getId())).hasSize(2);
        mvc.perform(put("/api/profesionales/"+p.getId()+"/configuracion").contentType("application/json")
                .content("{\"permitirMultiplesTurnosPorClienteEnDia\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.permitirMultiplesTurnosPorClienteEnDia").value(true));
        var siguiente=service.elegir(s,new Elegir(fecha,LocalTime.of(9,0)));
        service.confirmar(s,"third",new Confirmar(siguiente.token(),null));
        assertThat(turnos.findByDiaAgendaId(dia.getId())).hasSize(3);
    }

    @Test void flujoCompletoEIdempotencia() {
        String s=session();
        assertThat(service.fechas(s,fecha,fecha.plusDays(7))).containsExactly(fecha);
        assertThat(service.brechas(s,fecha).getFirst().intervalos()).hasSize(4);
        var ref=reference(s);
        var req=new Confirmar(ref.token(),"consulta");
        Resultado r=service.confirmar(s,"reserva-1",req);
        assertThat(r.estado()).isEqualTo("ASIGNADO");
        assertThat(service.confirmar(s,"reserva-1",req)).isEqualTo(r);
        assertThat(service.estadoSesion(s).claveResultado()).isEqualTo("reserva-1");
        assertThat(turnos.findByDiaAgendaId(dia.getId())).hasSize(1);
        assertThat(service.brechas(s,fecha).getFirst().intervalos()).hasSize(3);
        expectStatus(409,()->service.confirmar(s,"reserva-1",new Confirmar(ref.token(),"otro cuerpo")));
        expectStatus(409,()->service.confirmar(s,"otra-clave",req));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM autogestion_enlace WHERE huella=?",Integer.class,link)).isZero();
    }
    @Test void accionesClientesPendientesSeleccionMultipleYAtomicidad() throws Exception {
        c=registrar.ejecutar(p.getId(),"Pendiente","Cliente",TipoDocumento.DNI,"80999333","pendiente@test.com","1122334455",true,"test");
        var segundo=registrar.ejecutar(p.getId(),"Nuevo","Cliente",TipoDocumento.DNI,"80999222","nuevo@test.com","1122334455",true,"test");
        String base="/api/profesionales/"+p.getId()+"/clientes/pendientes-verificacion";
        mvc.perform(get(base+"/ids")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post(base+"/estado").contentType("application/json")
            .content("{\"clienteIds\":["+c.getId()+",999999999],\"estado\":\"HABILITADO\"}"))
            .andExpect(status().isBadRequest());
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,c.getId())).isEqualTo("PENDIENTE_DE_VERIFICACION");
        mvc.perform(post(base+"/estado").contentType("application/json")
            .content("{\"clienteIds\":["+c.getId()+","+segundo.getId()+"],\"estado\":\"REQUIERE_APROBACION\"}"))
            .andExpect(status().isNoContent());
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,c.getId())).isEqualTo("REQUIERE_APROBACION");
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,segundo.getId())).isEqualTo("REQUIERE_APROBACION");
        mvc.perform(get(base+"/ids")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void cambiarEstadoClienteValidaProfesionalYEstado() throws Exception {
        String endpoint="/api/profesionales/"+p.getId()+"/clientes/"+c.getId()+"/estado";
        mvc.perform(put(endpoint).contentType("application/json").content("{\"estado\":\"INHABILITADO\"}"))
            .andExpect(status().isNoContent());
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,c.getId())).isEqualTo("INHABILITADO");
        mvc.perform(put(endpoint).contentType("application/json").content("{\"estado\":\"INVALIDO\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/profesionales/"+professional().getId()+"/clientes/"+c.getId()+"/estado")
            .contentType("application/json").content("{\"estado\":\"HABILITADO\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put(endpoint).contentType("application/json").content("{\"estado\":\"HABILITADO\"}"))
            .andExpect(status().isNoContent());
    }

    @Test void configuracionObligaVerificacionClienteHabilitado() throws Exception {
        String endpoint="/api/profesionales/"+p.getId()+"/configuracion";
        mvc.perform(put(endpoint).contentType("application/json")
                .content("{\"todosLosTurnosPendientesVerificacion\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.todosLosTurnosPendientesVerificacion").value(true));
        String s=session();
        var primero=service.confirmar(s,"forzada",new Confirmar(reference(s).token(),null));
        assertThat(primero.estado()).isEqualTo("PENDIENTE_DE_APROBACION");
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,c.getId())).isEqualTo("HABILITADO");
        aprobar.ejecutar(primero.turnoId(),"test");
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO,primero.turnoId())).isEqualTo("ASIGNADO");
        mvc.perform(put(endpoint).contentType("application/json")
                .content("{\"todosLosTurnosPendientesVerificacion\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.todosLosTurnosPendientesVerificacion").value(false));
        var ref=service.elegir(s,new Elegir(fecha,LocalTime.of(8,30)));
        assertThat(service.confirmar(s,"normal",new Confirmar(ref.token(),null)).estado()).isEqualTo("ASIGNADO");
    }

    @Test void panelPendientesPaginaPorProfesionalYClientesFiltraEstadoActual() throws Exception {
        estados.registrarCambio(AmbitoEstado.CLIENTE,c.getId(),"REQUIERE_APROBACION","test","",null);
        String s=session();
        var primero=service.confirmar(s,"panel-first",new Confirmar(reference(s).token(),null));
        var ref=service.elegir(s,new Elegir(fecha,LocalTime.of(8,30)));
        service.confirmar(s,"panel-second",new Confirmar(ref.token(),null));
        String endpoint="/api/profesionales/"+p.getId()+"/turnos/pendientes-verificacion";
        mvc.perform(get(endpoint).param("size","1").param("page","0"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].id").value(primero.turnoId()))
            .andExpect(jsonPath("$.content[0].cliente.nombre").value("Ana"));
        mvc.perform(get("/api/profesionales/"+professional().getId()+"/turnos/pendientes-verificacion"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        aprobar.ejecutar(primero.turnoId(),"test");
        mvc.perform(get(endpoint)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        var nuevo=registrar.ejecutar(p.getId(),"Nuevo","Cliente",TipoDocumento.DNI,"80111222","nuevo@test.com","1122334455",true,"test");
        mvc.perform(get("/api/profesionales/"+p.getId()+"/clientes").param("estado","PENDIENTE_DE_VERIFICACION"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(nuevo.getId()));
    }
    @Test void accionesPendientesSeleccionMultipleBajaYAislamiento() throws Exception {
        estados.registrarCambio(AmbitoEstado.CLIENTE,c.getId(),"REQUIERE_APROBACION","test","",null);
        String s=session();
        var primero=service.confirmar(s,"bulk-first",new Confirmar(reference(s).token(),null));
        var ref=service.elegir(s,new Elegir(fecha,LocalTime.of(8,30)));
        var segundo=service.confirmar(s,"bulk-second",new Confirmar(ref.token(),null));
        String base="/api/profesionales/"+p.getId()+"/turnos/pendientes-verificacion";
        String ids="{\"turnoIds\":["+primero.turnoId()+","+segundo.turnoId()+"]}";
        mvc.perform(get(base+"/ids")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post("/api/profesionales/"+professional().getId()+"/turnos/pendientes-verificacion/verificar")
            .contentType("application/json").content(ids)).andExpect(status().isForbidden());
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO,primero.turnoId())).isEqualTo("PENDIENTE_DE_APROBACION");
        mvc.perform(post(base+"/baja").contentType("application/json").content(ids))
            .andExpect(status().isBadRequest());
        mvc.perform(post(base+"/verificar").contentType("application/json").content(ids))
            .andExpect(status().isOk());
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO,segundo.turnoId())).isEqualTo("ASIGNADO");
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,c.getId())).isEqualTo("REQUIERE_APROBACION");
        mvc.perform(post(base+"/verificar").contentType("application/json").content(ids))
            .andExpect(status().isBadRequest());
        var otroRef=service.elegir(s,new Elegir(fecha,LocalTime.of(9,0)));
        var tercero=service.confirmar(s,"bulk-third",new Confirmar(otroRef.token(),null));
        mvc.perform(post(base+"/baja").contentType("application/json")
            .content("{\"turnoIds\":["+tercero.turnoId()+"],\"motivo\":\"Solicitud del cliente\"}"))
            .andExpect(status().isOk());
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO,tercero.turnoId())).isEqualTo("DADO_DE_BAJA");
    }
    @Test void reservaSinTipoUsaDuracionYCapacidadGeneralYRevalidaCambios() throws Exception {
        tipos.delete(tipo);
        var config=configs.findByProfesionalId(p.getId()).orElseThrow();
        config.setDuracionAproximadaPorTurno(15); config.setCantidadMaxTurnosALaVez(2); config=configs.save(config);
        String s=session();
        assertThat(service.brechas(s,fecha).getFirst().intervalos()).hasSize(8);
        mvc.perform(get("/api/autogestion/brechas").header("Authorization","Bearer "+s).param("fecha",fecha.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].intervalos[0].fin").value("08:15:00"));
        var ref=reference(s);
        var r=service.confirmar(s,"general",new Confirmar(ref.token(),null));
        assertThat(Duration.between(r.inicio(),r.fin()).toMinutes()).isEqualTo(15);
        assertThat(turnos.findById(r.turnoId()).orElseThrow().getTipoAtencion()).isNull();
        assertThat(service.brechas(s,fecha).getFirst().intervalos()).hasSize(8);
        var segundo=reference(s);
        config.setCantidadMaxTurnosALaVez(1); config=configs.save(config);
        assertThat(service.brechas(s,fecha).getFirst().intervalos()).hasSize(7);
        expectStatus(409,()->service.confirmar(s,"capacity-change",new Confirmar(segundo.token(),null)));
        var libre=service.elegir(s,new Elegir(fecha,LocalTime.of(8,15)));
        config.setDuracionAproximadaPorTurno(30); config=configs.save(config);
        expectStatus(409,()->service.confirmar(s,"duration-change",new Confirmar(libre.token(),null)));
    }
    @Test void pendienteConsumeCapacidadGeneralSinMostrarseEnElCronograma() {
        estados.registrarCambio(AmbitoEstado.CLIENTE,c.getId(),"REQUIERE_APROBACION","test","",null);
        String s=session();
        var r=service.confirmar(s,"pending-capacity",new Confirmar(reference(s).token(),null));
        assertThat(r.estado()).isEqualTo("PENDIENTE_DE_APROBACION");
        assertThat(service.brechas(s,fecha).getFirst().intervalos()).hasSize(3);
    }
    @Test void requiereAprobacionDevuelveEstadoReal() {
        estados.registrarCambio(AmbitoEstado.CLIENTE,c.getId(),"REQUIERE_APROBACION","test","",null);
        String s=session();
        var r=service.confirmar(s,"pending",new Confirmar(reference(s).token(),null));
        assertThat(r.estado()).isEqualTo("PENDIENTE_DE_APROBACION");
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO,r.turnoId())).isEqualTo(r.estado());
    }
    @Test void nuevoQuedaPendienteYPuedeSolicitarReserva() {
        String s=service.abrir(link).token();
        assertThat(service.identificar(s,new Identificar("40111222")).estado()).isEqualTo("NUEVO");
        var r=service.registrar(s,new Nuevo("Nuevo","Cliente","nuevo@test.com","1199887766"));
        assertThat(r.estado()).isEqualTo("PENDIENTE_DE_VERIFICACION"); assertThat(r.puedeReservar()).isTrue();
        var reserva=service.confirmar(s,"nuevo",new Confirmar(reference(s).token(),null));
        assertThat(reserva.estado()).isEqualTo("PENDIENTE_DE_APROBACION");
        var nuevo=clientes.findByProfesionalIdAndNumeroDocumento(p.getId(),"40111222").orElseThrow();
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,nuevo.getId())).isEqualTo("PENDIENTE_DE_VERIFICACION");
        aprobar.ejecutar(reserva.turnoId(),"profesional");
        assertThat(service.misTurnos(s).getFirst().estado()).isEqualTo("ASIGNADO");
        assertThat(estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,nuevo.getId())).isEqualTo("PENDIENTE_DE_VERIFICACION");
    }
    @Test void contactoNoCambiaIdentidad() {
        String s=session();
        var r=service.contacto(s,new Contacto("actualizado@test.com","1199887766"));
        assertThat(r.nombre()).isEqualTo(c.getNombre());
        assertThat(clientes.findById(c.getId()).orElseThrow().getNumeroDocumento()).isEqualTo("30111222");
        expectStatus(409,()->service.identificar(s,new Identificar("40111222")));
    }
    @Test void identificaSoloPorDniYLimitaLosIntentos() {
        String s=service.abrir(link).token();
        assertThat(service.identificar(s,new Identificar("30111222")).nombre()).isEqualTo("Ana");
        for(int i=0;i<4;i++) expectStatus(409,()->service.identificar(s,new Identificar("30111222")));
        expectStatus(429,()->service.identificar(s,new Identificar("30111222")));
    }
    @Test void listaSoloTurnosDelClienteEnElProfesionalDelEnlace() {
        String s=session(); var r=service.confirmar(s,"own",new Confirmar(reference(s).token(),null));
        assertThat(service.misTurnos(s)).extracting(TurnoPublico::turnoId).containsExactly(r.turnoId());
        Profesional otro=professional();
        registrar.ejecutar(otro.getId(),"Otra","Persona",TipoDocumento.DNI,"30111222","otra@test.com","1122334455",false,"test");
        String otraSesion=service.abrir(service.regenerar(otro.getId()).token()).token();
        service.identificar(otraSesion,new Identificar("30111222"));
        assertThat(service.misTurnos(otraSesion)).isEmpty();
        String anonima=service.abrir(link).token();
        expectStatus(403,()->service.misTurnos(anonima));
        estados.registrarCambio(AmbitoEstado.TURNO,r.turnoId(),"CONFIRMADO","test","",null);
        assertThat(service.misTurnos(s).getFirst().estado()).isEqualTo("CONFIRMADO");
    }
    @Test void aislamientoProfesionalYReferencias() {
        String s=session(); Profesional other=professional(); TipoAtencion otherType=type(other);
        registrar.ejecutar(other.getId(),"Otro","Cliente",TipoDocumento.DNI,"30111222","otro@test.com","1199999999",false,"test");
        assertThat(service.brechas(s,fecha)).isNotEmpty();
        assertThat(reference(s)).isNotNull();
        String foreign=service.abrir(service.regenerar(other.getId()).token()).token();
        assertThat(service.identificar(foreign,new Identificar("30111222")).nombre()).isEqualTo("Otro");
        String ref=reference(s).token();
        expectStatus(403,()->service.confirmar(foreign,"foreign",new Confirmar(ref,null)));
        String second=session(); expectStatus(403,()->service.confirmar(second,"same-pro",new Confirmar(ref,null)));
        assertThat(turnos.findByDiaAgendaId(dia.getId())).isEmpty();
    }
    @Test void enlaceRevocadoInvalidaSesiones() {
        String s=session(); service.revocar(p.getId());
        expectStatus(401,()->service.abrir(link)); expectStatus(401,()->service.configuracion(s));
        String fresh=service.regenerar(p.getId()).token(); assertThat(service.abrir(fresh).token()).isNotBlank();
        expectStatus(401,()->service.abrir(link));
    }
    @Test void vencimientos() {
        String s=session(); var ref=reference(s);
        jdbc.update("UPDATE autogestion_intervalo SET vence=? WHERE huella=?",java.sql.Timestamp.from(clock.instant().minusSeconds(1)),TokensAutogestion.huella(ref.token()));
        expectStatus(409,()->service.confirmar(s,"expired",new Confirmar(ref.token(),null)));
        jdbc.update("UPDATE autogestion_sesion SET vence=? WHERE huella=?",java.sql.Timestamp.from(clock.instant().minusSeconds(1)),TokensAutogestion.huella(s));
        expectStatus(401,()->service.configuracion(s));
    }
    @Test void revalidaEstadoClienteYDisponibilidad() {
        String s=session(); var ref=reference(s);
        estados.registrarCambio(AmbitoEstado.CLIENTE,c.getId(),"INHABILITADO","test","",null);
        expectStatus(403,()->service.confirmar(s,"disabled",new Confirmar(ref.token(),null)));
        estados.registrarCambio(AmbitoEstado.CLIENTE,c.getId(),"HABILITADO","test","",null);
        brechas.deleteAll(brechas.findByDiaAgendaId(dia.getId()));
        expectStatus(409,()->service.confirmar(s,"removed",new Confirmar(ref.token(),null)));
    }
    @Test void rechazaHorarioFueraDeBrechaEIntervaloYFechasPasadas() {
        String s=session();
        expectStatus(409,()->service.elegir(s,new Elegir(fecha,LocalTime.of(7,30))));
        expectStatus(409,()->service.elegir(s,new Elegir(fecha,LocalTime.of(8,15))));
        expectStatus(400,()->service.brechas(s,LocalDate.now(clock).minusDays(1)));
        expectStatus(400,()->service.fechas(s,fecha,fecha.plusDays(91)));
    }
    @Test void agendaPrivadaBloqueaSesionesExistentes() {
        String s=session(); Configuracion cfg=configs.findByProfesionalId(p.getId()).orElseThrow(); cfg.setAgendaSoloManejadaPorProfesional(true); configs.save(cfg);
        expectStatus(403,()->service.abrir(link)); expectStatus(403,()->service.configuracion(s));
    }
    @Test void concurrenciaUltimoCupo() throws Exception {
        registrar.ejecutar(p.getId(),"Otro","Cliente",TipoDocumento.DNI,"50111222","segundo@test.com","1177777777",false,"test");
        String s1=session(),s2=service.abrir(link).token();
        service.identificar(s2,new Identificar("50111222"));
        String r1=reference(s1).token(),r2=reference(s2).token();
        ExecutorService executor=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Integer> a=()->{start.await();try{service.confirmar(s1,"a",new Confirmar(r1,null));return 201;}catch(AutogestionException e){return e.getStatus();}};
            Callable<Integer> b=()->{start.await();try{service.confirmar(s2,"b",new Confirmar(r2,null));return 201;}catch(AutogestionException e){return e.getStatus();}};
            Future<Integer> f1=executor.submit(a),f2=executor.submit(b);start.countDown();
            assertThat(List.of(f1.get(20,TimeUnit.SECONDS),f2.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
            assertThat(turnos.findByDiaAgendaId(dia.getId())).hasSize(1);
        } finally { executor.shutdownNow(); }
    }
    @Test void reintentosConcurrentesCreanUnSoloTurno() throws Exception {
        String s=session(),ref=reference(s).token(); ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Resultado> action=()->{start.await();return service.confirmar(s,"same-key",new Confirmar(ref,null));};
            Future<Resultado> a=executor.submit(action),b=executor.submit(action);start.countDown();
            assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));
            assertThat(turnos.findByDiaAgendaId(dia.getId())).hasSize(1);
        } finally {executor.shutdownNow();}
    }
    @Test void contratoHttpYCierreLegacy() throws Exception {
        mvc.perform(post("/api/autogestion/sesiones").contentType("application/json").content("{\"token\":\"invalid\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/autogestion/configuracion")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/autogestion/profesionales/"+p.getId()+"/tipos-atencion")).andExpect(status().isGone());
        mvc.perform(post("/api/profesionales/"+p.getId()+"/enlace-autogestion")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/profesionales/"+p.getId()+"/enlace-autogestion").principal(()->p.getId().toString()))
            .andExpect(status().isForbidden());
        String s=session();
        mvc.perform(put("/api/autogestion/cliente/contacto").header("Authorization","Bearer "+s)
            .contentType("application/json").content("{\"email\":\"invalid\",\"telefono\":\"12345678\"}"))
            .andExpect(status().isBadRequest());
        String ref=reference(s).token();
        mvc.perform(post("/api/autogestion/turnos").header("Authorization","Bearer "+s).header("Idempotency-Key","http")
            .contentType("application/json").content("{\"referencia\":\""+ref+"\",\"profesionalId\":999,\"clienteId\":999,\"diaAgendaId\":999}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("ASIGNADO"));
        assertThat(turnos.findByDiaAgendaId(dia.getId())).hasSize(1);
        assertThat(turnos.findByDiaAgendaId(dia.getId()).getFirst().getCliente().getId()).isEqualTo(c.getId());
    }
    @Test void limitesPersistentesIndependientes() {
        String bucket=UUID.randomUUID().toString();
        assertThat(limiter.permitido(bucket,2)).isTrue();assertThat(limiter.permitido(bucket,2)).isTrue();
        assertThat(limiter.permitido(bucket,2)).isFalse();
    }
    @Test void clienteNuevoPuedeReservarDespuesDeVerificacionProfesional() {
        String s=service.abrir(link).token(); service.identificar(s,new Identificar("40111222"));
        service.registrar(s,new Nuevo("Nuevo","Cliente","nuevo@test.com","1199887766"));
        Cliente nuevo=clientes.findByProfesionalIdAndNumeroDocumento(p.getId(),"40111222").orElseThrow();
        assertThat(reference(s).token()).isNotBlank();
        estados.registrarCambio(AmbitoEstado.CLIENTE,nuevo.getId(),"HABILITADO","profesional","Verificado",null);
        assertThat(service.confirmar(s,"verified",new Confirmar(reference(s).token(),null)).estado()).isEqualTo("ASIGNADO");
    }
    @Test void gestionDeEnlaceRequiereIdentidadYRolDelProfesional() throws Exception {
        mvc.perform(post("/api/profesionales/"+p.getId()+"/enlace-autogestion")
                .principal(()->p.getId().toString()).with(req->{req.addUserRole("PROFESIONAL");return req;}))
            .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(delete("/api/profesionales/"+(p.getId()+1)+"/enlace-autogestion")
                .principal(()->p.getId().toString()).with(req->{req.addUserRole("PROFESIONAL");return req;}))
            .andExpect(status().isForbidden());
    }
    @Test void filtroLimitaIpEIgnoraCabecerasDeIpFalsificadas() throws Exception {
        String ip="test-"+UUID.randomUUID();
        for(int i=0;i<60;i++) {
            mvc.perform(get("/api/autogestion/configuracion").with(req->{req.setRemoteAddr(ip);return req;})
                    .header("X-Forwarded-For","other-"+i))
                .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/autogestion/configuracion").with(req->{req.setRemoteAddr(ip);return req;})
                .header("X-Forwarded-For","fresh"))
            .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","60"));
    }
    @Test void eliminarAgendaNoDejaReferenciasReservables() {
        String s=session(); var ref=reference(s);
        var result=service.confirmar(s,"before-delete",new Confirmar(ref.token(),null));
        jdbc.update("DELETE FROM turno WHERE id=?",result.turnoId());
        expectStatus(403,()->service.confirmar(s,"before-delete",new Confirmar(ref.token(),null)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM autogestion_intervalo WHERE huella=?",Integer.class,TokensAutogestion.huella(ref.token()))).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM autogestion_confirmacion WHERE sesion=?",Integer.class,TokensAutogestion.huella(s))).isZero();
    }
    @Test void enlaceRecuperableYCifrado() throws Exception {
        assertThat(service.enlace(p.getId()).token()).isEqualTo(link);
        String encrypted=jdbc.queryForObject("SELECT token_cifrado FROM autogestion_enlace WHERE huella=?",String.class,TokensAutogestion.huella(link));
        assertThat(encrypted).doesNotContain(link);
        mvc.perform(get("/api/profesionales/"+p.getId()+"/enlace-autogestion"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/profesionales/"+p.getId()+"/enlace-autogestion").principal(()->p.getId().toString())
            .with(req->{req.addUserRole("PROFESIONAL");return req;}))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").value(link));
        service.revocar(p.getId()); assertThat(service.enlace(p.getId()).activo()).isFalse();
    }
    @Test void sesionDevuelvePerfilIdentidadYResultadoSinOtrosClientes() {
        String token=service.abrir(link).token();
        var first=service.estadoSesion(token);
        assertThat(first.profesional().nombre()).isEqualTo(p.getNombre());assertThat(first.cliente()).isNull();
        service.identificar(token,new Identificar("30111222"));
        var identified=service.estadoSesion(token);
        assertThat(identified.dni()).isEqualTo("30111222");assertThat(identified.cliente().nombre()).isEqualTo(c.getNombre());
        var result=service.confirmar(token,"resume",new Confirmar(reference(token).token(),null));
        assertThat(service.estadoSesion(token).resultado()).isEqualTo(result);
    }
}
