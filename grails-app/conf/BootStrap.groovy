import edu.berkeley.calnet.cflmonitor.domain.*
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
		
		// start action trigger
		def trigger = newTrigger()
			.withIdentity( "actionTrigger", null)
			.withSchedule( cronSchedule( cronString))
			.forJob( "actionJob")
			.build()
		
		ActionJob.schedule( trigger)
    }
	
    def destroy = {
    }
}
