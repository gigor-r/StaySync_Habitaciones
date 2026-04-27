package com.staysync.habitaciones.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class HabitacionEventPublisher {

    private static final String EXCHANGE = "staysync.habitaciones.exchange";

    private final RabbitTemplate rabbitTemplate;

    public void publicarCambioEstado(Long habitacionId, String estadoAnterior, String estadoNuevo) {
        Map<String, Object> evento = Map.of(
                "habitacionId",   habitacionId,
                "estadoAnterior", estadoAnterior,
                "estadoNuevo",    estadoNuevo,
                "timestamp",      LocalDateTime.now().toString()
        );
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "habitacion.estado.cambiado", evento);
            log.info("Evento publicado: habitacion.estado.cambiado para habitación {}", habitacionId);
        } catch (Exception e) {
            log.error("Error publicando evento de habitación: {}", e.getMessage());
        }
    }

    public void publicarDisponibilidadActualizada(Long habitacionId, String fecha, Boolean disponible, java.math.BigDecimal precio) {
        Map<String, Object> evento = Map.of(
                "habitacionId", habitacionId,
                "fecha",        fecha,
                "disponible",   disponible,
                "precio",       precio
        );
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, "habitacion.disponibilidad.actualizada", evento);
        } catch (Exception e) {
            log.error("Error publicando disponibilidad: {}", e.getMessage());
        }
    }
}
