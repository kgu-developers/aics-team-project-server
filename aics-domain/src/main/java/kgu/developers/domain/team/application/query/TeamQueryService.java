package kgu.developers.domain.team.application.query;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.section.domain.SectionDetail;
import kgu.developers.domain.section.domain.SectionRepository;
import kgu.developers.domain.section.exception.SectionNotFoundException;
import kgu.developers.domain.team.domain.Team;
import kgu.developers.domain.team.domain.TeamRepository;
import kgu.developers.domain.team.exception.TeamNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamQueryService {
    private final TeamRepository teamRepository;
    private final SectionRepository sectionRepository;

    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(TeamNotFoundException::new);
    }

    public void validateContactVisible(Long teamId) {
        Team team = getTeamById(teamId);
        SectionDetail sectionDetail = sectionRepository.findById(team.getSectionId())
                .orElseThrow(SectionNotFoundException::new);

        sectionDetail.section().validateContactVisible(LocalDateTime.now());
    }

    public List<Team> getTeamsBySectionId(Long sectionId) {
        validateSectionExists(sectionId);

        return teamRepository.findAllBySectionId(sectionId);
    }

    public void validateSectionExists(Long sectionId) {
        if (sectionRepository.findById(sectionId).isEmpty()) {
            throw new SectionNotFoundException();
        }
    }
}
