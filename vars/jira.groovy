def call(String jiraComponent, String resultsfilePath, String[] labels=[],
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Jira') {
        try {
            withEnv(['JIRA_SITE=LOCAL']) {
                jiraBaseUrl = getJiraBaseUrl()
                try {
                    def xml = new XmlParser().parse(resultsfilePath)
                    xml.testcase.each {
                        test ->
                            if (test.failure) {
                                echo 'checking for existing bug'
                                def bugExists = []
                                bugExists = jiraExists jiraComponent, test
                                if (bugExists) {
                                    echo 'Jira ticket already exists'
                                    bugExists.each {
                                        jiraKey ->
                                            appendBugId(jiraBaseUrl, jiraKey, test)
                                    }
                                } else {
                                    echo 'Going to raise a Jira ticket'
                                    raiseBug jiraComponent, fixVersions, issueType, labels, jiraBaseUrl, test
                                }
                            }else {
                                echo 'There are no test failures..'
                            }
                    }
                    def writer = new FileWriter(resultsfilePath)
                    new XmlNodePrinter(new PrintWriter(writer)).print(xml)
                } catch (FileNotFoundException e){
                    echo 'Unable to read test results files. File may be missing'
                }
            }
        }catch(Exception ex){
            echo 'Failed to connect to Jira'
        }
    }
}


def jiraExists(jiraComponent, failedTest){
    jira_summary = getJiraSummary(jiraComponent, failedTest)
    def jira_summary_updated = jira_summary.replace("'", "\\'")

    jira_summary_updated =  "\"${jira_summary_updated}\""
    def jql_str = "summary~${jira_summary_updated} AND status != Done"

    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try{
                def searchResults = jiraJqlSearch jql: jql_str
                def jiraKeys = []
                def issues = searchResults.data.issues
                for (i = 0; i <issues.size(); i++) {
                    try{
                        def issue = jiraGetIssue idOrKey: issues[i].key
                        def bugSummary = issue.data.fields.summary
                        if (jira_summary == bugSummary){
                            echo 'Duplicate bug found..'
                            jiraKeys<<issues[i].key
                        }
                    }
                    catch (Exception ex){
                        echo 'failed to get details from jiraGetIssue'
                    }
                }
                return jiraKeys
            }
            catch (Exception ex){
                echo 'failed to get details from JiraJqlSearch'
            }
        }
    }catch(Exception ex){
        echo 'failed to connect to Jira'
    }
}

def getJiraBaseUrl(){
    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try{
                def serverInfo = jiraGetServerInfo()
                return serverInfo.data.baseUrl
            }
            catch (Exception ex){
                echo 'Failed to get Jira Server Info'
            }
        }
    }catch (Exception ex) {
        echo 'Failed to connect to Jira'
    }
}

def raiseBug(jiraComponent, fixVersions, issueType, labels, jiraBaseUrl, test) {
    try {
        withEnv(['JIRA_SITE=LOCAL']) {
            try {
                summary = getJiraSummary(jiraComponent, test)
                description = test.failure.text()

                def jiraIssue =
                        [fields:
                                 [project    : [id: '16941'],
                                  summary    : summary,
                                  description: description,
                                  components : [[name: jiraComponent]],
                                  fixVersions: [[name: fixVersions]],
                                  issuetype  : [name: issueType],
                                  labels     : labels]]

                response = jiraNewIssue issue: jiraIssue
                appendBugId(jiraBaseUrl, response.data.key, test)

            } catch (Exception ex) {
                echo 'Faild to raise jira ticket'
            }
        }
    } catch(Exception ex){
                echo 'Failed to connect to Jira'
    }
}

def getJiraSummary(jiraComponent, failedTest){
    summary = failedTest.@name
    message = failedTest.failure.@'message'[0].split('   ')[0]
    def jira_summary = jiraComponent + ': ' + summary + ': ' + message
    jira_summary= jira_summary.take(220)
    return  jira_summary
}

def appendBugId(jiraBaseUrl, jiraKey, test){
    jiraLink = jiraBaseUrl + '/browse/' + jiraKey
    test.failure.@'message' = test.failure.@'message'[0] + '\n' + jiraLink
}