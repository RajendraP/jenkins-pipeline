def call(String jiraprojectName, String jiraComponent, String resultsfilePath, String issueType='Bug'){
    stage(name: 'Jira') {
        withEnv(['JIRA_SITE=LOCAL']) {
            jira_issues = parseTestResultXML resultsfilePath
            // query jira & find if any duplicates, if dup, skip if not continue
            jira_issues.each{
                issue ->
                    def jiraIssue =
                            [fields:
                                     [project: [name: jiraprojectName],
                                      summary: issue.summary,
                                      description: issue.details,
                                      components: [[name: jiraComponent]],
                                      issuetype: [name: issueType]]]
                    response = jiraNewIssue issue: jiraIssue
                    echo response.successful.toString()
                    echo response.data.toString()
            }

        }
    }
}

