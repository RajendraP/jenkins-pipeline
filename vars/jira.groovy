def call(String jiraComponent, String resultsFilePath, String[] labels=[],
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Jira') {
        jiraBaseUrl = getJiraBaseUrl()
        try {
            def testResults = new XmlParser().parse(resultsFilePath)
            testResults.testcase.each {
                test ->
                    if (test.failure) {
                        echo 'checking if Jira ticket already exist'
                        bugExists = jiraExists jiraComponent, test
                        if (bugExists) {
                            echo 'Jira ticket already exists'
                            bugExists.each {
                                jiraKey ->
                                    appendBugIdToResultsFile jiraKey, test
                            }
                        } else {
                            echo 'going to raise a Jira ticket'
                            raiseBug jiraComponent, fixVersions, issueType, labels, test
                        }
                    }
                }
            try {
                FileWriter writer = new FileWriter(resultsFilePath)
                new XmlNodePrinter(new PrintWriter(writer)).print(testResults)
                } catch (Exception ex){
                println "Failed to update results file: ${ex.message}"
            }
        }catch (FileNotFoundException ex){
            println "Unable to read test results files. File may be missing: ${ex.message}"
        }
    }
}

def jiraExists(String jiraComponent, failedTest){
    def jiraSummary = getJiraSummary(jiraComponent, failedTest)
    def escapedSingleQuoreJiraSummary = jiraSummary.replace("'", "\\'")

    escapedSingleQuoreJiraSummary =  "\"${escapedSingleQuoreJiraSummary}\""

    jql_string = "summary~${escapedSingleQuoreJiraSummary} AND status != Done"

    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try{
                def searchResults = jiraJqlSearch jql: jql_string
                jiraKeys = []
                def issues = searchResults.data.issues
                for (i = 0; i <issues.size(); i++) {
                    try{
                        def issue = jiraGetIssue idOrKey: issues[i].key
                        def bugSummary = issue.data.fields.summary
                        if (jiraSummary == bugSummary){
                            echo 'Duplicate bug found..'
                            jiraKeys<<issues[i].key
                        }
                    } catch (Exception ex){
                        println "failed to get details from jiraGetIssue step: ${ex.message}"
                    }
                }
                return jiraKeys
            } catch (Exception ex){
                println "failed to get details from JiraJqlSearch step: ${ex.message}"
            }
        }
    } catch(Exception ex){
        println "failed to connect to Jira: ${ex.message}"
    }
}

def getJiraBaseUrl(){
    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try{
                def serverInfo = jiraGetServerInfo()
                return serverInfo.data.baseUrl
            } catch (Exception ex){
                println "Failed to get Jira Server Info: ${ex.message}"
            }
        }
    } catch (Exception ex) {
        println "failed to connect to Jira: ${ex.message}"
    }
}

def raiseBug(String jiraComponent, String fixVersions, String issueType, String[] labels=[], test) {
    try {
        withEnv(['JIRA_SITE=LOCAL']) {
            try {
                summary = getJiraSummary(jiraComponent, test)
                description = test.failure.text()
                jiraIssue = [fields: [
                        project: [id: '16941'],
                        summary    : summary,
                        description: description,
                        components : [[name: jiraComponent]],
                        fixVersions: [[name: fixVersions]],
                        issuetype  : [name: issueType],
                        labels     : labels]]

                response = jiraNewIssue issue: jiraIssue
                appendBugIdToResultsFile(response.data.key, test)
            } catch (Exception ex) {
                println "faild to raise jira ticket: ${ex.message}"
            }
        }
    } catch(Exception ex){
        println "failed to connect to Jira: ${ex.message}"
    }
}

def getJiraSummary(String jiraComponent, failedTest){
    summary = failedTest.@name
    message = failedTest.failure.@'message'[0].split('   ')[0]
    def jiraSummary = jiraComponent + ': ' + summary + ': ' + message
    jiraSummary= jiraSummary.take(240)
    return  jiraSummary
}

def appendBugIdToResultsFile(jiraKey, test){
    jiraLink = jiraBaseUrl + '/browse/' + jiraKey
    println jiraLink
    test.failure.@'message' = test.failure.@'message'[0] + '\n' + jiraLink
}