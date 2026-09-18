package com.apiturnos.autogestion;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutenticacionProfesionalDesarrolloFilterTest {
    @Test void soloSeActivaEnDesarrolloSinProduccion() {
        for (String[] profiles : new String[][] { {}, {"dev"}, {"prod"}, {"dev", "prod"}, {"dev", "production"} }) {
            try (var context = new AnnotationConfigApplicationContext()) {
                context.getEnvironment().setActiveProfiles(profiles);
                context.register(AutenticacionProfesionalDesarrolloFilter.class);
                context.refresh();
                assertThat(context.getBeansOfType(AutenticacionProfesionalDesarrolloFilter.class))
                    .hasSize(profiles.length == 1 && profiles[0].equals("dev") ? 1 : 0);
            }
        }
    }

    @Test void permiteElProfesionalSeleccionadoYRechazaOtro() throws Exception {
        var service = mock(AutogestionService.class);
        var controller = new EnlaceAutogestionController(service);
        var request = new MockHttpServletRequest("GET", "/api/profesionales/1/enlace-autogestion");
        new AutenticacionProfesionalDesarrolloFilter().doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            var authenticated = (HttpServletRequest) req;
            controller.consultar(1L, authenticated, new MockHttpServletResponse());
            assertThatThrownBy(() -> controller.consultar(2L, authenticated, new MockHttpServletResponse()))
                .isInstanceOfSatisfying(AutogestionException.class, e -> assertThat(e.getStatus()).isEqualTo(403));
        });
        verify(service).enlace(1L);
        verifyNoMoreInteractions(service);
    }

    @Test void noAutenticaPortalPublicoNiRutasInvalidas() throws Exception {
        for (String path : new String[] {"/api/autogestion/sesion", "/api/profesionales/invalid/enlace-autogestion"}) {
            var request = new MockHttpServletRequest("GET", path);
            request.addHeader("X-Profesional-Desarrollo", path.contains("profesionales") ? "invalid" : "1");
            new AutenticacionProfesionalDesarrolloFilter().doFilter(request, new MockHttpServletResponse(),
                (req, res) -> assertThat(((HttpServletRequest) req).getUserPrincipal()).isNull());
        }
    }

    @Test void conservaIdentidadRealAunqueSeEnvieEncabezadoDePrueba() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/profesionales/1/enlace-autogestion");
        request.setUserPrincipal(() -> "2");
        new AutenticacionProfesionalDesarrolloFilter().doFilter(request, new MockHttpServletResponse(),
            (req, res) -> assertThat(((HttpServletRequest) req).getUserPrincipal().getName()).isEqualTo("2"));
    }
}
