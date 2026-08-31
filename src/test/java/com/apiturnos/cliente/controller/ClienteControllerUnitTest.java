package com.apiturnos.cliente.controller;

import com.apiturnos.cliente.dto.ClienteDetalleDto;
import com.apiturnos.cliente.dto.ClienteResumenDto;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.service.ListarCarteraClientes;
import com.apiturnos.cliente.service.ObtenerCliente;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClienteControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private ListarCarteraClientes listarCarteraClientes;

    @Mock
    private ObtenerCliente obtenerCliente;

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
}

