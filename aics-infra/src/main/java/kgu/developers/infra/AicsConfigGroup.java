package kgu.developers.infra;

import kgu.developers.infra.config.JpaAuditingConfig;
import kgu.developers.infra.config.JpaConfig;
import kgu.developers.infra.config.PropertiesConfig;
import kgu.developers.infra.config.RedisConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AicsConfigGroup {

	JPA(JpaConfig.class),
	JPA_AUDITING(JpaAuditingConfig.class),
	PROPERTIES(PropertiesConfig.class),
	REDIS(RedisConfig.class),
	;
	private final Class<? extends AicsConfig> configClass;
}
