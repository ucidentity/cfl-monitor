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

import grails.converters.JSON
import grails.transaction.Transactional

import java.beans.PropertyChangeEvent;
import java.sql.Timestamp

import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormatter

import edu.berkeley.calnet.cflmonitor.domain.*

@Transactional
class InboundService {

	private String THRESHOLD_PATH = "/v&version/thresholds/&id"
	
	def grailsApplication
	def actionService
	
	/**
	 * 
	 * @param id
	 * @return
	 */
    def getSubjectDetails( String id) {
		def result = [:]
		def numFailures = AuthFailure.countBySubject(id)
		
		if( numFailures == 0) {
			result.status = 404
			return result
		}
		
		AuthFailureCounts authFailureCount = AuthFailureCounts.findBySubject( id)
		if( authFailureCount) {
			numFailures = authFailureCount.currentCount
		}
		
		def lastReset = AuthReset.createCriteria().list {
			eq('subject', id)
			order('reset', 'desc')
		}
		
		result.subject = id
		result.count = numFailures
		
		// get exceeded thresholds
		def actionThresholds = ActionThreshold.createCriteria().list {
			lt('count', numFailures.toInteger())
			eq('enabled', 1)
		}
		
		if( actionThresholds) {
			def exceededThresholds = []
			actionThresholds.each { threshold ->
				exceededThresholds << threshold.asReportMap()
			}
			
			result.exceededThresholds = exceededThresholds
		}
		
		// get history records
		def historyList = History.createCriteria().list {
			eq('subject', id)	
		}
		
		def historyRecords = []
		
		historyList.each { history ->
			def historyRecord = [:]
			historyRecord.id = history.id
			historyRecord.comment = history.comment
			historyRecord.executed = history.executed
			
			if( history.action) {
				def actionDetails = [:]
				def actionThreshold = history.action
				
				actionDetails.id = actionThreshold.id
				actionDetails.description = actionThreshold.description
				actionDetails.count = actionThreshold.count
				actionDetails.action = actionThreshold.action
				
				historyRecord.action = actionDetails
			}
			
			historyRecords << historyRecord
		}
		
		result.historyRecords = historyRecords
		
		return result
    }
	
	/**
	 * Reset the failed auth count for the specified subject
	 * 
	 * @param id Subject to reset
	 * @param count
	 * @return
	 */
	def subjectReset(String id, Integer count) {
		def reset = new AuthReset()
		reset.subject = id
		reset.reset = new Timestamp( System.currentTimeMillis())
		
		// create a history record for the reset
		ActionService.createHistoryRecord( id, "Reset", "Failed count reset recorded for " + id)
		
		reset.save(flush:true)
	}
	
	/**
	 * 
	 * @param count
	 * @return
	 */
	def subjectCount(Long count) {
		def result = [:]
		
		def subjects = AuthFailureCounts.createCriteria().list() {
			projections {
				property( 'subject')
			}
			
			gt('currentCount', count)
		}
		
		result.count = count
		result.subjects = subjects
		
		return result
	}
	
	/**
	 * Return subjects whose current failure counts exceed action threshold identified by actionId.
	 * Only subjects that meet the execution criteria are returned.  For example, if a subject has a threshold count without resets
	 * that exceed an action count and the history for the subject shows that an action has been executed, the subject
	 * will not be included in the results.
	 * 
	 * @param count
	 * @param actionId id of the action to retrieve a count
	 * 
	 * @return subjects that have a failed count (minus resets) greater than count
	 */
	def subjectCountByAction(Long count, Long actionId) {
		def result = [:]
		
		def subjects = AuthFailureCounts.createCriteria().list() {
			projections {
				property( 'subject')
				property( 'currentCount')
				property( 'currentCount') // a placeholder to place the last reset time
			}
			
			gt('currentCount', count)
		}
		
		Timestamp now = new Timestamp( new Date().getTime())
		def returnSubjectList = []
		if( subjects.size() > 0) {
			returnSubjectList = subjects.findAll { subject ->
				// get the history
				def historyList = History.createCriteria().list {
					eq('subject', subject[0])
					order('executed', 'desc')
				}
				
				boolean processAction = true
				boolean foundReset = false
				boolean foundMatch = historyList.any { history ->
					// look for this action in the list that appears before a reset event
					if( history.action == null) {
						// reset actions have null action
						subject[2] = history.executed.toString()  // put the last reset time here for possible reporting in notification email
						foundReset = true
					}
					else if( history.action.id == actionId) {
						processAction = false
					}
					
					if( foundReset || !processAction) {
						subject[2] = history.executed.toString() // put the last reset time here for possible reporting in notification email
						return true
					}
				}
				
				if( foundReset || processAction) {
					return true
				}
				
				return false
			}
		}
		
		result.count = count
		result.subjects = returnSubjectList
		
		return result
	}
	
	/**
	 * Return all action thresholds
	 * 
	 * @return
	 */
	def thresholdList() {
		def result = [:]
		def thresholdList = ActionThreshold.findAll()
		
		def thresholds = [:]
		thresholdList.each { threshold ->
			String path = THRESHOLD_PATH.replaceFirst("&id", threshold.id.toString())
			path = path.replaceFirst( "&version", grailsApplication.metadata.'app.version')
			thresholds.put( threshold.id, path)
		}
		
		result.thresholds = thresholds
		return result
	}
	
	/**
	 * Return action threshold details
	 * 
	 * @param id
	 * @return
	 */
	def thresholdById( Integer id) {
		def result = [:]
		def threshold = ActionThreshold.findById( id)
		if( threshold) {
			def map = threshold.asReportMap()
			map.enabled = threshold.enabled
			
			result.threshold = map
		}
		else { 
			result = null
		}
		
		return result
	}
	
	/**
	 * Return subjects who have a failure count that exceeds the action threashold count.
	 * 
	 * @param id threshold id
	 * @return
	 */
	def subjectThresholdReport( Integer id) {
		def result = [:]
		def actionThreshold = ActionThreshold.findById( id)
		if( actionThreshold) {
			def subjects = subjectCount( actionThreshold.count)	
			result.threshold = actionThreshold.count.toString()
			
			def subjectList = []
			subjects.subjects.each { subject ->
				subjectList << subject
			}
			
			result.subjects = subjectList
		}
		
		return result
	}
	
	/**
	 * Create a new action threshold from JSON formatted data.
	 * 
	 * @param thresholdDetails
	 * @return success
	 */
	def createThreshold( JSONObject thresholdDetails) {
		boolean success = true
		
		def newThreshold = new ActionThreshold()
		
		newThreshold.description = thresholdDetails.description
		newThreshold.count = thresholdDetails.count
		newThreshold.action = thresholdDetails.action
		newThreshold.args = thresholdDetails.args
		newThreshold.enabled = thresholdDetails.enabled
		
		try {
			newThreshold.save(flush:true)
		}
		catch( Exception e) {
			success = false
		}
		
		return success
	}
	
	/**
	 * Update action threshold settings.
	 * 
	 * @param id
	 * @param thresholdDetails
	 * @return
	 */
	def updateThreshold( Integer id, JSONObject thresholdDetails) {
		boolean success = true
		ActionThreshold actionThreshold = ActionThreshold.findById( id)
		
		if( thresholdDetails.count) {
			actionThreshold.count = thresholdDetails.count
		} 
		
		if( thresholdDetails.description) {
			actionThreshold.description = thresholdDetails.description
		}
		
		if( thresholdDetails.action) {
			actionThreshold.action = thresholdDetails.action
		}
		
		if( thresholdDetails.args) {
			actionThreshold.args = thresholdDetails.args
		}
		
		if( thresholdDetails.enabled == 0 || thresholdDetails.enabled == 1) {
			actionThreshold.enabled = thresholdDetails.enabled
		}
		
		actionThreshold.save( flush:true)
		
		return success	
	}
	
	/**
	 * Delete an action threshold.
	 * 
	 * @param id
	 * @return
	 */
	def deleteThreshold( Integer id) {
		boolean success = true
		def actionThreshold = ActionThreshold.findById( id)
		if( actionThreshold)
			actionThreshold.delete( flush:true)
		else {
			success = false
		}
		
		return success
	}
	
	/**
	 * 
	 * @param time Time in ISO8601 format
	 * @return
	 */
	def actionsSinceTimestamp( String time) {
		def result = [:]
		
		DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis()
		
		def dateTime = parser.parseDateTime( time)
		def timestamp = new Timestamp( dateTime.getMillis())
		def historyList = History.findAllByExecutedGreaterThan( timestamp)
		
		def historyResult = []
		historyList.each { history ->
			def singleHistory = history.asReportMap()
			historyResult << singleHistory
		}
		
		result.history = historyResult
		return result
	}
	
	/**
	 * 
	 * @param body
	 * @return
	 */
	def updateConfiguration( JSONObject body) {
		def configList = Configuration.findAll() // should only be one
		def config = null
		
		if( configList.size() == 0) {
			config = new Configuration()
		}
		else {
			config = configList[0]
		}
		
		config.cron = body.cron
		config.save( flush:true)
		
		actionService.rescheduleJob()
	}
}
