package kgu.developers.common.exception;

import java.util.function.Supplier;

import org.springframework.dao.OptimisticLockingFailureException;

public final class OptimisticLocks {
  private OptimisticLocks() {
  }

  public static <T> T translate(Supplier<T> operation, Supplier<? extends RuntimeException> onConflict) {
    try {
      return operation.get();
    } catch (OptimisticLockingFailureException e) {
      throw onConflict.get();
    }
  }
}
