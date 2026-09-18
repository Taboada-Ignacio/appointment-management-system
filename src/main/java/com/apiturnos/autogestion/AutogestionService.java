package com.apiturnos.autogestion;

import com.apiturnos.agenda.repository.DiaAgendaRepository;
import com.apiturnos.agenda.model.MesAgenda;
import com.apiturnos.auditoria.model.OperacionAuditoria;
import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.cliente.service.RegistrarCliente;
import com.apiturnos.disponibilidad.service.CalcularDisponibilidadDia;
import com.apiturnos.turno.service.VerificadorCapacidad;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ConfiguracionRepository;
import com.apiturnos.turno.model.OrigenTurno;
import com.apiturnos.turno.model.Turno;
import com.apiturnos.turno.service.CrearTurno;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static com.apiturnos.autogestion.AutogestionDtos.*;

@Service
@Transactional
public class AutogestionService {
    private final JdbcTemplate jdbc;
    private final EntityManager em;
    private final Clock clock;
    private final ClienteRepository clientes;
    private final RegistrarCliente registrar;
    private final GestorCambioEstado estados;
    private final DiaAgendaRepository dias;
    private final ConfiguracionRepository configuraciones;
    private final CalcularDisponibilidadDia disponibilidad;
    private final VerificadorCapacidad capacidad;
    private final CrearTurno crear;
    private final RegistradorAuditoria auditoria;
    private final LimitarAutogestion limites;
    private final CifrarEnlaceAutogestion cifrado;

    public AutogestionService(JdbcTemplate jdbc, EntityManager em, Clock clock, ClienteRepository clientes,
            RegistrarCliente registrar, GestorCambioEstado estados,
            DiaAgendaRepository dias, ConfiguracionRepository configuraciones,
            CalcularDisponibilidadDia disponibilidad, VerificadorCapacidad capacidad,
            CrearTurno crear, RegistradorAuditoria auditoria, LimitarAutogestion limites, CifrarEnlaceAutogestion cifrado) {
        this.jdbc=jdbc; this.em=em; this.clock=clock; this.clientes=clientes; this.registrar=registrar;
        this.estados=estados; this.dias=dias; this.configuraciones=configuraciones;
        this.disponibilidad=disponibilidad; this.capacidad=capacidad; this.crear=crear;
        this.auditoria=auditoria; this.limites=limites;
        this.cifrado=cifrado;
    }

    public Credencial regenerar(Long profesional) {
        if (em.find(Profesional.class, profesional, LockModeType.PESSIMISTIC_WRITE) == null)
            throw new AutogestionException(404, "Profesional inexistente");
        revocar(profesional);
        String token = TokensAutogestion.nuevo();
        jdbc.update("INSERT INTO autogestion_enlace(profesional_id, huella, token_cifrado) VALUES (?, ?, ?)", profesional, TokensAutogestion.huella(token), cifrado.cifrar(token));
        auditar(profesional, profesional, "Enlace generado");
        return new Credencial(token, null);
    }

    public void revocar(Long profesional) {
        if (em.find(Profesional.class, profesional, LockModeType.PESSIMISTIC_WRITE) == null)
            throw new AutogestionException(404, "Profesional inexistente");
        jdbc.update("UPDATE autogestion_enlace SET activo=false WHERE profesional_id=? AND activo", profesional);
        auditar(profesional, profesional, "Enlace revocado; sesiones invalidadas");
    }

    public Credencial abrir(String token) {
        var rows = jdbc.query("SELECT id, profesional_id FROM autogestion_enlace WHERE huella=? AND activo FOR UPDATE",
                (rs,n) -> new long[]{rs.getLong(1),rs.getLong(2)}, TokensAutogestion.validar(token));
        if (rows.isEmpty()) throw invalida();
        habilitada(rows.getFirst()[1]);
        String session = TokensAutogestion.nuevo();
        Instant expires = clock.instant().plus(Duration.ofMinutes(30));
        jdbc.update("INSERT INTO autogestion_sesion(huella, enlace_id, vence) VALUES (?, ?, ?)",
                TokensAutogestion.huella(session), rows.getFirst()[0], ts(expires));
        return new Credencial(session, expires);
    }

    private record Sesion(String huella, long profesional, Long cliente, String dni, String telefono) {}

    public Enlace enlace(Long profesional) {
        if (em.find(Profesional.class, profesional) == null)
            throw new AutogestionException(404, "Profesional inexistente");
        var rows=jdbc.query("SELECT token_cifrado FROM autogestion_enlace WHERE profesional_id=? AND activo",
            (rs,n)-> {String value=rs.getString(1);return new Enlace(true,value!=null,value==null?null:cifrado.descifrar(value));},profesional);
        return rows.isEmpty()?new Enlace(false,false,null):rows.getFirst();
    }

    private record ConfirmacionReciente(Resultado resultado, String clave) {}

    public EstadoSesion estadoSesion(String token) {
        Sesion s=sesion(token);
        Profesional p=em.find(Profesional.class,s.profesional());
        ClientePublico c=s.cliente()==null?(s.dni()==null?null:new ClientePublico("NUEVO",false,null,null,null,null)):publico(cliente(s,false));
        var results=jdbc.query("SELECT * FROM autogestion_confirmacion WHERE sesion=? ORDER BY turno_id DESC LIMIT 1",
            (rs,n)->new ConfirmacionReciente(new Resultado(rs.getLong("turno_id"),rs.getString("estado"),rs.getTimestamp("inicio").toInstant(),rs.getTimestamp("fin").toInstant()),rs.getString("clave")),s.huella());
        return new EstadoSesion(new ProfesionalPublico(p.getNombre(),p.getApellido(),p.getEspecialidad(),p.getTelefono(),clock.getZone().getId()),
            c,s.dni(),results.isEmpty()?null:results.getFirst().resultado(),results.isEmpty()?null:results.getFirst().clave());
    }

    private Sesion sesion(String token) {
        String hash = TokensAutogestion.validar(token);
        var rows = jdbc.query("""
            SELECT s.*, e.profesional_id FROM autogestion_sesion s
            JOIN autogestion_enlace e ON e.id=s.enlace_id
            WHERE s.huella=? AND s.vence>? AND e.activo FOR UPDATE OF s, e
            """, (rs,n) -> new Sesion(hash, rs.getLong("profesional_id"),
                rs.getObject("cliente_id", Long.class), rs.getString("dni"), rs.getString("telefono")), hash, ts(clock.instant()));
        if (rows.isEmpty()) throw invalida();
        habilitada(rows.getFirst().profesional());
        return rows.getFirst();
    }

    public ClientePublico identificar(String token, Identificar request) {
        // The independent transaction preserves failed-attempt counters.
        String hash = TokensAutogestion.validar(token);
        if (!limites.permitido("dni-session:"+hash, 5)) throw new AutogestionException(429, "Demasiados intentos de identificación");
        Sesion s = sesion(token);
        if (!limites.permitido("dni:"+s.profesional()+":"+request.dni(), 5))
            throw new AutogestionException(429, "Demasiados intentos de identificación");
        if (s.dni()!=null) throw new AutogestionException(409, "La sesión ya tiene una identidad. Abrí una nueva sesión.");
        Cliente c=clientes.findByProfesionalIdAndNumeroDocumento(s.profesional(), request.dni()).orElse(null);
        jdbc.update("UPDATE autogestion_sesion SET dni=?, telefono=?, cliente_id=? WHERE huella=?",
                request.dni(), c==null?null:c.getTelefono(), c==null?null:c.getId(), s.huella());
        auditar(s.profesional(), c==null?s.profesional():c.getId(), "Identificación de cliente realizada");
        return c==null ? new ClientePublico("NUEVO", false, null,null,null,null) : publico(c);
    }

    public ClientePublico registrar(String token, Nuevo request) {
        Sesion s=sesion(token);
        if(s.dni()==null || s.cliente()!=null) throw new AutogestionException(409, "Primero identificá un cliente nuevo");
        Cliente c=registrar.ejecutar(s.profesional(), request.nombre(), request.apellido(), TipoDocumento.DNI,
                s.dni(), request.email(), TokensAutogestion.telefono(request.telefono()), true, "cliente-autogestion");
        jdbc.update("UPDATE autogestion_sesion SET cliente_id=? WHERE huella=?", c.getId(),s.huella());
        return publico(c);
    }

    private String nombreCliente(String valor) {
        if (valor.isBlank()) throw new AutogestionException(400,"Nombre y apellido son obligatorios");
        return java.util.Arrays.stream(valor.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .map(parte -> parte.substring(0,1).toUpperCase(Locale.ROOT)+parte.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }

    public ClientePublico contacto(String token, Contacto request) {
        Sesion s=sesion(token);
        Cliente c=cliente(s, false);
        if (request.nombre()!=null) c.setNombre(nombreCliente(request.nombre()));
        if (request.apellido()!=null) c.setApellido(nombreCliente(request.apellido()));
        c.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        c.setTelefono(TokensAutogestion.telefono(request.telefono()));
        clientes.save(c);
        auditar(s.profesional(),c.getId(),"Contacto actualizado desde autogestión");
        return publico(c);
    }

    public List<TurnoPublico> misTurnos(String token) {
        Sesion s = sesion(token);
        Cliente c = cliente(s, false);
        return em.createQuery("""
            SELECT t FROM Turno t
            JOIN FETCH t.diaAgenda d JOIN FETCH d.mesAgenda m
            JOIN FETCH m.agendaAnual a LEFT JOIN FETCH t.tipoAtencion
            WHERE t.cliente.id=:cliente AND a.profesional.id=:profesional
            ORDER BY t.inicioEstimado DESC, t.id DESC
            """, Turno.class).setParameter("cliente", c.getId())
            .setParameter("profesional", s.profesional()).getResultList().stream()
            .map(t -> new TurnoPublico(t.getId(), estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO,t.getId()),
                t.getInicioEstimado(),t.getFinEstimado(),t.getTipoAtencion()==null?null:t.getTipoAtencion().getNombre()))
            .toList();
    }

    private List<YearMonth> mesesActivos(Sesion s) {
        YearMonth actual=YearMonth.now(clock);
        return em.createQuery("SELECT m FROM MesAgenda m JOIN FETCH m.agendaAnual a WHERE a.profesional.id=:profesional",MesAgenda.class)
            .setParameter("profesional",s.profesional()).getResultList().stream()
            .filter(m -> "ACTIVO".equals(estados.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA,m.getId())))
            .map(m -> YearMonth.of(m.getAgendaAnual().getAnio(),m.getNroMes()))
            .filter(m -> !m.isBefore(actual)).sorted().toList();
    }

    public List<YearMonth> mesesActivos(String token) {
        Sesion s=sesion(token); cliente(s,true); return mesesActivos(s);
    }

    public List<DiaPublico> diasMes(String token, YearMonth mes) {
        Sesion s=sesion(token); cliente(s,true);
        var activos=mesesActivos(s);
        if(mes==null && activos.isEmpty()) return List.of();
        YearMonth elegido=mes==null?activos.getFirst():mes;
        if(!activos.contains(elegido)) throw new AutogestionException(409,"El mes no está activo para reservar");
        LocalDate hoy=LocalDate.now(clock);
        return dias.findByProfesionalIdAndFechaBetween(s.profesional(),elegido.atDay(1),elegido.atEndOfMonth()).stream()
            .filter(d -> "ACTIVO".equals(estados.obtenerNombreEstadoActual(AmbitoEstado.DIA_AGENDA,d.getId())))
            .sorted(Comparator.comparing(d -> d.getFecha()))
            .map(d -> new DiaPublico(d.getId(),d.getFecha(),"ACTIVO",!d.getFecha().isBefore(hoy))).toList();
    }

    public List<LocalDate> fechas(String token, LocalDate desde, LocalDate hasta) {
        Sesion s=sesion(token); cliente(s,true);
        if(desde==null || hasta==null || desde.isBefore(LocalDate.now(clock)) || hasta.isBefore(desde)
                || hasta.isAfter(desde.plusDays(90))) throw new AutogestionException(400,"Rango inválido; máximo 90 días");
        return dias.findByProfesionalIdAndFechaBetween(s.profesional(),desde,hasta).stream()
            .map(d -> d.getFecha()).filter(f -> !horarios(s,f).isEmpty()).sorted().toList();
    }

    public List<Brecha> brechas(String token, LocalDate fecha) {
        Sesion s=sesion(token); cliente(s,true);
        if(fecha==null || fecha.isBefore(LocalDate.now(clock))) throw new AutogestionException(400,"Fecha inválida");
        if(dias.findByProfesionalIdAndFecha(s.profesional(),fecha).isEmpty()) return List.of();
        List<Horario> available=horarios(s,fecha);
        return disponibilidad.ejecutar(s.profesional(),fecha).stream().map(b -> new Brecha(b.inicio(),b.fin(),
            available.stream().filter(h -> !h.inicio().isBefore(b.inicio()) && !h.fin().isAfter(b.fin())).toList()))
            .filter(b -> !b.intervalos().isEmpty()).toList();
    }

    public Credencial elegir(String token, Elegir request) {
        Sesion s=sesion(token); Cliente c=cliente(s,true);
        var dia=dias.findByProfesionalIdAndFecha(s.profesional(),request.fecha())
            .orElseThrow(() -> new AutogestionException(409,"Horario no disponible"));
        Horario h=horarios(s,request.fecha()).stream()
            .filter(v -> v.inicio().equals(request.horaInicio())).findFirst()
            .orElseThrow(() -> new AutogestionException(409,"Horario no disponible"));
        String reference=TokensAutogestion.nuevo(); Instant expires=clock.instant().plus(Duration.ofMinutes(5));
        jdbc.update("""
            INSERT INTO autogestion_intervalo(huella,sesion,cliente_id,dia_id,tipo_id,inicio,fin,vence)
            VALUES (?,?,?,?,?,?,?,?)
            """,TokensAutogestion.huella(reference),s.huella(),c.getId(),dia.getId(),null,
            ts(instant(request.fecha(),h.inicio())),ts(instant(request.fecha(),h.fin())),ts(expires));
        return new Credencial(reference,expires);
    }

    private record Intervalo(long dia, Long tipo, long cliente, Instant inicio, Instant fin, Instant vence, Long turno) {}

    public Resultado confirmar(String token, String clave, Confirmar request) {
        if(clave==null || !clave.matches("[A-Za-z0-9_-]{1,100}")) throw new AutogestionException(400,"Clave de idempotencia inválida");
        Sesion s=sesion(token);
        String ref=TokensAutogestion.validar(request.referencia());
        String observations=request.observaciones()==null?"":request.observaciones();
        var previous=jdbc.query("SELECT * FROM autogestion_confirmacion WHERE sesion=? AND clave=?", (rs,n) -> {
            if(!ref.equals(rs.getString("intervalo")) || !observations.equals(rs.getString("observaciones")))
                throw new AutogestionException(409,"La clave ya fue utilizada con otra solicitud");
            return new Resultado(rs.getLong("turno_id"),rs.getString("estado"),rs.getTimestamp("inicio").toInstant(),rs.getTimestamp("fin").toInstant());
        },s.huella(),clave);
        if(!previous.isEmpty()) return previous.getFirst();
        Cliente c=cliente(s,true);
        var references=jdbc.query("SELECT * FROM autogestion_intervalo WHERE huella=? AND sesion=? FOR UPDATE", (rs,n) ->
            new Intervalo(rs.getLong("dia_id"),rs.getObject("tipo_id",Long.class),rs.getLong("cliente_id"),rs.getTimestamp("inicio").toInstant(),
                rs.getTimestamp("fin").toInstant(),rs.getTimestamp("vence").toInstant(),rs.getObject("turno_id",Long.class)),ref,s.huella());
        if(references.isEmpty()) throw new AutogestionException(403,"Referencia ajena a la sesión");
        Intervalo i=references.getFirst();
        if(i.turno()!=null) throw new AutogestionException(409,"El intervalo ya fue confirmado");
        if(!i.vence().isAfter(clock.instant()) || i.cliente()!=c.getId()) throw new AutogestionException(409,"Referencia vencida o inválida");
        var dia=dias.findByIdForUpdate(i.dia()).orElseThrow(() -> new AutogestionException(409,"Día no disponible"));
        if(!dia.getMesAgenda().getAgendaAnual().getProfesional().getId().equals(s.profesional()))
            throw new AutogestionException(403,"Día ajeno al profesional");
        if(i.tipo()!=null) throw new AutogestionException(409,"Volvé a seleccionar un horario con la configuración actual");
        boolean available=horarios(s,dia.getFecha()).stream().anyMatch(h ->
            instant(dia.getFecha(),h.inicio()).equals(i.inicio()) && instant(dia.getFecha(),h.fin()).equals(i.fin()));
        if(!available) throw new AutogestionException(409,"El horario dejó de estar disponible");
        var result=crear.ejecutar(i.dia(),c.getId(),null,i.inicio(),i.fin(),OrigenTurno.CLIENTE_AUTOGESTION,false,observations,"cliente-autogestion");
        Long turno=result.getTurno().getId();
        String state=estados.obtenerNombreEstadoActual(AmbitoEstado.TURNO,turno);
        jdbc.update("UPDATE autogestion_intervalo SET turno_id=? WHERE huella=?",turno,ref);
        jdbc.update("""
            INSERT INTO autogestion_confirmacion(sesion,clave,intervalo,observaciones,turno_id,estado,inicio,fin)
            VALUES (?,?,?,?,?,?,?,?)
            """,s.huella(),clave,ref,observations,turno,state,ts(i.inicio()),ts(i.fin()));
        return new Resultado(turno,state,i.inicio(),i.fin());
    }

    public ConfiguracionPublica configuracion(String token) {
        Sesion s=sesion(token); cliente(s,true);
        var c=configuracionProfesional(s);
        return new ConfiguracionPublica(c.getDuracionAproximadaPorTurno(),c.getCantidadMaxTurnosALaVez());
    }
    private com.apiturnos.profesional.model.Configuracion configuracionProfesional(Sesion s) {
        var c=configuraciones.findByProfesionalId(s.profesional())
            .orElseThrow(() -> new AutogestionException(409,"El profesional todavía no configuró sus turnos"));
        if(c.getDuracionAproximadaPorTurno()==null || c.getDuracionAproximadaPorTurno()<1
                || c.getCantidadMaxTurnosALaVez()==null || c.getCantidadMaxTurnosALaVez()<1)
            throw new AutogestionException(409,"La configuración de turnos no es válida");
        em.lock(c,LockModeType.PESSIMISTIC_READ);
        return c;
    }
    private List<Horario> horarios(Sesion s, LocalDate fecha) {
        var dia=dias.findByProfesionalIdAndFecha(s.profesional(),fecha);
        if(dia.isEmpty() || !"ACTIVO".equals(estados.obtenerNombreEstadoActual(AmbitoEstado.MES_AGENDA,dia.get().getMesAgenda().getId())))
            return List.of();
        var c=configuracionProfesional(s);
        int duracion=c.getDuracionAproximadaPorTurno();
        List<Horario> result=new ArrayList<>();
        for(var brecha:disponibilidad.ejecutar(s.profesional(),fecha)) {
            int inicio=brecha.inicio().toSecondOfDay()/60;
            int fin=brecha.fin().toSecondOfDay()/60;
            for(int minuto=inicio; (long)minuto+duracion<=fin; minuto+=duracion) {
                LocalTime desde=LocalTime.of(minuto/60,minuto%60);
                LocalTime hasta=LocalTime.of((minuto+duracion)/60,(minuto+duracion)%60);
                Instant desdeInstant=instant(fecha,desde), hastaInstant=instant(fecha,hasta);
                if(desdeInstant.isAfter(clock.instant()) && !capacidad.excedidaCapacidad(
                        dia.get().getId(),desdeInstant,hastaInstant,null,c.getCantidadMaxTurnosALaVez()))
                    result.add(new Horario(desde,hasta));
            }
        }
        return result;
    }
    private Cliente cliente(Sesion s, boolean requireEnabled) {
        if(s.cliente()==null) throw new AutogestionException(403,"Identificá al cliente primero");
        Cliente c=em.find(Cliente.class,s.cliente(),LockModeType.PESSIMISTIC_WRITE);
        if(c==null || !c.getProfesional().getId().equals(s.profesional())) throw new AutogestionException(403,"Cliente ajeno al profesional");
        if(requireEnabled && !publico(c).puedeReservar()) throw new AutogestionException(403,"El estado del cliente no permite reservar");
        return c;
    }
    private ClientePublico publico(Cliente c) {
        String state=estados.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE,c.getId());
        return new ClientePublico(state,Set.of("HABILITADO","REQUIERE_APROBACION","PENDIENTE_DE_VERIFICACION").contains(state==null?"":state),
                c.getNombre(),c.getApellido(),c.getEmail(),c.getTelefono());
    }
    private void habilitada(long profesional) {
        if(configuraciones.findByProfesionalId(profesional).map(c -> Boolean.TRUE.equals(c.getAgendaSoloManejadaPorProfesional())).orElse(false))
            throw new AutogestionException(403,"La agenda no admite autogestión");
    }
    private void auditar(long profesional,long entidad,String detalle) {
        auditoria.registrar("AUTOGESTION","Autogestion",entidad,OperacionAuditoria.UPDATE,"autogestion",profesional,detalle);
    }
    private Instant instant(LocalDate date,LocalTime time) { return date.atTime(time).atZone(clock.getZone()).toInstant(); }
    private static Timestamp ts(Instant instant) { return Timestamp.from(instant); }
    private static AutogestionException invalida() { return new AutogestionException(401,"Credencial inválida, revocada o vencida"); }
}
