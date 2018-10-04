def call(String testresultsfilePath){
    def jira_issues = []
    println testresultsfilePath
    try {
        def xml = new XmlParser().parse(testresultsfilePath)
        xml.testcase.each{
            test ->
                def failedTests = [:]
                if (test.failure){
//                    failedTests.put('summary', test.@name)
//                    failedTests.put('file', test.@file)
//                    failedTests.put('details', test.failure.text())
//                    failedTests.put('description', test.properties.property.'@value'[0].trim())
//                    jira_issues << failedTests
//                    println (test.failure.@'message'[0]).split('\\n'[-1])
//                    println test.failure.@'message'[0].getClass()
//                    href_tag = '<p>This is a <a href=\'https://jira.corporate.local/browse/ION-7935\'>https://jira.corporate.local/browse/ION-7935</a> to another page</p>'
//                    test.failure.@'message' = test.failure.@'message' + "${href_tag}"
//                    println msg.split('-')[0]
//                    test.failure.@'message' = test.failure.@'message'[0] + ' - https://jira.corporate.local/browse/ION-7935'
//                    println test.failure.@'message'[0].replace('   ', '')
                    jiraComponent = 'bs35'
                    summary = test.@name
                    message = test.failure.@'message'[0].split('   ')[0]
//                    println summary
//                    println message
                    summary = jiraComponent + ': ' + summary + ': ' + message
//                    println summary
                    summary = summary.replace("'", "\\'")

                    def jql_str = "summary~" + "${summary}" + " AND status != Done"
                    println jql_str


//                    test.failure.'@message'[0] = test.failure.text() + ' - https://jira.corporate.local/browse/ION-7935'
//                    test.@name = test.@name + '- https://jira.corporate.local/browse/ION-7935'
//                    test.failure[0].@jira = 'https://jira.corporate.local/browse/ION-7935'
//                    def jira = new Node(test.failure[0], 'jira', 'https://jira.corporate.local/browse/ION-7935')
//                    println jira.toString()
//                    test.failure.add(jira)
//                    test.failure+ {Jira('https://jira.corporate.local/browse/ION-7935')}
//                    println test.failure.@message[0]
                }
        }

        def writer = new FileWriter(testresultsfilePath)
//        printer.preserveWhitespace = true
        new XmlNodePrinter(new PrintWriter(writer)).print(xml)


//        jira_issues
    }
    catch (FileNotFoundException exception){
        println 'file not found'
    }

//    xml.testcase.each{
//        test ->
//            def failedTests = [:]
//            if (test.failure){
//                failedTests.put('summary', test.@name)
//                failedTests.put('file', test.@file)
//                failedTests.put('details', test.failure.text())
//                failedTests.put('description', test.properties.property.'@value'[0].trim())
//                jira_issues << failedTests
//            }
//    }
//    jira_issues

    //jira_issues
}

resultfilePath = '/Users/rpandey/Documents/my-git/hello_python/TestResults.xml'
//
jira_issues = call(resultfilePath)
//jira_issues.each{
//    jira_issue ->
//        println jira_issue.summary
//        println jira_issue.file
//        println jira_issue.details
//        println jira_issue.description
//}
//
