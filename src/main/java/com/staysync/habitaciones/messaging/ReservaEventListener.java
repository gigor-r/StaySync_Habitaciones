package com.staysync.habitaciones.messaging;

import com.staysync.habitaciones.model.Habitacion.EstadoHabitacion;
import com.staysync.habitaciones.service.HabitacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservaEventListener {

    private final HabitacionService habitacionService;

    @RabbitListener(queues = "q.habitacion.estado")
    public void onReservaCheckin(Map<String, Object> evento) {
        try {
            String routingKey = (String) evento.getOrDefault("routingKey", "");
            Long habitacionId = Long.valueOf(evento.get("habitacionId").toString());

            if (routingKey.contains("checkin")) {
                habitacionService.cambiarEstado(habitacionId, EstadoHabitacion.OCUPADA);
                log.info("Habitación {} marcada como OCUPADA por check-in", habitacionId);
            } else if (routingKey.contains("checkout")) {
                habitacionService.cambiarEstado(habitacionId, EstadoHabitacion.DISPONIBLE);
                log.info("Habitación {} marcada como DISPONIBLE por check-out", habitacionId);
            }
        } catch (Exception e) {
            log.error("Error procesando evento de reserva en habitaciones: {}", e.getMessage());
        }
    }
}
