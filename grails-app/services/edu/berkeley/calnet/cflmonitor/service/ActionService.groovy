/*
 Copyright (c) 2014, University of California, Berkeley
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:
 
 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
 
 * Redistributions in binary form must reproduce the above copyright notice, this
   list of conditions and the following disclaimer in the documentation and/or
   other materials provided with the distribution.
 
 * Neither the name of the {organization} nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.
 
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.calnet.cflmonitor.service

import javax.mail.*

import javax.mail.internet.*

import java.util.Date;
import java.sql.Timestamp;

import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerKey
import org.quartz.impl.StdSchedulerFactory
import static org.quartz.TriggerBuilder.*
import static org.quartz.CronScheduleBuilder.*

import edu.berkeley.calnet.cflmonitor.domain.History
import edu.berkeley.calnet.cflmonitor.domain.ActionThreshold
import edu.berkeley.calnet.cflmonitor.domain.Configuration
import edu.berkeley.calnet.cflmonitor.ActionJob

class ActionService {
	def actions = [:]
	
	def grailsApplication
	def quartzScheduler
	
	static String JOB_IDENTITY = "actionTrigger"
	static String JOB_GROUP = "actionGroup"
	static String JOB_NAME = "actionJob"
	
	/*
	 * Load action config scripts
	 */
	def refreshActions() {
		actions.clear()
		
		new File( grailsApplication.config.cfl.externalFiles ).eachFile { file ->
			def config = new ConfigSlurper().parse( file.toURL())
			actions.put( config.action, config)
		}
	}
	
	def getActions() {
		return actions;
	}
	
	/**
	 * 
	 * @return
	 */
	def rescheduleJob() {
	   def config = Configuration.findAll()
	   if( config.size() > 0) {
		   def triggerKey = TriggerKey.triggerKey( JOB_IDENTITY, JOB_GROUP)
		   Trigger trigger = quartzScheduler.getTrigger( triggerKey)
		   trigger.setCronExpression( config[0].cron)
		   quartzScheduler.rescheduleJob( triggerKey, trigger)
	   }
	}
	
	/**
	 * Callback method for scripted actions to create history records for the action that is executed.
	 * 
	 * @param subject
	 * @param action
	 * @param comment
	 * @return
	 */
	static def createHistoryRecord( String subject, String action, String comment) {
		def date = new Date()
		def timestamp = new Timestamp( date.getTime())
		
		def actionThreshold = ActionThreshold.findByAction( action)
		
		def history = new History()
		history.subject = subject
		history.action = actionThreshold
		history.executed = timestamp
		history.comment = comment
		
		history.save( flush:true)
	}
}
