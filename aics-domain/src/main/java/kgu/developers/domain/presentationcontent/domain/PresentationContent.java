package kgu.developers.domain.presentationcontent.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class PresentationContent {
    private Long id;
    private Long submissionId;
    private String introText;
    private JsonNode features;
    private JsonNode screens;
    private String youtubeUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PresentationContent create(
            Long submissionId, String introText, JsonNode features, JsonNode screens, String youtubeUrl) {
        return PresentationContent.builder()
                .submissionId(submissionId)
                .introText(introText)
                .features(features)
                .screens(screens)
                .youtubeUrl(youtubeUrl)
                .build();
    }

    public void update(String introText, JsonNode features, JsonNode screens, String youtubeUrl) {
        this.introText = introText;
        this.features = features;
        this.screens = screens;
        this.youtubeUrl = youtubeUrl;
    }
}
