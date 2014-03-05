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
		def lastReset = AuthReset.createCriteria().list {
			eq('subject', id)
			order('reset', 'desc')
		}
		
		result.subject = id
		result.count = numFailures
		
		if( lastReset) {
			result.lastReset = lastReset[0].reset
			
			numFailures = AuthFailure.createCriteria().get {
				projections {
					count('id')
				}
				ge('recorded', lastReset[0].reset)
				eq('subject', id)
			}
		}
		
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
	def subjectCount(Integer count) {
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
	 * 
	 * @param count
	 * @param actionId id of the action to retrieve a count
	 * 
	 * @return subjects that have a failed count (minus resets) greater than count
	 */
	def subjectCountByAction(Integer count, Long actionId) {
		def result = [:]
		
		def subjects = AuthFailureCounts.createCriteria().list() {
			projections {
				property( 'subject')
			}
			
			gt('currentCount', count)
		}
		
		Timestamp now = new Timestamp( new Date().getTime())
		def returnSubjectList = []
		if( subjects.size() > 0) {
			returnSubjectList = subjects.findAll { subject ->
				// get the history
				def historyList = History.createCriteria().list {
					eq('subject', subject)
					order('executed', 'desc')
				}
				
				boolean processAction = true
				boolean foundReset = false
				boolean foundMatch = historyList.any { history ->
					// look for this action in the list that appears before a reset event
					if( history.action == null) {
						// reset actions have null action
						foundReset = true
					}
					else if( history.action.id == actionId) {
						processAction = false
					}
					
					if( foundReset || !processAction) {
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
	 * 
	 * @param time Time in ISO8601 format
	 * @return
	 */
	def actionsSinceTimestamp( String time) {
		def result = [:]
		
		DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis()
		
		def dateTime = parser.parseDateTime( time)
		print dateTime
		def timestamp = new Timestamp( dateTime.getMillis())
		def historyList = History.findAllByExecutedGreaterThan( timestamp)
		print historyList
		
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
