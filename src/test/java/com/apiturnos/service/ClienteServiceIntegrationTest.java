package com.apiturnos.service;

import com.apiturnos.cliente.dto.ClienteDetalleDto;
import com.apiturnos.cliente.dto.ClienteResumenDto;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.cliente.service.*;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.model.CambioEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ClienteServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private ProfesionalRepository profesionalRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private GestorCambioEstado gestorCambioEstado;

    @Autowired
    private RegistrarCliente registrarCliente;
    @Autowired
    private ObtenerCliente obtenerCliente;
    @Autowired
    private EditarCliente editarCliente;
    @Autowired
    private BuscarClientes buscarClientes;
    @Autowired
    private ListarCarteraClientes listarCarteraClientes;
    @Autowired
    private CambiarEstadoCliente cambiarEstadoCliente;
    @Autowired
    private VerificarCliente verificarCliente;
    @Autowired
    private VerificarClientes verificarClientes;
    @Autowired
    private InhabilitarCliente inhabilitarCliente;
    @Autowired
    private DarDeBajaCliente darDeBajaCliente;
    @Autowired
    private ReactivarCliente reactivarCliente;

    private Profesional profA;
    private Profesional profB;

    @BeforeEach
    void setUp() {
        profA = new Profesional();
        profA.setNombre("Claudio");
        profA.setApellido("Vidal");
        profA.setEmail("claudio." + System.nanoTime() + "@test.com");
        profA.setTelefono("+54911111111");
        profA.setEspecialidad("Clínica Médica");
        profA = profesionalRepository.save(profA);

        profB = new Profesional();
        profB.setNombre("Silvia");
        profB.setApellido("Castro");
        profB.setEmail("silvia." + System.nanoTime() + "@test.com");
        profB.setTelefono("+54911222222");
        profB.setEspecialidad("Pediatría");
        profB = profesionalRepository.save(profB);
    }

    @Test
    @DisplayName("1. Alta manual crea Cliente con estado inicial HABILITADO y auditoría")
    void test1_AltaManualCreaClienteHabilitado() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Juan", "Perez",
                TipoDocumento.DNI, " 20111222 ", "juan@test.com", "11445566", false, "admin");

        assertThat(cliente.getId()).isNotNull();
        assertThat(cliente.getNumeroDocumento()).isEqualTo("20111222"); // Normalizado con trim

        String estado = gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId());
        assertThat(estado).isEqualTo("HABILITADO");
    }

    @Test
    @DisplayName("2. Mismo DNI puede existir para profesionales distintos")
    void test2_MismoDniEnProfesionalesDistintos() {
        String dni = "22333444";

        Cliente c1 = registrarCliente.ejecutar(
                profA.getId(), "Ana", "Gomez",
                TipoDocumento.DNI, dni, "ana1@test.com", "11223344", false, "admin");

        Cliente c2 = registrarCliente.ejecutar(
                profB.getId(), "Ana", "Gomez",
                TipoDocumento.DNI, dni, "ana2@test.com", "11223344", false, "admin");

        assertThat(c1.getId()).isNotNull();
        assertThat(c2.getId()).isNotNull();
        assertThat(c1.getId()).isNotEqualTo(c2.getId());
        assertThat(c1.getNumeroDocumento()).isEqualTo(c2.getNumeroDocumento());
    }

    @Test
    @DisplayName("3. Mismo DNI no puede repetirse dentro del mismo Profesional")
    void test3_MismoDniRechazadoEnMismoProfesional() {
        String dni = "23444555";

        registrarCliente.ejecutar(
                profA.getId(), "Carlos", "Paz",
                TipoDocumento.DNI, dni, "carlos1@test.com", "11000001", false, "admin");

        assertThatThrownBy(() -> registrarCliente.ejecutar(
                profA.getId(), "Carlos Segundo", "Paz",
                TipoDocumento.DNI, " " + dni + " ", "carlos2@test.com", "11000002", false, "admin"))
                .isInstanceOf(ClienteDuplicadoException.class);
    }

    @Test
    @DisplayName("4. Búsqueda por DNI nunca devuelve Cliente de otro Profesional")
    void test4_BusquedaPorDniLimitadaAProfesional() {
        String dni = "24555666";

        Cliente clienteProfB = registrarCliente.ejecutar(
                profB.getId(), "Mariana", "Rojas",
                TipoDocumento.DNI, dni, "mariana@test.com", "11000003", false, "admin");

        // Buscar en profA con el DNI de profB -> no debe encontrarlo
        Optional<Cliente> resultadoEnA = buscarClientes.buscarPorDni(profA.getId(), dni);
        assertThat(resultadoEnA).isEmpty();

        // Buscar en profB -> lo encuentra
        Optional<Cliente> resultadoEnB = buscarClientes.buscarPorDni(profB.getId(), " " + dni + " ");
        assertThat(resultadoEnB).isPresent();
        assertThat(resultadoEnB.get().getId()).isEqualTo(clienteProfB.getId());
    }

    @Test
    @DisplayName("5. Búsqueda por apellido funciona parcialmente y case-insensitive")
    void test5_BusquedaPorApellidoParcialCaseInsensitive() {
        registrarCliente.ejecutar(
                profA.getId(), "Esteban", "Garrido",
                TipoDocumento.DNI, "25000111", "garrido@test.com", "11111111", false, "admin");
        registrarCliente.ejecutar(
                profA.getId(), "Lucia", "Garcia",
                TipoDocumento.DNI, "25000222", "garcia@test.com", "11111112", false, "admin");
        registrarCliente.ejecutar(
                profA.getId(), "Martin", "Garat",
                TipoDocumento.DNI, "25000333", "garat@test.com", "11111113", false, "admin");
        registrarCliente.ejecutar(
                profA.getId(), "Rosa", "Lopez",
                TipoDocumento.DNI, "25000444", "lopez@test.com", "11111114", false, "admin");

        List<Cliente> encontrados = buscarClientes.buscarPorApellido(profA.getId(), "gar");
        assertThat(encontrados).hasSize(3);
        assertThat(encontrados).extracting(Cliente::getApellido)
                .containsExactlyInAnyOrder("Garrido", "Garcia", "Garat");
    }

    @Test
    @DisplayName("6. Búsqueda por nombre funciona parcialmente y case-insensitive")
    void test6_BusquedaPorNombreParcialCaseInsensitive() {
        registrarCliente.ejecutar(
                profA.getId(), "Alejandro", "Sosa",
                TipoDocumento.DNI, "26000111", "ale1@test.com", "11111115", false, "admin");
        registrarCliente.ejecutar(
                profA.getId(), "Alejandra", "Benitez",
                TipoDocumento.DNI, "26000222", "ale2@test.com", "11111116", false, "admin");

        List<Cliente> encontrados = buscarClientes.buscarPorNombre(profA.getId(), "ALE");
        assertThat(encontrados).hasSize(2);
    }

    @Test
    @DisplayName("7. Detalle valida pertenencia al Profesional y devuelve estado actual")
    void test7_DetalleValidaPertenenciaProfesional() {
        Cliente clienteA = registrarCliente.ejecutar(
                profA.getId(), "Pedro", "Alonso",
                TipoDocumento.DNI, "27000111", "pedro@test.com", "11111117", false, "admin");

        // Consulta válida con profA
        ClienteDetalleDto detalle = obtenerCliente.ejecutar(profA.getId(), clienteA.getId());
        assertThat(detalle.getId()).isEqualTo(clienteA.getId());
        assertThat(detalle.getEstadoActual()).isEqualTo("HABILITADO");

        // Consulta inválida intentando acceder desde profB -> ClienteNoPerteneceProfesionalException
        assertThatThrownBy(() -> obtenerCliente.ejecutar(profB.getId(), clienteA.getId()))
                .isInstanceOf(ClienteNoPerteneceProfesionalException.class);
    }

    @Test
    @DisplayName("8. Edición modifica datos y no permite provocar DNI duplicado dentro del Profesional")
    void test8_EdicionNoPermiteDniDuplicado() {
        Cliente c1 = registrarCliente.ejecutar(
                profA.getId(), "Valeria", "Suarez",
                TipoDocumento.DNI, "28000111", "valeria@test.com", "11111118", false, "admin");
        Cliente c2 = registrarCliente.ejecutar(
                profA.getId(), "Gabriel", "Linares",
                TipoDocumento.DNI, "28000222", "gabriel@test.com", "11111119", false, "admin");

        // Edición normal de c1
        Cliente editado = editarCliente.ejecutar(
                profA.getId(), c1.getId(), "Valeria Maria", "Suarez",
                TipoDocumento.DNI, "28000111", "valeria.nueva@test.com", "11111120", false, "admin");
        assertThat(editado.getNombre()).isEqualTo("Valeria Maria");
        assertThat(editado.getEmail()).isEqualTo("valeria.nueva@test.com");
        assertThat(editado.getNotificacionesHabilitadas()).isFalse();

        // Intentar cambiar el DNI de c1 al DNI existente de c2 -> ClienteDuplicadoException
        assertThatThrownBy(() -> editarCliente.ejecutar(
                profA.getId(), c1.getId(), "Valeria", "Suarez",
                TipoDocumento.DNI, "28000222", "valeria@test.com", "11111118", true, "admin"))
                .isInstanceOf(ClienteDuplicadoException.class);
    }

    @Test
    @DisplayName("9. Verificar cliente en PENDIENTE_DE_VERIFICACION lo lleva a HABILITADO")
    void test9_VerificarPendienteLlevaAHabilitado() {
        // Autoregistro crea en PENDIENTE_DE_VERIFICACION
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Marcos", "Molina",
                TipoDocumento.DNI, "29000111", "marcos@test.com", "11111121", true, "autoregistro");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("PENDIENTE_DE_VERIFICACION");

        verificarCliente.ejecutar(profA.getId(), cliente.getId(), "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("HABILITADO");
    }

    @Test
    @DisplayName("10. No puede verificarse un Cliente en estado incompatible")
    void test10_VerificarEnEstadoIncompatibleLanzaExcepcion() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Franco", "Armani",
                TipoDocumento.DNI, "30000111", "franco@test.com", "11111122", false, "admin");
        // Estado ya es HABILITADO

        assertThatThrownBy(() -> verificarCliente.ejecutar(profA.getId(), cliente.getId(), "admin"))
                .isInstanceOf(ClienteNoPendienteDeVerificacionException.class);
    }

    @Test
    @DisplayName("11. Verificación masiva funciona transaccionalmente y falla atómicamente si uno es inválido")
    void test11_VerificacionMasivaAtomica() {
        Cliente c1 = registrarCliente.ejecutar(
                profA.getId(), "Cliente1", "Test",
                TipoDocumento.DNI, "31000111", "c1@test.com", "11111123", true, "user");
        Cliente c2 = registrarCliente.ejecutar(
                profA.getId(), "Cliente2", "Test",
                TipoDocumento.DNI, "31000222", "c2@test.com", "11111124", true, "user");
        Cliente c3Habilitado = registrarCliente.ejecutar(
                profA.getId(), "Cliente3", "Test",
                TipoDocumento.DNI, "31000333", "c3@test.com", "11111125", false, "user");

        // Intentar verificar masivamente [c1, c2, c3Habilitado] -> debe fallar porque c3 no es PENDIENTE
        assertThatThrownBy(() -> verificarClientes.ejecutar(
                profA.getId(), List.of(c1.getId(), c2.getId(), c3Habilitado.getId()), "admin"))
                .isInstanceOf(ClienteNoPendienteDeVerificacionException.class);

        // Ninguno debió haber cambiado a HABILITADO
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, c1.getId()))
                .isEqualTo("PENDIENTE_DE_VERIFICACION");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, c2.getId()))
                .isEqualTo("PENDIENTE_DE_VERIFICACION");

        // Verificación masiva exitosa con lista válida
        List<Cliente> verificados = verificarClientes.ejecutar(
                profA.getId(), List.of(c1.getId(), c2.getId()), "admin");
        assertThat(verificados).hasSize(2);
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, c1.getId()))
                .isEqualTo("HABILITADO");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, c2.getId()))
                .isEqualTo("HABILITADO");
    }

    @Test
    @DisplayName("12. INHABILITADO conserva Cliente e historial")
    void test12_InhabilitadoConservaHistorial() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Ignacio", "Scocco",
                TipoDocumento.DNI, "32000111", "scocco@test.com", "11111126", false, "admin");

        inhabilitarCliente.ejecutar(profA.getId(), cliente.getId(), "Inconducta", "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("INHABILITADO");

        List<CambioEstado> historial = gestorCambioEstado.obtenerHistorial(AmbitoEstado.CLIENTE, cliente.getId());
        assertThat(historial).hasSize(2);
        assertThat(historial.get(0).getEstado().getNombre()).isEqualTo("HABILITADO");
        assertThat(historial.get(1).getEstado().getNombre()).isEqualTo("INHABILITADO");
    }

    @Test
    @DisplayName("13. DADO_DE_BAJA no elimina físicamente al Cliente")
    void test13_DadoDeBajaNoEliminaFisicamente() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Enzo", "Perez",
                TipoDocumento.DNI, "33000111", "enzo@test.com", "11111127", false, "admin");

        darDeBajaCliente.ejecutar(profA.getId(), cliente.getId(), "Mudanza", "admin");

        // El cliente sigue existiendo en la base de datos
        assertThat(clienteRepository.findById(cliente.getId())).isPresent();
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("DADO_DE_BAJA");
    }

    @Test
    @DisplayName("14. Reactivación desde HABILITADO -> DADO_DE_BAJA recupera HABILITADO")
    void test14_ReactivacionDesdeHabilitado() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Javier", "Pinola",
                TipoDocumento.DNI, "34000111", "pinola@test.com", "11111128", false, "admin");

        darDeBajaCliente.ejecutar(profA.getId(), cliente.getId(), "Baja temporal", "admin");
        reactivarCliente.ejecutar(profA.getId(), cliente.getId(), "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("HABILITADO");
    }

    @Test
    @DisplayName("15. Reactivación desde REQUIERE_APROBACION -> DADO_DE_BAJA recupera REQUIERE_APROBACION")
    void test15_ReactivacionDesdeRequiereAprobacion() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Milton", "Casco",
                TipoDocumento.DNI, "35000111", "casco@test.com", "11111129", false, "admin");

        cambiarEstadoCliente.ejecutar(profA.getId(), cliente.getId(), "REQUIERE_APROBACION", "Requiere control", "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("REQUIERE_APROBACION");

        darDeBajaCliente.ejecutar(profA.getId(), cliente.getId(), "Baja", "admin");
        reactivarCliente.ejecutar(profA.getId(), cliente.getId(), "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("REQUIERE_APROBACION");
    }

    @Test
    @DisplayName("16. Reactivación desde INHABILITADO -> DADO_DE_BAJA recupera INHABILITADO")
    void test16_ReactivacionDesdeInhabilitado() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Bruno", "Zuculini",
                TipoDocumento.DNI, "36000111", "zucu@test.com", "11111130", false, "admin");

        inhabilitarCliente.ejecutar(profA.getId(), cliente.getId(), "Mora", "admin");
        darDeBajaCliente.ejecutar(profA.getId(), cliente.getId(), "Baja", "admin");
        reactivarCliente.ejecutar(profA.getId(), cliente.getId(), "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("INHABILITADO");
    }

    @Test
    @DisplayName("17. Estado actual corresponde al último CambioEstado")
    void test17_EstadoActualCorrespondeAUltimoCambioEstado() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Rodrigo", "Mora",
                TipoDocumento.DNI, "37000111", "mora@test.com", "11111131", false, "admin");

        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("HABILITADO");

        inhabilitarCliente.ejecutar(profA.getId(), cliente.getId(), "Pausa", "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("INHABILITADO");

        cambiarEstadoCliente.ejecutar(profA.getId(), cliente.getId(), "HABILITADO", "Levantar pausa", "admin");
        assertThat(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, cliente.getId()))
                .isEqualTo("HABILITADO");
    }

    @Test
    @DisplayName("18. Historial completo permanece disponible después de múltiples transiciones")
    void test18_HistorialCompletoDisponible() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Gonzalo", "Montiel",
                TipoDocumento.DNI, "38000111", "cachete@test.com", "11111132", false, "admin");

        inhabilitarCliente.ejecutar(profA.getId(), cliente.getId(), "Cambio 1", "admin");
        darDeBajaCliente.ejecutar(profA.getId(), cliente.getId(), "Cambio 2", "admin");
        reactivarCliente.ejecutar(profA.getId(), cliente.getId(), "admin");

        List<CambioEstado> historial = gestorCambioEstado.obtenerHistorial(AmbitoEstado.CLIENTE, cliente.getId());
        assertThat(historial).hasSize(4);
        assertThat(historial.get(0).getEstado().getNombre()).isEqualTo("HABILITADO");
        assertThat(historial.get(1).getEstado().getNombre()).isEqualTo("INHABILITADO");
        assertThat(historial.get(2).getEstado().getNombre()).isEqualTo("DADO_DE_BAJA");
        assertThat(historial.get(3).getEstado().getNombre()).isEqualTo("INHABILITADO"); // Reactivado a su estado anterior
    }

    @Test
    @DisplayName("19. Listado de cartera con paginación y filtros por estado y texto")
    void test19_ListadoCarteraConPaginacionYFiltros() {
        Cliente c1 = registrarCliente.ejecutar(
                profA.getId(), "Agustin", "Palavecino",
                TipoDocumento.DNI, "39000111", "pala@test.com", "11111133", false, "admin");
        Cliente c2 = registrarCliente.ejecutar(
                profA.getId(), "Matias", "Suarez",
                TipoDocumento.DNI, "39000222", "matias@test.com", "11111134", false, "admin");
        Cliente c3 = registrarCliente.ejecutar(
                profA.getId(), "Nicolas", "De La Cruz",
                TipoDocumento.DNI, "39000333", "nico@test.com", "11111135", false, "admin");

        inhabilitarCliente.ejecutar(profA.getId(), c2.getId(), "Test", "admin");

        // Listar todos los de profA paginados
        Page<ClienteResumenDto> pagina = listarCarteraClientes.ejecutar(
                profA.getId(), null, null, null, null, PageRequest.of(0, 10));
        assertThat(pagina.getTotalElements()).isGreaterThanOrEqualTo(3);

        // Filtrar por estado HABILITADO
        Page<ClienteResumenDto> habilitados = listarCarteraClientes.ejecutar(
                profA.getId(), null, null, null, "HABILITADO", PageRequest.of(0, 10));
        assertThat(habilitados.getContent()).extracting(ClienteResumenDto::getId)
                .contains(c1.getId(), c3.getId())
                .doesNotContain(c2.getId());

        // Filtrar por apellido "Suarez"
        Page<ClienteResumenDto> porApellido = listarCarteraClientes.ejecutar(
                profA.getId(), null, "Suarez", null, null, PageRequest.of(0, 10));
        assertThat(porApellido.getContent()).hasSize(1);
        assertThat(porApellido.getContent().get(0).getId()).isEqualTo(c2.getId());
    }

    @Test
    @DisplayName("20. Cambiar estado rechaza estados que no pertenecen al ámbito CLIENTE")
    void test20_RechazaEstadosDeOtrosAmbitos() {
        Cliente cliente = registrarCliente.ejecutar(
                profA.getId(), "Lucas", "Beltran",
                TipoDocumento.DNI, "40000111", "lucas.b@test.com", "11111136", false, "admin");

        // "ASIGNADO" es un estado de TURNO, no de CLIENTE
        assertThatThrownBy(() -> cambiarEstadoCliente.ejecutar(
                profA.getId(), cliente.getId(), "ASIGNADO", "Error", "admin"))
                .isInstanceOf(EstadoClienteInvalidoException.class);
    }
}

