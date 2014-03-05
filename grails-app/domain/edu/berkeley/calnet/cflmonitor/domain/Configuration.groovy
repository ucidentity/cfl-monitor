package edu.berkeley.calnet.cflmonitor.domain

class Configuration {
	
	String cron
	
	static mapping = {
		table 'cfl_configuration'
	}
	
    static constraints = {
		cron(size:1..32)
    }
}
