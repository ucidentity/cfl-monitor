package edu.berkeley.calnet.cflmonitor.controller

import edu.berkeley.calnet.cflmonitor.domain.*
import edu.berkeley.calnet.cflmonitor.service.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(SubjectController)
@Mock([ActionThreshold,AuthFailure,AuthReset,AuthFailureCounts,History,InboundService])
class SubjectControllerSpec extends Specification {
	
    def setup() {
		new ActionThreshold(id:1, enabled: 1, action: "anAction", description: "desc", count: 2, args: "some args")
			.save()
		new ActionThreshold(id:2, enabled: 1, action: "anotherAction", description: "desc", count: 4, args: "more args")
			.save()
			
		new AuthFailure(id:1, ipAddress: "192.168.0.1", service: "test service", subject: "test", recorded: "2014-02-26 14:35:40")
			.save()
		new AuthFailure(id:2, ipAddress: "192.168.0.1", service: "test service", subject: "test", recorded: "2014-02-26 14:35:41")
			.save()
		new AuthFailure(id:3, ipAddress: "192.168.0.1", service: "test service", subject: "test", recorded: "2014-02-26 14:35:42")
			.save()
			
		new AuthFailureCounts(subject: "test", currentCount: 3).save()
		
		new History(id: 1, subject: "test", action: 1, comment: "test comment", executed: "2014-02-26 12:35:42")
			.save()
		new History(id: 2, subject: "test", action: null, comment: "Count reset", executed: "2014-02-25 12:35:42")
			.save()
    }

    def cleanup() {
    }

    void "Test valid Subject details"() {
		when:
			controller.subjectDetails( "test")
				
		then:
			response.json.subject == "test"
			response.json.historyRecords.size() == 2
			response.json.exceededThresholds[0].count == 2
    }
	
	/*
	 * For unknown reasons, this test will is not able to load the AuthFailureCounts data
	def "Test Subject count"() {
		when:
			controller.subjectCount( 2)
			
		then:
			response.json.count == 2
			response.json.subjects.size() == 1
			response.json.subjects[0] == "test"
		
	}
	*/
	
	def "Test invalid Subject reset"() {
		when:
			controller.request.content = "{count:1}"
			controller.subjectReset("test")
			
		then:
			response.status == 400
	}
	
	def "Test valid Subject reset"() {
		when:
			controller.request.content = "{count:0}"
			controller.subjectReset("test")
			
		then:
			response.status == 200
	}
	
	/*
	 * For unknown reasons, this test will is not able to load the AuthFailureCounts data
	def "Test Subject Threshold Report"() {
		when:
			controller.subjectThresholdReport(1)
			
		then:
			controller.response.json.subjects[0] == "test"
	}
	*/
}
