def call(String jiraprojectName, String jiraComponent, String resultsfilePath, String logsPath, String[] labels=[],
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Jira') {
        try {
            withEnv(['JIRA_SITE=LOCAL']) {
                jiraBaseUrl = getJiraBaseUrl()
                try {
                    def xml = new XmlParser().parse(resultsfilePath)
                    xml.testcase.each {
                        test ->
//                            def failedTest = [:]
//                            def bugExists = []
                            if (test.failure) {
//                                failedTest.put('summary', test.@name)
//                                failedTest.put('message', test.failure.@'message'[0].split('   ')[0])
//                                failedTest.put('file', test.@file)
//                                failedTest.put('details', test.failure.text())
//                                failedTest.put('description', test.properties.property.'@value'[0].trim())

                                echo 'going to call jiraExists'
                                def bugExists = []
                                bugExists = jiraExists jiraComponent, test
                                if (bugExists) {
                                    echo 'Jira ticket already exists'
                                    bugExists.each {
                                        jira ->
                                            def bugId = jiraBaseUrl + '/browse/' + jira
                                            println bugId
                                            test.failure.@'message' = test.failure.@'message'[0] + '\n' + bugId
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
//    summary = issue.summary
//    description = issue.details
//    description = ((description.split('\\n')[-1]))
//    description = (description.split('\\n')[-1]).replace('/', '\\u002f').split(' ')[0]
//
//    def jql_str = "summary~${summary} AND description~${description} AND status != Done"
//    echo jql_str

//    failedTest.put('summary', test.@name)
//    failedTest.put('message', test.failure.@'message'[0].split('   ')[0])
//    failedTest.put('file', test.@file)
//    failedTest.put('details', test.failure.text())
//    failedTest.put('description', test.properties.property.'@value'[0].trim())

    echo 'inside jira exists'
//    summary = failedTest.@name
//     message = failedTest.failure.@'message'[0].split('   ')[0]
//    def jira_summary = jiraComponent + ': ' + summary + ': ' + message
//    jira_summary= jira_summary.take(220)

    jira_summary = getJiraSummary(jiraComponent, failedTest)

    def jira_summary_updated = jira_summary.replace("'", "\\'")
//    jira_query_updated.take(240)  // for safe

    jira_summary_updated =  "\"${jira_summary_updated}\""
    def jql_str = "summary~${jira_summary_updated} AND status != Done"
    println jql_str

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
                        println bugSummary
                        println jira_summary
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
    echo 'Going to raise a Jira ticket'
    try {
        withEnv(['JIRA_SITE=LOCAL']) {
            try {
//                summary = test.@name
//                message = test.failure.@'message'[0].split('   ')[0]
//                def jira_query = jiraComponent + ': ' + summary + ': ' + message
//                jira_query.take(220)
                summary = getJiraSummary(jiraComponent, test)
                description = test.failure.text()

                def jiraIssue =
                        [fields:
                                 [project    : [id: '16941'],
                                  summary    : summary,
                                  description: failedTest.details,
                                  components : [[name: jiraComponent]],
                                  fixVersions: [[name: fixVersions]],
                                  issuetype  : [name: issueType],
                                  labels     : labels]]

                response = jiraNewIssue issue: jiraIssue
                def jira = jiraBaseUrl + '/browse/' + response.data.key
                println jira

                test.failure.@'message' = test.failure.@'message' + ' - ' + jira

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
