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


import org.apache.ivy.util.url.BasicURLHandler.HttpStatus;
import grails.converters.JSON
import edu.berkeley.calnet.cflmonitor.domain.ActionThreshold
import grails.transaction.Transactional

class ActionThresholdController {

	def inboundService
	
    def index() { }
    
	def thresholdList() {
		def result = inboundService.thresholdList()
		render( result as JSON)
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	def thresholdById(Integer id) {
		def result = inboundService.thresholdById( id)
		if( !result) {
			response.status = 404
			result = [:]
		}
		render( result as JSON)
	}
	
	/**
	 * Create or update an action threshold
	 *  {
	 *  	id: 1,
	 *  	description: "This is a description"
	 *  	count: 1000,
	 *  	action: "action to take... TODO"
	 *  	args: json format args
	 *  	enabled: true/false
	 *  }
	 */
	def newThreshold() {
		def thresholdDetails = request.JSON
		boolean ret = inboundService.createThreshold( thresholdDetails)
		if( !ret)
			response.status = 400
		render ""
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	@Transactional
	def updateThreshold( Integer id) {
		def thresholdDetails = request.JSON
		boolean ret = inboundService.updateThreshold( id, thresholdDetails)
		if( !ret)
			response.status = 400
		render ""
	}
	
	/**
	 * 
	 * @param id threshold id to be deleted
	 * @return
	 */
	def deleteThreshold( Integer id) {
		boolean ret = inboundService.deleteThreshold( id)
		if( !ret){
			response.status = 400
		}
		
		render ""
	}
}
