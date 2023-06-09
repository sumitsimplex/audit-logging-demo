package ie.rsa.auditlogging.demo.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.event.AbstractAuthorizationEvent;
import org.springframework.security.access.event.AuthorizationFailureEvent;
import org.springframework.security.web.FilterInvocation;
import org.springframework.stereotype.Component;

@Component
public class LoggingAuditEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAuditEventListener.class);

  @EventListener
  public void on(AuditApplicationEvent event) {
    Map<String, String> backup = MDC.getCopyOfContextMap();
    MDC.put("event.type", event.getAuditEvent().getType());
    MDC.put("event.principal", event.getAuditEvent().getPrincipal());

    LOGGER.info("An Audit Event was received: {}", event);

    if (backup != null) {
      MDC.setContextMap(backup);
    }
  }

  /*
  or just listen for an `AuthorizationFailureEvent`
   */
  @EventListener
  public void on(AbstractAuthorizationEvent abstractEvent) {
    Map<String, String> backup = MDC.getCopyOfContextMap();
    if (abstractEvent instanceof AuthorizationFailureEvent) {
      AuthorizationFailureEvent event = (AuthorizationFailureEvent) abstractEvent;
      MDC.put("event.type", "AUTHORIZATION_FAILURE_EVENT");
      MDC.put("event.principal", event.getAuthentication().getName());
      FilterInvocation filterInvocation = (FilterInvocation) event.getSource();
      MDC.put("source.requestUrl", filterInvocation.getRequestUrl());
    }
    // and other checks for other subclasses
    LOGGER.info("An AuthorizationFailureEvent was received: {}", abstractEvent);

    if (backup != null) {
      MDC.setContextMap(backup);
    }
  }
}
