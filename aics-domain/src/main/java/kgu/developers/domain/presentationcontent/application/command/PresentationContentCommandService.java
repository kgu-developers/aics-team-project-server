package kgu.developers.domain.presentationcontent.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kgu.developers.domain.presentationcontent.domain.PresentationContent;
import kgu.developers.domain.presentationcontent.domain.PresentationContentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PresentationContentCommandService {
    private final PresentationContentRepository presentationContentRepository;

    public PresentationContent upsert(
            Long submissionId, String introText, JsonNode features, JsonNode screens, String youtubeUrl) {
        PresentationContent existing = presentationContentRepository.findBySubmissionId(submissionId).orElse(null);
        if (existing != null) {
            existing.update(introText, features, screens, youtubeUrl);
            return presentationContentRepository.save(existing);
        }
        return presentationContentRepository.save(
                PresentationContent.create(submissionId, introText, features, screens, youtubeUrl));
    }
}
