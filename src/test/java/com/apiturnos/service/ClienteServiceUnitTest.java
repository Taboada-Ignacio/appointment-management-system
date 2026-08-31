package com.apiturnos.service;

import com.apiturnos.auditoria.service.RegistradorAuditoria;
import com.apiturnos.cliente.model.Cliente;
import com.apiturnos.cliente.model.TipoDocumento;
import com.apiturnos.cliente.repository.ClienteRepository;
import com.apiturnos.cliente.service.EditarCliente;
import com.apiturnos.cliente.service.RegistrarCliente;
import com.apiturnos.cliente.service.VerificarCliente;
import com.apiturnos.cliente.service.VerificarClientes;
import com.apiturnos.estado.model.AmbitoEstado;
import com.apiturnos.estado.service.GestorCambioEstado;
import com.apiturnos.profesional.model.Profesional;
import com.apiturnos.profesional.repository.ProfesionalRepository;
import com.apiturnos.shared.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceUnitTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProfesionalRepository profesionalRepository;
    @Mock
    private GestorCambioEstado gestorCambioEstado;
    @Mock
    private RegistradorAuditoria registradorAuditoria;

    private Profesional profesional;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        profesional = new Profesional();
        profesional.setId(1L);
        profesional.setNombre("Dr. Lopez");

        cliente = new Cliente();
        cliente.setId(10L);
        cliente.setProfesional(profesional);
        cliente.setNombre("Mario");
        cliente.setApellido("Kempes");
        cliente.setTipoDocumento(TipoDocumento.DNI);
        cliente.setNumeroDocumento("10111222");
        cliente.setEmail("mario@test.com");
        cliente.setTelefono("11223344");
    }

    @Test
    @DisplayName("Registrar cliente valida campos obligatorios")
    void testRegistrarClienteValidaCamposObligatorios() {
        RegistrarCliente registrar = new RegistrarCliente(
                clienteRepository, profesionalRepository, gestorCambioEstado, registradorAuditoria);

        assertThatThrownBy(() -> registrar.ejecutar(
                null, "Mario", "Kempes", TipoDocumento.DNI, "10111222", "mario@test.com", "11223344", false, "admin"))
                .isInstanceOf(NegocioException.class);

        assertThatThrownBy(() -> registrar.ejecutar(
                1L, "", "Kempes", TipoDocumento.DNI, "10111222", "mario@test.com", "11223344", false, "admin"))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    @DisplayName("Editar cliente rechaza pertenencia a otro profesional")
    void testEditarClienteValidaProfesional() {
        EditarCliente editar = new EditarCliente(clienteRepository, registradorAuditoria);

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));

        // Profesional ID 999 no coincide con profesional ID 1
        assertThatThrownBy(() -> editar.ejecutar(
                999L, 10L, "Mario", "Kempes", TipoDocumento.DNI, "10111222", "mario@test.com", "11223344", true, "admin"))
                .isInstanceOf(ClienteNoPerteneceProfesionalException.class);
    }

    @Test
    @DisplayName("Verificar cliente individual rechaza si no está en PENDIENTE_DE_VERIFICACION")
    void testVerificarClienteRechazaSiNoEsPendiente() {
        VerificarCliente verificar = new VerificarCliente(
                clienteRepository, gestorCambioEstado, registradorAuditoria);

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, 10L)).thenReturn("HABILITADO");

        assertThatThrownBy(() -> verificar.ejecutar(1L, 10L, "admin"))
                .isInstanceOf(ClienteNoPendienteDeVerificacionException.class);
    }

    @Test
    @DisplayName("Verificación masiva no ejecuta cambios si algún cliente pertenece a otro profesional")
    void testVerificarClientesRechazaSiUnoNoPertenece() {
        VerificarClientes verificarMasivo = new VerificarClientes(
                clienteRepository, profesionalRepository, gestorCambioEstado, registradorAuditoria);

        Profesional otroProf = new Profesional();
        otroProf.setId(2L);

        Cliente clienteAjeno = new Cliente();
        clienteAjeno.setId(20L);
        clienteAjeno.setProfesional(otroProf);

        when(profesionalRepository.existsById(1L)).thenReturn(true);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findById(20L)).thenReturn(Optional.of(clienteAjeno));
        when(gestorCambioEstado.obtenerNombreEstadoActual(AmbitoEstado.CLIENTE, 10L)).thenReturn("PENDIENTE_DE_VERIFICACION");

        assertThatThrownBy(() -> verificarMasivo.ejecutar(1L, List.of(10L, 20L), "admin"))
                .isInstanceOf(ClienteNoPerteneceProfesionalException.class);

        verify(gestorCambioEstado, never()).registrarCambio(any(), any(), any(), any(), any(), any());
    }
}
