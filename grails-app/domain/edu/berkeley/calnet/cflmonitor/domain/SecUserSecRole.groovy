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

package edu.berkeley.calnet.cflmonitor.domain

import org.apache.commons.lang.builder.HashCodeBuilder

class SecUserSecRole implements Serializable {

	SecUser secUser
	SecRole secRole

	boolean equals(other) {
		if (!(other instanceof SecUserSecRole)) {
			return false
		}

		other.secUser?.id == secUser?.id &&
			other.secRole?.id == secRole?.id
	}

	int hashCode() {
		def builder = new HashCodeBuilder()
		if (secUser) builder.append(secUser.id)
		if (secRole) builder.append(secRole.id)
		builder.toHashCode()
	}

	static SecUserSecRole get(long secUserId, long secRoleId) {
		find 'from SecUserSecRole where secUser.id=:secUserId and secRole.id=:secRoleId',
			[secUserId: secUserId, secRoleId: secRoleId]
	}

	static SecUserSecRole create(SecUser secUser, SecRole secRole, boolean flush = false) {
		new SecUserSecRole(secUser: secUser, secRole: secRole).save(flush: flush, insert: true)
	}

	static boolean remove(SecUser secUser, SecRole secRole, boolean flush = false) {
		SecUserSecRole instance = SecUserSecRole.findBySecUserAndSecRole(secUser, secRole)
		if (!instance) {
			return false
		}

		instance.delete(flush: flush)
		true
	}

	static void removeAll(SecUser secUser) {
		executeUpdate 'DELETE FROM SecUserSecRole WHERE secUser=:secUser', [secUser: secUser]
	}

	static void removeAll(SecRole secRole) {
		executeUpdate 'DELETE FROM SecUserSecRole WHERE secRole=:secRole', [secRole: secRole]
	}

	static mapping = {
		id composite: ['secRole', 'secUser']
		version false
	}
}
