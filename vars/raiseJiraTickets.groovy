import groovy.xml.XmlUtil

def call(String jiraComponent, String resultsFilePath, String[] labels=[],
         String fixVersions='pipeline_fixes', String projectId='16941', String issueType='Bug') {
    jiraBaseUrl = getJiraBaseUrl()
    try {
        def testResultXMLText = readFile resultsFilePath
        try {
            failedTests = getFailedTests(testResultXMLText)
            failedTests.each {
                testcase ->
                    echo 'checking if Jira ticket already exists'
                    jiraKeysList = doesBugAlreadyExist jiraComponent, testcase["name"]
                    if (jiraKeysList) {
                        echo 'Jira ticket already exists'
                        testcase["jiraKeys"] = jiraKeysList
                        addCommentInExistingBugs jiraKeysList, testcase["failure"]
                    } else {
                        echo 'going to raise a Jira ticket'
                        response = raiseBug projectId, jiraComponent, issueType, fixVersions, labels, testcase["name"], testcase["failure"]
                        testcase["jiraKeys"] = [response.data.key]
                    }
            }
            testResults_str = appendBugIdToTestFailureMessage failedTests, testResultXMLText
            try {
                sh "sudo chmod 666 ${resultsFilePath}"
                writeFile file: resultsFilePath, text: testResults_str
            } catch (Exception ex) {
                println "Failed to update results file: ${ex.message}"
                throw ex
            }
        } catch (Exception ex) {
            println "Failed to collect test failures from XML report: ${ex.message}"
            throw ex
        }
    } catch (FileNotFoundException ex) {
        println "Unable to read test results files. File may be missing: ${ex.message}"
        throw ex
    }
}

def doesBugAlreadyExist(String jiraComponent, failedTestName) {
    jiraSummary = getJiraSummary(jiraComponent, failedTestName)
    jql_string = "summary~${jiraSummary} AND status != Done"
    try {
        withEnv(['JIRA_SITE=jira.corporate.local']) {
            try {
                // jiraJqlSearch: https://jenkinsci.github.io/jira-steps-plugin/steps/search/jira_jql_search/
                def searchResults = jiraJqlSearch jql: jql_string
                def issues = searchResults.data.issues
                return issues.collect {issue -> return issue.key}
            } catch (Exception ex){
                println "failed to get details from JiraJqlSearch step: ${ex.message}"
                throw ex
            }
        }
    } catch(Exception ex) {
        println "failed to connect to Jira: ${ex.message}"
        throw ex
    }
}

def getJiraBaseUrl() {
    try {
        withEnv(['JIRA_SITE=jira.corporate.local']) {
            try {
                // jiraGetServerInfo: https://jenkinsci.github.io/jira-steps-plugin/steps/admin/jira_server_info/
                def serverInfo = jiraGetServerInfo()
                return serverInfo.data.baseUrl
            } catch (Exception ex) {
                println "Failed to get Jira Server Info: ${ex.message}"
                throw ex
            }
        }
    } catch (Exception ex) {
        println "failed to connect to Jira: ${ex.message}"
        throw ex
    }
}

def raiseBug(String projectId, String jiraComponent, String issueType, String fixVersions, def labels, String testName, String testFailureMessage) {
    try {
        withEnv(['JIRA_SITE=jira.corporate.local']) {
            try {
                defaultLabel = 'PipelineBug'
                labels.add(defaultLabel)
                summary = getJiraSummary(jiraComponent, testName)
                description = test.failure.text()
                jiraIssue = [fields: [
                        project: [id: projectId],
                        summary    : summary,
                        description: testFailureMessage,
                        components : [[name: jiraComponent]],
                        fixVersions: [[name: fixVersions]],
                        issuetype  : [name: issueType],
                        labels     : labels]]

                // jiraNewIssue: https://jenkinsci.github.io/jira-steps-plugin/steps/issue/jira_new_issue/
                response = jiraNewIssue issue: jiraIssue
            } catch (Exception ex) {
                println "faild to raise jira ticket: ${ex.message}"
                throw ex
            }
        }
    } catch(Exception ex) {
        println "failed to connect to Jira: ${ex.message}"
        throw ex
    }
}

def getJiraSummary(String jiraComponent, failedTestName) {
    //Construct a summary string for the jira ticket out of the failed test name and component name,
    // conforming to 254 character length limit of Jira summary field.
    summary = jiraComponent + ":" + failedTestName
    return summary.take(254)
}

// Adding annotaion as Jenkins is returning NotSerializableException: groovy.util.slurpersupport.NodeChild
// for xmlSlurper object (testResults.testcase iterated variable 'test' )
@NonCPS
def appendBugIdToTestFailureMessage(def failedTests, String testResultXMLText) {
    def testResults = new XmlSlurper().parseText(testResultXMLText)
    try {
        testResults.testcase.each {
            test ->
                if (test.failure.size() > 0) {
                    String testName = test.@name.toString()
                    String testFailureText = test.failure.text().toString()
                    failedTests.each {
                        testcase ->
                            String jiraLink = ""
                            if (testName == testcase["name"]) {
                                testcase.jiraKeys.each {
                                    jiraKey-> jiraLink += jiraBaseUrl + '/browse/' + jiraKey + '\n'
                                }

                                test.failure = test.failure.text() + '\n' + jiraLink
                                println "Added bug id to test message: " + test.failure.text()
                            }
                    }
                }
        }
        return convertXMLToString(testResults)
    } catch (Exception ex) {
        throw ex
    }
}

def addCommentInExistingBugs(jiraKeysList, testFailureMessage) {
    jiraKeysList.each {
        jiraKey->
            try {
                withEnv(['JIRA_SITE=jira.corporate.local']) {
                    try {
                        // from jiraAddComment: https://jenkinsci.github.io/jira-steps-plugin/steps/comment/jira_add_comment/
                        jiraAddComment idOrKey: jiraKey, comment: testFailureMessage
                    } catch (Exception ex) {
                        println "failed to add comment in Jira: ${ex.message}"
                        throw ex
                    }
                }
            } catch(Exception ex) {
                println "failed to connect to Jira: ${ex.message}"
                throw ex
            }
    }
}

// Adding annotaion as Jenkins is returning NotSerializableException for StringWriter object (testResults_writer)
@NonCPS
def convertXMLToString(testResults) {
    def testResults_writer = new StringWriter()
    new XmlNodePrinter(new PrintWriter(testResults_writer)).print(testResults)
    XmlUtil.serialize( testResults, testResults_writer )
    return testResults_writer.toString()
}

// Adding annotaion as Jenkins is returning NotSerializableException: groovy.util.slurpersupport.NodeChild
// for xmlSlurper object (testResults.testcase iterated variable 'test' )
@NonCPS
def getFailedTests(String testResultXMLText){
    def failedTests = []
    def testResults = new XmlSlurper().parseText(testResultXMLText)
    try {
        testResults.testcase.each {
            test ->
                if (test.failure.size() > 0) {
                    String testName = test.@name.toString()
                    String testFailureText = test.failure.text().toString()
                    failedTests.add(["name":testName, "failure":testFailureText])
                }
        }
        return failedTests
    } catch (Exception ex) {
        throw ex
    }
}

