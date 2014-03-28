package edu.berkeley.calnet.cflmonitor.service

import com.sun.org.apache.xalan.internal.xsltc.compiler.Import;

import grails.transaction.Transactional
import groovyx.gpars.dataflow.operator.component.GracefulShutdownListener;
import edu.berkeley.calnet.cflmonitor.service.ActionService
import edu.berkeley.calnet.cflmonitor.service.InboundService
import edu.berkeley.calnet.cflmonitor.domain.ActionThreshold
import groovyx.gpars.AsyncFun
import static groovyx.gpars.GParsPool.withPool

@Transactional
class JobService {
	def actionService
	def inboundService
	
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
							performAction( service, subject, action.args)
						}
					}
				}
			}
		}
    }
}
