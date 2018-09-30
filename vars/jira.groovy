def call(String jiraprojectName, String jiraComponent, String resultsfilePath,
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    parseTestResultXML resultsfilePath
//    stage(name: 'Jira') {
//        withEnv(['JIRA_SITE=LOCAL']) {
            jira_issues = parseTestResultXML resultsfilePath
//            if (jira_issues) {
//                // query jira & find if any duplicates, if dup, skip if not continue
//                jira_issues.each {
//                    issue ->
//                        def jiraIssue =
//                                [fields:
//                                         [project    : [id: '16941'],
//                                          summary    : issue.summary,
//                                          description: issue.details,
//                                          components : [[name: jiraComponent]],
//                                          fixVersions: [[name: fixVersions]],
//                                          issuetype  : [name: issueType]]]
//                        response = jiraNewIssue issue: jiraIssue
//                        echo response.successful.toString()
//                        echo response.data.toString()
//                        println issue.summary
//                        println issue.details
//                        println jiraComponent
//                        println jiraprojectName
//                        println issueType
//                        println fixVersions
//
//                }
//
//            }
//            else{
//                println "no issues found"
//            }
//        }
    }
//}