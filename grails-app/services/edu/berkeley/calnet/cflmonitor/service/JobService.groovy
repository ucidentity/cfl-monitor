package edu.berkeley.calnet.cflmonitor.service

import static groovyx.gpars.GParsPool.withPool
import edu.berkeley.calnet.cflmonitor.domain.ActionThreshold
import grails.plugin.asyncmail.AsynchronousMailService
import grails.transaction.Transactional

@Transactional
class JobService {
	def actionService
	def inboundService
	def mailService
	
	AsynchronousMailService asyncMailService
	
	/**
	 * 
	 * @return
	 */
    def execute() {
		actionService.refreshActions()
		def actions = actionService.getActions()
		
		// load thresholds
		def thresholdList = ActionThreshold.findAll()
		thresholdList.each { action ->
			if( action.enabled == 1) {
				def service = actions[action.action]
				if( service) {
					// are there subjects with count exceeding the threshold?
					// run performAction async in thread pool
					withPool {
						def subjects = inboundService.subjectCountByAction( (Long)action.count, action.id)
						subjects.subjects.each { subject ->
							log.info "Perform action: " + action.action + " subject: " + subject
							
							Closure performAction = service.&performAction.asyncFun()
							performAction( mailService, service, subject, action.args)
						}
					}
				}
			}
		}
    }
}
