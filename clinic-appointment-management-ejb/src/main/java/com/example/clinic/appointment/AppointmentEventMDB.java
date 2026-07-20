package com.example.clinic.appointment;

import com.example.clinic.common.AuditEntry;
import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import java.util.logging.Level;
import java.util.logging.Logger;

@JMSDestinationDefinition(
        name = "java:jboss/exported/jms/queue/AppointmentEvents",
        interfaceName = "jakarta.jms.Queue",
        description = "Queue for appointment lifecycle events"
)
@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/exported/jms/queue/AppointmentEvents")
        }
)
public class AppointmentEventMDB implements MessageListener {

    private static final Logger LOG = Logger.getLogger(AppointmentEventMDB.class.getName());
    private final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            String payload = textMessage.getText();
            AppointmentEvent event = jsonb.fromJson(payload, AppointmentEvent.class);

            LOG.info("[AppointmentEventMDB] Processing event: " + event.eventType()
                    + " for appointment " + event.appointmentId()
                    + " in clinic " + event.clinicId());

            AuditEntry audit = AuditEntry.of(
                    event.correlationId(),
                    event.clinicId(),
                    "system",
                    event.eventType(),
                    "Appointment",
                    event.appointmentId()
            );
            LOG.info("[AppointmentEventMDB] Audit: " + audit);

        } catch (JMSRuntimeException e) {
            LOG.log(Level.SEVERE, "[AppointmentEventMDB] Failed to process JMS message", e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[AppointmentEventMDB] Failed to process appointment event", e);
        }
    }
}
