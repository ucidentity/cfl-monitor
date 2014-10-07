import javax.mail.*
import javax.mail.internet.*
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.*
import edu.berkeley.calnet.cflmonitor.domain.History
import edu.berkeley.calnet.cflmonitor.service.ActionService
import grails.plugin.mail.*

name = "Email Action"
action = "emailAction"

performAction = { mailService, config, subj, args ->
	try {
		JSONObject jsonArgs = JSON.parse( args)

		def String message = jsonArgs.message
    		message = message.replace( "SUBJECT", subj[0])
    		message = message.replace( "COUNT", String.valueOf(subj[1]))
    		message = message.replace( "LASTRESET", String.valueOf(subj[2]))
    		message = message.replace( "THRESHOLD", action)

	    mailService.sendMail {
		   to jsonArgs.recipient
		   from "put-someones-email-here@anemailhost.com"
		   subject jsonArgs.subject
		   body message
		}

		ActionService.createHistoryRecord( subj[0], config.action, "Email sent")
	}
	catch( Exception e) {
	   println "Error in mail"
	   e.printStackTrace()
	}
}