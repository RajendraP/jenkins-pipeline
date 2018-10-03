def call(String jiraprojectName, String jiraComponent, String resultsfilePath, String logsPath,
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Create_Jira') {
        try {
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
//                                        test.failure+ {existing_bug_id("https://jira.corporate.local/browse/IPF-8")}
//                                        add_jira(test.failure)
                                            uploadLogFile jira, logsPath
                                    }
                                } else {
                                    echo 'Going to raise a Jira ticket'
                                    try{
                                        def jiraIssue =
                                                [fields:
                                                         [project    : [idOrKey: jiraprojectName],
                                                          summary    : failedTest.summary,
                                                          description: failedTest.details,
                                                          components : [[name: jiraComponent]],
                                                          fixVersions: [[name: fixVersions]],
                                                          issuetype  : [name: issueType]]]
                                        response = jiraNewIssue issue: jiraIssue
                                        println(jiraBaseUrl + '/browse/' + response.data.key)
//                                test.failure+ {existing_bug_id("https://jira.corporate.local/browse/IPF-8")}
//                                add_jira(test.failure)
                                        uploadLogFile response.data.key
                                    }catch(Exception ex){
                                        echo 'faild to raise jira ticket'
                                    }
                                }
//                                def writer = new FileWriter(resultsfilePath)
//                                new XmlNodePrinter(new PrintWriter(writer)).print(xml)
                            }else {
                                echo 'There are no test failures..'
                            }
                    }
                } catch (FileNotFoundException exception){
                    println 'file not found'
                }
            }
        }catch(Exception ex){
            echo 'Failed to connect to Jira'
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
    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try{
                def searchResults = jiraJqlSearch jql: jql_str
                def jiraKeys = []
                def issues = searchResults.data.issues
                for (i = 0; i <issues.size(); i++) {
                    jiraKeys<< issues[i].key
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

def uploadLogFile(jiraKey, logsPath){
    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try {
                def attachment = jiraUploadAttachment idOrKey: jiraKey, file: "${logsPath}/hello_python.log"
            }catch(Exception ex){
                echo 'Failed to upload log file to jira'
            }
        }
    }catch(Exception ex){
        echo 'Failed to connect to Jira'
    }
}

def add_jira(failure){
    failure+ {bug_id("https://jira.corporate.local/browse/IPF-8")}
}