package kgu.developers.domain.auth.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenJpaEntity {

    // 회원당 한 행. 새로 발급하면 덮어써지므로 이전 토큰은 폐기된다.
    @Id
    @Column(length = 16)
    private String studentNumber;

    @Column(nullable = false, length = 512)
    private String token;

    public RefreshTokenJpaEntity(String studentNumber, String token) {
        this.studentNumber = studentNumber;
        this.token = token;
    }
}
