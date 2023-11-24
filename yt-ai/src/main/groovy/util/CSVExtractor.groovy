package util

import static com.xlson.groovycsv.CsvParser.parseCsv

import data.Issue

class CSVExtractor {
    static List<Issue> extract(String filename) {
        def issues = new ArrayList<Issue>()

        for(line in parseCsv(new FileReader(filename), separator: ',')) {
            issues.add(new Issue(line."data.Issue Id", line.Subsystem, line.Description, line.Summary))
        }

        return issues
    }
}
