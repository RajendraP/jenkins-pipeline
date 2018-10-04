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
//                            def failedTest = [:]
                            def bugExists = []
                            if (test.failure) {
//                                failedTest.put('summary', test.@name)
//                                failedTest.put('file', test.@file)
//                                failedTest.put('details', test.failure.text())
//                                failedTest.put('description', test.properties.property.'@value'[0].trim())

                                bugExists = jiraExists(test)
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
//                                add_jira(test.failure)
//                                            uploadLogFile jira, logsPath   // ignoring uploading of log file if jira alreay exists as it will upload every time
                                    }
                                } else {
                                    echo 'Going to raise a Jira ticket'
                                    summary = test.@name
                                    message = test.failure.@'message'[0].split('   ')[0]
                                    jira_summary = jiraComponent + ':' + summary + ': ' + message
                                    echo jira_summary

                                    try{
                                        def jiraIssue =
                                                [fields:
                                                         [project    : [idOrKey: jiraprojectName],
                                                          summary    : jira_summary,
                                                          description: test.failure.text(),
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


def jiraExists(test){

//    failedTest.put('summary', test.@name)
//                                failedTest.put('file', test.@file)
//                                failedTest.put('details', test.failure.text())
//                                failedTest.put('description', test.properties.property.'@value'[0].trim())

    summary = test.@name
    message = test.failure.@'message'[0].split('   ')[0]
    def jira_summary = jiraComponent + ':' + summary + ': ' + message
    println jira_summary


    def jql_str = "summary~${summary} AND status != Done"
    echo jql_str
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

//def add_jira(failure){
//    failure+ {bug_id("https://jira.corporate.local/browse/IPF-8")}
//}