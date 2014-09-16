import java.awt.TexturePaintContext.Int;
import java.sql.Timestamp

import javax.mail.AuthenticationFailedException;

import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

import edu.berkeley.calnet.cflmonitor.domain.*
import edu.berkeley.calnet.cflmonitor.service.*
import edu.berkeley.calnet.cflmonitor.ActionJob
import static org.quartz.TriggerBuilder.*
import static org.quartz.CronScheduleBuilder.*
import static org.quartz.DateBuilder.*

class BootStrap {

    def springSecurityService
    def grailsApplication 
	
    def init = { servletContext ->
        def userRole = SecRole.findByAuthority('ROLE_USER') ?: new SecRole(authority: 'ROLE_USER').save(failOnError: true)
        def adminRole = SecRole.findByAuthority('ROLE_ADMIN') ?: new SecRole(authority: 'ROLE_ADMIN').save(failOnError: true)

        def adminUser = SecUser.findByUsername('admin') ?: new SecUser(
            username: 'admin',
            password: 'admin',
            enabled: true).save(failOnError: true)
		
        def cflUser = SecUser.findByUsername('cflUser') ?: new SecUser(
            username: 'cflUser',
            password: 'cf1Us3r',
            enabled: true).save(failOnError: true)
			
        if (!adminUser.authorities.contains(adminRole)) {
            SecUserSecRole.create adminUser, adminRole
        }
                
        if (!adminUser.authorities.contains(userRole)) {
            SecUserSecRole.create adminUser, userRole
        }

        if (!cflUser.authorities.contains(userRole)) {
            SecUserSecRole.create cflUser, userRole
        }
		
		def cronString = grailsApplication.config.cfl.cron as String
		def configList = Configuration.findAll() // should only be one
		if( configList.size() > 0) {
			cronString = configList[0].cron
		}
		
		// start action trigger
		def trigger = newTrigger()
			.withIdentity( ActionService.JOB_IDENTITY, ActionService.JOB_GROUP)
			.withSchedule( cronSchedule( cronString))
			.forJob( ActionService.JOB_NAME)
			.build()
		
		ActionJob.schedule( trigger)
    }
	
    def destroy = {
    }
}
