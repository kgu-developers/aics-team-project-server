package kgu.developers.globalutils.logging;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.aspectj.lang.JoinPoint;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kgu.developers.globalutils.annotation.NoLogging;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoggingUtils {

  // startTime 속성이 없는 요청(인터셉터를 거치지 않은 경우)의 소요 시간
  private static final long UNKNOWN_DURATION = -1;

  // 이름에 아래 키워드가 포함된 필드는 값 대신 마스킹해 기록한다. @NoLogging으로 개별 지정도 가능하다.
  private static final List<String> SENSITIVE_FIELD_KEYWORDS = List.of(
      "password", "passwd", "secret", "token", "credential", "authorization", "apikey");
  private static final String MASKED = "****";

  public static List<String> getArguments(JoinPoint joinPoint) {
    return Arrays.stream(joinPoint.getArgs())
        .map(LoggingUtils::getObjectFields)
        .toList();
  }

  public static String getObjectFields(Object obj) {
    if (obj == null) {
      return "null";
    }
    Class<?> objClass = obj.getClass();
    StringJoiner result = new StringJoiner(", ", objClass.getSimpleName() + " {", "}");

    for (Field field : objClass.getDeclaredFields()) {
      if (field.isSynthetic()) {
        continue;
      }
      result.add(field.getName() + " = " + getFieldValue(obj, field));
    }
    return result.toString();
  }

  private static String getFieldValue(Object obj, Field field) {
    if (isSensitive(field)) {
      return MASKED;
    }

    try {
      field.setAccessible(true);
      return escapeLineBreaks(String.valueOf(field.get(obj)));
    } catch (IllegalAccessException | RuntimeException e) {
      return "ACCESS_DENIED";
    }
  }

  private static String escapeLineBreaks(String value) {
    return value.replace("\r", "\\r").replace("\n", "\\n");
  }

  private static boolean isSensitive(Field field) {
    if (field.isAnnotationPresent(NoLogging.class)) {
      return true;
    }
    String fieldName = field.getName().toLowerCase();
    return SENSITIVE_FIELD_KEYWORDS.stream().anyMatch(fieldName::contains);
  }

  public static String getParameterMessage(List<String> arguments) {
    if (arguments == null)
      return "";

    return String.join(" | ", arguments);
  }

  public static void logRequest() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = (authentication == null || Objects.equals(authentication.getName(), "anonymousUser")) ? "anonymous"
        : authentication.getName();
    String clientIp = HttpReqResUtils.getClientIpAddressIfServletRequestExist();

    log.info("[REQUEST] CLIENT IP : {} || USER ID : {}", clientIp, userId);
  }

  public static void logDuration(HttpServletRequest request, HttpServletResponse response, Exception ex) {
    String requestUrl = request.getRequestURI();
    String httpMethod = request.getMethod();
    int httpStatus = response.getStatus();

    Object startTime = request.getAttribute("startTime");
    long duration = (startTime == null) ? UNKNOWN_DURATION : System.currentTimeMillis() - (Long) startTime;

    if (ex == null) {
      log.info("[DURATION] ENDPOINT : {} {} || STATUS : {} || DURATION : {}ms", httpMethod, requestUrl,
          httpStatus, duration);
    } else {
      log.error("[DURATION] ENDPOINT : {} {} || STATUS : {} || DURATION : {}ms || EXCEPTION : {}",
              httpMethod, requestUrl, httpStatus, duration, escapeLineBreaks(String.valueOf(ex.getMessage())));
    }
  }
}
