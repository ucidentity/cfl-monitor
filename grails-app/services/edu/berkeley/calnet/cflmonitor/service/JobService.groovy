package edu.berkeley.calnet.cflmonitor.service

import grails.transaction.Transactional
import edu.berkeley.calnet.cflmonitor.service.ActionService
import edu.berkeley.calnet.cflmonitor.service.InboundService
import edu.berkeley.calnet.cflmonitor.domain.ActionThreshold

@Transactional
class JobService {
	def actionService
	def inboundService
	
    def execute() {
		actionService.refreshActions()
		def actions = actionService.getActions()
		
		// load thresholds
		def thresholdList = ActionThreshold.findAll()
		thresholdList.each { action ->
			def service = actions[action.action]
			if( service) {
				// are there subjects with count exceeding the threshold?
				def subjects = inboundService.subjectCountByAction( action.count, action.id)
				subjects.subjects.each { subject ->
					print "Perform action: " + action.action + " subject: " + subject
					service.performAction( service, subject, action.args)
				}
			}
			
		}
    }
}
