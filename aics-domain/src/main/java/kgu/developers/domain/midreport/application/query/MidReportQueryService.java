package kgu.developers.domain.midreport.application.query;

import kgu.developers.domain.midreport.domain.MidReport;
import kgu.developers.domain.midreport.domain.MidReportRepository;
import kgu.developers.domain.midreport.exception.MidReportNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MidReportQueryService {
    private final MidReportRepository midReportRepository;

    public MidReport getById(Long id) {
        return midReportRepository.findById(id).orElseThrow(MidReportNotFoundException::new);
    }
}
