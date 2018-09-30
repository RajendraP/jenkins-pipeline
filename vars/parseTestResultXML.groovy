def call(String testresultsfilePath){
//    def jira_issues = []
    println testresultsfilePath
    try {
        def xml = new XmlParser().parse(testresultsfilePath)
        xml.testcase.each{
            test ->
//                def failedTests = [:]
                if (test.failure){
//                    failedTests.put('summary', test.@name)
//                    failedTests.put('file', test.@file)
//                    failedTests.put('details', test.failure.text())
//                    failedTests.put('description', test.properties.property.'@value'[0].trim())
//                    jira_issues << failedTests
                    test.failure+ {Jira('https://jira.corporate.local/browse/ION-7935')}
                }
        }

        def writer = new FileWriter(testresultsfilePath)
        //printer.preserveWhitespace = true
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

//resultfilePath = '/Users/rpandey/Documents/my-git/hello_python/src/test/result.xml'
//
//jira_issues = call(resultfilePath)
//jira_issues.each{
//    jira_issue ->
//        println jira_issue.summary
//        println jira_issue.file
//        println jira_issue.details
//        println jira_issue.description
//}
//
