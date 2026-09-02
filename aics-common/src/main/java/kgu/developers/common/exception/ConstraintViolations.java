package kgu.developers.common.exception;

import org.springframework.dao.DataIntegrityViolationException;

public final class ConstraintViolations {
  private ConstraintViolations() {
  }

  public static boolean violates(DataIntegrityViolationException e, String indexName) {
    String message = e.getMostSpecificCause().getMessage();
    return message != null && message.contains(indexName);
  }
}
