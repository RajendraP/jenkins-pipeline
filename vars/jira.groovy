def call(String jiraprojectName, String jiraComponent, String resultsfilePath, String logsPath,
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Jira') {
        withEnv(['JIRA_SITE=LOCAL']) {
            jiraBaseUrl = getJiraBaseUrl()
            try {
                def xml = new XmlParser().parse(resultsfilePath)
                xml.testcase.each {
                    test ->
                        def failedTest = [:]
                        def bugExists = []
                        if (test.failure) {
                            failedTest.put('summary', test.@name)
                            failedTest.put('file', test.@file)
                            failedTest.put('details', test.failure.text())
                            failedTest.put('description', test.properties.property.'@value'[0].trim())

                            bugExists = jiraExists(failedTest)
                            if (bugExists) {
                                echo 'Jira ticket already exists'
                                bugExists.each {
                                    jira ->
                                        println(jiraBaseUrl + '/browse/' + jira)
//                                        test.failure+ {Bug(jiraBaseUrl + '/browse/' + jira) }
//                                        test.failure+ {existing_bug_id('https://jira.corporate.local/browse/IPF-8')}
                                }
                            } else {
                                echo 'Going to raise a Jira ticket'
                                def jiraIssue =
                                        [fields:
                                                 [project    : [id: '16941'],
                                                  summary    : failedTest.summary,
                                                  description: failedTest.details,
                                                  components : [[name: jiraComponent]],
                                                  fixVersions: [[name: fixVersions]],
                                                  issuetype  : [name: issueType]]]
                                response = jiraNewIssue issue: jiraIssue
                                println(jiraBaseUrl + '/browse/' + response.data.key)
//                                test.failure + { Bug(jiraBaseUrl + '/browse/' + response.data.key) }
//                            uploadLogFile response.data.key

                            }
                            def writer = new FileWriter(resultsfilePath)
                            new XmlNodePrinter(new PrintWriter(writer)).print(xml)
                        }else {
                            echo 'There are no test failures..'
                        }
                }
            } catch (FileNotFoundException exception){
                    println 'file not found'
            }
        }
    }
}


def jiraExists(issue){
    summary = issue.summary
    description = issue.details
    description = ((description.split('\\n')[-1]))
    description = (description.split('\\n')[-1]).replace('/', '\\u002f').split(' ')[0]

    def jql_str = "PROJECT = IPF AND summary~${summary} AND description~${description} AND status != Done"
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

def uploadLogFile(jiraKey){
    node {
        withEnv(['JIRA_SITE=LOCAL']) {
            println "${workspace}"
            def attachment = jiraUploadAttachment idOrKey: jiraKey, file: "${workspace}/logs/hello_python.log"
            echo attachment.data.toString()
        }
    }
}