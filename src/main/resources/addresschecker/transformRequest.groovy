import groovy.json.JsonSlurper
import groovy.xml.*;

def slurper = new JsonSlurper()
def input = slurper.parseText(payload)

def parser = new XmlParser()
def output = parser.parseText('<checkerRequest/>')

if (input.unit) {
	output.appendNode('addressLine1', input.unit + "/" + input.number + " " + input.street)
} else {
	output.appendNode('addressLine1', input.number + " " + input.street)
}
output.appendNode('addressLine2', input.suburb)
output.appendNode('addressLine3', input.city)
output.appendNode('country', input.country)

return XmlUtil.serialize(output)