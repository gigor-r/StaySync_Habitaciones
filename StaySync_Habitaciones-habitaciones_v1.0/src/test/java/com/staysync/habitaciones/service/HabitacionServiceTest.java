package com.staysync.habitaciones.service;

import com.staysync.habitaciones.dto.request.CrearHabitacionRequest;
import com.staysync.habitaciones.dto.response.HabitacionResponse;
import com.staysync.habitaciones.exception.HabitacionNotFoundException;
import com.staysync.habitaciones.exception.HabitacionNumeroExistenteException;
import com.staysync.habitaciones.messaging.HabitacionEventPublisher;
import com.staysync.habitaciones.model.Habitacion;
import com.staysync.habitaciones.model.Habitacion.EstadoHabitacion;
import com.staysync.habitaciones.model.TipoHabitacion;
import com.staysync.habitaciones.repository.AmenidadRepository;
import com.staysync.habitaciones.repository.HabitacionRepository;
import com.staysync.habitaciones.repository.TipoHabitacionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HabitacionService - Tests Unitarios")
class HabitacionServiceTest {

    @Mock private HabitacionRepository habitacionRepository;
    @Mock private TipoHabitacionRepository tipoRepository;
    @Mock private AmenidadRepository amenidadRepository;
    @Mock private HabitacionEventPublisher eventPublisher;

    @InjectMocks private HabitacionService habitacionService;

    private TipoHabitacion tipo;
    private Habitacion habitacion;

    @BeforeEach
    void setUp() {
        tipo = TipoHabitacion.builder().id(1L).nombre("Estándar").capacidad(2)
                .precioBase(BigDecimal.valueOf(80)).activo(true).build();

        habitacion = Habitacion.builder()
                .id(1L).numero("101").piso(1).tipo(tipo)
                .estado(EstadoHabitacion.DISPONIBLE)
                .precioPorNoche(BigDecimal.valueOf(100))
                .activa(true).build();
    }

    @Test
    @DisplayName("crear() - debe crear habitación correctamente")
    void debeCrearHabitacion() {
        CrearHabitacionRequest request = new CrearHabitacionRequest();
        request.setNumero("102");
        request.setPiso(1);
        request.setTipoId(1L);
        request.setPrecioPorNoche(BigDecimal.valueOf(100));

        when(habitacionRepository.existsByNumero("102")).thenReturn(false);
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(habitacionRepository.save(any())).thenReturn(habitacion);

        HabitacionResponse response = habitacionService.crear(request);

        assertThat(response).isNotNull();
        verify(habitacionRepository).save(any(Habitacion.class));
    }

    @Test
    @DisplayName("crear() - debe lanzar excepción si el número ya existe")
    void debeLanzarExcepcionNumeroExistente() {
        CrearHabitacionRequest request = new CrearHabitacionRequest();
        request.setNumero("101");

        when(habitacionRepository.existsByNumero("101")).thenReturn(true);

        assertThatThrownBy(() -> habitacionService.crear(request))
                .isInstanceOf(HabitacionNumeroExistenteException.class);

        verify(habitacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("obtenerPorId() - debe retornar habitación existente")
    void debeRetornarHabitacion() {
        when(habitacionRepository.findByIdAndActivaTrue(1L)).thenReturn(Optional.of(habitacion));

        HabitacionResponse response = habitacionService.obtenerPorId(1L);

        assertThat(response).isNotNull();
        assertThat(response.getNumero()).isEqualTo("101");
    }

    @Test
    @DisplayName("obtenerPorId() - debe lanzar excepción si no existe")
    void debeLanzarExcepcionNoExiste() {
        when(habitacionRepository.findByIdAndActivaTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitacionService.obtenerPorId(99L))
                .isInstanceOf(HabitacionNotFoundException.class);
    }

    @Test
    @DisplayName("cambiarEstado() - debe cambiar estado y publicar evento")
    void debeCambiarEstadoYPublicarEvento() {
        when(habitacionRepository.findByIdAndActivaTrue(1L)).thenReturn(Optional.of(habitacion));
        when(habitacionRepository.save(any())).thenReturn(habitacion);

        habitacionService.cambiarEstado(1L, EstadoHabitacion.OCUPADA);

        assertThat(habitacion.getEstado()).isEqualTo(EstadoHabitacion.OCUPADA);
        verify(eventPublisher).publicarCambioEstado(eq(1L), eq("DISPONIBLE"), eq("OCUPADA"));
    }

    @Test
    @DisplayName("listarDisponibles() - debe retornar habitaciones disponibles")
    void debeListarDisponibles() {
        when(habitacionRepository.findByEstadoAndActivaTrue(EstadoHabitacion.DISPONIBLE))
                .thenReturn(List.of(habitacion));

        List<HabitacionResponse> result = habitacionService.listarDisponibles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEstado()).isEqualTo(EstadoHabitacion.DISPONIBLE);
    }

    @Test
    @DisplayName("desactivar() - debe marcar como inactiva")
    void debeDesactivarHabitacion() {
        when(habitacionRepository.findByIdAndActivaTrue(1L)).thenReturn(Optional.of(habitacion));
        when(habitacionRepository.save(any())).thenReturn(habitacion);

        habitacionService.desactivar(1L);

        assertThat(habitacion.getActiva()).isFalse();
        verify(habitacionRepository).save(habitacion);
    }
}
