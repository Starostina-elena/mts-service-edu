package ru.aigul.mts_service.jca;

public interface TaigaConnection extends AutoCloseable {
    void createIssue(String subject, String description);
    void updateIssueStatus(Long issueId, String status);

    @Override
    void close();
}
