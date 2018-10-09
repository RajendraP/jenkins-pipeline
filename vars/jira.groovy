def call(String jiraComponent, String resultsFilePath, String[] labels=[],
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    jiraBaseUrl = getJiraBaseUrl()
    try {
        def testResults = new XmlParser().parse(resultsFilePath)
        testResults.testcase.each {
            test ->
                if (test.failure) {
                    echo 'checking if Jira ticket already exist'
                    jiraKeysList = jiraExists jiraComponent, test
                    if (jiraKeysList) {
                        echo 'Jira ticket already exists'
                        appendBugIdToTestFailureMessage jiraKeysList, test
                        addCommentInExistingBugs jiraKeysList, test

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
            throw ex
        }
    }catch (FileNotFoundException ex){
        println "Unable to read test results files. File may be missing: ${ex.message}"
        throw ex
    }
}


def jiraExists(String jiraComponent, failedTest){
    jiraSummary = getJiraSummary(jiraComponent, failedTest)
    jql_string = "summary~${jiraSummary} AND status != Done"
    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try{
                def searchResults = jiraJqlSearch jql: jql_string
                jiraKeys = []
                def issues = searchResults.data.issues
                for (i = 0; i <issues.size(); i++) {
                    jiraKeys<<issues[i].key
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
    summary = failedTest.@name
    summary = jiraComponent + ":" + summary
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
                        } catch (Exception ex) {
                            println "failed to add comment in Jira: ${ex.message}"
                            throw ex
                        }
                    }
                }catch(Exception ex){
                    println "failed to connect to Jira: ${ex.message}"
                    throw ex
                }
    }
}


//def isNewFailure(jiraKey, test){
//    description = test.failure.@'message'[0]
//    description = description.split("\n").minus(
//            description.split("\n")[0],
//            description.split("\n")[1],
//            description.split("\n")[2]).join("\n")
//
//    bugDescription = ""
//    try {
//        withEnv(['JIRA_SITE=LOCAL']) {
//            def issue = jiraGetIssue idOrKey: jiraKey
//            bugDescription = issue.data.fields.description
//        }
//    }catch(Exception ex){
//        println "failed to connect to Jira: ${ex.message}"
//        throw ex
//    }
//    bugDescription = bugDescription.split("\n").minus(
//            bugDescription.split("\n")[0],
//            bugDescription.split("\n")[1],
//            bugDescription.split("\n")[2],
//            bugDescription.split("\n")[-1]).join("\n")
//    if (description != bugDescription)
//    {
//        return true
//    }
//    else{
//        return false
//    }
//}