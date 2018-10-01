def call(String jiraprojectName, String jiraComponent, String resultsfilePath,
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Jira') {
        withEnv(['JIRA_SITE=LOCAL']) {
            failures = parseTestResultXML resultsfilePath
            if (failures) {
                jiraBaseUrl =  getJiraBaseUrl()
                failures.each {
                    issue ->
                        // query jira & find if any duplicates, if dup, skip if not continue
                        def bugExists = []
                        bugExists = jiraExists(issue)
                        if (bugExists) {
                            echo 'Jira ticket already exists'
//                            jiraBaseUrl =  getJiraBaseUrl()
                            bugExists.each{
                                jira ->
                                println (jiraBaseUrl + '/browse/' + jira)
                            }


                        } else {
//                            jiraBaseUrl =  getJiraBaseUrl()
                            echo 'Going to raise a Jira ticket'
                            def jiraIssue =
                                    [fields:
                                             [project    : [id: '16941'],
                                              summary    : issue.summary,
                                              description: issue.details,
                                              components : [[name: jiraComponent]],
                                              fixVersions: [[name: fixVersions]],
                                              issuetype  : [name: issueType]]]
                            response = jiraNewIssue issue: jiraIssue
//                            echo response.successful.toString()
//                            echo response.data.key
                            println (jiraBaseUrl + '/browse/' + response.data.key)
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
    details = (description.split('\\n')[-1]).replace('/', '\\u002f').split(' ')[0]

    def jql_str = "PROJECT = IPF AND summary~${summary} AND description~${details} AND status != Done"
    echo jql_str

    node {
            withEnv(['JIRA_SITE=LOCAL']) {
                def searchResults = jiraJqlSearch jql: jql_str
                def jiraKeys = []
                def issues = searchResults.data.issues
                for (i = 0; i <issues.size(); i++) {
                    jiraKeys<< issues[i].key
                }
                return jiraKeys
            }
    }
}


def getJiraBaseUrl(){
    node {
            withEnv(['JIRA_SITE=LOCAL']) {
                def serverInfo = jiraGetServerInfo()
                return serverInfo.data.baseUrl
            }
    }
}
