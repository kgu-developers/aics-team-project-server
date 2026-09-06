package preSurveyResponse.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.domain.preSurveyResponse.domain.PreferredPeerStatus;

class PreSurveyResponseSchemaSqlTest {

	private static final Pattern COMMENT = Pattern.compile("--[^\\n]*");
	private static final Pattern UNIQUE_INDEX = Pattern.compile(
			"CREATE\\s+UNIQUE\\s+INDEX[^;]*;", Pattern.CASE_INSENSITIVE);
	// 컬럼 순서(user_id, section_id)는 유니크 인덱스 의미상 바뀌어도 상관없으므로 둘 다 허용한다
	private static final Pattern ACTIVE_USER_SECTION_INDEX = Pattern.compile(
			"CREATE\\s+UNIQUE\\s+INDEX\\s+(IF\\s+NOT\\s+EXISTS\\s+)?uk_pre_survey_response_active_user_section\\s+"
					+ "ON\\s+\"?pre_survey_response\"?\\s*\\(\\s*"
					+ "(user_id\\s*,\\s*section_id|section_id\\s*,\\s*user_id)\\s*\\)\\s*"
					+ "WHERE\\s+deleted_at\\s+IS\\s+NULL\\s*;",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PREFERRED_PEER_STATUS_CHECK = Pattern.compile(
			"CHECK\\s*\\(\\s*preferred_peer_status\\s+IN\\s*\\([^)]*\\)\\s*\\)",
			Pattern.CASE_INSENSITIVE);
	// 자기 자신 지목 금지. NULL 을 통과시키려면 <> 가 아니라 IS DISTINCT FROM 이어야 한다.
	private static final Pattern PREFERRED_PEER_NOT_SELF_CHECK = Pattern.compile(
			"CHECK\\s*\\(\\s*preferred_peer_user_id\\s+IS\\s+DISTINCT\\s+FROM\\s+user_id\\s*\\)",
			Pattern.CASE_INSENSITIVE);
	// 지목 대상과 상태의 NULL 여부가 항상 같아야 한다
	private static final Pattern PREFERRED_PEER_PAIRED_CHECK = Pattern.compile(
			"CHECK\\s*\\(\\s*\\(\\s*preferred_peer_user_id\\s+IS\\s+NULL\\s*\\)\\s*=\\s*"
					+ "\\(\\s*preferred_peer_status\\s+IS\\s+NULL\\s*\\)\\s*\\)",
			Pattern.CASE_INSENSITIVE);
	// CREATE UNIQUE INDEX 말고도 이 두 형태로 무조건 유니크 제약이 몰래 들어올 수 있다
	private static final Pattern ALTER_ADD_UNIQUE = Pattern.compile(
			"ALTER\\s+TABLE[^;]*ADD\\s+CONSTRAINT[^;]*UNIQUE[^;]*;", Pattern.CASE_INSENSITIVE);
	private static final Pattern INLINE_UNIQUE_IN_CREATE_TABLE = Pattern.compile(
			"CREATE\\s+TABLE[^;]*?\\bUNIQUE\\s*\\([^)]*\\)[^;]*;", Pattern.CASE_INSENSITIVE);

	@Test
	@DisplayName("DDL은 지목 상태를 PreferredPeerStatus 세 값으로만 제한한다")
	void ddlConstrainsPreferredPeerStatus() {
		String ddl = readDdl();

		assertThat(PREFERRED_PEER_STATUS_CHECK.matcher(ddl).find())
				.as("preferred_peer_status CHECK 제약이 사라졌거나 정의가 바뀌었다: %s", ddl)
				.isTrue();

		// enum 에 값을 추가하면 DDL 도 같이 고쳐야 한다는 걸 이 테스트가 잡는다
		for (PreferredPeerStatus status : PreferredPeerStatus.values()) {
			assertThat(ddl).containsIgnoringCase("'" + status.name() + "'");
		}
	}

	@Test
	@DisplayName("DDL은 자기 자신 지목과 대상·상태 불일치를 CHECK 제약으로 막는다")
	void ddlConstrainsPreferredPeerInvariants() {
		String ddl = readDdl();

		assertThat(PREFERRED_PEER_NOT_SELF_CHECK.matcher(ddl).find())
				.as("자기 자신 지목을 막는 CHECK 제약이 사라졌거나 정의가 바뀌었다: %s", ddl)
				.isTrue();
		assertThat(PREFERRED_PEER_PAIRED_CHECK.matcher(ddl).find())
				.as("지목 대상·상태를 짝지어 두는 CHECK 제약이 사라졌거나 정의가 바뀌었다: %s", ddl)
				.isTrue();
	}

	@Test
	@DisplayName("DDL은 살아있는 응답만 (학번, 분반)당 하나로 막는 부분 유니크 인덱스를 선언한다")
	void ddlDeclaresPartialUniqueIndex() {
		String ddl = readDdl();

		assertThat(ACTIVE_USER_SECTION_INDEX.matcher(ddl).find())
				.as("uk_pre_survey_response_active_user_section 부분 유니크 인덱스가 사라졌거나 정의가 바뀌었다: %s", ddl)
				.isTrue();
	}

	@Test
	@DisplayName("DDL에는 소프트 삭제된 응답의 재제출까지 막는 무조건 유니크 인덱스가 없다")
	void ddlHasNoUnconditionalUniqueIndexOnUserAndSection() {
		String ddl = readDdl();

		List<String> statements = new ArrayList<>();
		Matcher matcher = UNIQUE_INDEX.matcher(ddl);
		while (matcher.find()) {
			String statement = matcher.group();
			if (statement.toLowerCase().contains("user_id") && statement.toLowerCase().contains("section_id")) {
				statements.add(statement);
			}
		}

		assertThat(statements).hasSize(1);
		assertThat(statements.get(0)).containsIgnoringCase("where deleted_at is null");

		assertThat(mentionsBothColumns(ALTER_ADD_UNIQUE, ddl))
				.as("ALTER TABLE ... ADD CONSTRAINT ... UNIQUE 형태로 무조건 유니크 제약이 추가됐다: %s", ddl)
				.isFalse();
		assertThat(mentionsBothColumns(INLINE_UNIQUE_IN_CREATE_TABLE, ddl))
				.as("CREATE TABLE 안의 인라인 UNIQUE(...)로 무조건 유니크 제약이 추가됐다: %s", ddl)
				.isFalse();
	}

	private boolean mentionsBothColumns(Pattern pattern, String ddl) {
		Matcher matcher = pattern.matcher(ddl);
		while (matcher.find()) {
			String statement = matcher.group().toLowerCase();
			if (statement.contains("user_id") && statement.contains("section_id")) {
				return true;
			}
		}
		return false;
	}

	private String readDdl() {
		Path ddlPath = findDdlPath();
		try {
			String raw = Files.readString(ddlPath, StandardCharsets.UTF_8);
			return COMMENT.matcher(raw).replaceAll(" ").replaceAll("\\s+", " ");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Path findDdlPath() {
		Path current = Paths.get("").toAbsolutePath();
		while (current != null) {
			Path candidate = current.resolve("database").resolve("pre_survey_response.sql");
			if (Files.exists(candidate)) {
				return candidate;
			}
			current = current.getParent();
		}
		throw new IllegalStateException("database/pre_survey_response.sql 을 찾지 못했다");
	}
}
