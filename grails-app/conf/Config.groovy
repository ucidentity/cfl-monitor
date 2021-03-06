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

// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination

// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['Gecko', 'WebKit', 'Presto', 'Trident']
grails.mime.types = [
    all:           '*/*',
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    hal:           ['application/hal+json','application/hal+xml'],
    xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}
 
grails.converters.encoding = "UTF-8"
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

environments {
    development {
        grails.logging.jul.usebridge = true
    }
	development-mysql {
		grails.logging.jul.usebridge = true
	}
    production {
        grails.logging.jul.usebridge = false
        // TODO: grails.serverURL = "http://www.changeme.com"
    }
}

// log4j configuration
def catalinaBase = System.properties.getProperty('catalina.base')
if (!catalinaBase) catalinaBase = '/Users/mglazier/work'
def logDirectory = "${catalinaBase}/logs/cfl"

log4j = {
    appenders {
        console name:'stdout', layout:pattern(conversionPattern: '%d [%t] %-5p %c{2} %x - %m%n')
		appender new DailyRollingFileAppender(
			name: "rollingFileCfl",
			file: "${logDirectory}/cfl.log",
			datePattern: "'.'yyyy-MM-dd",
			layout: pattern(conversionPattern: '%d [%t] %-5p %c{2} %x - %m%n')
		)
    }
    
	root {
		info 'stdout', 'rollingFileCfl'
		additivity = false
	}
	
	debug 'org.quartz'
}

grails.plugins.springsecurity.userLookup.userDomainClassName = 'edu.berkeley.calnet.cflmonitor.domain.SecUser'
grails.plugins.springsecurity.userLookup.authorityJoinClassName = 'edu.berkeley.calnet.cflmonitor.domain.SecUserSecRole'
grails.plugins.springsecurity.authority.className = 'edu.berkeley.calnet.cflmonitor.domain.SecRole'

import grails.plugins.springsecurity.SecurityConfigType
import org.apache.log4j.DailyRollingFileAppender
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader

grails.plugins.springsecurity.useSessionFixationPrevention = true
grails.plugins.springsecurity.securityConfigType = SecurityConfigType.InterceptUrlMap
grails.plugins.springsecurity.rejectIfNoRule = true

grails.plugins.springsecurity.interceptUrlMap = [
	// now user can do everything
	'/v*/**': ['ROLE_USER'],
	'/*.html':							[
		'IS_AUTHENTICATED_ANONYMOUSLY'
	],
	'/*.xml':							[
		'IS_AUTHENTICATED_ANONYMOUSLY'
	],
	'/*.gsp':							[
		'IS_AUTHENTICATED_ANONYMOUSLY'
	],
	'/css/**':							[
		'IS_AUTHENTICATED_ANONYMOUSLY'
	],
	'/js/**':							[
		'IS_AUTHENTICATED_ANONYMOUSLY'
	],
	'/images/**':							[
		'IS_AUTHENTICATED_ANONYMOUSLY'
	],
	'/html/**':							[
		'IS_AUTHENTICATED_ANONYMOUSLY'
	],
	'/dbconsole/**':							[
		'ROLE_ADMIN'
	]
]

grails.plugins.springsecurity.secureChannel.definition = [
	'/login/**':						'REQUIRES_SECURE_CHANNEL',
	'/register/**':						'REQUIRES_SECURE_CHANNEL',
]

// Set remember me timeout to 10 days
grails.plugins.springsecurity.rememberMe.tokenValiditySeconds=60*60*24*10

//grails.plugins.springsecurity.ui.forgotPassword.emailFrom = ''
//grails.plugins.springsecurity.ui.register.emailFrom = ''

grails.plugins.springsecurity.useSecurityEventListener = true
grails.plugins.springsecurity.logout.handlerNames =
		[
			'rememberMeServices',
			'securityContextLogoutHandler'
		]

grails.plugins.springsecurity.useBasicAuth = true
grails.plugins.springsecurity.basic.realmName = "CFL Server"

grails.converters.pretty.print = true
grails.json.date = "javascript"

grails {
	mail {
	  host = "smtp.gmail.com"
	  port = 465
	  username = ""
	  password = ""
	  props = ["mail.smtp.auth":"true",
			   "mail.smtp.socketFactory.port":"465",
			   "mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
			   "mail.smtp.socketFactory.fallback":"false"]
	}
 }

// this is the default action polling time
cfl {
	externalFiles = "/var/cfl/actions"
	cron = "0 0/2 * * * ?"
}