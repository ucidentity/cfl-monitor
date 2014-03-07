import javax.mail.*
import javax.mail.internet.*
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.*
import edu.berkeley.calnet.cflmonitor.domain.History
import edu.berkeley.calnet.cflmonitor.service.ActionService

name = "Email Action"
action = "emailAction"

performAction = { config, subject, args ->
	def props = new Properties()
	props.put( "mail.smtps.auth", true)

	def session = Session.getDefaultInstance( props, null)
	def msg = new MimeMessage( session)

	JSONObject jsonArgs = JSON.parse( args)

	msg.setSubject( jsonArgs.subject)
	msg.setText( jsonArgs.message)
	msg.addRecipient( Message.RecipientType.TO, new InternetAddress( jsonArgs.recipient))

	def transport = session.getTransport( "smtps")

	try {
		transport.connect( jsonArgs.host, jsonArgs.username, jsonArgs.password)
		transport.sendMessage( msg, msg.getAllRecipients())

		ActionService.createHistoryRecord( subject, config.action, "Email sent")
	}
	catch( Exception e) {
	   println "Error in mail"
	   e.printStackTrace()
	}
}