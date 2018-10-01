def call(String testresultsfilePath){
    def jira_issues = []
    println testresultsfilePath
    try {
        def xml = new XmlParser().parse(testresultsfilePath)
        xml.testcase.each{
            test ->
                def failedTests = [:]
                if (test.failure){
                    failedTests.put('summary', test.@name)
                    failedTests.put('file', test.@file)
                    failedTests.put('details', test.failure.text())
                    failedTests.put('description', test.properties.property.'@value'[0].trim())
                    jira_issues << failedTests
                }
        }
        jira_issues
    }
    catch (FileNotFoundException exception){
        echo 'Exception:', exception
    }
    jira_issues
}

