package com.atlassian.migration.app.zephyr.jira.model;

import java.util.List;
import java.util.Objects;

public class JiraIssueFieldResponse {
    public JiraIssueTypeResponse issuetype;
    public String summary;
    public String description;
    public List<String> labels;
    public JiraReporterResponse reporter;

    public JiraIssueStatusResponse status;
    public List<IssueLink> issuelinks;
    public List<JiraIssueComponent> components;
    public JiraIssuePriority priority;
    public List<Attachment> attachment = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JiraIssueFieldResponse that = (JiraIssueFieldResponse) o;
        return Objects.equals(issuetype, that.issuetype)
                && Objects.equals(summary, that.summary)
                && Objects.equals(description, that.description)
                && Objects.equals(labels, that.labels)
                && Objects.equals(reporter, that.reporter)
                && Objects.equals(status, that.status)
                && Objects.equals(issuelinks, that.issuelinks)
                && Objects.equals(components, that.components)
                && Objects.equals(priority, that.priority)
                && Objects.equals(attachment, that.attachment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issuetype, summary, description, labels, reporter, status, issuelinks, components, priority, attachment);
    }
}
