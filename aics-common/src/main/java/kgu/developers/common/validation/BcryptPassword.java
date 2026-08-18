package kgu.developers.common.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * BCryptPasswordEncoder는 UTF-8 기준 72바이트를 넘는 비밀번호를 인코딩할 때 예외를 던진다.
 * {@code @Size}는 문자 수만 세므로 한글 등 멀티바이트 문자를 걸러내지 못한다.
 */
@Documented
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = BcryptPassword.Validator.class)
public @interface BcryptPassword {

	int MAX_BYTES = 72;

	String message() default "비밀번호는 UTF-8 기준 " + MAX_BYTES + "바이트를 넘을 수 없습니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<BcryptPassword, String> {

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return value == null || value.getBytes(UTF_8).length <= MAX_BYTES;
		}
	}
}
