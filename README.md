cfl-monitor
========
Counting Failed Logins - Monitor

The issue tracker is available at https://ucidentity.atlassian.net/browse/CFLM

##System Requirements
1. Grails 2.3.4
2. Java 7
3. soapUI for testing


##Database Setup##
The default development environment utilizes an H2 file database.

Grails will automatically create the tables that exist as domain classes in the project.

After the app is running for the first time, you will need to drop the ``authentication_failure_counts`` table and create a view in its place.

**Do the following when running on localhost for the first time:**

1. Browse to http://localhost:8080/cfl/dbconsole
2. If the browser prompts for a password, enter `admin/admin`
3. On the H2 console authentication screen, enter 'sa' as the user name and a blank password
4. The JDBC URL is: ``jdbc:h2:file:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000`` 
5. Use the SQL statement window on the H2 console to drop the authentication_failure_counts table and then create it as a view
<pre>
``drop table authentication_failure_counts;``
``create or replace view authentication_failure_counts as select distinct f.subject, (select count(*) as current_count 
from authentication_failures f2 where f2.subject = f.subject and f2.recorded  > 
(select coalesce( max(r.reset), '2001-01-01') from authentication_resets r where r.subject = f.subject)) as current_count from authentication_failures f;``
</pre>

##Running the web-app
The most simple way to run the app is to execute ``grails run-app`` from the project parent directory.  
To generate a WAR file, execute ``grails dev war`` from the project parent directory.  The resulting WAR will be located in the target directory.

##Sample Action Script

Actions that are executed when a threshold is exceeded are configured in the ``action_thresholds`` table.  There is a sample action script at the top level of the project in a file named EmailAction.groovy.

The application searches a configured directory for the action script files.  The default location is set to ``/var/cfl`` in Config.groovy.  Verify that this location is valid and readable by the application and copy EmailAction.groovy to the directory.  If you are familiar with Grails, the default can be modified to any valid location.  A re-build and re-run of the application will be necessary if the default is changed.

In order to populate the EmailAction to the database, it is necessary to send an HTTP POST to [http://localhost:8080/cfl/v1/thresholds](http://localhost:8080/cfl/v1/thresholds)
Using soapUI, or any other tool that can send REST commands to a server, send the JSON body below to add the action to the database.  Note that the application endpoints are secured by Basic Authentication, so soapUI will need to have the username/password sent with the request.  There are two users that are created by default at startup - `admin/admin` and `cflUser/cf1Us3r`.

<pre>
{
description: "test description",
count: 2,
action: "emailAction",
args: "{'subject': 'Too many failures', 'host': 'some-smtp-host', 'message': 'Too many failures, fix it', 'username': 'your-user-name', 'password': 'your-password', 'recipient': 'some-email-address'}",
enabled: 1
}
</pre>

The same data can also be added via SQL into the `action_thresholds` table if desired.

###Populating Test Failure Data

In order for the sample action to execute, there must be data in the `authentication_failures` table.

The following SQL will populate a row in the failures table:
<pre>
insert into authentication_failures (ip_address,recorded,service,subject) values ('192.168.0.1',now(),'test service', 'test');
</pre>

Several rows will need to be added depending on the threshold count for the configured actions.

##Action Scripts

The threshold action scripts are essentially Groovy configuration files that are read by the application at runtime.  There are two important pieces of configuration that the script needs to have in order to be executed.

<pre>
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.*
import edu.berkeley.calnet.cflmonitor.service.ActionService

action = "actionKey"
performAction = { config, subject, args ->
	ActionService.createHistoryRecord( subject, config.action, "Action executed")
	JSONObject jsonArgs = JSON.parse( args)
}
</pre>

The `action` property in the script must match the `action` property in the `action_thresholds` table.  See the example JSON body in the previous section.

The `performAction` property is a Groovy closure that the application will execute on each subject that meets the criteria for execution.  There are three parameters that are passed into the closure:

`config` : a reference to the script itself so that the script can access any additional properties that may be present in the script.

`subject`: a String reference to the subject.

`args`: a String reference to the value of the args column for the action in the `action_thresholds` table.

##Default Polling Interval

The polling interval is set as a cron string and is located along with the action script directory in Config.groovy.  The default polling interval is set to:
`cron = "0 0/2 * * * ?"` which will poll every two minutes.

During each polling cycle, the application will read all scripts in the action scripts directory and gather all subjects that exceed the thresholds of each configured action.

The best way to change the polling interval is to use the application's REST interface.  The endpoint to change the polling interval is [http://localhost:8080/cfl/v1/admin/config](http://localhost:8080/cfl/v1/admin/config).  Send an HTTP POST with a cron string in the JSON body:
<pre>
{
	cron: '0 30 * * * ?'
}
</pre>

After sending this command, the polling interval will be stored in the database and the default from the Config.groovy file will no longer be used.

