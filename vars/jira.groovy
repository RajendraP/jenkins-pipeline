def call(String jiraComponent, String resultsFilePath, String[] labels=[],
         String fixVersions='pipeline_fixes', String projectId='16941', String issueType='Bug') {
    jiraBaseUrl = getJiraBaseUrl()
    try {
        def testResults = new XmlParser().parse(resultsFilePath)
        testResults.testcase.each {
            test ->
                if (test.failure) {
                    echo 'checking if Jira ticket already exists'
                    jiraKeysList = isBugAlreadyExists jiraComponent, test
                    if (jiraKeysList) {
                        echo 'Jira ticket already exists'
                        appendBugIdToTestFailureMessage jiraKeysList, test
                        addCommentInExistingBugs jiraKeysList, test

                    } else {
                        echo 'going to raise a Jira ticket'
                        raiseBug projectId, jiraComponent, issueType, fixVersions, labels, test
                    }
                }
            }
        try {
            FileWriter writer = new FileWriter(resultsFilePath)
            new XmlNodePrinter(new PrintWriter(writer)).print(testResults)
        } catch (Exception ex){
            println "Failed to update results file: ${ex.message}"
            throw ex
        }
    } catch (FileNotFoundException ex){
        println "Unable to read test results files. File may be missing: ${ex.message}"
        throw ex
    }
}


def isBugAlreadyExists(String jiraComponent, failedTest){
    jiraSummary = getJiraSummary(jiraComponent, failedTest)
    jql_string = "summary~${jiraSummary} AND status != Done"
    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try{
                def searchResults = jiraJqlSearch jql: jql_string
                jiraKeys = []
                def issues = searchResults.data.issues
                for (i = 0; i < issues.size(); i++) {
                    jiraKeys << issues[i].key
                }
                return jiraKeys
            } catch (Exception ex){
                println "failed to get details from JiraJqlSearch step: ${ex.message}"
                throw ex
            }
        }
    } catch(Exception ex){
        println "failed to connect to Jira: ${ex.message}"
        throw ex
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
                throw ex
            }
        }
    } catch (Exception ex) {
        println "failed to connect to Jira: ${ex.message}"
        throw ex
    }
}

def raiseBug(String projectId, String jiraComponent, String issueType, String fixVersions, String[] labels, test) {
    try {
        withEnv(['JIRA_SITE=LOCAL']) {
            try {
                defaultLabel = 'PipelineBug'
                labels = labels.plus(defaultLabel)
                summary = getJiraSummary(jiraComponent, test)
                description = test.failure.text()
                jiraIssue = [fields: [
                        project: [id: projectId],
                        summary    : summary,
                        description: description,
                        components : [[name: jiraComponent]],
                        fixVersions: [[name: fixVersions]],
                        issuetype  : [name: issueType],
                        labels     : labels]]

                response = jiraNewIssue issue: jiraIssue
                appendBugIdToTestFailureMessage([response.data.key], test)
            } catch (Exception ex) {
                println "faild to raise jira ticket: ${ex.message}"
                throw ex
            }
        }
    } catch(Exception ex){
        println "failed to connect to Jira: ${ex.message}"
        throw ex
    }
}

def getJiraSummary(String jiraComponent, failedTest){
    summary = jiraComponent + ":" + failedTest.@name
    return summary.take(254)
}

def appendBugIdToTestFailureMessage(jiraKeysList, test){
    String jiraLink = ""
    jiraKeysList.each{
        jiraKey-> jiraLink += jiraBaseUrl + '/browse/' + jiraKey + '\n'
    }
    println jiraLink
    test.failure.@'message' = test.failure.@'message'[0] + '\n' + jiraLink
}

def addCommentInExistingBugs(jiraKeysList, test){
    jiraKeysList.each{
        jiraKey->
            try {
                withEnv(['JIRA_SITE=LOCAL']) {
                    try {
                        jiraAddComment idOrKey: jiraKey, comment: test.failure.text()
                    } catch (Exception ex){
                        println "failed to add comment in Jira: ${ex.message}"
                        throw ex
                    }
                }
            } catch(Exception ex){
                println "failed to connect to Jira: ${ex.message}"
                throw ex
            }
    }
}
