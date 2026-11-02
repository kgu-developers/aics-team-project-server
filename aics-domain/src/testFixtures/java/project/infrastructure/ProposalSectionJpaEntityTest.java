package project.infrastructure;

import kgu.developers.domain.project.domain.ProposalSection;
import kgu.developers.domain.project.domain.ProposalSectionType;
import kgu.developers.domain.project.infrastructure.ProposalSectionJpaEntity;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProposalSectionJpaEntityTest {
    @Test
    void contentVersionSurvivesEntityMappingAndMetadataUpdates() {
        ProposalSection section = ProposalSection.builder().id(1L).projectId(10L)
            .type(ProposalSectionType.DATA).contentVersion(7L).build();
        section.assign("student");
        section.updateCompleted(true);
        ProposalSection persisted = ProposalSectionJpaEntity.toEntity(section).toDomain();
        assertThat(persisted.getContentVersion()).isEqualTo(7L);
        persisted.contentChanged();
        assertThat(persisted.getContentVersion()).isEqualTo(8L);
        assertThat(persisted.isCompleted()).isFalse();
    }
}
