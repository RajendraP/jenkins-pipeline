def call(String jiraprojectName, String jiraComponent, String resultsfilePath,
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Jira') {
        withEnv(['JIRA_SITE=LOCAL']) {
            jira_issues = parseTestResultXML resultsfilePath
            if (jira_issues) {

                jira_issues.each {
                    issue ->
                        // query jira & find if any duplicates, if dup, skip if not continue

                        bugExists = jiraExists(issue)
                        if (bugExists) {
                            echo 'not going to raise jira as, jira already exists'
//                            echo bugExists
                        } else {
//                            def jiraIssue =
//                                    [fields:
//                                             [project    : [id: '16941'],
//                                              summary    : issue.summary,
//                                              description: issue.details,
//                                              components : [[name: jiraComponent]],
//                                              fixVersions: [[name: fixVersions]],
//                                              issuetype  : [name: issueType]]]
//                            response = jiraNewIssue issue: jiraIssue
//                            echo response.successful.toString()
//                            echo response.data.toString()
//                            println issue.summary
//                            println issue.details
//                            println jiraComponent
//                            println jiraprojectName
//                            println issueType
//                            println fixVersions

                            echo 'going to raise a jira'

                        }
                }

            }
            else{
                echo 'There are no test failures..'
            }
        }
    }
}


def jiraExists(issue){
    summary = issue.summary
    description = issue.details
    details = ((description.split('\\n')[-1]))
    details = (record.replace('/', '\\u002f')).split(' ')[0]
    echo summary
    echo details

    def jql_str = "PROJECT = IPF AND summary~${summary} AND description~${details}"
    echo jql_str

    node {
            withEnv(['JIRA_SITE=LOCAL']) {
                def searchResults = jiraJqlSearch jql: jql_str
                def jiraKeys = []
                def issues = searchResults.data.issues
                for (i = 0; i <issues.size(); i++) {
                    echo issues[i].key
                    jiraKeys<< issues[i].key
                }
                return jiraKeys
            }
        }
    }
