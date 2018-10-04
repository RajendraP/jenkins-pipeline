def call(String jiraprojectName, String jiraComponent, String resultsfilePath, String logsPath,
         String issueType='Bug', String fixVersions='pipeline_fixes') {
    stage(name: 'Jira') {
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
                                failedTest.put('message', test.failure.@'message'[0].split('   ')[0])
                                failedTest.put('file', test.@file)
                                failedTest.put('details', test.failure.text())
                                failedTest.put('description', test.properties.property.'@value'[0].trim())

                                echo 'going to call jiraExists'
                                bugExists = jiraExists(failedTest, jiraComponent)
                                if (bugExists) {
                                    echo 'Jira ticket already exists'
                                    bugExists.each {
                                        jira ->
                                            println(jiraBaseUrl + '/browse/' + jira)
                                            test.failure.@'message' = test.failure.@'message'[0] + ' - https://jira.corporate.local/browse/ION-7935'
//                                            href_tag = '<p>This is a <a href=\'https://jira.corporate.local/browse/ION-7935\'>https://jira.corporate.local/browse/ION-7935</a> to another page</p>'
//                                            test.failure.@'message' = test.failure.@'message' + "${href_tag}"
//                                            test.failure.text = test.failure.text() + ' - https://jira.corporate.local/browse/ION-7935'
//                                            test.failure[0].@jira = 'https://jira.corporate.local/browse/ION-7935'
//                                            new Node(test.failure[0], 'jira', 'https://jira.corporate.local/browse/ION-7935')
//                                            uploadLogFile jira, logsPath   // ignoring uploading of log file if jira alreay exists as it will upload every time
                                    }
                                } else {
                                    echo 'Going to raise a Jira ticket'
                                    try{
                                        summary = test.@name
                                        message = test.failure.@'message'[0].split('   ')[0]
                                        def jira_query = jiraComponent + ': ' + summary + ': ' + message
                                        echo 'c'
                                        println jira_query
                                        jira_query =  "\"${jira_query}\""

                                        //jira_query = jira_query.replace("'", "\\'")
                                        //jira_query =  "\"${jira_query}\""

                                        def jiraIssue =
                                                [fields:
                                                         [project    : [id: '16941'],
                                                          summary    : jira_query,
                                                          description: failedTest.details,
                                                          components : [[name: jiraComponent]],
                                                          fixVersions: [[name: fixVersions]],
                                                          issuetype  : [name: issueType]]]
                                        response = jiraNewIssue issue: jiraIssue
                                        println(jiraBaseUrl + '/browse/' + response.data.key)
//                                        test.@name = test.@name + ' - https://jira.corporate.local/browse/ION-7935'
//                                        test.@details = test.@details + ' - https://jira.corporate.local/browse/ION-7935'
                                        test.failure.@'message' = test.failure.@'message' + ' - https://jira.corporate.local/browse/ION-7935'
//                                        test.failure[0].@jira = 'https://jira.corporate.local/browse/ION-7935'
//                                test.failure+ {existing_bug_id("https://jira.corporate.local/browse/IPF-8")}
//                                        new Node(test.failure[0], 'jira', 'https://jira.corporate.local/browse/ION-7935')
//                                        uploadLogFile response.data.key
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
                    def writer = new FileWriter(resultsfilePath)
//        printer.preserveWhitespace = true
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


def jiraExists(failedTest, jiraComponent){
//    summary = issue.summary
//    description = issue.details
//    description = ((description.split('\\n')[-1]))
//    description = (description.split('\\n')[-1]).replace('/', '\\u002f').split(' ')[0]
//
//    def jql_str = "summary~${summary} AND description~${description} AND status != Done"
//    echo jql_str

    echo 'inside jira exists'
    summary = failedTest.summary
    message = failedTest.message
    def jira_query = jiraComponent + ': ' + summary + ': ' + message

    jira_query = jira_query.replace("'", "\\'")
    jira_query =  "\"${jira_query}\""
    def jql_str = "summary~${jira_query} AND status != Done"
    println jql_str

    try{
        withEnv(['JIRA_SITE=LOCAL']) {
            try{
                echo 'b'
                def searchResults = jiraJqlSearch jql: jql_str
                def jiraKeys = []
                def issues = searchResults.data.issues
                for (i = 0; i <issues.size(); i++) {
                    try{
                        echo 'a'
                        def issue = jiraGetIssue idOrKey: issues[i].key
                        def bugSummary = issue.data.fields.summary
                        println bugSummary
                        if (jira_query == bugSummary){
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
