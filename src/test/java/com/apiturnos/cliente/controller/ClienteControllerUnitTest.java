package com.apiturnos.cliente.controller;

import com.apiturnos.cliente.dto.ClienteDetalleDto;
import com.apiturnos.cliente.dto.ClienteResumenDto;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.service.ListarCarteraClientes;
import com.apiturnos.cliente.service.ObtenerCliente;
import com.apiturnos.cliente.service.RegistrarCliente;
import com.apiturnos.cliente.service.EditarCliente;
import com.apiturnos.cliente.service.DarDeBajaCliente;
import com.apiturnos.shared.exception.ClienteNoPerteneceProfesionalException;
import com.apiturnos.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClienteControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private ListarCarteraClientes listarCarteraClientes;

    @Mock
    private ObtenerCliente obtenerCliente;
    @Mock private RegistrarCliente registrarCliente;
    @Mock private EditarCliente editarCliente;
    @Mock private DarDeBajaCliente darDeBajaCliente;

    @InjectMocks
    private ClienteController clienteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/profesionales/{profesionalId}/clientes - Retorna lista paginada")
    void testListarCartera() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setNombre("Carlos");
        cliente.setApellido("Tevez");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("32111222");

        ClienteResumenDto dto = new ClienteResumenDto(cliente, "HABILITADO");
        when(listarCarteraClientes.ejecutar(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/profesionales/1/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].nombre").value("Carlos"))
                .andExpect(jsonPath("$.content[0].apellido").value("Tevez"))
                .andExpect(jsonPath("$.content[0].numeroDocumento").value("32111222"))
                .andExpect(jsonPath("$.content[0].estadoActual").value("HABILITADO"));
    }

    @Test
    @DisplayName("GET /api/profesionales/{profesionalId}/clientes/{clienteId} - 403 cuando no pertenece")
    void testObtenerClienteAjeno403() throws Exception {
        when(obtenerCliente.ejecutar(1L, 99L))
                .thenThrow(new ClienteNoPerteneceProfesionalException(99L, 1L));

        mockMvc.perform(get("/api/profesionales/1/clientes/99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST crea un cliente y responde 201")
    void crearCliente() throws Exception {
        Cliente creado = cliente(10L, "Ana", "Pérez");
        when(registrarCliente.ejecutar(eq(1L), eq("Ana"), eq("Pérez"), eq(TipoDocumento.DNI),
                eq("30111222"), eq("ana@test.com"), eq("11223344"), eq(false), eq("profesional")))
                .thenReturn(creado);
        when(obtenerCliente.ejecutar(1L, 10L)).thenReturn(new ClienteDetalleDto(creado, "HABILITADO"));

        mockMvc.perform(post("/api/profesionales/1/clientes")
                        .header("X-Usuario", "profesional")
                        .contentType("application/json")
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","tipoDocumento":"DNI",
                                 "numeroDocumento":"30111222","email":"ana@test.com","telefono":"11223344"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/profesionales/1/clientes/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estadoActual").value("HABILITADO"));
    }

    @Test
    @DisplayName("PUT modifica todos los datos editables")
    void editarCliente() throws Exception {
        Cliente editado = cliente(10L, "Ana María", "Pérez");
        when(editarCliente.ejecutar(eq(1L), eq(10L), eq("Ana María"), eq("Pérez"),
                eq(TipoDocumento.DNI), eq("30111222"), eq("ana.nueva@test.com"),
                eq("1199999999"), eq(false), eq("profesional"))).thenReturn(editado);
        when(obtenerCliente.ejecutar(1L, 10L)).thenReturn(new ClienteDetalleDto(editado, "HABILITADO"));

        mockMvc.perform(put("/api/profesionales/1/clientes/10")
                        .header("X-Usuario", "profesional")
                        .contentType("application/json")
                        .content("""
                                {"nombre":"Ana María","apellido":"Pérez","tipoDocumento":"DNI",
                                 "numeroDocumento":"30111222","email":"ana.nueva@test.com",
                                 "telefono":"1199999999","notificacionesHabilitadas":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"));
    }

    @Test
    @DisplayName("DELETE realiza baja lógica y responde 204")
    void darDeBajaCliente() throws Exception {
        when(darDeBajaCliente.ejecutar(1L, 10L, "Solicitud del cliente", "profesional"))
                .thenReturn(cliente(10L, "Ana", "Pérez"));

        mockMvc.perform(delete("/api/profesionales/1/clientes/10")
                        .header("X-Usuario", "profesional")
                        .contentType("application/json")
                        .content("{\"motivo\":\"Solicitud del cliente\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST rechaza un email inválido")
    void crearClienteInvalido() throws Exception {
        mockMvc.perform(post("/api/profesionales/1/clientes")
                        .contentType("application/json")
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","tipoDocumento":"DNI",
                                 "numeroDocumento":"30111222","email":"invalido","telefono":"11223344"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private Cliente cliente(Long id, String nombre, String apellido) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNombre(nombre);
        cliente.setApellido(apellido);
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("30111222");
        cliente.setEmail("ana@test.com");
        cliente.setTelefono("11223344");
        cliente.setNotificacionesHabilitadas(true);
        return cliente;
    }
}

