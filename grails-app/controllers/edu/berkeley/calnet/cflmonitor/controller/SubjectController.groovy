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

package edu.berkeley.calnet.cflmonitor.controller

import grails.converters.JSON
import java.sql.Timestamp
import edu.berkeley.calnet.cflmonitor.domain.*

class SubjectController {
	
	def inboundService
	
    def index() { }
    
	/**
	 * 
	 * @param id
	 * @return
	 */
	def subjectDetails(String id) {
		def result = inboundService.getSubjectDetails( id) //[:]
		if( result.status == 404) {
			response.status = 404
		}
		render(result as JSON)
	}
	
	/**
	 * 
	 * @param id Subject id to reset
	 * @return
	 */
	def subjectReset(String id) {
		def requestBody = request.JSON
		def count = requestBody.count as Integer
		if( count != 0) {
			// return 400 Bad Request
			response.status = 400
		}
		else {
			try {
				inboundService.subjectReset( id, count)
				response.status = 200
			}
			catch( Exception e) {
				e.printStackTrace()
				response.status = 400
			}
		}
		
		render ""
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	def subjectThresholdReport(Integer id) {
		def result = inboundService.subjectThresholdReport( id)
		render( result as JSON)
	}
	
	/**
	 * 
	 * @param count
	 * @return subjects that have a failed count (minus resets) greater than count
	 */
	def subjectCount(Integer count) {
		def result = inboundService.subjectCount( count)
		render( result as JSON)
	}
	
	/**
	 * 
	 * @param timestamp
	 * @return
	 */
	def actionsSinceTimestamp(String timestamp) {
		def result = inboundService.actionsSinceTimestamp( timestamp)
		render( result as JSON)
	}
}
