def call(String jiraprojectName, String jiraComponent, String resultsfilePath,
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Jira') {
        withEnv(['JIRA_SITE=LOCAL']) {
            jira_issues = parseTestResultXML resultsfilePath
            if (jira_issues) {

                jira_issues.each {
                    issue ->
                        // query jira & find if any duplicates, if dup, skip if not continue
                        def bugExists = []
                        bugExists = jiraExists(issue)
                        if (bugExists) {
                            echo 'not going to raise jira as, jira already exists'
//                            println bugExists
//                            echo bugExists
                            jiraBaseUrl =  getJiraBaseUrl()
                            bugExists.each{
                                jira ->
                                println (jiraBaseUrl + '/browse/' + jira)
                            }


                        } else {
                            jiraBaseUrl =  getJiraBaseUrl()
                            echo 'going to raise a jira'
                            def jiraIssue =
                                    [fields:
                                             [project    : [id: '16941'],
                                              summary    : issue.summary,
                                              description: issue.details,
                                              components : [[name: jiraComponent]],
                                              fixVersions: [[name: fixVersions]],
                                              issuetype  : [name: issueType]]]
                            response = jiraNewIssue issue: jiraIssue
                            echo response.successful.toString()
//                            println (jiraBaseUrl + '/browse/' + jira)


//                            echo response.data.toString()
//                            println issue.summary
//                            println issue.details
//                            println jiraComponent
//                            println jiraprojectName
//                            println issueType
//                            println fixVersions
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
//                    echo issues[i].key
                    jiraKeys<< issues[i].key
//                    echo issues[i].key
//                    jiraBaseUrl =  getJiraBaseUrl()
//                    println (jiraBaseUrl + '/browse' + issues[i].key)
                }
                return jiraKeys
            }
    }
}


def getJiraBaseUrl(){
    node {
            withEnv(['JIRA_SITE=LOCAL']) {
                def serverInfo = jiraGetServerInfo()
//                echo serverInfo.data.baseUrl
                return serverInfo.data.baseUrl
            }
    }
}
