package ie.rsa.auditlogging.demo.config;

import ie.rsa.auditlogging.demo.audit.HttpRequestReceivedEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
  // UUIDv4, matching either case, but depends on what format you want to use
  private static final Pattern UUID_PATTERN =
      Pattern.compile("([a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){4}[a-fA-F0-9]{8})");
  @Value("${correlation.key}")
  public String correlationId;

  private final ApplicationEventPublisher publisher;

  public CorrelationIdFilter(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String correlationId = request.getHeader("correlation-id");
    if (null == correlationId || !UUID_PATTERN.matcher(correlationId).matches()) {
      // only allow UUIDs, if it's not valid according to our contract, allow it to be rewritten
      // alternatively, we would reject the request with an HTTP 400 Bad Request, as a client
      // hasn't fulfilled the contract
      correlationId = UUID.randomUUID().toString();
    }
    publisher.publishEvent(new HttpRequestReceivedEvent(request, correlationId));
    // make sure that the Mapped Diagnostic Context (MDC) has the `correlationId` so it can then
    // be populated in the logs
    try (MDC.MDCCloseable ignored = MDC.putCloseable(this.correlationId, correlationId)) {
      response.addHeader("correlation-id", correlationId);
      filterChain.doFilter(request, response);
    }
  }
}
