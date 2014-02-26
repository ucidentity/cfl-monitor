package edu.berkeley.calnet.cflmonitor.controller

import spock.lang.*
import edu.berkeley.calnet.cflmonitor.domain.*
import edu.berkeley.calnet.cflmonitor.service.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

@TestFor(ActionThresholdController)
@Mock([ActionThreshold,InboundService])
class ActionThresholdControllerSpec extends Specification {
	
	def setup() {
		grailsApplication.metadata.'app.version' = "1"
	}

	def cleanup() {
	}

	void "Test thresholdList"() {
		given:
			for(int i = 0; i < 5; i++) {
				new ActionThreshold(id:i, enabled: 1, action: "anAction", description: "desc", count: 2, args: "some args")
					.save()
			}
		
		when:
			controller.thresholdList()
			
		then:
			response.json.thresholds.size() == 5
	}
    
	void "Test valid thresholdById"() {
		given:
			new ActionThreshold(id:1, enabled: 1, action: "testAction", description: "test description", args: "{arg: 1}", count: 300)
				.save()
		when:
			controller.thresholdById( 1)
		then:
			response.json.threshold.id == 1
			response.json.threshold.count == 300
	}
	
	void "Test invalid thresholdById"() {
		given:
			new ActionThreshold(id:1, enabled: 1, action: "testAction", description: "test description", args: "{arg: 1}", count: 300)
				.save()
		when:
			controller.thresholdById( 2)
		then:
			response.status == 404
	}
}
