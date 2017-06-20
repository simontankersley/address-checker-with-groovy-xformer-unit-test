import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.xml.*;

def parser = new XmlSlurper()
def input = parser.parseText(payload)

def output = [:]

output.result = input.text()

return JsonOutput.toJson(output)